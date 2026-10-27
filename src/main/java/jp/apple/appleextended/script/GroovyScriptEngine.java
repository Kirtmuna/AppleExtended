package jp.apple.appleextended.script;

import jp.ngt.ngtlib.io.NGTFileLoader;
import jp.ngt.ngtlib.io.NGTLog;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import javax.script.ScriptEngine;
import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GroovyScriptEngine {

    private static final Map<String, groovy.lang.GroovyClassLoader> LOADER_CACHE =
            new ConcurrentHashMap<>();

    private GroovyScriptEngine() {
    }

    public static ScriptEngine getEngineForScript(String scriptPath) {
        groovy.lang.GroovyClassLoader gcl = resolveClassLoader(scriptPath);
        return new GroovyScriptEngineImpl(gcl);
    }

    public static void clearCache() {
        for (groovy.lang.GroovyClassLoader gcl : LOADER_CACHE.values()) {
            try {
                gcl.close();
            } catch (Exception e) {
                NGTLog.debug("[GroovyScriptEngine] Failed to close GroovyClassLoader: " + e.getMessage());
            }
        }
        LOADER_CACHE.clear();
        NGTLog.debug("[GroovyScriptEngine] ClassLoader cache cleared.");
    }

    private static String stripDomain(String scriptPath) {
        String p = scriptPath.replace("\\", "/");
        return p.contains(":") ? p.substring(p.indexOf(":") + 1) : p;
    }

    private static groovy.lang.GroovyClassLoader resolveClassLoader(String scriptPath) {
        File scriptFile = resolveScriptFile(scriptPath);
        String packRootKey = getPackRootKey(scriptFile, scriptPath);

        return LOADER_CACHE.computeIfAbsent(packRootKey, key -> {
            try {
                URL rootUrl = getPackRootUrl(scriptFile, scriptPath);
                NGTLog.debug("[GroovyScriptEngine] Creating ClassLoader for: " + rootUrl);

                groovy.lang.GroovyClassLoader gcl = new groovy.lang.GroovyClassLoader(
                        GroovyScriptEngine.class.getClassLoader()
                );
                gcl.addURL(rootUrl);

                final File rootDirFinal = ("file".equals(rootUrl.getProtocol())) ? new File(rootUrl.getPath()) : null;
                if (rootDirFinal != null) {
                    gcl.setResourceLoader(filename -> {
                        String relativePath = filename.replace('.', '/') + ".groovy";
                        File f = new File(rootDirFinal, relativePath);
                        NGTLog.debug("[GroovyScriptEngine] resourceLoader lookup: " + filename + " -> " + f.getAbsolutePath());
                        boolean exists = f.exists();
                        if (exists) {
                            try {
                                return f.toURI().toURL();
                            } catch (Exception e) {
                                return null;
                            }
                        }
                        return null;
                    });
                }

                return gcl;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create GroovyClassLoader for: " + scriptPath, e);
            }
        });
    }

    private static File resolveScriptFile(String scriptPath) {
        String normalized = stripDomain(scriptPath);
        for (File f : jp.ngt.rtm.modelpack.ModelPackManager.INSTANCE.fileCache) {
            String absPath = f.getAbsolutePath().replace("\\", "/");
            if (absPath.endsWith(normalized)) {
                return f;
            }
        }
        throw new RuntimeException("Script file not found in fileCache: " + scriptPath);
    }

    private static URL getPackRootUrl(File scriptFile, String scriptPath) throws Exception {
        String absPath = scriptFile.getAbsolutePath().replace("\\", "/");
        String normalized = stripDomain(scriptPath);
        String scriptRelative = "assets/minecraft/" + normalized;
        int idx = absPath.indexOf(scriptRelative);
        if (idx < 0) {
            throw new RuntimeException("Cannot determine pack root from: " + absPath);
        }

        String suffix = NGTFileLoader.getArchiveSuffix(absPath);
        if (!suffix.isEmpty()) {
            String zipPath = NGTFileLoader.getArchivePath(absPath, suffix);
            String inZipRoot = absPath.substring(zipPath.length() + 1, idx + "assets/minecraft/".length());
            return new URL("jar:file:" + zipPath + "!/" + inZipRoot);
        } else {
            String rootPath = absPath.substring(0, idx) + "assets/minecraft/";
            File rootDir = new File(rootPath);

            if (!rootDir.exists() || !rootDir.isDirectory()) {
                NGTLog.debug("[GroovyScriptEngine] WARNING: computed pack root does not exist as directory: " + rootDir.getAbsolutePath());
            }
            String rawPath = rootDir.getAbsolutePath().replace(File.separatorChar, '/');
            if (!rawPath.startsWith("/")) {
                rawPath = "/" + rawPath;
            }
            if (!rawPath.endsWith("/")) {
                rawPath = rawPath + "/";
            }
            return new URL("file", "", rawPath);
        }
    }

    private static String getPackRootKey(File scriptFile, String scriptPath) {
        String absPath = scriptFile.getAbsolutePath().replace("\\", "/");
        String normalized = stripDomain(scriptPath);
        String scriptRelative = "assets/minecraft/" + normalized;
        int idx = absPath.indexOf(scriptRelative);
        if (idx < 0) return absPath;
        return absPath.substring(0, idx + "assets/minecraft/".length());
    }
}