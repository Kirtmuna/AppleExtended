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

package jp.apple.appleextended.render;

import jp.apple.appleextended.tileentity.TileEntityTrainModel;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetTrain;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

public class RenderTileEntityTrainModel extends TileEntitySpecialRenderer<TileEntityTrainModel> {

    @Override
    public void render(TileEntityTrainModel te, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        ModelSetTrain modelSet = te.getResourceState().getResourceSet();

        if (modelSet == null || modelSet.isDummy() || modelSet.modelObj == null) {
            return;
        }

        GL11.glPushMatrix();


        GL11.glTranslated(x + 0.5D, y, z + 0.5D);
        GL11.glTranslatef(te.getOffsetX(), te.getOffsetY(), te.getOffsetZ());


        GL11.glRotatef(-te.getRotation(), 0.0F, 1.0F, 0.0F);


        if (te.getRotationX() != 0.0F) GL11.glRotatef(te.getRotationX(), 1.0F, 0.0F, 0.0F);
        if (te.getRotationZ() != 0.0F) GL11.glRotatef(te.getRotationZ(), 0.0F, 0.0F, 1.0F);


        float s = te.getScale();
        if (s != 1.0F) GL11.glScalef(s, s, s);

        TrainConfig cfg = modelSet.getConfig();

        modelSet.modelObj.render(null, cfg, 0, partialTicks);
        modelSet.modelObj.render(null, cfg, 1, partialTicks);
        int brightness = te.getWorld().getCombinedLight(te.getPos(), 0);
        int lightX = brightness % 65536;
        int lightY = brightness / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        if (modelSet.bogieModels != null) {
            for (int i = 0; i < 2; ++i) {
                GL11.glPushMatrix();
                float[] fa = cfg.getBogiePos()[i];
                GL11.glTranslatef(fa[0], fa[1], fa[2]);
                GL11.glRotatef(180.0F * (float) i, 0.0F, 1.0F, 0.0F);
                modelSet.bogieModels[i].render(null, cfg, 0, partialTicks);
                GL11.glPopMatrix();
            }
        }
        GL11.glPopMatrix();
    }
}