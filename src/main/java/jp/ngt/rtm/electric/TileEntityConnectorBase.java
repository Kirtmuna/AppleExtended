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

package jp.ngt.rtm.electric;

import com.anatawa12.fixRtm.rtm.electric.TileEntityConnectorBaseKt;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.math.PooledVec3;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.cfg.ConnectorConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetConnector;
import jp.ngt.rtm.modelpack.modelset.ResourceSet;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class TileEntityConnectorBase extends TileEntityElectricalWiring implements IResourceSelector {
    private ResourceState<ModelSetConnector> state = new ResourceState<>(this.getSubType(), this);
    private Vec3 wirePos = Vec3.ZERO;
    /**
     * モデルの更新確認用
     */
    private String modelName = "";

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.getResourceState().readFromNBT(nbt.getCompoundTag("State"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setTag("State", this.getResourceState().writeToNBT());
        return nbt;
    }

    @Override
    public void update() {
        super.update();

        this.checkWireUpdate();
    }

    private final void checkWireUpdate() {
        ResourceSet set = this.getResourceState().getResourceSet();
        if (!set.isDummy() && !set.getConfig().getName().equals(this.modelName)) {
            this.updateWirePos();//Clientで更新するすべがここしかないため
            this.modelName = set.getConfig().getName();
        }
    }

    public Vec3 getWirePos() {
        return this.wirePos;
    }

    //元はgetModelSet()内で更新
    public void updateWirePos() {
        ConnectorConfig cfg = this.getResourceState().getResourceSet().getConfig();
        float s = this.getScale();
        Vec3 vec = PooledVec3.create(cfg.wirePos[0] * s, cfg.wirePos[1] * s, cfg.wirePos[2] * s);
        int meta = this.getBlockMetadata();
        switch (meta) {
            case 0:
                vec = vec.rotateAroundZ(180.0F);
                break;
            case 1:
                break;
            case 2://Z
                vec = vec.rotateAroundX(-90.0F);
                vec = vec.rotateAroundY(180.0F);
                break;
            case 3://Z
                vec = vec.rotateAroundX(-90.0F);
                break;
            case 4://X
                vec = vec.rotateAroundX(-90.0F);
                vec = vec.rotateAroundY(-90.0F);
                break;
            case 5://X
                vec = vec.rotateAroundX(-90.0F);
                vec = vec.rotateAroundY(90.0F);
                break;
        }
        if (this.getRotationX() != 0.0F) vec = vec.rotateAroundX(this.getRotationX());
        if (this.getRotationZ() != 0.0F) vec = vec.rotateAroundZ(this.getRotationZ());

        vec = vec.rotateAroundY(this.getRotation());
        vec = vec.add(this.getOffsetX(), this.getOffsetY(), this.getOffsetZ());

        this.wirePos = vec;
    }

    @Override
    public int[] getSelectorPos() {
        return new int[]{this.getX(), this.getY(), this.getZ()};
    }

    @Override
    public boolean closeGui(ResourceState par1) {
        this.updateResourceState();
        return true;
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass >= 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        final int rangeXZ = 32;
        final int rangeY = 16;
        AxisAlignedBB bb = new AxisAlignedBB(this.getX() - rangeXZ, this.getY() - rangeY, this.getZ() - rangeXZ,
                this.getX() + rangeXZ, this.getY() + rangeY, this.getZ() + rangeXZ);
        return bb;
    }

    @Override
    public void updateResourceState() {
        this.updateWirePos();
        this.sendPacket();
        this.markDirty();
        BlockUtil.markBlockForUpdate(this.getWorld(), this.getPos());
    }

    @Override
    public ResourceState<ModelSetConnector> getResourceState() {
        return this.state;
    }

    protected abstract ResourceType getSubType();

    @Override
    public void setOffset(float offsetX, float offsetY, float offsetZ, boolean sync) {
        super.setOffset(offsetX, offsetY, offsetZ, sync);
        if (world != null)
            this.updateWirePos();
    }

    @Override
    public void addInfoToCrashReport(CrashReportCategory reportCategory) {
        super.addInfoToCrashReport(reportCategory);
        TileEntityConnectorBaseKt.addInfoToCrashReport(this, reportCategory);
    }
}