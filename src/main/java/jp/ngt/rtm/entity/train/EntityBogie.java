/*
 *
 *  * AppleExtended
 *  *
 *  * Original code (c) 2020 anatawa12 and other contributors.
 *  * Modifications (c) 2026 Applepie.
 *  *
 *  * This file is part of AppleExtended, which is a derivative work of fixRTM.
 *  * Both are licensed under the GNU Lesser General Public License version 3.
 *  * See LICENSE.txt in the mod root for full license text.
 *
 *
 */

package jp.ngt.rtm.entity.train;

import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.math.PooledVec3;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.ngtlib.protection.Lockable;
import jp.ngt.ngtlib.util.PermissionManager;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.entity.EntityBumpingPost;
import jp.ngt.rtm.entity.train.parts.EntityVehiclePart;
import jp.ngt.rtm.entity.train.util.BogieController.MotionState;
import jp.ngt.rtm.entity.train.util.BogieController.UpdateFlag;
import jp.ngt.rtm.entity.train.util.TrainState;
import jp.ngt.rtm.entity.vehicle.VehicleTrackerEntry;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import jp.ngt.rtm.rail.TileEntityLargeRailBase;
import jp.ngt.rtm.rail.TileEntityLargeRailCore;
import jp.ngt.rtm.rail.TileEntityLargeRailSwitchCore;
import jp.ngt.rtm.rail.util.RailMap;
import jp.ngt.rtm.world.RTMChunkManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public final class EntityBogie extends Entity implements Lockable, jp.ngt.rtm.world.IChunkLoader {
    private Ticket ticket;
    private int prevChunkCoordX = Integer.MIN_VALUE;
    private int prevChunkCoordZ = Integer.MIN_VALUE;
    public static final DataParameter<Integer> TRAIN_ID = EntityDataManager.<Integer>createKey(EntityBogie.class, DataSerializers.VARINT);
    public static final DataParameter<Byte> BOGIE_STATE = EntityDataManager.<Byte>createKey(EntityBogie.class, DataSerializers.BYTE);

    private EntityTrainBase parentTrain;
    public boolean isActivated = false;
    public float movingYaw;
    private boolean tracked;

    private TileEntityLargeRailCore currentRailObj;
    private RailMap currentRailMap;
    private final double[] posBuf = new double[3];
    private final float[] rotationBuf = new float[4];
    private int split = -1;
    private static final int SPLITS_PER_METER = 360;
    private int prevPosIndex;
    private float jointDelay;
    private boolean reverbSound;
    private int jointIndex;
    private int existCount;

    public float rotationRoll;
    public float prevRotationRoll;

    @SideOnly(Side.CLIENT)
    private int carPosRotationInc;
    @SideOnly(Side.CLIENT)
    private float carYaw, carPitch, carRoll;

    public EntityBogie(World world) {
        super(world);
        this.ignoreFrustumCheck = true;
        this.preventEntitySpawning = true;
        this.setBogieSize(EntityTrainBase.TRAIN_WIDTH, EntityTrainBase.TRAIN_HEIGHT);
    }

    public EntityBogie(World world, byte id, EntityTrainBase parent) {
        this(world);
        this.setBogieId(id);
        this.parentTrain = parent;
    }

    public void setBogieSize(float width, float height) {
        this.setSize(width, height);
    }

    @Override
    protected void entityInit() {
        this.getDataManager().register(TRAIN_ID, Integer.valueOf(0));
        this.getDataManager().register(BOGIE_STATE, Byte.valueOf((byte) 0));
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound nbt) {
        return false;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity par1) {
        return (par1 instanceof EntityVehiclePart) ? null : par1.getEntityBoundingBox();
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return null;
        //return this.getEntityBoundingBox();
    }

    @Override
    public double getYOffset() {
        return 0.0D;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    public MotionState updateBogiePos(float speed, float trainLength, EntityBogie frontBogie) {
        if (this.updateCollision()) {
            return MotionState.STOP;
        }

        this.movingYaw = NGTMath.wrapAngle(this.rotationYaw + (this.isFront() ? 0.0F : 180.0F));

        double px = this.posX + (double) NGTMath.sin(this.movingYaw) * (double) speed;
        double py = this.posY;
        double pz = this.posZ + (double) NGTMath.cos(this.movingYaw) * (double) speed;

        if (!this.resetRailObj(px, py, pz)) {
            return MotionState.FLY;
        }

        RailMap rm = this.currentRailMap;
        int pIndex = 0;
        if (frontBogie == null || this.prevPosIndex == -1) {
            pIndex = rm.getNearlestPoint(this.split, px, pz);
        } else {
            int indexInc = (int) ((Math.abs(speed) + 0.25F) * SPLITS_PER_METER);
            int indexNeg = this.prevPosIndex - indexInc;
            int indexPos = this.prevPosIndex + indexInc;
            int indexMin = (indexNeg < 0) ? 0 : indexNeg;
            int indexMax = (indexPos > this.split) ? this.split : indexPos;
            double[] fp = frontBogie.getPosBuf();
            double dif = Double.MAX_VALUE;
            double tlSq = trainLength * trainLength;
            for (int i = indexMin; i < indexMax; ++i) {
                double[] pxz = rm.getRailPos(this.split, i);
                double lenTemp = getDistanceSq(pxz[1], py, pxz[0], fp[0], fp[1], fp[2]);
                double difTemp = Math.abs(lenTemp - tlSq);
                if (difTemp < dif) {
                    dif = difTemp;
                    pIndex = i;
                }
            }
        }
        this.prevPosIndex = pIndex;

        double[] posZX = rm.getRailPos(this.split, pIndex);
        py = rm.getRailHeight(this.split, pIndex) + this.getYOffset();
        float railYaw = NGTMath.wrapAngle(rm.getRailRotation(this.split, pIndex));
        float movYaw = this.fixBogieYaw(this.movingYaw, railYaw);
        float yaw = this.fixBogieYaw(this.rotationYaw, movYaw);
        float pitch = this.fixBogiePitch(rm.getRailPitch(this.split, pIndex), railYaw, yaw);
        float cant = rm.getCant(this.split, pIndex);
        if (Math.abs(NGTMath.wrapAngle(railYaw - yaw)) > 90.0F) {
            cant *= -1.0F;
        }

        if ((this.posX == posZX[1]) && (this.posZ == posZX[0])) {
            return MotionState.MOVE;
        }

        this.posBuf[0] = posZX[1];
        this.posBuf[1] = py;
        this.posBuf[2] = posZX[0];
        this.rotationBuf[0] = yaw;
        this.rotationBuf[1] = pitch;
        this.rotationBuf[2] = movYaw;
        this.rotationBuf[3] = cant;

        if (this.jointDelay > 0.0F) {
            this.jointDelay -= Math.abs(speed);
            if (this.jointDelay <= 0.0F) {
                this.playJointSound();
            }
        }

        return MotionState.MOVE;
    }

    private static double getDistanceSq(double x0, double y0, double z0, double x1, double y1, double z1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean resetRailObj(double px, double py, double pz) {
        TileEntityLargeRailCore tileentitylargerailcore = this.getRail(px, py, pz);

        if (tileentitylargerailcore == null) {
            return false;
        } else {
            if (this.currentRailObj != tileentitylargerailcore) {
                RailMap railMap;
                if (tileentitylargerailcore instanceof TileEntityLargeRailSwitchCore) {
                    TileEntityLargeRailSwitchCore tileentitylargerailswitchcore = (TileEntityLargeRailSwitchCore) tileentitylargerailcore;
                    railMap = tileentitylargerailswitchcore.getSwitch().getNearestPoint(this).getActiveRailMap(this.world);
                } else {
                    railMap = tileentitylargerailcore.getRailMap(this);
                }
                this.currentRailObj = tileentitylargerailcore;
                this.currentRailMap = railMap;
                this.split = (int) (this.currentRailMap.getLength() * SPLITS_PER_METER);
                this.prevPosIndex = -1;
                this.onChangeRail(tileentitylargerailcore);
            }

            return true;
        }
    }

    private TileEntityLargeRailCore getRail(double px, double py, double pz) {
        TileEntityLargeRailBase railObj = TileEntityLargeRailBase.getRailFromCoordinates(this.world, px, py + 2.0D, pz, 0);
        if (railObj != null) {
            TileEntityLargeRailCore coreObj = railObj.getRailCore();
            if (coreObj != null) {
                return coreObj;
            }
        }
        return null;
    }

    protected void onChangeRail(TileEntityLargeRailCore newRail) {
        this.reverbSound = newRail.isReberbSound();
        EntityTrainBase train = this.getTrain();
        if (train != null) {
            TrainConfig trainconfig = train.getResourceState().getResourceSet().getConfig();
            if (!trainconfig.muteJointSound) {
                this.jointIndex = 0;
                this.playJointSound();
            }
        }
    }

    protected boolean reverseJointArray() {
        boolean b0 = this.getTrain().getTrainDirection() == 0;
        boolean b1 = this.getBogieId() == 0;
        return b0 ^ b1;
    }

    protected void playJointSound() {
        EntityTrainBase entitytrainbase = this.getTrain();
        TrainConfig trainconfig = entitytrainbase.getResourceState().getResourceSet().getConfig();
        if (!trainconfig.muteJointSound) {
            float f = Math.abs(entitytrainbase.getSpeed()) / trainconfig.maxSpeed[4] * 0.5F + 1.0F;
            String s = this.reverbSound ? "rtm:sounds/train/joint_reverb.ogg" : "rtm:sounds/train/joint.ogg";
            RTMCore.proxy.playSound(this, s, 4.0F, f);

            int i = trainconfig.jointDelay[this.getBogieId()].length;
            if (this.jointIndex < i - 1) {
                int j = this.reverseJointArray() ? i - this.jointIndex - 1 : this.jointIndex;
                ++this.jointIndex;
                int k = this.reverseJointArray() ? i - this.jointIndex - 1 : this.jointIndex;
                float fcur = trainconfig.jointDelay[this.getBogieId()][j];
                float fnex = trainconfig.jointDelay[this.getBogieId()][k];
                this.jointDelay = Math.abs(fnex - fcur);
            }
        }
    }

    public double[] getPosBuf() {
        return this.posBuf;
    }

    public void moveBogie(EntityTrainBase train, double x, double y, double z, UpdateFlag flag) {
        this.setPosition(x, y, z);
        switch (flag) {
            case ALL:
                this.setRotation(this.rotationBuf[0], this.rotationBuf[1]);
                this.movingYaw = this.rotationBuf[2];
                this.rotationRoll = this.rotationBuf[3];
                break;
            case YAW:
                float movYaw = this.fixBogieYaw(this.movingYaw, train.rotationYaw);
                float yaw = this.fixBogieYaw(this.rotationYaw, movYaw);
                this.rotationYaw = yaw % 360.0F;
                this.movingYaw = movYaw;
                break;
            case NONE:
                break;
        }
    }

    @Override
    public final void onUpdate() {
        if (!this.world.isRemote && !this.tracked) {
            this.tracked = VehicleTrackerEntry.trackingVehicle(this);
        }

        if (!this.world.isRemote) {
            this.updateChunks();
            if (this.currentRailObj != null) {
                this.currentRailObj.colliding = true;
            } else {
                this.resetRailObj(this.posX, this.posY, this.posZ);
            }
        }

        ++this.existCount;
        if (this.existCount > 100 && this.getTrain() == null) {
            if (!this.world.isRemote) {
                this.setDead();
            }
        }
    }

    public void onBogieUpdate() {
        super.onUpdate();
        this.prevRotationRoll = this.rotationRoll;

        if (this.world.isRemote) {
            this.updatePosAndRotationClient();

            if (this.world.isBlockLoaded(this.getPosition())) {
                this.doBlockCollisions();
            }
        }
    }

    @Override
    public void setDead() {
        if (!this.world.isRemote) {
            this.releaseTicket();
        }
        boolean flag = this.getEntityWorld().isRemote && this.getTrain() != null && this.getTrain().getVehicleState(TrainState.TrainStateType.ChunkLoader) > 0;
        if (this.existCount > 100 && !flag || (this.getEntityWorld().isRemote && this.getTrain() == null)) {
            super.setDead();
        }
    }

    @SideOnly(Side.CLIENT)
    protected void updatePosAndRotationClient() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.carPosRotationInc > 0) {
            float d0 = 1.0F / (float) this.carPosRotationInc;
            this.rotationYaw += NGTMath.wrapAngle((float) (this.carYaw - (double) this.rotationYaw)) * d0;
            this.rotationPitch += (this.carPitch - (double) this.rotationPitch) * d0;
            this.rotationRoll += (this.carRoll - (double) this.rotationRoll) * d0;
            --this.carPosRotationInc;
        }

        EntityTrainBase train = this.getTrain();
        if (train != null) {
            float[][] pos = train.getResourceState().getResourceSet().getConfig().getBogiePos();
            int bogieIndex = this.getBogieId();
            Vec3 v31 = PooledVec3.create(pos[bogieIndex][0], pos[bogieIndex][1], pos[bogieIndex][2]);
            v31 = v31.rotateAroundX(train.rotationPitch);
            v31 = v31.rotateAroundY(train.rotationYaw);
            double newX = train.posX + v31.getX();
            double newY = train.posY + v31.getY();
            double newZ = train.posZ + v31.getZ();
            this.setPosition(newX, newY, newZ);
        }

        this.setRotation(this.rotationYaw, this.rotationPitch);
        this.setPosition(this.posX, this.posY, this.posZ);
    }

    @Override
    public boolean attackEntityFrom(DamageSource par1, float par2) {
        if (this.getTrain() != null && !this.getTrain().isDead) {
            return this.getTrain().attackEntityFrom(par1, par2);
        } else {
            this.setDead();
            return true;
        }
    }

    private boolean updateCollision() {
        double range = this.width * 2.0F;
        List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this,
                new AxisAlignedBB(
                        this.posX - range, this.posY - EntityTrainBase.TRAIN_HEIGHT, this.posZ - range,
                        this.posX + range, this.posY + EntityTrainBase.TRAIN_HEIGHT, this.posZ + range));
        return this.collideWithEntities(list);
    }

    private boolean collideWithEntities(List<Entity> list) {
        EntityTrainBase train = this.getTrain();
        EntityBogie nearestBogie = null;
        double distanceSq = Double.MAX_VALUE;

        for (Entity entity : list) {
            if (entity instanceof EntityBogie) {
                if (this.collideWithBogie((EntityBogie) entity)) {
                    double d0 = this.getDistanceSq(entity);
                    if (d0 < distanceSq) {
                        nearestBogie = (EntityBogie) entity;
                        distanceSq = d0;
                    }
                }
            } else if (entity instanceof EntityBumpingPost) {
                this.collideWithBumpingPost((EntityBumpingPost) entity);
            } else if (entity instanceof EntityLivingBase) {
                this.collideWithLiving((EntityLivingBase) entity);
            }
        }

        if (nearestBogie != null) {
            this.connectBogie(nearestBogie);
            return true;
        }

        return false;
    }

    private boolean collideWithBogie(EntityBogie bogie) {
        EntityTrainBase train = this.getTrain();
        EntityTrainBase train2 = bogie.getTrain();

        if (train2 != null && !train.equals(train2)) {
            if (train.getFormation() != null && train.getFormation().containBogie(bogie)) {
                return false;
            }
            if (!train.inConnectableRange(train2)) {
                return false;
            }
            ;

            RailMap rm0 = TileEntityLargeRailBase.getRailMapFromCoordinates(this.world, this, this.posX, this.posY, this.posZ);
            RailMap rm1 = TileEntityLargeRailBase.getRailMapFromCoordinates(this.world, bogie, bogie.posX, bogie.posY, bogie.posZ);
            if (!(rm0 != null && rm0.canConnect(rm1))) {
                return false;
            }

            if (this.isActivated || bogie.isActivated) {
                return true;
            }

            float speed0 = train.getSpeed();
            float speed1 = train2.getSpeed();
            boolean b0 = train.getTrainDirection() == this.getBogieId();
            boolean b1 = train2.getTrainDirection() == bogie.getBogieId();

            float sp0 = b0 ? speed0 : -speed0;
            float sp1 = b1 ? -speed1 : speed1;
            if (sp0 - sp1 >= 0.0F) {
                train2.setSpeed(b0 ^ b1 ? speed0 : -speed0);
                train.setSpeed(b0 ^ b1 ? speed1 : -speed1);
            }
        }

        return false;
    }

    private void connectBogie(EntityBogie bogie) {
        this.getTrain().connectTrain(this, bogie);
        this.isActivated = false;
        bogie.isActivated = false;
    }

    private boolean collideWithBumpingPost(EntityBumpingPost entity) {
        EntityTrainBase train = this.getTrain();
        double dis = NGTMath.pow((EntityTrainBase.TRAIN_WIDTH / 2.0F) + 0.375F, 2);
        double dsq = this.getDistanceSq(entity);
        if (train.getTrainDirection() == this.getBogieId() && dsq <= dis) {
            train.stopTrain(true);
        }
        return false;
    }

    private boolean collideWithLiving(EntityLivingBase entity) {
        EntityTrainBase train = this.getTrain();

        if (!entity.equals(train.getFirstPassenger()) && !entity.isRiding()) {
            float speed = train.getSpeed();
            double dis = NGTMath.pow((this.width / 2.0F) + 0.375F, 2);

            if (speed > 0.0F && this.getDistanceSq(entity) < dis) {
                double d2 = entity.posX - this.posX;
                double d3 = entity.posZ - this.posZ;
                double d4 = (d2 * d2 + d3 * d3);
                entity.addVelocity(d2 / d4 * 10.0D, 0.3D, d3 / d4 * 10.0D);
                DamageSource dmgSource = new EntityDamageSource("train", train);
                float damage = speed * 7.2F;
                entity.attackEntityFrom(dmgSource, damage);
            }
        }

        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void setPositionAndRotationDirect(double par1, double par3, double par5, float par7, float par8, int par9, boolean par10) {
        this.carPosRotationInc = par9;
        this.carYaw = par7;
        this.carPitch = par8;
    }

    @SideOnly(Side.CLIENT)
    public void setRoll(float par1) {
        this.carRoll = par1;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void setVelocity(double par1, double par3, double par5) {
    }

    @Override
    public void move(MoverType type, double par1, double par3, double par5) {
    }

    @Override
    public void addVelocity(double par1, double par3, double par5) {
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (this.getTrain() != null) {
            if (player.isSneaking()) {
                if (player.world.isRemote) {
                    player.openGui(RTMCore.instance, RTMCore.guiIdSelectEntityModel,
                            player.world, this.getTrain().getEntityId(), 0, 0);
                }
                return true;
            }
            if (player.world.isRemote)
                return true;
            if (PermissionManager.INSTANCE.hasPermission(player, RTMCore.DRIVE_TRAIN)) {
                return this.getTrain().interactTrain(this, player);
            }
        } else if (player.inventory.getCurrentItem() != null && player.inventory.getCurrentItem().getItem() == RTMItem.paddle) {
            return true;
        }
        return true;
    }

    public void setTrain(EntityTrainBase train) {
        this.getDataManager().set(TRAIN_ID, Integer.valueOf(train.getEntityId()));
    }

    public EntityTrainBase getTrain() {
        if (this.parentTrain == null || this.parentTrain.isDead) {
            int id = this.getDataManager().get(TRAIN_ID);
            Entity entity = this.world.getEntityByID(id);
            if (entity instanceof EntityTrainBase) {
                this.parentTrain = (EntityTrainBase) entity;
            }

            if (this.parentTrain == null || this.parentTrain.isDead) {
                return null;
            }
        }
        return this.parentTrain;
    }

    private byte getBogieState() {
        return this.getDataManager().get(BOGIE_STATE);
    }

    private void setBogieState(int par1) {
        this.getDataManager().set(BOGIE_STATE, Byte.valueOf((byte) par1));
    }

    public boolean isFront() {
        int state = this.getBogieState();
        return (state >> 4) == 1;
    }

    public void setFront(boolean par1) {
        int i = par1 ? 1 : 0;
        int state = this.getBogieState();
        state = (state & 0x0F) | (i << 4);
        this.setBogieState(state);
    }

    public byte getBogieId() {
        int state = this.getBogieState();
        return (byte) ((state & 0x0F) - 1);
    }

    public void setBogieId(int par1) {
        int state = this.getBogieState();
        state = (state & 0xF0) | (par1 + 1);
        this.setBogieState(state);
    }

    public static float fixBogieYaw(float yaw1, float yaw2) {
        float f0 = Math.abs(yaw1 - yaw2);
        f0 = f0 > 180.0F ? 360.0F - f0 : f0;
        return NGTMath.wrapAngle(f0 > 90.0F ? yaw2 + 180.0F : yaw2);
    }

    public static float fixBogiePitch(float railPitch, float railYaw, float bogieYaw) {
        float f0 = Math.abs(bogieYaw - railYaw);
        return NGTMath.wrapAngle(f0 > 45.0F ? -railPitch : railPitch);
    }

    @Override
    public Object getTarget(World world, int x, int y, int z) {
        return this.getTrain();
    }

    @Override
    public boolean lock(EntityPlayer player, String code) {
        return true;
    }

    @Override
    public boolean unlock(EntityPlayer player, String code) {
        return true;
    }

    @Override
    public int getProhibitedAction() {
        return 1;
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        com.anatawa12.fixRtm.rtm.entity.train.EntityBogieKt.onRemovedFromWorld(this);
    }

    @Override
    public void addEntityCrashInfo(net.minecraft.crash.CrashReportCategory category) {
        super.addEntityCrashInfo(category);
        com.anatawa12.fixRtm.rtm.entity.train.EntityBogieKt.addEntityCrashInfo(this, category);
    }

    private void updateChunks() {
        if (!this.world.isRemote) {
            if (this.isChunkLoaderEnable()) {
                if (this.ticket == null || this.chunkCoordX != this.prevChunkCoordX || this.chunkCoordZ != this.prevChunkCoordZ) {
                    this.forceChunkLoading();
                }
            } else {
                this.releaseTicket();
            }

            this.prevChunkCoordX = this.chunkCoordX;
            this.prevChunkCoordZ = this.chunkCoordZ;
        }
    }

    @Override
    public boolean isChunkLoaderEnable() {
        EntityTrainBase train = this.getTrain();
        return !this.isDead && train != null && train.isChunkLoaderEnable();
    }

    @Override
    public void setChunkTicket(Ticket par1) {
        if (this.ticket != par1) {
            this.releaseTicket();
        }
        this.ticket = par1;
    }

    @Override
    public void forceChunkLoading() {
        this.forceChunkLoading(this.chunkCoordX, this.chunkCoordZ);
    }

    @Override
    public void forceChunkLoading(int x, int z) {
        if (!this.world.isRemote) {
            if (this.ticket == null) {
                this.ticket = RTMChunkManager.INSTANCE.getNewTicket(this.world, ForgeChunkManager.Type.ENTITY);
                if (this.ticket != null) {
                    this.ticket.bindEntity(this);
                }
            }

            if (this.ticket != null) {
                ForgeChunkManager.forceChunk(this.ticket, new ChunkPos(x, z));
            }
        }
    }

    private void releaseTicket() {
        if (this.ticket != null) {
            ForgeChunkManager.releaseTicket(this.ticket);
            this.ticket = null;
        }
    }
    /**
     * 現在このボギーが乗っているレールのコア。
     * まだレールに乗っていない場合は null。
     */
    public TileEntityLargeRailCore getCurrentRailObj() {
        return this.currentRailObj;
    }
}
