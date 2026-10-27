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

package jp.ngt.rtm.item;

import jp.ngt.ngtlib.item.ItemArgHolderBase.ItemArgHolder;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.math.PooledVec3;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.RTMBlock;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.cfg.RailConfig;
import jp.ngt.rtm.modelpack.cfg.RailConfig.BallastSet;
import jp.ngt.rtm.modelpack.modelset.ResourceSet;
import jp.ngt.rtm.modelpack.state.ResourceState;
import jp.ngt.rtm.modelpack.state.ResourceStateRail;
import jp.ngt.rtm.rail.BlockLargeRailBase;
import jp.ngt.rtm.rail.BlockMarker;
import jp.ngt.rtm.rail.TileEntityLargeRailCore;
import jp.ngt.rtm.rail.util.RailPosition;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemRail extends ItemWithModel {
    public ItemRail() {
        super();
        this.setMaxStackSize(1);
    }

    @Override
    protected ActionResult<ItemStack> onItemUse(ItemArgHolder holder, float hitX, float hitY, float hitZ) {
        World world = holder.getWorld();
        IBlockState blockState = world.getBlockState(holder.getBlockPos());
        if (blockState.getBlock() instanceof BlockMarker) {
            return holder.pass();
        }

        if (world.isRemote) {
        } else {
            TileEntityLargeRailCore core = BlockLargeRailBase.getCore(world, holder.getBlockPos());
            if (core != null) {
                ResourceStateRail state = this.getModelState(holder.getItemStack());
                if (holder.getPlayer().isSneaking()) {
                    core.replaceRail(state);
                } else {
                    core.addSubRail(state);
                }
            } else {
                BlockPos newPos = holder.getBlockPos().up();
                this.placeRail(world, newPos, holder.getItemStack(), holder.getPlayer());
            }
        }
        return holder.success();
    }

    @Override
    public ResourceStateRail getModelState(ItemStack itemStack) {
        ResourceType type = this.getModelType(itemStack);
        if (type != null) {
            ResourceStateRail state = new ResourceStateRail(type, null);
            if (itemStack.hasTagCompound()) {
                state.readFromNBT(itemStack.getTagCompound().getCompoundTag("State"));
            } else {
                state.setResourceName(type.defaultName);
                NBTTagCompound nbt = new NBTTagCompound();
                nbt.setTag("State", state.writeToNBT());
                itemStack.setTagCompound(nbt);
            }
            return state;
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
        if (!this.isInCreativeTab(tab)) {
            return;
        }

        List<ResourceSet> rails = ModelPackManager.INSTANCE.getModelList(RTMResource.RAIL);
        for (ResourceSet modelSet : rails) {
            RailConfig cfg = (RailConfig) modelSet.getConfig();
            if (cfg.defaultBallast == null) {
                continue;
            }

            for (BallastSet set : cfg.defaultBallast) {
                Block block = Block.getBlockFromName(set.blockName);
                int meta = set.blockMetadata;
                float h = set.height <= 0.0F ? 0.0625F : set.height;
                if (block == null) {
                    block = Blocks.AIR;
                }

                ItemStack itemStack = new ItemStack(RTMItem.itemLargeRail, 1, 0);
                ResourceStateRail state = (ResourceStateRail) this.getModelState(itemStack);
                state.setResourceName(cfg.getName());
                state.setBlock(block, meta);
                state.setHeight(h);
                this.setModelState(itemStack, state);
                list.add(itemStack);
            }
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack itemStack) {
        String s = super.getItemStackDisplayName(itemStack);
        ResourceStateRail state = (ResourceStateRail) this.getModelState(itemStack);
        if (state == null) {
            return s;
        }

        String localizedName = "";
        if (I18n.canTranslate(state.unlocalizedName)) {
            localizedName = ", " + NGTUtil.translate(state.unlocalizedName);
        }
        return s + "(" + state.getResourceSet().getConfig().getName() + localizedName + ")";
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected void addInformation(ItemArgHolder holder, List list, ITooltipFlag flag) {
        super.addInformation(holder, list, flag);
        ResourceStateRail state = (ResourceStateRail) this.getModelState(holder.getItemStack());
        if (state == null) {
            return;
        }
        list.add(TextFormatting.GRAY + "Height:" + state.blockHeight);

        if (holder.getItemStack().hasTagCompound() && holder.getItemStack().getTagCompound().hasKey("ShapeName")) {
            String shape = holder.getItemStack().getTagCompound().getString("ShapeName");
            list.add(TextFormatting.GRAY + shape);
        }
    }

    public static ItemStack getRailItem(ResourceStateRail prop) {
        ItemStack itemStack = new ItemStack(RTMItem.itemLargeRail, 1, 0);
        ((ItemRail) RTMItem.itemLargeRail).setModelState(itemStack, prop);
        return itemStack;
    }

    public static ResourceStateRail getDefaultProperty() {
        ResourceStateRail state = new ResourceStateRail(RTMResource.RAIL, null);
        state.setResourceToDefault();
        return state;
    }

    @Override
    protected ResourceType getModelType(ItemStack itemStack) {
        return RTMResource.RAIL;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen newGuiScreen(ItemArgHolder holder) {
        return newGuiSelectModel(holder);
    }

    public int getGuiId(ItemStack stack) {
        return 0;
    }

    @Override
    protected ResourceState getNewState(ItemStack itemStack, ResourceType type) {
        return new ResourceStateRail(type, (Object) null);
    }

    private static List<RailPosition> getRPFromItem(ItemStack stack) {
        List<RailPosition> list = new ArrayList<>();
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null) {
            byte size = nbt.getByte("Size");
            for (int i = 0; i < size; ++i) {
                list.add(RailPosition.readFromNBT(nbt.getCompoundTag("RP" + i)));
            }
        }
        return list;
    }

    private static void setRPToItem(ItemStack stack, RailPosition[] rps) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = stack.getTagCompound();

        nbt.setByte("Size", (byte) rps.length);
        for (int i = 0; i < rps.length; ++i) {
            nbt.setTag("RP" + i, rps[i].writeToNBT());
        }
    }

    public static ItemStack copyItemFromRail(TileEntityLargeRailCore core) {
        ItemStack itemstack = getRailItem(core.getResourceState());
        RailPosition[] arailposition = core.getLogicalRailPositions();
        if (arailposition == null) return ItemStack.EMPTY;
        setRPToItem(itemstack, arailposition);
        String s = core.getRailShapeName();
        itemstack.getTagCompound().setString("ShapeName", s);
        return itemstack;
    }

    private boolean placeRail(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        List<RailPosition> list = getRPFromItem(stack);
        if (list.isEmpty()) {
            return false;
        } else {
            int i = BlockMarker.getMarkerDir(RTMBlock.marker, com.anatawa12.fixRtm.rtm.rail.BlockMarker.getFaceMeta(player));
            RailPosition railposition = list.get(0);
            int j = i - railposition.direction;
            int k = railposition.blockX;
            int l = railposition.blockY;
            int i1 = railposition.blockZ;
            for (RailPosition rp : list) {
                double dif2X = (rp.blockX + 0.5D) - (k + 0.5D);
                double dif2Y = (rp.blockY + 0.5D) - (l + 0.5D);
                double dif2Z = (rp.blockZ + 0.5D) - (i1 + 0.5D);
                Vec3 vec = PooledVec3.create(dif2X, dif2Y, dif2Z);
                vec = vec.rotateAroundY(j * 45.0F);
                rp.blockX = NGTMath.floor(pos.getX() + 0.5D + vec.getX());
                rp.blockY = NGTMath.floor(pos.getY() + 0.5D + vec.getY());
                rp.blockZ = NGTMath.floor(pos.getZ() + 0.5D + vec.getZ());
                rp.direction = (byte) ((rp.direction + j + 8) & 7);
                rp.anchorYaw = NGTMath.wrapAngle(rp.anchorYaw + j * 45.0F);
                rp.init();
            }
            ResourceStateRail state = this.getModelState(stack);
            boolean isCreative = player.capabilities.isCreativeMode;
            if (list.size() == 2 && list.get(0).blockY == list.get(1).blockY) {
                Collections.reverse(list);
            }
            return BlockMarker.createRail(world, pos.getX(), pos.getY(), pos.getZ(), list, state, true, isCreative);
        }
    }
}