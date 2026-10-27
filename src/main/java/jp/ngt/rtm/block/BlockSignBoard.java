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

import com.anatawa12.fixRtm.UtilsKt;
import com.anatawa12.fixRtm.gui.GuiId;
import com.anatawa12.fixRtm.rtm.block.BlockOrnamentMain;
import jp.ngt.ngtlib.block.BlockArgHolder;
import jp.ngt.ngtlib.block.BlockContainerCustomWithMeta;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.util.PermissionManager;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.block.tileentity.TileEntitySignBoard;
import jp.ngt.rtm.item.ItemInstalledObject;
import jp.ngt.rtm.item.ItemInstalledObject.IstlObjType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockSignBoard extends BlockContainerCustomWithMeta {
    public BlockSignBoard() {
        super(Material.CIRCUITS);
        this.setLightOpacity(0);
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2) {
        return new TileEntitySignBoard();
    }

    @Override
    protected ItemStack getItem(int damage) {
        return new ItemStack(RTMItem.installedObject, 1, IstlObjType.SIGNBOARD.id);
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
    public boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        if (holder.getWorld().isRemote) {
            int x = holder.getBlockPos().getX();
            int y = holder.getBlockPos().getY();
            int z = holder.getBlockPos().getZ();

            if (holder.getPlayer().inventory.getCurrentItem().getItem() == RTMItem.crowbar) {
                UtilsKt.openGui(holder.getPlayer(), GuiId.ChangeOffset, holder.getWorld(), x, y, z);
                return true;
            }

            holder.getPlayer().openGui(RTMCore.instance, RTMCore.instance.guiIdSignboard, holder.getWorld(), x, y, z);
        }
        return true;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.SIGNBOARD);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntitySignBoard tile = (TileEntitySignBoard) world.getTileEntity(pos);
        if (tile != null) {
            int value = tile.getResourceState().getResourceSet().getConfig().lightValue;
            if (value >= 0) {
                return value;
            } else if (value == -16) {
                return NGTMath.RANDOM.nextInt(6) * 3;
            } else if (tile.isGettingPower) {
                return -value;
            }
        }
        return 0;
    }
}