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
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.block.TileEntityPlaceable;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.item.ItemInstalledObject;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public abstract class BlockElectricalWiring extends BlockContainerCustomWithMeta implements IBlockConnective {
    protected BlockElectricalWiring(Material material) {
        super(material);
    }

    @Override
    public boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        if (holder.getPlayer().inventory.getCurrentItem().getItem() == RTMItem.crowbar) {
            if (holder.getWorld().isRemote) {
                TileEntity te = holder.getWorld().getTileEntity(holder.getBlockPos());
                if (te instanceof TileEntityPlaceable) {
                    AppleGuiHelper.openOffsetGui(te);
                }
            }
            return true;
        }
        if (holder.getWorld().isRemote) {
            return true;
        } else {
            TileEntityElectricalWiring tile = (TileEntityElectricalWiring) holder.getWorld().getTileEntity(holder.getBlockPos());
            tile.onRightClick(holder.getPlayer());
            return true;
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, istlObjType(BlockUtil.getMetadata(world, pos)));
    }

    protected abstract ItemInstalledObject.IstlObjType istlObjType(int damage);

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntityElectricalWiring tile = (TileEntityElectricalWiring) world.getTileEntity(pos);
        tile.onBlockBreaked();
        super.breakBlock(world, pos, state);
    }
}