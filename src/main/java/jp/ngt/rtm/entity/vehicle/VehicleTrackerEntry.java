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

import jp.ngt.ngtlib.network.PacketNBT;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMEntity;
import jp.ngt.rtm.entity.train.EntityBogie;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.network.PacketVehicleMovement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketEntityAttach;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraft.network.play.server.SPacketSetPassengers;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VehicleTrackerEntry extends EntityTrackerEntry {
    private Entity trackedEntity;
    /**
     * check for sync when ticks % updateFrequency==0
     */
    private int updateFrequency;
    private boolean updatedPlayerVisibility;
    private List<Entity> passengers = Collections.<Entity>emptyList();


    private double posX, posY, posZ;
    private boolean isDataInitialized;
    private Entity rider;
    private boolean isTrain;
    
    private final Set<Integer> trackedPlayerIds = new HashSet<>();

    public VehicleTrackerEntry(Entity par2) {
        super(par2, 256, 256, RTMEntity.FREQ_VEHICLE, false);
        this.trackedEntity = par2;
        this.updateFrequency = RTMEntity.FREQ_VEHICLE;

        this.isTrain = (this.trackedEntity instanceof EntityTrainBase || this.trackedEntity instanceof EntityBogie);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VehicleTrackerEntry ? ((VehicleTrackerEntry) obj).trackedEntity.getEntityId() == this.trackedEntity.getEntityId() : false;
    }

    @Override
    public int hashCode() {
        return this.trackedEntity.getEntityId();
    }

    public void setPlayers(Set<EntityPlayerMP> par1) {
        this.trackingPlayers.clear();
        this.trackingPlayers.addAll(par1);
    }

    @Override
    public void updatePlayerList(List<EntityPlayer> par1) {
        this.playerEntitiesUpdated = false;

        if (!this.isDataInitialized || this.trackedEntity.getDistanceSq(this.posX, this.posY, this.posZ) > 16.0D) {
            this.posX = this.trackedEntity.posX;
            this.posY = this.trackedEntity.posY;
            this.posZ = this.trackedEntity.posZ;
            this.isDataInitialized = true;
            this.playerEntitiesUpdated = true;
        }
        this.updatePlayerEntities(par1);

        this.trackedPlayerIds.removeIf(id -> {
            boolean stillWatching = false;
            for (EntityPlayer p : par1) {
                if (p instanceof EntityPlayerMP && p.getEntityId() == id) {
                    stillWatching = this.isPlayerWatchingThisChunk((EntityPlayerMP) p) || this.trackedEntity.forceSpawn;
                    break;
                }
            }
            return !stillWatching;
        });

        List<Entity> list = this.trackedEntity.getPassengers();
        if (!list.equals(this.passengers)) {
            this.passengers = list;
            this.sendPacketToTrackedPlayers(new SPacketSetPassengers(this.trackedEntity));
        }

        if (this.updateCounter % this.updateFrequency == 0) {
            if (this.trackedEntity.getRidingEntity() == null) {
                RTMCore.NETWORK_WRAPPER.sendToAll(new PacketVehicleMovement(this.trackedEntity, false));

                if (this.isTrain) {
                    ;
                } else {
                    
                /*this.sendPacketToTrackedPlayers(new SPacketEntityVelocity(this.trackedEntity.getEntityId(),
                		this.trackedEntity.motionX, this.trackedEntity.motionY, this.trackedEntity.motionZ));
                this.ridingEntity = false;*/
                }
            }
        }

        this.sendMetadataToAllAssociatedPlayers();

        ++this.updateCounter;
    }

    /**
     * DataWatcherの同期
     */
    private void sendMetadataToAllAssociatedPlayers() {
        EntityDataManager entitydatamanager = this.trackedEntity.getDataManager();

        if (entitydatamanager.isDirty()) {
            this.sendToTrackingAndSelf(new SPacketEntityMetadata(this.trackedEntity.getEntityId(), entitydatamanager, false));
        }
    }

    @Override
    public void sendPacketToTrackedPlayers(Packet<?> packetIn) {
        for (EntityPlayerMP entityplayermp : this.trackingPlayers) {
            entityplayermp.connection.sendPacket(packetIn);
        }
    }

    @Override
    public void sendToTrackingAndSelf(Packet<?> packetIn) {
        this.sendPacketToTrackedPlayers(packetIn);
    }


    @Override
    public void sendDestroyEntityPacketToTrackedPlayers() {
        for (EntityPlayerMP entityplayermp : this.trackingPlayers) {
            this.trackedEntity.removeTrackingPlayer(entityplayermp);
            entityplayermp.removeEntity(this.trackedEntity);
        }
    }

    @Override
    public void removeFromTrackedPlayers(EntityPlayerMP playerMP) {
        if (this.trackingPlayers.contains(playerMP) || this.trackedPlayerIds.contains(playerMP.getEntityId())) {
            this.trackedEntity.removeTrackingPlayer(playerMP);
            playerMP.removeEntity(this.trackedEntity);
            this.trackingPlayers.remove(playerMP);
            this.trackedPlayerIds.remove(playerMP.getEntityId());
        }
    }

    @Override
    public void updatePlayerEntity(EntityPlayerMP par1) {
        if (par1 != this.trackedEntity) {
            if (this.isVisibleTo(par1)) {
                boolean alreadyTracked = this.trackingPlayers.contains(par1) || this.trackedPlayerIds.contains(par1.getEntityId());
                if (!alreadyTracked && (this.isPlayerWatchingThisChunk(par1) || this.trackedEntity.forceSpawn)) {
                    this.trackingPlayers.add(par1);
                    this.trackedPlayerIds.add(par1.getEntityId());
                    Packet packet = FMLNetworkHandler.getEntitySpawningPacket(this.trackedEntity);
                    par1.connection.sendPacket(packet);

                    if (!this.trackedEntity.getDataManager().isEmpty()) {
                        par1.connection.sendPacket(new SPacketEntityMetadata(this.trackedEntity.getEntityId(), this.trackedEntity.getDataManager(), true));
                    }

                    PacketNBT.sendToClient(this.trackedEntity, par1);

                    if (this.trackedEntity.getRidingEntity() != null) {
                        par1.connection.sendPacket(new SPacketEntityAttach(this.trackedEntity, this.trackedEntity.getRidingEntity()));
                    }

                    ForgeEventFactory.onStartEntityTracking(this.trackedEntity, par1);
                }
            } else if (this.trackingPlayers.contains(par1) || this.trackedPlayerIds.contains(par1.getEntityId())) {
                this.trackingPlayers.remove(par1);
                this.trackedPlayerIds.remove(par1.getEntityId());
                par1.removeEntity(this.trackedEntity);
                ForgeEventFactory.onStopEntityTracking(trackedEntity, par1);
            }
        }
    }

    @Override
    public boolean isVisibleTo(EntityPlayerMP playerMP) {
        return true;
    }

    private boolean isPlayerWatchingThisChunk(EntityPlayerMP playerMP) {
        return playerMP.getServerWorld().getPlayerChunkMap().isPlayerWatchingChunk(playerMP, this.trackedEntity.chunkCoordX, this.trackedEntity.chunkCoordZ);
    }

    @Override
    public void updatePlayerEntities(List<EntityPlayer> players) {
        for (int i = 0; i < players.size(); ++i) {
            this.updatePlayerEntity((EntityPlayerMP) players.get(i));
        }
    }


    /**
     * @param par1 EntityVehicleBase or EntityBogie
     */
    public static boolean trackingVehicle(Entity par1) {
        if (par1.world instanceof WorldServer) {
            WorldServer world = (WorldServer) par1.world;
            EntityTracker tracker = world.getEntityTracker();


            EntityTrackerEntry entry = tracker.trackedEntityHashTable.lookup(par1.getEntityId());

            if (entry != null && !(entry instanceof VehicleTrackerEntry)) {

                VehicleTrackerEntry newEntry = new VehicleTrackerEntry(par1);


                newEntry.trackingPlayers.addAll(entry.trackingPlayers);


                Set<EntityTrackerEntry> entries = getTrackedEntities(tracker);
                if (entries != null) {
                    entries.remove(entry);
                    entries.add(newEntry);
                }

                tracker.trackedEntityHashTable.addKey(par1.getEntityId(), newEntry);

                return true;
            }
        }
        return false;
    }

    protected static Set<EntityTrackerEntry> getTrackedEntities(EntityTracker tracker) {
        return (Set<EntityTrackerEntry>) NGTUtil.getField(EntityTracker.class, tracker, new String[]{"entries", "field_72793_b"});
    }
}
