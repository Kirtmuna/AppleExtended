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

package jp.apple.appleextended.compat;

import com.replaymod.replay.ReplayModReplay;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.entity.train.parts.EntityCargoWithModel;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.modelset.ResourceSet;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraftforge.fml.common.Loader;

import java.util.Map;

public class ReplaySyncManager {

    private static final DataParameter<String> SYNC_MODEL_TRAIN =
            EntityDataManager.createKey(jp.ngt.rtm.entity.train.EntityTrainBase.class, DataSerializers.STRING);
    private static final DataParameter<String> SYNC_MODEL_CARGO =
            EntityDataManager.createKey(jp.ngt.rtm.entity.train.parts.EntityCargoWithModel.class, DataSerializers.STRING);
    private static boolean isReplayModLoaded = false;
    private static boolean checked = false;

    public static void init() {
        if (!checked) {
            isReplayModLoaded = Loader.isModLoaded("replaymod");
            checked = true;
        }
    }

    public static boolean isReplayActive() {
        if (!isReplayModLoaded) return false;
        try {
            return ReplayModReplay.instance.getReplayHandler() != null;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void registerData(Entity entity) {
        if (!isReplayModLoaded) return;
        if (entity instanceof jp.ngt.rtm.entity.train.EntityTrainBase) {
            entity.getDataManager().register(SYNC_MODEL_TRAIN, "");
        } else if (entity instanceof EntityCargoWithModel) {
            entity.getDataManager().register(SYNC_MODEL_CARGO, "");
        }
    }

    public static void patchMetadata(Entity entity, NBTTagCompound nbt) {
        if (!isReplayActive()) return;


        if (entity instanceof EntityTrainBase && nbt.hasKey("FormationEntry")) {
            NBTTagCompound formationTag = nbt.getCompoundTag("FormationEntry");
            formationTag.setLong("FormationId", entity.getEntityId() + 2000000L);
            formationTag.setByte("EntryPos", (byte) 0);
        }


        if (entity instanceof IResourceSelector && nbt.hasKey("State")) {
            NBTTagCompound stateTag = nbt.getCompoundTag("State");
            String nbtModelName = stateTag.getString("ResourceName");
            if (isValidName(nbtModelName)) {
                forceRegisterModelToSmpMap(((IResourceSelector) entity).getResourceState().type, nbtModelName);
            }
        }
    }

    public static void syncModel(Entity entity, boolean isUpdateResourceState) {
        if (!isReplayModLoaded) return;

        DataParameter<String> key = (entity instanceof EntityTrainBase) ? SYNC_MODEL_TRAIN :
                (entity instanceof EntityCargoWithModel) ? SYNC_MODEL_CARGO : null;
        if (key == null || !(entity instanceof IResourceSelector)) return;

        IResourceSelector selector = (IResourceSelector) entity;
        ResourceState state = selector.getResourceState();

        if (entity.world.isRemote) {

            String serverModelName = entity.getDataManager().get(key);

            if (isValidName(serverModelName)) {

                if (isReplayActive()) {
                    forceRegisterModelToSmpMap(state.type, serverModelName);
                }

                String currentModelName = state.getResourceName();

                if (!serverModelName.equals(currentModelName) || state.getResourceSet().isDummy()) {
                    state.setResourceName(serverModelName);

                    selector.updateResourceState();
                }
            }
        } else {

            if (isUpdateResourceState) {
                String name = state.getResourceName();
                if (isValidName(name)) {
                    entity.getDataManager().set(key, name);
                }
            }
        }
    }

    private static boolean isValidName(String name) {
        return name != null && !name.isEmpty() && !"no_name".equals(name) && !"default".equals(name);
    }

    private static void forceRegisterModelToSmpMap(ResourceType type, String modelName) {
        if (!isValidName(modelName)) return;

        ModelPackManager mng = ModelPackManager.INSTANCE;
        ResourceType targetType = (type.parent != null) ? type.parent : type;

        Map<String, ResourceSet> smpMap = mng.smpModelSetMap.get(targetType);
        if (smpMap != null && !smpMap.containsKey(modelName)) {
            Map<String, ResourceSet> allMap = mng.allModelSetMap.get(targetType);
            if (allMap != null) {
                ResourceSet localSet = allMap.get(modelName);
                if (localSet != null) {
                    smpMap.put(modelName, localSet);
                }
            }
        }
    }

    public static void patchRailMetadata(jp.ngt.rtm.rail.TileEntityLargeRailCore tile, NBTTagCompound nbt) {
        if (!isReplayActive()) return;

        if (nbt.hasKey("State")) {
            String modelName = nbt.getCompoundTag("State").getString("ResourceName");
            if (isValidName(modelName)) {
                forceRegisterModelToSmpMap(jp.ngt.rtm.RTMResource.RAIL, modelName);
            }
        }
    }

    public static void checkRailModel(jp.ngt.rtm.rail.TileEntityLargeRailCore tile) {
        if (!isReplayActive()) return;

        jp.ngt.rtm.modelpack.state.ResourceStateRail state = tile.getResourceState();

        if (state.getResourceSet().isDummy()) {
            String name = state.getResourceName();
            if (isValidName(name)) {
                forceRegisterModelToSmpMap(state.type, name);

                state.setResourceName(name);
                tile.shouldRerenderRail = true;
                tile.shouldRerenderBlock = true;
            }
        }
    }

    public static void patchResourceSelectorMetadata(IResourceSelector<?> selector, NBTTagCompound nbt) {
        if (!isReplayActive()) return;
        NBTTagCompound stateTag = nbt.hasKey("State") ? nbt.getCompoundTag("State") :
                (nbt.hasKey("Property") ? nbt.getCompoundTag("Property") : null);

        if (stateTag != null && stateTag.hasKey("ResourceName")) {
            String modelName = stateTag.getString("ResourceName");
            if (isValidName(modelName)) {
                forceRegisterModelToSmpMap(selector.getResourceState().type, modelName);
            }
        }
    }

    public static void checkModelSync(IResourceSelector<?> selector) {
        if (!isReplayActive()) return;

        jp.ngt.rtm.modelpack.state.ResourceState<?> state = selector.getResourceState();
        if (state.getResourceSet().isDummy()) {
            String name = state.getResourceName();
            if (isValidName(name)) {
                forceRegisterModelToSmpMap(state.type, name);
                state.setResourceName(name);
                if (selector instanceof jp.ngt.rtm.block.tileentity.TileEntityMachineBase) {
                }
            }
        }
    }
}
