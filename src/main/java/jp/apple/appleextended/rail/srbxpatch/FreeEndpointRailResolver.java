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

/*
 * SRBX-Patchからの移植
 */
package jp.apple.appleextended.rail.srbxpatch;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reflection keeps Minecraft/SRG references out of the coremod. Metadata is cached per class;
 * worlds, cores, maps and bogies are never cached. A failure delegates to AppleExtended.
 */
public final class FreeEndpointRailResolver {
    private static final AtomicBoolean warned = new AtomicBoolean();
    private static final boolean debugLookup = Boolean.getBoolean("appleextended.debugRailLookup");
    private static final AtomicLong nextTrace = new AtomicLong();
    private static final ClassValue<Members> members = new ClassValue<Members>() {
        protected Members computeValue(Class<?> type) {
            return new Members(type);
        }
    };
    private static volatile boolean healthy = true;

    public static boolean isHealthy() {
        return healthy;
    }

    public static Object resolve(Object bogie, double x, double y, double z) {
        if (!healthy) return null;
        if (!EndpointGeometry.finite(x) || !EndpointGeometry.finite(y) || !EndpointGeometry.finite(z)) return null;
        try {
            Object world = field(bogie, "world", "field_70170_p");
            if (world == null || (Boolean) field(world, "isRemote", "field_72995_K")) return null;
            Object core = field(bogie, "currentRailObj"), map = field(bogie, "currentRailMap");
            int split = ((Number) field(bogie, "split")).intValue();
            int previous = ((Number) field(bogie, "prevPosIndex")).intValue();
            if (core == null || map == null || split <= 0 || previous < 0 || previous > split) return null;
            double bx = number(bogie, "posX", "field_70165_t").doubleValue();
            double bz = number(bogie, "posZ", "field_70161_v").doubleValue();
            double movement = Math.hypot(x - bx, z - bz);
            if (!EndpointGeometry.finite(movement) || movement > 10.0) return null;
            int margin = (int) Math.ceil((movement + 1.0) * 360.0);
            if (!live(world, core)) return null;
            Object maps = call(core, "getAllRailMaps");
            boolean live = false;
            if (maps != null) for (int i = 0; i < Array.getLength(maps); i++) live |= Array.get(maps, i) == map;
            if (!live) return null;
            Object start = call(map, "getStartRP"), end = call(map, "getEndRP");
            double sx = number(start, "posX").doubleValue(), sz = number(start, "posZ").doubleValue();
            double ex = number(end, "posX").doubleValue(), ez = number(end, "posZ").doubleValue();
            double sy = ((Number) call(map, "getRailYaw", split, Math.min(1, split))).doubleValue();
            double ey = ((Number) call(map, "getRailYaw", split, Math.max(0, split - 1))).doubleValue();
            if (!EndpointGeometry.finite(sy) || !EndpointGeometry.finite(ey)) return null;
            boolean freeStart = previous <= margin && EndpointGeometry.freeInteriorEndpoint(sx, sz, sy);
            boolean freeEnd = split - previous <= margin && EndpointGeometry.freeInteriorEndpoint(ex, ez, ey);
            Method sample = members.get(map.getClass()).method("getRailPos", 2);
            double[] previousPoint = (double[]) sample.invoke(map, split, previous);
            if (Math.hypot(bx - previousPoint[1], bz - previousPoint[0]) > 0.25
                    || !validHeight(map, bogie, split, previous, y)) return null;
            // Ownership of overlapping ballast is not a reason to change a live map.
            // Still release it at either nearby endpoint plane: boundary endpoints
            // use native lookup, free interior endpoints use precise connection lookup.
            if (previous <= margin && projection(x - sx, z - sz, sy) < -1E-7)
                return crossed(world, bogie, core, start, sy + 180, x, y, z, bx, bz, movement, freeStart);
            if (split - previous <= margin && projection(x - ex, z - ez, ey) > 1E-7)
                return crossed(world, bogie, core, end, ey, x, y, z, bx, bz, movement, freeEnd);
            int search = (int) Math.ceil((movement + 0.25) * 360.0);
            int low = Math.max(0, previous - search), high = Math.min(split, previous + search);
            double best = Double.MAX_VALUE;
            int nearest = previous;
            // Try the projected sample first. Dense lookup remains the conservative fallback.
            double localYaw = ((Number) call(map, "getRailYaw", split, previous)).doubleValue();
            if (!EndpointGeometry.finite(localYaw)) return null;
            double tangent = Math.toRadians(localYaw);
            int projected = Math.max(low, Math.min(high, previous + (int) Math.round(
                    ((x - bx) * Math.sin(tangent) + (z - bz) * Math.cos(tangent)) * 360.0)));
            double[] predicted = (double[]) sample.invoke(map, split, projected);
            double pdx = x - predicted[1], pdz = z - predicted[0];
            double projectedDistance = pdx * pdx + pdz * pdz;
            // Within one sampling step: no tile search and no redundant dense scan.
            if (projectedDistance <= 1.0 / (360.0 * 360.0) && validHeight(map, bogie, split, projected, y))
                return retained(core, previous, split, x, y, z);
            for (int i = low; i <= high; i++) {
                double[] point = (double[]) sample.invoke(map, split, i);
                double dx = x - point[1], dz = z - point[0];
                double distance = dx * dx + dz * dz;
                if (distance < best) {
                    best = distance;
                    nearest = i;
                }
            }
            if (best > 0.015625) return null; // 0.125m maximum horizontal deviation
            if (!validHeight(map, bogie, split, nearest, y)) return null;
            return retained(core, previous, split, x, y, z);
        } catch (ReflectiveOperationException | RuntimeException error) {
            healthy = false;
            if (warned.compareAndSet(false, true))
                System.err.println("[AppleExtended] free endpoint hook unavailable; delegating to native lookup: " + error);
            return null;
        }
    }

