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

import com.anatawa12.fixRtm.rtm.block.BlockOrnamentMain;
import jp.apple.appleextended.gui.AppleGuiHelper;
import jp.ngt.ngtlib.block.BlockArgHolder;
import jp.ngt.ngtlib.block.BlockContainerCustomWithMeta;
import jp.ngt.ngtlib.block.TileEntityPlaceable;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.item.ItemInstalledObject;
import jp.ngt.rtm.item.ItemInstalledObject.IstlObjType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class BlockSignal extends BlockContainerCustomWithMeta implements IBlockConnective {
    public BlockSignal() {
        super(Material.ROCK);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int par2) {
        return new TileEntitySignal();
    }

    @Override
    public boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        if (holder.getWorld().isRemote) {
            int x = holder.getBlockPos().getX();
            int y = holder.getBlockPos().getY();
            int z = holder.getBlockPos().getZ();
            if (holder.getPlayer().inventory.getCurrentItem().getItem() == RTMItem.crowbar) {
                TileEntity te = holder.getWorld().getTileEntity(holder.getBlockPos());
                if (te instanceof TileEntityPlaceable) {
                    AppleGuiHelper.openOffsetGui(te);
                }
                return true;
            }
            if (holder.getPlayer().isSneaking()) {
                holder.getPlayer().openGui(RTMCore.instance, RTMCore.guiIdSelectTileEntityModel, holder.getWorld(), x, y, z);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removedByPlayer(BlockArgHolder holder, boolean willHarvest) {
        World world = holder.getWorld();
        BlockPos pos = holder.getBlockPos();
        if (!world.isRemote) {
            TileEntitySignal tile = (TileEntitySignal) world.getTileEntity(pos);
            tile.setOrigBlock();
            if (!holder.getPlayer().capabilities.isCreativeMode) {
                this.dropBlockAsItemWithChance(world, pos, null, 0.0F, 0);
            }
        }
        return true;
    }

    @Override
    protected ItemStack getItem(int damage) {
        return new ItemStack(RTMItem.installedObject, 1, IstlObjType.SIGNAL.id);
    }

    @Override
    public boolean canConnect(World world, int x, int y, int z) {
        return true;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.FLUORESCENT);
    }
}