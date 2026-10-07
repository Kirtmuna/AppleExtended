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

import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.math.PooledVec3;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.ngtlib.renderer.NGTParticle;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.ngtlib.util.PermissionManager;
import jp.ngt.rtm.*;
import jp.ngt.rtm.electric.WireManager;
import jp.ngt.rtm.entity.npc.EntityMotorman;
import jp.ngt.rtm.entity.npc.macro.MacroRecorder;
import jp.ngt.rtm.entity.train.util.*;
import jp.ngt.rtm.entity.train.util.BogieController.MotionState;
import jp.ngt.rtm.entity.train.util.BogieController.UpdateFlag;
import jp.ngt.rtm.entity.train.util.TrainState.TrainStateType;
import jp.ngt.rtm.entity.vehicle.EntityVehicleBase;
import jp.ngt.rtm.item.ItemTrain;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetTrain;
import jp.ngt.rtm.modelpack.modelset.ModelSetVehicleBase;
import jp.ngt.rtm.network.PacketNotice;
import jp.ngt.rtm.rail.TileEntityLargeRailBase;
import jp.ngt.rtm.rail.TileEntityLargeRailCore;
import jp.ngt.rtm.world.IChunkLoader;
import jp.ngt.rtm.world.RTMChunkManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;
import java.util.Set;

