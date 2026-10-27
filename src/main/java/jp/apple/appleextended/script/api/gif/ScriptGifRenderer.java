package jp.apple.appleextended.script.api.gif;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.render.ModelObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ScriptGifRenderer {
    private static final Map<String, GifAnimation> GIF_CACHE = new HashMap<>();
    private static final ThreadLocal<ModelObject> currentModelObject = new ThreadLocal<>();

    public static void setCurrentModelObject(ModelObject modelObject) {
        currentModelObject.set(modelObject);
    }

    public static void clearCurrentModelObject() {
        currentModelObject.remove();
    }

    /**
     * setGif([[x,y,z],[x,y,z],[x,y,z],[x,y,z]], "path/file.gif")
     */
    public static void setGif(Object verticesObj, String path) {
        if (verticesObj == null || path == null) return;
        try {
            ScriptObjectMirror outer = (ScriptObjectMirror) verticesObj;
            if (!outer.isArray()) return;

            float[][] vertices = new float[4][3];
            for (int i = 0; i < 4; i++) {
                ScriptObjectMirror inner = (ScriptObjectMirror) outer.getSlot(i);
                if (inner == null || !inner.isArray()) return;

                for (int j = 0; j < 3; j++) {
                    Object val = inner.getSlot(j);
                    vertices[i][j] = ((Number) val).floatValue();
                }
            }

            GifAnimation anim = GIF_CACHE.get(path);
            if (anim == null) {
                ResourceLocation loc = ModelPackManager.INSTANCE.getResource(path);
                InputStream stream = Minecraft.getMinecraft()
                        .getResourceManager().getResource(loc).getInputStream();
                anim = new GifAnimation(stream);
                GIF_CACHE.put(path, anim);
            }

            anim.render(vertices);

        } catch (Exception e) {
        }
    }

    public static void clearCache() {
        GIF_CACHE.clear();
    }
}
