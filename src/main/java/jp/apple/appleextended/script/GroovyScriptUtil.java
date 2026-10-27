package jp.apple.appleextended.script;

import com.anatawa12.fixRtm.scripting.FIXScriptUtil;
import jp.ngt.ngtlib.io.NGTLog;
import net.minecraft.util.ResourceLocation;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import javax.script.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class GroovyScriptUtil {

    private static final String SCRIPT_NAME_PROPERTY =
            FIXScriptUtil.SCRIPT_NAME_PROPERTY;

    private GroovyScriptUtil() {
    }

    public static ScriptEngine doScript(String source) {
        return doScript(source, null);
    }

    public static ScriptEngine doScript(String source, String fileName) {
        ScriptEngine engine = GroovyScriptEngine.getEngineForScript(fileName != null ? fileName : "");
        try {
            engine.eval(source);
            if (fileName != null) {
                engine.put(SCRIPT_NAME_PROPERTY, fileName);
            }
            return engine;
        } catch (ScriptException e) {
            throw new RuntimeException("Groovy script eval error: " + fileName, e);
        }
    }

    public static CompiledScript compile(ScriptEngine engine, ResourceLocation resource) {
        String path = resource.getResourcePath();
        try (InputStream is = GroovyScriptUtil.class.getResourceAsStream("/" + path);
             InputStreamReader reader = new InputStreamReader(
                     is == null ? openViaResourceManager(resource) : is,
                     StandardCharsets.UTF_8)) {

            CompiledScript compiled = ((Compilable) engine).compile(reader);
            compiled.eval();
            return compiled;
        } catch (IOException e) {
            throw new RuntimeException("Groovy script load error: " + path, e);
        } catch (ScriptException e) {
            throw new RuntimeException("Groovy script eval error: " + path, e);
        }
    }

    private static InputStream openViaResourceManager(ResourceLocation resource) throws IOException {
        throw new IOException("リソースが見つかりません: " + resource);
    }

    public static Object doScriptFunction(ScriptEngine engine, String func, Object... args) {
        if (engine instanceof GroovyScriptEngineImpl) {
            ClassLoader cl = ((GroovyScriptEngineImpl) engine).getClassLoader();
            NGTLog.debug("[GroovyScriptUtil] exec classloader identityHash="
                    + System.identityHashCode(cl) + " class=" + cl.getClass().getName());
        }
        try {
            return ((Invocable) engine).invokeFunction(func, args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "Groovy: 関数が見つかりません: " + func + " in " + scriptName(engine), e);
        } catch (ScriptException e) {
            throw new RuntimeException(
                    "Groovy script exec error: " + func + " in " + scriptName(engine), e);
        }
    }

    public static Object doScriptIgnoreError(ScriptEngine engine, String func, Object... args) {
        try {
            return doScriptFunction(engine, func, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getScriptField(ScriptEngine engine, String fieldName) {
        return engine.get(fieldName);
    }

    private static String scriptName(ScriptEngine engine) {
        try {
            Object name = engine.get(SCRIPT_NAME_PROPERTY);
            return name == null ? null : name.toString();
        } catch (Throwable t) {
            return null;
        }
    }
}