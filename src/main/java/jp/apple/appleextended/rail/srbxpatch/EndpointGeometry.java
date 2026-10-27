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

/**
 * Pure geometry shared by the runtime and regression tests.
 */
public final class EndpointGeometry {
    private EndpointGeometry() {
    }

    public static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    public static boolean interior(double x, double z, double sx, double sz, double ex, double ez,
                                   double startYaw, double endYaw) {
        double s = Math.toRadians(startYaw), e = Math.toRadians(endYaw);
        // RailMap yaw is the forward tangent at both ends. Crossing keeps native resolution.
        return (x - sx) * Math.sin(s) + (z - sz) * Math.cos(s) >= -1.0E-7
                && (x - ex) * Math.sin(e) + (z - ez) * Math.cos(e) <= 1.0E-7;
    }

    public static boolean freeInteriorEndpoint(double x, double z, double yaw) {
        double angle = Math.toRadians(yaw);
        boolean xFace = Math.abs(Math.sin(angle)) > 1.0E-6 && Math.abs(x - Math.rint(x)) <= 1.0E-7;
        boolean zFace = Math.abs(Math.cos(angle)) > 1.0E-6 && Math.abs(z - Math.rint(z)) <= 1.0E-7;
        return !xFace && !zFace;
    }
}
