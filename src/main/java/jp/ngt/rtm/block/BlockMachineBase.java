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
import jp.apple.appleextended.gui.AppleGuiHelper;
import jp.ngt.ngtlib.block.BlockArgHolder;
import jp.ngt.ngtlib.block.BlockContainerCustomWithMeta;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.block.tileentity.TileEntityMachineBase;
import jp.ngt.rtm.item.ItemInstalledObject;
import jp.ngt.rtm.modelpack.cfg.MachineConfig;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class BlockMachineBase extends BlockContainerCustomWithMeta {
    protected BlockMachineBase(Material mat) {
        super(mat);
    }

    @Override
    public boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        this.clickMachine(holder.getWorld(), holder.getBlockPos().getX(), holder.getBlockPos().getY(), holder.getBlockPos().getZ(), holder.getPlayer());
        return true;
    }

    protected boolean clickMachine(World world, int x, int y, int z, EntityPlayer player) {
        if (world.isRemote) {
            if (NGTUtil.isEquippedItem(player, RTMItem.crowbar)) {
                TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
                AppleGuiHelper.openOffsetGui(te);
                return true;
            }

            if (player.isSneaking()) {
                player.openGui(RTMCore.instance, RTMCore.guiIdSelectTileEntityModel, player.world, x, y, z);
                return true;
            }
        }
        if (player.isSneaking()) {
            if (world.isRemote) {
                player.openGui(RTMCore.instance, RTMCore.guiIdSelectTileEntityModel, player.getEntityWorld(), x, y, z);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    @Deprecated
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityMachineBase)) return super.getLightValue(state);
        TileEntityMachineBase tileentitymachinebase = (TileEntityMachineBase) tile;
        if (tileentitymachinebase == null) {
            return 0;
        } else {
            MachineConfig machineconfig = tileentitymachinebase.getResourceState().getResourceSet().getConfig();
            return tileentitymachinebase.isGettingPower ? machineconfig.brightness[1] : machineconfig.brightness[0];
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.FLUORESCENT);
    }
}