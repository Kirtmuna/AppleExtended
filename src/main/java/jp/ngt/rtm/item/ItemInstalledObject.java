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

package jp.ngt.rtm.item;

import com.anatawa12.fixRtm.rtm.item.ItemWithModelEx;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.item.ItemArgHolderBase.ItemArgHolder;
import jp.ngt.ngtlib.item.SerializableItemType;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.rtm.RTMBlock;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.block.tileentity.*;
import jp.ngt.rtm.electric.Connection.ConnectionType;
import jp.ngt.rtm.electric.*;
import jp.ngt.rtm.entity.EntityATC;
import jp.ngt.rtm.entity.EntityBumpingPost;
import jp.ngt.rtm.entity.EntityInstalledObject;
import jp.ngt.rtm.entity.EntityTrainDetector;
import jp.ngt.rtm.gui.GuiSelectTexture;
import jp.ngt.rtm.gui.GuiSignboard;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.modelset.ModelSetMachine;
import jp.ngt.rtm.modelpack.state.ResourceState;
import jp.ngt.rtm.modelpack.state.ResourceStateSignboard;
import jp.ngt.rtm.rail.TileEntityLargeRailBase;
import jp.ngt.rtm.rail.util.RailMap;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemInstalledObject extends ItemWithModel {
    public ItemInstalledObject() {
        super();
        this.setHasSubtypes(true);
    }

    @Override
    protected ActionResult<ItemStack> onItemUse(ItemArgHolder holder, float hitX, float hitY, float hitZ) {
        ItemStack itemstack = holder.getItemStack();
        World world = holder.getWorld();
        EntityPlayer entityplayer = holder.getPlayer();
        BlockPos blockpos = holder.getBlockPos();
        EnumFacing enumfacing = holder.getFacing();

        if (!world.isRemote) {
            BlockPos blockpos1 = blockpos;
            if (!world.getBlockState(blockpos).getBlock().isReplaceable(world, blockpos)) {
                blockpos1 = blockpos.offset(enumfacing);
                if (!world.isAirBlock(blockpos1)) {
                    return holder.success();
                }
            }
            int i = blockpos1.getX();
            int j = blockpos1.getY();
            int k = blockpos1.getZ();
            int i1 = itemstack.getItemDamage();
            int sideIndex = enumfacing.getIndex();
            Block block = null;
            IstlObjType iteminstalledobject$istlobjtype = IstlObjType.getType(i1);

            if (iteminstalledobject$istlobjtype == IstlObjType.FLUORESCENT) {
                int l = NGTMath.floor((double) (entityplayer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
                if (entityplayer.canPlayerEdit(blockpos1, enumfacing, itemstack)) {
                    block = RTMBlock.fluorescent;
                    BlockUtil.setBlock(world, i, j, k, block, i1, 3);
                    byte b0 = 0;
                    switch (sideIndex) {
                        case 0:
                            if (l == 0 || l == 2) {
                                b0 = 0;
                            } else if (l == 1 || l == 3) {
                                b0 = 4;
                            }
                            break;
                        case 1:
                            if (l == 0 || l == 2) {
                                b0 = 2;
                            } else if (l == 1 || l == 3) {
                                b0 = 6;
                            }
                            break;
                        case 2:
                            b0 = 1;
                            break;
                        case 3:
                            b0 = 3;
                            break;
                        case 4:
                            b0 = 5;
                            break;
                        case 5:
                            b0 = 7;
                            break;
                    }
                    TileEntityFluorescent tileentityfluorescent = (TileEntityFluorescent) BlockUtil.getTileEntity(world, i, j, k);
                    tileentityfluorescent.setDir(b0);
                    tileentityfluorescent.setRotation(entityplayer, entityplayer.isSneaking() ? 1.0F : 15.0F, true);
                    ItemWithModelEx.applyOffsetToTileEntity(itemstack, tileentityfluorescent);
                    this.updateResource(tileentityfluorescent, itemstack);
                }
            } else if (iteminstalledobject$istlobjtype == IstlObjType.CROSSING) {
                if (enumfacing == EnumFacing.UP) {
                    block = RTMBlock.crossingGate;
                    BlockUtil.setBlock(world, i, j, k, block, 0, 3);
                    TileEntityCrossingGate tileentitycrossinggate = (TileEntityCrossingGate) BlockUtil.getTileEntity(world, i, j, k);
                    tileentitycrossinggate.setRotation(entityplayer, 15.0F, true);
                    ItemWithModelEx.applyOffsetToTileEntity(itemstack, tileentitycrossinggate);
                    this.updateResource(tileentitycrossinggate, itemstack);
                }
            } else if (iteminstalledobject$istlobjtype == IstlObjType.RAILLOAD_SIGN) {
                if (enumfacing == EnumFacing.UP || enumfacing == EnumFacing.DOWN) {
                    block = RTMBlock.railroadSign;
                    BlockUtil.setBlock(world, i, j, k, block, 0, 3);
                    TileEntityRailroadSign tileentityrailroadsign = (TileEntityRailroadSign) BlockUtil.getTileEntity(world, i, j, k);
                    tileentityrailroadsign.setRotation(entityplayer, 15.0F, true);
                    ItemWithModelEx.applyOffsetToTileEntity(itemstack, tileentityrailroadsign);
                    this.updateResource(tileentityrailroadsign, itemstack);
                }
            } else if (iteminstalledobject$istlobjtype == IstlObjType.SIGNAL) {
                if (enumfacing != EnumFacing.UP && enumfacing != EnumFacing.DOWN) {
                    i = blockpos.getX();
                    j = blockpos.getY();
                    k = blockpos.getZ();
                    IBlockState state = world.getBlockState(blockpos);
                    Block target = state.getBlock();
                    if (target != RTMBlock.signal) {
                        TileEntity origTile = null;
                        if (target.hasTileEntity(state)) {
                            origTile = world.getTileEntity(blockpos);
                        }

                        int meta2 = BlockUtil.getMetadata(world, blockpos);
                        BlockUtil.setBlock(world, i, j, k, RTMBlock.signal, meta2, 3);

                        TileEntity tile = BlockUtil.getTileEntity(world, i, j, k);
                        if (tile instanceof TileEntitySignal) {
                            TileEntitySignal teSignal = ((TileEntitySignal) tile);
                            int dir = sideIndex == 2 ? 2 : (sideIndex == 4 ? 3 : (sideIndex == 3 ? 0 : 1));
                            teSignal.setSignalProperty(this.getModelState(itemstack).getResourceName(), target, dir, entityplayer, origTile);
                            block = RTMBlock.signal;
                        }
                    }
                }
            } else if (iteminstalledobject$istlobjtype == IstlObjType.TURNSTILE) {
                block = RTMBlock.turnstile;
                int l1 = (NGTMath.floor((NGTMath.normalizeAngle(entityplayer.rotationYaw + 180.0D) / 90.0D) + 0.5D) & 3);
                BlockUtil.setBlock(world, i, j, k, block, l1, 3);
                TileEntityTurnstile tileentityturnstile = (TileEntityTurnstile) BlockUtil.getTileEntity(world, i, j, k);
                tileentityturnstile.setRotation(entityplayer, 90.0F, true);
                ItemWithModelEx.applyOffsetToTileEntity(itemstack, tileentityturnstile);
                this.updateResource(tileentityturnstile, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.BUMPING_POST) {
                if (enumfacing == EnumFacing.UP && this.setEntityOnRail(world, new EntityBumpingPost(world), i, j - 1, k, entityplayer, itemstack)) {
                    block = Blocks.STONE;
                }
            } else if (iteminstalledobject$istlobjtype == IstlObjType.LINEPOLE) {
                block = RTMBlock.linePole;
                BlockUtil.setBlock(world, i, j, k, block, 0, 3);
                TileEntityPole tileentitypole = (TileEntityPole) BlockUtil.getTileEntity(world, i, j, k);
                ItemWithModelEx.applyOffsetToTileEntity(itemstack, tileentitypole);
                this.updateResource(tileentitypole, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.STAIR) {
                block = RTMBlock.scaffoldStairs;
                BlockUtil.setBlock(world, i, j, k, block, 0, 3);
                block.onBlockPlacedBy(world, blockpos1, null, entityplayer, null);
                TileEntityScaffoldStairs tile = (TileEntityScaffoldStairs) BlockUtil.getTileEntity(world, i, j, k);
                this.updateResource(tile, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.SCAFFOLD) {
                block = RTMBlock.scaffold;
                BlockUtil.setBlock(world, i, j, k, block, 0, 3);
                block.onBlockPlacedBy(world, blockpos1, null, entityplayer, null);
                TileEntityScaffold tile = (TileEntityScaffold) BlockUtil.getTileEntity(world, i, j, k);
                this.updateResource(tile, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.POINT) {
                if (enumfacing == EnumFacing.UP) {
                    block = RTMBlock.point;
                    BlockUtil.setBlock(world, i, j, k, block, 0, 3);
                    TileEntityPoint tileentitypoint = (TileEntityPoint) BlockUtil.getTileEntity(world, i, j, k);
                    tileentitypoint.setRotation(entityplayer, 15.0F, false);
                    ItemWithModelEx.applyOffsetToTileEntity(itemstack, tileentitypoint);
                    this.updateResource(tileentitypoint, itemstack);
                }
            } else if (iteminstalledobject$istlobjtype == IstlObjType.SIGNBOARD) {
                block = RTMBlock.signboard;
                BlockUtil.setBlock(world, i, j, k, block, sideIndex, 3);
                TileEntitySignBoard tileentitysignboard = (TileEntitySignBoard) BlockUtil.getTileEntity(world, i, j, k);
                int j2 = NGTMath.floor(NGTMath.normalizeAngle((double) entityplayer.rotationYaw + 180.0D) / 90.0D + 0.5D) & 3;
                tileentitysignboard.setDirection((byte) j2);
                ItemWithModelEx.applyOffsetToTileEntity(itemstack, tileentitysignboard);
                this.updateResource(tileentitysignboard, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.TICKET_VENDOR) {
                block = RTMBlock.ticketVendor;
                BlockUtil.setBlock(world, i, j, k, block, 0, 3);
                TileEntityTicketVendor tileentityticketvendor = (TileEntityTicketVendor) BlockUtil.getTileEntity(world, i, j, k);
                tileentityticketvendor.setRotation(entityplayer, 15.0F, true);
                this.updateResource(tileentityticketvendor, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.LIGHT) {
                block = RTMBlock.light;
                BlockUtil.setBlock(world, i, j, k, block, sideIndex, 3);
                TileEntityLight tileentitylight = (TileEntityLight) BlockUtil.getTileEntity(world, i, j, k);
                tileentitylight.setRotation(entityplayer, 15.0F, true);
                ItemWithModelEx.applyOffsetToTileEntity(itemstack, tileentitylight);
                this.updateResource(tileentitylight, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.FLAG) {
                block = RTMBlock.flag;
                BlockUtil.setBlock(world, i, j, k, block, 0, 3);
                TileEntityFlag tileentityflag = (TileEntityFlag) BlockUtil.getTileEntity(world, i, j, k);
                tileentityflag.setRotation(entityplayer, 15.0F, true);
                this.updateResource(tileentityflag, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.ATC) {
                if (enumfacing == EnumFacing.UP && this.setEntityOnRail(world, new EntityATC(world), i, j - 1, k, entityplayer, itemstack)) {
                    block = Blocks.STONE;
                }
            } else if (iteminstalledobject$istlobjtype == IstlObjType.TRAIN_DETECTOR) {
                if (enumfacing == EnumFacing.UP && this.setEntityOnRail(world, new EntityTrainDetector(world), i, j - 1, k, entityplayer, itemstack)) {
                    block = Blocks.STONE;
                }
            } else if (iteminstalledobject$istlobjtype == IstlObjType.INSULATOR) {
                block = RTMBlock.insulator;
                BlockUtil.setBlock(world, i, j, k, block, sideIndex, 3);
                TileEntityInsulator tileentityinsulator = (TileEntityInsulator) BlockUtil.getTileEntity(world, i, j, k);
                ItemWithModelEx.applyOffsetToTileEntity(itemstack, tileentityinsulator);
                this.updateResource(tileentityinsulator, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.CONNECTOR_IN || iteminstalledobject$istlobjtype == IstlObjType.CONNECTOR_OUT) {
                Block target = BlockUtil.getBlock(world, blockpos);
                if (target instanceof IBlockConnective && ((IBlockConnective) target).canConnect(world, i, j, k)) {
                    if (iteminstalledobject$istlobjtype == IstlObjType.CONNECTOR_OUT) {
                        sideIndex += 6;
                    }
                    block = RTMBlock.connector;
                    BlockUtil.setBlock(world, i, j, k, block, sideIndex, 3);
                    TileEntityConnector tileentityconnector = (TileEntityConnector) BlockUtil.getTileEntity(world, i, j, k);
                    ItemWithModelEx.applyOffsetToTileEntity(itemstack, tileentityconnector);
                    this.updateResource(tileentityconnector, itemstack);
                    tileentityconnector.setConnectionTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), ConnectionType.DIRECT, null);
                }
            } else if (iteminstalledobject$istlobjtype == IstlObjType.PIPE) {
                block = RTMBlock.pipe;
                BlockUtil.setBlock(world, i, j, k, block, 0, 3);
                TileEntityPipe tileentitypipe = (TileEntityPipe) BlockUtil.getTileEntity(world, i, j, k);
                tileentitypipe.setAttachedSide((byte) enumfacing.getIndex());
                tileentitypipe.refresh();
                world.notifyNeighborsOfStateChange(new BlockPos(i, j, k), block, true);
                this.updateResource(tileentitypipe, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.PLANT) {
                block = RTMBlock.plant_ornament;
                BlockUtil.setBlock(world, i, j, k, block, 0, 3);
                TileEntityPlantOrnament tileentityplantornament = (TileEntityPlantOrnament) BlockUtil.getTileEntity(world, i, j, k);
                tileentityplantornament.setRotation(entityplayer, entityplayer.isSneaking() ? 1.0F : 15.0F, true);
                ItemWithModelEx.applyOffsetToTileEntity(itemstack, tileentityplantornament);
                this.updateResource(tileentityplantornament, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.SPEAKER) {
                block = RTMBlock.speaker;
                BlockUtil.setBlock(world, i, j, k, block, sideIndex, 3);
                TileEntitySpeaker tileentityspeaker = (TileEntitySpeaker) BlockUtil.getTileEntity(world, i, j, k);
                tileentityspeaker.setRotation(entityplayer, 15.0F, true);
                this.updateResource(tileentityspeaker, itemstack);
            } else if (iteminstalledobject$istlobjtype == IstlObjType.MECHANISM) {
                block = RTMBlock.mechanism;
                BlockUtil.setBlock(world, i, j, k, block, 0, 3);
                TileEntityMechanism tile = (TileEntityMechanism) BlockUtil.getTileEntity(world, i, j, k);
                tile.setSide(sideIndex);
                this.updateResource(tile, itemstack);
            }

            if (block != null) {
                SoundType soundtype = block.getSoundType(world.getBlockState(blockpos), world, blockpos, entityplayer);
                RTMCore.proxy.playSound(entityplayer, "block.stone.place", (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                itemstack.shrink(1);
            }
        }
        return holder.success();
    }

    private void updateResource(final IResourceSelector selector, ItemStack stack) {
        selector.getResourceState().readFromNBT(this.getModelState(stack).writeToNBT());
        selector.updateResourceState();
    }

    private boolean setEntityOnRail(World world, EntityInstalledObject entity, int x, int y, int z, EntityPlayer player, ItemStack stack) {
        RailMap rm0 = TileEntityLargeRailBase.getRailMapFromCoordinates(world, null, x, y, z);
        if (rm0 == null) {
            return false;
        }

        int split = 128;
        int i0 = rm0.getNearlestPoint(split, (double) x + 0.5D, (double) z + 0.5D);
        double posX = rm0.getRailPos(split, i0)[1];
        double posY = rm0.getRailHeight(split, i0) + 0.0625D;
        double posZ = rm0.getRailPos(split, i0)[0];
        float yaw = rm0.getRailRotation(split, i0);
        float yaw2 = -player.rotationYaw + 180.0F;
        float dif = NGTMath.wrapAngle(yaw - yaw2);
        boolean invert = false;
        if (Math.abs(dif) > 90.0F) {
            yaw += 180.0F;
            invert = true;
        }

        ResourceState<ModelSetMachine> itemState = this.getModelState(stack);
        entity.setPosition(posX, posY, posZ);
        entity.rotationYaw = yaw;
        entity.rotationPitch = -rm0.getRailPitch(split, i0) * (invert ? -1.0F : 1.0F);
        entity.rotationRoll = rm0.getCant(split, i0) * (invert ? -1.0F : 1.0F);
        world.spawnEntity(entity);
        entity.getResourceState().readFromNBT(itemState.writeToNBT());
        entity.updateResourceState();
        return true;
    }

    @Override
    public String getUnlocalizedName(ItemStack itemStack) {
        return super.getUnlocalizedName() + "." + itemStack.getItemDamage();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
        if (!this.isInCreativeTab(tab)) {
            return;
        }

        for (IstlObjType type : IstlObjType.values()) {
            if (type == IstlObjType.NONE) {
                continue;
            }
            list.add(new ItemStack(this, 1, type.id));
        }
    }

    @Override
    protected ResourceType getModelType(ItemStack itemStack) {
        return IstlObjType.getType(itemStack.getItemDamage()).type;
    }

    public enum IstlObjType implements SerializableItemType {
        FLUORESCENT(0, RTMResource.ORNAMENT_LAMP),
        PLANT(1, RTMResource.ORNAMENT_PLANT),
        INSULATOR(3, RTMResource.CONNECTOR_RELAY),
        PIPE(4, RTMResource.ORNAMENT_PIPE),
        CROSSING(5, RTMResource.MACHINE_GATE),
        RAILLOAD_SIGN(6, RTMResource.RRS),
        SIGNAL(7, RTMResource.SIGNAL),
        CONNECTOR_IN(8, RTMResource.CONNECTOR_INPUT),
        CONNECTOR_OUT(9, RTMResource.CONNECTOR_OUTPUT),
        ATC(10, RTMResource.MACHINE_ANTENNA_SEND),
        TRAIN_DETECTOR(11, RTMResource.MACHINE_ANTENNA_RECEIVE),
        TURNSTILE(12, RTMResource.MACHINE_TURNSTILE),
        BUMPING_POST(13, RTMResource.MACHINE_BUMPINGPOST),
        LINEPOLE(14, RTMResource.ORNAMENT_POLE),
        POINT(16, RTMResource.MACHINE_POINT),
        SIGNBOARD(17, RTMResource.SIGNBOARD),
        TICKET_VENDOR(18, RTMResource.MACHINE_VENDOR),
        LIGHT(19, RTMResource.MACHINE_LIGHT),
        FLAG(20, RTMResource.FLAG),
        STAIR(21, RTMResource.ORNAMENT_STAIR),
        SCAFFOLD(22, RTMResource.ORNAMENT_SCAFFOLD),
        SPEAKER(23, RTMResource.MACHINE_SPEAKER),
        MECHANISM(24, RTMResource.MECHANISM),
        NONE(-1, null);

        public final byte id;
        public final ResourceType type;

        private IstlObjType(int par1, ResourceType par2) {
            this.id = (byte) par1;
            this.type = par2;
        }

        public static IstlObjType getType(int id) {
            for (IstlObjType type : IstlObjType.values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return NONE;
        }

        @Override
        public int getId() {
            return this.id;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen newGuiScreen(ItemArgHolder holder) {
        int i = holder.getItemStack().getItemDamage();
        IstlObjType iteminstalledobject$istlobjtype = IstlObjType.getType(i);
        if (iteminstalledobject$istlobjtype != IstlObjType.RAILLOAD_SIGN && iteminstalledobject$istlobjtype != IstlObjType.FLAG) {
            return iteminstalledobject$istlobjtype == IstlObjType.SIGNBOARD ? newGuiSignboard(holder) : newGuiSelectModel(holder);
        } else {
            return new GuiSelectTexture(new ResourceSelector(holder), null);
        }
    }

    @SideOnly(Side.CLIENT)
    private GuiScreen newGuiSignboard(ItemArgHolder holder) {
        return new GuiSignboard(new ResourceSelector(holder));
    }

    public int getGuiId(ItemStack stack) {
        int meta = stack.getItemDamage();
        IstlObjType type = IstlObjType.getType(meta);
        if (type == IstlObjType.RAILLOAD_SIGN || type == IstlObjType.FLAG) {
            return RTMCore.guiIdSelectItemTexture;
        } else if (type == IstlObjType.SIGNBOARD) {
            return RTMCore.guiIdSignboard;
        }
        return RTMCore.guiIdSelectItemModel;
    }

    @Override
    protected ResourceState getNewState(ItemStack itemStack, ResourceType type) {
        IstlObjType iteminstalledobject$istlobjtype = IstlObjType.getType(itemStack.getItemDamage());
        return (ResourceState) (iteminstalledobject$istlobjtype == IstlObjType.SIGNBOARD ? new ResourceStateSignboard(type, (Object) null) : new ResourceState(type, (Object) null));
    }
}