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

import jp.apple.appleextended.compat.ReplaySyncManager;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.math.AABBInt;
import jp.ngt.ngtlib.renderer.GLHelper;
import jp.ngt.ngtlib.renderer.GLObject;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMRail;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.state.ResourceState;
import jp.ngt.rtm.modelpack.state.ResourceStateRail;
import jp.ngt.rtm.network.PacketLargeRailCore;
import jp.ngt.rtm.rail.util.RailMap;
import jp.ngt.rtm.rail.util.RailMapBasic;
import jp.ngt.rtm.rail.util.RailMapCustom;
import jp.ngt.rtm.rail.util.RailPosition;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class TileEntityLargeRailCore extends TileEntityLargeRailBase implements IResourceSelector {
    public boolean breaking;
    protected boolean isCollidedTrain = false;
    public boolean colliding = false;
    private int signal = 0;
    private ResourceStateRail state = new ResourceStateRail(RTMResource.RAIL, this);
    public final List<ResourceStateRail> subRails = new ArrayList<>();

    protected RailPosition[] railPositions;
    protected RailMap railmap;

    @SideOnly(Side.CLIENT)
    private AxisAlignedBB renderAABB;

    @SideOnly(Side.CLIENT)
    public GLObject[] glLists;
    @SideOnly(Side.CLIENT)
    public GLObject railBlocks;
    @SideOnly(Side.CLIENT)
    public boolean shouldRerenderRail;
    @SideOnly(Side.CLIENT)
    public boolean shouldRerenderBlock;
    @SideOnly(Side.CLIENT)
    private int brightness;
    @SideOnly(Side.CLIENT)
    public int rerenderCount;
    private int count;
    protected int fixRTMRailMapVersion;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        ReplaySyncManager.patchRailMetadata(this, nbt);

        super.readFromNBT(nbt);
        this.readRailStates(nbt);
        this.readRailData(nbt);
    }

    public void readRailStates(NBTTagCompound nbt) {
        this.state.readFromNBT(nbt.getCompoundTag("State"));
        if (this.state.version < 1) {
            this.state.readFromNBT(nbt.getCompoundTag("Property"));
        }

        this.subRails.clear();
        NBTTagList list = nbt.getTagList("SubRails", 10);
        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound nbt1 = list.getCompoundTagAt(i);
            ResourceStateRail state1 = new ResourceStateRail(RTMResource.RAIL, this);
            state1.readFromNBT(nbt1);
            this.subRails.add(state1);
        }
    }

    protected void readRailData(NBTTagCompound nbt) {
        if (nbt.hasKey("StartRP")) {
            this.railPositions = new RailPosition[2];
            this.railPositions[0] = RailPosition.readFromNBT(nbt.getCompoundTag("StartRP"));
            this.railPositions[1] = RailPosition.readFromNBT(nbt.getCompoundTag("EndRP"));
            this.fixRTMRailMapVersion = nbt.getInteger("fixRTMRailMapVersion");
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        this.writeRailStates(nbt);
        this.writeRailData(nbt);
        return nbt;
    }

    public void writeRailStates(NBTTagCompound nbt) {
        nbt.setTag("State", this.state.writeToNBT());
        NBTTagList tagList = new NBTTagList();
        for (ResourceStateRail state1 : this.subRails) {
            tagList.appendTag(state1.writeToNBT());
        }
        nbt.setTag("SubRails", tagList);
    }

    protected void writeRailData(NBTTagCompound nbt) {
        if (this.railPositions == null || this.railPositions.length < 2 || this.railPositions[0] == null || this.railPositions[1] == null) {
            return;
        }
        
        nbt.setTag("StartRP", this.railPositions[0].writeToNBT());
        nbt.setTag("EndRP", this.railPositions[1].writeToNBT());
        nbt.setInteger("fixRTMRailMapVersion", this.fixRTMRailMapVersion);
    }

    @Override
    public void setStartPoint(int x, int y, int z) {
        this.startPoint[0] = x;
        this.startPoint[1] = y;
        this.startPoint[2] = z;
    }

    public void createRailMap() {
        if (this.isLoaded()) {
            if (this.getRailPositions()[0].hasScript()) {
                RailPosition railposition = this.getRailPositions()[0];
                this.railmap = new RailMapCustom(railposition, railposition.scriptName, railposition.scriptArgs);
            } else {
                this.railmap = new RailMapBasic(this.railPositions[0], this.railPositions[1], fixRTMRailMapVersion);
            }
        }
    }

    public boolean isLoaded() {
        return this.railPositions != null && this.railPositions.length > 0;
    }

    public RailPosition[] getRailPositions() {
        return this.railPositions;
    }

    public void setRailPositions(RailPosition[] par1) {
        this.railPositions = par1;
        this.railmap = null;
    }

    public int getSignal() {
        return this.signal;
    }

    public void setSignal(int par1) {
        this.signal = par1;
    }

    @Override
    public TileEntityLargeRailCore getRailCore() {
        return this;
    }

    @Override
    public void sendPacket() {
        if ((this.world == null || !this.world.isRemote) && this.isLoaded()) {
            RTMCore.NETWORK_WRAPPER.sendToAll(new PacketLargeRailCore(this, this.getPacketType()));
        }
    }

    public byte getPacketType() {
        return PacketLargeRailCore.TYPE_NORMAL;
    }

    @Override
    public void onChunkUnload() {
        if (this.world.isRemote) {
            this.deleteGLList();
        }
    }

    @Override
    public void invalidate() {
        if (this.world.isRemote) {
            this.deleteGLList();
        }
    }

    @SideOnly(Side.CLIENT)
    private void deleteGLList() {
        if (this.glLists != null) {
            for (GLObject glList : this.glLists) {
                GLHelper.deleteGLList(glList);
            }
        }
        GLHelper.deleteGLList(this.railBlocks);
        this.glLists = new GLObject[this.subRails.size() + 1];
        this.railBlocks = null;
    }

    public void replaceRail(ResourceStateRail state) {
        this.getResourceState().readFromNBT(state.writeToNBT());
        this.subRails.clear();
        this.sendPacket();
    }

    public void addSubRail(ResourceStateRail state) {
        ResourceStateRail newState = new ResourceStateRail(RTMResource.RAIL, this);
        newState.readFromNBT(state.writeToNBT());
        ResourceStateRail oldState = null;
        for (ResourceStateRail state1 : this.subRails) {
            if (state1.getResourceName().equals(newState.getResourceName())) {
                oldState = state1;
                break;
            }
        }

        if (oldState == null) {
            if (!this.getResourceState().getResourceName().equals(newState.getResourceName())) {
                this.subRails.add(newState);
            }
        } else {
            this.subRails.remove(oldState);
        }

        this.sendPacket();
    }

    @Override
    public void update() {
        super.update();

        if (!this.world.isRemote) {
            this.isCollidedTrain = this.colliding;
            this.colliding = false;
        } else {
            if (this.count >= 200) {
                this.updateBrightness();
                this.count = 0;
            }
            ++this.count;

            if (this.rerenderCount > 0) {
                ++this.rerenderCount;
                if (this.rerenderCount >= 100) {
                    this.shouldRerenderBlock = true;
                    this.rerenderCount = 0;
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void updateBrightness() {
        int light = NGTUtil.getSkyLight(this.getWorld(), this.getPos());
        if (this.brightness != light) {
            this.brightness = light;
            this.shouldRerenderRail = this.shouldRerenderBlock = true;
        }
    }

    @Override
    public RailMap getRailMap(Entity entity) {
        if (this.railmap == null) {
            this.createRailMap();
        }
        return this.railmap;
    }

    @Nullable
    public RailMap[] getAllRailMaps() {
        RailMap rm = this.getRailMap(null);
        return rm != null ? new RailMap[]{rm} : null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public double getMaxRenderDistanceSquared() {
        return NGTUtil.getChunkLoadDistanceSq() * 4;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (!this.isLoaded()) {
            return INFINITE_EXTENT_AABB;
        }

        if (this.renderAABB == null) {
            this.renderAABB = this.getRenderAABB();
            if (this.renderAABB == null) {
                return INFINITE_EXTENT_AABB;
            }
        }
        return this.renderAABB;
    }

    @SideOnly(Side.CLIENT)
    protected AxisAlignedBB getRenderAABB() {
        AABBInt box = this.getRailSize();
        AxisAlignedBB aabb = new AxisAlignedBB(box.minX - 1, box.minY, box.minZ - 1, box.maxX + 2, box.maxY + 2, box.maxZ + 2);
        if (aabb.maxX - aabb.minX <= 3 && aabb.maxZ - aabb.minZ <= 3) {
            return null;
        }
        return aabb;
    }

    public AABBInt getRailSize() {
        int startX = this.railPositions[0].blockX;
        int startY = this.railPositions[0].blockY;
        int startZ = this.railPositions[0].blockZ;
        int endX = this.railPositions[1].blockX;
        int endY = this.railPositions[1].blockY;
        int endZ = this.railPositions[1].blockZ;

        int minX = startX <= endX ? startX : endX;
        int maxX = startX >= endX ? startX : endX;
        int minY = startY <= endY ? startY : endY;
        int maxY = startY >= endY ? startY : endY;
        int minZ = startZ <= endZ ? startZ : endZ;
        int maxZ = startZ >= endZ ? startZ : endZ;
        return new AABBInt(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void setPos(int x, int y, int z, int prevX, int prevY, int prevZ) {
        int difX = x - prevX;
        int difY = y - prevY;
        int difZ = z - prevZ;
        for (RailPosition rp : this.railPositions) {
            rp.movePos(difX, difY, difZ);
        }
        super.setPos(x, y, z, prevX, prevY, prevZ);
    }

    @Override
    public void updateResourceState() {
        if (this.world == null || !this.world.isRemote) {
            this.markDirty();
            this.sendPacket();
        }
        this.shouldRerenderBlock = this.shouldRerenderRail = true;
    }

    @Override
    public int[] getSelectorPos() {
        return new int[]{this.getPos().getX(), this.getPos().getY(), this.getPos().getZ()};
    }

    @Override
    public boolean closeGui(ResourceState par1) {
        return true;
    }

    @Override
    public ResourceStateRail getResourceState() {
        return this.state;
    }

    public abstract String getRailShapeName();

    protected void invalidateRailMapCache() {
        this.railmap = null;
    }
    
    protected Block getBallastBlock() {
        return RTMRail.largeRailBase;
    }

    public boolean isSameLogicalRail(TileEntityLargeRailCore other) {
        return this == other;
    }

    public RailPosition[] getLogicalRailPositions() {
        return this.railPositions;
    }

    public List<int[]> getRailGroupCorePositions() {
        int[] pos = this.getStartPoint();
        return Collections.singletonList(new int[]{pos[0], pos[1], pos[2]});
    }

    public boolean isLogicalRailOccupied() {
        return this.isCollidedTrain;
    }

    public void breakLogicalRail() {
        RailMap[] maps = this.getAllRailMaps();
        if (maps != null) {
            for (RailMap rm : maps) {
                rm.breakRail(this.world, this.getResourceState(), this);
            }
        }
    }
    
    public int getRailRenderMinimumSplit() {
        return 0;
    }

    public int getRailRenderEndOffset() {
        return 0;
    }

    public boolean shouldRenderRailStartCap() {
        return true;
    }

    public boolean shouldRenderRailEndCap() {
        return true;
    }

    public boolean relocateRail(RailPosition[] newPositions) {
        if (this.world == null || this.world.isRemote || !this.isLoaded()) {
            return false;
        }

        ResourceStateRail prop = this.getResourceState();
        RailMap[] oldMaps = this.getAllRailMaps();
        RailPosition[] oldPositions = this.railPositions;
        
        this.railPositions = newPositions;
        this.invalidateRailMapCache();
        RailMap[] newMaps = this.getAllRailMaps();

        if (newMaps == null || newMaps.length == 0) {
            this.railPositions = oldPositions;
            this.invalidateRailMapCache();
            return false;
        }
        for (RailMap rm : newMaps) {
            if (!rm.canPlaceRail(this.world, false, prop)) {
                this.railPositions = oldPositions;
                this.invalidateRailMapCache();
                return false;
            }
        }
        
        if (oldMaps != null) {
            for (RailMap rm : oldMaps) {
                this.clearBallast(rm, prop);
            }
        }
        
        int[] start = this.getStartPoint();
        Block ballast = this.getBallastBlock();
        for (RailMap rm : newMaps) {
            rm.setRail(this.world, ballast, start[0], start[1], start[2], prop);
        }

        this.reconcilePositionBlocks(oldPositions, newPositions);
        
        this.markDirty();
        this.sendPacket();

        return true;
    }

    protected void reconcilePositionBlocks(RailPosition[] oldPositions, RailPosition[] newPositions) {
    }

    private void clearBallast(RailMap rm, ResourceStateRail prop) {
        for (int[] pos : rm.getRailBlockList(prop, true)) {
            int x = pos[0], y = pos[1], z = pos[2];
            if (BlockUtil.getBlock(this.world, x, y, z) instanceof BlockLargeRailBase) {
                TileEntityLargeRailBase rail = (TileEntityLargeRailBase) BlockUtil.getTileEntity(this.world, x, y, z);
                TileEntityLargeRailCore core2 = rail.getRailCore();
                if (core2 == null || core2 == this) {
                    this.world.setBlockToAir(new BlockPos(x, y, z));
                }
            }
        }
    }
}