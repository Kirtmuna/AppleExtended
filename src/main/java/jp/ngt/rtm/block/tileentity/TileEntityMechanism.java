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

package jp.ngt.rtm.block.tileentity;

import com.anatawa12.fixRtm.rtm.block.tileentity.TileEntityMechanismKt;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.block.TileEntityCustom;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.math.Mat3;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.entity.vehicle.EntityLift;
import jp.ngt.rtm.entity.vehicle.LiftMotion;
import jp.ngt.rtm.item.ItemWithModel;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.cfg.MechanismConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetMechanism;
import jp.ngt.rtm.modelpack.modelset.ModelSetWire;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityMechanism extends TileEntityCustom implements IResourceSelector, ITickable {
    private static TileEntityMechanism SELECT_TARGET;

    private ResourceState<ModelSetMechanism> state = new ResourceState<>(RTMResource.MECHANISM, this);
    private ResourceState<ModelSetWire> wireState = new ResourceState<>(RTMResource.WIRE, this);
    /**
     * 取り付け面(EnumFacing)
     */
    protected EnumFacing side = EnumFacing.UP;
    /**
     * 回転角度(0~3)
     */
    protected byte dir;
    private BlockPos pulleySource;
    public boolean invertWirePos;

    /**
     * [yp, zp, xn, zn, xp]<br>
     * 回転未適用<br>
     * 1~4はEnumFacing.NSWEと同じindex
     *
     */
    protected final float[] outputSpeeds = new float[5];
    /**
     * rpm
     */
    protected float inputSpeed;
    /**
     * [0:変換前→変換後, 1:変換後→変換前][facingId]
     */
    protected final EnumFacing[][] indexConverter = new EnumFacing[2][6];
    public boolean isPowered;
    /**
     * ギア/プーリーの現在の入力源
     */
    protected BlockPos inputSource;
    private boolean needUpdate = true;
    /**
     * 滑車中央からの相対位置
     */
    public Vec3 wirePosDst = Vec3.ZERO, wirePosSrc = Vec3.ZERO, wirePosDstOpposite = Vec3.ZERO;
    private TileEntityMechanism dstMecha, srcMecha;
    public float totalDistance, distanceOffset;
    public float wirePosAngle;

    /**
     * Client Only, Rendererで更新
     */
    public final float[] rotations = new float[6];
    /**
     * Client Only, Rendererで更新
     */
    public long prevTime;
    /**
     * Client Only, Rendererで更新
     */
    public float wireMove = 0.0F;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.state.readFromNBT(nbt.getCompoundTag("state"));
        this.side = EnumFacing.getFront(nbt.getByte("side"));
        this.dir = nbt.getByte("dir");
        boolean isPulley = (this.getType() == MechanismType.PULLEY);
        if (isPulley) {
            int[] pos = nbt.getIntArray("source");
            if (pos.length == 3) {
                this.pulleySource = new BlockPos(pos[0], pos[1], pos[2]);
            }
            this.invertWirePos = nbt.getBoolean("invert");
            this.wireState.readFromNBT(nbt.getCompoundTag("wire_state"));
        }

        this.needUpdate = true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setTag("state", this.state.writeToNBT());
        nbt.setByte("side", (byte) this.side.getIndex());
        nbt.setByte("dir", this.dir);
        if (this.getType() == MechanismType.PULLEY) {
            if (this.pulleySource != null) {
                int[] pos = {this.pulleySource.getX(), this.pulleySource.getY(), this.pulleySource.getZ()};
                nbt.setIntArray("source", pos);
            }
            nbt.setBoolean("invert", this.invertWirePos);
            nbt.setTag("wire_state", this.wireState.writeToNBT());
        }
        return nbt;
    }

    @Override
    public void update() {
        if (this.needUpdate) {
            this.calcIndex();
            this.needUpdate = false;
            if (this.getType() == MechanismType.PULLEY) {
                this.needUpdate = !this.calcWirePos();//接続先チャンク未読み込みを想定
            }
        }

        this.updateSpeed();
    }

    private void updateSpeed() {
        MechanismConfig cfg = this.getResourceState().getResourceSet().getConfig();
        MechanismType type = this.getType();
        boolean powered = false;
        if (cfg.useRedstonePower) {
            powered = this.world.isBlockIndirectlyGettingPowered(this.getPos()) > 0;
        }
        this.isPowered = powered;

        this.inputSpeed = this.getInput(type);

        this.setOutputs(type, this.inputSpeed, powered);
    }

    private float getInput(MechanismType type) {
        MechanismConfig cfg = this.getResourceState().getResourceSet().getConfig();

        if (type == MechanismType.POWER) {
            return cfg.maxSpeed;
        } else if (type == MechanismType.GEAR) {
            if (cfg.radius > 0.0F)//シャフト(r=0)を除く
            {
                float speed = this.getGearInput();
                if (speed != 0.0F) {
                    return speed;
                }
            }
        } else if (type == MechanismType.PULLEY) {
            if (cfg.radius > 0.0F)//シャフト(r=0)を除く
            {
                float speed = this.getPulleyInput();
                if (speed != 0.0F) {
                    return speed;
                }
            }
        }
        return this.getBackInput();
    }

    /**
     * 入力軸からの回転入力
     */
    private float getBackInput() {
        BlockPos pos = this.getPos();
        pos = pos.offset(this.side.getOpposite());
        TileEntity tileEntity = this.getWorld().getTileEntity(pos);
        if (tileEntity instanceof TileEntityMechanism) {
            return ((TileEntityMechanism) tileEntity).getOutput(this.side);
        }
        return 0.0F;
    }

    /**
     * 側面からの回転入力(ギア)
     */
    private float getGearInput() {
        BlockPos pos = this.getPos();
        this.inputSource = null;
        for (int i = 0; i < 4; ++i) {
            EnumFacing facing = this.indexConverter[0][i + 2];//NSWEの現在の面
            BlockPos sidePos = pos.offset(facing);
            TileEntity tileEntity = this.getWorld().getTileEntity(sidePos);
            if (tileEntity instanceof TileEntityMechanism) {
                TileEntityMechanism sideMecha = (TileEntityMechanism) tileEntity;
                MechanismType sideType = this.getType();
                if (sideType != MechanismType.GEAR) {
                    continue;//隣接ブロックがギアでない
                }

                if (sideMecha.inputSource != null && sideMecha.inputSource.equals(pos)) {
                    continue;//このブロックが隣接ブロックの入力源の場合
                }

                if (this.side.getAxis() != sideMecha.side.getAxis()) {
                    continue;//軸が異なる
                }

                float sideOutput = sideMecha.getOutputRaw(1);
                if (sideOutput != 0.0F) {
                    float ratio = this.calcGearRatio(sideMecha, this);
                    if (ratio > 0.0F) {
                        this.inputSource = sidePos;
                        return -sideOutput * ratio;
                    }
                }
            }
        }
        return 0.0F;
    }

    /**
     * ワイヤーからの回転入力(プーリー)
     */
    private float getPulleyInput() {
        this.inputSource = null;
        if (this.pulleySource != null && !this.isConnectedToPlayer()) {
            TileEntity tileEntity = this.getWorld().getTileEntity(this.pulleySource);
            if (tileEntity instanceof TileEntityMechanism) {
                TileEntityMechanism source = (TileEntityMechanism) tileEntity;
                if (source.inputSource != null && source.inputSource.equals(this.getPos())) {
                    return 0.0F;//このブロックが隣接ブロックの入力源の場合
                }

                float speed = source.getOutputRaw(1);
                if (speed != 0.0F) {
                    float ratio = this.calcGearRatio(source, this);
                    if (ratio > 0.0F) {
                        this.inputSource = this.pulleySource;
                        return speed * ratio;
                    }
                }
            }
        }
        return 0.0F;
    }

    private float calcGearRatio(TileEntityMechanism src, TileEntityMechanism dst) {
        float srcR = src.getResourceState().getResourceSet().getConfig().radius;
        float dstR = dst.getResourceState().getResourceSet().getConfig().radius;
        return (srcR > 0.0F && dstR > 0.0F) ? (srcR / dstR) : 0.0F;
    }

    private void setOutputs(MechanismType type, float speed, boolean powered) {
        MechanismConfig cfg = this.getResourceState().getResourceSet().getConfig();
        float[] ratio = powered ? cfg.transmissionRatioON : cfg.transmissionRatioOFF;
        for (int i = 0; i < this.outputSpeeds.length; ++i) {
            float output;
            if (type == MechanismType.POWER) {
                float target = speed * ratio[i];
                float acceleration = cfg.acceleration;
                float currentSpeed = this.outputSpeeds[i];
                if (currentSpeed < target) {
                    currentSpeed += acceleration;
                    if (currentSpeed > target) {
                        currentSpeed = target;
                    }
                } else if (currentSpeed > target) {
                    currentSpeed -= acceleration;
                    if (currentSpeed < target) {
                        currentSpeed = target;
                    }
                }
                output = currentSpeed;
            } else {
                output = speed * ratio[i];

                if (this.invertWirePos) {
                    output *= -1.0F;
                }
            }
            this.outputSpeeds[i] = output;
        }
    }

    public float getOutput(EnumFacing facing) {
        int index = this.convertFace(facing).getIndex();
        return this.getOutputRaw(index);
    }

    public float getOutputRaw(int index) {
        float speed = 0.0F;
        switch (index) {
            case 0:
                speed = this.inputSpeed;
                break;
            case 1:
                speed = this.outputSpeeds[0];
                break;
            case 2:
                speed = this.outputSpeeds[1 + 2];
                break;
            case 3:
                speed = this.outputSpeeds[1 + 0];
                break;
            case 4:
                speed = this.outputSpeeds[1 + 1];
                break;
            case 5:
                speed = this.outputSpeeds[1 + 3];
                break;
        }
        return speed;
    }

    public EnumFacing convertFace(EnumFacing face) {
        EnumFacing facing = this.indexConverter[1][face.getIndex()];
        return (facing == null) ? EnumFacing.UP : facing;
    }

    private void calcIndex() {
        Mat3 mat = Mat3.IDENTITY;

        switch (this.side) {
            case DOWN:
                mat = mat.rotateAroundX(180.0F);
                break;
            case UP:
                break;
            case NORTH:
                mat = mat.rotateAroundX(90.0F);
                mat = mat.rotateAroundZ(180.0F);
                break;
            case SOUTH:
                mat = mat.rotateAroundX(90.0F);
                break;
            case WEST:
                mat = mat.rotateAroundX(90.0F);
                mat = mat.rotateAroundZ(90.0F);
                break;
            case EAST:
                mat = mat.rotateAroundX(90.0F);
                mat = mat.rotateAroundZ(-90.0F);
                break;
        }

        mat = mat.rotateAroundY(this.dir * 90.0F);

        for (int i = 0; i < 6; ++i) {
            Vec3 vec = null;
            switch (i) {
                case 0:
                case 1:
                    vec = mat.getY();
                    break;
                case 2:
                case 3:
                    vec = mat.getZ();
                    break;
                case 4:
                case 5:
                    vec = mat.getX();
                    break;
            }
            EnumFacing facing = EnumFacing.getFacingFromVector((float) vec.getX(), (float) vec.getY(), (float) vec.getZ());
            if (i % 2 == 0) {
                facing = facing.getOpposite();
            }
            this.indexConverter[0][i] = facing;
            this.indexConverter[1][facing.getIndex()] = EnumFacing.getFront(i);
        }
    }

    private boolean calcWirePos() {
        if (this.isConnectedToPlayer() || (this.pulleySource == null)) {
            return true;
        }

        TileEntity tileEntity = this.getWorld().getTileEntity(this.pulleySource);
        if (!(tileEntity instanceof TileEntityMechanism)) {
            return false;
        }
        TileEntityMechanism source = (TileEntityMechanism) tileEntity;

        EnumFacing dstSide = this.side;
        EnumFacing srcSide = source.side;
        Vec3 dstNormal = this.getPulleyNormal();
        Vec3 srcNormal = source.getPulleyNormal();
        float dstR = this.getResourceState().getResourceSet().getConfig().radius;
        float srcR = source.getResourceState().getResourceSet().getConfig().radius;
        double dx = source.getX() - this.getX();
        double dy = source.getY() - this.getY();
        double dz = source.getZ() - this.getZ();
        Vec3 srcPos = new Vec3(source.getX() - this.getX(), source.getY() - this.getY(), source.getZ() - this.getZ());
        float dstInvert = this.invertWirePos ? 1.0F : -1.0F;
        float srcInvert = source.invertWirePos ? 1.0F : -1.0F;

        if (dstSide.getAxis() == srcSide.getAxis())//両滑車の法線が並行
        {
            Vec3 v0 = new Vec3(srcPos.getX() * dstNormal.getX(), srcPos.getY() * dstNormal.getY(), srcPos.getZ() * dstNormal.getZ());
            Vec3 srcPosFix = srcPos.sub(v0);
            double len0 = srcPosFix.length();//法線成分を除いた長さ
            float ang0 = (float) NGTMath.toDegrees(Math.acos((dstR - srcR) / len0));//法線方向から見たときのsrc円中心・終点のなす角
            Vec3 spfNrm = srcPosFix.normalize();
            Vec3 dstRV = spfNrm.rotateAroundVec(dstNormal, ang0 * dstInvert).multi(dstR);
            Vec3 srcRV = spfNrm.rotateAroundVec(srcNormal, ang0 * srcInvert).multi(srcR);

            this.wirePosDst = dstRV;
            this.wirePosSrc = srcRV.add(srcPos);
            source.wirePosDstOpposite = srcRV;
        } else//両滑車の法線がねじれ
        {
            Vec3 v0 = new Vec3(srcPos.getX() * dstNormal.getX(), srcPos.getY() * dstNormal.getY(), srcPos.getZ() * dstNormal.getZ());
            Vec3 srcPosFix = srcPos.sub(v0);
            double len0 = srcPosFix.length();//法線成分を除いた長さ
            float ang0 = (float) NGTMath.toDegrees(Math.acos(dstR / len0));
            Vec3 spfNrm = srcPosFix.normalize();
            Vec3 dstRV = spfNrm.rotateAroundVec(dstNormal, ang0 * dstInvert).multi(dstR);

            Vec3 v1 = new Vec3(srcPos.getX() * srcNormal.getX(), srcPos.getY() * srcNormal.getY(), srcPos.getZ() * srcNormal.getZ());
            Vec3 dstPosFix = srcPos.sub(v1);
            double len1 = dstPosFix.length();//法線成分を除いた長さ
            float ang1 = (float) NGTMath.toDegrees(Math.acos(srcR / len1));
            Vec3 dpfNrm = dstPosFix.normalize();
            Vec3 srcRV = dpfNrm.rotateAroundVec(srcNormal, ang1 * srcInvert).multi(srcR);

            this.wirePosDst = dstRV;
            this.wirePosSrc = srcRV.add(srcPos);
            source.wirePosDstOpposite = srcRV;
        }

        double circumference = 2.0D * Math.PI * this.getResourceState().getResourceSet().getConfig().radius;
        float totalAngle = this.wirePosDst.getAngle360(this.wirePosDstOpposite, this.getPulleyNormal()) + 180.0F;
        double len0 = circumference * (Math.abs(totalAngle) / 360.0F);
        double len1 = this.wirePosDst.sub(this.wirePosSrc).length();
        this.distanceOffset = (float) (-len1);
        this.totalDistance = (float) (len0 + len1);

        this.srcMecha = source;
        source.dstMecha = this;

        EnumFacing faceZ = this.indexConverter[0][EnumFacing.SOUTH.getIndex()];
        Vec3 vecZ = new Vec3(faceZ.getFrontOffsetX(), faceZ.getFrontOffsetY(), faceZ.getFrontOffsetZ());
        this.wirePosAngle = vecZ.getAngle360(this.wirePosDst, this.getPulleyNormal());

        return true;
    }

    private Vec3 normal;

    public Vec3 getPulleyNormal() {
        if (this.normal == null) {
            this.normal = new Vec3(this.side.getFrontOffsetX(), this.side.getFrontOffsetY(), this.side.getFrontOffsetZ());
        }
        return this.normal;
    }

    public boolean isConnectedToPlayer() {
        return (this.pulleySource != null && this.pulleySource.getY() == 0);
    }

    /**
     * ブロック右クリック
     */
    public void onRightClick(EntityPlayer player) {
        if (!player.getEntityWorld().isRemote) {
            Item heldItem = player.getHeldItemMainhand().getItem();
			/*if(heldItem == RTMItem.crowbar)//debug
			{
				int[] ia = this.indexConverter[0];
				NGTLog.sendChatMessageToAll(
						String.format("0->%d 1->%d 2->%d 3->%d 4->%d 5->%d", ia[0], ia[1], ia[2], ia[3], ia[4], ia[5]));
				NGTLog.sendChatMessageToAll(String.format("in:%.1f, out:%.1f", this.inputSpeed, this.outputSpeeds[0]));
			}*/
            if (heldItem == RTMItem.itemWire && this.getType() == MechanismType.PULLEY) {
                if (SELECT_TARGET == null) {
                    SELECT_TARGET = this;
                    this.setInputSource(new BlockPos(player.getEntityId(), 0, 0), player.getHeldItemMainhand());
                    NGTLog.sendChatMessageToAll("Select target pulley.");
                } else {
                    SELECT_TARGET.setInputSource(this.getPos(), player.getHeldItemMainhand());
                    NGTLog.sendChatMessageToAll(String.format("Set target pulley (%s)", SELECT_TARGET.pulleySource.toString()));
                    SELECT_TARGET = null;
                }
            } else {
                if (this.getType() == MechanismType.PULLEY) {
                    this.invertWirePos ^= true;
                    if (this.dstMecha != null) {
                        this.dstMecha.sendPacket();
                    }

                    if (this.srcMecha != null) {
                        this.srcMecha.sendPacket();
                    }
                } else {
                    this.dir = (byte) ((this.dir + 1) % 4);
                }
                this.needUpdate = true;
                this.sendPacket();
            }
        }
    }

    private void setInputSource(BlockPos pos, ItemStack item) {
        ResourceState state = ((ItemWithModel) item.getItem()).getModelState(item);
        this.wireState.readFromNBT(state.writeToNBT());
        this.pulleySource = pos;
        this.needUpdate = true;
        this.sendPacket();
    }

    public BlockPos getPulleySource() {
        return this.pulleySource;
    }

    /**
     * ブロック設置
     */
    public void setSide(int par1) {
        this.side = EnumFacing.getFront(par1);
        this.needUpdate = true;
    }

    public LiftMotion getMotion(EntityLift lift, double moveDistance, int timeDif) {
        double moveR = this.getOutputRaw(0) * ((double) timeDif / (1000.0D * 60.0D));
        double circumference = 2.0D * Math.PI * this.getResourceState().getResourceSet().getConfig().radius;
        moveDistance -= moveR * circumference;//反転

        Vec3 pos;
        float yaw, pitch;
        if (moveDistance < 0.0D)//直線区間
        {
            if (moveDistance < this.distanceOffset)//伝達元滑車へ移動
            {
                if (this.srcMecha != null && this.srcMecha != this) {
                    double move2 = (this.srcMecha.totalDistance + this.srcMecha.distanceOffset) + (moveDistance - this.distanceOffset);
                    return this.srcMecha.getMotion(lift, move2, 0);
                }
            }

            double moveAbs = moveDistance - this.distanceOffset;
            Vec3 moveVec = this.wirePosDst.sub(this.wirePosSrc).multi(moveAbs / -this.distanceOffset);
            pos = moveVec.add(this.wirePosSrc);
            yaw = moveVec.getYaw();
            pitch = moveVec.getPitch();
        } else if (moveDistance > 0.0D)//回転区間
        {
            double arcLen = this.totalDistance + this.distanceOffset;
            if (moveDistance > arcLen)//伝達先滑車へ移動
            {
                if (this.dstMecha != null && this.dstMecha != this) {
                    double move2 = this.dstMecha.distanceOffset + (moveDistance - arcLen);
                    return this.dstMecha.getMotion(lift, move2, 0);
                }
            }

            double moveAngle = 360.0D * (moveDistance / circumference);
            pos = this.wirePosDst.rotateAroundVec(this.getPulleyNormal(), (float) -moveAngle);
            Vec3 moveVec = pos.rotateAroundVec(this.getPulleyNormal(), -90.0F);
            yaw = moveVec.getYaw();
            pitch = moveVec.getPitch();
        } else {
            pos = this.wirePosDst;
            Vec3 moveVec = pos.rotateAroundVec(this.getPulleyNormal(), -90.0F);
            yaw = moveVec.getYaw();
            pitch = moveVec.getPitch();
        }

        float[] gripPos = lift.getResourceState().getResourceSet().getConfig().gripPos;
        pos = pos.add(this.getX() + 0.5D - gripPos[0], this.getY() + 0.5D - gripPos[1], this.getZ() + 0.5D - gripPos[2]);

        return new LiftMotion(pos, yaw, pitch, (float) moveDistance, this);
    }

    public ResourceState<ModelSetWire> getWireState() {
        return this.wireState;
    }

    public MechanismType getType() {
        return this.getResourceState().getResourceSet().getConfig().type;
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass >= 0;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public ResourceState<ModelSetMechanism> getResourceState() {
        return this.state;
    }

    @Override
    public void updateResourceState() {
        if (this.world == null || !this.world.isRemote) {
            this.sendPacket();
            this.markDirty();
            BlockUtil.markBlockForUpdate(this.getWorld(), this.getPos());
        }
    }

    @Override
    public int[] getSelectorPos() {
        return new int[]{this.getPos().getX(), this.getPos().getY(), this.getPos().getZ()};
    }

    @Override
    public boolean closeGui(ResourceState par1) {
        return true;
    }

    @Override
    public void addInfoToCrashReport(CrashReportCategory reportCategory) {
        super.addInfoToCrashReport(reportCategory);
        TileEntityMechanismKt.addInfoToCrashReport(this, reportCategory);
    }
}