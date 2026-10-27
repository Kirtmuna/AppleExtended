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

import com.anatawa12.fixRtm.rtm.HooksKt;
import jp.ngt.ngtlib.io.NGTJson;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.io.NGTText;
import jp.ngt.rtm.RTMSound;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SideOnly(Side.CLIENT)
public final class MovingSoundMaker {
    /**
     * {domain, {name, path}}
     *
     */
    private static final Map<String, Map<String, String>> NAME_COMPATIBLE_MAP = new HashMap<>();

    /**
     * @return 該当ファイル無し時はnull
     *
     */
    public static MovingSoundEntity create(Entity entity, String sound, boolean repeat) {
        if (!checkSoundFile(sound)) {
            return null;
        }

        if (entity instanceof EntityTrainBase) {
            return new MovingSoundTrain((EntityTrainBase) entity, sound, repeat);
        } else {
            return new MovingSoundEntity(entity, sound, repeat);
        }
    }

    /**
     * @return 該当ファイル無し時はnull
     *
     */
    public static MovingSoundTileEntity create(TileEntity entity, String sound, boolean repeat) {
        if (!checkSoundFile(sound)) {
            return null;
        }

        return new MovingSoundTileEntity(entity, sound, repeat);
    }

    private static boolean checkSoundFile(String sound) {
        sound = fixSoundName(sound);
        if (RTMSound.ALL_OGG_FILES.contains(sound)) {
            return true;
        } else if (sound.split(":")[0].equals("minecraft")) {
            return true;
        }
        NGTLog.debug("[MovingSound] Invalid sound : %s", sound);
        return false;
    }

    private static String fixSoundName(String sound) {
        if (sound.contains("ogg")) {
            return sound;
        }

        String domain;
        String path;
        if (sound.contains(":")) {
            String[] sa = sound.split(":");
            domain = sa[0];
            path = sa[1];
        } else {
            domain = "minecraft";
            path = sound;
        }

        if (!NAME_COMPATIBLE_MAP.containsKey(domain)) {
            loadSoundJson(domain);
        }

        Map<String, String> map = NAME_COMPATIBLE_MAP.get(domain);
        if (map.containsKey(sound)) {
            return map.get(sound);
        }

        path = path.replace('.', '/');
        return domain + ":sounds/" + path + ".ogg";
    }

    private static void loadSoundJson(String domain) {
        Map<String, String> nameMap = new HashMap<>();
        try {
            String json = NGTText.getText(new ResourceLocation(domain, "sounds.json"), true);
            Map<String, SoundListDummy> map = NGTJson.getGson().fromJson(json, TYPE);

            HooksKt.MovingSoundMaker_loadSoundJson_nullCheck(map, domain);
            for (Entry<String, SoundListDummy> entry : map.entrySet()) {
                if (!entry.getValue().sounds.isEmpty()) {
                    Object obj = entry.getValue().sounds.get(0);
                    String path;
                    if (obj instanceof Map) {
                        path = (String) ((Map) obj).get("name");
                    } else {
                        path = (String) obj;
                    }

                    if (path.contains(":")) {
                        path = path.replace(":", ":sounds/") + ".ogg";
                    } else {
                        path = domain + ":sounds/" + path + ".ogg";
                    }

                    String name = domain + ":" + entry.getKey();
                    nameMap.put(name, path);
                    NGTLog.debug("[MSM] Add Sound map (%s)->(%s)", name, path);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        NAME_COMPATIBLE_MAP.put(domain, nameMap);
    }

    private static final ParameterizedType TYPE = new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{String.class, SoundListDummy.class};
        }

        @Override
        public Type getRawType() {
            return Map.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    };

    private class SoundListDummy {
        public String category;
        public List sounds;
    }
}