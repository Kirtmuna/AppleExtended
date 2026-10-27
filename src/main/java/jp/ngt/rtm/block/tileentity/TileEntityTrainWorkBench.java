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

package jp.ngt.rtm.block.tileentity;

import jp.ngt.ngtlib.block.TileEntityCustom;
import jp.ngt.ngtlib.item.ItemUtil;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.gui.ContainerRTMWorkBench;
import jp.ngt.rtm.network.PacketNotice;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ITickable;

import java.util.Arrays;

public class TileEntityTrainWorkBench extends TileEntityCustom implements ITickable {
    private ItemStack[] craftSlots = ItemUtil.getEmptyArray(30);

    public static final int Max_CraftingTime = 64;
    private int craftingTime = 0;
    private boolean isCrafting = false;
    private boolean isCreative = false;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        IBlockState state = getWorld().getBlockState(getPos());
        this.getWorld().notifyBlockUpdate(this.pos, state, state, 0);
        NBTTagList nbttaglist = nbt.getTagList("Items", 10);
        Arrays.fill(this.craftSlots, ItemStack.EMPTY);
        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            NBTTagCompound nbt1 = nbttaglist.getCompoundTagAt(i);
            int j = nbt1.getByte("Slot") & 255;
            if (j >= 0 && j < this.craftSlots.length) {
                this.craftSlots[j] = new ItemStack(nbt1);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        NBTTagList tagList = new NBTTagList();
        for (int i = 0; i < this.craftSlots.length; ++i) {
            if (this.craftSlots[i] != null) {
                NBTTagCompound nbt1 = new NBTTagCompound();
                nbt1.setByte("Slot", (byte) i);
                this.craftSlots[i].writeToNBT(nbt1);
                tagList.appendTag(nbt1);
            }
        }
        nbt.setTag("Items", tagList);
        return nbt;
    }

    @Override
    public void update() {
        if (this.isCrafting) {
            if (this.craftingTime < Max_CraftingTime) {
                if (this.isCreative) {
                    this.craftingTime = Max_CraftingTime;
                } else {
                    ++this.craftingTime;
                }
            } else {
                this.craftingTime = 0;
                this.isCrafting = false;
            }
        }
    }

    public void readItemsFromTile(IInventory inventory, IInventory inv2) {
        for (int i = 0; i < 25; ++i) {
            inventory.setInventorySlotContents(i, this.craftSlots[i]);
        }

        for (int i = 25; i < 30; ++i) {
            inv2.setInventorySlotContents(i - 25, this.craftSlots[i]);
        }
    }

    public void writeItemsToTile(IInventory inventory, IInventory inv2) {
        for (int i = 0; i < this.craftSlots.length; ++i) {
            this.craftSlots[i] = inventory.getStackInSlot(i);
        }

        for (int i = 25; i < 30; ++i) {
            this.craftSlots[i] = inv2.getStackInSlot(i - 25);
        }

        this.sendPacket();
    }

    public int getCraftingTime() {
        return this.craftingTime;
    }

    public void setCraftingTime(int par1) {
        this.craftingTime = par1;
    }

    public void startCrafting(EntityPlayer player, boolean sendPacket) {
        this.craftingTime = 0;
        this.isCrafting = true;
        this.isCreative = NGTUtil.isServer() && player.capabilities.isCreativeMode;

        ContainerRTMWorkBench container = (ContainerRTMWorkBench) player.openContainer;
        container.startCrafting();

        if (sendPacket) {
            String s = "StartCrafting";
            RTMCore.NETWORK_WRAPPER.sendToServer(new PacketNotice(PacketNotice.Side_SERVER, s, this));
        }
    }

    public boolean isCrafting() {
        return this.isCrafting;
    }
}