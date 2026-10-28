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

package jp.apple.appleextended.train.render;

import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.rtm.entity.train.EntityBogie;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.entity.vehicle.EntityVehicleBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * 車体の揺れ
 *
 * <h3>仕組み</h3>
 * 車体のロール(左右)とピッチ(前後)を、それぞれ「ばね+ダンパー」で動かす
 * <ul>
 *   <li>目標値: 加減速と横加速度から決まる、車体が傾きたい角度(下の *_PER_* と *_SIGN)</li>
 *   <li>車体は目標へ、固有振動数(*_FREQ)と減衰(DAMPING)で決まる遅れ方で追従する</li>
 *   <li>外乱: 線路の細かな凹凸(NOISE_*)と、継ぎ目を通る衝撃(JOINT_*)が、揺れの速度に足される</li>
 * </ul>
 *
 */
@SideOnly(Side.CLIENT)
public final class TrainBodySway {

    // ================= 回転の中心 =================

    /**
     * 車体が傾くときの回転中心の高さ
     * <ul>
     *   <li>大きくする: 車体の上のほうが中心になり、床がメインで揺れる</li>
     *   <li>小さくする: 床の近くが中心になり、天井がメインで揺れる</li>
     *   <li>目安: 0.0〜1.2</li>
     * </ul>
     */
    public static final float ROLL_CENTER = 0.6F;

    // ================= ダンパー =================

    /**
     * ロールの固有振動数 [Hz]
     * <ul>
     *   <li>大きくする: ばねが固くなる(小刻みで素早い揺れ)</li>
     *   <li>小さくする: ばねが柔らかくなる(ゆったりした大きな揺れ)</li>
     *   <li>目安: 0.5〜2.0</li>
     * </ul>
     */
    private static final float ROLL_FREQ = 1.0F;

    /**
     * ピッチの固有振動数 [Hz]。意味は ROLL_FREQ と同じ
     * 目安: 0.8〜2.5。
     */
    private static final float PITCH_FREQ = 1.3F;

    /**
     * 減衰比。ロール・ピッチ共通
     * <ul>
     *   <li>0に近い: 揺れが残る</li>
     *   <li>0.2〜0.3: 数回揺れて収まる</li>
     *   <li>1.0: 行き過ぎずに目標へ収まる</li>
     *   <li>1.0より大きい: 揺れない、目標へ追従するだけ</li>
     * </ul>
     */
    private static final float DAMPING = 0.25F;

    // ================= 加速度に対する傾き =================

    /**
     * 横加速度あたりのロールの目標角
     * <ul>
     *   <li>大きくする: カーブの出入りで車体が大きく傾く</li>
     *   <li>0にする: 横加速度では傾かなくなる</li>
     *   <li>目安: 0.1〜0.8</li>
     * </ul>
     */
    private static final float ROLL_PER_LAT = 0.15F;

    /**
     * ロールの向きの符号。+1 or -1 だけを入れる
     */
    private static final float ROLL_SIGN = -1.0F;

    /**
     * 前後加速度あたりのピッチの目標角
     * 減速で前のめり、加速で後ろ下がりになる
     * <ul>
     *   <li>大きくする: ブレーキで前のめりが強くなる</li>
     *   <li>0にする: 加減速で前後に傾かなくなる</li>
     *   <li>目安: 0.1〜0.5</li>
     * </ul>
     */
    private static final float PITCH_PER_LON = 0.05F;

    /**
     * 前後加速度を平均化する強さ(0〜1)。1tickごとにこの割合だけ新しい値に近づける
     * <ul>
     *   <li>大きくする: 加速度の細かな変動にも反応する</li>
     *   <li>小さくする: ブレーキの変化にゆっくり反応する</li>
     *   <li>目安: 0.05〜0.4</li>
     * </ul>
     */
    private static final float LON_FILTER = 0.5F;

    /**
     * 加速度の上限。横・前後ともこの値で頭打ちにさせる
     * 速度の飛びで車体が極端に傾くのを防ぐため
     * 目安: 3〜10
     */
    private static final float MAX_ACCEL = 6.0F;

    // ================= 揺れの上限 =================

    /** ロールの最大角 [度] */
    private static final float MAX_ROLL = 2.5F;

    /** ピッチの最大角 [度] */
    private static final float MAX_PITCH = 1.2F;

    // ================= 線路の凹凸 =================

    /**
     * ロールの小さなランダムな揺れの強さ [度/秒]。毎tick揺れの速度に足される
     * <ul>
     *   <li>大きくする: 走行中ずっと細かく揺れる</li>
     *   <li>0にする: 継ぎ目とカーブ以外では揺れない</li>
     *   <li>目安: 0〜1.0</li>
     * </ul>
     */
    private static final float NOISE_ROLL = 0.4F;

    /** ピッチのランダムな揺れの強さ [度/秒]。意味は NOISE_ROLL と同じ。目安: 0〜0.5 */
    private static final float NOISE_PITCH = 0.15F;

    // ================= ジョイント =================

    /**
     * ジョイントを台車が通るときのロールの揺れの強さ [度/秒]。左右の向きは毎回ランダム
     * この値に「速度/SPEED_REF」を掛けた分が加わる。高速ではほぼ揺れない
     * <ul>
     *   <li>大きくする: 揺れが強くなる</li>
     *   <li>0にする: 揺れなくなる</li>
     *   <li>目安: 0〜5</li>
     * </ul>
     */
    private static final float JOINT_ROLL = 1.5F;

