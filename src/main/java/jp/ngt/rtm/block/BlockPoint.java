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

package jp.ngt.rtm.block;

import com.anatawa12.fixRtm.rtm.block.BlockOrnamentMain;
import jp.ngt.ngtlib.block.BlockArgHolder;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.block.tileentity.TileEntityPoint;
import jp.ngt.rtm.item.ItemInstalledObject;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class BlockPoint extends BlockMachineBase {
    public BlockPoint() {
        super(Material.ROCK);
        this.setAABB(new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 0.3125F, 1.0F));
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2) {
        return new TileEntityPoint();
    }

    @Override
    public boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        World world = holder.getWorld();
        BlockPos pos = holder.getBlockPos();
        EntityPlayer player = holder.getPlayer();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (!this.clickMachine(world, x, y, z, player)) {
            TileEntity tile = BlockUtil.getTileEntity(world, x, y, z);
            if (tile != null && tile instanceof TileEntityPoint) {
                TileEntityPoint point = (TileEntityPoint) tile;
                if (NGTUtil.isEquippedItem(player, RTMItem.crowbar)) {
                    if (!world.isRemote) {
                        float f0 = point.getMove();
                        point.setMove(-f0);
                    }
                } else {
                    if (!world.isRemote) {
                        boolean b0 = point.isActivated();
                        point.setActivated(!b0);
                        world.notifyNeighborsOfStateChange(pos, this, true);
                        world.notifyNeighborsOfStateChange(pos.down(), this, true);
                    }
                }
                point.onActivate();
                return true;
            }
        }
        return true;
    }

    @Override
    protected int getWeakPower(BlockArgHolder holder) {
        TileEntity tile = BlockUtil.getTileEntity(holder.getBlockAccess(), holder.getBlockPos());
        boolean b = tile != null && tile instanceof TileEntityPoint && ((TileEntityPoint) tile).isActivated();
        return b ? 15 : 0;
    }

    @Override
    protected int getStrongPower(BlockArgHolder holder) {
        return this.getWeakPower(holder);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.POINT);
    }
}