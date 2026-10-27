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

public final class RailMapSwitch extends RailMapBasic {
    public final RailDir startDir, endDir;
    private boolean isOpen;

    @Deprecated
    public RailMapSwitch(RailPosition par1, RailPosition par2, RailDir sDir, RailDir eDir) {
        this(par1, par2, sDir, eDir, 0);
        Deprecation.found("RailMapSwitch#RailMapSwitch");
    }

    public RailMapSwitch(RailPosition par1, RailPosition par2, RailDir sDir, RailDir eDir, int version) {
        super(par1, par2, version);
        this.startDir = sDir;
        this.endDir = eDir;
    }

    @Deprecated
    public RailMapSwitch setState(boolean par1) {
        this.isOpen = par1;
        return this;
    }
}