public abstract class EntityTrainBase extends EntityVehicleBase<ModelSetTrain> implements IChunkLoader {
    private static final DataParameter<Integer> BOGIE_ID0 = EntityDataManager.<Integer>createKey(EntityTrainBase.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> BOGIE_ID1 = EntityDataManager.<Integer>createKey(EntityTrainBase.class, DataSerializers.VARINT);
    private static final DataParameter<Float> TRAIN_SPEED = EntityDataManager.createKey(EntityTrainBase.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> CAB_DIRECTION = EntityDataManager.createKey(EntityTrainBase.class, DataSerializers.BOOLEAN);

    public static final short MAX_AIR_COUNT = 2880;
    public static final short MIN_AIR_COUNT = 2480;
    public static final float TRAIN_WIDTH = 2.75F;
    public static final float TRAIN_HEIGHT = 1.1875F;

    public final BogieController bogieController = new BogieController();
    private Formation formation;

    public int brakeCount = 72;
    public int atsCount;
    public boolean onRail = true;

    public int brakeAirCount = MAX_AIR_COUNT;
    public boolean complessorActive;

    private float wave;
    // ATS制動制御用
    private boolean atsBraking = false;
    private float atsTargetSpeed = 0.0F;
    /** ATS制動時の1tickあたり減速量。engageAtsBrake で車両性能から算出 */
    private float atsDecelPerTick = 0.0F;
    /** 減速度が取得できなかった場合のフォールバック */
    private static final float ATS_DECEL_FALLBACK = 0.005F;

    public EntityTrainBase(World world) {
        super(world);
        this.setSize(TRAIN_WIDTH, TRAIN_HEIGHT);
        this.noClip = true;
    }

    public EntityTrainBase(World world, String s) {
        this(world);
    }

    public void spawnTrain(World world) {
        world.spawnEntity(this);
        this.formation = FormationManager.getInstance().createNewFormation(this);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.getDataManager().register(BOGIE_ID0, Integer.valueOf(0));
        this.getDataManager().register(BOGIE_ID1, Integer.valueOf(0));
        this.getDataManager().register(TRAIN_SPEED, 0.0f);
        this.getDataManager().register(CAB_DIRECTION, false);

        jp.apple.replaymod.compat.ReplaySyncManager.registerData(this);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);

        NBTTagCompound entryData = new NBTTagCompound();
        this.writeFormationData(entryData);
        if (!entryData.hasNoTags()) {
            nbt.setTag("FormationEntry", entryData);
        } else {
            nbt.removeTag("FormationEntry");
        }

        nbt.setInteger("trainDir", this.getTrainDirection());
        nbt.setBoolean("cabDir", this.getDataManager().get(CAB_DIRECTION));
    }

    private void writeFormationData(NBTTagCompound nbt) {
        if (this.formation == null) {
            return;
        }

        FormationEntry entry = this.formation.getEntry(this);
        if (entry != null) {
            nbt.setLong("FormationId", this.formation.id);
            nbt.setByte("EntryPos", entry.entryId);
            nbt.setByte("EntryDir", entry.dir);
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        jp.apple.replaymod.compat.ReplaySyncManager.patchMetadata(this, nbt);
        super.readEntityFromNBT(nbt);

        if (nbt.hasKey("FormationEntry", 10)) {
            this.readFormationData(nbt.getCompoundTag("FormationEntry"));
        } else {
            this.formation = FormationManager.getInstance().createNewFormation(this);
        }

        this.setTrainDirection(nbt.getInteger("trainDir"));
        this.getDataManager().set(CAB_DIRECTION, nbt.getBoolean("cabDir"));
    }

    private void readFormationData(NBTTagCompound nbt) {
        if (nbt == null || nbt.hasNoTags() || !nbt.hasKey("FormationId")) {
            this.formation = FormationManager.getInstance().createNewFormation(this);
            return;
        }

        long fid = nbt.getLong("FormationId");
        int entryPos = nbt.getByte("EntryPos") & 0xFF;
        byte entryDir = nbt.getByte("EntryDir");

        if (fid <= 0L) {
            this.formation = FormationManager.getInstance().createNewFormation(this);
            return;
        }

        Formation fm = FormationManager.getInstance().getFormation(fid);
        if (fm == null) {
            this.formation = FormationManager.getInstance().createNewFormation(this);
            if (!jp.ngt.ngtlib.util.NGTUtil.isServer()) {
                com.anatawa12.fixRtm.network.NetworkHandler.sendPacketServer(
                        new com.anatawa12.fixRtm.network.RequestFormation(fid));
            }
            return;
        }

        if (entryPos < 0 || entryPos >= fm.size()) {
            this.formation = FormationManager.getInstance().createNewFormation(this);
            return;
        }

        this.formation = fm;
        fm.setTrain(this, entryPos, entryDir);
    }

    @Override
    public Entity[] getParts() {
        return null;
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity par1) {
        return null;
    }

    @Override
    public double getYOffset() {
        return 0.0D;
    }

    @Override
    public void setDead() {
        super.setDead();

        if (!this.world.isRemote) {
            this.releaseTicket();
            this.bogieController.setDead(this);

            try {
                this.formation.onRemovedTrain(this);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onVehicleUpdate() {
        jp.apple.replaymod.compat.ReplaySyncManager.syncModel(this, false);

        this.updateSpeed();

        if (this.existBogies()) {
            this.bogieController.updateBogies(this);
        }

        super.onVehicleUpdate();

        jp.apple.train.TrainPlatformHandler.updateStandingEntities(this);

        if (this.world.isRemote) {
            this.spawnSmoke();
        } else {
            this.updateChunks();
            this.updateATS();
        }
    }

    @Override
    protected void updateFallState() {
        if (this.onRail) {
            this.motionY = 0.0D;
        } else {
            super.updateFallState();
        }
    }

    @Override
    protected void updateMovement() {
        if (this.formation != null && this.formation.isFrontCar(this)) {
            this.formation.updateTrainMovement();
        }
    }

    public void updateTrainMovement(MotionState state) {
        if (state == MotionState.FLY) {
            if (this.onRail) {
                float speed = this.getSpeed() * this.getMoveDir();
                Vec3 vec3 = PooledVec3.create(0.0D, 0.0D, speed);
                vec3 = vec3.rotateAroundX(this.rotationPitch + 15.0F);
                vec3 = vec3.rotateAroundY(this.rotationYaw);
                this.motionX = vec3.getX();
                this.motionY = vec3.getY();
                this.motionZ = vec3.getZ();
                this.onRail = false;
                this.noClip = false;
                this.setSpeed(0.0F);
            }
        } else if (state == MotionState.MOVE) {
            if (!this.onRail) {
                this.motionX = this.motionY = this.motionZ = 0.0D;
                this.onRail = true;
                this.noClip = true;
            }
        }

        if (!this.onRail) {
            super.updateMovement();
            this.bogieController.updateBogiePos(this, 0, UpdateFlag.NONE);
            this.bogieController.updateBogiePos(this, 1, UpdateFlag.NONE);
        }
    }

    @Override
    protected void applyPhysicalEffect() {
        double d0 = 0.99D;
        this.motionX *= d0;
        this.motionY *= d0;
        this.motionZ *= d0;

        float f0 = 0.125F;

        if (this.rotationPitch > 0.0F) {
            this.rotationPitch -= f0;
        } else if (this.rotationPitch < 0.0F) {
            this.rotationPitch += f0;
        }

        if (Math.abs(this.rotationPitch) < f0) {
            this.rotationPitch = 0.0F;
        }

        this.getBogie(0).rotationPitch = this.rotationPitch;
        this.getBogie(1).rotationPitch = this.rotationPitch * -1.0F;
    }

    public void updateRoll(float par1) {
        TrainConfig cfg = this.getResourceState().getResourceSet().getConfig();
        float f0 = -cfg.rolling;
        float pendulum = NGTMath.wrapAngle((this.rotationYaw - this.prevRotationYaw) * f0);
        if (this.getTrainDirection() == 1) {
            pendulum *= -1.0F;
        }
        float roll = par1 + pendulum;
        this.wave = (float) ((this.wave + this.getSpeed() * cfg.rollSpeedCoefficient) % (2.0D * Math.PI));
        float sw = (NGTMath.getSin(this.wave) + NGTMath.getSin(this.wave * cfg.rollVariationCoefficient)) * 0.5F;
        this.rotationRoll = roll + sw * cfg.rollWidthCoefficient;
    }

    protected void updateATS() {
        if (this.atsCount > 0) {
            ++this.atsCount;
            if (this.atsCount >= 100) {
                this.stopTrain(false);
                this.atsCount = 0;
            }
        }
    }

    @Override
    protected void updateBlockCollisionState() {
        int minY = NGTMath.floor(this.posY) - 3;
        TileEntityLargeRailBase rail = TileEntityLargeRailBase.getRailFromCoordinates(this.world, this.posX, this.posY + 1.0D, this.posZ, minY);
        if (rail != null) {
            TileEntityLargeRailCore railCore = rail.getRailCore();
            if (railCore != null) {
                int signal = railCore.getSignal();
                this.setSignal(signal);
            }
        }

        this.doBlockCollisions();
    }

    @SideOnly(Side.CLIENT)
    protected void spawnSmoke() {
        ModelSetTrain modelsettrain = this.getResourceState().getResourceSet();
        if (modelsettrain.getConfig().smoke != null) {
            float f = this.getSpeed();
            int i = this.getNotch();
            Random random = this.world.rand;

            for (int j = 0; j < (modelsettrain.getConfig()).smoke.length; ++j) {
                Vec3 vec3 = PooledVec3.create((double) (modelsettrain.getConfig()).smoke[j][0], (double) (modelsettrain.getConfig()).smoke[j][1], (double) (modelsettrain.getConfig()).smoke[j][2]);
                vec3 = vec3.rotateAroundX(this.rotationPitch);
                vec3 = vec3.rotateAroundY(this.rotationYaw);
                double d0 = (double) (modelsettrain.getConfig()).smoke[j][4];
                double d1 = (double) (modelsettrain.getConfig()).smoke[j][5];
                int k = f > 0.05F ? (int) d1 : (i > 0 ? (int) d0 + 3 : (int) d0);
                EnumParticleTypes enumparticletypes = NGTParticle.getParticle((String) (modelsettrain.getConfig()).smoke[j][3]);

                for (int l = 0; l < k; ++l) {
                    double d2 = this.posX + vec3.getX() + (double) random.nextFloat() * 0.5D - 0.25D;
                    double d3 = this.posY + vec3.getY() + (double) this.getVehicleYOffset();
                    double d4 = this.posZ + vec3.getZ() + (double) random.nextFloat() * 0.5D - 0.25D;
                    double d5 = 0.0625D;
                    if ((modelsettrain.getConfig()).smoke.length == 7) {
                        d5 = (double) (modelsettrain.getConfig()).smoke[j][6];
                    }

                    double d6 = (random.nextDouble() * 2.0D - 1.0D) * d5;
                    double d7 = (random.nextDouble() * 2.0D - 1.0D) * d5;
                    this.world.spawnParticle(enumparticletypes, d2, d3, d4, d6, 0.25D, d7, new int[0]);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected void updateAnimation() {
        super.updateAnimation();

        if (this.getTrainDirection() == 0 && this.seatRotation > -MAX_SEAT_ROTATION) {
            --this.seatRotation;
        }

        if (this.getTrainDirection() == 1 && this.seatRotation < MAX_SEAT_ROTATION) {
            ++this.seatRotation;
        }

        this.setRollsignAnimation(this.getVehicleState(TrainStateType.Destination));

        if (this.rollsignAnimation > this.rollsignV) {
            --this.rollsignAnimation;
        } else if (this.rollsignAnimation < this.rollsignV) {
            ++this.rollsignAnimation;
        }

        if (this.getResourceState().type == RTMResource.TRAIN_EC) {
            if (this.complessorActive) {
                ++this.brakeAirCount;
                if (this.brakeAirCount >= MAX_AIR_COUNT) {
                    this.complessorActive = false;
                    RTMCore.proxy.playSound(this, RTMSound.CP_FIN, 1.0F, 1.0F);
                }
            } else {
                if (this.brakeAirCount < MIN_AIR_COUNT) {
                    this.complessorActive = true;
                }
            }
        }
    }

    private static final int[] PANTO_POS_ZERO = new int[]{0, 0};

    @Override
    protected int[] getPantographMaxHeight() {
        int[] ia;
        ModelSetTrain modelSet = this.getResourceState().getResourceSet();
        if (modelSet.getConfig().pantoPos != null) {
            ia = new int[modelSet.getConfig().pantoPos.length];
            for (int i = 0; i < ia.length; ++i) {
                float[] fa = modelSet.getConfig().pantoPos[i];
                if (fa[3] > 0.0F) {
                    double trainY = this.posY + this.getVehicleYOffset();
                    Vec3 vec = PooledVec3.create(fa[0], fa[3], fa[1]);
                    vec = vec.rotateAroundX(this.rotationPitch);
                    vec = vec.rotateAroundY(this.rotationYaw);
                    double y = WireManager.INSTANCE.getWireY(this.posX + vec.getX(), trainY + vec.getY(), this.posZ + vec.getZ());
                    ia[i] = (int) (((y - trainY) - fa[3]) / (fa[2] - fa[3]) * MAX_PANTOGRAPH_MOVE);

                    long time = this.getEntityWorld().getWorldTime() % 24000;
                    if ((trainY + vec.getY() != y) && this.getSpeed() != 0.0F && (time >= 11615 && time <= 22925)
                            && this.getEntityWorld().getBiome(this.getPosition()).isSnowyBiome()) {
                        Random rand = this.getEntityWorld().rand;
                        if (rand.nextInt(20) == 0) {
                            for (int k = 0; k < 5; ++k) {
                                NGTParticle.INSTANCE.spawnParticle(this.getEntityWorld(), RTMParticle.PARTICLE_SPARK, false,
                                        this.posX + vec.getX(), y, this.posZ + vec.getZ(),
                                        rand.nextGaussian() * 0.0625D, -0.25D, rand.nextGaussian() * 0.0625D, ia);
                            }
                        }
                    }
                }
            }
        } else {
            ia = PANTO_POS_ZERO;
        }
        return ia;
    }

    @SideOnly(Side.CLIENT)
    public float getCouplerYaw(int dir) {
        EntityTrainBase conTrain = this.getConnectedTrain(dir);
        if (conTrain == null) {
            return 0.0F;
        } else {
            float dif = NGTMath.getAngleD(this.posZ, this.posX, conTrain.posZ, conTrain.posX);
            float angle = NGTMath.wrapAngle(dif - (this.rotationYaw + (dir == 0 ? 0.0F : 180.0F)));
            return angle + (Math.abs(angle) > 90.0F ? 180.0F : 0.0F);
        }
    }

    protected void playBrakeReleaseSound(boolean isStrong) {
        ModelSetTrain modelSet = this.getResourceState().getResourceSet();
        String sound = isStrong ? modelSet.getConfig().sound_BrakeRelease : modelSet.getConfig().sound_BrakeRelease2;
        if (sound != null) {
            RTMCore.proxy.playSound(this, sound, 1.0F, 1.0F);
        }
    }

    protected void updateSpeed() {
        if (!this.onRail) {
            return;
        }

        if (this.atsBraking) {
            if (this.isControlCar() && this.formation != null && !this.world.isRemote) {
                float current = this.getSpeed();
                float target = this.atsTargetSpeed;
                float decel = this.atsDecelPerTick;
                if (decel <= 0.0F) decel = ATS_DECEL_FALLBACK;

                if (Math.abs(current) > Math.abs(target) + 1.0E-3F) {
                    float sign = Math.signum(current);

                    float speedDiff = Math.abs(current - target);
                    float ratio = Math.min(1.0F, speedDiff / 0.05F);

                    float actualDecel = decel * (0.3F + 0.7F * ratio);
                    float newSpeed = current - sign * actualDecel;

                    if (Math.signum(newSpeed) != sign) {
                        newSpeed = target;
                    } else if (Math.abs(newSpeed) < Math.abs(target)) {
                        newSpeed = target;
                    }
                    this.setSpeed(newSpeed);
                }
            }
            return;
        }

        int i = this.getNotch();
        float f = this.getSpeed();

        Entity passenger = this.getFirstPassenger();
        if (passenger == null || !(passenger instanceof EntityPlayer || passenger instanceof EntityMotorman)) {
        }

        boolean flag = true;

        if (i < 0) {
            int max = i < 0 ? i * -18 : 0;
            if (this.brakeCount < max) {
                ++this.brakeCount;
                if (this.world.isRemote) {
                    --this.brakeAirCount;
                }
            } else if (this.brakeCount > max) {
                this.brakeCount -= (this.brakeCount - max > 1) ? 2 : 1;
            }
        } else if (i >= 0) {
            if (this.brakeCount > 0) {
                if (f <= 0.0F) {
                    flag = false;
                }
                this.brakeCount -= 2;
            } else if (this.brakeCount < 0) {
                this.brakeCount = 0;
            }
        }

        if (this.isControlCar() && flag && !this.world.isRemote) {
            ModelSetTrain modelsettrain = this.getResourceState().getResourceSet();
            float f1 = EnumNotch.getAcceleration(i, Math.abs(f), modelsettrain.getConfig(), this);
            byte dir = this.getVehicleState(TrainState.TrainStateType.Role);
            if ((dir == TrainState.Role_Back.data && f > 0) || (dir == TrainState.Role_Front.data && f < 0)) {
                f1 = Math.abs(f1);
            }
            if (dir == TrainState.Role_Back.data) {
                f1 *= -1;
            }
            if (i >= 0) {
                float f2;
                if (this.rotationPitch == 0.0F) {
                    float f3 = -modelsettrain.getConfig().deccelerations[0];
                    f2 = f > 0.0F ? f3 : (f < 0.0F ? -f3 : 0.0F);
                } else {
                    float f5 = 0.0125F;
                    float f4 = this.getTrainDirection() == 0 ? f5 : -f5;
                    f2 = NGTMath.sin(this.rotationPitch) * f4;
                }

                f = f + (f1 - f2);
            } else {
                f = f + f1;
            }

            this.setSpeed(f);
        }
    }

    @Override
    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
        this.setPosition(x, y, z);
        this.setRotation(yaw, pitch);
    }

    @Override
    protected void setRotation(float yaw, float pitch) {
        this.rotationYaw = NGTMath.wrapAngle(yaw);
        this.rotationPitch = NGTMath.wrapAngle(pitch);
    }

    @Override
    public void move(MoverType type, double x, double y, double z) {
        if (!this.onRail) {
            super.move(type, x, y, z);
        }
    }

    @Override
    public void addVelocity(double par1, double par3, double par5) {
    }

    public double getDefaultDistanceToConnectedTrain(EntityTrainBase par1) {
        ModelSetVehicleBase<TrainConfig> modelSet0 = this.getResourceState().getResourceSet();
        ModelSetVehicleBase<TrainConfig> modelSet1 = par1.getResourceState().getResourceSet();
        if (modelSet0 != null && modelSet1 != null) {
            double d0 = modelSet0.getConfig().trainDistance;
            double d1 = modelSet1.getConfig().trainDistance;
            return d0 + d1;
        }
        return 20.25D;
    }

    public boolean inConnectableRange(EntityTrainBase par1) {
        double d0 = this.getDefaultDistanceToConnectedTrain(par1);
        return this.getDistanceSq(par1) <= d0 * d0;
    }

    @Override
    public Vec3 getRiderPos(Entity passenger) {
        return super.getRiderPos(passenger).add(0.0D, this.getMountedYOffset(), 0.0D);
    }

    @Override
    protected int getRiderPosIndex() {
        return this.getCabDirection();
    }

    @Override
    public double getMountedYOffset() {
        return (double) (this.height + 1.1875F - 0.93F);
    }

    @Override
    public boolean attackEntityFrom(DamageSource par1, float par2) {
        if (this.isEntityInvulnerable(par1) || this.isDead) {
            return false;
        } else {
            if (!par1.isExplosion() && par1.getTrueSource() instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) par1.getTrueSource();
                if (!this.world.isRemote && PermissionManager.INSTANCE.hasPermission(player, RTMCore.EDIT_VEHICLE)) {
                    if (!player.capabilities.isCreativeMode) {
                        int damage = 0;
                        ModelSetTrain model = this.getResourceState().getResourceSet();
                        if (model != null) {
                            String type = model.getConfig().getSubType();
                            damage = type.equals("DC") ? 0 : (type.equals("EC") ? 1 : (type.equals("CC") ? 2 : 3));
                        }
                        this.entityDropItem(new ItemStack(RTMItem.itemtrain, 1, damage), 0.0F);
                    }
                    this.setDead();
                }
                return true;
            }
        }
        return false;
    }

    protected boolean interactTrain(EntityBogie bogie, EntityPlayer player) {
        if (this.getFirstPassenger() != null && !this.getFirstPassenger().equals(player)) {
            return true;
        }

        if (!this.world.isRemote) {
            ItemStack itemstack = player.inventory.getCurrentItem();
            if (!itemstack.isEmpty()) {
                if (itemstack.getItem() == RTMItem.crowbar) {
                    this.concatenation(bogie, player);
                } else if (itemstack.getItem() == RTMItem.itemMotorman) {
                    this.mountMotorman(bogie, player, itemstack);
                } else if (itemstack.getItem() == RTMItem.paddle) {
                    NGTLog.sendChatMessage(player, "UUID:" + this.getUniqueID().toString() + "(bogie, found train)");
                }
            } else {
                int id1 = bogie.getBogieId();
                if (id1 >= 0) {
                    this.mountEntityToTrain(player, id1);
                }
            }
        }
        return true;
    }

    protected void concatenation(EntityBogie bogie, EntityPlayer player) {
        int id1 = bogie.getBogieId();
        if (id1 >= 0) {
            if (this.getConnectedTrain(id1) == null) {
                bogie.isActivated = true;
                NGTLog.sendChatMessage(player, "message.train.concatenation_mode");
            } else {
                this.formation.onDisconnectedTrain(this, id1);
                NGTLog.sendChatMessage(player, "message.train.deconcatenation");
            }
        }
    }

    protected void mountMotorman(EntityBogie bogie, EntityPlayer player, ItemStack stack) {
        int id1 = bogie.getBogieId();
        if (id1 >= 0) {
            EntityMotorman motorman = new EntityMotorman(this.world, player);
            motorman.setLocationAndAngles(this.posX, this.posY, this.posZ, 0.0F, 0.0F);
            if (this.world.spawnEntity(motorman)) {
                this.mountEntityToTrain(motorman, id1);
                stack.shrink(1);
            }
        }
    }

    private void mountEntityToTrain(Entity entity, int direction) {
        if (this.isControlCar()) {
            this.setTrainDirection(direction);
            if (this.formation != null && this.formation.size() > 1) {
                byte data = this.getVehicleState(TrainState.TrainStateType.Role);
                byte newData = this.getCabDirection() == this.getTrainDirection() ? data : (byte) (data ^ 2);
                this.setTrainStateData_NoSync(TrainState.TrainStateType.Role, newData);
            }
        }

        setCabDirection(direction);
        entity.startRiding(this);
    }

    @Override
    protected void removePassengerFromVehicle(Entity passenger) {
        Entity entity = this.getBogie(this.getTrainDirection());
        if (entity == null || this.getDistanceSq(entity) > 15.0D * 15.0D) {
            entity = this;
        }
        this.fixRiderPosOnDismount(passenger, entity);
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (player.isSneaking()) {
            if (this.world.isRemote) {
                player.openGui(RTMCore.instance, RTMCore.guiIdSelectEntityModel, player.world, this.getEntityId(), 0, 0);
            }
            return true;
        }

        if (NGTUtil.isEquippedItem(player, RTMItem.paddle)) {
            if (!this.world.isRemote) {
                NGTLog.sendChatMessage(player, "UUID:" + this.getUniqueID().toString() + "(train)");
            }
            return true;
        }
        return false;
    }

    @Override
    public void updateResourceState() {
        jp.apple.replaymod.compat.ReplaySyncManager.syncModel(this, true);

        super.updateResourceState();
        this.updateTrainSize();
    }

    private void updateTrainSize() {
        TrainConfig cfg = this.getResourceState().getResourceSet().getConfig();
        float[][] bogiePos = cfg.getBogiePos();
        float hBogieZ = Math.abs(bogiePos[0][2] - bogiePos[1][2]) * 0.5F;
        float td2 = (float) (cfg.trainDistance / 3.0D * 2.0D);
        float width = hBogieZ < td2 ? hBogieZ : td2;
        if (width < TRAIN_WIDTH) {
            this.setSize(width, TRAIN_HEIGHT);
            if (this.existBogies()) {
                this.getBogie(0).setBogieSize(width, TRAIN_HEIGHT);
                this.getBogie(1).setBogieSize(width, TRAIN_HEIGHT);
            }
        }
    }

    public void connectTrain(EntityBogie par1, EntityBogie par2) {
        if (!this.world.isRemote && par2.getTrain() != null) {
            int i = par1.getBogieId();
            int j = par2.getBogieId();
            if (i >= 0 && j >= 0 && this.getConnectedTrain(i) == null && par2.getTrain().formation != null) {
                this.formation.connectTrain(this, par2.getTrain(), i, j, par2.getTrain().formation);
                RTMCore.proxy.playSound(par1, "block.anvil.place", 2.0F, 1.0F);
                EntityPlayer entityplayer = null;
                if (this.getFirstPassenger() instanceof EntityPlayer) {
                    entityplayer = (EntityPlayer) this.getFirstPassenger();
                } else if (par2.getTrain().getFirstPassenger() instanceof EntityPlayer) {
                    entityplayer = (EntityPlayer) par2.getTrain().getFirstPassenger();
                }

                if (entityplayer != null) {
                    NGTLog.sendChatMessage(entityplayer, "message.train.concatenated");
                }
            }
        }
    }

    @Override
    public float getSpeed() {
        return this.getDataManager().get(TRAIN_SPEED);
    }

    @Override
    public void setSpeed(float par1) {
        if (!this.world.isRemote && this.isControlCar() && this.formation != null) {
            this.formation.setSpeed(par1);
        }
    }

    public void setSpeed_NoSync(float par1) {
        this.getDataManager().set(TRAIN_SPEED, par1);
    }

    public void stopTrain(boolean changeSpeed) {
        if (this.formation != null) {
            this.setEmergencyBrake();
            if (changeSpeed) {
                this.setSpeed(0.0F);
            }
        }
    }

    public boolean isControlCar() {
        byte role = this.getVehicleState(TrainState.TrainStateType.Role);
        return role == TrainState.Role_Front.data || role == TrainState.Role_Back.data;
    }

    public boolean existBogies() {
        return this.getBogie(0) != null && this.getBogie(1) != null;
    }

    public int getBogieEntityId(int bogieId) {
        DataParameter<Integer> key = bogieId == 0 ? BOGIE_ID0 : BOGIE_ID1;
        return this.getDataManager().get(key);
    }

    public void setBogieEntityId(int bogieId, int entityId) {
        DataParameter<Integer> key = bogieId == 0 ? BOGIE_ID0 : BOGIE_ID1;
        this.getDataManager().set(key, Integer.valueOf(entityId));
    }

    public EntityBogie getBogie(int bogieId) {
        return this.bogieController.getBogie(this, bogieId);
    }

    public EntityTrainBase getConnectedTrain(int par1) {
        if (this.formation != null) {
            FormationEntry entry = this.formation.getEntry(this);
            if (entry == null) {
                return null;
            }
            int pos = entry.entryId;
            int dif = (par1 == 0) ? -1 : 1;
            if (entry.dir == 1) {
                dif *= -1;
            }
            pos += dif;
            if (pos < 0 || pos >= this.formation.size()) {
                return null;
            }
            FormationEntry connected = this.formation.get(pos);
            if (connected != null) {
                return connected.train;
            }
        }
        return null;
    }

    @Override
    public Formation getFormation() {
        return this.formation;
    }

    public void setFormation(Formation par1) {
        this.formation = par1;
    }

    public int getCabDirection() {
        return this.getDataManager().get(CAB_DIRECTION) ? 1 : 0;
    }

    private void setCabDirection(int direction) {
        this.getDataManager().set(CAB_DIRECTION, direction != 0);
    }

    public int getTrainDirection() {
        return this.getVehicleState(TrainState.TrainStateType.Direction);
    }

    public void setTrainDirection(int par1) {
        par1 = TrainState.TrainStateType.Direction.clap((byte) par1, this);
        if (this.formation == null) {
            this.setTrainDirection_NoSync((byte) par1);
        } else {
            this.formation.setTrainDirection((byte) par1, this);
        }
    }

    public void setTrainDirection_NoSync(byte par1) {
        super.setVehicleState(TrainStateType.Direction, par1);
        int id2 = 1 - par1;
        if (id2 < 2) {
            if (this.existBogies()) {
                this.getBogie(par1).setFront(true);
                this.getBogie(id2).setFront(false);
            }
        }
    }

    public int getNotch() {
        return this.getVehicleState(TrainStateType.Notch);
    }

    @SideOnly(Side.CLIENT)
    public void syncNotch(int notchInc) {
        RTMCore.NETWORK_WRAPPER.sendToServer(new PacketNotice(PacketNotice.Side_SERVER, "notch:" + notchInc, this));
        MacroRecorder.INSTANCE.recNotch(this.getEntityWorld(), notchInc);
    }

    public boolean addNotch(Entity driver, int par2) {
        if (par2 != 0) {
            int i = this.getNotch();
            if (this.setNotch(i + par2)) {
                RTMCore.proxy.playSound(driver, "rtm:sounds/train/lever.ogg", 1.0F, 1.0F);
                return true;
            }
        }

        return false;
    }

    public boolean setNotch(int par1) {
        if (this.isControlCar() && this.getResourceState().getResourceSet().getConfig().isNotchInRange(par1)) {
            int i = this.getNotch();
            if (i != par1) {
                super.setVehicleState(TrainState.TrainStateType.Notch, (byte) par1);

                if (i < 0 && par1 - i > 0 && !this.world.isRemote) {
                    java.util.Arrays.stream(getFormation().entries)
                            .filter(java.util.Objects::nonNull)
                            .map(entry -> entry.train)
                            .filter(java.util.Objects::nonNull)
                            .forEach(train -> train.playBrakeReleaseSound(par1 >= 0));
                }
                return true;
            }
        }

        return false;
    }

    public void setEmergencyBrake() {
        int prevNotch = this.getNotch();
        int EB_NOTCH = -(this.getResourceState().getResourceSet().getConfig().deccelerations.length - 1);
        if (prevNotch != EB_NOTCH) {
            this.setVehicleState(jp.ngt.rtm.entity.train.util.TrainState.TrainStateType.Notch, (byte) EB_NOTCH);
        }
    }

    public int getSignal() {
        return this.getVehicleState(TrainStateType.Signal);
    }

    public void setSignal(int par1) {
        int signal = this.getSignal();
        if (par1 > 0 && signal != -1) {
            this.setSignal2(par1);

            if (par1 == 1 && this.getSpeed() > 0.0F) {
                ++this.atsCount;
            }
        }
    }

    public void setSignal2(int par1) {
        if (par1 == -1) {
            this.atsCount = 0;
        }

        super.setVehicleState(TrainStateType.Signal, (byte) par1);
    }

    @Override
    public void setVehicleState(TrainStateType type, byte data) {
        byte b = data < type.min ? type.max : (data > type.max ? type.min : data);
        if (this.formation != null) {
            this.formation.setTrainStateData(type, b, this);
        }
    }

    public void setTrainStateData_NoSync(TrainStateType type, byte data) {
        super.setVehicleState(type, data);
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected boolean useInteriorLight() {
        return this.getVehicleState(TrainStateType.InteriorLight) > 0;
    }

    @Override
    public float getMoveDir() {
        int i = this.getTrainDirection();
        if (this.getBogie(i) != null) {
            boolean b0 = this.getBogie(i).isFront();
            return ((i == 0 && b0) || (i == 1 && !b0)) ? 1.0F : -1.0F;
        }
        return 1.0F;
    }

    @Override
    public float getVehicleYOffset() {
        return TRAIN_HEIGHT;
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return ItemTrain.convertFormationAsItem(this);
    }

    private Ticket ticket;
    private final Set<ChunkPos> loadedChunks = new java.util.LinkedHashSet<>();
    private int prevChunkCoordX = Integer.MIN_VALUE;
    private int prevChunkCoordZ = Integer.MIN_VALUE;
    private int prevChunkLoaderRadius = Integer.MIN_VALUE;

    private void updateChunks() {
        if (this.isChunkLoaderEnable()) {
            this.forceChunkLoading();
        } else {
            this.releaseTicket();
        }

        this.prevChunkCoordX = this.chunkCoordX;
        this.prevChunkCoordZ = this.chunkCoordZ;
        this.prevChunkLoaderRadius = this.getVehicleState(TrainState.TrainStateType.ChunkLoader);
    }

    @Override
    public boolean isChunkLoaderEnable() {
        return this.getVehicleState(TrainStateType.ChunkLoader) > 0;
    }

    private void releaseTicket() {
        this.loadedChunks.clear();
        if (this.ticket != null) {
            ForgeChunkManager.releaseTicket(this.ticket);
            this.ticket = null;
        }
    }

    private boolean requestTicket() {
        Ticket ticket = RTMChunkManager.INSTANCE.getNewTicket(this.world, Type.ENTITY);
        if (ticket != null) {
            ticket.getModData();
            ticket.bindEntity(this);
            this.setChunkTicket(ticket);
            return true;
        } else {
            NGTLog.debug("[RTM] Failed to get ticket (Chunk Loader)");
            return false;
        }
    }

    @Override
    public void setChunkTicket(Ticket par1) {
        if (this.ticket != par1) {
            ForgeChunkManager.releaseTicket(this.ticket);
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
            int chunkLoaderRadius = this.getVehicleState(TrainState.TrainStateType.ChunkLoader);
            boolean radiusChanged = chunkLoaderRadius != this.prevChunkLoaderRadius;
            boolean moved = x != this.prevChunkCoordX || z != this.prevChunkCoordZ;

            if (this.ticket == null && !this.requestTicket()) {
                return;
            }

            if (!radiusChanged && !moved && !this.loadedChunks.isEmpty()) {
                return;
            }

            this.setupChunks(x, z);

            int chunkLoadSquareRadius = chunkLoaderRadius * 2 + 1;
            this.ticket.setChunkListDepth(Math.min(chunkLoadSquareRadius * chunkLoadSquareRadius, ForgeChunkManager.getMaxChunkDepthFor("rtm")));

            for (ChunkPos chunkpos : this.loadedChunks) {
                ForgeChunkManager.forceChunk(this.ticket, chunkpos);
            }

            ChunkPos chunkpos1 = new ChunkPos(x, z);
            ForgeChunkManager.forceChunk(this.ticket, chunkpos1);
        }
    }

    private void setupChunks(int xChunk, int zChunk) {
        int rad = this.getVehicleState(TrainStateType.ChunkLoader);
        RTMChunkManager.INSTANCE.getChunksAround(this.loadedChunks, xChunk, zChunk, rad);
    }
    /**
     * ATS制動を開始する。ノッチ操作は無視され、targetSpeed に向かって
     * この車両の最大減速度で減速する
     */
    public void engageAtsBrake(float targetSpeed) {
        if (!this.isControlCar()) return;
        this.atsBraking = true;
        this.atsTargetSpeed = targetSpeed;
        this.atsDecelPerTick = computeMaxDecelPerTick();
    }
    /**
     * この車両の常用最大減速度を返す
     */
    private float computeMaxDecelPerTick() {
        try {
            ModelSetTrain ms = this.getResourceState().getResourceSet();
            if (ms == null || ms.getConfig() == null) return ATS_DECEL_FALLBACK;
            float[] decels = ms.getConfig().deccelerations;
            if (decels == null || decels.length == 0) return ATS_DECEL_FALLBACK;
            
            int index = decels.length > 1 ? decels.length - 2 : 0;
            float decel = Math.abs(decels[index]);
            if (decel <= 0.0F) return ATS_DECEL_FALLBACK;
            
            return decel;
        } catch (Exception e) {
            return ATS_DECEL_FALLBACK;
        }
    }
    /**
     * ATS制動を解除する。以降は通常のノッチ制御に戻る。
     */
    public void releaseAtsBrake() {
        if (!this.isControlCar()) return;
        this.atsBraking = false;
    }

    public boolean isAtsBraking() {
        return this.atsBraking;
    }
}
