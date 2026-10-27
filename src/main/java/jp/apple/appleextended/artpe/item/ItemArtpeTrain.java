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

package jp.apple.appleextended.artpe.item;

import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.entity.train.*;
import jp.ngt.rtm.entity.train.util.Formation;
import jp.ngt.rtm.entity.train.util.FormationEntry;
import jp.ngt.rtm.entity.train.util.TrainState.TrainStateType;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetVehicleBase;
import jp.ngt.rtm.rail.TileEntityLargeRailBase;
import jp.ngt.rtm.rail.util.RailMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ItemArtpeTrain extends Item {
    private static final AtomicLong lastId = new AtomicLong(0);

    private static final int SEARCH_SPLIT = 2048;

    private static final int POS_SPLIT = 2048;

    public ItemArtpeTrain() {
        super();
        this.setUnlocalizedName("artpe_train");
        this.setRegistryName("artpe_train");
        this.setMaxStackSize(1);
    }

    public static List<TrainSet> getFormationFromItem(ItemStack stack) {
        List<TrainSet> list = new ArrayList<>();
        if (stack.hasTagCompound()) {
            NBTTagList tagList = stack.getTagCompound().getTagList("formations", 10);
            for (int i = 0; i < tagList.tagCount(); ++i) {
                list.add(TrainSet.readFromNBT(tagList.getCompoundTagAt(i)));
            }
        }
        return list;
    }

    public static void spawnFormation(World world, ItemStack stack, BlockPos pos, EntityPlayer player) {
        if (world.isRemote) return;

        ItemArtpeTrain inst = (ItemArtpeTrain) stack.getItem();
        RailMap rm0 = inst.findRailMap(world, player, pos.getX(), pos.getY(), pos.getZ());
        if (rm0 == null) return;

        List<TrainSet> trainSets = getFormationFromItem(stack);
        if (trainSets.isEmpty()) return;

        int startIndex = rm0.getNearlestPoint(SEARCH_SPLIT, pos.getX() + 0.5D, pos.getZ() + 0.5D);
        double startDist = rm0.getLength() * ((double) startIndex / SEARCH_SPLIT);

        float railYawAtStart = NGTMath.wrapAngle(rm0.getRailYaw(SEARCH_SPLIT, startIndex));
        float fixedYaw = EntityBogie.fixBogieYaw(-player.rotationYaw, railYawAtStart);
        boolean isReverse = Math.abs(NGTMath.wrapAngle(fixedYaw - railYawAtStart)) > 90.0F;
        double dirMul = isReverse ? -1.0D : 1.0D;

        long formationId = inst.getUniqueId();
        Formation formation = new Formation(formationId, trainSets.size());
        RailContext ctx = new RailContext(rm0, startDist);
        float prevYaw = fixedYaw;

        for (int i = 0; i < trainSets.size(); i++) {
            TrainSet set = trainSets.get(i);
            double offsetFromStart = set.posZ * dirMul;
            double targetDist = startDist + offsetFromStart;
            PosRotation pr = inst.resolvePos(world, player, ctx, targetDist, prevYaw, dirMul);
            prevYaw = pr.yaw;

            EntityTrainBase train = inst.createTrainEntity(world, set.modelName);
            int entryDir = set.dir;
            float finalYaw = pr.yaw + (entryDir == 1 ? 180.0F : 0.0F);

            train.setPositionAndRotation(pr.posX, pr.posY, pr.posZ, finalYaw, pr.pitch);
            train.rotationRoll = pr.roll;
            train.prevRotationRoll = pr.roll;

            world.spawnEntity(train);

            train.getResourceState().setResourceName(set.modelName);
            train.setTrainStateData_NoSync(TrainStateType.Role, (byte) 1);
            train.setTrainStateData_NoSync(TrainStateType.Notch, (byte) -8);
            train.setTrainStateData_NoSync(TrainStateType.Direction, (byte) entryDir);
            train.setTrainStateData_NoSync(TrainStateType.ChunkLoader, (byte) 1);
            train.setSpeed_NoSync(0.0F);

            train.prevPosX = train.lastTickPosX = train.posX;
            train.prevPosY = train.lastTickPosY = train.posY;
            train.prevPosZ = train.lastTickPosZ = train.posZ;

            if (train.existBogies()) {
                train.getBogie(0).isActivated = true;
                train.getBogie(1).isActivated = true;
            }

            FormationEntry entry = new FormationEntry(train, i, entryDir);
            formation.entries[i] = entry;
            train.setFormation(formation);
            train.updateResourceState();
        }

        try {
            Method realloc = Formation.class.getDeclaredMethod("reallocation");
            realloc.setAccessible(true);
            realloc.invoke(formation);

            Method sendPacket = Formation.class.getDeclaredMethod("sendPacket");
            sendPacket.setAccessible(true);
            sendPacket.invoke(formation);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!player.capabilities.isCreativeMode) stack.shrink(1);
    }

    public static void spawnFormation(World world, ItemStack stack, BlockPos railPos, float spawnYaw) {
        if (world.isRemote) return;

        ItemArtpeTrain inst = (ItemArtpeTrain) stack.getItem();

        RailMap rm0 = inst.findRailMap(world, null, railPos.getX(), railPos.getY(), railPos.getZ());
        if (rm0 == null) return;

        List<TrainSet> trainSets = getFormationFromItem(stack);
        if (trainSets.isEmpty()) return;

        int startIndex = rm0.getNearlestPoint(SEARCH_SPLIT, railPos.getX() + 0.5D, railPos.getZ() + 0.5D);
        double startDist = rm0.getLength() * ((double) startIndex / SEARCH_SPLIT);

        float railYawAtStart = NGTMath.wrapAngle(rm0.getRailYaw(SEARCH_SPLIT, startIndex));
        float fixedYaw = EntityBogie.fixBogieYaw(spawnYaw, railYawAtStart);

        boolean isReverse = Math.abs(NGTMath.wrapAngle(fixedYaw - railYawAtStart)) > 90.0F;
        double dirMul = isReverse ? -1.0D : 1.0D;

        long formationId = inst.getUniqueId();
        Formation formation = new Formation(formationId, trainSets.size());
        RailContext ctx = new RailContext(rm0, startDist);
        float prevYaw = fixedYaw;

        for (int i = 0; i < trainSets.size(); i++) {
            TrainSet set = trainSets.get(i);
            double offsetFromStart = set.posZ * dirMul;
            double targetDist = startDist + offsetFromStart;

            PosRotation pr = inst.resolvePos(world, null, ctx, targetDist, prevYaw, dirMul);
            prevYaw = pr.yaw;

            EntityTrainBase train = inst.createTrainEntity(world, set.modelName);
            int entryDir = set.dir;
            float finalYaw = pr.yaw + (entryDir == 1 ? 180.0F : 0.0F);

            train.setPositionAndRotation(pr.posX, pr.posY, pr.posZ, finalYaw, pr.pitch);
            train.rotationRoll = pr.roll;
            train.prevRotationRoll = pr.roll;

            world.spawnEntity(train);

            train.getResourceState().setResourceName(set.modelName);
            train.setTrainStateData_NoSync(TrainStateType.Role, (byte) 1);
            train.setTrainStateData_NoSync(TrainStateType.Notch, (byte) -8);
            train.setTrainStateData_NoSync(TrainStateType.Direction, (byte) entryDir);
            train.setTrainStateData_NoSync(TrainStateType.ChunkLoader, (byte) 1);
            train.setSpeed_NoSync(0.0F);

            train.prevPosX = train.lastTickPosX = train.posX;
            train.prevPosY = train.lastTickPosY = train.posY;
            train.prevPosZ = train.lastTickPosZ = train.posZ;

            if (train.existBogies()) {
                train.getBogie(0).isActivated = true;
                train.getBogie(1).isActivated = true;
            }

            FormationEntry entry = new FormationEntry(train, i, entryDir);
            formation.entries[i] = entry;
            train.setFormation(formation);
            train.updateResourceState();
        }

        try {
            Method realloc = Formation.class.getDeclaredMethod("reallocation");
            realloc.setAccessible(true);
            realloc.invoke(formation);

            Method sendPacket = Formation.class.getDeclaredMethod("sendPacket");
            sendPacket.setAccessible(true);
            sendPacket.invoke(formation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long getUniqueId() {
        return lastId.incrementAndGet() + System.currentTimeMillis();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack itemStack = player.getHeldItem(hand);
        if (world.isRemote) return EnumActionResult.SUCCESS;

        RailMap rm0 = findRailMap(world, player, pos.getX(), pos.getY(), pos.getZ());
        if (rm0 == null) return EnumActionResult.PASS;

        List<TrainSet> trainSets = getFormationFromItem(itemStack);
        if (trainSets.isEmpty()) return EnumActionResult.FAIL;

        int startIndex = rm0.getNearlestPoint(SEARCH_SPLIT, pos.getX() + 0.5D, pos.getZ() + 0.5D);
        double startDist = rm0.getLength() * ((double) startIndex / SEARCH_SPLIT);

        float railYawAtStart = NGTMath.wrapAngle(rm0.getRailYaw(SEARCH_SPLIT, startIndex));
        float fixedYaw = EntityBogie.fixBogieYaw(-player.rotationYaw, railYawAtStart);
        boolean isReverse = Math.abs(NGTMath.wrapAngle(fixedYaw - railYawAtStart)) > 90.0F;
        double dirMul = isReverse ? -1.0D : 1.0D;

        long formationId = getUniqueId();
        Formation formation = new Formation(formationId, trainSets.size());
        RailContext ctx = new RailContext(rm0, startDist);
        float prevYaw = fixedYaw;

        for (int i = 0; i < trainSets.size(); i++) {
            TrainSet set = trainSets.get(i);
            double offsetFromStart = set.posZ * dirMul;
            double targetDist = startDist + offsetFromStart;
            PosRotation pr = resolvePos(world, player, ctx, targetDist, prevYaw, dirMul);
            prevYaw = pr.yaw;

            EntityTrainBase train = createTrainEntity(world, set.modelName);
            int entryDir = set.dir;
            float finalYaw = pr.yaw + (entryDir == 1 ? 180.0F : 0.0F);

            train.setPositionAndRotation(pr.posX, pr.posY, pr.posZ, finalYaw, pr.pitch);
            train.rotationRoll = pr.roll;
            train.prevRotationRoll = pr.roll;

            world.spawnEntity(train);

            train.getResourceState().setResourceName(set.modelName);
            train.setTrainStateData_NoSync(TrainStateType.Role, (byte) 1);
            train.setTrainStateData_NoSync(TrainStateType.Notch, (byte) -8);
            train.setTrainStateData_NoSync(TrainStateType.Direction, (byte) entryDir);
            train.setTrainStateData_NoSync(TrainStateType.ChunkLoader, (byte) 1);
            train.setSpeed_NoSync(0.0F);

            train.prevPosX = train.lastTickPosX = train.posX;
            train.prevPosY = train.lastTickPosY = train.posY;
            train.prevPosZ = train.lastTickPosZ = train.posZ;

            if (train.existBogies()) {
                train.getBogie(0).isActivated = true;
                train.getBogie(1).isActivated = true;
            }

            FormationEntry entry = new FormationEntry(train, i, entryDir);
            formation.entries[i] = entry;
            train.setFormation(formation);
            train.updateResourceState();
        }

        try {
            Method realloc = Formation.class.getDeclaredMethod("reallocation");
            realloc.setAccessible(true);
            realloc.invoke(formation);

            Method sendPacket = Formation.class.getDeclaredMethod("sendPacket");
            sendPacket.setAccessible(true);
            sendPacket.invoke(formation);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!player.capabilities.isCreativeMode) itemStack.shrink(1);
        return EnumActionResult.SUCCESS;
    }

    public RailMap findRailMap(World world, EntityPlayer player, int bx, int by, int bz) {
        for (int dy = 0; dy >= -2; dy--) {
            RailMap rm = TileEntityLargeRailBase.getRailMapFromCoordinates(
                    world, player, bx + 0.5D, by + dy, bz + 0.5D);
            if (rm != null) return rm;
        }
        return null;
    }

    private RailMap findNextRailMap(World world, EntityPlayer player,
                                    double cx, double cy, double cz,
                                    RailMap exclude) {
        int bx = MathHelper.floor(cx);
        int by = MathHelper.floor(cy);
        int bz = MathHelper.floor(cz);

        for (int dy = 1; dy >= -1; dy--) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    RailMap rm = TileEntityLargeRailBase.getRailMapFromCoordinates(
                            world, player,
                            bx + dx + 0.5D,
                            by + dy,
                            bz + dz + 0.5D);
                    if (rm != null && !rm.equals(exclude)) {
                        return rm;
                    }
                }
            }
        }
        return null;
    }

    public PosRotation resolvePos(World world, EntityPlayer player,
                                  RailContext ctx, double targetDist,
                                  float refYaw, double dirMul) {
        return traverseFromStart(world, player, ctx.railMap, ctx.baseDist,
                targetDist, refYaw, dirMul, 0);
    }

    private PosRotation traverseFromStart(World world, EntityPlayer player,
                                          RailMap startMap, double startDist,
                                          double targetDist, float refYaw,
                                          double dirMul, int depth) {
        double len = startMap.getLength();
        double localTarget = targetDist;

        if (localTarget >= 0.0D && localTarget <= len) {
            return sampleRail(startMap, localTarget, refYaw);
        }

        if (depth >= 16) {
            return extrapolate(startMap, localTarget, refYaw);
        }
        boolean forward = localTarget > len;
        int edgeIdx = forward ? POS_SPLIT : 0;
        double[] edgePosZX = startMap.getRailPos(POS_SPLIT, edgeIdx);
        double edgeX = edgePosZX[1];
        double edgeZ = edgePosZX[0];
        double edgeY = startMap.getRailHeight(POS_SPLIT, edgeIdx);

        RailMap nextMap = findNextRailMap(world, player, edgeX, edgeY, edgeZ, startMap);

        if (nextMap == null) {
            return extrapolate(startMap, localTarget, refYaw);
        }
        double overflow = forward ? (localTarget - len) : localTarget;

        int edgeIdxNext = forward ? 0 : POS_SPLIT;
        double[] nextEdgePosZX = nextMap.getRailPos(POS_SPLIT, edgeIdxNext);
        double nextEdgeX = nextEdgePosZX[1];
        double nextEdgeZ = nextEdgePosZX[0];
        double[] nextEndPosZX = nextMap.getRailPos(POS_SPLIT, forward ? POS_SPLIT : 0);
        double distToStart = dist2D(edgeX, edgeZ, nextEdgePosZX[1], nextEdgePosZX[0]);
        double distToEnd = dist2D(edgeX, edgeZ, nextEndPosZX[1], nextEndPosZX[0]);

        double nextTargetDist;
        if (distToStart <= distToEnd) {
            nextTargetDist = forward ? overflow : nextMap.getLength() + overflow;
        } else {
            nextTargetDist = forward ? nextMap.getLength() - overflow : -overflow;
        }

        return traverseFromStart(world, player, nextMap, 0.0D,
                nextTargetDist, refYaw, dirMul, depth + 1);
    }

    private double dist2D(double x0, double z0, double x1, double z1) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private PosRotation sampleRail(RailMap rm, double dist, float refYaw) {
        double len = rm.getLength();
        if (len <= 0.0D) {
            double[] p = rm.getRailPos(1, 0);
            return new PosRotation(refYaw, 0f, 0f, p[1], rm.getRailHeight(1, 0), p[0]);
        }
        double ratio = MathHelper.clamp(dist / len, 0.0D, 1.0D);
        int index = MathHelper.clamp((int) (ratio * POS_SPLIT), 0, POS_SPLIT);

        float railYaw = NGTMath.wrapAngle(rm.getRailYaw(POS_SPLIT, index));
        float yaw = EntityBogie.fixBogieYaw(refYaw, railYaw);
        float pitch = EntityBogie.fixBogiePitch(rm.getRailPitch(POS_SPLIT, index), railYaw, yaw);
        float roll = rm.getRailRoll(POS_SPLIT, index);

        double[] posZX = rm.getRailPos(POS_SPLIT, index);
        return new PosRotation(yaw, pitch, roll, posZX[1], rm.getRailHeight(POS_SPLIT, index), posZX[0]);
    }

    private PosRotation extrapolate(RailMap rm, double dist, float refYaw) {
        boolean forward = dist > rm.getLength();
        double clampedDist = forward ? rm.getLength() : 0.0D;
        double overflow = forward ? dist - rm.getLength() : dist;

        PosRotation edge = sampleRail(rm, clampedDist, refYaw);
        float radF = (float) Math.toRadians(edge.yaw);
        float pitchF = (float) Math.toRadians(edge.pitch);
        double cosP = Math.cos(pitchF);

        return new PosRotation(edge.yaw, edge.pitch, edge.roll,
                edge.posX + (-Math.sin(radF) * cosP * overflow),
                edge.posY + (-Math.sin(pitchF) * overflow),
                edge.posZ + (Math.cos(radF) * cosP * overflow));
    }

    public EntityTrainBase createTrainEntity(World world, String modelName) {
        try {
            ModelSetVehicleBase<TrainConfig> modelSet =
                    ModelPackManager.INSTANCE.getResourceSet(RTMResource.TRAIN_EC, modelName);
            if (modelSet != null && !modelSet.isDummy()) {
                String subType = modelSet.getConfig().getSubType();
                if ("DC".equalsIgnoreCase(subType)) return new EntityTrainDieselCar(world, "");
                if ("CC".equalsIgnoreCase(subType)) return new EntityFreightCar(world, "");
                if ("TC".equalsIgnoreCase(subType)) return new EntityTanker(world, "");
                if ("Test".equalsIgnoreCase(subType)) return new EntityTrainTest(world, "");
            }
        } catch (Exception ignored) {
        }
        return new EntityTrainElectricCar(world, "");
    }

    private static class RailContext {
        final RailMap railMap;
        final double baseDist;

        RailContext(RailMap rm, double base) {
            this.railMap = rm;
            this.baseDist = base;
        }
    }

    public static class TrainSet {
        public String modelName;
        public double posX, posY, posZ;
        public float yaw, pitch;
        public int index, dir;

        public TrainSet(String model, int index, double x, double y, double z,
                        float yaw, float pitch, int dir) {
            this.modelName = model;
            this.index = index;
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.dir = dir;
        }

        public static TrainSet readFromNBT(NBTTagCompound nbt) {
            return new TrainSet(
                    nbt.getString("model"), nbt.getInteger("index"),
                    nbt.getFloat("pos_x"), nbt.getFloat("pos_y"), nbt.getFloat("pos_z"),
                    nbt.getFloat("yaw"), nbt.getFloat("pitch"),
                    nbt.hasKey("dir") ? nbt.getInteger("dir") : 0);
        }
    }

    private static class PosRotation {
        final float yaw, pitch, roll;
        final double posX, posY, posZ;

        PosRotation(float yaw, float pitch, float roll,
                    double posX, double posY, double posZ) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }
    }
}