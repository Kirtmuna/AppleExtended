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

package jp.ngt.rtm.rail;

import com.anatawa12.fixRtm.asm.config.MainConfig;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.renderer.GLHelper;
import jp.ngt.ngtlib.renderer.NGTRenderer;
import jp.ngt.ngtlib.renderer.NGTTessellator;
import jp.ngt.ngtlib.util.ColorUtil;
import jp.ngt.ngtlib.util.NGTUtilClient;
import jp.ngt.rtm.RTMBlock;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.gui.InternalButton;
import jp.ngt.rtm.gui.InternalGUI;
import jp.ngt.rtm.network.PacketMarkerRPClient;
import jp.ngt.rtm.rail.util.MarkerState;
import jp.ngt.rtm.rail.util.RailMap;
import jp.ngt.rtm.rail.util.RailPosition;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

@SideOnly(Side.CLIENT)
public final class RenderMarkerBlock extends TileEntitySpecialRenderer<TileEntityMarker> {
    public static final RenderMarkerBlock INSTANCE = new RenderMarkerBlock();

    private static final double FIT_RANGE_SQ = 2.0D * 2.0D;
    private String[] displayStrings;

    private RenderMarkerBlock() {
        super();

        this.displayStrings = new String[RTMCore.markerDisplayDistance / 10];
        for (int i = 0; i < this.displayStrings.length; ++i) {
            this.displayStrings[i] = String.valueOf((i + 1) * 10) + "m";
        }
    }

    @Override
    public boolean isGlobalRenderer(TileEntityMarker tileEntity) {
        return true;
    }

