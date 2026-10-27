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

package jp.ngt.rtm.entity.train.parts;

import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.entity.vehicle.EntityVehicleBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class EntityFloor extends EntityVehiclePart {
    private static final DataParameter<Byte> TYPE = EntityDataManager.<Byte>createKey(EntityFloor.class, DataSerializers.BYTE);

    private int seatType;

    public EntityFloor(World par1) {
        super(par1);
        this.preventEntitySpawning = true;
        this.setSize(1.0F, 0.0625F);
    }

    public EntityFloor(World par1, EntityVehicleBase par2, float[] par3Pos, byte par4Type) {
        super(par1, par2, par3Pos);
        this.setSeatType(par4Type);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.getDataManager().register(TYPE, Byte.valueOf((byte) 0));
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        this.setSeatType(nbt.getByte("seatType"));
        super.readEntityFromNBT(nbt);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setByte("seatType", this.getSeatType());
        super.writeEntityToNBT(nbt);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return null;
    }

    @Override
    public void onLoadVehicle() {
        this.getVehicle().setFloor(this);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
    }

    @Override
    public double getMountedYOffset() {
        return (double) this.height + 0.25D;
    }

    @Override
    public boolean attackEntityFrom(DamageSource par1, float par2) {
        if (this.getVehicle() == null || this.getVehicle().isDead) {
            if (!this.world.isRemote) {
                this.setDead();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        Entity passenger = this.getFirstPassenger();
        if (passenger != null) {
            if (passenger instanceof EntityPlayer && passenger != player) {
                return true;
            } else if (passenger instanceof EntityLiving) {
                passenger.startRiding((Entity) null);
                return true;
            }
            return true;
        } else {
            int seatType = this.getSeatType();
            if (!this.world.isRemote && seatType != 0) {
                if (NGTUtil.isEquippedItem(player, Items.LEAD)) {
                    double d0 = 7.0D;
                    List<EntityLiving> list = this.world.getEntitiesWithinAABB(EntityLiving.class,
                            new AxisAlignedBB(this.posX - d0, this.posY - d0, this.posZ - d0, this.posX + d0, this.posY + d0, this.posZ + d0));
                    for (EntityLiving living : list) {
                        if (living.getLeashed() && living.getLeashHolder().equals(player)) {
                            living.clearLeashed(true, true);
                            living.startRiding(this);
                            return true;
                        }
                    }
                }

                if (!player.isSneaking()) {
                    player.startRiding(this);
                }
            }
            return true;
        }
    }

    public void setSeatType(byte par1) {
        this.getDataManager().set(TYPE, Byte.valueOf(par1));
    }

    public byte getSeatType() {
        return this.getDataManager().get(TYPE);
    }

    @Override
    protected void doBlockCollisions() {
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getBrightnessForRender() {
        EntityVehicleBase entityvehiclebase = this.getVehicle();
        return entityvehiclebase != null ? entityvehiclebase.getBrightnessForRender() : super.getBrightnessForRender();
    }

    @Override
    public void updatePassenger(Entity passenger) {
        super.updatePassenger(passenger);

        if (!this.isPassenger(passenger)) return;

        EntityVehicleBase<?> vehicle = this.getVehicle();

        if (!(passenger instanceof EntityPlayer) || !(vehicle instanceof EntityTrainBase))
            return;

        EntityPlayer player = ((EntityPlayer) passenger);

        float yaw = MathHelper.wrapDegrees(vehicle.rotationYaw - vehicle.prevRotationYaw);
        float pitch = MathHelper.wrapDegrees(vehicle.rotationPitch - vehicle.prevRotationPitch);

        player.renderYawOffset -= yaw;
        player.rotationYaw -= yaw;
        player.rotationPitch -= pitch;
    }
}