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
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MovingSoundEntity extends MovingSoundCustom {
    protected final Entity entity;
    private float baseVolume = -1.0F;

    protected MovingSoundEntity(Entity par1Entity, String par2Sound, boolean par3Repeat) {
        super(par2Sound, par3Repeat);
        this.entity = par1Entity;
        this.baseVolume = RTMCore.trainSoundVol;
        this.volume = AppleConfig.runningSoundRange / 16.0F;
    }

    @Override
    public void update() {
        super.update();

        if (this.entity.isDead) {
            this.donePlaying = true;
            return;
        }

        this.xPosF = (float) this.entity.posX;
        this.yPosF = (float) this.entity.posY;
        this.zPosF = (float) this.entity.posZ;

        if (this.baseVolume >= 0.0F) {
            float adjusted = SoundRangeHelper.calcVolume(
                    this.baseVolume,
                    AppleConfig.runningSoundRange,
                    this.xPosF, this.yPosF, this.zPosF);
            this.volume = adjusted;
        }
    }

    @Override
    public void setVolume(float par1) {
        float v = par1 * RTMCore.trainSoundVol;
        this.baseVolume = v;
        super.setVolume(v);
    }
}