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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

public class TileEntitySoundRemover extends TileEntity implements ITickable {
    private boolean useRS = false;

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("UseRS", useRS);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.useRS = compound.getBoolean("UseRS");
    }

    @Override
    public void update() {
        if (!world.isRemote) {
            if (this.useRS && !world.isBlockPowered(pos)) {
                return;
            }
            AxisAlignedBB aabb = new AxisAlignedBB(pos.getX() - 3, pos.getY() - 3, pos.getZ() - 3,
                    pos.getX() + 4, pos.getY() + 4, pos.getZ() + 4);
            List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, aabb);

            for (Entity entity : entities) {
                if (entity instanceof EntityTrainBase) {
                    EntityTrainBase train = (EntityTrainBase) entity;
                    String currentSound = train.getEntityData().getString("arse_sound");

                    if (!currentSound.equals("none")) {
                        train.getEntityData().setString("arse_sound", "none");
                        train.getEntityData().setBoolean("arse_loop", false);

                        train.velocityChanged = true;

                        ARSE.network.sendToAll(
                                new PacketArsePlaySound(train, "none", false));

                        System.out.println("[ARSE] Force Stopped: " + train.getEntityId());
                    }
                }
            }
        }
    }

    public boolean isUseRS() {
        return useRS;
    }

    public void setUseRS(boolean useRS) {
        this.useRS = useRS;
        this.markDirty();

        if (this.world != null) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }
}