    private static Object retained(Object core, int previous, int split, double x, double y, double z)
            throws ReflectiveOperationException {
        // Opt-in, at most one message per second globally; no world/entity cache.
        if (traceAllowed()) {
            System.out.println("[AppleExtended AE lookup] retained: core=" + call(core, "getPos|func_174877_v")
                    + ", index=" + previous + "/" + split + ", predicted=" + x + "," + y + "," + z);
        }
        return core;
    }

    private static boolean traceAllowed() {
        if (!debugLookup) return false;
        long now = System.nanoTime(), next = nextTrace.get();
        return (next == 0 || now - next >= 0) && nextTrace.compareAndSet(next, now + 1000000000L);
    }

    private static double projection(double x, double z, double yaw) {
        double angle = Math.toRadians(yaw);
        return x * Math.sin(angle) + z * Math.cos(angle);
    }

    private static Object pos(Object core, int x, int y, int z) throws ReflectiveOperationException {
        Object position = call(core, "getPos|func_174877_v");
        return position.getClass().getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
    }

    private static boolean live(Object world, Object core) throws ReflectiveOperationException {
        Object position = call(core, "getPos|func_174877_v");
        return !(Boolean) call(core, "isInvalid|func_145837_r")
                && (Boolean) call(world, "isBlockLoaded|func_175667_e", position)
                && call(world, "getTileEntity|func_175625_s", position) == core;
    }

    private static Object activeMap(Object core, Object bogie, Object world) throws ReflectiveOperationException {
        // Match resetRailObj rather than selecting an inactive branch from getAllRailMaps.
        if (core.getClass().getName().contains("SwitchCore"))
            return call(call(call(core, "getSwitch"), "getNearestPoint", bogie), "getActiveRailMap", world);
        return call(core, "getRailMap", bogie);
    }

