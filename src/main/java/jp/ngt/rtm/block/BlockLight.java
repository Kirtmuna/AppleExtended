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
import jp.ngt.ngtlib.util.PermissionManager;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.block.tileentity.TileEntityLight;
import jp.ngt.rtm.item.ItemInstalledObject;
import jp.ngt.rtm.item.ItemInstalledObject.IstlObjType;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class BlockLight extends BlockMachineBase {
    public BlockLight() {
        super(Material.GLASS);
        this.setSoundType(SoundType.GLASS);
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2) {
        return new TileEntityLight();
    }

    @Override
    protected ItemStack getItem(int meta) {
        return new ItemStack(RTMItem.installedObject, 1, IstlObjType.LIGHT.id);
    }

    @Override
    public boolean removedByPlayer(BlockArgHolder holder, boolean willHarvest) {
        if (holder.getWorld().isRemote) {
            return super.removedByPlayer(holder, willHarvest);
        } else {
            if (PermissionManager.INSTANCE.hasPermission(holder.getPlayer(), RTMCore.EDIT_ORNAMENT)) {
                return super.removedByPlayer(holder, willHarvest);
            }
            return false;
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.LIGHT);
    }
}