    @Override
    public void render(TileEntityMarker tileEntity, double par2, double par4, double par6, float par8, int par9, float par10) {
        if (tileEntity.getMarkerRP() == null) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GLHelper.disableLighting();
        GL11.glTranslatef((float) par2, (float) par4, (float) par6);

        this.renderGUI(tileEntity);

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        if (tileEntity.getState(MarkerState.GRID) && tileEntity.getGrid() != null) {
            this.renderGrid(tileEntity);
        }

        if (tileEntity.getState(MarkerState.LINE1) || tileEntity.getState(MarkerState.LINE2)) {
            RailPosition rp0 = tileEntity.getMarkerRP();
            float x = (float) (rp0.posX - (double) rp0.blockX);
            float y = (float) (rp0.posY - (double) rp0.blockY);
            float z = (float) (rp0.posZ - (double) rp0.blockZ);

            if (tileEntity.getState(MarkerState.LINE1) && tileEntity.getRailMaps() != null) {
                this.renderLine(tileEntity, x, y, z);
            }

            if (tileEntity.getCoreMarker() != null) {
                this.renderAnchor(tileEntity, x, y, z);
            }
        }

        if (tileEntity.getState(MarkerState.DISTANCE)) {
            this.renderDistanceMark(tileEntity);
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GLHelper.enableLighting();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    private void renderGUI(TileEntityMarker marker) {
        if (marker.gui == null) {
            int i = 61440;
            float f = 2.8F;
            float f1 = 0.5F;
            float f2 = (f1 + 0.1F) * 6.0F + 0.1F;
            float f3 = 0.5F;
            float f4 = -(f + 0.2F) / 2.0F;
            marker.gui = (new InternalGUI(f4, f3, f + 0.2F, f2)).setColor(65535);
            marker.buttons = new InternalButton[6];
            f4 = f4 + 0.1F;
            f3 = f3 + 0.1F;
            marker.buttons[0] = (new InternalButton(f4, f3, f, f1)).setColor(i).setListner((button) -> marker.flipState(MarkerState.ANCHOR21));
            f3 = f3 + f1 + 0.1F;
            marker.buttons[1] = (new InternalButton(f4, f3, f, f1)).setColor(i).setListner((button) -> marker.flipState(MarkerState.LINE2));
            f3 = f3 + f1 + 0.1F;
            marker.buttons[2] = (new InternalButton(f4, f3, f, f1)).setColor(i).setListner((button) -> marker.flipState(MarkerState.LINE1));
            f3 = f3 + f1 + 0.1F;
            marker.buttons[3] = (new InternalButton(f4, f3, f, f1)).setColor(i).setListner((button) -> marker.flipState(MarkerState.GRID));
            f3 = f3 + f1 + 0.1F;
            marker.buttons[4] = (new InternalButton(f4, f3, f, f1)).setColor(i).setListner((button) -> marker.flipState(MarkerState.DISTANCE));
            f3 = f3 + f1 + 0.1F;
            marker.buttons[5] = (new InternalButton(f4, f3, f, f1)).setColor(i).setListner((button) -> marker.flipState(MarkerState.FIT_NEIGHBOR));

            for (int j = 0; j < marker.buttons.length; ++j) {
                marker.gui.addButton(marker.buttons[j]);
            }
        }

        marker.buttons[0].setText(marker.getStateString(MarkerState.ANCHOR21), 16777215, 0.05F);
        marker.buttons[1].setText(marker.getStateString(MarkerState.LINE2), 16777215, 0.05F);
        marker.buttons[2].setText(marker.getStateString(MarkerState.LINE1), 16777215, 0.05F);
        marker.buttons[3].setText(marker.getStateString(MarkerState.GRID), 16777215, 0.05F);
        marker.buttons[4].setText(marker.getStateString(MarkerState.DISTANCE), 16777215, 0.05F);
        marker.buttons[5].setText(marker.getStateString(MarkerState.FIT_NEIGHBOR), 16777215, 0.05F);
        GL11.glPushMatrix();
        float f5 = 0.5F;
        if (marker.getState(MarkerState.LINE1)) {
            f5 = 1.0F;
        }

        if (marker.getState(MarkerState.LINE2) && f5 < marker.getMarkerRP().constLimitHP) {
            f5 = marker.getMarkerRP().constLimitHP;
        }

        GL11.glTranslatef(0.5F, f5, 0.5F);
        GL11.glRotatef(-NGTUtilClient.getMinecraft().getRenderManager().playerViewY + 180.0F, 0.0F, 1.0F, 0.0F);
        marker.gui.render();
        GL11.glPopMatrix();
    }

    private void renderGrid(TileEntityMarker marker) {
        GL11.glPushMatrix();
        NGTTessellator tessellator = NGTTessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorOpaque_I(0);
        for (int[] ia : marker.getGrid()) {
            BlockPos pos = marker.getPos();
            NGTRenderer.addFrame(tessellator, ia[0] - pos.getX(), ia[1] - pos.getY(), ia[2] - pos.getZ(), 1.0F, 1.0F, 1.0F);
        }
        tessellator.draw();
        GL11.glPopMatrix();
    }

    private void renderDistanceMark(TileEntityMarker marker) {
        GL11.glPushMatrix();
        GL11.glTranslatef(0.5F, 0.0625F, 0.5F);
        int i = marker.getBlockMetadata();
        Block block = marker.getBlockType();
        int j = block == RTMBlock.marker ? 16711680 : 255;
        float f = (float) BlockMarker.getMarkerDir(marker.getBlockType(), i) * 45.0F;
        GL11.glRotatef(f, 0.0F, 1.0F, 0.0F);
        GL11.glDisable(3553);
        NGTTessellator ngttessellator = NGTTessellator.instance;
        ngttessellator.startDrawingQuads();
        ngttessellator.setColorOpaque_I(j);

        for (int k = 1; k < this.displayStrings.length; ++k) {
            float f2 = (float) k * 10.0F;

            for (int l = -1; l <= 1; ++l) {
                float f3 = f2 * (float) l;
                if (!MainConfig.markerDistanceMoreRealPosition) {
                    ngttessellator.addVertex(-0.4F + f3, 0.0F, 0.4F + f2);
                    ngttessellator.addVertex(-0.4F + f3, 0.0F, -0.4F + f2);
                    ngttessellator.addVertex(0.4F + f3, 0.0F, -0.4F + f2);
                    ngttessellator.addVertex(0.4F + f3, 0.0F, 0.4F + f2);
                } else {
                    ngttessellator.addVertex(-0.4F + f3, 0.0F, -0.4F + f2);
                    ngttessellator.addVertex(-0.4F + f3, 0.0F, -0.6F + f2);
                    ngttessellator.addVertex(0.4F + f3, 0.0F, -0.6F + f2);
                    ngttessellator.addVertex(0.4F + f3, 0.0F, -0.4F + f2);
                    ngttessellator.addVertex(-0.1F + f3, 0.0F, -0.1F + f2);
                    ngttessellator.addVertex(-0.1F + f3, 0.0F, -0.9F + f2);
                    ngttessellator.addVertex(0.1F + f3, 0.0F, -0.9F + f2);
                    ngttessellator.addVertex(0.1F + f3, 0.0F, -0.1F + f2);
                }
            }
        }

        ngttessellator.draw();
        GL11.glEnable(3553);
        FontRenderer fontrenderer = NGTUtilClient.getMinecraft().getRenderManager().getFontRenderer();

        for (int j1 = 0; j1 < this.displayStrings.length; ++j1) {
            float f5 = (float) (j1 + 1) * 10.0F;

            for (int k1 = -1; k1 <= 1; ++k1) {
                float f4 = f5 * (float) k1;
                GL11.glPushMatrix();
                if (!MainConfig.markerDistanceMoreRealPosition) {
                    GL11.glTranslatef(f4, 0.0F, f5);
                } else {
                    GL11.glTranslatef(f4, 0.0F, f5 - 0.5F);
                }
                GL11.glRotatef(-NGTUtilClient.getMinecraft().getRenderManager().playerViewY - f, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(-0.25F, -0.25F, 0.25F);
                String s = this.displayStrings[j1];
                int i1 = fontrenderer.getStringWidth(s) / 2;
                fontrenderer.drawString(s, -i1 / 2, -10, j);
                GL11.glPopMatrix();
            }
        }

        GL11.glPopMatrix();
    }

    private void renderLine(TileEntityMarker marker, float x, float y, float z) {
        if (marker.linePos == null) {
            marker.linePos = new float[(marker.getRailMaps()).length][][];

            for (int i = 0; i < marker.linePos.length; ++i) {
                RailMap railmap = marker.getRailMaps()[i];
                RailPosition railposition = railmap.getStartRP();
                if (marker.getMarkerRP().equals(railposition)) {
                    int j = (int) ((float) railmap.getLength() * 2.0F);
                    double[] adouble = railmap.getRailPos(j, 0);
                    double d0 = railmap.getRailHeight(j, 0);
                    marker.linePos[i] = new float[j + 1][];

                    for (int k = 0; k < marker.linePos[i].length; ++k) {
                        double[] adouble1 = railmap.getRailPos(j, k);
                        marker.linePos[i][k] = new float[]{(float) (adouble1[1] - adouble[1]), (float) (railmap.getRailHeight(j, k) - d0), (float) (adouble1[0] - adouble[0])};
                    }
                }
            }
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);
        NGTTessellator ngttessellator = NGTTessellator.instance;

        for (int l = 0; l < marker.linePos.length; ++l) {
            if (marker.linePos[l] != null) {
                GL11.glPushMatrix();
                RailMap railmap1 = marker.getRailMaps()[l];
                float f = (float) (railmap1.getStartRP().posX - marker.getMarkerRP().posX);
                float f1 = (float) (railmap1.getStartRP().posY - marker.getMarkerRP().posY);
                float f2 = (float) (railmap1.getStartRP().posZ - marker.getMarkerRP().posZ);
                GL11.glTranslatef(f, f1, f2);
                ngttessellator.startDrawing(3);
                ngttessellator.setColorOpaque_I(16384);

                for (int i1 = 0; i1 < marker.linePos[l].length; ++i1) {
                    ngttessellator.addVertex(marker.linePos[l][i1][0], marker.linePos[l][i1][1], marker.linePos[l][i1][2]);
                }

                ngttessellator.draw();
                GL11.glPopMatrix();
            }
        }

        GL11.glPopMatrix();

        if (MainConfig.showRailLength) {
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, z);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            FontRenderer fontRenderer = getFontRenderer();
            for (RailMap rm : marker.getRailMaps()) {
                GL11.glPushMatrix();
                int split = (int) (rm.getLength() * 4.0D);
                double[] pos = rm.getRailPos(split, split / 2);
                float x0 = (float) (pos[1] - marker.getMarkerRP().posX);
                float y0 = (float) ((rm.getStartRP().posY + rm.getEndRP().posY) / 2 - marker.getMarkerRP().posY);
                float z0 = (float) (pos[0] - marker.getMarkerRP().posZ);
                GL11.glTranslatef(x0, y0, z0);
                GL11.glScalef(-0.05F, -0.05F, -0.05F);
                GL11.glRotatef(-NGTUtilClient.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
                String s = String.valueOf((float) Math.round(rm.getLength() * 10000) / 10000);
                int stringWidth = fontRenderer.getStringWidth(s);
                fontRenderer.drawString(s, -stringWidth / 2, -10, 0x00EE00);
                GL11.glPopMatrix();
            }

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }

    private void renderAnchor(TileEntityMarker marker, float x, float y, float z) {
        this.changeAnchor(marker);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);
        MarkerElement hoveredElement = MarkerElement.values()[marker.editMode];
        if (marker.editMode == 0) {
            hoveredElement = this.renderAnchorLine(marker, true, null);
        }

        if (marker.editMode == 0 && hoveredElement != MarkerElement.NONE && Mouse.isButtonDown(1)) {
            marker.editMode = hoveredElement.ordinal();
            marker.startPlayerPitch = NGTUtilClient.getMinecraft().player.rotationPitch;
            marker.startPlayerYaw = NGTUtilClient.getMinecraft().player.rotationYawHead;
            marker.startMarkerHeight = marker.getMarkerRP().height;
        }
        this.renderAnchorLine(marker, false, hoveredElement);
        GL11.glPopMatrix();
    }

    private MarkerElement renderAnchorLine(TileEntityMarker marker, boolean isPickMode, MarkerElement hoveredElement) {
        float lineWidth = (float) NGTUtilClient.getMinecraft().displayHeight * 0.01F;

        if (isPickMode) {
            GLHelper.startMousePicking(lineWidth * 2.0F);
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        float prevPointSize = GL11.glGetFloat(GL11.GL_POINT_SIZE);
        float prevLineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        GL11.glPointSize(lineWidth * 3.0F);
        GL11.glLineWidth(lineWidth);

        RailPosition rp = marker.getMarkerRP();
        int shadow = 0xC0C0C0;
        int color;

        if (marker.getState(MarkerState.LINE2) && marker.isCoreMarker()) {
            GL11.glPushMatrix();
            GL11.glRotatef(rp.anchorYaw, 0.0F, 1.0F, 0.0F);

            if (isPickMode) {
                GL11.glLoadName(MarkerElement.CONST_LIMIT_WP.ordinal());
            }
            color = MarkerElement.CONST_LIMIT_WP.getColor();
            color = (hoveredElement == MarkerElement.CONST_LIMIT_WP) ? ColorUtil.multiplicating(color, shadow) : color;
            this.renderLine(rp.constLimitWP, rp.constLimitHN, 0.0F, rp.constLimitWP, rp.constLimitHP, 0.0F, color);

            if (isPickMode) {
                GL11.glLoadName(MarkerElement.CONST_LIMIT_WN.ordinal());
            }
            color = MarkerElement.CONST_LIMIT_WN.getColor();
            color = (hoveredElement == MarkerElement.CONST_LIMIT_WN) ? ColorUtil.multiplicating(color, shadow) : color;
            this.renderLine(rp.constLimitWN, rp.constLimitHP, 0.0F, rp.constLimitWN, rp.constLimitHN, 0.0F, color);

            if (isPickMode) {
                GL11.glLoadName(MarkerElement.CONST_LIMIT_HP.ordinal());
            }
            color = MarkerElement.CONST_LIMIT_HP.getColor();
            color = (hoveredElement == MarkerElement.CONST_LIMIT_HP) ? ColorUtil.multiplicating(color, shadow) : color;
            this.renderLine(rp.constLimitWP, rp.constLimitHP, 0.0F, rp.constLimitWN, rp.constLimitHP, 0.0F, color);

            if (isPickMode) {
                GL11.glLoadName(MarkerElement.CONST_LIMIT_HN.ordinal());
            }
            color = MarkerElement.CONST_LIMIT_HN.getColor();
            color = (hoveredElement == MarkerElement.CONST_LIMIT_HN) ? ColorUtil.multiplicating(color, shadow) : color;
            this.renderLine(rp.constLimitWN, rp.constLimitHN, 0.0F, rp.constLimitWP, rp.constLimitHN, 0.0F, color);

            GL11.glPopMatrix();
        }

        if (marker.getState(MarkerState.LINE1)) {
            GL11.glPushMatrix();
            if (isPickMode) {
                GL11.glLoadName(MarkerElement.HEIGHT.ordinal());
            }
            color = MarkerElement.HEIGHT.getColor();
            color = (hoveredElement == MarkerElement.HEIGHT) ? ColorUtil.multiplicating(color, shadow) : color;
            this.renderLine(0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, color);

            GL11.glRotatef(rp.anchorYaw, 0.0F, 1.0F, 0.0F);
            if (isPickMode) {
                GL11.glLoadName(MarkerElement.HORIZONTIAL.ordinal());
            }
            color = MarkerElement.HORIZONTIAL.getColor();
            color = (hoveredElement == MarkerElement.HORIZONTIAL) ? ColorUtil.multiplicating(color, shadow) : color;
            this.renderLine(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, rp.anchorLengthHorizontal, color);

            GL11.glPushMatrix();
            GL11.glRotatef(-rp.anchorPitch, 1.0F, 0.0F, 0.0F);
            if (isPickMode) {
                GL11.glLoadName(MarkerElement.VERTICAL.ordinal());
            }
            color = MarkerElement.VERTICAL.getColor();
            color = (hoveredElement == MarkerElement.VERTICAL) ? ColorUtil.multiplicating(color, shadow) : color;
            this.renderLine(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, rp.anchorLengthVertical, color);
            GL11.glPopMatrix();

            GL11.glPushMatrix();
            float len = 1.0F;
            GL11.glRotatef(rp.cantEdge, 0.0F, 0.0F, 1.0F);
            if (isPickMode) {
                GL11.glLoadName(MarkerElement.CANT_EDGE.ordinal());
            }
            color = MarkerElement.CANT_EDGE.getColor();
            color = (hoveredElement == MarkerElement.CANT_EDGE) ? ColorUtil.multiplicating(color, shadow) : color;
            this.renderLine(0.0F, 0.0F, 0.0F, len, 0.0F, 0.0F, color);
            this.renderLine(0.0F, 0.0F, 0.0F, -len, 0.0F, 0.0F, color);
            GL11.glPopMatrix();

            GL11.glPushMatrix();
            if (marker.isCoreMarker() && marker.getRailMaps() != null && marker.getRailMaps().length == 1) {
                RailMap rm = marker.getRailMaps()[0];
                int max = (int) ((float) rm.getLength() * 2.0F);
                int index = max / 2;
                double[] pos0 = rm.getRailPos(max, 0);
                double[] pos = rm.getRailPos(max, index);
                double h0 = rm.getRailHeight(max, 0);
                double h = rm.getRailHeight(max, index);
                float yaw0 = rm.getRailRotation(max, 0);
                float yaw = rm.getRailRotation(max, index);
                GL11.glRotatef(-rp.anchorYaw, 0.0F, 1.0F, 0.0F);
                GL11.glTranslatef((float) (pos[1] - pos0[1]), (float) (h - h0), (float) (pos[0] - pos0[0]));
                GL11.glRotatef(rp.anchorYaw - (yaw0 - yaw), 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(rp.cantCenter, 0.0F, 0.0F, 1.0F);
                if (isPickMode) {
                    GL11.glLoadName(MarkerElement.CANT_CENTER.ordinal());
                }
                color = MarkerElement.CANT_CENTER.getColor();
                color = (hoveredElement == MarkerElement.CANT_CENTER) ? ColorUtil.multiplicating(color, shadow) : color;
                this.renderLine(0.0F, 0.0F, 0.0F, len, 0.0F, 0.0F, color);
                this.renderLine(0.0F, 0.0F, 0.0F, -len, 0.0F, 0.0F, color);
            }
            GL11.glPopMatrix();

            GL11.glPopMatrix();
        }

        GL11.glPointSize(prevPointSize);
        GL11.glLineWidth(prevLineWidth);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (marker.getState(MarkerState.LINE1) && !isPickMode) {
            FontRenderer fontRenderer = NGTUtilClient.getMinecraft().getRenderManager().getFontRenderer();
            float scale = 0.04F;
            GL11.glPushMatrix();
            GL11.glRotatef(-NGTUtilClient.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(-scale, -scale, scale);
            float x = 3.0F;
            float y = -34.0F;
            fontRenderer.drawString(String.valueOf(rp.height), x, y, MarkerElement.HEIGHT.getColor(), false);
            y += 6.0F;
            fontRenderer.drawString(String.valueOf(rp.anchorYaw), x, y, MarkerElement.HORIZONTIAL.getColor(), false);
            y += 6.0F;
            fontRenderer.drawString(String.valueOf(rp.anchorPitch), x, y, MarkerElement.VERTICAL.getColor(), false);
            y += 6.0F;
            fontRenderer.drawString(String.valueOf(rp.cantEdge), x, y, MarkerElement.CANT_EDGE.getColor(), false);
            y += 6.0F;
            fontRenderer.drawString(String.valueOf(rp.cantCenter), x, y, MarkerElement.CANT_CENTER.getColor(), false);
            GL11.glPopMatrix();
        }

        if (isPickMode) {
            int hits = GLHelper.finishMousePicking();
            if (hits > 0) {
                int pickedId = GLHelper.getPickedObjId(0);
                return MarkerElement.values()[pickedId];
            }
        }

        return MarkerElement.NONE;
    }

    public static void renderLine(float startX, float startY, float startZ, float endX, float endY, float endZ, int color) {
        NGTTessellator tessellator = NGTTessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorOpaque_I(color);
        tessellator.addVertex(startX, startY, startZ);
        tessellator.addVertex(endX, endY, endZ);
        tessellator.draw();

        tessellator.startDrawing(GL11.GL_POINTS);
        tessellator.setColorOpaque_I(color);
        tessellator.addVertex(endX, endY, endZ);
        tessellator.draw();
    }

    private boolean changeAnchor(TileEntityMarker marker) {
        if (marker.editMode == 0 || marker.getCoreMarker() == null) {
            return false;
        }

        if (marker.editMode > 0 && Mouse.isButtonDown(1)) {
            marker.editMode = 0;
            RTMCore.NETWORK_WRAPPER.sendToServer(new PacketMarkerRPClient(marker.getCoreMarker()));
        }

        MarkerElement curElm = MarkerElement.values()[marker.editMode];
        Minecraft mc = NGTUtilClient.getMinecraft();
        RailPosition rp = marker.getMarkerRP();

        float pitchDif = mc.player.rotationPitch - marker.startPlayerPitch;
        float yawDif = mc.player.rotationYawHead - marker.startPlayerYaw;

        if (marker.getState(MarkerState.LINE1)) {
            if (curElm == MarkerElement.HEIGHT) {
                int height = marker.startMarkerHeight + (int) (-pitchDif / 1.0F);
                height = height < 0 ? 0 : (height > 15 ? 15 : height);
                if (height != marker.getMarkerRP().height) {
                    rp.height = (byte) height;
                    rp.init();
                    marker.onChangeRailShape();
                    return true;
                }
                return false;
            } else if (curElm == MarkerElement.CANT_EDGE) {
                float cantLimit = 80.0F;
                float f12 = pitchDif < -cantLimit ? -cantLimit : (pitchDif > cantLimit ? cantLimit : pitchDif);
                RailPosition railposition2 = this.getNeighborRail(marker);
                if (railposition2 != null && marker.getState(MarkerState.FIT_NEIGHBOR)) {
                    f12 = -railposition2.cantEdge;
                }

                rp.cantEdge = f12;
                marker.onChangeRailShape();
                return true;
            } else if (curElm == MarkerElement.CANT_CENTER) {
                float cantLimit = 80.0F;
                float cant = pitchDif < -cantLimit ? -cantLimit : (pitchDif > cantLimit ? cantLimit : pitchDif);
                rp.cantCenter = cant;
                marker.onChangeRailShape();
                return true;
            }
        }

        if (marker.getState(MarkerState.LINE2)) {
            RailMap map = marker.getRailMaps()[0];

            if (curElm == MarkerElement.CONST_LIMIT_HP) {
                float height = 3.0F + (-pitchDif / 10.0F);
                height = (height < 1.9F) ? 1.9F : height;
                map.getStartRP().constLimitHP = map.getEndRP().constLimitHP = height;
                marker.onChangeRailShape();
                return true;
            } else if (curElm == MarkerElement.CONST_LIMIT_HN) {
                float height = (-pitchDif / 10.0F);
                height = (height > 0.0F) ? 0.0F : height;
                map.getStartRP().constLimitHN = map.getEndRP().constLimitHN = height;
                marker.onChangeRailShape();
                return true;
            } else if (curElm == MarkerElement.CONST_LIMIT_WP) {
                float width = 1.5F + (-yawDif / 10.0F);
                width = (width < 0.49F) ? 0.49F : width;
                map.getStartRP().constLimitWP = map.getEndRP().constLimitWP = width;
                marker.onChangeRailShape();
                return true;
            } else if (curElm == MarkerElement.CONST_LIMIT_WN) {
                float width = -1.5F + (-yawDif / 10.0F);
                width = (width > -0.49F) ? -0.49F : width;
                map.getStartRP().constLimitWN = map.getEndRP().constLimitWN = width;
                marker.onChangeRailShape();
                return true;
            }
        }

        if (marker.getState(MarkerState.LINE1)) {
            RayTraceResult target = BlockUtil.getMOPFromPlayer(mc.player, 128.0D, true);
            if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) {
                return false;
            }

            Vec3d targetVec = target.hitVec;
            boolean fitOpposite = false;
            RailPosition oppositeRP = this.getOppositeRail(marker);
            if (oppositeRP != null) {
                double dSq = NGTMath.getDistanceSq(targetVec.x, targetVec.z, oppositeRP.posX, oppositeRP.posZ);
                if (dSq <= FIT_RANGE_SQ) {
                    targetVec = new Vec3d(oppositeRP.posX, oppositeRP.posY, oppositeRP.posZ);
                    fitOpposite = true;
                }
            }

            if (marker.getState(MarkerState.ANCHOR21)) {
                double d0 = 2.0D / 3.0D;
                double x = (targetVec.x - rp.posX) * d0 + rp.posX;
                double y = (targetVec.y - rp.posY) * d0 + rp.posY;
                double z = (targetVec.z - rp.posZ) * d0 + rp.posZ;
                targetVec = new Vec3d(x, y, z);
            }

            double dx = targetVec.x - rp.posX;
            double dz = targetVec.z - rp.posZ;
            if (dx != 0.0D && dz != 0.0D) {
                RailPosition railposition3 = this.getNeighborRail(marker);
                float f2 = (float) Math.atan2(dx, dz);
                float f3 = NGTMath.toDegrees(f2);

                float f13 = (float) (Math.sqrt(dx * dx + dz * dz) * (dz >= 0.0D ? 1.0D : -1.0D));

                if (curElm == MarkerElement.HORIZONTIAL) {
                    if (railposition3 != null && marker.getState(MarkerState.FIT_NEIGHBOR)) {
                        f3 = MathHelper.wrapDegrees(railposition3.anchorYaw + 180.0F);
                    }
                    if (f3 == rp.anchorYaw && f13 == rp.anchorLengthHorizontal) {
                        return false;
                    }
                    rp.anchorYaw = f3;
                    rp.anchorLengthHorizontal = f13;
                } else if (curElm == MarkerElement.VERTICAL) {
                    float f4 = MathHelper.wrapDegrees(f3 - rp.anchorYaw);
                    if (railposition3 != null && marker.getState(MarkerState.FIT_NEIGHBOR)) {
                        f4 = -railposition3.anchorPitch;
                    } else if (fitOpposite) {
                        double dy = targetVec.y - rp.posY;
                        f4 = (float) NGTMath.toDegrees(Math.atan2(dy, NGTMath.firstSqrt(dx * dx + dz * dz)));
                    }
                    if (f4 == rp.anchorPitch && f13 == rp.anchorLengthVertical) {
                        return false;
                    }
                    rp.anchorPitch = f4;
                    rp.anchorLengthVertical = f13;
                }
                marker.onChangeRailShape();
                return true;
            }
        }

        return false;
    }

    private RailPosition getOppositeRail(TileEntityMarker tileEntity) {
        if (tileEntity.getRailMaps() == null) {
            return null;
        }

        RailPosition rp = tileEntity.getMarkerRP();
        RailPosition oppositeRP = null;
        for (RailMap map : tileEntity.getRailMaps()) {
            if (map.getStartRP().equals(rp)) {
                oppositeRP = map.getEndRP();
                break;
            } else if (map.getEndRP().equals(rp)) {
                oppositeRP = map.getStartRP();
                break;
            }
        }
        return oppositeRP;
    }

    private RailPosition getNeighborRail(TileEntityMarker tileEntity) {
        BlockPos neighborPos = tileEntity.getMarkerRP().getNeighborBlockPos();

        TileEntity tile = tileEntity.getWorld().getTileEntity(neighborPos);
        if (!(tile instanceof TileEntityLargeRailBase)) {
            return null;
        }

        TileEntityLargeRailCore core = ((TileEntityLargeRailBase) tile).getRailCore();
        if (core == null) {
            return null;
        }

        double distanceSq = Double.MAX_VALUE;
        RailPosition rp = null;
        for (RailMap map : core.getAllRailMaps()) {
            double d2 = NGTMath.getDistanceSq(tileEntity.getMarkerRP().posX, tileEntity.getMarkerRP().posZ, map.getStartRP().posX, map.getStartRP().posZ);
            if (d2 < distanceSq) {
                distanceSq = d2;
                rp = map.getStartRP();
            }

            d2 = NGTMath.getDistanceSq(tileEntity.getMarkerRP().posX, tileEntity.getMarkerRP().posZ, map.getEndRP().posX, map.getEndRP().posZ);
            if (d2 < distanceSq) {
                distanceSq = d2;
                rp = map.getEndRP();
            }
        }

        return rp;
    }

    public enum MarkerElement {
        NONE(0x000000),
        HORIZONTIAL(0x00FF20),
        VERTICAL(0xFF8800),
        CANT_EDGE(0xFF00FF),
        CANT_CENTER(0xFF00FF),
        HEIGHT(0xFF1000),
        CONST_LIMIT_HP(0x1060FF),
        CONST_LIMIT_HN(0x1060FF),
        CONST_LIMIT_WP(0x1060FF),
        CONST_LIMIT_WN(0x1060FF);

        public final int color;

        private MarkerElement(int par2) {
            this.color = par2;
        }

        public int getColor() {
            return this.color;
        }
    }
}