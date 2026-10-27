package jp.apple.appleextended.script;

import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ScriptExecuter;
import jp.ngt.rtm.modelpack.modelset.ModelSetBase;

import javax.script.ScriptEngine;

public class GroovyScriptExecuter extends ScriptExecuter {

    private static boolean isGroovyEngine(ScriptEngine engine) {
        if (engine == null) return false;
        String name = engine.getFactory().getEngineName();
        return name != null && name.toLowerCase().contains("groovy");
    }

    @Override
    protected Object callMethod(IResourceSelector selector, String name, Object... args) {
        ModelSetBase set = (ModelSetBase) selector.getResourceState().getResourceSet();
        if (isGroovyEngine(set.serverSE)) {
            return GroovyScriptUtil.doScriptIgnoreError(set.serverSE, name, args);
        }
        return super.callMethod(selector, name, args);
    }
}