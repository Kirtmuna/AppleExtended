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
import jp.ngt.ngtlib.block.BlockContainerCustomWithMeta;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.rtm.RTMBlock;
import jp.ngt.rtm.RTMSound;
import jp.ngt.rtm.block.tileentity.TileEntityScaffold;
import jp.ngt.rtm.block.tileentity.TileEntityScaffoldStairs;
import jp.ngt.rtm.item.ItemInstalledObject;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class BlockScaffold extends BlockContainerCustomWithMeta {
    public BlockScaffold() {
        super(Material.IRON);
        this.setHardness(2.0F);
        this.setResistance(10.0F);
        ;
        this.setSoundType(RTMSound.SOUND_METAL2);
        this.setAABB(FULL_BLOCK_AABB);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int par2) {
        return new TileEntityScaffold();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        int i0 = (NGTMath.floor((NGTMath.normalizeAngle(placer.rotationYaw + 180.0D) / 90.0D) + 0.5D) & 3);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityScaffold) {
            ((TileEntityScaffold) tile).setDir((byte) i0);
        }
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
        this.setAABB(new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F));
        super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);

        TileEntity tile = world.getTileEntity(pos);
        byte dir = ((TileEntityScaffold) tile).getDir();
        boolean b0 = (dir == 0 || dir == 2);
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        byte flag0 = BlockScaffold.getConnectionType(world, x + 1, y, z, (byte) 1);
        byte flag1 = BlockScaffold.getConnectionType(world, x - 1, y, z, (byte) 1);
        byte flag2 = BlockScaffold.getConnectionType(world, x, y, z + 1, (byte) 0);
        byte flag3 = BlockScaffold.getConnectionType(world, x, y, z - 1, (byte) 0);
        boolean flagXP = !(flag0 >= 1 && flag0 <= 3) && (b0 || (flag2 == 1 || flag3 == 1 || flag2 == 3 || flag3 == 3));
        boolean flagXN = !(flag1 >= 1 && flag1 <= 3) && (b0 || (flag2 == 1 || flag3 == 1 || flag2 == 3 || flag3 == 3));
        boolean flagZP = !(flag2 >= 1 && flag2 <= 3) && (!b0 || (flag0 == 2 || flag1 == 2 || flag0 == 3 || flag1 == 3));
        boolean flagZN = !(flag3 >= 1 && flag3 <= 3) && (!b0 || (flag0 == 2 || flag1 == 2 || flag0 == 3 || flag1 == 3));

        if (flagXP) {
            this.setAABB(new AxisAlignedBB(0.9375F, 0.0F, 0.0F, 1.0F, 1.5F, 1.0F));
            super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
        }

        if (flagXN) {
            this.setAABB(new AxisAlignedBB(0.0F, 0.0F, 0.0F, 0.0625F, 1.5F, 1.0F));
            super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
        }

        if (flagZP) {
            this.setAABB(new AxisAlignedBB(0.0F, 0.0F, 0.9375F, 1.0F, 1.5F, 1.0F));
            super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
        }

        if (flagZN) {
            this.setAABB(new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 1.5F, 0.0625F));
            super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
        }

        this.setAABB(FULL_BLOCK_AABB);
    }

    public static byte getConnectionType(IBlockAccess world, int x, int y, int z, byte dir) {
        IBlockState state = BlockUtil.getBlockState(world, x, y, z);
        Block block0 = state.getBlock();
        Block block1 = BlockUtil.getBlock(world, x, y - 1, z);

        if (block0 == RTMBlock.scaffold) {
            TileEntity tile = BlockUtil.getTileEntity(world, x, y, z);
            if (tile instanceof TileEntityScaffold) {
                byte dir2 = ((TileEntityScaffold) tile).getDir();
                boolean b0 = (dir2 == 0 || dir2 == 2);
                return (byte) (b0 ? 1 : 2);
            }
            return 0;
        } else if (block0 == RTMBlock.scaffoldStairs) {
            TileEntity tile = BlockUtil.getTileEntity(world, x, y, z);
            if (tile instanceof TileEntityScaffoldStairs) {
                byte dir2 = ((TileEntityScaffoldStairs) tile).getDir();
                boolean flag = (dir == 1 && ((dir2 == 1) || (dir2 == 3))) || (dir == 0 && ((dir2 == 0) || (dir2 == 2)));
                return (byte) (flag ? 3 : 0);
            }
            return 0;
        } else if (block1 == RTMBlock.scaffoldStairs) {
            TileEntity tile = BlockUtil.getTileEntity(world, x, y - 1, z);
            if (tile instanceof TileEntityScaffoldStairs) {
                byte dir2 = ((TileEntityScaffoldStairs) tile).getDir();
                boolean flag = (dir == 1 && ((dir2 == 1) || (dir2 == 3))) || (dir == 0 && ((dir2 == 0) || (dir2 == 2)));
                return (byte) (flag ? 3 : 0);
            }
            return 0;
        } else if (state.isOpaqueCube()) {
            return 4;
        } else {
            return 0;
        }
    }

    @Override
    protected ItemStack getItem(int damage) {
        return new ItemStack(Item.getItemFromBlock(this), 1, damage);
    }

    public static float getSpeed(IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityScaffold) {
            return ((TileEntityScaffold) tile).getResourceState().getResourceSet().getConfig().conveyorSpeed;
        }
        return 0.0F;
    }

    @Override
    public Boolean isEntityInsideMaterial(IBlockAccess world, BlockPos pos, IBlockState state, Entity entity, double yToTest, Material materialIn, boolean testingHead) {
        return BlockScaffold.getSpeed(world, pos) != 0.0F;
    }

    @Override
    public Vec3d modifyAcceleration(World world, BlockPos pos, Entity entity, Vec3d motion) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityScaffold) {
            Vec3 vec = ((TileEntityScaffold) tile).getMotionVec();
            BlockScaffold.addVecToEntity(entity, vec);
        }
        return motion;
    }

    public static void addVecToEntity(Entity entity, Vec3 vec) {
        if (vec.length() > 0.0D && entity.isPushedByWater()) {
            double d1 = 1.0;
            entity.motionX += vec.getX() * d1;
            entity.motionY += vec.getY() * d1;
            entity.motionZ += vec.getZ() * d1;
        }
    }

    @Override
    protected boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        return BlockOrnamentMain.onBlockActivated(holder);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.SCAFFOLD);
    }
}