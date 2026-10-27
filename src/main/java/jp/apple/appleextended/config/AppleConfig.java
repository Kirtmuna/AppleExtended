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

package jp.apple.appleextended.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class AppleConfig {
    public static final String CATEGORY_CONTROL = "control";
    public static final String CATEGORY_SECURITY = "security";
    public static final String CATEGORY_SOUND = "sound";
    public static final String CATEGORY_SOUND_RANGE = "sound_range";
    public static final String CATEGORY_RENDER = "render";

    public static float runningSoundRange = 16.0F;
    public static float crossingSoundRange = 64.0F;
    public static float hornSoundRange = 16.0F;

    public static int notchRepeatInterval = 1;
    public static boolean enableBlockChangeLog = true;

    public static boolean enableJointSound = true;
    public static boolean enableNotchSound = true;

    public static boolean enableCantCamera = true;
    public static boolean enableSmoothLight = true;

    private static Configuration config;

    public static Configuration getConfig() {
        return config;
    }

    public static void init(File file) {
        config = new Configuration(file);
        sync(true);
    }
    
    public static void sync(boolean load) {
        Configuration cfg = config;
        try {
            if (load) {
                cfg.load();
            }
            notchRepeatInterval = cfg.getInt("notchRepeatInterval", CATEGORY_CONTROL, 1, 1, 20, "Interval for A/D notch repeat (ticks)");
            enableBlockChangeLog = cfg.getBoolean(
                    "enableBlockChangeLog",
                    CATEGORY_SECURITY,
                    true,
                    "Enable or disable the block change logger (AppleLib/log)"
            );
            enableJointSound = cfg.getBoolean("enableJointSound", CATEGORY_SOUND, true, "Whether to enable RTM standard joint sounds");
            enableNotchSound = cfg.getBoolean("enableNotchSound", CATEGORY_SOUND, true, "Whether to enable the RTM standard notch operation sound");

            runningSoundRange = cfg.getFloat("runningSoundRange", CATEGORY_SOUND_RANGE,
                    16.0F, 1.0F, 1024.0F, "Set the range in which sound can be heard.");
            crossingSoundRange = cfg.getFloat("crossingSoundRange", CATEGORY_SOUND_RANGE,
                    64.0F, 1.0F, 1024.0F, "Set the range in which sound can be heard.");
            hornSoundRange = cfg.getFloat("hornSoundRange", CATEGORY_SOUND_RANGE,
                    16.0F, 1.0F, 1024.0F, "Set the range in which sound can be heard.");
            enableCantCamera = cfg.getBoolean("enableCantCamera", CATEGORY_RENDER, true,
                    "Tilt the player's view along with the train's cant");
            enableSmoothLight = cfg.getBoolean("enableSmoothLight", CATEGORY_RENDER, true,
                    "Smooth the per-car brightness gradient between blocks");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cfg.hasChanged()) cfg.save();
        }
    }
}
