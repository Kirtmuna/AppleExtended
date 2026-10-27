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

import jp.ngt.ngtlib.block.BlockArgHolder;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.item.ItemInstalledObject;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockConnector extends BlockElectricalWiring {
    public BlockConnector() {
        super(Material.ROCK);
        this.setLightOpacity(0);
        this.setAABB(FULL_BLOCK_AABB);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int par2) {
        return par2 >= 6 ? new TileEntityConnectorOut() : new TileEntityConnectorIn();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
        return this.getBoundingBox(state, world, pos).offset(pos);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        int meta = state.getBlock().getMetaFromState(state);
        return this.getBlockBounds(meta % 6);
    }

    protected AxisAlignedBB getBlockBounds(int par1) {
        float range = 0.25F;
        float minX = 0.5F - range;
        float minY = 0.5F - range;
        float minZ = 0.5F - range;
        float maxX = 0.5F + range;
        float maxY = 0.5F + range;
        float maxZ = 0.5F + range;
        switch (par1) {
            case 0:
                maxY = 1.0F;
                break;
            case 1:
                minY = 0.0F;
                break;
            case 2:
                maxZ = 1.0F;
                break;
            case 3:
                minZ = 0.0F;
                break;
            case 4:
                maxX = 1.0F;
                break;
            case 5:
                minX = 0.0F;
                break;
        }
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!world.isRemote) {
            int meta = BlockUtil.getMetadata(world, pos);
            spawnAsEntity(world, pos, this.getItem(meta));
        }
    }

    @Override
    protected ItemStack getItem(int damage) {
        damage = istlObjType(damage).id;
        return new ItemStack(RTMItem.installedObject, 1, damage);
    }

    @Override
    public boolean canConnect(World world, int x, int y, int z) {
        return true;
    }

    @Override
    protected ItemInstalledObject.IstlObjType istlObjType(int damage) {
        return damage < 6 ? ItemInstalledObject.IstlObjType.CONNECTOR_IN : ItemInstalledObject.IstlObjType.CONNECTOR_OUT;
    }

    @Override
    public boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        if (holder.getPlayer().isSneaking()) {
            if (holder.getWorld().isRemote) {
                int x = holder.getBlockPos().getX();
                int y = holder.getBlockPos().getY();
                int z = holder.getBlockPos().getZ();
                holder.getPlayer().openGui(RTMCore.instance, RTMCore.guiIdSelectTileEntityModel, holder.getWorld(), x, y, z);
            }
            return true;
        }
        return super.onBlockActivated(holder, hitX, hitY, hitZ);
    }
}
