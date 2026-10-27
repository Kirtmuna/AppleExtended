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

package jp.ngt.mcte.block;

import jp.apple.appleextended.gui.AppleGuiHelper;
import jp.ngt.mcte.MCTE;
import jp.ngt.mcte.item.ItemMiniature;
import jp.ngt.mcte.item.ItemMiniature.MiniatureMode;
import jp.ngt.ngtlib.block.BlockArgHolder;
import jp.ngt.ngtlib.block.BlockContainerCustom;
import jp.ngt.ngtlib.block.NGTObject;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.RTMItem;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlockMiniature extends BlockContainerCustom {
    public BlockMiniature() {
        super(Material.ROCK);
        this.setLightOpacity(0);
    }

    //1.8以前
	/*@Override
	public int getMobilityFlag()
    {
        return 0;//ピストンで押せる(大嘘)
    }*/

    @Override
    public TileEntity createNewTileEntity(World world, int par2) {
        return new TileEntityMiniature();
    }

    @Override
    public boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        if (NGTUtil.isEquippedItem(holder.getPlayer(), RTMItem.crowbar)) {
            if (holder.getWorld().isRemote) {
                AppleGuiHelper.openOffsetGui(
                        this.getMiniatureTileEntity(holder.getWorld(), holder.getBlockPos()));
            }
            return true;
        }
        ItemStack stack = holder.getPlayer().inventory.getCurrentItem();
        if (stack == null || stack.getItem() != MCTE.itemMiniature) {
            if (!holder.getWorld().isRemote) {
                TileEntityMiniature tile = this.getMiniatureTileEntity(holder.getWorld(), holder.getBlockPos());
                tile.setRotation((float) NGTMath.normalizeAngle(tile.getRotation() + MCTE.rotationInterval), true);
            }
            return true;
        }
        return false;
    }


    private TileEntityMiniature getMiniatureTileEntity(IBlockAccess world, BlockPos pos) {
        return (TileEntityMiniature) world.getTileEntity(pos);
    }

    //隣からRS入力
    @Override
    protected void neighborChanged(BlockArgHolder holder) {
        if (!holder.getWorld().isRemote) {
            TileEntityMiniature te = this.getMiniatureTileEntity(holder.getWorld(), holder.getBlockPos());
            te.port.onNeighborBlockChange(te);
        }
    }

    /***********************************************************************************************/

    @Override
    public float getBlockHardness(IBlockState blockState, World world, BlockPos pos) {
        return this.getMiniatureTileEntity(world, pos).getMBState().hardness;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
        List<AxisAlignedBB> aabbList = this.getMiniatureTileEntity(world, pos).getCollisionBoxes(pos.getX(), pos.getY(), pos.getZ());
        if (aabbList.isEmpty()) {
            AxisAlignedBB aabb = this.getCollisionBoundingBox(state, world, pos);

            if (aabb != null && entityBox.intersects(aabb)) {
                collidingBoxes.add(aabb);
            }
        } else {
            for (AxisAlignedBB aabb : aabbList) {
                if (aabb != null && entityBox.intersects(aabb)) {
                    collidingBoxes.add(aabb);
                }
            }
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntityMiniature tile = this.getMiniatureTileEntity(world, pos);
        return tile == null ? super.getCollisionBoundingBox(state, world, pos) : tile.getSelectBox(pos.getX(), pos.getY(), pos.getZ());
    }

    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(World world, BlockPos pos) {
        return this.getCollisionBoundingBox(null, world, pos);
    }

    @Override
    protected int getWeakPower(BlockArgHolder holder) {
        return this.getStrongPower(holder);
    }

    @Override
    protected int getStrongPower(BlockArgHolder holder) {
        TileEntityMiniature te = this.getMiniatureTileEntity(holder.getBlockAccess(), holder.getBlockPos());
        if (te == null) {
            return 0;
        }
        int power = te.port.isProvidingPower(te, holder.getFacing());
        return power > 0 ? power : this.getMiniatureTileEntity(holder.getBlockAccess(), holder.getBlockPos()).getMBState().redstonePower;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    protected boolean shouldCheckWeakPower(BlockArgHolder holder) {
        return false;
    }

    @Override
    protected boolean canConnectRedstone(BlockArgHolder holder) {
        return this.getStrongPower(holder) > 0;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntityMiniature miniature = this.getMiniatureTileEntity(world, pos);
        return miniature != null ? miniature.getMBState().lightValue : 0;
    }

    /**
     * Entityの中心座標がブロック内にないと意味なし->living.onLadder()
     */
    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return this.getMiniatureTileEntity(world, pos).getMBState().isLadder();
    }

    @Override
    public boolean isBurning(IBlockAccess world, BlockPos pos) {
        return this.getMiniatureTileEntity(world, pos).getMBState().isBurning();
    }

    @Override
    public boolean isFireSource(World world, BlockPos pos, EnumFacing side) {
        return this.getMiniatureTileEntity(world, pos).getMBState().isFireSource();
    }

    @Override
    public ArrayList<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        ret.add(this.getMiniatureItem(world, pos));
        return ret;
    }

    /*@Override
    public boolean isBed(IBlockAccess world, BlockPos pos, Entity player)
    {
    	return this.getMiniatureTileEntity(world, pos).getMBState().isBed();
    }

    @Override
    public BlockPos getBedSpawnPosition(IBlockAccess world, BlockPos pos, EntityPlayer player)
    {
    	if(world instanceof World)
    	{
            return BlockBed.getSafeExitLocation((World)world, pos, 0);
    	}
        return null;
    }

    @Override
    public boolean isBedFoot(IBlockAccess world, BlockPos pos)
    {
    	return false;
    }*/

    @Override
    public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
        return this.getMiniatureTileEntity(world, pos).getMBState().explosionResistance;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return this.getMiniatureItem(world, pos);
    }

    @Override
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
    }

    private ItemStack getMiniatureItem(IBlockAccess world, BlockPos pos) {
        TileEntityMiniature tile = this.getMiniatureTileEntity(world, pos);
        NGTObject ngto = tile.blocksObject;
        float scale = tile.scale;
        float mx = tile.offsetX;
        float my = tile.offsetY;
        float mz = tile.offsetZ;
        MiniatureMode mode = tile.mode;
        MiniatureBlockState state = tile.getMBState();
        return ItemMiniature.createMiniatureItem(ngto, scale, mx, my, mz, mode, state);
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }
}