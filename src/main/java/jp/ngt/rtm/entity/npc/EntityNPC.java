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

package jp.ngt.rtm.entity.npc;

import com.anatawa12.fixRtm.rtm.entity.npc.EntityNPCKt;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.network.PacketNBT;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.entity.EntityBullet;
import jp.ngt.rtm.entity.ai.ScriptExecuterNPC;
import jp.ngt.rtm.item.ItemGun;
import jp.ngt.rtm.item.ItemGun.GunType;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.cfg.NPCConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetNPC;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EntityNPC extends EntityTameable implements IResourceSelector, IRangedAttackMob {
    private static final DataParameter<Byte> USING_ITEM = EntityDataManager.createKey(EntityNPC.class, DataSerializers.BYTE);
    private static final DataParameter<String> MENU = EntityDataManager.createKey(EntityNPC.class, DataSerializers.STRING);

    public static final float MAX_HEALTH = 40.0F;
    public static final float FOLLOWING_RANGE = 64.0F;
    public static final float SPEED = 0.45F;
    public static final float ATTACK_POWER = 1.0F;

    private ResourceState<ModelSetNPC> state = new ResourceState<>(RTMResource.NPC, this);
    private ScriptExecuterNPC executer = new ScriptExecuterNPC();
    protected Role myRole = Role.MANNEQUIN;
    private EntityDummyPlayer playerDummy;

    protected int useItemCount;
    protected boolean roleChanged;
    public InventoryNPC inventory = new InventoryNPC(this);

    public EntityNPC(World world) {
        super(world);
        this.setSize(0.6F, 1.8F);
        this.playerDummy = new EntityDummyPlayer(world, this);
    }

    public EntityNPC(World world, EntityPlayer player) {
        this(world);
        this.setOwnerId(player.getUniqueID());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(MAX_HEALTH);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(FOLLOWING_RANGE);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(SPEED);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(ATTACK_POWER);
    }

    @Override
    public void entityInit() {
        super.entityInit();
        this.getDataManager().register(USING_ITEM, Byte.valueOf((byte) 0));
        this.getDataManager().register(MENU, "");
    }

    private void syncData() {
        this.updateResourceState();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.syncData();
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);

        this.state.readFromNBT(nbt.getCompoundTag("State"));
        if (this.state.version < 1) {
            String s = nbt.getString("ModelName");
            this.state.setResourceName(s);
        }
        NBTTagList nbttaglist = nbt.getTagList("Inventory", 10);
        this.inventory.readFromNBT(nbttaglist);
        this.setMenu(nbt.getString("menu"));
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);

        nbt.setTag("State", this.state.writeToNBT());
        nbt.setTag("Inventory", this.inventory.writeToNBT(new NBTTagList()));
        nbt.setString("menu", this.getMenu());
    }

    @Override
    public EntityAgeable createChild(EntityAgeable entity) {
        return null;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public double getYOffset() {
        return 0.0D;
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return 0;
    }

    @Override
    protected void dropFewItems(boolean par1, int par2) {
    }

    @Override
    public void onUpdate() {
        if (this.inventory.isOpening) {
            return;
        }

        super.onUpdate();

        this.playerDummy.setPosition(this.posX, this.posY, this.posZ);
        this.playerDummy.rotationYaw = this.rotationYaw;
        this.playerDummy.rotationPitch = this.rotationPitch;

        if (this.isUsingItem()) {
            ItemStack item = this.getHeldItem();
            boolean hasGun = (!item.isEmpty() && item.getItem() instanceof ItemGun);

            if (!hasGun || this.useItemCount > item.getMaxItemUseDuration()) {
                if (!this.world.isRemote) {
                    if (hasGun) {
                        item.onPlayerStoppedUsing(this.world, this.playerDummy, this.useItemCount);
                    }
                    this.setUseItem(false);
                }
                this.useItemCount = 0;
            } else {
                if (!this.world.isRemote) {
                    item.getItem().onUsingTick(item, this.playerDummy, this.useItemCount);
                }
            }

            ++this.useItemCount;
        } else {
            this.useItemCount = 0;
        }

        if (!this.getEntityWorld().isRemote) {
            this.executer.execScript(this);
        }
    }

    @Override
    public void onLivingUpdate() {
        if (this.myRole != Role.MANNEQUIN) {
            super.onLivingUpdate();

            if (!this.world.isRemote) {
                this.healNPC();
            }
        }

        if (this.roleChanged) {
            this.roleChanged = false;
            this.myRole = Role.getRole(this.state.getResourceSet().getConfig().role);
            this.myRole.init(this);
            this.onInventoryChanged();
        }
    }

    private void debugCurrentAI() {
        for (EntityAITaskEntry entry : this.tasks.taskEntries) {
            if (entry.using) {
                NGTLog.debug("AI:%s", entry.action.getClass().getSimpleName());
            }
        }
    }

    public Role getRole() {
        return this.myRole;
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        this.syncData();
    }

    protected void healNPC() {
        if (this.ticksExisted % 3 == 0 && this.getHealth() < this.getMaxHealth()) {
            int index = this.inventory.hasItem(ItemFood.class);
            if (index >= 0) {
                ItemStack stack = this.inventory.getStackInSlot(index);
                this.heal((float) ((ItemFood) stack.getItem()).getHealAmount(stack));
                stack.shrink(1);
            }
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!this.world.isRemote) {
            this.inventory.dropAllItems();
            if (source.getTrueSource() instanceof EntityPlayer && !((EntityPlayer) source.getTrueSource()).capabilities.isCreativeMode) {
                this.dropEntity();
            }
        }
    }

    protected void dropEntity() {
        int damage = this instanceof EntityMotorman ? 0 : 1;
        this.entityDropItem(new ItemStack(this.getDropItem(), 1, damage), 0.5F);
    }

    @Override
    public boolean attackEntityFrom(DamageSource damageSource, float par2) {
        Entity attacker = damageSource.getTrueSource();
        if ((attacker instanceof EntityPlayer)) {
            if (attacker.equals(this.getOwner())) {
                par2 = 10000.0F;
            } else if (!((EntityPlayer) attacker).capabilities.isCreativeMode && this.myRole == Role.MANNEQUIN) {
                return false;
            }
        }

        if (!this.executer.onAttackedFrom(this, attacker)) {
            return false;
        }

        return super.attackEntityFrom(damageSource, par2);
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        float power = (float) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        int knockback = 0;
        ItemStack stack = this.getHeldItem();
        DamageSource damSrc = DamageSource.causeMobDamage(this);

        if (!this.executer.attackEntity(this, target)) {
            return false;
        }

        if (target instanceof EntityLivingBase) {
            List<ItemStack> list = new ArrayList<>();
            list.add(stack);
            power += EnchantmentHelper.getEnchantmentModifierDamage(list, damSrc);
            knockback += EnchantmentHelper.getKnockbackModifier(this);
        }

        boolean flag = target.attackEntityFrom(damSrc, power);

        if (flag) {
            if (knockback > 0) {
                double vx = -MathHelper.sin(NGTMath.toRadians(this.rotationYaw)) * (float) knockback * 0.5F;
                double vz = MathHelper.cos(NGTMath.toRadians(this.rotationYaw)) * (float) knockback * 0.5F;
                target.addVelocity(vx, 0.1D, vz);
                this.motionX *= 0.6D;
                this.motionZ *= 0.6D;
            }

            int j = EnchantmentHelper.getFireAspectModifier(this);

            if (j > 0) {
                target.setFire(j * 4);
            }

            if (target instanceof EntityLivingBase) {
                EnchantmentHelper.applyThornEnchantments((EntityLivingBase) target, this);
            }

            EnchantmentHelper.applyArthropodEnchantments(this, target);
        }

        return flag;
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float strength) {
        if (!this.executer.attackEntity(this, target)) {
            return;
        }

        if (!this.isUsingItem()) {
            ItemStack item = this.getHeldItem();
            if (!item.isEmpty() && item.getItem() instanceof ItemGun) {
                item.useItemRightClick(this.world, this.playerDummy, EnumHand.MAIN_HAND);
                this.setUseItem(true);
            }
        }
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (!this.world.isRemote) {
            player.openGui(RTMCore.instance, RTMCore.guiIdNPC, this.world, this.getEntityId(), 0, 0);
        }
        return true;
    }

    public boolean isUsingItem() {
        return this.getDataManager().get(USING_ITEM) == 1;
    }

    public void setUseItem(boolean par1) {
        byte value = (byte) (par1 ? 1 : 0);
        this.getDataManager().set(USING_ITEM, Byte.valueOf(value));
    }

    public int getItemUseCount() {
        return this.useItemCount;
    }

    public String getMenu() {
        return this.getDataManager().get(MENU);
    }

    public void setMenu(String s) {
        this.getDataManager().set(MENU, s);
    }

    @Override
    protected void damageArmor(float damage) {
        this.inventory.damageArmor(this, damage);
    }

    @Override
    public int getTotalArmorValue() {
        return this.inventory.getTotalArmorValue();
    }

    @Override
    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slot) {
        if (slot == EntityEquipmentSlot.MAINHAND) {
            return this.inventory.mainInventory[0];
        } else if (slot == EntityEquipmentSlot.OFFHAND) {
            return ItemStack.EMPTY;
        } else {
            return this.inventory.armorInventory[slot.getSlotIndex() - 1];
        }
    }

    @Override
    public Iterable<ItemStack> getHeldEquipment() {
        return Arrays.<ItemStack>asList(this.getHeldItem());
    }

    public ItemStack getHeldItem() {
        return this.inventory.mainInventory[0];
    }

    @Override
    public void setItemStackToSlot(EntityEquipmentSlot slot, ItemStack stack) {
        if (slot == EntityEquipmentSlot.MAINHAND) {
            this.inventory.mainInventory[0] = stack;
            ;
        } else if (slot == EntityEquipmentSlot.OFFHAND) {
            ;
        } else {
            this.inventory.armorInventory[slot.getSlotIndex() - 1] = stack;
        }
    }

    @Override
    public Iterable<ItemStack> getArmorInventoryList() {
        return Arrays.<ItemStack>asList(this.inventory.armorInventory);
    }

    @Override
    public String getName() {
        return this.getResourceState().getName();
    }

    @Override
    public void updateResourceState() {
        this.roleChanged = true;

        if (this.world == null || !this.world.isRemote) {
            PacketNBT.sendToClient(this);
        }
    }

    @Override
    public int[] getSelectorPos() {
        return new int[]{this.getEntityId(), -1, 0};
    }

    @Override
    public boolean closeGui(ResourceState par1) {
        return true;
    }

    public boolean isMotorman() {
        return false;
    }

    public void onInventoryChanged() {
        this.myRole.onInventoryChanged(this);

        NPCConfig cfg = this.getResourceState().getResourceSet().getConfig();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(cfg.health);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(cfg.speed);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(cfg.damage);
    }

    public EntityBullet getBullet(GunType type) {
        if (this.getAttackTarget() == null) {
            return new EntityBullet(this.world, this, type.speed, type.bulletType);
        }
        return new EntityBullet(this.world, this, this.getAttackTarget(), type.speed, type.bulletType);
    }

    @Override
    public ResourceState<ModelSetNPC> getResourceState() {
        return this.state;
    }

    @Override
    public void setSwingingArms(boolean swingingArms) {
    }

    @Override
    public void addEntityCrashInfo(CrashReportCategory category) {
        super.addEntityCrashInfo(category);
        EntityNPCKt.addEntityCrashInfo(this, category);
    }
}