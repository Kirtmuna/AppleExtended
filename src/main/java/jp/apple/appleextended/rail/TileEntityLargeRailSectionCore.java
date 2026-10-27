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

package jp.apple.appleextended.rail;

import jp.apple.appleextended.rail.util.RailMapSection;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.rtm.modelpack.state.ResourceStateRail;
import jp.ngt.rtm.network.PacketLargeRailCore;
import jp.ngt.rtm.rail.TileEntityLargeRailBase;
import jp.ngt.rtm.rail.TileEntityLargeRailCore;
import jp.ngt.rtm.rail.TileEntityLargeRailNormalCore;
import jp.ngt.rtm.rail.util.RailMap;
import jp.ngt.rtm.rail.util.RailMapBasic;
import jp.ngt.rtm.rail.util.RailPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class TileEntityLargeRailSectionCore extends TileEntityLargeRailNormalCore {
    public static final String SECTION_TAG = "RailSection";
    private static final double RATIO_EPSILON = 1.0E-7;

    private UUID railGroupId;
    private RailPosition[] logicalRailPositions;
    private double sectionStartRatio = 0.0D;
    private double sectionEndRatio = 1.0D;
    private int[][] railGroupCorePositions = new int[0][];

    private static RailPosition[] copyRailPositions(RailPosition[] positions) {
        RailPosition[] result = new RailPosition[positions.length];
        for (int i = 0; i < positions.length; ++i) {
            result[i] = RailPosition.readFromNBT(positions[i].writeToNBT());
        }
        return result;
    }

    private static int[][] copyPositions(List<int[]> positions) {
        int[][] result = new int[positions.size()][];
        for (int i = 0; i < positions.size(); ++i) {
            result[i] = positions.get(i).clone();
        }
        return result;
    }

    public void configureRailSection(UUID groupId, RailPosition[] logicalPositions, RailPosition[] sectionPositions,
                                     double startRatio, double endRatio, List<int[]> corePositions) {
        this.railGroupId = groupId;
        this.logicalRailPositions = copyRailPositions(logicalPositions);
        this.railPositions = copyRailPositions(sectionPositions);
        this.sectionStartRatio = startRatio;
        this.sectionEndRatio = endRatio;
        this.railGroupCorePositions = copyPositions(corePositions);
        this.invalidateRailMapCache();
    }

    public boolean isRailSection() {
        return this.railGroupId != null && this.logicalRailPositions != null && this.logicalRailPositions.length == 2;
    }

    public UUID getRailGroupId() {
        return this.railGroupId;
    }

    @Override
    public boolean isSameLogicalRail(TileEntityLargeRailCore other) {
        if (this == other) {
            return true;
        }
        if (!this.isRailSection() || !(other instanceof TileEntityLargeRailSectionCore)) {
            return false;
        }
        TileEntityLargeRailSectionCore that = (TileEntityLargeRailSectionCore) other;
        if (!that.isRailSection() || !this.railGroupId.equals(that.railGroupId)) {
            return false;
        }
        return this.containsCorePosition(that.getPos().getX(), that.getPos().getY(), that.getPos().getZ())
                && that.containsCorePosition(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());
    }

    @Override
    public RailPosition[] getLogicalRailPositions() {
        if (this.isRailSection()) {
            return copyRailPositions(this.logicalRailPositions);
        }
        return super.getLogicalRailPositions();
    }

    @Override
    public List<int[]> getRailGroupCorePositions() {
        List<int[]> result = new ArrayList<>(this.railGroupCorePositions.length);
        for (int[] pos : this.railGroupCorePositions) {
            result.add(pos.clone());
        }
        return result;
    }

    @Override
    protected void readRailData(NBTTagCompound nbt) {
        super.readRailData(nbt);
        this.readSectionData(nbt);
    }

    @Override
    protected void writeRailData(NBTTagCompound nbt) {
        super.writeRailData(nbt);
        this.writeSectionData(nbt);
    }

    public void readSectionData(NBTTagCompound parent) {
        this.railGroupId = null;
        this.logicalRailPositions = null;
        this.sectionStartRatio = 0.0D;
        this.sectionEndRatio = 1.0D;
        this.railGroupCorePositions = new int[0][];
        this.invalidateRailMapCache();

        if (!parent.hasKey(SECTION_TAG)) {
            return;
        }
        NBTTagCompound section = parent.getCompoundTag(SECTION_TAG);
        if (!section.hasKey("GroupMost") || !section.hasKey("GroupLeast")
                || !section.hasKey("StartRatio") || !section.hasKey("EndRatio")
                || !section.hasKey("LogicalStartRP") || !section.hasKey("LogicalEndRP")
                || !section.hasKey("CorePositions")) {
            return;
        }

        this.railGroupId = new UUID(section.getLong("GroupMost"), section.getLong("GroupLeast"));
        this.logicalRailPositions = new RailPosition[]{
                RailPosition.readFromNBT(section.getCompoundTag("LogicalStartRP")),
                RailPosition.readFromNBT(section.getCompoundTag("LogicalEndRP"))
        };
        this.sectionStartRatio = section.getDouble("StartRatio");
        this.sectionEndRatio = section.getDouble("EndRatio");

        NBTTagList positions = section.getTagList("CorePositions", 10);
        this.railGroupCorePositions = new int[positions.tagCount()][];
        for (int i = 0; i < positions.tagCount(); ++i) {
            NBTTagCompound pos = positions.getCompoundTagAt(i);
            this.railGroupCorePositions[i] = new int[]{pos.getInteger("X"), pos.getInteger("Y"), pos.getInteger("Z")};
        }
        this.invalidateRailMapCache();
    }

    public void writeSectionData(NBTTagCompound parent) {
        if (!this.isRailSection()) {
            return;
        }
        NBTTagCompound section = new NBTTagCompound();
        section.setLong("GroupMost", this.railGroupId.getMostSignificantBits());
        section.setLong("GroupLeast", this.railGroupId.getLeastSignificantBits());
        section.setDouble("StartRatio", this.sectionStartRatio);
        section.setDouble("EndRatio", this.sectionEndRatio);
        section.setTag("LogicalStartRP", this.logicalRailPositions[0].writeToNBT());
        section.setTag("LogicalEndRP", this.logicalRailPositions[1].writeToNBT());

        NBTTagList positions = new NBTTagList();
        for (int[] corePos : this.railGroupCorePositions) {
            NBTTagCompound pos = new NBTTagCompound();
            pos.setInteger("X", corePos[0]);
            pos.setInteger("Y", corePos[1]);
            pos.setInteger("Z", corePos[2]);
            positions.appendTag(pos);
        }
        section.setTag("CorePositions", positions);
        parent.setTag(SECTION_TAG, section);
    }

    @Override
    public void createRailMap() {
        if (!this.isLoaded()) {
            return;
        }
        if (!this.isRailSection()) {
            super.createRailMap();
            return;
        }
        RailPosition[] logical = copyRailPositions(this.logicalRailPositions);
        RailMapBasic sourceMap = new RailMapBasic(logical[0], logical[1], this.fixRTMRailMapVersion);
        this.railmap = new RailMapSection(sourceMap, this.railPositions[0], this.railPositions[1],
                this.sectionStartRatio, this.sectionEndRatio);
    }

    @Override
    protected void invalidateRailMapCache() {
        this.railmap = null;
    }

    @Override
    public void replaceRail(ResourceStateRail state) {
        if (!this.isRailSection() || this.world == null) {
            super.replaceRail(state);
            return;
        }
        for (TileEntityLargeRailSectionCore core : this.groupCores()) {
            core.replaceRailLocal(state);
        }
    }

    private void replaceRailLocal(ResourceStateRail state) {
        super.replaceRail(state);
    }

    @Override
    public void addSubRail(ResourceStateRail state) {
        if (!this.isRailSection() || this.world == null) {
            super.addSubRail(state);
            return;
        }
        for (TileEntityLargeRailSectionCore core : this.groupCores()) {
            core.addSubRailLocal(state);
        }
    }

    private void addSubRailLocal(ResourceStateRail state) {
        super.addSubRail(state);
    }

    @Override
    public void setSignal(int signal) {
        if (!this.isRailSection() || this.world == null) {
            super.setSignal(signal);
            return;
        }
        for (TileEntityLargeRailSectionCore core : this.groupCores()) {
            core.setSignalLocal(signal);
        }
    }

    private void setSignalLocal(int signal) {
        super.setSignal(signal);
    }

    @Override
    public boolean isLogicalRailOccupied() {
        if (!this.isRailSection() || this.world == null) {
            return super.isLogicalRailOccupied();
        }
        for (TileEntityLargeRailSectionCore core : this.groupCores()) {
            if (core.isLocallyOccupied()) {
                return true;
            }
        }
        return false;
    }

    private boolean isLocallyOccupied() {
        return this.isCollidedTrain;
    }

    @Override
    public int getRailRenderMinimumSplit() {
        return this.isRailSection() ? 1 : 0;
    }

    @Override
    public int getRailRenderEndOffset() {
        return (this.isRailSection() && this.sectionEndRatio < 1.0D - RATIO_EPSILON) ? 1 : 0;
    }

    @Override
    public boolean shouldRenderRailStartCap() {
        return !this.isRailSection() || this.sectionStartRatio <= RATIO_EPSILON;
    }

    @Override
    public boolean shouldRenderRailEndCap() {
        return !this.isRailSection() || this.sectionEndRatio >= 1.0D - RATIO_EPSILON;
    }

    @Override
    public void breakLogicalRail() {
        if (!this.isRailSection() || this.world == null) {
            super.breakLogicalRail();
            return;
        }

        List<TileEntityLargeRailSectionCore> cores = this.groupCores();
        for (TileEntityLargeRailSectionCore core : cores) {
            core.breaking = true;
        }

        RailPosition[] logical = copyRailPositions(this.logicalRailPositions);
        RailMapBasic fullMap = new RailMapBasic(logical[0], logical[1], this.fixRTMRailMapVersion);

        java.util.LinkedHashSet<BlockPos> candidates = new java.util.LinkedHashSet<>();
        for (int[] pos : fullMap.getRailBlockList(this.getResourceState(), true)) {
            candidates.add(new BlockPos(pos[0], pos[1], pos[2]));
        }
        for (TileEntityLargeRailSectionCore core : cores) {
            RailMap rm = core.getRailMap(null);
            if (rm != null) {
                for (int[] pos : rm.getRailBlockList(core.getResourceState(), true)) {
                    candidates.add(new BlockPos(pos[0], pos[1], pos[2]));
                }
            }
        }
        for (int[] pos : this.railGroupCorePositions) {
            candidates.add(new BlockPos(pos[0], pos[1], pos[2]));
        }

        java.util.LinkedHashSet<BlockPos> targets = new java.util.LinkedHashSet<>();
        for (BlockPos pos : candidates) {
            TileEntity tile = this.world.getTileEntity(pos);
            if (tile instanceof TileEntityLargeRailBase) {
                TileEntityLargeRailCore owner = ((TileEntityLargeRailBase) tile).getRailCore();
                if (owner != null && this.isSameLogicalRail(owner)) {
                    targets.add(pos);
                }
            }
        }
        for (BlockPos pos : targets) {
            this.world.setBlockToAir(pos);
        }
    }

    @Override
    public void setPos(int x, int y, int z, int prevX, int prevY, int prevZ) {
        int difX = x - prevX;
        int difY = y - prevY;
        int difZ = z - prevZ;
        if (this.isRailSection()) {
            for (RailPosition rp : this.logicalRailPositions) {
                rp.movePos(difX, difY, difZ);
            }
            for (int[] pos : this.railGroupCorePositions) {
                pos[0] += difX;
                pos[1] += difY;
                pos[2] += difZ;
            }
        }
        super.setPos(x, y, z, prevX, prevY, prevZ);
        this.invalidateRailMapCache();
    }

    @Override
    public String getRailShapeName() {
        if (!this.isRailSection()) {
            return super.getRailShapeName();
        }
        RailPosition[] positions = this.logicalRailPositions;
        String sb = "Type:Normal(Section), " +
                "X:" + (positions[1].blockX - positions[0].blockX) + ", " +
                "Y:" + (positions[1].blockY - positions[0].blockY) + ", " +
                "Z:" + (positions[1].blockZ - positions[0].blockZ);
        return sb;
    }

    private List<TileEntityLargeRailSectionCore> groupCores() {
        if (!this.isRailSection() || this.world == null) {
            return java.util.Collections.singletonList(this);
        }
        List<TileEntityLargeRailSectionCore> result = new ArrayList<>();
        for (int[] pos : this.railGroupCorePositions) {
            TileEntity tile = BlockUtil.getTileEntity(this.world, pos[0], pos[1], pos[2]);
            if (tile instanceof TileEntityLargeRailSectionCore) {
                TileEntityLargeRailSectionCore core = (TileEntityLargeRailSectionCore) tile;
                if (this.isSameLogicalRail(core)) {
                    result.add(core);
                }
            }
        }
        if (!result.contains(this)) {
            result.add(this);
        }
        return result;
    }

    private boolean containsCorePosition(int x, int y, int z) {
        for (int[] pos : this.railGroupCorePositions) {
            if (pos[0] == x && pos[1] == y && pos[2] == z) {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte getPacketType() {
        return this.isRailSection() ? PacketLargeRailCore.TYPE_SECTION : super.getPacketType();
    }
}