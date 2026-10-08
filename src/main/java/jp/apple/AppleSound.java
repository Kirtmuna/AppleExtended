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

package jp.apple;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = AppleLib.MODID)
public class AppleSound {

    public static final SoundEvent ATS_CHIME =
            new SoundEvent(new ResourceLocation("rtm", "ats.chime"))
                    .setRegistryName("rtm", "ats.chime");

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(ATS_CHIME);
    }
}
