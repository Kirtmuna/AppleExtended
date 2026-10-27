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

package jp.ngt.rtm.entity.vehicle;

import com.anatawa12.fixRtm.rtm.entity.vehicle.EntityVehicleBaseKt;
import jp.ngt.ngtlib.entity.EntityCustom;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.math.PooledVec3;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.ngtlib.network.PacketNBT;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.entity.train.parts.EntityFloor;
import jp.ngt.rtm.entity.train.util.Formation;
import jp.ngt.rtm.entity.train.util.TrainState;
import jp.ngt.rtm.entity.train.util.TrainState.TrainStateType;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.ScriptExecuter;
import jp.ngt.rtm.modelpack.cfg.VehicleBaseConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetBase;
import jp.ngt.rtm.modelpack.modelset.ModelSetVehicleBase;
import jp.ngt.rtm.modelpack.state.ResourceState;
import jp.ngt.rtm.network.PacketNotice;
import jp.ngt.rtm.network.PacketSetTrainState;
import jp.ngt.rtm.network.PacketVehicleMovement;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class EntityVehicleBase<T extends ModelSetVehicleBase> extends EntityCustom implements IResourceSelector {

    public static final int MAX_SEAT_ROTATION = 45;
    public static final int MAX_DOOR_MOVE = 60;
    public static final int MAX_PANTOGRAPH_MOVE = 40;
    public static final float TO_ANGULAR_VELOCITY = (float) (360.0D / Math.PI);
    private static final int RENDER_DELAY_TICKS = 6;
    private static final double DEFAULT_PER_TICK_ALPHA = 0.06D;

    protected int renderDelayTicks = RENDER_DELAY_TICKS;
    protected double perTickAlpha = DEFAULT_PER_TICK_ALPHA;

    private ResourceState<T> state = new ResourceState<>(this.getSubType(), this);
    private ScriptExecuter executer = new ScriptExecuter();

    protected List<EntityFloor> vehicleFloors = new ArrayList<>();
    protected final IUpdateVehicle soundUpdater;

    private boolean floorLoaded;
    /**
     * TrainTrackerEntryを生成したかどうか
     */
    private boolean tracked;
    private EntityLivingBase rider;

    public float rotationRoll;
    public float prevRotationRoll;

    @SideOnly(Side.CLIENT)
    public int seatRotation;
    @SideOnly(Side.CLIENT)
    public int rollsignAnimation;
    @SideOnly(Side.CLIENT)
    public int rollsignV;

    @SideOnly(Side.CLIENT)
    public int doorMoveL, doorMoveR;
    @SideOnly(Side.CLIENT)
    public int pantograph_F, pantograph_B;
    @SideOnly(Side.CLIENT)
    public float wheelRotationR, wheelRotationL;

    @SideOnly(Side.CLIENT)
    protected int vehiclePosRotationInc;
    @SideOnly(Side.CLIENT)
    protected double vehicleX, vehicleY, vehicleZ;
    @SideOnly(Side.CLIENT)
    protected float vehicleYaw, vehiclePitch, vehicleRoll;

    private static class Snapshot {
        final long tick;
        final double x, y, z;
        final float yaw, pitch, roll;

        Snapshot(long tick, double x, double y, double z, float yaw, float pitch, float roll) {
            this.tick = tick;
            this.x = x; this.y = y; this.z = z;
            this.yaw = yaw; this.pitch = pitch; this.roll = roll;
        }
    }

    private final ArrayDeque<Snapshot> snapshotBuffer = new ArrayDeque<>();
    private double targetOffset = Double.NaN;
    private double smoothedOffset = Double.NaN;

    public EntityVehicleBase(World world) {
        super(world);
        this.ignoreFrustumCheck = true;
        if (!NGTUtil.isServer()) {
            setRenderDistanceWeight(16.0D);
        }
        this.preventEntitySpawning = true;
        this.soundUpdater = world != null ? RTMCore.proxy.getSoundUpdater(this) : null;

        if (world.isRemote) {
            world.addWeatherEffect(new WeatherEffectDummy(world, this));
        }

        for (TrainStateType type : TrainStateType.values()) {
            this.setVehicleState(type, type.min);
        }
    }


    @Override
    protected void entityInit() {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setTag("State", this.getResourceState().writeToNBT());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        this.getResourceState().readFromNBT(nbt.getCompoundTag("State"));

        if (this.world != null && this.world.isRemote) {
            this.updateResourceState();
        }
    }


    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass >= 0;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity par1) {
		
	/*if(par1 instanceof EntityVehiclePart)
	{
		if(((EntityVehiclePart)par1).getVehicle() == this)
		{
			return null;
		}
	}
	else if(par1 instanceof EntityBogie)
	{
		if(((EntityBogie)par1).getTrain() == this)
		{
			return null;
		}
	}
	return par1.getEntityBoundingBox();*/
        return null;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox() {


        return null;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public void syncData() {
        this.updateResourceState();
    }

    public void setFloor(EntityFloor par1) {

        this.vehicleFloors.add(par1);
    }

    @Override
    public void setDead() {
        super.setDead();

        if (this.world.isRemote) {
            ;
        } else {
            for (EntityFloor floor : this.vehicleFloors) {
                if (floor != null) {
                    floor.setDead();
                }
            }


            RTMCore.NETWORK_WRAPPER.sendToAll(new PacketVehicleMovement(this, true));
        }
    }

    @Override
    public final void onUpdate() {
        this.onVehicleUpdate();
    }

    /**
     * Entity.onUpdate()を上書き
     */
    public double lastTickPosXPublic, lastTickPosYPublic, lastTickPosZPublic;

    protected void onVehicleUpdate() {
        super.onUpdate();

        this.prevRotationRoll = this.rotationRoll;

        if (this.world.isRemote) {
            if (this.ticksExisted == (60 + (this.getEntityId() % 20))) {
                String currentModel = this.getResourceState().getResourceName();
                String defaultModel = this.getSubType().defaultName;
                if (currentModel.equals(defaultModel)) {
                    RTMCore.NETWORK_WRAPPER.sendToServer(new PacketNotice(PacketNotice.Side_SERVER, "requestSync", this));
                }
            }
            if (this.soundUpdater != null) {
                this.soundUpdater.update();
            }

            this.updateAnimation();
            this.updatePosAndRotationClient();
        } else {
            if (!this.tracked) {
                this.tracked = VehicleTrackerEntry.trackingVehicle(this);
            }

            if (!this.floorLoaded) {
                this.setupFloors();
            }

            Entity passenger = this.getFirstPassenger();
            if (passenger != null) {
                if (this.rider == null && passenger instanceof EntityLivingBase) {
                    this.rider = (EntityLivingBase) passenger;
                }
            } else if (this.rider != null) {

                this.rider = null;
            }

            this.executer.execScript(this);

            this.updateBlockCollisionState();
            this.updateEntityCollisionState();

            this.updateFallState();
            this.updateMovement();
        }
    }

    /**
     * (Server Only)
     */
    protected void updateBlockCollisionState() {
    }

    /**
     * (Server Only)
     */
    protected void updateEntityCollisionState() {
    }

    protected void updateFallState() {
        this.motionY -= 0.05D;
    }

    protected void updateMovement() {
        this.applyPhysicalEffect();
        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
    }

    protected void applyPhysicalEffect() {
        this.motionX *= 0.9900000095367432D;
        this.motionY *= 0.949999988079071D;
        this.motionZ *= 0.9900000095367432D;
    }

    @SideOnly(Side.CLIENT)
    protected void updatePosAndRotationClient() {
        if (this.snapshotBuffer.isEmpty() || Double.isNaN(this.smoothedOffset)) {
            return;
        }

        if (!Double.isNaN(this.targetOffset)) {
            this.smoothedOffset += (this.targetOffset - this.smoothedOffset) * this.perTickAlpha;
        }

        double renderTickD = (double) this.ticksExisted + this.smoothedOffset - this.renderDelayTicks;
        long renderTick = (long) Math.floor(renderTickD);

        Snapshot before = null;
        Snapshot after = null;
        Snapshot[] snapshots;
        synchronized (this.snapshotBuffer) {
            snapshots = this.snapshotBuffer.toArray(new Snapshot[0]);
        }

        for (Snapshot s : snapshots) {
            if (s.tick <= renderTick) {
                before = s;
            } else if (after == null) {
                after = s;
                break;
            }
        }

        if (before != null && after != null && after.tick > before.tick) {
            float t = (float) ((renderTickD - before.tick) / (double) (after.tick - before.tick));
            t = MathHelper.clamp(t, 0.0F, 1.0F);

            this.posX = before.x + (after.x - before.x) * t;
            this.posY = before.y + (after.y - before.y) * t;
            this.posZ = before.z + (after.z - before.z) * t;
            this.rotationYaw = before.yaw + NGTMath.wrapAngle(after.yaw - before.yaw) * t;
            this.rotationPitch = before.pitch + (after.pitch - before.pitch) * t;
            this.rotationRoll = before.roll + (after.roll - before.roll) * t;
        } else if (before != null) {
            this.posX = before.x;
            this.posY = before.y;
            this.posZ = before.z;
            this.rotationYaw = before.yaw;
            this.rotationPitch = before.pitch;
            this.rotationRoll = before.roll;
        }

        this.setRotation(this.rotationYaw, this.rotationPitch);
        this.setPosition(this.posX, this.posY, this.posZ);
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return this.getPassengers().size() <= 0;
    }


    @Override
    public boolean canPassengerSteer() {
        return false;
    }

    @Override
    public Entity getControllingPassenger() {


        return null;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        this.removePassengerFromVehicle(passenger);
    }

    protected void removePassengerFromVehicle(Entity passenger) {
        this.fixRiderPosOnDismount(passenger, this);
    }

    /**
     * 降りたEntityがハマらないように位置修正する
     *
     */
    public void fixRiderPosOnDismount(Entity passenger, Entity ridingEntity) {
        AxisAlignedBB vehicleAABB = null;

        try {
            ModelSetBase set = this.getResourceState().getResourceSet();
            vehicleAABB = set.getCollisionObj().getSizeBox();
        } catch (NullPointerException e) {
        }

        if (vehicleAABB == null) {
            vehicleAABB = new AxisAlignedBB(-1.5D, 0.0D, -2.0D, 1.5D, 3.0D, 2.0D);
        }


        Vec3 vec = PooledVec3.create(ridingEntity.posX - this.posX, ridingEntity.posY - this.posY, ridingEntity.posZ - this.posZ);

        vec = vec.rotateAroundY(-this.rotationYaw);
        float yawDif = NGTMath.wrapAngle(-passenger.rotationYaw - this.rotationYaw);

        double fixX = (yawDif >= 0.0D) ? vehicleAABB.maxX + 0.5D : vehicleAABB.minX - 0.5D;
        vec = PooledVec3.create(fixX, vec.getY(), vec.getZ());
        vec = vec.rotateAroundY(this.rotationYaw);

        if (vec.length() <= 16.0D) {
            final double maxY = 5.0D;
            double x = this.posX + vec.getX();
            double y = this.posY + vec.getY();
            double z = this.posZ + vec.getZ();
            double newY = this.getDismountY(x, y, z, y + maxY);
            if (newY - y >= maxY) {
                vec = PooledVec3.create(-fixX, vec.getY(), vec.getZ());
                vec = vec.rotateAroundY(this.rotationYaw);
                x = this.posX + vec.getX();
                y = this.posY + vec.getY();
                z = this.posZ + vec.getZ();
                newY = this.getDismountY(x, y, z, y + maxY);
            }
            passenger.setPositionAndUpdate(x, y, z);
        }
    }

    private double getDismountY(double x, double y, double z, double maxY) {
        while (y < maxY) {
            if (this.world.isAirBlock(new BlockPos(x, y, z))) {
                break;
            }
            y += 1.0D;
        }
        return y;
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (this.isPassenger(passenger)) {
            Vec3 vec = this.getRiderPos(passenger);
            vec = vec.rotateAroundZ(-this.rotationRoll);
            vec = vec.rotateAroundX(this.rotationPitch);
            vec = vec.rotateAroundY(this.rotationYaw);
            double x = this.posX + vec.getX();
            double y = this.posY + vec.getY();
            double z = this.posZ + vec.getZ();
            passenger.setPosition(x, y, z);


            passenger.rotationYaw -= NGTMath.wrapAngle(this.rotationYaw - this.prevRotationYaw);
            passenger.rotationPitch -= NGTMath.wrapAngle(this.rotationPitch - this.prevRotationPitch);
        }
    }

    public Vec3 getRiderPos(Entity passenger) {
        float[] pos = this.getResourceState().getResourceSet().getConfig().getPlayerPos()[this.getRiderPosIndex()];
        return PooledVec3.create((double) pos[0], (double) pos[1] + passenger.getYOffset(), (double) pos[2]);
    }

    protected int getRiderPosIndex() {
        return 0;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void applyOrientationToEntity(Entity entity) {


    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        this.setPositionAndRotationDirect(x, y, z, yaw, pitch, posRotationIncrements, teleport, -1L);
    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int par6, boolean par10, long serverTick) {
        long tickKey = (serverTick >= 0) ? serverTick : this.ticksExisted;
        synchronized (this.snapshotBuffer) {
            this.snapshotBuffer.addLast(new Snapshot(tickKey, x, y, z, yaw, pitch, this.vehicleRoll));
            while (this.snapshotBuffer.size() > 16) {
                this.snapshotBuffer.removeFirst();
            }
        }

        this.targetOffset = (double) tickKey - (double) this.ticksExisted;
        if (Double.isNaN(this.smoothedOffset)) {
            this.smoothedOffset = this.targetOffset;
        }
    }

    @SideOnly(Side.CLIENT)
    public void setRollAndSpeed(float speed, float roll) {
        this.setSpeed(speed);
        this.vehicleRoll = roll;
    }

    public float getRoll() {
        return this.rotationRoll;
    }

    public abstract float getSpeed();

    public abstract void setSpeed(float par1);

    @SideOnly(Side.CLIENT)
    @Override
    public void setVelocity(double par1, double par3, double par5) {
    }

    @SideOnly(Side.CLIENT)
    protected void updateAnimation() {
        ModelSetVehicleBase modelSet = this.getResourceState().getResourceSet();
        VehicleBaseConfig cfg = modelSet.getConfig();
        float speed = this.getSpeed();
        float f0 = speed * TO_ANGULAR_VELOCITY * cfg.wheelRotationSpeed * this.getMoveDir();

        this.wheelRotationR = (this.wheelRotationR + f0) % 360.0F;
        this.wheelRotationL = (this.wheelRotationL + f0) % 360.0F;


        int doorState = this.getVehicleState(TrainStateType.Door);
        boolean doorROpen = (doorState & 1) == 1;
        this.doorMoveR = this.updateDoorAnimation(modelSet, doorROpen, this.doorMoveR);
        boolean doorLOpen = (doorState & 2) == 2;
        this.doorMoveL = this.updateDoorAnimation(modelSet, doorLOpen, this.doorMoveL);


        int pantoState = this.getVehicleState(TrainStateType.Pantograph);

        if (pantoState == TrainState.Pantograph_Down.data) {
            if (this.pantograph_F < MAX_PANTOGRAPH_MOVE) {
                ++this.pantograph_F;
            }
            if (this.pantograph_B < MAX_PANTOGRAPH_MOVE) {
                ++this.pantograph_B;
            }
        } else {
            int[] ia = this.getPantographMaxHeight();

            if (this.pantograph_F > ia[0]) {
                --this.pantograph_F;
            } else if (this.pantograph_F < ia[0]) {
                ++this.pantograph_F;
            }

            if (this.pantograph_B > ia[1]) {
                --this.pantograph_B;
            } else if (this.pantograph_B < ia[1]) {
                ++this.pantograph_B;
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public float getRollsignAnimation() {
        return (float) this.rollsignAnimation / 16.0F;
    }

    @SideOnly(Side.CLIENT)
    public void setRollsignAnimation(int par1) {
        this.rollsignV = par1 * 16;
    }

    /**
     * -1.0~1.0
     */
    @SideOnly(Side.CLIENT)
    public float getSeatRotation() {
        return (float) this.seatRotation / MAX_SEAT_ROTATION;
    }

    protected int[] getPantographMaxHeight() {
        return new int[]{0, 0};
    }

    protected int updateDoorAnimation(ModelSetVehicleBase modelSet, boolean isOpen, int move) {
        if (isOpen) {
            if (move < MAX_DOOR_MOVE) {
                if (move == 0) {
                    RTMCore.proxy.playSound(this, modelSet.getConfig().sound_DoorOpen, 1.0F, 1.0F);
                }
                ++move;
            }
        } else {
            if (move > 0) {
                if (move == MAX_DOOR_MOVE) {
                    RTMCore.proxy.playSound(this, modelSet.getConfig().sound_DoorClose, 1.0F, 1.0F);
                }
                --move;
            }
        }
        return move;
    }

    /**
     * +1.0 or -1.0
     */
    public float getMoveDir() {
        return 1.0F;
    }

    /**
     * Server Only
     */
    private void setupFloors() {
        for (EntityFloor entity : this.vehicleFloors) {
            if (entity != null) {
                entity.setDead();
            }
        }

        T set = this.getResourceState().getResourceSet();
        this.floorLoaded = true;
        for (int i = 0; i < set.getConfig().getSlotPos().length; ++i) {
            float[] fa = set.getConfig().getSlotPos()[i];
            EntityFloor floor = new EntityFloor(this.world, this, new float[]{fa[0], fa[1] + this.getVehicleYOffset(), fa[2]}, (byte) fa[3]);
            if (this.world.spawnEntity(floor)) {
                this.vehicleFloors.add(floor);
            } else {
                this.floorLoaded = false;
                break;
            }
        }
    }

    @Override
    public String getName() {
        return this.getResourceState().getName();
    }

    @Override
    public ResourceState<T> getResourceState() {
        return this.state;
    }

    @Override
    public void updateResourceState() {
        if (this.world == null || !this.world.isRemote) {
            PacketNBT.sendToClient(this);
            this.floorLoaded = false;
        }

        if (this.world.isRemote) {
            this.soundUpdater.onModelChanged();
        }
    }

    @Override
    public int[] getSelectorPos() {
        return new int[]{this.getEntityId(), -1, 0};
    }

    @Override
    public boolean closeGui(ResourceState par1) {
        return true;
    }

    protected abstract ResourceType getSubType();

    @SideOnly(Side.CLIENT)
    protected boolean useInteriorLight() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getBrightnessForRender() {
	/*if(this.shouldUseInteriorLight())
	{
		return 0xF000F0;
	}*/
        return super.getBrightnessForRender();
    }

    /**
     * 描画位置、座席位置に使用
     */
    public float getVehicleYOffset() {
        return 0.0F;
    }


    public Formation getFormation() {
        return null;
    }


    @Deprecated
    public byte getTrainStateData(int id) {
        return this.getVehicleState(TrainState.getStateType(id));
    }

    @Deprecated
    public void setTrainStateData(int id, byte data) {
        this.setVehicleState(TrainState.getStateType(id), data);
    }

    public byte getVehicleState(TrainStateType type) {
        return type.clap((byte) this.getResourceState().getDataMap().getInt(type.toString()), this);
    }

    public void setVehicleState(TrainStateType type, byte data) {
        this.getResourceState().getDataMap().setInt(type.toString(), type.clap(data, this), 3);
    }


    @SideOnly(Side.CLIENT)
    public void syncVehicleState(TrainStateType type, byte data) {
        RTMCore.NETWORK_WRAPPER.sendToServer(new PacketSetTrainState(this, type, data));
    }

    public VehicleNGTO getNGTO() {
        return null;
    }

    @Override
    public void onRemovedFromWorld() {
        this.removeWeatherEffectDummy();
        super.onRemovedFromWorld();
        EntityVehicleBaseKt.onRemovedFromWorld(this);
    }

    private void removeWeatherEffectDummy() {
        if (this.world == null || !this.world.isRemote) {
            return;
        }

        Iterator<Entity> iterator = this.world.weatherEffects.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity instanceof WeatherEffectDummy && ((WeatherEffectDummy) entity).isLinkedTo(this)) {
                entity.setDead();
                iterator.remove();
            }
        }
    }

    @Override
    public void addEntityCrashInfo(CrashReportCategory category) {
        super.addEntityCrashInfo(category);
        EntityVehicleBaseKt.addEntityCrashInfo(this, category);
    }
}
