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

package jp.ngt.rtm.rail.util;

import com.anatawa12.fixRtm.Deprecation;
import com.anatawa12.fixRtm.UtilsKt;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public abstract class SwitchType {
    public final byte id;
    protected RailMapSwitch[] railMaps;
    protected Point[] points;
    public final int fixRTMRailMapVersion;

    @Deprecated
    protected SwitchType(int par1) {
        this(par1, 0);
        Deprecation.found("SwitchType#SwitchType");
    }

    protected SwitchType(int par1, int fixRTMRailMapVersion) {
        this.id = (byte) par1;
        this.fixRTMRailMapVersion = fixRTMRailMapVersion;
    }

    public abstract boolean init(List<RailPosition> switchList, List<RailPosition> normalList);

    public abstract String getName();

    public void onBlockChanged(World world) {
        ;
    }

    public void onUpdate(World world) {
        for (int i = 0; i < this.points.length; ++i) {
            this.points[i].onUpdate(world);
        }
    }

    public abstract RailMap getRailMap(Entity entity);

    public RailMapSwitch[] getAllRailMap() {
        return this.railMaps;
    }

    public Point[] getPoints() {
        return this.points;
    }

    public Point getNearestPoint(Entity entity) {
        Point point = null;
        double distance = Double.MAX_VALUE;
        for (Point p0 : this.getPoints()) {
            double d0 = entity.getDistanceSq(p0.rpRoot.posX, 0.0D, p0.rpRoot.posZ);
            if (d0 <= distance) {
                point = p0;
                distance = d0;
            }
        }
        return point;
    }

    public static class SwitchBasic extends SwitchType {
        @Deprecated
        public SwitchBasic() {
            this(0);
            Deprecation.found("SwitchBasic#SwitchBasic");
        }

        public SwitchBasic(int fixRTMRailMapVersion) {
            super(0, fixRTMRailMapVersion);
        }

        @Override
        public boolean init(List<RailPosition> switchList, List<RailPosition> normalList) {
            RailMapSwitch[] arailmapswitch = new RailMapSwitch[2];
            RailPosition railposition = switchList.get(0);
            RailPosition railposition1 = normalList.get(0);
            RailPosition railposition2 = normalList.get(1);
            RailDir raildir = railposition.getDir(railposition1, railposition2);
            arailmapswitch[0] = new RailMapSwitch(railposition, railposition1, raildir, RailDir.NONE, fixRTMRailMapVersion);
            arailmapswitch[1] = new RailMapSwitch(railposition, railposition2, raildir.invert(), RailDir.NONE, fixRTMRailMapVersion);
            this.railMaps = arailmapswitch;
            this.points = new Point[3];
            this.points[0] = new Point(railposition, arailmapswitch[0], arailmapswitch[1]);
            this.points[1] = new Point(railposition1, arailmapswitch[0]);
            this.points[2] = new Point(railposition2, arailmapswitch[1]);
            return true;
        }

        @Override
        public void onBlockChanged(World world) {
            super.onBlockChanged(world);

            if (this.railMaps[0].getStartRP().checkRSInput(world)) {
                this.railMaps[0].setState(false);
                this.railMaps[1].setState(true);
            } else {
                this.railMaps[0].setState(true);
                this.railMaps[1].setState(false);
            }
        }

        @Override
        public RailMap getRailMap(Entity entity) {
            return this.points[0].getActiveRailMap(entity.world);
        }

        @Override
        public String getName() {
            return "Simple";
        }
    }

    public static class SwitchSingleCross extends SwitchType {
        @Deprecated
        public SwitchSingleCross() {
            this(0);
            Deprecation.found("SwitchSingleCross#SwitchSingleCross");
        }

        public SwitchSingleCross(int fixRTMRailMapVersion) {
            super(1, fixRTMRailMapVersion);
        }

        @Override
        public boolean init(List<RailPosition> switchList, List<RailPosition> normalList) {
            RailMapSwitch[] arailmapswitch = new RailMapSwitch[3];
            RailPosition railposition = switchList.get(0);
            RailPosition railposition1 = switchList.get(1);
            RailDir raildir = RailDir.NONE;
            RailDir raildir1 = RailDir.NONE;
            int i = 0;

            for (RailPosition railposition2 : switchList) {
                for (RailPosition railposition3 : normalList) {
                    if (railposition2.direction != railposition3.direction) {
                        boolean flag = (railposition2 == railposition);
                        RailPosition railposition4 = flag ? railposition1 : railposition;
                        RailDir raildir2 = railposition2.getDir(railposition4, railposition3);
                        if (flag) {
                            raildir = raildir2;
                        } else {
                            raildir1 = raildir2;
                        }
                        arailmapswitch[i] = new RailMapSwitch(railposition2, railposition3, raildir2.invert(), RailDir.NONE, fixRTMRailMapVersion);
                    }
                }
                ++i;
            }
            arailmapswitch[2] = new RailMapSwitch(railposition, railposition1, raildir, raildir1, fixRTMRailMapVersion);

            if (!UtilsKt.isAllNotNull(arailmapswitch))
                return false;

            this.railMaps = arailmapswitch;
            this.points = new Point[4];
            this.points[0] = new Point(railposition, arailmapswitch[0], arailmapswitch[2]);
            this.points[1] = new Point(railposition1, arailmapswitch[1], arailmapswitch[2]);
            this.points[2] = new Point(railposition == arailmapswitch[0].startRP ? arailmapswitch[0].endRP : arailmapswitch[0].startRP, arailmapswitch[0]);
            this.points[3] = new Point(railposition1 == arailmapswitch[1].startRP ? arailmapswitch[1].endRP : arailmapswitch[1].startRP, arailmapswitch[1]);

            return true;
        }

        @Override
        public void onBlockChanged(World world) {
            super.onBlockChanged(world);

            if (this.railMaps[2].isGettingPowered(world)) {
                this.railMaps[0].setState(false);
                this.railMaps[1].setState(false);
                this.railMaps[2].setState(true);
            } else {
                this.railMaps[0].setState(true);
                this.railMaps[1].setState(true);
                this.railMaps[2].setState(false);
            }
        }

        @Override
        public RailMap getRailMap(Entity entity) {
            RailMap map1 = this.points[0].getActiveRailMap(entity.world);
            RailMap map2 = this.points[1].getActiveRailMap(entity.world);
            if (map1 == map2) {
                return map1;
            } else {
                int n1 = map1.getNearlestPoint(16, entity.posX, entity.posZ);
                int n2 = map2.getNearlestPoint(16, entity.posX, entity.posZ);
                double[] pos1 = map1.getRailPos(16, n1);
                double[] pos2 = map1.getRailPos(16, n2);
                double d1 = entity.getDistanceSq(pos1[1], 0.0D, pos1[0]);
                double d2 = entity.getDistanceSq(pos2[1], 0.0D, pos2[0]);
                return d1 < d2 ? map1 : map2;
            }
        }

        @Override
        public String getName() {
            return "Crossover";
        }
    }

    public static class SwitchScissorsCross extends SwitchType {
        @Deprecated
        public SwitchScissorsCross() {
            this(0);
            Deprecation.found("SwitchScissorsCross#SwitchScissorsCross");
        }

        public SwitchScissorsCross(int fixRTMRailMapVersion) {
            super(2, fixRTMRailMapVersion);
        }

        @Override
        public boolean init(List<RailPosition> switchList, List<RailPosition> normalList) {
            RailMapSwitch[] rails = new RailMapSwitch[4];
            RailPosition[][] rps = new RailPosition[4][2];
            int rpsCount = 0;
            for (int i = 0; i < 4; ++i) {
                for (int j = i + 1; j < 4; ++j) {
                    int dirDif = Math.abs(switchList.get(i).direction - switchList.get(j).direction);
                    if (dirDif > 4) {
                        dirDif = 8 - dirDif;
                    }

                    if (dirDif > 2 && rpsCount < 4) {
                        rps[rpsCount] = new RailPosition[]{switchList.get(i), switchList.get(j)};
                        ++rpsCount;
                    }
                }
            }

            if (rpsCount == 4) {
                for (int i = 0; i < 4; ++i) {
                    RailDir dir0 = RailDir.NONE;
                    RailDir dir1 = RailDir.NONE;

                    for (int j = 0; j < 4; ++j) {
                        if (i == j) {
                            continue;
                        }

                        if (rps[i][0] == rps[j][0]) {
                            dir0 = rps[i][0].getDir(rps[i][1], rps[j][1]);
                        } else if (rps[i][0] == rps[j][1]) {
                            dir0 = rps[i][0].getDir(rps[i][1], rps[j][0]);
                        } else if (rps[i][1] == rps[j][0]) {
                            dir1 = rps[i][1].getDir(rps[i][0], rps[j][1]);
                        } else if (rps[i][1] == rps[j][1]) {
                            dir1 = rps[i][1].getDir(rps[i][0], rps[j][0]);
                        }
                    }
                    rails[i] = new RailMapSwitch(rps[i][0], rps[i][1], dir0, dir1, fixRTMRailMapVersion);
                }

                this.railMaps = rails;

                this.points = new Point[4];
                for (int i = 0; i < 4; ++i) {
                    RailPosition rp = switchList.get(i);
                    RailMapSwitch rms1 = null;
                    RailMapSwitch rms2 = null;

                    for (int j = 0; j < 4; ++j) {
                        if (rails[j].startRP == rp || rails[j].endRP == rp) {
                            if (rms1 == null) {
                                rms1 = rails[j];
                            } else {
                                rms2 = rails[j];
                                break;
                            }
                        }
                    }

                    this.points[i] = new Point(rp, rms1, rms2);
                }

                return true;
            }
            return false;
        }

        @Override
        public void onBlockChanged(World world) {
            super.onBlockChanged(world);

            RailMapSwitch openRMS = null;
            for (int j = 0; j < 2; ++j) {
                for (int i = 0; i < 4; ++i) {
                    RailMapSwitch rms = this.railMaps[i];
                    if (rms.startDir == rms.endDir) {
                        if (j == 0) {
                            if (rms.isGettingPowered(world)) {
                                openRMS = rms;
                                break;
                            }
                        } else {
                            if (rms == openRMS) {
                                rms.setState(true);
                            } else {
                                rms.setState(false);
                            }
                        }
                    } else {
                        if (j == 1) {
                            if (openRMS == null) {
                                rms.setState(true);
                            } else {
                                rms.setState(false);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public RailMap getRailMap(Entity entity) {
            RailMap map = null;
            double distance = Double.MAX_VALUE;
            for (Point point : this.getPoints()) {
                RailMap map1 = point.getActiveRailMap(entity.world);
                if (map1 == map) {
                    continue;
                }

                int n1 = map1.getNearlestPoint(16, entity.posX, entity.posZ);
                double[] pos1 = map1.getRailPos(16, n1);
                double d1 = entity.getDistanceSq(pos1[1], 0.0D, pos1[0]);
                if (d1 < distance) {
                    distance = d1;
                    map = map1;
                }
            }
            return map;
        }

        @Override
        public String getName() {
            return "Scissors Crossing";
        }
    }

    public static class SwitchDiamondCross extends SwitchType {
        @Deprecated
        public SwitchDiamondCross() {
            this(0);
            Deprecation.found("SwitchDiamondCross#SwitchDiamondCross");
        }

        public SwitchDiamondCross(int fixRTMRailMapVersion) {
            super(3, fixRTMRailMapVersion);
        }

        @Override
        public boolean init(List<RailPosition> switchList, List<RailPosition> normalList) {
            List<RailPosition> rpList = new ArrayList<RailPosition>();
            rpList.addAll(switchList);
            rpList.addAll(normalList);

            RailMapSwitch[] arailmapswitch = new RailMapSwitch[2];
            int i = 0;

            for (int j = 0; j < 4; ++j) {
                for (int k = 0; k < 4; ++k) {
                    if (j < k && Math.abs((rpList.get(j)).direction - (rpList.get(k)).direction) == 4) {
                        arailmapswitch[i] = new RailMapSwitch(rpList.get(j), rpList.get(k), RailDir.NONE, RailDir.NONE, fixRTMRailMapVersion);
                        ++i;
                        if (i >= 2) {
                            this.railMaps = arailmapswitch;
                            this.points = new Point[4];
                            this.points[0] = new Point(arailmapswitch[0].startRP, arailmapswitch[0]);
                            this.points[1] = new Point(arailmapswitch[0].endRP, arailmapswitch[0]);
                            this.points[2] = new Point(arailmapswitch[1].startRP, arailmapswitch[1]);
                            this.points[3] = new Point(arailmapswitch[1].endRP, arailmapswitch[1]);
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public void onBlockChanged(World world) {
            super.onBlockChanged(world);
        }

        @Override
        public RailMap getRailMap(Entity entity) {
            RailMap map1 = this.points[0].getActiveRailMap(entity.world);
            RailMap map2 = this.points[2].getActiveRailMap(entity.world);
            int n1 = map1.getNearlestPoint(16, entity.posX, entity.posZ);
            int n2 = map2.getNearlestPoint(16, entity.posX, entity.posZ);
            double[] pos1 = map1.getRailPos(16, n1);
            double[] pos2 = map2.getRailPos(16, n2);
            double d1 = entity.getDistanceSq(pos1[1], 0.0D, pos1[0]);
            double d2 = entity.getDistanceSq(pos2[1], 0.0D, pos2[0]);
            return d1 < d2 ? map1 : map2;
        }

        @Override
        public String getName() {
            return "Diamond Crossing";
        }
    }
}