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

import com.anatawa12.fixRtm.rtm.block.tileentity.TileEntityMachineBaseKt;
import jp.apple.appleextended.compat.ReplaySyncManager;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.block.TileEntityPlaceable;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.ScriptExecuter;
import jp.ngt.rtm.modelpack.modelset.ModelSetMachine;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class TileEntityMachineBase extends TileEntityPlaceable implements IResourceSelector, ITickable {
    private ResourceState<ModelSetMachine> state = new ResourceState<>(this.getSubType(), this);
    private ScriptExecuter executer = new ScriptExecuter();
    private float pitch;

    public int tick;
    public boolean isGettingPower;
    protected Vec3 normal;

    /**
     * メタで保存してた方向データを更新したか
     */
    private boolean yawFixed;

    public TileEntityMachineBase() {
        super();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        ReplaySyncManager.patchResourceSelectorMetadata(this, nbt);
        super.readFromNBT(nbt);
        this.state.readFromNBT(nbt.getCompoundTag("State"));
        this.pitch = nbt.getFloat("Pitch");
        this.yawFixed = nbt.hasKey("Yaw");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setTag("State", this.state.writeToNBT());
        nbt.setFloat("Pitch", this.pitch);
        return nbt;
    }

    @Override
    public void update() {
        ++this.tick;
        if (this.tick == Integer.MAX_VALUE) {
            this.tick = 0;
        }
        if (this.world.isRemote) {
            ReplaySyncManager.checkModelSync(this);
        }
        if (!this.getWorld().isRemote) {
            this.executer.execScript(this);
        }
    }

    @Override
    public void setRotation(float par1, boolean synch) {
        super.setRotation(par1, synch);
        this.yawFixed = true;
    }


    public float getPitch() {
        return this.pitch;
    }

    public Vec3 getNormal(float x, float y, float z, float pitch, float yaw) {
        if (this.normal == null) {
            this.normal = new Vec3(x, y, z);
        }
        return this.normal;
    }

    /**
     * 右クリック時
     */
    public void onActivate() {
        ModelSetMachine set = this.getResourceState().getResourceSet();
        if (this.world.isRemote && set.getConfig().sound_OnActivate != null) {
            RTMCore.proxy.playSound(this, set.getConfig().sound_OnActivate, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass >= 0;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        AxisAlignedBB bb = new AxisAlignedBB(this.getPos(), this.getPos().add(1, 1, 1));
        return bb.offset(getOffsetX(), getOffsetY(), getOffsetZ());
    }

    @Override
    public ResourceState<ModelSetMachine> getResourceState() {
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

    public abstract ResourceType getSubType();

    @Override
    public void addInfoToCrashReport(CrashReportCategory reportCategory) {
        super.addInfoToCrashReport(reportCategory);
        TileEntityMachineBaseKt.addInfoToCrashReport(this, reportCategory);
    }

    @Override
    public float getRotationX() {
        return super.getRotationX();
    }

    @Override
    public void setRotationXYZS(float x, float y, float z, float s, boolean sync) {
        super.setRotationXYZS(x, y, z, s, sync);
        this.pitch = x;
    }

    @Override
    public void setRotation(EntityPlayer player, float rotationInterval, boolean synch) {
        super.setRotation(player, rotationInterval, synch);
        this.pitch = 0.0F;
    }
}