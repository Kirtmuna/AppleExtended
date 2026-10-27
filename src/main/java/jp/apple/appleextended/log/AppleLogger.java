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

package jp.apple.appleextended.log;

import jp.apple.appleextended.config.AppleConfig;
import jp.apple.appleextended.util.AppleDir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AppleLogger {
    private static final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private static File currentLogFile;
    private static BufferedWriter writer;
    private static Thread writerThread;
    private static volatile boolean running = false;

    public static void init() {
        if (!AppleConfig.enableBlockChangeLog) return;
        if (AppleDir.appleLibDir == null) return;

        // AppleLib/log フォルダを作成
        File logDir = new File(AppleDir.appleLibDir, "log");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        String fileName = "blockchange-" + fileDateFormat.format(new Date()) + ".log";
        currentLogFile = new File(logDir, fileName);

        try {
            writer = new BufferedWriter(new FileWriter(currentLogFile, true));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        running = true;
        writerThread = new Thread(AppleLogger::processQueue, "AppleLogger-Writer");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    private static void processQueue() {
        while (running || !queue.isEmpty()) {
            try {
                String line = queue.poll(500, TimeUnit.MILLISECONDS);
                if (line != null) {
                    writer.write(line);
                    writer.newLine();
                    if (queue.isEmpty()) {
                        writer.flush();
                    }
                }
            } catch (InterruptedException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void logBlockChange(EntityPlayer player, BlockPos pos, IBlockState state, String action) {
        if (currentLogFile == null) return;

        String time = logDateFormat.format(new Date());
        String username = player.getName();
        String blockName = state.getBlock().getRegistryName() != null ? state.getBlock().getRegistryName().toString() : "unknown";
        String coords = String.format("%d %d %d", pos.getX(), pos.getY(), pos.getZ());
        String line = String.format("[%s] %s %s %s %s", time, username, action, blockName, coords);

        queue.offer(line);
    }

    public static void shutdown() {
        running = false;
        try {
            if (writerThread != null) writerThread.join(2000);
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}