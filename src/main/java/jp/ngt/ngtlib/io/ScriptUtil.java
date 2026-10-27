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

import com.anatawa12.fixRtm.scripting.FIXScriptUtil;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.ResourceLocation;

import javax.script.*;
import java.io.IOException;

public final class ScriptUtil {
    private static ScriptEngineManager SEM;

    private static void init() {
        LaunchClassLoader loader = Launch.classLoader;
        loader.addClassLoaderExclusion("jdk.nashorn.");

        SEM = new ScriptEngineManager(null);
    }

    private static void showScripts(ScriptEngineManager mng) {
        NGTLog.debug("SEF");
        for (ScriptEngineFactory factory : mng.getEngineFactories()) {
            System.out.println("Engine name: " + factory.getEngineName());
            System.out.println("Engine version: " + factory.getEngineVersion());
            System.out.println("Language name: " + factory.getLanguageName());
            System.out.println("Language version: " + factory.getLanguageVersion());

            for (String extension : factory.getExtensions()) {
                System.out.println("Extension: " + extension);
            }
            for (String mimeType : factory.getMimeTypes()) {
                System.out.println("MimeType: " + mimeType);
            }
            for (String name : factory.getNames()) {
                System.out.println("Short name: " + name);
            }
            System.out.println();
        }
    }

    public static ScriptEngine doScript(String s) {
        return doScript(s, null);
    }

    public static ScriptEngine doScript(String s, String fileName) {
        if (SEM == null) {
            init();
        }

        com.anatawa12.fixRtm.ngtlib.io.ScriptUtil.INSTANCE.prepareSystemProperty();
        ScriptEngine se = new NashornScriptEngineFactory().getScriptEngine("-doe", "--language=es6");

        try {
            se.eval("load(\"nashorn:mozilla_compat.js\");");
            se.eval(s);
            if (fileName != null)
                se.put(FIXScriptUtil.SCRIPT_NAME_PROPERTY, fileName);
            return se;
        } catch (ScriptException e) {
            throw new RuntimeException("Script exec error: " + fileName, e);
        }
    }

    public static CompiledScript compile(ScriptEngine engine, ResourceLocation resource) {
        try {
            String script = NGTText.getText(resource, true);
            CompiledScript compiledScript = ((Compilable) engine).compile(script);
            compiledScript.eval();
            return compiledScript;
        } catch (IOException e) {
            throw new RuntimeException("Script load error : " + resource.getResourcePath(), e);
        } catch (ScriptException e) {
            throw new RuntimeException("Script load error : " + resource.getResourcePath(), e);
        }
    }

    public static Object doScriptFunction(ScriptEngine se, String func, Object... args) {
        try {
            return ((Invocable) se).invokeFunction(func, args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Script exec error : " + func + " in file: " + scriptName(se), e);
        } catch (ScriptException e) {
            throw new RuntimeException("Script exec error : " + func + " in file: " + scriptName(se), e);
        }
    }

    private static String scriptName(ScriptEngine engine) {
        try {
            Object name = engine.get(FIXScriptUtil.SCRIPT_NAME_PROPERTY);
            if (name == null) return null;
            return name.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object doScriptIgnoreError(ScriptEngine se, String func, Object... args) {
        try {
            return doScriptFunction(se, func, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getScriptField(ScriptEngine se, String fieldName) {
        return se.get(fieldName);
    }

}