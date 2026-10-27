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
import jp.ngt.ngtlib.block.BlockSet;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.RTMMaterial;
import jp.ngt.rtm.RTMSound;
import jp.ngt.rtm.block.tileentity.TileEntityPipe;
import jp.ngt.rtm.item.ItemInstalledObject;
import jp.ngt.rtm.item.ItemInstalledObject.IstlObjType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.util.List;

public class BlockPipe extends BlockContainerCustomWithMeta {
    public BlockPipe() {
        super(RTMMaterial.fireproof);
        this.setLightOpacity(0);
        this.setHardness(2.0F);
        this.setResistance(10.0F);
        this.setSoundType(RTMSound.SOUND_METAL2);
    }

    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
        return new TileEntityPipe();
    }

    @Override
    protected void neighborChanged(BlockArgHolder holder) {
        super.neighborChanged(holder);
        TileEntityPipe tile = (TileEntityPipe) BlockUtil.getTileEntity(holder.getWorld(), holder.getBlockPos());
        tile.refresh();
    }

    public List<BlockSet> setLiquid(World world, int x, int y, int z, int fromX, int fromY, int fromZ, List<BlockSet> list, int count) {
        if (count > 255) {
            return list;
        }

        int x0;
        int y0;
        int z0;

        TileEntityPipe tile = (TileEntityPipe) BlockUtil.getTileEntity(world, x, y, z);
        for (int i = 0; i < 6; ++i) {
            x0 = x + BlockUtil.facing[i][0];
            y0 = y + BlockUtil.facing[i][1];
            z0 = z + BlockUtil.facing[i][2];
            if (!(x0 == fromX && y0 == fromY && z0 == fromZ)) {
                if (tile.connection[i] == 3) {
                    Block block = BlockUtil.getBlock(world, x0, y0, z0);
                    int m0 = BlockUtil.getMetadata(world, x0, y0, z0);
                    BlockSet bs = new BlockSet(x0, y0, z0, block, m0);
                    if (!list.contains(bs)) {
                        list.add(bs);
                    }
                } else if (tile.connection[i] == 2) {
                    this.setLiquid(world, x0, y0, z0, x, y, z, list, ++count);
                }
            }
        }
        return list;
    }

    @Override
    public String getHarvestTool(IBlockState state) {
        return "pickaxe";
    }

    @Override
    public int getHarvestLevel(IBlockState state) {
        return 0;
    }

    @Override
    protected ItemStack getItem(int damage) {
        return new ItemStack(RTMItem.installedObject, 1, IstlObjType.PIPE.id);
    }

    @Override
    protected boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        return BlockOrnamentMain.onBlockActivated(holder);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.PIPE);
    }
}