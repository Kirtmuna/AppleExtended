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

import com.anatawa12.fixRtm.rtm.electric.TileEntitySignalKt;
import jp.apple.appleextended.compat.ReplaySyncManager;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.block.TileEntityPlaceable;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ScriptExecuter;
import jp.ngt.rtm.modelpack.modelset.ModelSetSignal;
import jp.ngt.rtm.modelpack.state.ResourceState;
import jp.ngt.rtm.modelpack.state.ResourceStateWithBlock;
import net.minecraft.block.Block;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntitySignal extends TileEntityPlaceable implements IProvideElectricity, IResourceSelector, ITickable {
    private ResourceStateWithBlock<ModelSetSignal> state = new ResourceStateWithBlock<>(RTMResource.SIGNAL, this);
    private ScriptExecuter executer = new ScriptExecuter();

    private TileEntity origTileEntity;
    public int blockDirection;
    private int signalLevel = 0;
    public int tick;

    public TileEntitySignal() {
        this.state.setBlock(Blocks.AIR, 0);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        ReplaySyncManager.patchResourceSelectorMetadata(this, nbt);
        super.readFromNBT(nbt);
        this.state.readFromNBT(nbt.getCompoundTag("State"));
        this.blockDirection = nbt.getInteger("blockDir");
        this.signalLevel = nbt.getInteger("Signal");

        if (this.world != null && this.world.isRemote) {
            BlockUtil.markBlockForUpdate(this.getWorld(), this.getPos());//描画の更新
        }

        if (nbt.hasKey("BaseBlockData")) {
            this.origTileEntity = TileEntity.create(null, nbt.getCompoundTag("BaseBlockData"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setTag("State", this.state.writeToNBT());
        nbt.setInteger("blockDir", this.blockDirection);
        nbt.setInteger("Signal", this.signalLevel);

        if (this.origTileEntity != null) {
            NBTTagCompound nbt2 = new NBTTagCompound();
            this.origTileEntity.writeToNBT(nbt2);
            nbt.setTag("BaseBlockData", nbt2);
        }

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
    public int getElectricity() {
        return 0;
    }

    @Override
    public void setElectricity(int x, int y, int z, int level) {
        if (!this.world.isRemote) {
            ModelSetSignal modelSet = this.getResourceState().getResourceSet();
            if (level > modelSet.getConfig().maxSignalLevel) {
                level = modelSet.getConfig().maxSignalLevel;
            }
            this.signalLevel = level;
            this.markDirty();
            this.sendPacket();
        }
    }

    public void setSignalProperty(String name, Block par1, int par2, EntityPlayer player, TileEntity tile) {
        this.state.setResourceName(name);
        this.state.setBlock(par1, this.getBlockMetadata());
        this.origTileEntity = tile;
        this.blockDirection = par2;
        this.setRotation(player, 15.0F, false);
        this.sendPacket();
        this.markDirty();
    }

    public TileEntity getOrigTileEntity() {
        return this.origTileEntity;
    }

    @SideOnly(Side.CLIENT)
    public float getBlockDirection() {
        return (float) this.blockDirection * 90.0F;
    }

    @SideOnly(Side.CLIENT)
    public int getSignal() {
        return this.signalLevel;
    }

    @SideOnly(Side.CLIENT)
    public void setSignal(int par1) {
        this.signalLevel = par1;
    }

    public Block getRenderBlock() {
        return this.getResourceState().block;
    }

    public void setOrigBlock() {
        Block block = this.getRenderBlock();
        int meta = BlockUtil.getMetadata(this.getWorld(), this.getPos());
        BlockUtil.setBlock(this.getWorld(), this.getPos(), block, meta, 3);

        TileEntity tile = this.getWorld().getTileEntity(this.getPos());
        if (this.origTileEntity != null && tile != null) {
            NBTTagCompound nbt = new NBTTagCompound();
            this.origTileEntity.writeToNBT(nbt);
            tile.readFromNBT(nbt);
        }
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass >= 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        double i = this.getPos().getX() + this.getOffsetX();
        double j = this.getPos().getY() + this.getOffsetY();
        double k = this.getPos().getZ() + this.getOffsetZ();
        return new AxisAlignedBB((double) i, (double) j, (double) k, (double) (i + 1), (double) (j + 2), (double) (k + 1))
                .union(new AxisAlignedBB(this.getPos(), this.getPos().add(1, 1, 1)));
    }

    @Override
    public void updateResourceState() {
        if (this.world == null || !this.world.isRemote) {
            this.markDirty();
            this.sendPacket();
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
    public ResourceStateWithBlock<ModelSetSignal> getResourceState() {
        return this.state;
    }

    @Override
    public void addInfoToCrashReport(CrashReportCategory reportCategory) {
        super.addInfoToCrashReport(reportCategory);
        TileEntitySignalKt.addInfoToCrashReport(this, reportCategory);
    }
}