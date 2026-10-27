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

package jp.ngt.rtm.render;

import com.anatawa12.fixRtm.asm.config.MainConfig;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.io.ScriptUtil;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.renderer.GLHelper;
import jp.ngt.ngtlib.renderer.NGTTessellator;
import jp.ngt.ngtlib.util.MCWrapperClient;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.ngtlib.util.NGTUtilClient;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.modelset.ModelSetBase;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import javax.script.ScriptEngine;
import java.net.URLClassLoader;
import java.util.*;

import jp.apple.appleextended.script.api.gif.ScriptGifRenderer;

@SideOnly(Side.CLIENT)
public abstract class PartsRenderer<T, MS extends ModelSetBase> {
    public static final String PACKAGE_NAME = "jp.ngt.rtm.render";
    public static final Calendar CALENDAR = Calendar.getInstance();
    public static final ResourceLocation TEX_LIGHT = new ResourceLocation("rtm", "textures/effect/light.png");

    protected MS modelSet;
    protected ModelObject modelObj;
    private ScriptEngine script;
    protected List<Parts> partsList = new ArrayList<Parts>();
    protected List<ActionParts> targetsList = new ArrayList<ActionParts>();
    protected Map<Integer, Object> dataMap = new HashMap<Integer, Object>();

    public int currentMatId;
    public int currentPass;
    public T hittedEntity;
    protected Map<T, ActionParts> hittedParts = new HashMap<>();
    private int mouseHoldCount;
    private int dragStartPos;

    public PartsRenderer(String... par1) {
    }

    public void setScript(ScriptEngine se, ResourceLocation rl) {
        this.script = se;
    }

    public ScriptEngine getScript() {
        return this.script;
    }

    private void execScriptFunc(String func, Object... args) {
        try {
            ScriptUtil.doScriptFunction(this.script, func, args);
        } catch (Throwable e) {
            try {
                if (this.script instanceof GroovyScriptEngineImpl) {
                    ClassLoader cl = ((GroovyScriptEngineImpl) this.script).getClassLoader();
                    NGTLog.debug("[PartsRenderer] exec classloader identityHash=" + System.identityHashCode(cl) + " class=" + cl.getClass().getName());
                    if (cl instanceof URLClassLoader) {
                        NGTLog.debug("[PartsRenderer] urls=" + Arrays.toString(((URLClassLoader) cl).getURLs()));
                    }
                    NGTLog.debug("[PartsRenderer] classloader toString=" + cl.toString());
                }
            } catch (Throwable t2) {
                NGTLog.debug("[PartsRenderer] failed to inspect classloader: " + t2);
            }
            throw new RuntimeException("On " + func + " script : " + this.modelSet.getConfig().getName(), e);
        }
    }

    public Parts registerParts(Parts par1) {
        this.partsList.add(par1);
        if (par1 instanceof ActionParts) {
            ActionParts actionParts = (ActionParts) par1;
            actionParts.id = this.targetsList.size() + 1;
            this.targetsList.add(actionParts);
        }
        return par1;
    }

    public void init(MS par1, ModelObject par2) {
        this.modelSet = par1;
        this.modelObj = par2;

        if (this.script != null) {
            this.execScriptFunc("init", par1, par2);
        }

        for (Parts parts : this.partsList) {
            parts.init(this);
        }
    }

    public void preRender(T t, boolean smoothing, boolean culling, float par3) {
    }

    public void postRender(T t, boolean smoothing, boolean culling, float par3) {
    }

    private ActionParts selectHits(T t, int hits) {
        if (true) {
            int i = 1;
            double d0 = Double.MAX_VALUE;

            for (int j = 0; j < hits; ++j) {
                double d1 = GLHelper.getRealDistance(GLHelper.getPickedObjDepth(j));
                if (0 < d1 && d1 < d0) {
                    int k = GLHelper.getPickedObjId(j);
                    i = k;
                    d0 = d1;
                }
            }

            if (!MainConfig.actionPartsImprovements
                    || d0 <= NGTUtilClient.getMinecraft().playerController.getBlockReachDistance())
                return this.targetsList.get(i - 1);
        }

        return Mouse.isButtonDown(1) ? this.hittedParts.get(t) : null;
    }

