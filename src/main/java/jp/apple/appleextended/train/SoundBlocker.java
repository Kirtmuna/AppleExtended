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

package jp.apple.appleextended.train;

import jp.apple.appleextended.config.AppleConfig;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SoundBlocker {

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() == null) return;

        String soundName = event.getName();

        if (!AppleConfig.enableJointSound) {
            if (soundName.contains("sounds/train/joint") || soundName.contains("sounds/train/joint_reverb")) {
                event.setResultSound(null);
                return;
            }
        }

        if (!AppleConfig.enableNotchSound) {
            if (soundName.contains("sounds/train/lever")) {
                event.setResultSound(null);
            }
        }
    }
}