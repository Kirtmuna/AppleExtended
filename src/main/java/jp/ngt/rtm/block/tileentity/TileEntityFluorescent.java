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

import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.modelpack.ResourceType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.MathHelper;

public class TileEntityFluorescent extends TileEntityOrnament implements ITickable {
    private int count = 0;
    public byte dirF;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.dirF = nbt.getByte("dir");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("dir", this.dirF);
        return nbt;
    }

    public byte getDir() {
        return this.dirF;
    }

    public void setDir(byte byte0) {
        this.dirF = byte0;

    }

    @Override
    public void update() {
        if (this.getBlockMetadata() == 2) {
            ++this.count;
            if (this.count == 3) {
                //明るさ更新
                this.world.checkLight(this.getPos());
                this.count = 0;
            }
        }
    }

    @Override
    public ResourceType getSubType() {
        return RTMResource.ORNAMENT_LAMP;
    }

    @Override
    public void setRotation(EntityPlayer player, float rotationInterval, boolean synch) {
        int yaw = MathHelper.floor(NGTMath.normalizeAngle(-player.rotationYaw + 180.0D + (rotationInterval / 2.0D)) / (double) rotationInterval);
        if (this.dirF >= 4) {
            yaw += 90 / rotationInterval;
        }
        if (this.dirF == 1 || this.dirF == 7) {
            yaw += 180 / rotationInterval;
        }
        this.setRotation((float) yaw * rotationInterval, synch);
    }
}