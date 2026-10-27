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

import com.anatawa12.fixRtm.ngtlib.renderer.model.PolygonModelCharsetDetector;
import kotlin.Pair;
import net.minecraft.util.ResourceLocation;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class NGTText {
    public static String getText(ResourceLocation resource, boolean indention) throws IOException {
        List<String> list = readText(resource);
        return append(list, indention);
    }

    public static List<String> readText(ResourceLocation resource) throws IOException {
        return readTextL(NGTFileLoader.getInputStream(resource), "");
    }

    public static String readText(File file, boolean indention, String encoding) throws IOException {
        return append(readText(file, encoding), indention);
    }

    public static List<String> readText(File file, String encoding) throws IOException {
        return readTextL(NGTFileLoader.getInputStreamFromFile(file), encoding);
    }

    public static String[][] readCSV(File file, String encoding) throws IOException {
        List<String> texts = readText(file, encoding);
        String[][] array = new String[texts.size()][];
        for (int i = 0; i < texts.size(); ++i) {
            array[i] = texts.get(i).split(",");
        }
        return array;
    }

    public static String append(List<String> list, boolean indention) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s);
            if (indention) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public static List<String> readTextL(InputStream is, String encoding) {
        List<String> list = new ArrayList<>();
        InputStreamReader inputstreamreader;
        if (encoding == null || encoding.isEmpty()) {
            Pair<Charset, InputStream> pair = PolygonModelCharsetDetector.INSTANCE.detectCharset(is, Charset.defaultCharset());
            inputstreamreader = new InputStreamReader(pair.component2(), pair.component1());
        } else {
            try {
                inputstreamreader = new InputStreamReader(is, encoding);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                inputstreamreader = new InputStreamReader(is);
            }
        }
        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
        Stream<String> stream = bufferedreader.lines();
        stream.forEachOrdered(line -> {
            list.add(line);
        });
        stream.close();
        return list;
    }

    @Deprecated
    public static List<String> readTextL(File file, String encoding) {
        List<String> strings = new ArrayList<>();

        if (file.getAbsolutePath().contains(".zip")) {
            String path = file.getAbsolutePath();
            int index = path.indexOf(".zip");
            String zipPath = path.substring(0, index + 4);
            try {
                ZipFile zip = new ZipFile(zipPath);
                Enumeration<? extends ZipEntry> enu = zip.entries();
                while (enu.hasMoreElements()) {
                    ZipEntry ze = enu.nextElement();
                    if (!ze.isDirectory()) {
                        File fileInZip = new File(zipPath, ze.getName());
                        if (fileInZip.getName().equals(file.getName())) {
                            InputStream is = zip.getInputStream(ze);
                            BufferedInputStream bis = new BufferedInputStream(is);
                            BufferedReader br = new BufferedReader(new InputStreamReader(bis));
                            String string;
                            while ((string = br.readLine()) != null) {
                                strings.add(string);
                            }
                            br.close();
                            break;
                        }
                    }
                }
                zip.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                InputStreamReader isr;
                if (encoding.isEmpty()) {
                    isr = new InputStreamReader(new FileInputStream(file));
                } else {
                    isr = new InputStreamReader(new FileInputStream(file), encoding);
                }
                BufferedReader br = new BufferedReader(isr);
                String string;
                while ((string = br.readLine()) != null) {
                    strings.add(string);
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return strings;
    }

    public static boolean writeToText(File file, String... texts) {
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));
            for (String s : texts) {
                pw.println(s);
            }
            pw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}