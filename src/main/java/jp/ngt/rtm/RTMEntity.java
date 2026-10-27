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

import com.anatawa12.fixRtm.rtm.entity.RenderEntityElectricalWiring;
import jp.ngt.ngtlib.util.IMod;
import jp.ngt.ngtlib.util.NGTRegHandler;
import jp.ngt.ngtlib.util.NGTUtilClient;
import jp.ngt.rtm.entity.*;
import jp.ngt.rtm.entity.fluid.EntityFluid;
import jp.ngt.rtm.entity.fluid.RenderFluid;
import jp.ngt.rtm.entity.npc.EntityMotorman;
import jp.ngt.rtm.entity.npc.EntityNPC;
import jp.ngt.rtm.entity.npc.RenderNPC;
import jp.ngt.rtm.entity.train.*;
import jp.ngt.rtm.entity.train.parts.*;
import jp.ngt.rtm.entity.vehicle.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class RTMEntity {
    public static final byte FREQ_VEHICLE = 2;
    public static final byte FREQ_INSTALLED = 10;

    private static short nextId;
    private static final short RANGE = 1024;

    public static void init(IMod mod) {
        NGTRegHandler.register(EntityBullet.class, "bullet", "RTM.E.Bullet", getNextId(), 256, 3, true, mod);
        NGTRegHandler.register(EntityMMBoundingBox.class, "mmbb", "RTM.E.MMBB", getNextId(), 160, Integer.MAX_VALUE, false, mod);
        NGTRegHandler.register(EntityFluid.class, "fluid", "RTM.E.Fluid", getNextId(), 256, 3, true, mod);

        NGTRegHandler.register(EntityMotorman.class, "motorman", "RTM.E.Motorman", getNextId(), RANGE, 3, true, mod);
        NGTRegHandler.register(EntityNPC.class, "npc", "RTM.E.NPC", getNextId(), RANGE, 3, true, mod);

        NGTRegHandler.register(EntityATC.class, "atc", "RTM.E.ATC", getNextId(), 160, FREQ_INSTALLED, false, mod);
        NGTRegHandler.register(EntityTrainDetector.class, "train_detector", "RTM.E.TrainDetector", getNextId(), 160, FREQ_INSTALLED, false, mod);
        NGTRegHandler.register(EntityBumpingPost.class, "bumping_post", "RTM.E.BumpingPost", getNextId(), 160, FREQ_INSTALLED, false, mod);

        NGTRegHandler.register(EntityFloor.class, "floor", "RTM.E.Floor", getNextId(), RANGE, FREQ_VEHICLE, false, mod);
        NGTRegHandler.register(EntityBogie.class, "bogie", "RTM.E.Bogie", getNextId(), RANGE, FREQ_VEHICLE, false, mod);
        NGTRegHandler.register(EntityContainer.class, "container", "RTM.E.Container", getNextId(), 160, FREQ_VEHICLE, false, mod);
        NGTRegHandler.register(EntityArtillery.class, "artillery", "RTM.E.Artillery", getNextId(), 160, FREQ_VEHICLE, false, mod);
        NGTRegHandler.register(EntityTie.class, "tie", "RTM.E.Tie", getNextId(), 160, FREQ_VEHICLE, false, mod);

        NGTRegHandler.register(EntityCar.class, "car", "RTM.E.Car", getNextId(), 160, FREQ_VEHICLE, true, mod);
        NGTRegHandler.register(EntityShip.class, "ship", "RTM.E.Ship", getNextId(), 160, FREQ_VEHICLE, true, mod);
        NGTRegHandler.register(EntityPlane.class, "plane", "RTM.E.Plane", getNextId(), 160, FREQ_VEHICLE, true, mod);
        NGTRegHandler.register(EntityTrolley.class, "trolley", "RTM.E.Trolley", getNextId(), 160, FREQ_VEHICLE, true, mod);
        NGTRegHandler.register(EntityLift.class, "lift", "RTM.E.Lift", getNextId(), 160, FREQ_VEHICLE, true, mod);

        registerTrain(EntityTrainElectricCar.class, "electric_car", "RTM.E.ElectricCar", mod);
        registerTrain(EntityTrainDieselCar.class, "diesel_car", "RTM.E.DieselCar", mod);
        registerTrain(EntityTrainTest.class, "test_car", "RTM.E.TrainTest", mod);
        registerTrain(EntityFreightCar.class, "freight_car", "RTM.E.FreightCar", mod);
        registerTrain(EntityTanker.class, "tanker", "RTM.E.Tanker", mod);
    }

    private static void registerTrain(Class<? extends EntityTrainBase> clazz, String regName, String name, IMod mod) {
        NGTRegHandler.register(clazz, regName, name, getNextId(), RANGE, FREQ_VEHICLE, false, mod);
    }

    @SideOnly(Side.CLIENT)
    public static void initClient() {
        NGTUtilClient.registerEntityRender(EntityVehicle.class, RenderVehicleBase.class);
        NGTUtilClient.registerEntityRender(EntityTrainBase.class, RenderVehicleBase.class);
        NGTUtilClient.registerEntityRender(WeatherEffectDummy.class, RenderWeatherEffectDummy.class);
        NGTUtilClient.registerEntityRender(EntityBogie.class, RenderBogie.class);
        NGTUtilClient.registerEntityRender(EntityFloor.class, RenderSeat.class);
        NGTUtilClient.registerEntityRender(EntityATC.class, RenderEntityElectricalWiring.class);
        NGTUtilClient.registerEntityRender(EntityTrainDetector.class, RenderEntityElectricalWiring.class);
        NGTUtilClient.registerEntityRender(EntityBumpingPost.class, RenderEntityInstalledObject.class);
        NGTUtilClient.registerEntityRender(EntityContainer.class, RenderContainer.class);
        NGTUtilClient.registerEntityRender(EntityArtillery.class, RenderArtillery.class);
        NGTUtilClient.registerEntityRender(EntityBullet.class, RenderBullet.class);
        NGTUtilClient.registerEntityRender(EntityTie.class, RenderTie.class);
        NGTUtilClient.registerEntityRender(EntityMMBoundingBox.class, RenderMMBB.class);
        NGTUtilClient.registerEntityRender(EntityNPC.class, RenderNPC.class);
        NGTUtilClient.registerEntityRender(EntityFluid.class, RenderFluid.class);
        NGTUtilClient.registerEntityRender(EntityTrolley.class, RenderTrolley.class);
    }

    public static int getNextId() {
        return nextId++;
    }
}