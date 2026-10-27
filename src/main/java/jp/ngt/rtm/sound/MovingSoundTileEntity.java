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

package jp.ngt.rtm.sound;

import jp.apple.appleextended.config.AppleConfig;
import jp.apple.appleextended.sound.SoundRangeHelper;
import jp.ngt.ngtlib.sound.MovingSoundCustom;
import jp.ngt.rtm.RTMCore;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MovingSoundTileEntity extends MovingSoundCustom {
    protected final TileEntity entity;
    private float baseVolume = -1.0F;
    private boolean started = false;

    protected MovingSoundTileEntity(TileEntity par1Entity, String par2Sound, boolean par3Repeat) {
        super(par2Sound, par3Repeat);
        this.entity = par1Entity;

        this.xPosF = (float) this.entity.getPos().getX() + 0.5F;
        this.yPosF = (float) this.entity.getPos().getY() + 0.5F;
        this.zPosF = (float) this.entity.getPos().getZ() + 0.5F;
        this.volume = AppleConfig.crossingSoundRange / 16.0F;
    }

    @Override
    public void update() {
        super.update();

        if (this.entity.isInvalid()) {
            this.donePlaying = true;
            return;
        }

        if (this.baseVolume >= 0.0F) {
            if (!this.started) {
                this.started = true;
                this.volume = this.baseVolume;
            } else {
                float adjusted = SoundRangeHelper.calcVolume(
                        this.baseVolume,
                        AppleConfig.crossingSoundRange,
                        this.xPosF, this.yPosF, this.zPosF);
                this.volume = adjusted;
            }
        }
    }

    @Override
    public void setVolume(float par1) {
        float v = par1 * RTMCore.trainSoundVol;
        this.baseVolume = v;
    }
}