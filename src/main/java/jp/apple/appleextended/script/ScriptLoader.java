package jp.apple.appleextended.script;

import com.anatawa12.fixRtm.scripting.FIXScriptUtil;
import jp.ngt.ngtlib.io.NGTFileLoader;
import jp.ngt.rtm.modelpack.ModelPackManager;
import net.minecraft.util.ResourceLocation;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ScriptLoader {

    private static final Object GROOVY_COMPILE_LOCK = new Object();

    private ScriptLoader() {
    }

    public static ScriptEngine load(String scriptPath) {
        if (scriptPath == null) {
            return null;
        }

        if (scriptPath.endsWith(".groovy")) {
            return loadGroovy(scriptPath);
        } else {
            return FIXScriptUtil.getScriptAndDoScript(scriptPath);
        }
    }

    public static void clearGroovyCache() {
        GroovyScriptEngine.clearCache();
    }

    private static ScriptEngine loadGroovy(String scriptPath) {
        try {
            ScriptEngine engine = GroovyScriptEngine.getEngineForScript(scriptPath);

            ResourceLocation rl =
                    ModelPackManager.INSTANCE.getResource(scriptPath);

            synchronized (GROOVY_COMPILE_LOCK) {
                try (InputStream is = NGTFileLoader.getInputStream(rl);
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    engine.eval(reader);
                }
            }

            engine.put(FIXScriptUtil.SCRIPT_NAME_PROPERTY, scriptPath);
            return engine;

        } catch (ScriptException e) {
            throw new RuntimeException("Groovy script eval error: " + scriptPath, e);
        } catch (Exception e) {
            throw new RuntimeException("Groovy script load failed: " + scriptPath, e);
        }
    }
}