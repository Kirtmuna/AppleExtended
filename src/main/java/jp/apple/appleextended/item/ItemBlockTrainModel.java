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

package jp.apple.appleextended.item;

import jp.apple.appleextended.AppleLib;
import jp.apple.appleextended.tileentity.TileEntityTrainModel;
import jp.ngt.ngtlib.item.ItemArgHolderBase.ItemArgHolder;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.item.ItemWithModel;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemBlockTrainModel extends ItemWithModel {

    private final Block block;

    public ItemBlockTrainModel(Block block) {
        super();
        this.block = block;
        this.setRegistryName(block.getRegistryName());
        this.setUnlocalizedName(block.getUnlocalizedName());
        this.setCreativeTab(AppleLib.tabAppleLib);
    }

    @Override
    protected ResourceType getModelType(ItemStack itemStack) {

        return RTMResource.TRAIN_EC;
    }

    @Override
    protected ResourceState getNewState(ItemStack itemStack, ResourceType type) {

        ResourceState state = new ResourceState(type, null);
        state.setResourceName("Dummy");
        return state;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen newGuiScreen(ItemArgHolder holder) {

        return this.newGuiSelectModel(holder);
    }

    @Override
    protected ActionResult<ItemStack> onItemRightClick(ItemArgHolder holder) {

        return super.onItemRightClick(holder);
    }

    @Override
    protected ActionResult<ItemStack> onItemUse(ItemArgHolder holder, float hitX, float hitY, float hitZ) {
        World world = holder.getWorld();
        EntityPlayer player = holder.getPlayer();
        ItemStack stack = holder.getItemStack();
        BlockPos pos = holder.getBlockPos();
        EnumFacing facing = holder.getFacing();


        if (player.isSneaking()) {
            return super.onItemUse(holder, hitX, hitY, hitZ);
        }

        if (!world.isRemote) {
            BlockPos placePos = pos;
            if (!world.getBlockState(placePos).getBlock().isReplaceable(world, placePos)) {
                placePos = placePos.offset(facing);
            }

            if (stack.getCount() > 0 && player.canPlayerEdit(placePos, facing, stack) && world.mayPlace(this.block, placePos, false, facing, player)) {
                int meta = this.getMetadata(stack.getMetadata());
                IBlockState iblockstate = this.block.getStateForPlacement(world, placePos, facing, hitX, hitY, hitZ, meta, player, EnumHand.MAIN_HAND);

                if (world.setBlockState(placePos, iblockstate, 11)) {
                    iblockstate = world.getBlockState(placePos);
                    if (iblockstate.getBlock() == this.block) {
                        this.block.onBlockPlacedBy(world, placePos, iblockstate, player, stack);

                        TileEntity te = world.getTileEntity(placePos);
                        if (te instanceof TileEntityTrainModel) {
                            ResourceState itemState = this.getModelState(stack);
                            TileEntityTrainModel trainTe = (TileEntityTrainModel) te;

                            trainTe.getResourceState().readFromNBT(itemState.writeToNBT());

                            Block hitBlock = world.getBlockState(pos).getBlock();
                            if (hitBlock instanceof jp.ngt.rtm.rail.BlockLargeRailBase) {
                                trainTe.setOffset(0, 0.2325F, 0, false);
                            }

                            trainTe.markDirty();
                            world.notifyBlockUpdate(placePos, iblockstate, iblockstate, 3);
                        }
                    }
                    stack.shrink(1);
                    return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
                }
            }
        }
        return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
    }
}