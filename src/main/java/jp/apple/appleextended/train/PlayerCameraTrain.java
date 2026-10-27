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

package jp.apple.appleextended.train;

import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
public class PlayerCameraTrain {
    private static final double SEARCH_RANGE = 25.0D;

    private static EntityTrainBase lastTrain = null;
    private static float lastAppliedTrainYaw = Float.NaN;

    private static EntityTrainBase findNearestTrainPlayerIsOn(EntityPlayer player) {
        AxisAlignedBB box = player.getEntityBoundingBox().grow(SEARCH_RANGE, 3.0D, SEARCH_RANGE);
        List<Entity> list = player.world.getEntitiesWithinAABB(EntityTrainBase.class, box);
        EntityTrainBase nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Entity e : list) {
            EntityTrainBase train = (EntityTrainBase) e;

            if (!isPlayerStandingOnTrain(player, train)) {
                continue;
            }

            double distSq = train.getDistanceSq(player);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = train;
            }
        }
        return nearest;
    }

    private static boolean isPlayerStandingOnTrain(EntityPlayer player, EntityTrainBase train) {
        double halfLength = getHalfLength(train);
        double halfWidth = EntityTrainBase.TRAIN_WIDTH / 2.0D + 0.15D;

        double relX = player.posX - train.posX;
        double relZ = player.posZ - train.posZ;

        double rad = Math.toRadians(train.rotationYaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        double localLength = relX * sin + relZ * cos;
        double localWidth = relX * cos - relZ * sin;

        double widthMargin = halfWidth + 0.5D;
        double lengthMargin = halfLength + 0.3D;

        boolean horizontalOverlap = Math.abs(localWidth) <= widthMargin && Math.abs(localLength) <= lengthMargin;
        if (!horizontalOverlap) {
            return false;
        }
        double feetY = player.getEntityBoundingBox().minY;
        double trainTopY = train.posY + EntityTrainBase.TRAIN_HEIGHT;
        return feetY >= trainTopY - 1.0D && feetY <= trainTopY + 0.5D;
    }

    private static double getHalfLength(EntityTrainBase train) {
        if (train.getResourceState() != null && train.getResourceState().getResourceSet() != null) {
            TrainConfig cfg = train.getResourceState().getResourceSet().getConfig();
            if (cfg != null) {
                return cfg.trainDistance;
            }
        }
        return EntityTrainBase.TRAIN_WIDTH / 2.0D;
    }

    static EntityTrainBase getStandingTrain() {
        return lastTrain;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || player.isRiding()) {
            lastAppliedTrainYaw = Float.NaN;
            lastTrain = null;
            return;
        }

        EntityTrainBase train = findNearestTrainPlayerIsOn(player);
        if (train == null) {
            lastAppliedTrainYaw = Float.NaN;
            lastTrain = null;
            return;
        }

        if (train != lastTrain) {
            lastTrain = train;
            lastAppliedTrainYaw = Float.NaN;
        }

        float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
        float interpolatedYaw = train.prevRotationYaw
                + MathHelper.wrapDegrees(train.rotationYaw - train.prevRotationYaw) * partialTicks;

        if (Float.isNaN(lastAppliedTrainYaw)) {
            lastAppliedTrainYaw = interpolatedYaw;
            return;
        }

        float frameDYaw = MathHelper.wrapDegrees(interpolatedYaw - lastAppliedTrainYaw);
        if (frameDYaw != 0.0F) {
            player.rotationYaw -= frameDYaw;
        }
        lastAppliedTrainYaw = interpolatedYaw;
    }
}
