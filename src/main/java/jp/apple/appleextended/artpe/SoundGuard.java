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

/*
^_^かす
 */
package jp.apple.appleextended.artpe;

import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class SoundGuard {
    public SoundGuard() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new SoundGuard());
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote)
            return;
        if (!(event.getEntity() instanceof EntityTrainBase))
            return;

        try {
            EntityTrainBase train = (EntityTrainBase) event.getEntity();
            try {
                if ("no_name".equals(train.getResourceState().getResourceName())) {
                    train.getResourceState().setResourceName("Root.Train.Kiha600");
                    NGTLog.debug("[SoundGuard] Renamed no_name to Root.Train.Kiha600 to avoid NPE");
                }
            } catch (Throwable t) {
                NGTLog.debug("[SoundGuard] Failed to inspect or rename resource name: %s", t.getMessage());
            }

            try {
                Class<?> c = Class.forName("jp.ngt.rtm.sound.MovingSoundMaker");
                Field f = c.getDeclaredField("NAME_COMPATIBLE_MAP");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Map<String, String>> nameMap = (Map<String, Map<String, String>>) f.get(null);
                if (nameMap != null) {
                    if (!nameMap.containsKey("rtm")) {
                        nameMap.put("rtm", new HashMap<>());
                        NGTLog.debug("[SoundGuard] Inserted empty map for domain 'rtm' to prevent NPE");
                    }
                }
            } catch (Throwable t) {
                NGTLog.debug("[SoundGuard] Reflection failed: %s", t.getMessage());
            }
        } catch (Throwable e) {
            NGTLog.debug("[SoundGuard] Unexpected error: %s", e.getMessage());
        }
    }
}
// u~n^_^