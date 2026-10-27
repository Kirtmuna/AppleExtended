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

package jp.apple.appleextended.reloader;

import com.anatawa12.fixRtm.UtilsKt;
import com.anatawa12.fixRtm.rtm.modelpack.init.UnconstructSetsQueue;
import jp.ngt.ngtlib.io.NGTFileLoader;
import jp.ngt.ngtlib.io.NGTJson;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.io.NGTText;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.cfg.ResourceConfig;
import jp.ngt.rtm.modelpack.init.ModelPackLoadThread;
import jp.ngt.rtm.modelpack.init.ProgressStateHolder.ProgressState;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class PackReloader {
    private static volatile boolean isReloading = false;

    public static boolean isSystemBusy() {
        ModelPackManager m = ModelPackManager.INSTANCE;


        if (isReloading) return true;


        if (!m.modelLoaded || !m.modelConstructed) return true;


        try {
            Field backedField = UnconstructSetsQueue.class.getDeclaredField("backed");
            backedField.setAccessible(true);
            Queue<?> backedQueue = (Queue<?>) backedField.get(m.unconstructSets);
            return !backedQueue.isEmpty();
        } catch (Exception e) {


            return m.unconstructSets.size() > 0 && !m.modelConstructed;
        }
    }

    public static void startReload() {
        if (isSystemBusy()) {
            NGTLog.debug("[AppleLib] System is busy. Ignoring reload request.");
            return;
        }

        Side side = FMLCommonHandler.instance().getSide();
        ModelPackLoadThread loadThread = new ModelPackLoadThread(side);

        new Thread(() -> {
            isReloading = true;
            try {
                NGTLog.debug("[AppleLib] Start ModelPack Reloading...");
                resetModelPackSystem();
                executeReloadLogic(loadThread);
                waitForCompletion(loadThread);
            } catch (Exception e) {
                NGTLog.debug("[AppleLib] Reload Failed: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isReloading = false;
                loadThread.finish();
                System.gc();
                NGTLog.debug("[AppleLib] Reload process finished.");
            }
        }, "AppleReloader-Main").start();

        loadThread.start();
    }

    private static void resetModelPackSystem() throws Exception {
        ModelPackManager manager = ModelPackManager.INSTANCE;
        UnconstructSetsQueue queueWrapper = manager.unconstructSets;


        Field countField = UnconstructSetsQueue.class.getDeclaredField("count");
        countField.setAccessible(true);
        AtomicInteger count = (AtomicInteger) countField.get(queueWrapper);
        count.set(0);


        Field backedField = UnconstructSetsQueue.class.getDeclaredField("backed");
        backedField.setAccessible(true);
        Queue<?> backedQueue = (Queue<?>) backedField.get(queueWrapper);
        backedQueue.clear();

        manager.modelSetMapLock.writeLock().lock();
        try {
            manager.allModelSetMap.values().forEach(Map::clear);
            manager.smpModelSetMap.values().forEach(Map::clear);
            manager.clearCache();
            manager.modelLoaded = false;
            manager.modelConstructed = false;
        } finally {
            manager.modelSetMapLock.writeLock().unlock();
        }
    }

    private static void executeReloadLogic(ModelPackLoadThread loadThread) throws IOException {
        ModelPackManager manager = ModelPackManager.INSTANCE;
        List<File> fileList = NGTFileLoader.findFile((file) ->
                file.getName().endsWith(".json") && file.getName().contains("_")
        );

        loadThread.setBarValue(0, ProgressState.LOADING_MODEL);
        loadThread.setBarMaxValue(1, fileList.size(), "Scanning JSONs...");

        int processed = 0;
        for (File file : fileList) {
            processed++;
            String typeName = file.getName().split("_")[0];
            ResourceType type = manager.getType(typeName);
            if (type == null) continue;

            loadThread.setBarValue(1, processed, "Processing: " + file.getName());

            byte[] currentMd5;
            try (FileInputStream fis = new FileInputStream(file)) {
                currentMd5 = DigestUtils.md5(fis);
            }

            List<String> jsonLines = NGTText.readText(file, null);
            String jsonRaw = NGTText.append(jsonLines, false);

            manager.modelSetMapLock.writeLock().lock();
            try {
                ResourceConfig config = (ResourceConfig) NGTJson.getObjectFromJson(
                        UtilsKt.joinLinesForJsonReading(jsonLines),
                        type.cfgClass
                );
                config.file = file;
                manager.registerResourceSet(type, config, jsonRaw);
            } finally {
                manager.modelSetMapLock.writeLock().unlock();
            }
        }
    }

    private static void waitForCompletion(ModelPackLoadThread loadThread) throws InterruptedException {
        ModelPackManager manager = ModelPackManager.INSTANCE;
        loadThread.setBarValue(0, ProgressState.CONSTRUCTING_MODEL);


        int timeoutTicks = 0;
        while (!manager.modelConstructed && timeoutTicks < 600) {

            int actualRemaining = 0;
            try {
                Field backedField = UnconstructSetsQueue.class.getDeclaredField("backed");
                backedField.setAccessible(true);
                Queue<?> backedQueue = (Queue<?>) backedField.get(manager.unconstructSets);
                actualRemaining = backedQueue.size();
            } catch (Exception e) {
            }

            loadThread.setBarMaxValue(1, actualRemaining + 1, "Constructing...");
            loadThread.setBarValue(1, 1, "Queue remaining: " + actualRemaining);

            Thread.sleep(200);
            timeoutTicks++;
        }


        manager.modelConstructed = true;
        manager.modelLoaded = true;
    }
}