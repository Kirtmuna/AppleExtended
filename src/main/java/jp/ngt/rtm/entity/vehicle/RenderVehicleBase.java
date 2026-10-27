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

package jp.ngt.rtm.entity.vehicle;

import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.math.PooledVec3;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.ngtlib.renderer.GLHelper;
import jp.ngt.ngtlib.renderer.GLObject;
import jp.ngt.ngtlib.renderer.NGTObjectRenderer;
import jp.ngt.ngtlib.renderer.NGTTessellator;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.ngtlib.util.NGTUtilClient;
import jp.ngt.ngtlib.world.NGTWorld;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.entity.train.util.TrainState;
import jp.ngt.rtm.entity.train.util.TrainState.TrainStateType;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import jp.ngt.rtm.modelpack.cfg.VehicleBaseConfig.Light;
import jp.ngt.rtm.modelpack.cfg.VehicleBaseConfig.Rollsign;
import jp.ngt.rtm.modelpack.modelset.ModelSetVehicleBase;
import jp.ngt.rtm.render.PartsRenderer;
import jp.ngt.rtm.render.RenderPass;
import jp.ngt.rtm.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;

@SideOnly(Side.CLIENT)
public class RenderVehicleBase extends Render<EntityVehicleBase> {
    protected static final int DAYLIGHT_LIMIT = 7;
    private final FloatBuffer lightPlaneBuf = BufferUtils.createFloatBuffer(4);
    protected Vector3f lightVecF = new Vector3f();

    public RenderVehicleBase(RenderManager renderManager) {
        super(renderManager);
    }

    protected void renderVehicleBase(EntityVehicleBase vehicle, double par2, double par4, double par6, float par8, float par9) {
        GL11.glPushMatrix();
        GL11.glEnable(32826);
        GL11.glTranslated(par2, par4 + vehicle.getVehicleYOffset(), par6);

        float f = vehicle.prevRotationYaw + NGTMath.wrapAngle(vehicle.rotationYaw - vehicle.prevRotationYaw) * par9;
        GL11.glRotatef(f, 0.0F, 1.0F, 0.0F);
        float f1 = vehicle.prevRotationPitch + (vehicle.rotationPitch - vehicle.prevRotationPitch) * par9;
        GL11.glRotatef(-f1, 1.0F, 0.0F, 0.0F);
        float f2 = vehicle.prevRotationRoll + (vehicle.rotationRoll - vehicle.prevRotationRoll) * par9;
        GL11.glRotatef(f2, 0.0F, 0.0F, 1.0F);
        if (Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox()) {
            this.debugCollision(vehicle);
        }

        VehicleNGTO vehiclengto = vehicle.getNGTO();
        if (vehiclengto != null) {
            this.renderVehicleNGTO((EntityVehicle) vehicle, vehiclengto, par9);
        } else {
            ModelSetVehicleBase modelSet = (ModelSetVehicleBase) vehicle.getResourceState().getResourceSet();
            if (modelSet != null) {
                float[] fa = modelSet.getConfig().offset;
                GL11.glTranslated(fa[0], fa[1], fa[2]);
                this.renderVehicleMain(vehicle, modelSet, par9);
            } else {
                RTMCore.proxy.renderMissingModel();
            }
        }

        GL11.glPopMatrix();
    }

