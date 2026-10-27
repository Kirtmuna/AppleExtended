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

package jp.ngt.ngtlib.network;

import com.anatawa12.fixRtm.Deprecation;
import io.netty.buffer.ByteBuf;
import jp.ngt.ngtlib.NGTCore;
import jp.ngt.ngtlib.block.BlockUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketNBT extends PacketCustom {
    private static final byte Type_Entity = 0;
    private static final byte Type_TileEntity = 1;
    private static final byte Type_PlayerItem = 2;
    public NBTTagCompound nbtData;

    public PacketNBT() {
    }

    private PacketNBT(Entity entity, boolean toClient) {
        super(entity);
        NBTTagCompound nbt = new NBTTagCompound();
        entity.writeToNBT(nbt);
        this.initPacket(entity, nbt, toClient);
    }

    private PacketNBT(TileEntity tileEntity, NBTTagCompound nbt, boolean toClient) {
        super(tileEntity);
        this.initPacket(tileEntity, nbt, toClient);
    }

    private PacketNBT(TileEntity tileEntity, boolean toClient) {
        super(tileEntity);
        NBTTagCompound nbt = new NBTTagCompound();
        tileEntity.writeToNBT(nbt);
        this.initPacket(tileEntity, nbt, toClient);
    }

    private PacketNBT(EntityPlayer player, ItemStack stack) {
        super(player);
        this.nbtData = new NBTTagCompound();
        this.nbtData.setTag("TagData", stack.getTagCompound());
        this.nbtData.setBoolean("ToClient", false);
        this.nbtData.setByte("DataType", Type_PlayerItem);
    }

    private void initPacket(Entity entity, NBTTagCompound nbt, boolean toClient) {
        this.nbtData = nbt;
        this.nbtData.setBoolean("ToClient", toClient);
        this.nbtData.setByte("DataType", Type_Entity);
    }

    private void initPacket(TileEntity tileEntity, NBTTagCompound nbt, boolean toClient) {
        this.nbtData = nbt;
        this.nbtData.setBoolean("ToClient", toClient);
        this.nbtData.setByte("DataType", Type_TileEntity);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        ByteBufUtils.writeTag(buffer, this.nbtData);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        this.nbtData = ByteBufUtils.readTag(buffer);
    }

    protected boolean onGetPacket(World world) {
        if (world == null) {
            return false;
        }

        byte type = this.nbtData.getByte("DataType");
        if (type == Type_Entity) {
            Entity entity = this.getEntity(world);
            if (entity == null) {
            } else {
                entity.readFromNBT(this.nbtData);
                return true;
            }
        } else if (type == Type_TileEntity) {
            TileEntity tileEntity = this.getTileEntity(world);
            if (tileEntity == null) {
            } else {
                tileEntity.readFromNBT(this.nbtData);
                if (!world.isRemote) {
                    tileEntity.markDirty();
                    BlockUtil.markBlockForUpdate(tileEntity.getWorld(), tileEntity.getPos());
                }
                return true;
            }
        } else if (type == Type_PlayerItem) {
            Entity entity = this.getEntity(world);
            if (entity instanceof EntityPlayer) {
                ItemStack stack = ((EntityPlayer) entity).inventory.getCurrentItem();
                if (stack != null) {
                    int index = ((EntityPlayer) entity).inventory.currentItem;
                    NBTTagCompound data = this.nbtData.getCompoundTag("TagData");
                    stack.setTagCompound(data);
                    if (entity instanceof EntityPlayerMP) {
                        this.updateCurrentItem((EntityPlayerMP) entity);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void updateCurrentItem(EntityPlayerMP player) {
        Slot slot = player.inventoryContainer.getSlotFromInventory(player.inventory, player.inventory.currentItem);
        player.connection.sendPacket(new SPacketSetSlot(player.inventoryContainer.windowId, slot.slotNumber, player.inventory.getCurrentItem()));
    }

    public static void sendToServer(Entity entity) {
        NGTCore.NETWORK_WRAPPER.sendToServer(new PacketNBT(entity, false));
    }

    public static void sendToServer(EntityPlayer player, ItemStack item) {
        NGTCore.NETWORK_WRAPPER.sendToServer(new PacketNBT(player, item));
    }

    public static void sendToServer(TileEntity entity) {
        NGTCore.NETWORK_WRAPPER.sendToServer(new PacketNBT(entity, false));
    }

    public static void sendToServer(TileEntity entity, NBTTagCompound nbt) {
        NGTCore.NETWORK_WRAPPER.sendToServer(new PacketNBT(entity, nbt, false));
    }

    public static void sendToClient(final Entity entity) {
        NGTCore.NETWORK_WRAPPER.sendToAll(new PacketNBT(entity, true));
    }

    public static void sendToClient(Entity entity, EntityPlayerMP player) {
        NGTCore.NETWORK_WRAPPER.sendTo(new PacketNBT(entity, true), player);
    }

    @Deprecated
    public static void sendToClient(final TileEntity entity) {
        Deprecation.found("PacketNBT#sendToClient");
        NGTCore.NETWORK_WRAPPER.sendToAll(new PacketNBT(entity, true));
    }
}