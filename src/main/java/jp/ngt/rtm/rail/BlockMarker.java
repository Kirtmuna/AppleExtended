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

package jp.ngt.rtm.rail;

import com.anatawa12.fixRtm.asm.config.MainConfig;
import jp.apple.appleextended.rail.TileEntityLargeRailSectionCore;import jp.apple.appleextended.rail.util.RailChunkSectioner;import jp.apple.appleextended.rail.util.RailMapSection;import jp.apple.appleextended.rail.util.RailSection;import jp.ngt.ngtlib.block.BlockArgHolder;
import jp.ngt.ngtlib.block.BlockContainerCustomWithMeta;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.util.PermissionManager;
import jp.ngt.rtm.RTMBlock;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.RTMRail;
import jp.ngt.rtm.item.ItemRail;
import jp.ngt.rtm.modelpack.state.ResourceStateRail;
import jp.ngt.rtm.rail.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class BlockMarker extends BlockContainerCustomWithMeta {
    public final MarkerType markerType;

    public BlockMarker(MarkerType type) {
        super(Material.GLASS);
        this.markerType = type;
        this.setLightOpacity(1);
        this.setLightLevel(1.0F);
        this.setHardness(1.0F);
        this.setResistance(5.0F);
        this.setSoundType(SoundType.GLASS);
        this.setAABB(new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F));
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (MainConfig.mergeMarker) {
            items.add(new ItemStack(this, 1, 8));
            return;
        }
        switch (this.markerType) {
            case STANDARD:
                items.add(new ItemStack(this, 1, 0));
                items.add(new ItemStack(this, 1, 4));
                break;
            case SWITCH:
                items.add(new ItemStack(this, 1, 0));
                items.add(new ItemStack(this, 1, 4));
                break;
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int par2) {
        return new TileEntityMarker();
    }

    public static int getFacing(EntityLivingBase placer, boolean isDiagonal) {
        return isDiagonal ? NGTMath.floor(NGTMath.normalizeAngle((double) placer.rotationYaw + 180.0D) / 90.0D) & 3 : NGTMath.floor(NGTMath.normalizeAngle((double) placer.rotationYaw + 180.0D) / 90.0D + 0.5D) & 3;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        int i = stack.getItemDamage();
        if (i == com.anatawa12.fixRtm.rtm.rail.BlockMarker.MERGED_DAMAGE) {
            com.anatawa12.fixRtm.rtm.rail.BlockMarker.onBlockPlacedBy(this, world, pos, state, placer, stack);
            return;
        }
        int j = getFacing(placer, i >= 4);
        int k = i / 4;
        BlockUtil.setBlock(world, pos, this, j + k * 4, 2);
    }

    @Override
    protected boolean onBlockActivated(BlockArgHolder holder, float hitX, float hitY, float hitZ) {
        World world = holder.getWorld();
        EntityPlayer player = holder.getPlayer();
        int x = holder.getBlockPos().getX();
        int y = holder.getBlockPos().getY();
        int z = holder.getBlockPos().getZ();
        ItemStack item = player.inventory.getCurrentItem();
        if (item != null) {
            TileEntity tile = world.getTileEntity(holder.getBlockPos());
            if (!(tile instanceof TileEntityMarker)) {
                return true;
            }

            if (item.getItem() == Item.getItemFromBlock(RTMBlock.marker) || item.getItem() == Item.getItemFromBlock(RTMBlock.markerSwitch)) {
                if (world.isRemote) {
                    player.openGui(RTMCore.instance, RTMCore.guiIdRailMarker, world, x, y, z);
                }
                return true;
            }
        }

        if (!world.isRemote) {
            if (this.onMarkerActivated(world, x, y, z, player, true)) {
                if (!player.capabilities.isCreativeMode) {
                    item.shrink(1);
                }
            }
        }

        return true;
    }

    public void makeRailMap(TileEntityMarker marker, int x, int y, int z) {
        this.onMarkerActivated(marker.getWorld(), x, y, z, null, false);
    }

    public boolean onMarkerActivated(World world, int x, int y, int z, EntityPlayer player, boolean makeRail) {
        ResourceStateRail resourcestaterail = this.hasRail(player, makeRail);
        if (resourcestaterail != null) {
            boolean flag = (player == null) || player.capabilities.isCreativeMode;
            List<RailPosition> list = this.searchAllMarker(world, x, y, z);
            for (RailPosition railposition : list) {
                railposition.addHeight((double) (resourcestaterail.blockHeight - 0.0625F));
            }
            return createRail(world, x, y, z, list, resourcestaterail, makeRail, flag, player);
        }
        return false;
    }

    private List<RailPosition> searchAllMarker(World world, int x, int y, int z) {
        int i = RTMCore.railGeneratingDistance;
        int k = RTMCore.railGeneratingHeight;
        int xMin = x - i;
        int xMax = x + i;
        int yMin = y - i;
        int yMax = y + i;
        int zMin = z - i;
        int zMax = z + i;

        List<RailPosition> list = world.loadedTileEntityList.stream()
                .filter(TileEntityMarker.class::isInstance)
                .map(TileEntityMarker.class::cast)
                .filter(tile -> xMin <= tile.getX() && tile.getX() <= xMax
                        && yMin <= tile.getY() && tile.getY() <= yMax
                        && zMin <= tile.getZ() && tile.getZ() <= zMax)
                .filter(tile -> Math.abs(tile.getY() - y) < k)
                .sorted(Comparator.<TileEntity>comparingInt(v -> v.getPos().getX())
                        .thenComparingInt(v -> v.getPos().getY())
                        .thenComparingInt(v -> v.getPos().getZ()))
                .map(TileEntityMarker::getMarkerRP)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        list.sort((arg0, arg1) -> {
            if (arg0.switchType != arg1.switchType) {
                return arg1.switchType - arg0.switchType;
            } else {
                return arg0.blockY != arg1.blockY ? arg0.blockY - arg1.blockY : arg0.hashCode() - arg1.hashCode();
            }
        });
        return list;
    }

    public static boolean createRail(World world, int x, int y, int z, List<RailPosition> rps, ResourceStateRail state, boolean makeRail, boolean isCreative) {
        return createRail(world, x, y, z, rps, state, makeRail, isCreative, null);
    }

    private static boolean createRail(World world, int x, int y, int z, List<RailPosition> rps, ResourceStateRail state, boolean makeRail, boolean isCreative, EntityPlayer player) {
        if (rps.size() == 1) {
            RailPosition railposition = rps.get(0);
            if (railposition.hasScript()) {
                createCustomRail(world, railposition, state, makeRail, isCreative);
            }
        } else if (rps.size() == 2) {
            RailPosition railposition1 = rps.get(0);
            RailPosition railposition4 = rps.get(1);
            if (railposition1.switchType == 1 && railposition4.switchType == 1) {
                createTurntable(world, railposition1, railposition4, state, makeRail, isCreative);
            } else {
                RailPosition railposition2 = railposition1.blockY >= railposition4.blockY ? railposition4 : railposition1;
                RailPosition railposition3 = railposition1.blockY >= railposition4.blockY ? railposition1 : railposition4;
                createNormalRail(world, railposition2, railposition3, state, makeRail, isCreative);
            }
        } else if (rps.size() > 2) {
            createSwitchRail(world, x, y, z, rps, state, makeRail, isCreative, player);
        }

        return false;
    }

    private static boolean createNormalRail(World world, RailPosition start, RailPosition end, ResourceStateRail prop, boolean makeRail, boolean isCreative) {
        RailMap railmap = new RailMapBasic(start, end, RailMapBasic.fixRTMRailMapVersionCurrent);
        if (makeRail && railmap.canPlaceRail(world, isCreative, prop)) {
            if (prop.autoSplit) {
                List<RailSection> sections = RailChunkSectioner.split((RailMapBasic) railmap);
                if (sections.size() > 1) {
                    return createSectionedRail(world, (RailMapBasic) railmap, sections, prop, isCreative);
                }
            }

            railmap.setRail(world, RTMRail.largeRailBase, start.blockX, start.blockY, start.blockZ, prop);
            BlockUtil.setBlock(world, start.blockX, start.blockY, start.blockZ, RTMRail.largeRailCore, 0, 3);
            TileEntityLargeRailCore tileentitylargerailcore = (TileEntityLargeRailCore) BlockUtil.getTileEntity(world, start.blockX, start.blockY, start.blockZ);
            tileentitylargerailcore.setRailPositions(new RailPosition[]{start, end});
            tileentitylargerailcore.getResourceState().readFromNBT(prop.writeToNBT());
            tileentitylargerailcore.setStartPoint(start.blockX, start.blockY, start.blockZ);
            tileentitylargerailcore.fixRTMRailMapVersion = ((RailMapBasic) railmap).fixRTMRailMapVersion;
            tileentitylargerailcore.createRailMap();
            tileentitylargerailcore.sendPacket();
            if (BlockUtil.getBlock(world, end.blockX, end.blockY, end.blockZ) instanceof BlockMarker) {
                BlockUtil.setAir(world, end.blockX, end.blockY, end.blockZ);
            }

            return true;
        } else {
            TileEntity tileentity = BlockUtil.getTileEntity(world, start.blockX, start.blockY, start.blockZ);
            if (tileentity instanceof TileEntityMarker) {
                List<BlockPos> list = new ArrayList();
                list.add(new BlockPos(start.blockX, start.blockY, start.blockZ));
                list.add(new BlockPos(end.blockX, end.blockY, end.blockZ));
                ((TileEntityMarker) tileentity).setMarkersPos(list);
            }

            return false;
        }
    }

    private static boolean createCustomRail(World world, RailPosition rp, ResourceStateRail prop, boolean makeRail, boolean isCreative) {
        RailMap railMap = new RailMapCustom(rp, rp.scriptName, rp.scriptArgs);
        if (makeRail && railMap.canPlaceRail(world, isCreative, prop)) {
            railMap.setRail(world, RTMRail.largeRailBase, rp.blockX, rp.blockY, rp.blockZ, prop);

            BlockUtil.setBlock(world, rp.blockX, rp.blockY, rp.blockZ, RTMRail.largeRailCore, 0, 3);
            TileEntityLargeRailCore tile = (TileEntityLargeRailCore) BlockUtil.getTileEntity(world, rp.blockX, rp.blockY, rp.blockZ);
            tile.setRailPositions(new RailPosition[]{rp, railMap.getEndRP()});
            tile.getResourceState().readFromNBT(prop.writeToNBT());
            tile.setStartPoint(rp.blockX, rp.blockY, rp.blockZ);

            tile.createRailMap();
            tile.sendPacket();

            return true;
        } else {
            TileEntity tile = BlockUtil.getTileEntity(world, rp.blockX, rp.blockY, rp.blockZ);
            if (tile instanceof TileEntityMarker) {
                List<BlockPos> list = new ArrayList<>();
                list.add(new BlockPos(rp.blockX, rp.blockY, rp.blockZ));
                ((TileEntityMarker) tile).setMarkersPos(list);
            }
            return false;
        }
    }

    private static boolean createSwitchRail(World world, int x, int y, int z, List<RailPosition> list, ResourceStateRail prop, boolean makeRail, boolean isCreative, EntityPlayer player) {
        RailMaker railmaker = new RailMaker(world, list, RailMapBasic.fixRTMRailMapVersionCurrent);
        SwitchType switchtype = railmaker.getSwitch();
        if (switchtype == null) {
            if (world != null && !world.isRemote)
                player.sendMessage(new TextComponentTranslation("message.rail.switch_type", list.get(0).blockX, list.get(0).blockY, list.get(0).blockZ));
            return false;
        } else {
            RailMap[] arailmap = switchtype.getAllRailMap();
            if (arailmap == null) {
                return false;
            } else {
                boolean flag = false;

                for (RailMap railmap : arailmap) {
                    if (!railmap.canPlaceRail(world, isCreative, prop)) {
                        flag = true;
                    }
                }

                if (makeRail && !flag) {
                    RailPosition railposition = (RailPosition) list.get(0);
                    x = railposition.blockX;
                    y = railposition.blockY;
                    z = railposition.blockZ;

                    for (RailMap railmap1 : arailmap) {
                        railmap1.setRail(world, RTMRail.largeRailBase, x, y, z, prop);
                    }

                    for (RailPosition railposition1 : list) {
                        BlockUtil.setBlock(world, railposition1.blockX, railposition1.blockY, railposition1.blockZ, RTMRail.largeRailSwitchBase, 0, 3);
                        TileEntityLargeRailSwitchBase tileentitylargerailswitchbase = (TileEntityLargeRailSwitchBase) BlockUtil.getTileEntity(world, railposition1.blockX, railposition1.blockY, railposition1.blockZ);
                        tileentitylargerailswitchbase.setStartPoint(x, y, z);
                    }

                    BlockUtil.setBlock(world, x, y, z, RTMRail.largeRailSwitchCore, 0, 3);
                    TileEntityLargeRailSwitchCore tileentitylargerailswitchcore = (TileEntityLargeRailSwitchCore) BlockUtil.getTileEntity(world, x, y, z);
                    tileentitylargerailswitchcore.fixRTMRailMapVersion = railmaker.fixRTMRailMapVersion;
                    tileentitylargerailswitchcore.setRailPositions(list.toArray(new RailPosition[list.size()]));
                    tileentitylargerailswitchcore.getResourceState().readFromNBT(prop.writeToNBT());
                    tileentitylargerailswitchcore.setStartPoint(x, y, z);
                    tileentitylargerailswitchcore.createRailMap();
                    tileentitylargerailswitchcore.sendPacket();
                    return true;
                } else {
                    TileEntity tileentity = BlockUtil.getTileEntity(world, x, y, z);
                    if (tileentity instanceof TileEntityMarker) {
                        List<BlockPos> list1 = new ArrayList();

                        for (int i = 0; i < list.size(); ++i) {
                            RailPosition railposition2 = list.get(i);
                            list1.add(new BlockPos(railposition2.blockX, railposition2.blockY, railposition2.blockZ));
                        }

                        ((TileEntityMarker) tileentity).setMarkersPos(list1);
                    }

                    return false;
                }
            }
        }
    }

    private static boolean createTurntable(World world, RailPosition start, RailPosition end, ResourceStateRail prop, boolean makeRail, boolean isCreative) {
        int i = 0;
        int j = start.blockY;
        int k = 0;
        int l = 0;

        if (start.blockX == end.blockX && (start.blockZ - end.blockZ) % 2 == 0) {
            i = start.blockX;
            k = (start.blockZ + end.blockZ) / 2;
            l = Math.abs(start.blockZ - end.blockZ) / 2;
        }

        if (start.blockZ == end.blockZ && (start.blockX - end.blockX) % 2 == 0) {
            i = (start.blockX + end.blockX) / 2;
            k = start.blockZ;
            l = Math.abs(start.blockX - end.blockX) / 2;
        }

        if (l == 0) {
            return false;
        } else {
            RailMapTurntable railmapturntable = new RailMapTurntable(start, end, i, j, k, l, RailMapTurntable.fixRTMRailMapVersionCurrent);
            if (makeRail && railmapturntable.canPlaceRail(world, isCreative, prop)) {
                railmapturntable.setRail(world, RTMRail.largeRailBase, i, j, k, prop);
                BlockUtil.setBlock(world, i, j, k, RTMRail.TURNTABLE_CORE, 0, 3);
                TileEntityTurnTableCore tileentityturntablecore = (TileEntityTurnTableCore) BlockUtil.getTileEntity(world, i, j, k);
                tileentityturntablecore.setRailPositions(new RailPosition[]{start, end});
                tileentityturntablecore.getResourceState().readFromNBT(prop.writeToNBT());
                tileentityturntablecore.setStartPoint(i, j, k);
                tileentityturntablecore.createRailMap();
                tileentityturntablecore.sendPacket();
                return true;
            }

            return false;
        }
    }

    private static boolean createSectionedRail(World world, RailMapBasic source, List<RailSection> sections,
                                               ResourceStateRail prop, boolean isCreative) {
        UUID groupId = UUID.randomUUID();
        RailPosition logicalStart = copyRailPosition(source.getStartRP());
        RailPosition logicalEnd = copyRailPosition(source.getEndRP());
        RailPosition[] logicalPositions = new RailPosition[]{logicalStart, logicalEnd};

        List<int[]> corePositions = new ArrayList<>();
        for (RailSection section : sections) {
            corePositions.add(new int[]{section.getStartRP().blockX, section.getStartRP().blockY, section.getStartRP().blockZ});
        }
        
        for (RailSection section : sections) {
            RailMapSection sectionMap = new RailMapSection(source, section.getStartRP(), section.getEndRP(),
                    section.getStartRatio(), section.getEndRatio());
            if (!sectionMap.canPlaceRail(world, isCreative, prop)) {
                return false;
            }
        }
        
        source.prepareBaseBlocks(world, logicalStart.blockX, logicalStart.blockY, logicalStart.blockZ);
        
        for (RailSection section : sections) {
            RailMapSection sectionMap = new RailMapSection(source, section.getStartRP(), section.getEndRP(),
                    section.getStartRatio(), section.getEndRatio());
            int[] corePos = new int[]{section.getStartRP().blockX, section.getStartRP().blockY, section.getStartRP().blockZ};
            sectionMap.placeRailBlocks(world, RTMRail.largeRailBase, corePos[0], corePos[1], corePos[2], prop);
        }
        
        for (RailSection section : sections) {
            RailPosition sectionStart = copyRailPosition(section.getStartRP());
            RailPosition sectionEnd = copyRailPosition(section.getEndRP());
            int coreX = sectionStart.blockX;
            int coreY = sectionStart.blockY;
            int coreZ = sectionStart.blockZ;

            BlockUtil.setBlock(world, coreX, coreY, coreZ, RTMRail.largeRailCore, 1, 3);
            TileEntity tileEntity = BlockUtil.getTileEntity(world, coreX, coreY, coreZ);
            if (!(tileEntity instanceof TileEntityLargeRailSectionCore)) {
                return false;
            }

            TileEntityLargeRailSectionCore tile = (TileEntityLargeRailSectionCore) tileEntity;
            tile.configureRailSection(groupId, logicalPositions, new RailPosition[]{sectionStart, sectionEnd},
                    section.getStartRatio(), section.getEndRatio(), corePositions);
            tile.getResourceState().readFromNBT(prop.writeToNBT());
            tile.setStartPoint(coreX, coreY, coreZ);
            tile.fixRTMRailMapVersion = source.fixRTMRailMapVersion;
            tile.createRailMap();
            tile.sendPacket();
        }

        if (BlockUtil.getBlock(world, logicalEnd.blockX, logicalEnd.blockY, logicalEnd.blockZ) instanceof BlockMarker) {
            BlockUtil.setAir(world, logicalEnd.blockX, logicalEnd.blockY, logicalEnd.blockZ);
        }
        return true;
    }

    private static RailPosition copyRailPosition(RailPosition source) {
        return RailPosition.readFromNBT(source.writeToNBT());
    }

    public static byte getMarkerDir(Block block, int meta) {
        int i0 = meta & 3;
        int i1 = ((6 - i0) & 3) * 2;
        if ((block == RTMBlock.marker || block == RTMBlock.markerSwitch) && meta >= 4) {
            i1 = (i1 + 7) & 7;
        }
        return (byte) i1;
    }

    private RailPosition getRailPosition(World world, int x, int y, int z) {
        TileEntity tile = BlockUtil.getTileEntity(world, x, y, z);
        if (tile instanceof TileEntityMarker) {
            return ((TileEntityMarker) tile).getMarkerRP();
        }
        return null;
    }

    public ResourceStateRail hasRail(@Nullable EntityPlayer player, boolean par2) {
        if (player == null) {
            return ItemRail.getDefaultProperty();
        } else if (PermissionManager.INSTANCE.hasPermission(player, RTMCore.EDIT_RAIL)) {
            ItemStack item = player.inventory.getCurrentItem();
            if (item.getItem() == RTMItem.itemLargeRail) {
                return ((ItemRail) RTMItem.itemLargeRail).getModelState(item);
            }

            if (player.capabilities.isCreativeMode || !par2) {
                return ItemRail.getDefaultProperty();
            }
        }
        return null;
    }

    @Override
    protected ItemStack getItem(int damage) {
        if (MainConfig.mergeMarker) {
            return new ItemStack(this, 1, 8);
        } else {
            return new ItemStack(this, 1, damage & 4);
        }
    }

    public enum MarkerType {
        STANDARD(0xFF0000),
        SWITCH(0x0000FF);

        public final int color;

        private MarkerType(int par1) {
            this.color = par1;
        }
    }
}