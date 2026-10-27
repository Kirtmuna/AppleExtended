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

import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.item.ItemArgHolderBase.ItemArgHolder;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.math.PooledVec3;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.ngtlib.util.PermissionManager;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.entity.train.*;
import jp.ngt.rtm.entity.train.parts.EntityVehiclePart;
import jp.ngt.rtm.entity.train.util.FormationEntry;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import jp.ngt.rtm.modelpack.state.ResourceState;
import jp.ngt.rtm.rail.TileEntityLargeRailBase;
import jp.ngt.rtm.rail.util.RailMap;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public final class ItemTrain extends ItemWithModel {
    private static final int SPLIT = 128;

    public ItemTrain() {
        super();
    }

    @Override
    protected ActionResult<ItemStack> onItemRightClick(ItemArgHolder holder) {
        if (holder.getWorld().isRemote) {
            List<TrainSet> trainSets = getFormationFromItem(holder.getItemStack());
            if (!trainSets.isEmpty()) {
                return holder.success();
            }
            return super.onItemRightClick(holder);
        }
        return holder.success();
    }

    @Override
    protected ActionResult<ItemStack> onItemUse(ItemArgHolder holder, float hitX, float hitY, float hitZ) {
        ItemStack itemstack = holder.getItemStack();
        World world = holder.getWorld();
        BlockPos blockpos = holder.getBlockPos();
        EntityPlayer entityplayer = holder.getPlayer();

        if (world.isRemote || !PermissionManager.INSTANCE.hasPermission(entityplayer, RTMCore.EDIT_VEHICLE)) {
            return holder.success();
        }

        int i = blockpos.getX();
        int j = blockpos.getY();
        int k = blockpos.getZ();
        ResourceState resourcestate = this.getModelState(itemstack);

        RailMap railmap = TileEntityLargeRailBase.getRailMapFromCoordinates(world, entityplayer, i, j, k);
        if (railmap == null) {
            return holder.success();
        } else if (!checkObstacle((TrainConfig) resourcestate.getResourceSet().getConfig(), entityplayer, world, i, j, k, railmap)) {
            return holder.success();
        } else {
            PosRotation pr = computePosRotation(railmap, i, k, -entityplayer.rotationYaw);
            this.doSpawn(pr, world, itemstack, resourcestate);

            itemstack.shrink(1);
            return holder.success();
        }
    }

    public static class PosRotation {
        public final float yaw, pitch;
        public final double posX, posY, posZ;

        public PosRotation(float yaw, float pitch, double posX, double posY, double posZ) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }
    }

    public static PosRotation computePosRotation(RailMap railmap, int i, int k, float yaw) {
        int l = railmap.getNearlestPoint(128, (double) i + 0.5D, (double) k + 0.5D);
        float f = NGTMath.wrapAngle(railmap.getRailYaw(128, l));
        float f1 = EntityBogie.fixBogieYaw(yaw, f);
        float f2 = EntityBogie.fixBogiePitch(railmap.getRailPitch(128, l), f, f1);
        double d0 = railmap.getRailPos(128, l)[1];
        double d1 = railmap.getRailHeight(128, l);
        double d2 = railmap.getRailPos(128, l)[0];
        return new PosRotation(f1, f2, d0, d1, d2);
    }

    private void doSpawn(PosRotation pr, World world, ItemStack itemstack, ResourceState resourcestate) {
        float f1 = pr.yaw, f2 = pr.pitch;
        double d0 = pr.posX, d1 = pr.posY, d2 = pr.posZ;
        List<TrainSet> list = getFormationFromItem(itemstack);
        if (list.isEmpty()) {
            EntityTrainBase entitytrainbase = this.getTrain(itemstack, world);
            entitytrainbase.setPositionAndRotation(d0, d1, d2, f1, f2);
            entitytrainbase.getResourceState().readFromNBT(resourcestate.writeToNBT());
            entitytrainbase.spawnTrain(world);
            entitytrainbase.updateResourceState();
        } else {
            for (TrainSet trainset : list) {
                Vec3 vec3 = PooledVec3.create((double) trainset.posX, (double) trainset.posY, (double) trainset.posZ);
                vec3 = vec3.rotateAroundY(f1);
                EntityTrainBase entitytrainbase1 = this.getTrain(itemstack, world);
                entitytrainbase1.setPositionAndRotation(d0 + vec3.getX(), d1 + vec3.getY(), d2 + vec3.getZ(), f1 + trainset.yaw, f2 + trainset.pitch);
                entitytrainbase1.getResourceState().setResourceName(trainset.modelName);
                entitytrainbase1.spawnTrain(world);
                entitytrainbase1.updateResourceState();
                entitytrainbase1.getBogie(0).isActivated = true;
                entitytrainbase1.getBogie(1).isActivated = true;
                entitytrainbase1.setNotch(1);
            }
        }
    }

    public static boolean checkObstacle(TrainConfig cfg, EntityPlayer player, World world, int x, int y, int z, RailMap rm0) {
        float f = cfg.trainDistance + 4.0F;

        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(player, new AxisAlignedBB((double) ((float) x - f), (double) (y - 4), (double) ((float) z - f), (double) ((float) x + f + 1.0F), (double) (y + 8), (double) ((float) z + f + 1.0F)))) {
            if (entity instanceof EntityTrainBase || entity instanceof EntityBogie || entity instanceof EntityVehiclePart) {
                double d0 = entity.getDistanceSq((double) x, (double) y, (double) z);
                RailMap railmap = TileEntityLargeRailBase.getRailMapFromCoordinates(world, player, entity.posX, entity.posY, entity.posZ);
                if (d0 < (double) (f * f) && rm0.equals(railmap)) {
                    NGTLog.sendChatMessage(player, "message.train.obstacle", new Object[]{entity.toString()});
                    return false;
                }
            }
        }

        return true;
    }

    private EntityTrainBase getTrain(ItemStack itemStack, World world) {
        switch (itemStack.getItemDamage()) {
            case 1:
                return new EntityTrainElectricCar(world, "");
            case 2:
                return new EntityFreightCar(world, "");
            case 3:
                return new EntityTanker(world, "");
            case 127:
                return new EntityTrainTest(world, "");
            default:
                return new EntityTrainDieselCar(world, "");
        }
    }

    @Override
    public String getUnlocalizedName(ItemStack par1) {
        return this.getUnlocalizedName() + "." + par1.getItemDamage();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
        if (!this.isInCreativeTab(tab)) {
            return;
        }

        list.add(new ItemStack(this, 1, 0));
        list.add(new ItemStack(this, 1, 1));
        list.add(new ItemStack(this, 1, 2));
        list.add(new ItemStack(this, 1, 3));
        list.add(new ItemStack(this, 1, 127));
    }

    @Override
    protected ResourceType getModelType(ItemStack itemStack) {
        switch (itemStack.getItemDamage()) {
            case 0:
                return RTMResource.TRAIN_DC;
            case 1:
                return RTMResource.TRAIN_EC;
            case 2:
                return RTMResource.TRAIN_CC;
            case 3:
                return RTMResource.TRAIN_TC;
            case 127:
                return RTMResource.TRAIN_TEST;
            default:
                return RTMResource.TRAIN_DC;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen newGuiScreen(ItemArgHolder holder) {
        return newGuiSelectModel(holder);
    }

    public int getGuiId(ItemStack stack) {
        return 0;
    }

    protected ResourceState getNewState(ItemStack itemStack, ResourceType type) {
        return new ResourceState(type, (Object) null);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addInformation(ItemArgHolder holder, List list, ITooltipFlag flag) {
        List<TrainSet> list1 = getFormationFromItem(holder.getItemStack());
        if (list1.isEmpty()) {
            super.addInformation(holder, list, flag);
        } else {
            StringBuilder stringbuilder = new StringBuilder("{");

            for (TrainSet itemtrain$trainset : list1) {
                stringbuilder.append(itemtrain$trainset.modelName);
                stringbuilder.append(",");
            }

            stringbuilder.append("}");
            list.add(TextFormatting.GRAY + stringbuilder.toString());
        }
    }

    private static ItemStack getItem(ResourceType type) {
        if (type == RTMResource.TRAIN_DC) {
            return new ItemStack(RTMItem.itemtrain, 1, 0);
        } else if (type == RTMResource.TRAIN_EC) {
            return new ItemStack(RTMItem.itemtrain, 1, 1);
        } else if (type == RTMResource.TRAIN_CC) {
            return new ItemStack(RTMItem.itemtrain, 1, 2);
        } else if (type == RTMResource.TRAIN_TC) {
            return new ItemStack(RTMItem.itemtrain, 1, 3);
        } else if (type == RTMResource.TRAIN_TEST) {
            return new ItemStack(RTMItem.itemtrain, 1, 127);
        }
        return new ItemStack(RTMItem.itemtrain, 1, 0);
    }

    public static ItemStack convertFormationAsItem(EntityTrainBase train) {
        ItemStack stack = getItem(train.getResourceState().type);
        NBTTagList tagList = new NBTTagList();
        for (FormationEntry entry : train.getFormation().entries) {
            if (entry != null) {
                EntityTrainBase train2 = entry.train;
                Vec3 vec = PooledVec3.create(train2.posX - train.posX, train2.posY - train.posY, train2.posZ - train.posZ);
                vec = vec.rotateAroundY(NGTMath.wrapAngle(-train.rotationYaw));
                TrainSet set = new TrainSet(train2.getResourceState().getResourceName(), entry.entryId,
                        (float) vec.getX(), (float) vec.getY(), (float) vec.getZ(), (float) (train2.rotationYaw - train.rotationYaw), (float) (train2.rotationPitch - train.rotationPitch));
                tagList.appendTag(set.writeToNBT());
            }
        }
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("formations", tagList);
        stack.setTagCompound(nbt);
        return stack;
    }

    public static List<TrainSet> getFormationFromItem(ItemStack stack) {
        List<TrainSet> list = new ArrayList<>();
        if (stack.hasTagCompound()) {
            NBTTagList tagList = stack.getTagCompound().getTagList("formations", 10);
            for (int i = 0; i < tagList.tagCount(); ++i) {
                list.add(TrainSet.readFromNBT(tagList.getCompoundTagAt(i)));
            }
        }
        return list;
    }

    public static class TrainSet {
        public final String modelName;
        public final int index;
        public final float posX, posY, posZ;
        public final float yaw, pitch;

        public TrainSet(String p1, int p2, float p3, float p4, float p5, float p6, float p7) {
            this.modelName = p1;
            this.index = p2;
            this.posX = p3;
            this.posY = p4;
            this.posZ = p5;
            this.yaw = p6;
            this.pitch = p7;
        }

        public NBTTagCompound writeToNBT() {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setString("model", this.modelName);
            nbt.setInteger("index", this.index);
            nbt.setFloat("pos_x", this.posX);
            nbt.setFloat("pos_y", this.posY);
            nbt.setFloat("pos_z", this.posZ);
            nbt.setFloat("yaw", this.yaw);
            nbt.setFloat("pitch", this.pitch);
            return nbt;
        }

        public static TrainSet readFromNBT(NBTTagCompound nbt) {
            String s = nbt.getString("model");
            int i = nbt.getInteger("index");
            float f0 = nbt.getFloat("pos_x");
            float f1 = nbt.getFloat("pos_y");
            float f2 = nbt.getFloat("pos_z");
            float f3 = nbt.getFloat("yaw");
            float f4 = nbt.getFloat("pitch");
            return new TrainSet(s, i, f0, f1, f2, f3, f4);
        }
    }
}