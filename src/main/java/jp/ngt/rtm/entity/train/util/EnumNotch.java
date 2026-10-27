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

package jp.ngt.rtm.entity.train.util;

import jp.ngt.ngtlib.io.ScriptUtil;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;

public enum EnumNotch {
    accelerate_5(5, 1.80F, 0.002F),
    accelerate_4(4, 1.44F, 0.002F),
    accelerate_3(3, 1.08F, 0.002F),
    accelerate_2(2, 0.72F, 0.002F),
    accelerate_1(1, 0.36F, 0.002F),
    inertia(0, 0.0F, 0.0F),
    brake_1(-1, 0.0F, -0.0005F),
    brake_2(-2, 0.0F, -0.001F),
    brake_3(-3, 0.0F, -0.0015F),
    brake_4(-4, 0.0F, -0.002F),
    brake_5(-5, 0.0F, -0.0025F),
    brake_6(-6, 0.0F, -0.003F),
    brake_7(-7, 0.0F, -0.0035F),
    emergency_brake(-8, 0.0F, -0.01F);

    public final int id;
    public final float max_speed;
    public final float acceleration;

    private EnumNotch(int par1, float par2, float par3) {
        this.id = par1;
        this.max_speed = par2;
        this.acceleration = par3;
    }

    public static EnumNotch getNotch(int par1) {
        for (EnumNotch notch : EnumNotch.values()) {
            if (notch.id == par1) {
                return notch;
            }
        }
        return inertia;
    }

    public static EnumNotch getNotchFromSignal(int par1) {
        switch (par1) {
            case 1:
                return accelerate_5;
            case 2:
                return accelerate_4;
            case 3:
                return accelerate_3;
            case 4:
                return accelerate_2;
            case 5:
                return brake_4;
            default:
                return inertia;
        }
    }

    public static float getAcceleration(int par1, float par2) {
        EnumNotch notch = getNotch(par1);
        if (par1 > 0 && par2 >= notch.max_speed) {
            return 0.0F;
        }
        return notch.acceleration;
    }

    public static EnumNotch getSuitableNotchFromSpeed(float par1) {
        if (par1 >= accelerate_5.max_speed) {
            return accelerate_5;
        }

        for (EnumNotch notch : EnumNotch.values()) {
            if (notch.max_speed >= par1 && notch.max_speed - 0.3F < par1) {
                return notch;
            }
        }
        return inertia;
    }

    public static EnumNotch getSuitableNotchFromAcceleration(float par1) {
        if (par1 > 0.0F) {
            return accelerate_4;
        }

        for (EnumNotch notch : EnumNotch.values()) {
            if (notch.acceleration <= par1 && notch.acceleration + 0.0005F > par1) {
                return notch;
            }
        }
        return inertia;
    }

    public static float getAcceleration(int notch, float prevSpeed, TrainConfig cfg, EntityTrainBase train) {
        if (notch == 0) {
            return 0.0F;
        } else if (notch > 0) {
            --notch;
            if (prevSpeed >= cfg.maxSpeed[notch])
                return 0.0F;
            if (cfg.useVariableAcceleration) {
                Object obj = ScriptUtil.doScriptIgnoreError(
                        train.getResourceState().getResourceSet().serverSE,
                        "getAcceleration",
                        train,
                        prevSpeed);
                return obj != null ? Float.parseFloat(obj.toString()) : 0.0f;
            } else {
                return cfg.accelerateions[Math.min(cfg.accelerateions.length - 1, notch)];
            }
        } else {
            float deceleration;
            if (cfg.useVariableDeceleration) {
                Object obj = ScriptUtil.doScriptIgnoreError(
                        train.getResourceState().getResourceSet().serverSE,
                        "getDeceleration",
                        train,
                        prevSpeed);
                deceleration = obj != null ? Float.parseFloat(obj.toString()) : 0.0f;
            } else {
                deceleration = cfg.deccelerations[Math.min(cfg.deccelerations.length - 1, -notch)];
            }
            float absSpeed = Math.abs(prevSpeed);
            return absSpeed + deceleration < 0.0F ? -absSpeed : deceleration;
        }
    }
}