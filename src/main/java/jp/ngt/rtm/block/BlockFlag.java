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
import jp.ngt.ngtlib.block.BlockContainerCustom;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.block.tileentity.TileEntityFlag;
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

public class BlockFlag extends BlockContainerCustom {
    public BlockFlag() {
        super(Material.IRON);
        this.setLightOpacity(0);
        this.setAABB(new AxisAlignedBB(0.4375F, 0.0F, 0.4375F, 0.5625F, 1.0F, 0.5625F));
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityFlag();
    }

    @Override
    public boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        if (holder.getWorld().isRemote) {
            int x = holder.getBlockPos().getX();
            int y = holder.getBlockPos().getY();
            int z = holder.getBlockPos().getZ();
            holder.getPlayer().openGui(RTMCore.instance, RTMCore.guiIdSelectTileEntityTexture, holder.getWorld(), x, y, z);
        }
        return true;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.FLAG);
    }
}