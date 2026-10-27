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
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.block.tileentity.TileEntityTurnstile;
import jp.ngt.rtm.item.ItemInstalledObject;
import jp.ngt.rtm.item.ItemInstalledObject.IstlObjType;
import jp.ngt.rtm.item.ItemTicket;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class BlockTurnstile extends BlockMachineBase {
    private static final float MAX_Y = 1.5F;

    public BlockTurnstile() {
        super(Material.ROCK);
        this.setLightOpacity(0);
        this.setHardness(3.0F);
        this.setResistance(20.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2) {
        return new TileEntityTurnstile();
    }

    @Override
    protected ItemStack getItem(int damage) {
        return new ItemStack(RTMItem.installedObject, 1, IstlObjType.TURNSTILE.id);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return canThrough(world, pos) ? NULL_AABB : this.getBoundingBox(state, world, pos);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
        this.setAABB(this.getCollisionBoundingBox(state, world, pos));
        super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);

        this.setAABB(FULL_BLOCK_AABB);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        int meta = BlockUtil.getMetadata(world, pos) & 3;
        if (meta != 2 && meta != 0) {
            return new AxisAlignedBB(0.375F, 0.0F, 0.0F, 0.625F, MAX_Y, 1.0F);
        } else {
            return new AxisAlignedBB(0.0F, 0.0F, 0.375F, 1.0F, MAX_Y, 0.625F);
        }
    }

    @Override
    public boolean isSideSolid(IBlockState base_state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        if (side == EnumFacing.UP || side == EnumFacing.DOWN) {
            return false;
        } else {
            int meta = BlockUtil.getMetadata(world, pos) & 3;
            if (meta != 2 && meta != 0) {
                return side == EnumFacing.NORTH || side == EnumFacing.SOUTH;
            } else {
                return side == EnumFacing.EAST || side == EnumFacing.WEST;
            }
        }
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return canThrough(world, pos);
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
            ItemStack itemStack = player.getHeldItemMainhand();
            if (itemStack != null && itemStack.getItem() instanceof ItemTicket) {
                this.openGate(world, x, y, z, player);
                if (((ItemTicket) itemStack.getItem()).ticketType != 2) {
                    ItemStack itemStack2 = ItemTicket.consumeTicket(itemStack);
                    if (!world.isRemote && itemStack2 != null) {
                        spawnAsEntity(world, new BlockPos(x, y + 1, z), itemStack2);
                    }
                }
            }
        }
        return true;
    }

    public void openGate(World world, int x, int y, int z, EntityPlayer player) {
        TileEntityTurnstile tile = (TileEntityTurnstile) BlockUtil.getTileEntity(world, x, y, z);
        if (!tile.canThrough()) {
            tile.setCount(30);
            tile.onActivate();
        }
    }

    public static boolean canThrough(IBlockAccess world, BlockPos pos) {
        TileEntityTurnstile tile = (TileEntityTurnstile) BlockUtil.getTileEntity(world, pos);
        return tile != null ? tile.canThrough() : false;
    }

    @Override
    public PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.canThrough(world, pos) ? PathNodeType.OPEN : PathNodeType.BLOCKED;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.TURNSTILE);
    }
}