    private void checkMouseAction(T t) {
        if (Mouse.isButtonDown(1)) {
            ActionParts actionparts = this.hittedParts.get(t);
            if (actionparts != null && this.hittedEntity == t) {
                if (actionparts.behavior == ActionType.TOGGLE) {
                    if (this.mouseHoldCount == 0) {
                        this.onRightClick(this.hittedEntity, actionparts);
                    }

                    ++this.mouseHoldCount;
                } else {
                    int i = actionparts.behavior == ActionType.DRAG_X ? Mouse.getX() : Mouse.getY();
                    if (this.mouseHoldCount == 0) {
                        this.dragStartPos = i;
                    }

                    this.onRightDrag(t, actionparts, i - this.dragStartPos);
                    ++this.mouseHoldCount;
                }
            }
        } else {
            this.mouseHoldCount = 0;
            this.dragStartPos = 0;
        }
    }

    private void onRightClick(T t, ActionParts parts) {
        this.execScriptFunc("onRightClick", t, parts);
    }

    private void onRightDrag(T t, ActionParts parts, int move) {
        this.execScriptFunc("onRightDrag", t, parts, move);
    }

    public void render(T t, RenderPass pass, float partialTick) {
        if (t != null && pass == RenderPass.NORMAL && this.currentMatId == 0 && !this.targetsList.isEmpty()) {
            this.render(t, RenderPass.PICK, partialTick);
        }

        if (pass == RenderPass.PICK
                && MainConfig.actionPartsImprovements
                && NGTUtilClient.getMinecraft().currentScreen != null) {
            this.hittedParts.put(t, null);
            this.checkMouseAction(t);
            return;
        }

        this.currentPass = pass.id;
        if (pass == RenderPass.PICK) {
            GLHelper.startMousePicking(1.0F);
        }

        if (this.modelObj != null) {
            ScriptGifRenderer.setCurrentModelObject(this.modelObj);
        }

        this.execScriptFunc("render", t, pass.id, partialTick);

        ScriptGifRenderer.clearCurrentModelObject();

        if (pass == RenderPass.PICK) {
            int hits = GLHelper.finishMousePicking();
            ActionParts actionparts = this.selectHits(t, hits);
            if (actionparts != null) {
                this.hittedEntity = t;
            }

            this.hittedParts.put(t, actionparts);
            this.checkMouseAction(t);
        }

    }

    public boolean shouldRenderOutline(Object entity) {
        return this.hittedEntity == entity && this.hittedParts.get(entity) != null;
    }

    public String getModelName() {
        return this.modelSet.getConfig().getName();
    }

    @Deprecated
    public float sigmoid(float par1) {
        return (float) NGTMath.sigmoid(par1, 5.0D);
    }

