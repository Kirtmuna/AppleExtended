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

public class BlockScaffoldStairs extends BlockContainerCustomWithMeta {
    public BlockScaffoldStairs(Block par1) {
        super(Material.IRON);
        this.setHardness(2.0F);
        this.setResistance(10.0F);
        this.setSoundType(RTMSound.SOUND_METAL2);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityScaffoldStairs();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        int facing = (NGTMath.floor((NGTMath.normalizeAngle(placer.rotationYaw + 180.0D) / 90.0D) + 0.5D) & 3);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityScaffold) {
            ((TileEntityScaffold) tile).setDir((byte) facing);
        }
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityScaffold) {
            byte dir = ((TileEntityScaffold) tile).getDir();
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            byte flag0 = getConnectionType(world, x + 1, y, z, dir);
            byte flag1 = getConnectionType(world, x - 1, y, z, dir);
            byte flag2 = getConnectionType(world, x, y, z + 1, dir);
            byte flag3 = getConnectionType(world, x, y, z - 1, dir);

            if (dir == 0 || dir == 2) {
                if (flag1 != 3) {
                    this.setAABB(new AxisAlignedBB(0.0F, 0.0F, 0.0F, 0.0625F, 2.0F, 1.0F));
                    super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
                }

                if (flag0 != 3) {
                    this.setAABB(new AxisAlignedBB(0.9375F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F));
                    super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
                }

                for (int i = 0; i < 4; ++i) {
                    float f0 = i * 0.25F;
                    float f1 = (dir == 2) ? f0 : 0.75F - f0;
                    this.setAABB(new AxisAlignedBB(0.0F, 0.0F + f0, f1, 1.0F, 0.25F + f0, 0.25F + f1));
                    super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
                }
            } else {
                if (flag3 != 3) {
                    this.setAABB(new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0625F));
                    super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
                }

                if (flag2 != 3) {
                    this.setAABB(new AxisAlignedBB(0.0F, 0.0F, 0.9375F, 1.0F, 2.0F, 1.0F));
                    super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
                }

                for (int i = 0; i < 4; ++i) {
                    float f0 = i * 0.25F;
                    float f1 = (dir == 1) ? f0 : 0.75F - f0;
                    this.setAABB(new AxisAlignedBB(f1, 0.0F + f0, 0.0F, 0.25F + f1, 0.25F + f0, 1.0F));
                    super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
                }
            }

            this.setAABB(FULL_BLOCK_AABB);
        } else {
            this.setAABB(FULL_BLOCK_AABB);
            super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
        }
    }

    public static byte getConnectionType(IBlockAccess world, int x, int y, int z, byte dir) {
        IBlockState state = BlockUtil.getBlockState(world, x, y, z);
        Block block = state.getBlock();
        Block blockD = BlockUtil.getBlock(world, x, y - 1, z);
        Block blockU = BlockUtil.getBlock(world, x, y + 1, z);

        if (block == RTMBlock.scaffold) {
            TileEntity tile = BlockUtil.getTileEntity(world, x, y, z);
            if (tile instanceof TileEntityScaffold) {
                byte dir2 = ((TileEntityScaffold) tile).getDir();
                boolean b0 = (dir2 == 0 || dir2 == 2);
                return (byte) (b0 ? 1 : 2);
            }
            return 0;
        } else if (block == RTMBlock.scaffoldStairs) {
            TileEntity tile = BlockUtil.getTileEntity(world, x, y, z);
            if (tile instanceof TileEntityScaffoldStairs) {
                if (((TileEntityScaffoldStairs) tile).getDir() == dir) {
                    return 3;
                }
            }
            return 0;
        } else if (blockD == RTMBlock.scaffoldStairs) {
            TileEntity tile = BlockUtil.getTileEntity(world, x, y - 1, z);
            if (tile instanceof TileEntityScaffoldStairs) {
                if (((TileEntityScaffoldStairs) tile).getDir() == dir) {
                    return 3;
                }
            }
            return 0;
        } else if (blockU == RTMBlock.scaffoldStairs) {
            TileEntity tile = BlockUtil.getTileEntity(world, x, y + 1, z);
            if (tile instanceof TileEntityScaffoldStairs) {
                if (((TileEntityScaffoldStairs) tile).getDir() == dir) {
                    return 3;
                }
            }
            return 0;
        } else if (state.isOpaqueCube()) {
            return 4;
        } else {
            return 0;
        }
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!world.isRemote) {
            spawnAsEntity(world, pos, this.getItem(state.getBlock().getMetaFromState(state)));
        }
    }

    @Override
    protected ItemStack getItem(int damage) {
        return new ItemStack(Item.getItemFromBlock(this), 1, damage);
    }

    @Override
    public Boolean isEntityInsideMaterial(IBlockAccess world, BlockPos pos, IBlockState state, Entity entity, double yToTest, Material materialIn, boolean testingHead) {
        return BlockScaffold.getSpeed(world, pos) != 0.0F;
    }

    @Override
    public Vec3d modifyAcceleration(World world, BlockPos pos, Entity entity, Vec3d motion) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityScaffoldStairs) {
            Vec3 vec = ((TileEntityScaffoldStairs) tile).getMotionVec();
            BlockScaffold.addVecToEntity(entity, vec);
        }
        return motion;
    }

    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityScaffoldStairs) {
            Vec3 vec = ((TileEntityScaffoldStairs) tile).getMotionVec();
            BlockScaffold.addVecToEntity(entity, vec);
        }
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
    }

    @Override
    protected boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        return BlockOrnamentMain.onBlockActivated(holder);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.STAIR);
    }
}