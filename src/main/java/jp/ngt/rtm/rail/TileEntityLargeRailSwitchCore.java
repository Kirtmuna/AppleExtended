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

import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.math.AABBInt;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.rtm.RTMRail;
import jp.ngt.rtm.network.PacketLargeRailCore;
import jp.ngt.rtm.rail.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileEntityLargeRailSwitchCore extends TileEntityLargeRailCore {
    private SwitchType switchObj;

    private List<RailMapSwitch> openedRails = new ArrayList<RailMapSwitch>();
    private final Map<Integer, Boolean> apiPointOverrides = new HashMap<>();

    public TileEntityLargeRailSwitchCore() {
        super();
    }

    @Override
    protected void readRailData(NBTTagCompound nbt) {
        apiPointOverrides.clear();
        NBTTagList ovr = nbt.getTagList("ApiPointOverrides", 10);
        for (int i = 0; i < ovr.tagCount(); i++) {
            NBTTagCompound t = ovr.getCompoundTagAt(i);
            apiPointOverrides.put(t.getInteger("I"), t.getBoolean("R"));
        }
        
        byte size = nbt.getByte("Size");
        this.railPositions = new RailPosition[size];

        for (int i = 0; i < size; ++i) {
            this.railPositions[i] = RailPosition.readFromNBT(nbt.getCompoundTag("RP" + i));
        }

        this.fixRTMRailMapVersion = nbt.getInteger("fixRTMRailMapVersion");

        // switchObj が既に作られていれば、読み込んだ override を反映
        if (this.switchObj != null) {
            this.applyApiOverridesToPoints();
            this.applyPointStatesToRailMaps();
        }
    }

    private RailPosition getRP(int x, int y, int z, byte dir, boolean b) {
        RailPosition rp = new RailPosition(x, y, z, dir, (byte) (b ? 1 : 0));
        rp.anchorYaw = NGTMath.wrapAngle((float) dir * 45.0F);
        return rp;
    }

    @Override
    protected void writeRailData(NBTTagCompound nbt) {
        NBTTagList ovr = new NBTTagList();
        for (Map.Entry<Integer, Boolean> e : apiPointOverrides.entrySet()) {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("I", e.getKey());
            t.setBoolean("R", e.getValue());
            ovr.appendTag(t);
        }
        nbt.setTag("ApiPointOverrides", ovr);
        
        nbt.setByte("Size", (byte) this.railPositions.length);

        for (int i = 0; i < this.railPositions.length; ++i) {
            nbt.setTag("RP" + i, this.railPositions[i].writeToNBT());
        }

        nbt.setInteger("fixRTMRailMapVersion", this.fixRTMRailMapVersion);
    }

    @Override
    public void setRailPositions(RailPosition[] par1) {
        super.setRailPositions(par1);
        this.onBlockChanged();
    }

    @Override
    public void createRailMap() {
        if (this.isLoaded() && this.switchObj == null) {
            this.switchObj = (new RailMaker(this.getWorld(), this.railPositions, this.fixRTMRailMapVersion)).getSwitch();
            this.applyApiOverridesToPoints();
            this.applyPointStatesToRailMaps();
        }
    }
    /** switchObj の各 Point に override を書き込む */
    private void applyApiOverridesToPoints() {
        if (this.switchObj == null) return;
        Point[] points = this.switchObj.getPoints();
        if (points == null) return;
        
        for (Point p : points) {
            p.setForcedReversed(null);
        }
        for (Map.Entry<Integer, Boolean> e : this.apiPointOverrides.entrySet()) {
            int idx = e.getKey();
            if (idx >= 0 && idx < points.length) {
                points[idx].setForcedReversed(e.getValue());
            }
        }
    }

    public SwitchType getSwitch() {
        if (this.switchObj == null) {
            this.createRailMap();
        }
        return this.switchObj;
    }

    @Override
    public byte getPacketType() {
        return PacketLargeRailCore.TYPE_SWITCH;
    }

    @Override
    public void update() {
        super.update();

        if (this.getSwitch() != null) {
            this.getSwitch().onUpdate(this.getWorld());
        }
    }

    public void onBlockChanged() {
        this.getSwitch().onBlockChanged(this.getWorld());
        this.applyApiOverridesToPoints();
        this.applyPointStatesToRailMaps();
        if (!this.getWorld().isRemote) {
            this.sendPacket();
        }
    }

    @Override
    public RailMap getRailMap(Entity entity) {
        SwitchType st = this.getSwitch();
        if (st == null) {
            return null;
        }

        if (entity == null) {
            return this.getAllRailMaps()[0];
        }

        return st.getRailMap(entity);
    }

    @Override
    public RailMapSwitch[] getAllRailMaps() {
        if (this.getSwitch() != null) {
            return this.getSwitch().getAllRailMap();
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected AxisAlignedBB getRenderAABB() {
        AABBInt box = this.getRailSize();
        AxisAlignedBB aabb = new AxisAlignedBB(box.minX - 1, box.minY, box.minZ - 1, box.maxX + 2, box.maxY + 2, box.maxZ + 2);
        boolean flag = (aabb.maxX - aabb.minX <= 3 && aabb.maxZ - aabb.minZ <= 3);
        return flag ? null : aabb;
    }

    @Override
    public AABBInt getRailSize() {
        int minX = this.startPoint[0];
        int maxX = this.startPoint[0];
        int minY = this.getPos().getY();
        int maxY = this.getPos().getY();
        int minZ = this.startPoint[2];
        int maxZ = this.startPoint[2];
        for (RailPosition rp : this.railPositions) {
            minX = minX <= rp.blockX ? minX : rp.blockX;
            maxX = maxX >= rp.blockX ? maxX : rp.blockX;
            minZ = minZ <= rp.blockZ ? minZ : rp.blockZ;
            maxZ = maxZ >= rp.blockZ ? maxZ : rp.blockZ;
        }
        return new AABBInt(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public String getRailShapeName() {
        SwitchType st = this.getSwitch();
        AABBInt box = this.getRailSize();
        StringBuilder sb = new StringBuilder();
        sb.append("Type:Switch ").append(st.getName()).append(", ");
        sb.append("X:").append(box.sizeX()).append(", ");
        sb.append("Y:").append(box.sizeY()).append(", ");
        sb.append("Z:").append(box.sizeZ());
        return sb.toString();
    }

    @Override
    protected void invalidateRailMapCache() {
        this.switchObj = null;
    }
    
    @Override
    protected void reconcilePositionBlocks(RailPosition[] oldPositions, RailPosition[] newPositions) {
        int[] start = this.getStartPoint();
        
        for (RailPosition oldRp : oldPositions) {
            boolean stillUsed = false;
            for (RailPosition newRp : newPositions) {
                if (newRp.blockX == oldRp.blockX && newRp.blockY == oldRp.blockY && newRp.blockZ == oldRp.blockZ) {
                    stillUsed = true;
                    break;
                }
            }
            if (!stillUsed && BlockUtil.getBlock(this.world, oldRp.blockX, oldRp.blockY, oldRp.blockZ) instanceof BlockLargeRailSwitchBase) {
                BlockUtil.setAir(this.world, oldRp.blockX, oldRp.blockY, oldRp.blockZ);
            }
        }
        
        for (RailPosition newRp : newPositions) {
            if (!(BlockUtil.getBlock(this.world, newRp.blockX, newRp.blockY, newRp.blockZ) instanceof BlockLargeRailSwitchBase)) {
                BlockUtil.setBlock(this.world, newRp.blockX, newRp.blockY, newRp.blockZ, RTMRail.largeRailSwitchBase, 0, 3);
                TileEntityLargeRailSwitchBase base =
                        (TileEntityLargeRailSwitchBase) BlockUtil.getTileEntity(this.world, newRp.blockX, newRp.blockY, newRp.blockZ);
                base.setStartPoint(start[0], start[1], start[2]);
            }
        }
    }
    /**
     * APIからポイントを転換する。clearApiPointPositionが呼ばれるまで
     * RS入力より優先される。
     * @param pointIndex SwitchType.getPoints() の index
     * @param reversed true=REVERSE, false=NORMAL
     */
    public void setApiPointPosition(int pointIndex, boolean reversed) {
        apiPointOverrides.put(pointIndex, reversed);
        if (this.switchObj != null) {
            Point[] points = this.switchObj.getPoints();
            if (points != null && pointIndex >= 0 && pointIndex < points.length) {
                points[pointIndex].setForcedReversed(reversed);
            }
        }
        this.applyPointStatesToRailMaps();
        this.markDirty();
        if (this.world != null && !this.world.isRemote) {
            this.sendPacket();
        }
    }

    public void clearApiPointPosition(int pointIndex) {
        if (apiPointOverrides.remove(pointIndex) != null) {
            if (this.switchObj != null) {
                Point[] points = this.switchObj.getPoints();
                if (points != null && pointIndex >= 0 && pointIndex < points.length) {
                    points[pointIndex].setForcedReversed(null);
                }
                this.switchObj.onBlockChanged(this.getWorld());
                this.applyPointStatesToRailMaps();
            }
            this.markDirty();
            if (this.world != null && !this.world.isRemote) {
                this.sendPacket();
            }
        }
    }

    @Nullable
    public Boolean getApiPointPosition(int pointIndex) {
        return apiPointOverrides.get(pointIndex);
    }
    /** API を RailMapSwitch の state に反映して描画を更新する */
    private void applyPointStatesToRailMaps() {
        if (this.switchObj == null) return;
        Point[] points = this.switchObj.getPoints();
        if (points == null) return;

        for (Point p : points) {
            if (p == null || p.getForcedReversed() == null) continue;
            if (p.branchDir == RailDir.NONE) continue;

            boolean reversed = p.getForcedReversed();
            p.rmMain.setState(!reversed);
            p.rmBranch.setState(reversed);
        }

        if (this.world != null && this.world.isRemote) {
            this.shouldRerenderRail = true;
            this.shouldRerenderBlock = true;
        }
    }
    /** apiPointOverrides を NBT に書き出す */
    public void writeApiOverrides(NBTTagCompound nbt) {
        NBTTagList ovr = new NBTTagList();
        for (Map.Entry<Integer, Boolean> e : apiPointOverrides.entrySet()) {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("I", e.getKey());
            t.setBoolean("R", e.getValue());
            ovr.appendTag(t);
        }
        nbt.setTag("ApiPointOverrides", ovr);
    }
    /** NBT から apiPointOverrides を読み込む */
    public void readApiOverrides(NBTTagCompound nbt) {
        apiPointOverrides.clear();
        NBTTagList ovr = nbt.getTagList("ApiPointOverrides", 10);
        for (int i = 0; i < ovr.tagCount(); i++) {
            NBTTagCompound t = ovr.getCompoundTagAt(i);
            apiPointOverrides.put(t.getInteger("I"), t.getBoolean("R"));
        }
        if (this.switchObj != null) {
            this.applyApiOverridesToPoints();
        }
    }
}