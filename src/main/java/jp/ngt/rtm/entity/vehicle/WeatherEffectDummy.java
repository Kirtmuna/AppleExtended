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

import jp.ngt.ngtlib.io.NGTLog;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

public final class WeatherEffectDummy extends Entity {
    private final EntityVehicleBase parent;

    public WeatherEffectDummy(World world, EntityVehicleBase vehicle) {
        super(world);
        this.parent = vehicle;
        NGTLog.debug("[WED] Spawn %d", this.parent.getEntityId());
    }

    public EntityVehicleBase getParent() {
        return this.parent;
    }

    public boolean isLinkedTo(EntityVehicleBase vehicle) {
        return this.parent == vehicle || this.getEntityId() == vehicle.getEntityId();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.shouldRemove()) {
            this.setDead();
            NGTLog.debug("[WED] Remove %d", this.parent.getEntityId());
            return;
        }

        this.lastTickPosX = this.parent.lastTickPosXPublic;
        this.lastTickPosY = this.parent.lastTickPosYPublic;
        this.lastTickPosZ = this.parent.lastTickPosZPublic;
        this.posX = this.parent.posX;
        this.posY = this.parent.posY;
        this.posZ = this.parent.posZ;
        this.rotationYaw = this.parent.rotationYaw;
        this.rotationPitch = this.parent.rotationPitch;
    }

    private boolean shouldRemove() {
        return this.parent.isDead
                || this.parent.world != this.world
                || !this.world.loadedEntityList.contains(this.parent);
    }

    @Override
    public int getEntityId() {
        return this.parent.getEntityId();
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return this.parent.shouldRenderInPass(pass);
    }

    @Override
    public boolean isInRangeToRender3d(double x, double y, double z) {
        return true;
    }

    @Override
    public int getBrightnessForRender() {
        return this.parent.getBrightnessForRender();
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
    }

    @Override
    public AxisAlignedBB getEntityBoundingBox() {
        return this.parent.getEntityBoundingBox();
    }
}
