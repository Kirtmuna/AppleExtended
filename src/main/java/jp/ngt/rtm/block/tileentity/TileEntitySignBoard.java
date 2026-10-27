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

import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.block.TileEntityPlaceable;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.cfg.SignboardConfig;
import jp.ngt.rtm.modelpack.modelset.TextureSetSignboard;
import jp.ngt.rtm.modelpack.state.ResourceState;
import jp.ngt.rtm.modelpack.state.ResourceStateSignboard;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntitySignBoard extends TileEntityPlaceable implements IResourceSelector, ITickable {
    private ResourceStateSignboard state = new ResourceStateSignboard(RTMResource.SIGNBOARD, this);

    public boolean isGettingPower;
    private byte direction;

    @SideOnly(Side.CLIENT)
    public int counter;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.state.readFromNBT(nbt.getCompoundTag("State"));

        if (this.state.version < 1) {
            this.getResourceState().setResourceName(nbt.getString("name"));
        }

        this.direction = nbt.getByte("dir");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setTag("State", this.state.writeToNBT());
        nbt.setByte("dir", this.direction);
        return nbt;
    }

    @Override
    public void update() {
        TextureSetSignboard set = this.getResourceState().getResourceSet();

        if (this.world.isRemote) {
            ++this.counter;
            if (this.counter >= set.getConfig().frame * set.getConfig().animationCycle) {
                this.counter = 0;
            }
        }

        boolean b = this.world.isBlockIndirectlyGettingPowered(this.getPos()) > 0;
        if (this.isGettingPower ^ b) {
            this.isGettingPower = b;
            this.world.checkLight(this.getPos());
        } else if (set.getConfig().lightValue == -16) {
            this.world.checkLight(this.getPos());
        }
    }

    @Override
    public void updateResourceState() {
        if (this.world == null || !this.world.isRemote) {
            this.markDirty();
            this.sendPacket();
            if (this.world != null) {
                BlockUtil.markBlockForUpdate(this.getWorld(), this.getPos());
            }
        } else {
            this.counter = 0;
        }
    }

    @Override
    public int[] getSelectorPos() {
        return new int[]{this.getPos().getX(), this.getPos().getY(), this.getPos().getZ()};
    }

    public byte getDirection() {
        return this.direction;
    }

    public void setDirection(byte par1) {
        this.direction = par1;
        this.sendPacket();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        TextureSetSignboard set = this.getResourceState().getResourceSet();
        SignboardConfig cfg = set.getConfig();
        int x = this.getPos().getX();
        int y = this.getPos().getY();
        int z = this.getPos().getZ();
        double height = cfg.height / 2.0F;
        double width = cfg.width / 2.0F;
        double depth = cfg.depth / 2.0F;
        double d0 = width >= depth ? width : depth;
        AxisAlignedBB bb = new AxisAlignedBB((double) x - d0, (double) y - height, (double) z - d0, (double) x + d0 + 1.0D, (double) y + height + 1.0D, (double) z + d0 + 1.0D);
        return bb;
    }

    @Override
    public ResourceStateSignboard getResourceState() {
        return this.state;
    }

    @Override
    public boolean closeGui(ResourceState par1) {
        return true;
    }
}