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

import jp.apple.appleextended.config.AppleConfig;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.entity.train.parts.EntityVehiclePart;
import jp.ngt.rtm.entity.vehicle.EntityVehicleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 車両のカントにプレイヤー視点を追従させる。
 * 運転席・座席・立ち乗りに対応。
 */
@SideOnly(Side.CLIENT)
public class PlayerCameraCant {
    private static final float ROLL_SIGN = 1.0F;

    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        
        if (player == null || event.getEntity() != player) return;

        EntityTrainBase train = findTrain(player);
        if (train == null) return;

        float pt = (float) event.getRenderPartialTicks();
        float roll = train.prevRotationRoll + (train.rotationRoll - train.prevRotationRoll) * pt;
        if (roll == 0.0F) return;
        
        float trainYaw = train.prevRotationYaw
                + MathHelper.wrapDegrees(train.rotationYaw - train.prevRotationYaw) * pt;
        float viewYaw = event.getYaw() - 180.0F;   // CameraSetupのyawはプレイヤーyaw+180
        double delta = Math.toRadians(MathHelper.wrapDegrees(trainYaw + viewYaw));

        float amount = ROLL_SIGN * roll;
        event.setRoll(event.getRoll() + amount * (float) Math.cos(delta));
        event.setPitch(event.getPitch() + amount * (float) Math.sin(delta));
    }

    private static EntityTrainBase findTrain(EntityPlayer player) {
        if (player.isRiding()) {
            Entity riding = player.getRidingEntity();
            if (riding instanceof EntityTrainBase) {
                return (EntityTrainBase) riding;           // 運転席
            }
            if (riding instanceof EntityVehiclePart) {      // 座席
                EntityVehicleBase vehicle = ((EntityVehiclePart) riding).getVehicle();
                return vehicle instanceof EntityTrainBase ? (EntityTrainBase) vehicle : null;
            }
            return null;
        }
        return PlayerCameraTrain.getStandingTrain();        // 立ち乗り
    }
}
