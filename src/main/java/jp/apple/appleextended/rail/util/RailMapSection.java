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

import jp.ngt.rtm.rail.util.RailMap;
import jp.ngt.rtm.rail.util.RailMapBasic;
import jp.ngt.rtm.rail.util.RailPosition;


public final class RailMapSection extends RailMap {
    private final RailMapBasic source;
    private final RailPosition sectionStartRP;
    private final RailPosition sectionEndRP;
    private final double startRatio;
    private final double endRatio;
    private final double ratioLength;

    public RailMapSection(RailMapBasic source, RailPosition sectionStartRP, RailPosition sectionEndRP,
                          double startRatio, double endRatio) {
        super();
        this.source = source;
        this.sectionStartRP = sectionStartRP;
        this.sectionEndRP = sectionEndRP;
        this.startRatio = startRatio;
        this.endRatio = endRatio;
        this.ratioLength = Math.max(0.0D, endRatio - startRatio);
    }

    public RailMapBasic getSource() {
        return this.source;
    }

    public double getStartRatio() {
        return this.startRatio;
    }

    public double getEndRatio() {
        return this.endRatio;
    }

    public double sourceRatio(double localRatio) {
        double clamped = Math.max(0.0D, Math.min(1.0D, localRatio));
        return this.startRatio + this.ratioLength * clamped;
    }

    private double sourceRatio(int split, int index) {
        double localRatio = split <= 0 ? 0.0D : (double) index / (double) split;
        return this.sourceRatio(localRatio);
    }

    @Override
    public RailPosition getStartRP() {
        return this.sectionStartRP;
    }

    @Override
    public RailPosition getEndRP() {
        return this.sectionEndRP;
    }

    @Override
    public double getLength() {
        return this.source.getLength() * this.ratioLength;
    }

    @Override
    public int getNearlestPoint(int split, double x, double z) {
        if (split <= 0) {
            return 0;
        }
        int nearest = 0;
        double distance = Double.MAX_VALUE;
        for (int index = 0; index <= split; ++index) {
            double[] point = this.getRailPos(split, index);
            double dx = x - point[1];
            double dz = z - point[0];
            double current = dx * dx + dz * dz;
            if (current < distance) {
                distance = current;
                nearest = index;
            }
        }
        return nearest;
    }

    @Override
    public double[] getRailPos(int split, int index) {
        return this.source.getRailPos(this.sourceRatio(split, index));
    }

    @Override
    public double getRailHeight(int split, int index) {
        return this.source.getRailHeight(this.sourceRatio(split, index));
    }

    @Override
    public float getRailYaw(int split, int index) {
        return this.source.getRailYaw(this.sourceRatio(split, index));
    }

    @Override
    public float getRailPitch(int split, int index) {
        return this.source.getRailPitch(this.sourceRatio(split, index));
    }

    @Override
    public float getRailRoll(int split, int index) {
        return this.source.getRailRoll(this.sourceRatio(split, index));
    }
}