    /**
     * ジョイントを台車が通るときの、ピッチの揺れの強さ [度/秒]。
     * 前の台車が通るときと後ろの台車が通るときで逆向きに入る
     * 速度による係数は JOINT_ROLL と同じ
     * 目安: 0〜3
     */
    private static final float JOINT_PITCH = 1.5F;

    /**
     * 速度の基準 [m/s]
     * 目安: 10〜40
     */
    private static final float SPEED_REF = 32.5F;

    // ================= 内部 =================

    private static final float DT = 0.05F;
    private static final Map<EntityVehicleBase, State> STATES =
            Collections.synchronizedMap(new WeakHashMap<EntityVehicleBase, State>());
    private static final Random RAND = new Random();
    private static final int MAX_CATCHUP_TICKS = 10;

    private static final class State {
        long tick = Long.MIN_VALUE;
        float roll, rollV, pitch, pitchV;
        float prevRoll, prevPitch;
        float lastSpeed, lastYaw, aLon;
        float pendRoll, pendPitch;
    }

    private TrainBodySway() {
    }

    /**
     * 台車がジョイントを通ったときに呼ばれる
     */
    public static void onJoint(EntityBogie bogie) {
        EntityTrainBase train = bogie.getTrain();
        if (train == null) {
            return;
        }
        State s = STATES.get(train);
        if (s == null) {
            return;
        }
        float k = Math.max(1.0F - s.lastSpeed / SPEED_REF, 0.0F);
        synchronized (s) {
            s.pendRoll += (RAND.nextBoolean() ? 1.0F : -1.0F) * JOINT_ROLL * k;
            s.pendPitch += (bogie.getBogieId() == 0 ? -1.0F : 1.0F) * JOINT_PITCH * k;
        }
    }

    /** 1tickに1回だけ状態を進める */
    public static void update(EntityVehicleBase v) {
        State s = STATES.get(v);
        if (s == null) {
            s = new State();
            STATES.put(v, s);
        }
        long now = v.world.getTotalWorldTime();
        if (s.tick == now) {
            return;
        }
        boolean first = (s.tick == Long.MIN_VALUE);
        long gap = first ? 0L : now - s.tick;
        s.tick = now;

        float speed = Math.abs(v.getSpeed()) * 20.0F;
        float yawDelta = NGTMath.wrapAngle(v.rotationYaw - s.lastYaw);
        float lastSpeed = s.lastSpeed;
        s.lastSpeed = speed;
        s.lastYaw = v.rotationYaw;

        if (first || gap > MAX_CATCHUP_TICKS) {
            s.roll = s.rollV = s.pitch = s.pitchV = 0.0F;
            s.prevRoll = s.prevPitch = 0.0F;
            s.aLon = 0.0F;
            synchronized (s) {
                s.pendRoll = s.pendPitch = 0.0F;
            }
            return;
        }

        int steps = (int) gap;
        float yawRate = (float) Math.toRadians(yawDelta) * 20.0F / steps;
        float aLat = clamp(speed * yawRate, MAX_ACCEL);
        float aLonIn = clamp((speed - lastSpeed) * 20.0F / steps, MAX_ACCEL);
        float k = Math.min(speed / SPEED_REF, 1.0F);

        for (int i = 0; i < steps; ++i) {
            s.aLon += (aLonIn - s.aLon) * LON_FILTER;

            float rollTarget = clamp(ROLL_SIGN * ROLL_PER_LAT * aLat, MAX_ROLL);
            float pitchTarget = clamp(PITCH_PER_LON * s.aLon, MAX_PITCH);

            if (i == steps - 1) {
                s.prevRoll = s.roll;
                s.prevPitch = s.pitch;
            }

            float wr = (float) (2.0D * Math.PI * ROLL_FREQ);
            s.rollV += (-wr * wr * (s.roll - rollTarget) - 2.0F * DAMPING * wr * s.rollV) * DT;
            s.roll += s.rollV * DT;

            float wp = (float) (2.0D * Math.PI * PITCH_FREQ);
            s.pitchV += (-wp * wp * (s.pitch - pitchTarget) - 2.0F * DAMPING * wp * s.pitchV) * DT;
            s.pitch += s.pitchV * DT;

            s.rollV += (float) RAND.nextGaussian() * NOISE_ROLL * k;
            s.pitchV += (float) RAND.nextGaussian() * NOISE_PITCH * k;

            synchronized (s) {
                s.rollV += s.pendRoll;
                s.pitchV += s.pendPitch;
                s.pendRoll = 0.0F;
                s.pendPitch = 0.0F;
            }

            s.roll = clamp(s.roll, MAX_ROLL);
            s.pitch = clamp(s.pitch, MAX_PITCH);
        }
    }

    /** 車体の追加ロール [度] */
    public static float getRoll(EntityVehicleBase v, float partialTick) {
        State s = STATES.get(v);
        return s == null ? 0.0F : s.prevRoll + (s.roll - s.prevRoll) * partialTick;
    }

    /** 車体の追加ピッチ [度] */
    public static float getPitch(EntityVehicleBase v, float partialTick) {
        State s = STATES.get(v);
        return s == null ? 0.0F : s.prevPitch + (s.pitch - s.prevPitch) * partialTick;
    }

    private static float clamp(float v, float max) {
        return v < -max ? -max : (v > max ? max : v);
    }

    /** 車体の最終ロール [度] = カントロール + 揺れ。プレイヤーカメラに渡す */
    public static float getTotalRoll(EntityVehicleBase v, float partialTick) {
        float base = v.prevRotationRoll + (v.rotationRoll - v.prevRotationRoll) * partialTick;
        return base + getRoll(v, partialTick);
    }
}
