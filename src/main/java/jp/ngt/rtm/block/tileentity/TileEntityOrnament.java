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

import com.anatawa12.fixRtm.rtm.block.tileentity.TileEntityOrnamentKt;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.block.TileEntityPlaceable;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.modelset.ModelSetOrnament;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class TileEntityOrnament extends TileEntityPlaceable implements IResourceSelector {
    private ResourceState<ModelSetOrnament> state = new ResourceState<>(this.getSubType(), this);
    private byte attachedSide;
    private float randomScale;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.state.readFromNBT(nbt.getCompoundTag("State"));
        this.attachedSide = nbt.getByte("AttachedSide");
        this.randomScale = nbt.getFloat("RandomScale");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setTag("State", this.state.writeToNBT());
        nbt.setByte("AttachedSide", this.attachedSide);
        nbt.setFloat("RandomScale", this.getRandomScale());
        return nbt;
    }

    public byte getAttachedSide() {
        return this.attachedSide;
    }

    public void setAttachedSide(byte side) {
        this.attachedSide = side;
        this.markDirty();
    }

    public float getRandomScale() {
        if (this.randomScale <= 0.0F) {
            float min = this.getResourceState().getResourceSet().getConfig().minRandomScale;
            float randF = NGTMath.RANDOM.nextFloat();
            this.randomScale = min + (1.0F - min) * randF;
        }
        return this.randomScale;
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass >= 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        float[] box = this.getResourceState().getResourceSet().getConfig().renderAABB;
        BlockPos pos = this.getPos();
        AxisAlignedBB aabb = new AxisAlignedBB(
                pos.getX() + box[0], pos.getY() + box[1], pos.getZ() + box[2],
                pos.getX() + box[3], pos.getY() + box[4], pos.getZ() + box[5]);
        return aabb;
    }

    @Override
    public ResourceState<ModelSetOrnament> getResourceState() {
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

    protected abstract ResourceType getSubType();

    @Override
    public void addInfoToCrashReport(CrashReportCategory reportCategory) {
        super.addInfoToCrashReport(reportCategory);
        TileEntityOrnamentKt.addInfoToCrashReport(this, reportCategory);
    }
}