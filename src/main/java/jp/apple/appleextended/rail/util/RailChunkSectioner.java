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

package jp.apple.appleextended.rail.util;

import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.rtm.rail.util.RailMapBasic;
import jp.ngt.rtm.rail.util.RailPosition;

import java.util.*;


public final class RailChunkSectioner {
    private static final double RATIO_EPSILON = 1.0E-7;
    private static final double SAMPLE_PER_METER = 4.0;
    private static final double MIN_SECTION_LENGTH_METERS = 2.0D;

    private RailChunkSectioner() {
    }

    public static List<RailSection> split(RailMapBasic source) {
        if (source.getLength() <= 0.0D) {
            return Collections.singletonList(
                    new RailSection(0.0D, 1.0D, copy(source.getStartRP()), copy(source.getEndRP())));
        }

        List<Double> boundaries = enforceMinimumSectionLength(findBoundaries(source), source.getLength());
        if (boundaries.isEmpty()) {
            return Collections.singletonList(
                    new RailSection(0.0D, 1.0D, copy(source.getStartRP()), copy(source.getEndRP())));
        }

        List<SectionStart> starts = new ArrayList<>();
        starts.add(new SectionStart(0.0D, copy(source.getStartRP())));

        Set<BlockPos3> occupiedCoreBlocks = new LinkedHashSet<>();
        RailPosition sourceStart = source.getStartRP();
        occupiedCoreBlocks.add(new BlockPos3(sourceStart.blockX, sourceStart.blockY, sourceStart.blockZ));

        for (int index = 0; index < boundaries.size(); ++index) {
            double ratio = boundaries.get(index);
            double nextRatio = (index + 1 < boundaries.size()) ? boundaries.get(index + 1) : 1.0D;
            BlockPos3 core = findCoreBlock(source, ratio, nextRatio, occupiedCoreBlocks);
            if (core == null) {
                continue;
            }
            RailPosition rp = createBoundaryRP(source, ratio, core);
            starts.add(new SectionStart(ratio, rp));
            occupiedCoreBlocks.add(core);
        }

        if (starts.size() == 1) {
            return Collections.singletonList(new RailSection(0.0D, 1.0D, starts.get(0).rp, copy(source.getEndRP())));
        }

        List<RailSection> result = new ArrayList<>(starts.size());
        for (int index = 0; index < starts.size(); ++index) {
            SectionStart start = starts.get(index);
            boolean hasNext = index + 1 < starts.size();
            double endRatio = hasNext ? starts.get(index + 1).ratio : 1.0D;
            RailPosition endRP = hasNext ? asSectionEnd(starts.get(index + 1).rp) : copy(source.getEndRP());
            if (endRatio - start.ratio > RATIO_EPSILON) {
                result.add(new RailSection(start.ratio, endRatio, copy(start.rp), endRP));
            }
        }
        return result;
    }

    private static List<Double> findBoundaries(RailMapBasic source) {
        int samples = Math.max(1, (int) Math.ceil(source.getLength() * SAMPLE_PER_METER));
        List<Double> result = new ArrayList<>();
        double previousRatio = 0.0D;
        ChunkPos previousChunk = chunkAt(source, previousRatio);

        for (int index = 1; index <= samples; ++index) {
            double currentRatio = (double) index / (double) samples;
            ChunkPos currentChunk = chunkAt(source, currentRatio);
            double searchStart = previousRatio;
            ChunkPos searchChunk = previousChunk;
            int guard = 0;

            while (!searchChunk.equals(currentChunk) && guard++ < 4) {
                double boundary = findFirstChunkChange(source, searchStart, currentRatio, searchChunk);
                if (boundary > RATIO_EPSILON && boundary < 1.0D - RATIO_EPSILON
                        && (result.isEmpty() || boundary - result.get(result.size() - 1) > RATIO_EPSILON)) {
                    result.add(boundary);
                }

                double after = Math.min(currentRatio,
                        boundary + Math.max(RATIO_EPSILON * 4.0D, (currentRatio - previousRatio) * 1.0E-5D));
                if (after <= searchStart + RATIO_EPSILON) {
                    break;
                }
                searchStart = after;
                searchChunk = chunkAt(source, searchStart);
                currentChunk = chunkAt(source, currentRatio);
            }

            previousRatio = currentRatio;
            previousChunk = currentChunk;
        }

        List<Double> filtered = new ArrayList<>();
        double delta = Math.max(RATIO_EPSILON * 8.0D, 1.0E-6D);
        for (double boundary : result) {
            ChunkPos before = chunkAt(source, Math.max(0.0D, boundary - delta));
            ChunkPos after = chunkAt(source, Math.min(1.0D, boundary + delta));
            if (!before.equals(after)) {
                filtered.add(boundary);
            }
        }
        return filtered;
    }