    protected void renderVehicleMain(EntityVehicleBase vehicle, ModelSetVehicleBase modelSet, float par4) {
        int pass = MinecraftForgeClient.getRenderPass();
        boolean smoothing = modelSet.getConfig().smoothing;
        boolean culling = modelSet.getConfig().doCulling;

        if (smoothing) {
            GL11.glShadeModel(GL11.GL_SMOOTH);
        }
        if (!culling) {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }

        if (pass == 0) {
            this.renderBodyNormal(vehicle, modelSet, par4);
        } else if (pass == 1) {
            if (modelSet.modelObj.light) {
                this.renderBodyLight(vehicle, modelSet, par4);
            }

            if (modelSet.rollsignTexture != null) {
                this.renderRollsign(vehicle, modelSet);
            }

            if (modelSet.modelObj.alphaBlend) {
                this.renderBodyTransparent(vehicle, modelSet, par4);
            }
        }

        if (!culling) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        if (smoothing) {
            GL11.glShadeModel(GL11.GL_FLAT);
        }

        if (pass == 1) {
            if (!NGTUtilClient.usingShader()) {
                GL11.glDisable(GL11.GL_CULL_FACE);
                this.renderLightEffect(vehicle, modelSet);
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
        }
    }

    private void preRenderBody(ModelSetVehicleBase modelSet, byte lightState, boolean useInteriorLighting, boolean customLighting) {
        Light[] lights = modelSet.getConfig().interiorLights;
        if (lightState > 0 && useInteriorLighting) {
            GLHelper.setLightmapMaxBrightness();
            float r = (lightState == TrainState.InteriorLight_On.data) ? 1.0F : -1.0F;
            for (int k = 0; k < lights.length; ++k) {
                float[] pos = lights[k].pos;
                RenderUtil.enableCustomLighting(k, pos[0], pos[1], pos[2], r, 1.0F, 1.0F);
            }
        } else if (customLighting) {
            GLHelper.setLightmapMaxBrightness();
            RenderUtil.enableCustomLighting(3, 128.0F, 256.0F, 128.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void postRenderBody(ModelSetVehicleBase modelSet, boolean useInteriorLighting, boolean customLighting) {
        Light[] lights = modelSet.getConfig().interiorLights;
        if (useInteriorLighting) {
            for (int k = 0; k < lights.length; ++k) {
                RenderUtil.disableCustomLighting(k);
            }
            GLHelper.enableLighting();
        } else if (customLighting) {
            RenderUtil.disableCustomLighting(3);
            GLHelper.enableLighting();
        }
    }

    private void renderBody(EntityVehicleBase vehicle, ModelSetVehicleBase modelSet, RenderPass pass, float par4) {
        modelSet.modelObj.preRender();
        modelSet.modelObj.renderWithTexture(vehicle, pass, par4);
        modelSet.modelObj.postRender();
    }

    private void renderBodyNormal(EntityVehicleBase vehicle, ModelSetVehicleBase modelSet, float par4) {
        GL11.glAlphaFunc(GL11.GL_EQUAL, 1.0F);

        int value = this.getLightValue(vehicle);
        byte state = vehicle.getVehicleState(TrainStateType.InteriorLight);
        boolean useInteriorLighting = modelSet.getConfig().interiorLights != null && value < DAYLIGHT_LIMIT;
        boolean customLighting = modelSet.modelObj.useShader;

        this.preRenderBody(modelSet, state, useInteriorLighting, customLighting);

        boolean maxBright = (state > 0 && useInteriorLighting) || customLighting;
        boolean gradient = !maxBright && this.beginLightGradient(vehicle, modelSet, par4);

        boolean canUseColor = (modelSet.getConfig().useCustomColor);
        if (canUseColor) {
            GLHelper.setColor(vehicle.getResourceState().color, 0xFF);
        }

        this.renderBody(vehicle, modelSet, RenderPass.NORMAL, par4);

        if (canUseColor) {
            GLHelper.setColor(0xFFFFFF, 0xFF);
        }

        if (gradient) {
            this.endLightGradient();
        }

        this.postRenderBody(modelSet, useInteriorLighting, customLighting);

        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
    }

    private void renderBodyLight(EntityVehicleBase vehicle, ModelSetVehicleBase modelSet, float par4) {
        boolean isTrain = vehicle instanceof EntityTrainBase;
        int dir = 0;
        byte mode = 0;
        boolean isFrontEmpty = false;
        boolean isBackEmpty = false;

        if (isTrain) {
            EntityTrainBase train = (EntityTrainBase) vehicle;
            dir = train.getTrainDirection();
            mode = train.getVehicleState(TrainStateType.Light);
            isFrontEmpty = train.getConnectedTrain(dir) == null;
            isBackEmpty = train.getConnectedTrain(1 - dir) == null;
        }

        for (int i = 0; i < 3; ++i) {
            boolean doRender = false;
            if (isTrain && ((TrainConfig) modelSet.getConfig()).isSingleTrain && isFrontEmpty && isBackEmpty) {
                switch (i) {
                    case 0:
                        doRender = (mode == 0 || mode == 1);
                        break;
                    case 1:
                        doRender = (mode == 1 && dir == 0) || mode == 2;
                        break;
                    case 2:
                        doRender = (mode == 1 && dir == 1) || mode == 2;
                        break;
                }
            } else {
                switch (i) {
                    case 0:
                        doRender = (mode == 0 || mode == 1);
                        break;
                    case 1:
                        doRender = (mode == 1 && isFrontEmpty) || mode == 2;
                        break;
                    case 2:
                        doRender = (mode == 1 && !isFrontEmpty && isBackEmpty) || mode == 2;
                        break;
                }
            }

            if (doRender) {
                boolean isLightON = (i > 0);

                if (isLightON) {
                    GLHelper.disableLighting();
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.8F);
                    GLHelper.setLightmapMaxBrightness();
                }

                RenderPass pass;
                switch (i) {
                    case 1:
                        pass = RenderPass.LIGHT_FRONT;
                        break;
                    case 2:
                        pass = RenderPass.LIGHT_BACK;
                        break;
                    default:
                        pass = RenderPass.LIGHT;
                }
                boolean gradient = !isLightON && this.beginLightGradient(vehicle, modelSet, par4);
                this.renderBody(vehicle, modelSet, pass, par4);
                if (gradient) {
                    this.endLightGradient();
                }

                if (isLightON) {
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                    GLHelper.enableLighting();
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        }

        if (modelSet.modelObj.renderer.shouldRenderOutline(vehicle)) {
            GLHelper.disableLighting();
            GLHelper.setLightmapMaxBrightness();
            modelSet.modelObj.renderWithTexture(vehicle, RenderPass.OUTLINE, par4);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GLHelper.enableLighting();
        }
    }

    private void renderBodyTransparent(EntityVehicleBase vehicle, ModelSetVehicleBase modelSet, float par4) {
        GL11.glAlphaFunc(GL11.GL_LESS, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        //boolean gradient = this.beginLightGradient(vehicle, modelSet, par4);
        boolean gradient = false; // 透過テクスチャの挙動が変なのでとりま勾配OFF
        this.renderBody(vehicle, modelSet, RenderPass.TRANSPARENT, par4);
        if (gradient) {
            this.endLightGradient();
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
    }

    protected void renderRollsign(EntityVehicleBase vehicle, ModelSetVehicleBase modelset) {
        GL11.glPushMatrix();
        this.bindTexture(modelset.rollsignTexture);

        for (int i = 0; i < modelset.getConfig().rollsigns.length; ++i) {
            Rollsign rollsign = modelset.getConfig().rollsigns[i];

            if (!rollsign.disableLighting) {
                GLHelper.disableLighting();
                GLHelper.setLightmapMaxBrightness();
            }

            float f0 = (rollsign.uv[3] - rollsign.uv[2]) / modelset.getConfig().rollsignNames.length;
            float uMin = rollsign.uv[0];
            float uMax = rollsign.uv[1];
            float f1 = 0.0F;
            if (vehicle instanceof EntityTrainBase) {
                EntityTrainBase train = (EntityTrainBase) vehicle;
                f1 = rollsign.doAnimation ? train.getRollsignAnimation() : train.getVehicleState(TrainStateType.Destination);
            }
            float vMin = rollsign.uv[2] + f0 * f1;
            float vMax = rollsign.uv[2] + f0 * (f1 + 1.0F);

            NGTTessellator tessellator = NGTTessellator.instance;
            tessellator.startDrawingQuads();
            for (int j = 0; j < modelset.getConfig().rollsigns[i].pos.length; ++j) {
                tessellator.addVertexWithUV(rollsign.pos[j][3][0], rollsign.pos[j][3][1], rollsign.pos[j][3][2], uMin, vMin);
                tessellator.addVertexWithUV(rollsign.pos[j][2][0], rollsign.pos[j][2][1], rollsign.pos[j][2][2], uMin, vMax);
                tessellator.addVertexWithUV(rollsign.pos[j][1][0], rollsign.pos[j][1][1], rollsign.pos[j][1][2], uMax, vMax);
                tessellator.addVertexWithUV(rollsign.pos[j][0][0], rollsign.pos[j][0][1], rollsign.pos[j][0][2], uMax, vMin);
            }
            tessellator.draw();

            if (!rollsign.disableLighting) {
                GLHelper.enableLighting();
            }
        }

        GL11.glPopMatrix();
    }

    private int getLightValue(EntityVehicleBase vehicle) {
        World world = NGTUtil.getClientWorld();
        int x = NGTMath.floor(vehicle.posX);
        int y = NGTMath.floor(vehicle.posY + 0.5D);
        int z = NGTMath.floor(vehicle.posZ);
        BlockPos pos = new BlockPos(x, y, z);
        int skyLight1 = NGTMath.floor(world.getSunBrightness(1.0F) * 15.0F);
        int skyLight2 = world.getLightFor(EnumSkyBlock.SKY, pos);
        int skyLight = skyLight1 * skyLight2 / 15;
        int blockLight = world.getLightFor(EnumSkyBlock.BLOCK, pos);
        return skyLight > blockLight ? skyLight : blockLight;
    }

    private boolean beginLightGradient(EntityVehicleBase vehicle, ModelSetVehicleBase modelSet, float partialTick) {
        if (NGTUtilClient.usingShader() || !(modelSet.getConfig() instanceof TrainConfig)
                || this.isBodyMaxBright(vehicle, modelSet)) {
            return false;
        }
        double half = ((TrainConfig) modelSet.getConfig()).trainDistance;
        if (half < 0.5D) {
            return false;
        }

        double cx = vehicle.lastTickPosX + (vehicle.posX - vehicle.lastTickPosX) * partialTick;
        double cy = vehicle.lastTickPosY + (vehicle.posY - vehicle.lastTickPosY) * partialTick;
        double cz = vehicle.lastTickPosZ + (vehicle.posZ - vehicle.lastTickPosZ) * partialTick;
        float yawDeg = vehicle.prevRotationYaw
                + NGTMath.wrapAngle(vehicle.rotationYaw - vehicle.prevRotationYaw) * partialTick;
        double yaw = Math.toRadians(yawDeg);
        double dx = Math.sin(yaw) * half;
        double dz = Math.cos(yaw) * half;
        int y = NGTMath.floor(cy + 0.5D);

        World world = vehicle.world;
        BlockPos frontPos = new BlockPos(NGTMath.floor(cx + dx), y, NGTMath.floor(cz + dz));
        BlockPos rearPos = new BlockPos(NGTMath.floor(cx - dx), y, NGTMath.floor(cz - dz));
        if (!world.isBlockLoaded(frontPos) || !world.isBlockLoaded(rearPos)) {
            return false;
        }
        int front = world.getCombinedLight(frontPos, 0);
        int rear = world.getCombinedLight(rearPos, 0);
        if (front == rear) {
            return false;
        }

        float uF = front & 0xFFFF, vF = front >> 16;
        float uR = rear & 0xFFFF, vR = rear >> 16;
        float len2 = (float) (2.0D * half);
        float offsetZ = modelSet.getConfig().offset[2];

        GLHelper.beginLightGradientState();
        OpenGlHelper.setActiveTexture(
                OpenGlHelper.lightmapTexUnit);
        this.setLightPlane(GL11.GL_S, (uF - uR) / len2, (uF + uR) * 0.5F, offsetZ);
        this.setLightPlane(GL11.GL_T, (vF - vR) / len2, (vF + vR) * 0.5F, offsetZ);
        OpenGlHelper.setActiveTexture(
                OpenGlHelper.defaultTexUnit);
        return true;
    }

    private boolean isBodyMaxBright(EntityVehicleBase vehicle, ModelSetVehicleBase modelSet) {
        byte state = vehicle.getVehicleState(TrainStateType.InteriorLight);
        boolean useInteriorLighting = modelSet.getConfig().interiorLights != null
                && this.getLightValue(vehicle) < DAYLIGHT_LIMIT;
        return (state > 0 && useInteriorLighting) || modelSet.modelObj.useShader;
    }

    private void setLightPlane(int coord, float k, float center, float offsetZ) {
        this.lightPlaneBuf.clear();
        this.lightPlaneBuf.put(0.0F).put(0.0F).put(k).put(center + k * offsetZ);
        this.lightPlaneBuf.flip();
        GL11.glTexGeni(coord, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_EYE_LINEAR);
        GL11.glTexGen(coord, GL11.GL_EYE_PLANE, this.lightPlaneBuf);
        GL11.glEnable(coord == GL11.GL_S ? GL11.GL_TEXTURE_GEN_S : GL11.GL_TEXTURE_GEN_T);
    }

    private void endLightGradient() {
        GLHelper.endLightGradientState();
        OpenGlHelper.setActiveTexture(
                OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
        OpenGlHelper.setActiveTexture(
                OpenGlHelper.defaultTexUnit);
    }

    private void renderLightEffect(EntityVehicleBase vehicle, ModelSetVehicleBase modelset) {
        boolean isTrain = vehicle instanceof EntityTrainBase;
        int dir = 0;
        byte mode = vehicle.getVehicleState(TrainStateType.Light);
        boolean b0 = false;
        boolean b1 = false;

        if (isTrain) {
            EntityTrainBase train = (EntityTrainBase) vehicle;
            dir = train.getTrainDirection();
            b0 = train.getConnectedTrain(dir) == null;
            b1 = train.getConnectedTrain(1 - dir) == null;
        } else {
            b0 = b1 = true;
        }

        if (modelset.getConfig().headLights == null || modelset.getConfig().tailLights == null || mode <= 0) {
            return;
        }

        GL11.glPushMatrix();
        GLHelper.disableLighting();
        GLHelper.setLightmapMaxBrightness();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_ALPHA_TEST);

        int value = this.getLightValue(vehicle);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);

        int renderModeHead = -1;
        int renderModeTail = -1;
        boolean b2 = isTrain && ((TrainConfig) modelset.getConfig()).isSingleTrain && b0 && b1;
        if (b2) {
            renderModeHead = (mode == 1) ? dir : (mode == 2 ? 2 : -1);
            renderModeTail = (mode == 1) ? 1 - dir : (mode == 2 ? 2 : -1);
        } else {
            renderModeHead = ((mode == 1 && b0) || mode == 2) ? 0 : -1;
            renderModeTail = ((mode == 1 && !b0 && b1) || mode == 2) ? 0 : -1;
        }

        Vec3 vec = PooledVec3.create(0.0D, 0.0D, 1.0D);
        vec = vec.rotateAroundY(vehicle.rotationYaw);
        vec = vec.rotateAroundZ(vehicle.rotationPitch);
        this.lightVecF.set((float) vec.getX(), (float) vec.getY(), (float) vec.getZ());

        if (renderModeHead >= 0) {
            for (Light light : modelset.getConfig().headLights) {
                if (light.type == 1 && value > DAYLIGHT_LIMIT) {
                    continue;
                }
                this.renderLightEffect(vehicle, this.lightVecF, light, renderModeHead);
            }
        }

        if (renderModeTail >= 0) {
            for (Light light : modelset.getConfig().headLights) {
                if (light.type == 1 && value > DAYLIGHT_LIMIT) {
                    continue;
                }
                this.renderLightEffect(vehicle, this.lightVecF, light, renderModeTail);
            }
        }

        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GLHelper.enableLighting();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private void renderLightEffect(EntityVehicleBase vehicle, Vector3f normal, Light light, int mode) {
        if (mode == 2) {
            this.renderLightEffect(vehicle, normal, light, 0);
            this.renderLightEffect(vehicle, normal, light, 1);
            return;
        }

        Vec3 vec = PooledVec3.create(light.pos[0], light.pos[1], light.pos[2]);
        vec = vec.rotateAroundY(vehicle.rotationYaw);
        vec = vec.rotateAroundZ(vehicle.rotationPitch);
        vec = vec.add(vehicle.posX, vehicle.posY, vehicle.posZ);

        GL11.glPushMatrix();
        GL11.glTranslatef(light.pos[0], light.pos[1], mode == 0 ? light.pos[2] : -light.pos[2]);
        if (mode == 1) {
            GL11.glScalef(1.0F, 1.0F, -1.0F);
        }

        PartsRenderer.renderLightEffectS(normal, vec.getX(), vec.getY(), vec.getZ(),
                light.r, 0.0625F, 16.0F, light.color, light.type, (mode == 1));

        if (mode == 1) {
            GL11.glScalef(1.0F, 1.0F, -1.0F);
        }

        GL11.glPopMatrix();
    }

    protected void renderVehicleNGTO(EntityVehicle vehicle, VehicleNGTO vngto, float par4) {
        GL11.glScalef(vngto.scale, vngto.scale, vngto.scale);
        GL11.glTranslatef(vngto.offsetX, vngto.offsetY, vngto.offsetZ);

        int pass = MinecraftForgeClient.getRenderPass();
        if (pass < 0 || pass > 1) {
            return;
        }

        if (vngto.glLists == null) {
            vngto.glLists = new GLObject[2];
        }

        GLHelper.disableLighting();
        GLHelper.setLightmapMaxBrightness();

        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        boolean smoothing = NGTUtilClient.getMinecraft().gameSettings.ambientOcclusion != 0;
        if (smoothing) {
            GL11.glShadeModel(GL11.GL_SMOOTH);
        }

        if (!GLHelper.isValid(vngto.glLists[pass])) {
            vngto.glLists[pass] = GLHelper.generateGLList(vngto.glLists[pass]);
            GLHelper.startCompile(vngto.glLists[pass]);
            NGTWorld world = new NGTWorld(vehicle.getEntityWorld(), vngto.ngto);
            NGTObjectRenderer.INSTANCE.renderNGTObject(world, vngto.ngto, false, 0, pass);
            GLHelper.endCompile();
        } else {
            GLHelper.callList(vngto.glLists[pass]);
        }

        if (smoothing) {
            GL11.glShadeModel(GL11.GL_FLAT);
        }
        GLHelper.enableLighting();
        NGTUtilClient.getMinecraft().entityRenderer.enableLightmap();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityVehicleBase par1) {
        return null;
    }

    @Override
    protected boolean bindEntityTexture(EntityVehicleBase entiy) {
        return false;
    }

    @Override
    public void doRender(EntityVehicleBase par1, double par2, double par4, double par6, float par8, float par9) {
    }

    @Override
    public boolean shouldRender(EntityVehicleBase entity, ICamera camera, double camX, double camY, double camZ) {
        return false;
    }

    private void debugCollision(EntityVehicleBase vehicle) {
        if (MinecraftForgeClient.getRenderPass() == 1) {
            ModelSetVehicleBase modelSet = (ModelSetVehicleBase) vehicle.getResourceState().getResourceSet();
            if (modelSet != null) {
                if (modelSet.getCollisionObj() != null) {
                    modelSet.getCollisionObj().checkAndRenderCollision(
                            Minecraft.getMinecraft().player, vehicle, vehicle.getResourceState().exclusionParts);
                }
            }
        }
    }
}