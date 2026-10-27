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
import com.anatawa12.fixRtm.rtm.rail.util.SwitchTypeSingleCrossFixRTMV1;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RailMaker {
    public final int fixRTMRailMapVersion;
    private World worldObj;
    private List<RailPosition> rpList;

    @Deprecated
    public RailMaker(World world, List<RailPosition> par2) {
        this(world, par2, 0);
        Deprecation.found("RailMaker#RailMaker");
    }

    public RailMaker(World world, List<RailPosition> par2, int fixRTMRailMapVersion) {
        this.worldObj = world;
        this.rpList = par2;
        this.fixRTMRailMapVersion = fixRTMRailMapVersion;
    }

    @Deprecated
    public RailMaker(World world, RailPosition[] par2) {
        this(world, par2, 0);
        Deprecation.found("RailMaker#RailMaker");
    }

    public RailMaker(World world, RailPosition[] par2, int fixRTMRailMapVersion) {
        this(world, new ArrayList<>(Arrays.asList(par2)), fixRTMRailMapVersion);
    }

    private SwitchType getSwitchType() {
        if (this.rpList.size() == 3) {
            int i0 = 0;
            for (RailPosition rp : this.rpList) {
                i0 += (rp.switchType == 1) ? 1 : 0;
            }

            if (i0 == 1) {
                return new SwitchType.SwitchBasic(fixRTMRailMapVersion);
            }
        } else if (this.rpList.size() == 4) {
            int i0 = 0;
            for (RailPosition rp : this.rpList) {
                i0 += (rp.switchType == 1) ? 1 : 0;
            }

            if (i0 == 2) {
                if (fixRTMRailMapVersion >= 1)
                    return new SwitchTypeSingleCrossFixRTMV1(fixRTMRailMapVersion);
                return new SwitchType.SwitchSingleCross(fixRTMRailMapVersion);
            } else if (i0 == 4) {
                for (int i = 0; i < this.rpList.size(); ++i) {
                    for (int j = i + 1; j < this.rpList.size(); ++j) {
                        if (this.rpList.get(i).direction == this.rpList.get(j).direction) {
                            return new SwitchType.SwitchScissorsCross(fixRTMRailMapVersion);
                        }
                    }
                }
                return new SwitchType.SwitchDiamondCross(fixRTMRailMapVersion);
            }
        }

        return null;
    }

    public SwitchType getSwitch() {
        SwitchType type = this.getSwitchType();
        if (type != null) {
            List<RailPosition> switchList = new ArrayList<>();
            List<RailPosition> normalList = new ArrayList<>();
            for (RailPosition rp : this.rpList) {
                if (rp.switchType == 1) {
                    switchList.add(rp);
                } else {
                    normalList.add(rp);
                }
            }

            if (type.init(switchList, normalList)) {
                return type;
            }
        }

        return null;
    }
}