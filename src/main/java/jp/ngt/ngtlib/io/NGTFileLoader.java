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

package jp.ngt.ngtlib.io;

import com.anatawa12.fixRtm.io.FIXFileLoader;
import jp.ngt.ngtlib.NGTCore;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class NGTFileLoader {
    public static final String NO_ZIP = "no_zip";
    private static final String[] EXCEPT_WORD = {"lang", "block", "item", "gui"};

    private static List<File> MODS_DIR;
    private static File PREV_OPENED_FOLDER;

    public static void log(String par1, Object... par2) {
        if (NGTCore.debugLog) {
            NGTLog.debug(par1, par2);
        }
    }

    public static List<File> findFile(FileMatcher matcher) {
        ScanResult result = findFile(new FileMatcher[]{matcher});
        return result.asList();
    }

    public static ScanResult findFile(FileMatcher... matchers) {
        ScanResult findFiles = new ScanResult();
        List<File> modsDir = getModsDir();
        for (File dir : modsDir) {
            log("[NGTFL] Set search path : " + dir.getAbsolutePath());
            try {
                findFileInDirectory(findFiles, dir, matchers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return findFiles;
    }

    public static List<File> findFileInDirectory(File dir, FileMatcher matcher) throws IOException {
        ScanResult result = new ScanResult();
        findFileInDirectory(result, dir, new FileMatcher[]{matcher});
        return result.asList();
    }

    public static void findFileInDirectory(ScanResult result, File dir, FileMatcher... matchers) throws IOException {
        String[] files = dir.list();

        if (files == null || files.length == 0) {
            return;
        }

        for (String entryName : files) {
            File entry = new File(dir, entryName);
            if (entry.isFile()) {
                String name = entry.getName();
                if (FileType.ZIP.match(name) || FileType.JAR.match(name)) {
                    findFileInZip(result, entry, "", matchers);
                } else {
                    for (FileMatcher matcher : matchers) {
                        if (matcher.match(entry)) {
                            result.add(NO_ZIP, matcher, entry);
                        }
                    }
                }
            } else if (entry.isDirectory() && !isExeptFolder(entry)) {
                findFileInDirectory(result, entry, matchers);
            }
        }
    }

    private static void findFileInZip(ScanResult result, File archive, String encoding, FileMatcher... matchers) {
        log("[NGTFL] Scan zip : " + archive.getName());

        try {
            ZipFile zip = getArchive(archive, encoding);
            zip.stream().filter(x -> !x.isDirectory()).forEach(entry -> {
                File file = new File(zip.getName(), entry.getName());
                for (FileMatcher matcher : matchers) {
                    if (matcher.match(file)) {
                        result.add(archive.getName(), matcher, file);
                    }
                }
            });
            zip.close();
        } catch (IOException e) {
            e.printStackTrace();
            NGTLog.debug("[NGTFL] IOException:" + archive.getName());
        } catch (IllegalArgumentException e) {
            if (encoding.isEmpty()) {
                findFileInZip(result, archive, "MS932", matchers);
                return;
            }
            e.printStackTrace();
            NGTLog.debug("[NGTFL] IllegalArgumentException:" + archive.getName());
        }
    }

    private static boolean isExeptFolder(File folder) {
        if (folder.getAbsolutePath().contains("sounds")) {
            return false;
        }

        for (String word : EXCEPT_WORD) {
            if (folder.getName().equals(word)) {
                return true;
            }
        }
        return false;
    }

    public static byte[] readBytes(File par1) throws IOException {
        InputStream is = new FileInputStream(par1);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int len = 0; (len = is.read(buffer)) > 0; bout.write(buffer, 0, len)) {
        }
        is.close();
        return bout.toByteArray();
    }

    public static List<File> getModsDir() {
        if (MODS_DIR != null) {
            return MODS_DIR;
        }

        LinkedHashSet<File> mods = new LinkedHashSet<>();

        for (File jarOrDir : FIXFileLoader.INSTANCE.getModsOrJars()) {
            if (jarOrDir.isDirectory())
                mods.add(jarOrDir);
            else
                mods.add(jarOrDir.getParentFile());
        }

        MODS_DIR = new ArrayList<>(mods);
        NGTLog.debug("[NGTFL] Add mods dir : " + MODS_DIR);
        return MODS_DIR;
    }

    private static JFileChooser getCustomChooser(String title) {
        JFileChooser chooser = new JFileChooser(PREV_OPENED_FOLDER) {
            @Override
            protected JDialog createDialog(Component parent) throws HeadlessException {
                JDialog dialog = super.createDialog(parent);
                dialog.setAlwaysOnTop(true);
                return dialog;
            }
        };

        chooser.setDialogTitle(title);
        chooser.requestFocusInWindow();
        return chooser;
    }

    public static File selectFile(FileType... types) {
        return showChooser(true, types);
    }

    public static File saveFile(FileType... types) {
        return showChooser(false, types);
    }

    private static synchronized File showChooser(boolean open, FileType... types) {
        final File[] result = new File[1];

        Runnable task = () -> {
            JFileChooser chooser = getCustomChooser(open ? "Select File" : "Save File");
            chooser.setAcceptAllFileFilterUsed(false);
            for (FileType type : types) {
                FileFilter filter = new FileNameExtensionFilter(type.getDescription(), type.getExtension());
                chooser.addChoosableFileFilter(filter);
            }

            int state = open ? chooser.showOpenDialog(null) : chooser.showSaveDialog(null);
            if (state == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                PREV_OPENED_FOLDER = file.getParentFile();
                if (!open && !file.getName().contains(".")) {
                    FileNameExtensionFilter filter = (FileNameExtensionFilter) chooser.getFileFilter();
                    file = new File(file.getAbsolutePath() + "." + filter.getExtensions()[0]);
                }
                result[0] = file;
            }
        };

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                SwingUtilities.invokeAndWait(task);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return result[0];
    }

    public static InputStream getInputStream(ResourceLocation par1) throws IOException {
        return FIXFileLoader.INSTANCE.getInputStream(par1);
    }

    public static InputStream getInputStreamFromFile(File file) throws IOException {
        String suffix = getArchiveSuffix(file.getAbsolutePath());
        if (!suffix.isEmpty()) {
            return getStreamFromArchive(file, suffix);
        } else {
            return new FileInputStream(file);
        }
    }

    public static InputStream getStreamFromArchive(File file, String suffix) throws IOException {
        String zipPath = getArchivePath(file.getAbsolutePath(), suffix);
        ZipFile zip = getArchive(new File(zipPath), "");
        Enumeration<? extends ZipEntry> enu = zip.entries();
        while (enu.hasMoreElements()) {
            ZipEntry ze = enu.nextElement();
            if (!ze.isDirectory()) {
                File fileInZip = new File(zipPath, ze.getName());
                if (fileInZip.getName().equals(file.getName())) {
                    InputStream is = zip.getInputStream(ze);
                    return new BufferedInputStream(is);
                }
            }
        }
        zip.close();

        throw new FileNotFoundException("On get stream : " + file.getName());
    }

    public static String getArchivePath(String absPath, String suffix) {
        int index = absPath.indexOf(suffix);
        return absPath.substring(0, index + 4);
    }

    public static ZipFile getArchive(File file, String encoding) throws IOException {
        String en2 = encoding.isEmpty() ? "UTF-8" : encoding;
        if (FileType.JAR.match(file.getName())) {
            return new JarFile(file.getAbsolutePath());
        } else if (FileType.ZIP.match(file.getName())) {
            return new ZipFile(file.getAbsolutePath(), Charset.forName(en2));
        }
        return null;
    }

    public static String getArchiveSuffix(String absPath) {
        if (absPath.contains(".zip")) {
            return ".zip";
        } else if (absPath.contains(".jar")) {
            return ".jar";
        }
        return "";
    }

    public static File createTempFile(InputStream is, String name) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        final File tempFile = new File(tempDir, name);
        tempFile.deleteOnExit();
        FileOutputStream out = new FileOutputStream(tempFile);
        IOUtils.copy(is, out);
        return tempFile;
    }
}