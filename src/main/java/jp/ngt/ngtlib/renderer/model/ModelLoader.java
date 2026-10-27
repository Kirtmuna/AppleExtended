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

package jp.ngt.ngtlib.renderer.model;

import com.anatawa12.fixRtm.ngtlib.renderer.model.ModelLoaderKt;
import jp.ngt.ngtlib.io.FileType;
import jp.ngt.ngtlib.io.NGTFileLoader;
import jp.ngt.ngtlib.io.ResourceLocationCustom;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.io.*;

@SideOnly(Side.CLIENT)
public final class ModelLoader {
    public static PolygonModel loadModel(String path, VecAccuracy par1, Object... args) {
        return loadModel(new ResourceLocationCustom("minecraft", path), par1, args);
    }

    public static PolygonModel loadModel(ResourceLocation resource, VecAccuracy par1, Object... args) {
        return ModelLoaderKt.loadModel(resource, par1, args);
    }

    public static PolygonModel loadModel__NGTLIB(ResourceLocation resource, VecAccuracy par1, Object... args) {
        String fileName = resource.toString();

        try {
            InputStream is = NGTFileLoader.getInputStream(resource);

            if (FileType.OBJ.match(resource.getResourcePath())) {
                String mtlFileName = resource.getResourcePath().replaceAll(".obj", ".mtl");
                ResourceLocation mtlFile = new ResourceLocation(resource.getResourceDomain(), mtlFileName);
                InputStream is2 = null;
                try {
                    is2 = NGTFileLoader.getInputStream(mtlFile);
                } catch (IOException e) {
                    ;
                }
                return loadModel(new InputStream[]{is, is2}, fileName, par1, args);
            } else {
                return loadModel(new InputStream[]{is}, fileName, par1, args);
            }
        } catch (IOException e) {
            throw new ModelFormatException("Failed to load model : " + fileName, e);
        }
    }

    public static PolygonModel loadModel(File file, VecAccuracy par1, Object... args) {
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file));

            if (FileType.OBJ.match(file.getName())) {
                String mtlFileName = file.getName().replaceAll(".obj", ".mtl");
                File mtlFile = new File(file.getParentFile(), mtlFileName);
                InputStream is2 = null;
                try {
                    is2 = new BufferedInputStream(new FileInputStream(mtlFile));
                } catch (IOException e) {
                    ;
                }
                return loadModel(new InputStream[]{is, is2}, file.getName(), par1, args);
            } else {
                return loadModel(new InputStream[]{is}, file.getName(), par1, args);
            }
        } catch (IOException e) {
            throw new ModelFormatException("Failed to load model : " + file.getName(), e);
        }
    }

    public static PolygonModel loadModel(InputStream[] is, String name, VecAccuracy par1, Object... args) {
        if (FileType.OBJ.match(name)) {
            return new ObjModel(is, name, par1);
        } else if (FileType.MQO.match(name)) {
            if (args.length > 0) {
                return new MqoModel(is, name, (int) args[0], par1);
            } else {
                return new MqoModel(is, name, GL11.GL_TRIANGLES, par1);
            }
        } else if (FileType.MQOZ.match(name)) {
            return new MqozModel(is, name, GL11.GL_TRIANGLES, par1);
        } else if (FileType.NPM.match(name)) {
            EncryptedModel em = EncryptedModel.getInstance(is[0], (byte[]) args[1]);
            return em.getModel(name, par1, args);
        }
        return null;
    }
}
