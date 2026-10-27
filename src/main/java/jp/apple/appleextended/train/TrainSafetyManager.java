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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TrainSafetyManager {


    private static final Set<UUID> notifiedTrains = new HashSet<>();

    public static void handleDerailment(EntityTrainBase train) {
        if (!train.onRail) {

            freezeTrain(train);


            if (!notifiedTrains.contains(train.getUniqueID())) {
                notifyDriver(train, "§cRail not found");
                notifiedTrains.add(train.getUniqueID());
            }
        } else {

            notifiedTrains.remove(train.getUniqueID());
        }
    }

    private static void freezeTrain(EntityTrainBase train) {
        train.motionX = 0.0D;
        train.motionY = 0.0D;
        train.motionZ = 0.0D;


        train.setSpeed_NoSync(0.0F);


        train.velocityChanged = true;
    }

    private static void notifyDriver(EntityTrainBase train, String message) {
        Entity driver = train.getFirstPassenger();
        if (driver instanceof EntityPlayer) {
            driver.sendMessage(new TextComponentString(message));
        }
    }

    public static void cleanup(EntityTrainBase train) {
        notifiedTrains.remove(train.getUniqueID());
    }
}