    private static Object connected(Object world, Object bogie, Object current, Object endpoint, double exitYaw,
                                    double x, double y, double z, double bx, double bz, double movement)
            throws ReflectiveOperationException {
        double ex = number(endpoint, "posX").doubleValue(), ez = number(endpoint, "posZ").doubleValue();
        double ey = number(endpoint, "posY").doubleValue();
        if (projection(x - bx, z - bz, exitYaw) <= 0 || Math.hypot(x - ex, z - ez) > movement + 0.25) return null;
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        Object selected = null;
        int fx = (int) Math.floor(ex), fy = (int) Math.floor(ey), fz = (int) Math.floor(ez);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                for (int dy = -2; dy <= 2; dy++) {
                    Object position = pos(current, fx + dx, fy + dy, fz + dz);
                    if (!(Boolean) call(world, "isBlockLoaded|func_175667_e", position)) continue;
                    Object tile = call(world, "getTileEntity|func_175625_s", position);
                    if (tile == null) continue;
                    Method getCore;
                    try {
                        getCore = members.get(tile.getClass()).method("getRailCore", 0);
                    } catch (NoSuchMethodException ignored) {
                        continue;
                    }
                    Object candidate = getCore.invoke(tile);
                    if (candidate == null || candidate == current || !visited.add(candidate) || !live(world, candidate))
                        continue;
                    Object map = activeMap(candidate, bogie, world);
                    if (map == null || map == field(bogie, "currentRailMap")) continue;
                    if (fitsConnection(map, bogie, endpoint, exitYaw, x, y, z, movement)) {
                        if (selected != null && selected != candidate) return null;
                        selected = candidate;
                    }
                }
        return selected;
    }

    private static Object crossed(Object world, Object bogie, Object current, Object endpoint, double exitYaw,
                                  double x, double y, double z, double bx, double bz, double movement, boolean free)
            throws ReflectiveOperationException {
        double ex = number(endpoint, "posX").doubleValue(), ez = number(endpoint, "posZ").doubleValue();
        if (projection(x - bx, z - bz, exitYaw) <= 0 || Math.hypot(x - ex, z - ez) > movement + 0.25) return null;
        // Section owner positions survive overlapping ballast ownership. Prefer only a
        // live member of the same logical group with a matching physical endpoint.
        if (current.getClass().getName().contains("SectionCore") && (Boolean) call(current, "isRailSection")) {
            Object positions = call(current, "getRailGroupCorePositions"), selected = null;
            List<?> corePositions = (List<?>) positions;
            int size = corePositions.size();
            if (size >= 0 && size <= 4096) for (int i = 0; i < size; i++) {
                int[] p = (int[]) corePositions.get(i);
                if (p == null || p.length < 3) continue;
                Object position = pos(current, p[0], p[1], p[2]);
                if (!(Boolean) call(world, "isBlockLoaded|func_175667_e", position)) continue;
                Object candidate = call(world, "getTileEntity|func_175625_s", position);
                if (candidate == null || candidate == current || !candidate.getClass().getName().contains("SectionCore")
                        || !(Boolean) call(current, "isSameLogicalRail", candidate) || !live(world, candidate))
                    continue;
                Object map = activeMap(candidate, bogie, world);
                if (map != null && fitsConnection(map, bogie, endpoint, exitYaw, x, y, z, movement)) {
                    if (selected != null && selected != candidate) return null;
                    selected = candidate;
                }
            }
            if (selected != null) {
                if (traceAllowed()) System.out.println("[AppleExtended AE lookup] section transition: from="
                        + call(current, "getPos|func_174877_v") + ", to=" + call(selected, "getPos|func_174877_v"));
                return selected;
            }
        }
        return free ? connected(world, bogie, current, endpoint, exitYaw, x, y, z, bx, bz, movement) : null;
    }