    private static double findFirstChunkChange(RailMapBasic source, double from, double to, ChunkPos fromChunk) {
        double low = from;
        double high = to;
        while (high - low > RATIO_EPSILON) {
            double middle = (low + high) * 0.5D;
            if (chunkAt(source, middle).equals(fromChunk)) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return high;
    }

    private static BlockPos3 findCoreBlock(RailMapBasic source, double startRatio, double endRatio, Set<BlockPos3> occupied) {
        double length = endRatio - startRatio;
        if (length <= RATIO_EPSILON) {
            return null;
        }

        double targetRatio = Math.min(endRatio, startRatio + Math.max(RATIO_EPSILON * 8.0D, length * 1.0E-4D));
        ChunkPos targetChunk = chunkAt(source, targetRatio);
        for (int index = 0; index <= 32; ++index) {
            double local = (index == 0) ? 1.0E-4D : (double) index / 32.0D;
            double ratio = Math.min(endRatio, startRatio + length * local);
            double[] point = source.getRailPos(ratio);
            BlockPos3 candidate = new BlockPos3(
                    (int) Math.floor(point[1]),
                    (int) Math.floor(source.getRailHeight(ratio)),
                    (int) Math.floor(point[0]));
            if (candidate.chunk().equals(targetChunk) && !occupied.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static RailPosition createBoundaryRP(RailMapBasic source, double ratio, BlockPos3 core) {
        float yaw = source.getRailYaw(ratio);
        int direction = ((int) Math.floor(normalizeAngle(yaw) / 45.0F + 0.5F)) & 7;
        double[] point = source.getRailPos(ratio);

        RailPosition rp = new RailPosition(core.x, core.y, core.z, direction, 0);
        rp.anchorYaw = NGTMath.wrapAngle(yaw);
        rp.anchorPitch = NGTMath.wrapAngle(source.getRailPitch(ratio));
        rp.anchorLengthHorizontal = 0.0F;
        rp.anchorLengthVertical = 0.0F;
        rp.cantEdge = source.getRailRoll(ratio);
        copyLimits(source.getStartRP(), rp);
        rp.setPosition(point[1], source.getRailHeight(ratio), point[0]);
        return rp;
    }

    private static float normalizeAngle(float angle) {
        float result = angle % 360.0F;
        if (result < 0.0F) {
            result += 360.0F;
        }
        return result;
    }

    private static ChunkPos chunkAt(RailMapBasic source, double ratio) {
        double[] point = source.getRailPos(ratio);
        return new ChunkPos(((int) Math.floor(point[1])) >> 4, ((int) Math.floor(point[0])) >> 4);
    }

    private static RailPosition copy(RailPosition source) {
        return RailPosition.readFromNBT(source.writeToNBT());
    }

    private static RailPosition asSectionEnd(RailPosition sectionStart) {
        RailPosition result = copy(sectionStart);
        double x = result.posX;
        double y = result.posY;
        double z = result.posZ;
        result.direction = (byte) ((result.direction + 4) & 7);
        result.anchorYaw = NGTMath.wrapAngle(result.anchorYaw + 180.0F);
        result.setPosition(x, y, z);
        return result;
    }

    private static void copyLimits(RailPosition source, RailPosition target) {
        target.constLimitHP = source.constLimitHP;
        target.constLimitHN = source.constLimitHN;
        target.constLimitWP = source.constLimitWP;
        target.constLimitWN = source.constLimitWN;
        target.cantCenter = source.cantCenter;
        target.cantRandom = source.cantRandom;
    }

    private static List<Double> enforceMinimumSectionLength(List<Double> boundaries, double totalLength) {
        if (boundaries.isEmpty() || totalLength <= 0.0D) {
            return boundaries;
        }
        double minRatio = MIN_SECTION_LENGTH_METERS / totalLength;

        List<Double> result = new ArrayList<>();
        double lastRatio = 0.0D;
        for (double boundary : boundaries) {
            if (boundary - lastRatio >= minRatio) {
                result.add(boundary);
                lastRatio = boundary;
            }
        }
        if (!result.isEmpty() && 1.0D - result.get(result.size() - 1) < minRatio) {
            result.remove(result.size() - 1);
        }

        return result;
    }

    private static final class SectionStart {
        final double ratio;
        final RailPosition rp;

        SectionStart(double ratio, RailPosition rp) {
            this.ratio = ratio;
            this.rp = rp;
        }
    }

    private static final class ChunkPos {
        final int x, z;

        ChunkPos(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ChunkPos)) return false;
            ChunkPos p = (ChunkPos) obj;
            return p.x == this.x && p.z == this.z;
        }

        @Override
        public int hashCode() {
            return x * 31 + z;
        }
    }

    private static final class BlockPos3 {
        final int x, y, z;

        BlockPos3(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        ChunkPos chunk() {
            return new ChunkPos(x >> 4, z >> 4);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BlockPos3)) return false;
            BlockPos3 p = (BlockPos3) obj;
            return p.x == this.x && p.y == this.y && p.z == this.z;
        }

        @Override
        public int hashCode() {
            return (x * 31 + y) * 31 + z;
        }
    }
}