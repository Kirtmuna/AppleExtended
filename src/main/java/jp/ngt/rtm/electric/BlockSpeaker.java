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

import com.anatawa12.fixRtm.rtm.block.BlockOrnamentMain;
import jp.ngt.ngtlib.block.BlockArgHolder;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.block.BlockMachineBase;
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

public class BlockSpeaker extends BlockMachineBase implements IBlockConnective {
    public BlockSpeaker() {
        super(Material.GLASS);
        this.setSoundType(SoundType.GLASS);
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2) {
        return new TileEntitySpeaker();
    }

    @Override
    protected ItemStack getItem(int meta) {
        return new ItemStack(RTMItem.installedObject, 1, IstlObjType.SPEAKER.id);
    }

    @Override
    public boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        World world = holder.getWorld();
        BlockPos pos = holder.getBlockPos();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (holder.getPlayer().isSneaking()) {
            super.onBlockActivated(holder, hitX, hitY, hitZ);
        } else {
            if (world.isRemote) {
                holder.getPlayer().openGui(RTMCore.instance, RTMCore.instance.guiIdSpeaker, world, x, y, z);
            }
        }
        return true;
    }

    @Override
    public boolean canConnect(World world, int x, int y, int z) {
        return true;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, IstlObjType.SPEAKER);
    }
}