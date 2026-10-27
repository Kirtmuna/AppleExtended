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

package jp.ngt.rtm.electric;

import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.rtm.entity.EntityElectricalWiring;

public class TileEntityDummyEW extends TileEntityElectricalWiring {
    public final EntityElectricalWiring entityEW;
    private int prevSignal;

    public TileEntityDummyEW(EntityElectricalWiring par1Entity) {
        super();
        this.entityEW = par1Entity;
    }

    @Override
    public void onGetElectricity(int x, int y, int z, int level, int counter) {
        super.onGetElectricity(x, y, z, level, counter);

        if (!(x == this.getX() && y == this.getY() && z == this.getZ())) {
            this.entityEW.setElectricity(level);
        }
    }

    @Override
    public void update() {
        super.update();

        if (!this.world.isRemote) {
            int level = this.entityEW.getElectricity();
            if (level >= 0 && level != this.prevSignal) {
                this.onGetElectricity(this.getX(), this.getY(), this.getZ(), level, 0);
                this.prevSignal = level;
            }
        }
    }

    @Override
    public boolean isBlockTile() {
        return false;
    }

    @Override
    public Vec3 getWirePos() {
        return new Vec3(0.0, -0.5, 0.0);
    }
}