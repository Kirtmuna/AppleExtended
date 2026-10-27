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

package jp.apple.appleextended.arse.tileentity;

import jp.apple.appleextended.arse.core.ARSE;
import jp.apple.appleextended.arse.network.PacketArsePlaySound;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

public class TileEntitySounder extends TileEntity implements ITickable {
    private String selectedSound = "minecraft:block.note.pling";
    private String selectedSound2 = "minecraft:block.note.pling";
    private boolean useRSIF = false;
    private boolean loop = true;

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setString("SelectedSound", selectedSound);
        compound.setString("SelectedSound2", selectedSound2);
        compound.setBoolean("UseRSIF", useRSIF);
        compound.setBoolean("Loop", loop);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.selectedSound = compound.getString("SelectedSound");
        this.selectedSound2 = compound.getString("SelectedSound2");
        this.useRSIF = compound.getBoolean("UseRSIF");
        this.loop = compound.getBoolean("Loop");


        if (this.selectedSound.isEmpty())
            this.selectedSound = "minecraft:block.note.pling";
        if (this.selectedSound2.isEmpty())
            this.selectedSound2 = "minecraft:block.note.pling";
    }

    @Override
    public void update() {
        if (!world.isRemote) {

            String targetSound = selectedSound;
            if (this.useRSIF) {

                targetSound = world.isBlockPowered(pos) ? selectedSound : selectedSound2;
            }

            AxisAlignedBB aabb = new AxisAlignedBB(pos.getX() - 3, pos.getY() - 3, pos.getZ() - 3,
                    pos.getX() + 4, pos.getY() + 4, pos.getZ() + 4);
            List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, aabb);

            for (Entity entity : entities) {
                if (entity instanceof EntityTrainBase) {
                    EntityTrainBase train = (EntityTrainBase) entity;
                    String currentSound = train.getEntityData().getString("arse_sound");

                    if (!currentSound.equals(targetSound)) {
                        ARSE.network.sendToAll(new PacketArsePlaySound(train, targetSound, loop));
                        train.getEntityData().setString("arse_sound", targetSound);
                        train.getEntityData().setBoolean("arse_loop", loop);
                        train.velocityChanged = true;
                    }
                }
            }
        }
    }

    public String getSelectedSound() {
        return selectedSound;
    }

    public void setSelectedSound(String sound) {
        this.selectedSound = sound;
        this.markDirty();
        this.sync();
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
        this.markDirty();
        this.sync();
    }

    public String getSelectedSound2() {
        return selectedSound2;
    }

    public void setSelectedSound2(String sound) {
        this.selectedSound2 = sound;
        markDirty();
        sync();
    }

    public boolean isUseRSIF() {
        return useRSIF;
    }

    public void setUseRSIF(boolean use) {
        this.useRSIF = use;
        markDirty();
        sync();
    }

    private void sync() {
        if (this.world != null) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }


    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        this.writeToNBT(nbt);
        return new SPacketUpdateTileEntity(this.pos, 1, nbt);
    }


    @Override
    public void onDataPacket(NetworkManager net,
                             SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }


    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }
}