    public void rotate(float angle, char axis, float x, float y, float z) {
        GL11.glTranslatef(x, y, z);
        switch (axis) {
            case 'X':
                GL11.glRotatef(angle, 1.0F, 0.0F, 0.0F);
                break;
            case 'Y':
                GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);
                break;
            case 'Z':
                GL11.glRotatef(angle, 0.0F, 0.0F, 1.0F);
                break;
        }
        GL11.glTranslatef(-x, -y, -z);
    }

    public int getMCTime() {
        return (int) NGTUtil.getClientWorld().getWorldTime() % 24000;
    }

    public int getMCHour() {
        int t0 = this.getMCTime();
        return ((t0 / 1000) + 6) % 24;
    }

    public int getMCMinute() {
        int t0 = this.getMCTime();
        return (int) ((float) (t0 % 1000) * 0.06F);
    }

    public int getSystemTime() {
        return (int) ((System.currentTimeMillis() / 1000L) % 86400L);
    }

    public long getSystemTimeMillis() {
        return System.currentTimeMillis();
    }

    public int getSystemHour() {
        return CALENDAR.get(Calendar.HOUR_OF_DAY);
    }

    public int getSystemMinute() {
        return CALENDAR.get(Calendar.MINUTE);
    }

    public int getSystemSecond() {
        return CALENDAR.get(Calendar.SECOND);
    }

    public int getSystemMillisecond() {
        return CALENDAR.get(Calendar.MILLISECOND);
    }

    public Object getData(int id) {
        if (this.dataMap.containsKey(id)) {
            return this.dataMap.get(id);
        }
        return 0;
    }

    public void setData(int id, Object value) {
        this.dataMap.put(id, value);
    }

    public static Vector3f getViewerVec(double x, double y, double z) {
        Entity viewer = NGTUtilClient.getMinecraft().getRenderViewEntity();
        float vx = (float) (viewer.posX - x);
        float vy = (float) (viewer.posY + (double) viewer.getEyeHeight() - y);
        float vz = (float) (viewer.posZ - z);
        return new Vector3f(vx, vy, vz);
    }

    public void renderLightEffect(Vector3f normal, double[] pos, float rL, float rS, float length, int color, int type, boolean reverse) {
        GL11.glDisable(GL11.GL_CULL_FACE);

        GLHelper.disableLighting();
        GLHelper.setLightmapMaxBrightness();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);

        renderLightEffectS(normal, pos[0], pos[1], pos[2], rL, rS, length, color, type, reverse);

        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GLHelper.enableLighting();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    protected static final double BRIGHTNESS_RATE = 1.0D / 256.0D;
    protected static final boolean DEBUG = false;
    protected static final byte DIV_NUM = 32;
    protected static final float ANGLE = 360.0F / DIV_NUM;

    public static void renderLightEffectS(Vector3f normal, double x, double y, double z, float rL, float rS, float length, int color, int type, boolean reverse) {
        boolean useVec = (normal != null);
        Vector3f viewerVec = null;
        float viewerAngle = 0.0F;
        if (useVec) {
            viewerVec = getViewerVec(x, y, z);
            viewerAngle = NGTMath.toDegrees(Vector3f.angle(normal, viewerVec));
        }

        if (reverse) {
            viewerAngle = NGTMath.wrapAngle(viewerAngle + 180.0F);
        }

        if (viewerAngle > 90.0F) {
            viewerAngle = 180.0F - viewerAngle;
        }

        float lightStrength = 1.0F;
        if (viewerAngle > 45.0F) {
            lightStrength = (90.0F - viewerAngle) / 45.0F;
        }

        NGTTessellator tessellator = NGTTessellator.instance;

        if (DEBUG && useVec) {
            tessellator.startDrawing(GL11.GL_LINES);
            tessellator.setColorRGBA_I(0xFF0000, 0xFF);
            tessellator.addVertex(0.0F, 0.0F, 0.0F);
            tessellator.addVertex(normal.x, normal.y, normal.z);
            tessellator.setColorRGBA_I(0x00FF00, 0xFF);
            tessellator.addVertex(0.0F, 0.0F, 0.0F);
            tessellator.addVertex(viewerVec.x, viewerVec.y, viewerVec.z);
            tessellator.draw();
        }

        if (type == 0) {
            tessellator.startDrawing(GL11.GL_TRIANGLE_FAN);
            tessellator.setColorRGBA_I(color, 0xFF);
            tessellator.addVertex(0.0F, 0.0F, 0.0F);
            tessellator.setColorRGBA_I(0x000000, 0x00);
            for (int i = 0; i <= DIV_NUM; ++i) {
                float rad = NGTMath.toRadians((float) i * ANGLE);
                tessellator.addVertex(NGTMath.getCos(rad) * rL * lightStrength, NGTMath.getSin(rad) * rL * lightStrength, 0.0F);
            }
            tessellator.draw();
        } else if (type == 1) {
            float angle = NGTMath.toDegrees((float) Math.atan2(rL, length));
            float distance = 256.0F;
            if (useVec) {
                distance = viewerVec.lengthSquared();
            }

            float brightness = 0.0F;
            if (viewerAngle < angle) {
                brightness = 1.0F - (viewerAngle / angle);
            } else {
                float b0 = ((viewerAngle - angle) / (90.0F - angle));
                float b1 = (float) ((double) distance * BRIGHTNESS_RATE);
                if (b1 > 1.0F) {
                    b1 = 1.0F;
                }
                brightness = b0 * b1;
            }

            if (brightness > 0.0F) {
                int alpha = (int) (255.0F * brightness);
                tessellator.startDrawing(GL11.GL_TRIANGLE_FAN);
                tessellator.setColorRGBA_I(color, alpha);
                tessellator.addVertex(0.0F, 0.0F, 0.0F);
                tessellator.setColorRGBA_I(0x000000, 0x00);
                for (int i = 0; i <= DIV_NUM; ++i) {
                    float rad = NGTMath.toRadians((float) -i * ANGLE);
                    tessellator.addVertex(NGTMath.getCos(rad) * rL, NGTMath.getSin(rad) * rL, length);
                }
                tessellator.draw();

                float b3 = (float) ((double) distance * BRIGHTNESS_RATE);
                if (b3 > 1.0F) {
                    b3 = 1.0F;
                }
                float f3 = rS * b3;

                tessellator.startDrawing(GL11.GL_TRIANGLES);
                for (int i = 0; i <= DIV_NUM; ++i) {
                    float rad = NGTMath.toRadians((float) i * ANGLE);
                    float sin = NGTMath.getSin(rad);
                    float cos = NGTMath.getCos(rad);
                    tessellator.setColorRGBA_I(0x000000, 0x00);
                    tessellator.addVertex(cos * rL, sin * rL, length);
                    tessellator.setColorRGBA_I(color, alpha >> 1);
                    tessellator.addVertex(0.0F, 0.0F, 0.0F);
                    tessellator.setColorRGBA_I(0x000000, 0x00);
                    tessellator.addVertex(cos * f3, sin * f3, 0.0F);
                }
                tessellator.draw();
            }
        }
    }

    public void bindTexture(ResourceLocation texture) {
        NGTUtilClient.bindTexture(texture);
    }

    public abstract World getWorld(T entity);

    public int getColor(T entity) {
        if (entity instanceof IResourceSelector) {
            return ((IResourceSelector) entity).getResourceState().color;
        }
        return 0;
    }

    public ItemStack getInventoryItem(T entity, int slotIndex) {
        if (entity instanceof IInventory) {
            IInventory inv = (IInventory) entity;
            if (slotIndex >= 0 && slotIndex < inv.getSizeInventory()) {
                return inv.getStackInSlot(slotIndex);
            }
        }
        return null;
    }

    public int getStackSize(ItemStack stack) {
        return stack.getCount();
    }

    public void renderItem(T entity, ItemStack stack) {
        GL11.glPushMatrix();
        GLHelper.enableLighting();
        NGTUtilClient.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
        GLHelper.enableLighting();
        GL11.glPopMatrix();
    }

    @Deprecated
    public void spawnParticle(T entity, String name, double posX, double posY, double posZ, double speedX, double speedY, double speedZ) {
        MCWrapperClient.spawnParticle(this.getWorld(entity), name, posX, posY, posZ, speedX, speedY, speedZ);
    }

    @Deprecated
    public void playSound(T entity, String name, double posX, double posY, double posZ, float volume, float pitch, boolean distanceDelay) {
        MCWrapperClient.playSound(this.getWorld(entity), name, posX, posY, posZ, volume, pitch, distanceDelay);
    }

    public void debug(String msg, Object... args) {
        NGTLog.debug(msg, args);
    }

    public static boolean validPath(String par1) {
        return par1 != null && par1.length() > 0;
    }
}