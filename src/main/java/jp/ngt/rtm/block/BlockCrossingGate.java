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
import jp.ngt.rtm.block.tileentity.TileEntityCrossingGate;
import jp.ngt.rtm.item.ItemInstalledObject;
import jp.ngt.rtm.item.ItemInstalledObject.IstlObjType;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class BlockCrossingGate extends BlockMachineBase {
    public BlockCrossingGate() {
        super(Material.ROCK);
        this.setSoundType(SoundType.GLASS);
        this.setLightOpacity(0);
        this.setAABB(new AxisAlignedBB(0.125F, 0.0F, 0.125F, 0.875F, 3.0F, 0.875F));
    }

    @Override
    public TileEntity createNewTileEntity(World world, int par2) {
        return new TileEntityCrossingGate();
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!world.isRemote) {
            spawnAsEntity(world, pos, new ItemStack(RTMItem.installedObject, 1, IstlObjType.CROSSING.id));
        }
    }

    @Override
    protected void neighborChanged(BlockArgHolder holder) {
        super.neighborChanged(holder);
        BlockPos pos = holder.getBlockPos();
        this.checkPower(holder.getWorld(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        this.checkPower(world, pos.getX(), pos.getY(), pos.getZ());
    }

    private void checkPower(World world, int x, int y, int z) {
        TileEntityCrossingGate tile = (TileEntityCrossingGate) BlockUtil.getTileEntity(world, x, y, z);
        tile.isGettingPower = world.isBlockIndirectlyGettingPowered(new BlockPos(x, y, z)) > 0;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.CROSSING);
    }
}