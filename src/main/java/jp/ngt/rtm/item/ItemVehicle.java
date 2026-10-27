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

import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.item.ItemArgHolderBase.ItemArgHolder;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.util.PermissionManager;
import jp.ngt.rtm.RTMBlock;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.block.tileentity.MechanismType;
import jp.ngt.rtm.block.tileentity.TileEntityMechanism;
import jp.ngt.rtm.entity.vehicle.*;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.block.BlockRailBase;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemVehicle extends ItemWithModel {
    public ItemVehicle() {
        super();
    }

    @Override
    protected ActionResult<ItemStack> onItemRightClick(ItemArgHolder holder) {
        ItemStack itemStack = holder.getItemStack();
        World world = holder.getWorld();
        EntityPlayer player = holder.getPlayer();

        if (itemStack.getItemDamage() == VehicleType.SHIP.id) {
            RayTraceResult mop = BlockUtil.getMOPFromPlayer(player, 5.0D, true);
            if (mop != null && mop.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos pos = mop.getBlockPos();
                if (world.getBlockState(pos).getMaterial().isLiquid()) {
                    if (!world.isRemote) {
                        int x = pos.getX();
                        int y = pos.getY();
                        int z = pos.getZ();
                        this.setVehicle(itemStack, world, player, new EntityShip(world), x, y, z);
                    }
                    return holder.success();
                }
            }
        }

        return super.onItemRightClick(holder);
    }

    @Override
    protected ActionResult<ItemStack> onItemUse(ItemArgHolder holder, float hitX, float hitY, float hitZ) {
        ItemStack itemStack = holder.getItemStack();
        World world = holder.getWorld();
        BlockPos pos = holder.getBlockPos();
        EntityPlayer player = holder.getPlayer();

        if (!world.isRemote) {
            int damage = itemStack.getItemDamage();
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            if (damage == VehicleType.CAR.id) {
                if (holder.getFacing() == EnumFacing.UP) {
                    this.setVehicle(itemStack, world, player, new EntityCar(world), x, y, z);
                }
            } else if (damage == VehicleType.PLANE.id) {
                if (holder.getFacing() == EnumFacing.UP) {
                    this.setVehicle(itemStack, world, player, new EntityPlane(world), x, y, z);
                }
            } else if (damage == VehicleType.TROLLEY.id) {
                if (holder.getFacing() == EnumFacing.UP) {
                    if (world.getBlockState(pos).getBlock() instanceof BlockRailBase) {
                        EntityTrolley entity = new EntityTrolley(world);
                        this.setTrolley(itemStack, world, player, entity, x, y, z);
                    }
                }
            } else if (damage == VehicleType.LIFT.id) {
                if (world.getBlockState(pos).getBlock() == RTMBlock.mechanism) {
                    TileEntity tileEntity = world.getTileEntity(pos);
                    if (tileEntity instanceof TileEntityMechanism) {
                        TileEntityMechanism mecha = (TileEntityMechanism) tileEntity;
                        if (mecha.getType() == MechanismType.PULLEY) {
                            this.setLift(itemStack, world, player, new EntityLift(world), mecha);
                        }
                    }
                }
            }
        }

        return holder.success();
    }


    protected void setVehicle(ItemStack itemStack, World world, EntityPlayer player, EntityVehicle vehicle, int x, int y, int z) {
        if (PermissionManager.INSTANCE.hasPermission(player, RTMCore.EDIT_VEHICLE)) {
            vehicle.setPosition((double) x + 0.5D, (double) y + 1.0D, (double) z + 0.5D);
            vehicle.rotationYaw = NGTMath.wrapAngle(-player.rotationYaw);
            world.spawnEntity(vehicle);
            vehicle.getResourceState().readFromNBT(this.getModelState(itemStack).writeToNBT());
            vehicle.updateResourceState();
        }

        if (!player.capabilities.isCreativeMode) {
            itemStack.shrink(1);
        }
    }

    protected void setTrolley(ItemStack itemStack, World world, EntityPlayer player, EntityTrolley vehicle, int x, int y, int z) {
        if (PermissionManager.INSTANCE.hasPermission(player, RTMCore.EDIT_VEHICLE)) {
            vehicle.setPosition((double) x + 0.5D, (double) y + 0.0625D, (double) z + 0.5D);
            world.spawnEntity(vehicle);
            vehicle.getResourceState().readFromNBT(this.getModelState(itemStack).writeToNBT());
            vehicle.updateResourceState();
        }

        if (!player.capabilities.isCreativeMode) {
            itemStack.shrink(1);
        }
    }

    protected void setLift(ItemStack itemStack, World world, EntityPlayer player, EntityLift vehicle, TileEntityMechanism mecha) {
        if (PermissionManager.INSTANCE.hasPermission(player, RTMCore.EDIT_VEHICLE)) {
            LiftMotion motion = mecha.getMotion(vehicle, 0.0D, 0);
            vehicle.setPosition((double) motion.pos.getX(), (double) motion.pos.getY(), (double) motion.pos.getZ());
            vehicle.rotationYaw = motion.yaw;
            vehicle.rotationPitch = motion.pitch;
            vehicle.setMecha(motion.mecha);
            world.spawnEntity(vehicle);
            vehicle.getResourceState().readFromNBT(this.getModelState(itemStack).writeToNBT());
            vehicle.updateResourceState();
        }

        if (!player.capabilities.isCreativeMode) {
            itemStack.shrink(1);
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

        for (VehicleType type : VehicleType.values()) {
            list.add(new ItemStack(this, 1, type.id));
        }
    }

    @Override
    protected ResourceType getModelType(ItemStack itemStack) {
        int damage = itemStack.getItemDamage();
        for (VehicleType type : VehicleType.values()) {
            if (type.id == damage) {
                return type.type;
            }
        }
        return RTMResource.VEHICLE_CAR;
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
}