    private static boolean fitsConnection(Object map, Object bogie, Object endpoint, double exitYaw,
                                          double x, double y, double z, double movement)
            throws ReflectiveOperationException {
        double ex = number(endpoint, "posX").doubleValue(), ez = number(endpoint, "posZ").doubleValue();
        double ey = number(endpoint, "posY").doubleValue();
        Object start = call(map, "getStartRP"), end = call(map, "getEndRP");
        double length = ((Number) call(map, "getLength")).doubleValue();
        if (!EndpointGeometry.finite(length) || length <= 0 || length > Integer.MAX_VALUE / 360.0) return false;
        int count = Math.max(1, (int) (length * 360.0));
        boolean fits = false;
        for (int side = 0; side < 2; side++) {
            Object rp = side == 0 ? start : end;
            if (Math.hypot(number(rp, "posX").doubleValue() - ex, number(rp, "posZ").doubleValue() - ez) > 0.001
                    || Math.abs(number(rp, "posY").doubleValue() - ey) > 0.03) continue;
            double yaw = ((Number) call(map, "getRailYaw", count, side == 0 ? Math.min(1, count) : Math.max(0, count - 1))).doubleValue();
            double inward = side == 0 ? yaw : yaw + 180.0;
            if (!EndpointGeometry.finite(inward) || Math.cos(Math.toRadians(inward - exitYaw)) < 0.95) continue;
            double progress = projection(x - ex, z - ez, inward);
            if (progress < 0 || progress > length + 0.001) continue;
            int reach = Math.min(count, (int) Math.ceil((movement + 0.25) * 360.0));
            int nearest = side == 0 ? 0 : count;
            double best = Double.MAX_VALUE;
            for (int n = 0; n <= reach; n++) {
                int index = side == 0 ? n : count - n;
                double[] point = (double[]) call(map, "getRailPos", count, index);
                double distance = Math.pow(x - point[1], 2) + Math.pow(z - point[0], 2);
                if (distance < best) {
                    best = distance;
                    nearest = index;
                }
            }
            if (best <= 0.015625 && validHeight(map, bogie, count, nearest, y)) fits = true;
        }
        return fits;
    }

    private static boolean validHeight(Object map, Object bogie, int split, int index, double y)
            throws ReflectiveOperationException {
        double railY = ((Number) call(map, "getRailHeight", split, index)).doubleValue();
        double offset = ((Number) call(bogie, "getYOffset|func_70033_W")).doubleValue();
        return EndpointGeometry.finite(railY) && EndpointGeometry.finite(offset)
                && Math.abs(y - (railY + offset)) <= 0.75;
    }

    private static Number number(Object object, String... names) throws ReflectiveOperationException {
        return (Number) field(object, names);
    }

    private static Object field(Object object, String... names) throws ReflectiveOperationException {
        return members.get(object.getClass()).field(names).get(object);
    }

    private static Object call(Object object, String names, Object... args) throws ReflectiveOperationException {
        return members.get(object.getClass()).method(names, args.length).invoke(object, args);
    }

    private static final class Members {
        private final Class<?> type;
        private final Map<String, Field> fields = new HashMap<String, Field>();
        private final Map<String, Method> methods = new HashMap<String, Method>();

        Members(Class<?> type) {
            this.type = type;
        }

        synchronized Field field(String[] names) throws NoSuchFieldException {
            String key = names[0];
            if (fields.containsKey(key)) return fields.get(key);
            for (String name : names)
                for (Class<?> t = type; t != null; t = t.getSuperclass()) {
                    try {
                        Field f = t.getDeclaredField(name);
                        f.setAccessible(true);
                        fields.put(key, f);
                        return f;
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            throw new NoSuchFieldException(type.getName() + "." + key);
        }

        synchronized Method method(String names, int count) throws NoSuchMethodException {
            String key = names + "/" + count;
            if (methods.containsKey(key)) return methods.get(key);
            for (String name : names.split("\\|"))
                for (Method m : type.getMethods()) {
                    if (m.getName().equals(name) && m.getParameterTypes().length == count) {
                        m.setAccessible(true);
                        methods.put(key, m);
                        return m;
                    }
                }
            throw new NoSuchMethodException(type.getName() + "." + key);
        }
    }
}
