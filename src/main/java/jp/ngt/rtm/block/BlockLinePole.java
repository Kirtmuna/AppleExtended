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
import jp.ngt.rtm.RTMBlock;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.block.tileentity.TileEntityPole;
import jp.ngt.rtm.item.ItemInstalledObject;
import jp.ngt.rtm.item.ItemInstalledObject.IstlObjType;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockLinePole extends BlockContainerCustom {
    public BlockLinePole() {
        super(Material.ROCK);
        this.setHardness(2.0F);
        this.setResistance(10.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int par2) {
        return new TileEntityPole();
    }

    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {

        return true;
    }

    @Override
    protected ItemStack getItem(int damage) {
        if (this == RTMBlock.linePole) {
            return new ItemStack(RTMItem.installedObject, 1, IstlObjType.LINEPOLE.id);
        } else {
            return new ItemStack(RTMBlock.framework, 1, damage);
        }
    }

	/*@SideOnly(Side.CLIENT)
    public void getSubBlocks(Item par1, CreativeTabs tab, List list)
    {
		if(this == RTMBlock.linePole)
		{
			;
		}
		else
		{
			for(int i = 0; i < 16; ++i)
			{
				list.add(new ItemStack(par1, 1, i));
			}
		}
    }*/

    /**
     * 接続先の座標を指定
     *
     * @param type 0:同一ブロックのみ, 1:不透明ブロック, 2:全ブロック
     *
     */
    public static boolean isConnected(IBlockAccess world, int x, int y, int z, int type) {
        IBlockState state = world.getBlockState(new BlockPos(x, y, z));
        Block block = state.getBlock();
        boolean isPole = (block == RTMBlock.linePole || block == RTMBlock.framework);
        if (type == 0) {
            return isPole;
        } else if (type == 1) {
            return isPole || state.isOpaqueCube();
        } else if (type == 2) {
            Material material = state.getMaterial();
            return !(material == Material.AIR || material.isLiquid());
        }
        return true;
    }

    @Override
    protected boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        return BlockOrnamentMain.onBlockActivated(holder);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return BlockOrnamentMain.getPickBlock(world, pos, ItemInstalledObject.IstlObjType.LINEPOLE);
    }
}