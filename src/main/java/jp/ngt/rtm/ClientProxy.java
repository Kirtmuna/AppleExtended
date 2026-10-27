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

package jp.ngt.rtm;

import jp.apple.appleextended.config.AppleConfig;
import jp.apple.appleextended.train.render.TrainBodySway;
import jp.ngt.ngtlib.io.NGTFileLoader;
import jp.ngt.ngtlib.io.NGTJson;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.io.NGTText;
import jp.ngt.ngtlib.item.ItemRenderHandler;
import jp.ngt.ngtlib.renderer.NGTParticle;
import jp.ngt.ngtlib.sound.MovingSoundCustom;
import jp.ngt.ngtlib.sound.NGTSound;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.ngtlib.util.NGTUtilClient;
import jp.ngt.ngtlib.util.PackInfo;
import jp.ngt.ngtlib.util.VersionChecker;
import jp.ngt.rtm.block.ParticleSpark;
import jp.ngt.rtm.block.tileentity.RenderDecoration;
import jp.ngt.rtm.entity.EntityMeltedMetalFX;
import jp.ngt.rtm.entity.train.EntityBogie;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.entity.train.parts.EntityArtillery;
import jp.ngt.rtm.entity.train.util.FormationManager;
import jp.ngt.rtm.entity.vehicle.EntityVehicle;
import jp.ngt.rtm.entity.vehicle.EntityVehicleBase;
import jp.ngt.rtm.entity.vehicle.IUpdateVehicle;
import jp.ngt.rtm.event.RTMEventHandlerClient;
import jp.ngt.rtm.event.RTMKeyHandlerClient;
import jp.ngt.rtm.event.RTMTickHandlerClient;
import jp.ngt.rtm.gui.camera.Camera;
import jp.ngt.rtm.modelpack.init.ModelPackLoadThread;
import jp.ngt.rtm.modelpack.model.ModelMissing;
import jp.ngt.rtm.modelpack.modelset.ModelSetFirearm;
import jp.ngt.rtm.rail.TileEntityLargeRailCore;
import jp.ngt.rtm.sound.MovingSoundMaker;
import jp.ngt.rtm.sound.SoundUpdaterTrain;
import jp.ngt.rtm.sound.SoundUpdaterVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    public static final byte ViewMode_Artillery = 0;
    public static final byte ViewMode_SR = 1;
    public static final byte ViewMode_AMR = 2;
    public static final byte ViewMode_NVD = 3;
    public static final byte ViewMode_Camera = 4;

    private final ModelBase missing = new ModelMissing();
    private final ResourceLocation texture = new ResourceLocation("rtm", "textures/missing.png");
    private byte connectionState = 0;

    private final FormationManager fmClient = new FormationManager(true);

    private List<TileEntityLargeRailCore> unloadedRails = new ArrayList<TileEntityLargeRailCore>();

    @Override
    public void preInit() {
        this.versionCheck();

        RTMEntity.initClient();
        RTMBlock.initClient();
        RTMItem.initClient();

        MinecraftForge.EVENT_BUS.register(new RTMEventHandlerClient(Minecraft.getMinecraft()));

        RTMKeyHandlerClient.init();
    }

    private void versionCheck() {
        if (!RTMCore.versionCheck) {
            return;
        }

        List<File> fileList = NGTFileLoader.findFile((file) -> {
            return file.getName().startsWith("pack") && file.getName().endsWith(".json");
        });
        for (File file : fileList) {
            try {
                String json = NGTText.readText(file, false, "UTF-8");
                PackInfo info = NGTJson.getObjectFromJson(json, PackInfo.class);
                if (info != null) {
                    VersionChecker.addToCheckList(info);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        VersionChecker.addToCheckList(new PackInfo(RTMCore.metadata.name, RTMCore.metadata.url, RTMCore.metadata.updateUrl, RTMCore.metadata.version));
    }

    @Override
    public void init() {
        ModelPackLoadThread thread = new ModelPackLoadThread(Side.CLIENT);
        thread.start();

        MinecraftForge.EVENT_BUS.register(RTMKeyHandlerClient.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new RTMTickHandlerClient());
        RTMBlock.initClient2();
        RTMItem.initClient2();

        ItemRenderHandler.INSTANCE.register(RTMItem.decoration_block, new RenderDecoration());

        NGTParticle.INSTANCE.register(RTMParticle.PARTICLE_SPARK, (particleID, worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, p_178902_15_) -> {
            return new ParticleSpark(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        });
    }

    @Override
    public void complete() {
        RTMSound.init();
    }

    @Override
    public IUpdateVehicle getSoundUpdater(EntityVehicleBase vehicle) {
        if (vehicle instanceof EntityTrainBase) {
            return new SoundUpdaterTrain(NGTUtilClient.getMinecraft().getSoundHandler(), (EntityTrainBase) vehicle);
        } else if (vehicle instanceof EntityVehicle) {
            return new SoundUpdaterVehicle(NGTUtilClient.getMinecraft().getSoundHandler(), (EntityVehicle) vehicle);
        }
        return null;
    }

    @Override
    public byte getConnectionState() {
        return this.connectionState;
    }

    @Override
    public void setConnectionState(byte par1) {
        this.connectionState = par1;
        NGTLog.debug("[RTM](Client) Set connection state : " + par1);
    }

    @Override
    public void spawnModParticle(World world, double x, double y, double z, double mX, double mY, double mZ) {
        EntityMeltedMetalFX entityFX = new EntityMeltedMetalFX(world, x, y, z, mX, mY, mZ);
        entityFX.setParticleTexture(NGTUtilClient.getIcon("rtm:blocks/meltedMetal"));
        NGTUtilClient.getMinecraft().effectRenderer.addEffect(entityFX);
    }

    @Override
    public void renderMissingModel() {
        NGTUtilClient.getMinecraft().renderEngine.bindTexture(texture);
        this.missing.render(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
    }

    @Override
    public float getFov(EntityPlayer player, float fov) {
        switch (getViewMode(player)) {
            case ViewMode_Artillery:
                return 0.1F;
            case ViewMode_SR:
                return 0.25F;
            case ViewMode_AMR:
                return 0.1F;
            case ViewMode_Camera:
                return Camera.INSTANCE.getFov();
            default:
                return fov;
        }
    }

    public static byte getViewMode(EntityPlayer player) {
        if (player != null && NGTUtilClient.getMinecraft().gameSettings.thirdPersonView == 0) {
            ItemStack helmet = player.inventory.armorItemInSlot(3);
            if (helmet.getItem() == RTMItem.nvd) {
                return ViewMode_NVD;
            }

            if (player.isRiding() && player.getRidingEntity() instanceof EntityArtillery) {
                ModelSetFirearm set = ((EntityArtillery) player.getRidingEntity()).getResourceState().getResourceSet();
                if (set.getConfig().fpvMode) {
                    return ViewMode_Artillery;
                }
            }

            ItemStack stack = player.getHeldItemMainhand();
            if (stack.getItem() == RTMItem.sniper_rifle) {
                return ViewMode_SR;
            } else if (stack.getItem() == RTMItem.amr) {
                return ViewMode_AMR;
            } else if (stack.getItem() == RTMItem.camera) {
                return ViewMode_Camera;
            }
        }
        return -1;
    }

    @Override
    public void playSound(Entity entity, String sound, float vol, float pitch) {
        if (sound == null) {
            NGTLog.debug("Proxy:Sound is null");
            return;
        }

        if (NGTUtil.isServer()) {
            super.playSound(entity, sound, vol, pitch);
        } else {
            if (entity instanceof EntityBogie && isJointSound(sound)) {
                TrainBodySway.onJoint((EntityBogie) entity);
            }
            MovingSoundCustom ms = MovingSoundMaker.create(entity, sound, false);
            if (ms != null) {
                ms.setSoundRange(AppleConfig.hornSoundRange);
                this.playSound(ms, vol, pitch);
            }
        }
    }

    @Override
    public void playSound(TileEntity entity, String sound, float vol, float pitch) {
        if (sound == null) {
            NGTLog.debug("Proxy:Sound is null");
            return;
        }

        if (NGTUtil.isServer()) {
            super.playSound(entity, sound, vol, pitch);
        } else {
            MovingSoundCustom ms = MovingSoundMaker.create(entity, sound, false);
            if (ms != null) {
                this.playSound(ms, vol, pitch);
            }
        }
    }

    public static void playSound(MovingSoundCustom sound, float vol, float pitch) {
        sound.setVolume(vol);
        sound.setPitch(pitch);
        sound.update();
        NGTSound.playSound(sound);
    }

    @Override
    public FormationManager getFormationManager() {
        return NGTUtil.isServer() ? super.getFormationManager() : this.fmClient;
    }

    private static boolean isJointSound(String sound) {
        return "rtm:sounds/train/joint.ogg".equals(sound)
                || "rtm:sounds/train/joint_reverb.ogg".equals(sound);
    }
}