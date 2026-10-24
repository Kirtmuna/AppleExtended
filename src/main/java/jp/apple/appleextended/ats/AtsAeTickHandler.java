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

package jp.apple.appleextended.ats;

import jp.apple.appleextended.AppleSound;
import jp.apple.aris.api.ArisApiUtil;
import jp.apple.aris.ctc.state.SectionState;
import jp.apple.aris.ctc.state.SignalState;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.modelpack.state.DataMap;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.WeakHashMap;

public class AtsAeTickHandler {
    // ========================================================================
    // 定数
    // ========================================================================

    /**
     * 停止目標とする、信号手前の余裕距離 (m)
     */
    private static final double STOP_MARGIN = 20.0;

    /**
     * 信号が背後にあると判定する対象範囲 (m)
     */
    private static final double PASSED_RANGE = 30.0;
    /**
     * 赤に変わった時、この距離以内なら「間に合わない」とみなす (m)
     */
    private static final double TOO_LATE_RANGE = 10.0;
    /**
     * 「間に合わない」判定に必要な最低速度 (km/h)
     */
    private static final double TOO_LATE_MIN_SPEED = 5.0;
    /**
     * 基準位置を更新する最小移動量の2乗 (0.1m)
     */
    private static final double MOVE_EPSILON_SQ = 0.01;
    /**
     * 段階ごとの速度比 (現在速度 / パターン速度) 閾値
     * 0.60 = ん？ / 0.70 = うおw / 1.00 = 何やってんだよ
     */
    private static final double[] STAGE_THRESHOLDS = {0.60, 0.70, 1.00};
    /**
     * 段階ごとの鳴らす回数
     */
    private static final int[] STAGE_COUNTS = {1, 2, 3};
    /**
     * 段階の解除マージン
     */
    private static final double STAGE_HYSTERESIS = 0.03;
    /**
     * 連続再生の間隔 (tick)
     */
    private static final int CHIME_INTERVAL_TICKS = 5;
    /**
     * 加速危険時のチャイム回数
     */
    private static final int ACCEL_WARN_CHIME_COUNT = 3;
    /**
     * 加速危険と判定する、制限速度の超過量 (km/h)
     * 制限速度をこの値より超過した場合のみ警報+制動
     */
    private static final double ACCEL_DANGER_MARGIN = 15.0;
    /**
     * 加速中と判定する最小速度変化 (km/h/tick)
     */
    private static final double ACCEL_DETECT_THRESHOLD = 0.1;
    /**
     * RTM の速度単位 → km/h
     */
    private static final double SPEED_TO_KMH = 72.0;
    // ========================================================================
    // 車両ごとの状態
    // ========================================================================
    private static final Map<EntityTrainBase, AtsState> STATES = new WeakHashMap<>();
    // ========================================================================
    // 0. ON/OFF
    // ========================================================================
    private static boolean isDisabled(DataMap dm) {
        return !dm.contains("atsDisabled") || dm.getInt("atsDisabled") == 1;
    }

    private static void reset(EntityTrainBase train, DataMap dm) {
        train.releaseAtsBrake();
        STATES.remove(train);
        setIfChanged(dm, "atsLimit", -1);
        setIfChanged(dm, "atsPattern", -1);
    }
    // ========================================================================
    // メイン
    // ========================================================================

    /**
     * 前回位置からの移動ベクトルを返す
     * 0.1m 以上動いた時だけ基準位置を更新する
     */
    private static double[] updateMoveVector(AtsState st, EntityTrainBase train) {
        double mx = 0.0, mz = 0.0;
        if (st.lastPos != null) {
            mx = train.posX - st.lastPos[0];
            mz = train.posZ - st.lastPos[1];
        }
        if (st.lastPos == null || mx * mx + mz * mz > MOVE_EPSILON_SQ) {
            st.lastPos = new double[]{train.posX, train.posZ};
        }
        return new double[]{mx, mz};
    }
    /**
     * 無視中の信号が変わったら(=区間が進んだら)解除する
     */
    private static void releaseIgnoredIfSignalChanged(AtsState st, String signalId) {
        if (st.ignoredSignal != null && !st.ignoredSignal.equals(signalId)) {
            st.ignoredSignal = null;
        }
    }
    // ========================================================================
    // 3. 次信号の目標
    // ========================================================================
    private static Target resolveTarget(AtsState st, EntityTrainBase train, SignalState signal,
                                        String signalId, double[] move, double currentSpeed) {
        if (signal == null) {
            return new Target(-1.0, Double.MAX_VALUE);
        }
        double rawDistance = Double.MAX_VALUE;
        boolean behind = false;
        BlockPos[] positions = signal.getSignalPositions();
        if (positions != null && positions.length > 0 && positions[0] != null) {
            BlockPos pos = positions[0];
            double dx = (pos.getX() + 0.5) - train.posX;
            double dz = (pos.getZ() + 0.5) - train.posZ;
            rawDistance = Math.sqrt(dx * dx + dz * dz);
            behind = (dx * move[0] + dz * move[1]) < 0.0;
        }
        if (st.ignoredSignal == null) {
            boolean passed = behind && rawDistance < PASSED_RANGE;
            boolean justTurnedRed = signal.getCurrentAspect() == 0 && st.prevNextLimit > 0.0;
            boolean tooLate = justTurnedRed
                    && rawDistance < TOO_LATE_RANGE
                    && currentSpeed > TOO_LATE_MIN_SPEED;
            if (passed || tooLate) {
                st.ignoredSignal = signalId;
            }
        }
        if (st.ignoredSignal != null) {
            return new Target(-1.0, Double.MAX_VALUE);
        }
        double distance = (rawDistance == Double.MAX_VALUE)
                ? Double.MAX_VALUE
                : Math.max(0.0, rawDistance - STOP_MARGIN);
        return new Target(aspectToLimit(signal.getCurrentAspect()), distance);
    }
    // ========================================================================
    // 2. 通過済み / 間に合わない赤 の無視
    // ========================================================================
    /**
     * 区間が変わった時は、接近中に見えていた進行系現示の制限を引き継ぐ
     * (在線中の始端信号は必ず赤になるため、始端信号の現示は読まない)
     */
    private static double resolveLimit(AtsState st, SectionState section, double nextLimit) {
        double limit;
        if (!st.hasPrev || st.prevSection != section) {
            Double approach = st.approachLimit;
            st.approachLimit = null;
            if (approach != null) {
                limit = approach;
            } else {
                SignalState startSig = (section != null) ? section.getStartSignal() : null;
                int startAspect = (startSig != null) ? startSig.getCurrentAspect() : -1;
                limit = (startAspect > 0) ? aspectToLimit(startAspect) : -1.0;
            }
        } else {
            limit = st.prevLimit;
        }
        if (nextLimit > 0.0) {
            st.approachLimit = nextLimit;
        }
        return limit;
    }
    private static void saveSnapshot(AtsState st, SectionState section, double limit, double nextLimit) {
        st.hasPrev = true;
        st.prevSection = section;
        st.prevLimit = limit;
        st.prevNextLimit = nextLimit;
    }
    /**
     * パターン速度を km/h で返す。算出できない場合は Double.MAX_VALUE
     */
    private static double computePatternSpeed(EntityTrainBase train, double distance, double targetKmh) {
        if (distance == Double.MAX_VALUE || targetKmh < 0.0) {
            return Double.MAX_VALUE;
        }
        double maxDecel = AtsAeUtil.getMaxDeceleration(train);
        if (maxDecel <= 0.1) return Double.MAX_VALUE;

        double vTarget = targetKmh / 3.6;
        double vPattern = Math.sqrt(vTarget * vTarget + 2.0 * maxDecel * distance);
        return vPattern * 3.6;
    }
    // ========================================================================
    // 4. 現在の制限速度
    // ========================================================================

    private static double aspectToLimit(int aspect) {
        switch (aspect) {
            case 0:
                return 0.0;
            case 1:
                return 25.0;
            case 2:
                return 45.0;
            case 3:
                return 75.0;
            case 4:
                return 130.0;
            case 5:
                return 160.0;
            default:
                return -1.0;
        }
    }
    /**
     * 制限速度を ACCEL_DANGER_MARGIN 超過した時、1回だけ3回警報を鳴らす
     * accelWarned は超過中のラッチ
     */
    private static void updateAccelWarning(AtsState st, double currentSpeed, double limit, double nextLimit) {
        if (limit >= 0.0 && currentSpeed > limit + ACCEL_DANGER_MARGIN) {
            if (!st.accelWarned) {
                st.stage = 0;
                st.pendingChimes = ACCEL_WARN_CHIME_COUNT;
                st.accelWarned = true;
            }
        } else if (limit < 0.0 || currentSpeed <= limit) {
            st.accelWarned = false;
        }
    }
    // ========================================================================
    // 5. パターン速度
    // ========================================================================
    /**
     * 減速中: パターンへの接近度に応じて段階を上げ、上がった分だけ鳴らす
     */
    private static void updateDecelStage(AtsState st, double currentSpeed, double patternSpeed, double nextLimit) {
        boolean hasPattern = patternSpeed < Double.MAX_VALUE
                && nextLimit >= 0.0
                && currentSpeed > nextLimit;
        double ratio = (patternSpeed < Double.MAX_VALUE && patternSpeed > 0.0)
                ? currentSpeed / patternSpeed : 0.0;

        int newStage = 0;
        if (hasPattern) {
            for (int i = 0; i < STAGE_THRESHOLDS.length; i++) {
                int stage = i + 1;
                double threshold = (stage <= st.stage)
                        ? STAGE_THRESHOLDS[i] - STAGE_HYSTERESIS
                        : STAGE_THRESHOLDS[i];
                if (ratio >= threshold) newStage = stage;
            }
        }
        if (newStage > st.stage) {
            st.pendingChimes += STAGE_COUNTS[newStage - 1];
        }
        st.stage = newStage;
    }
    /**
     * パターン超過なら目標速度まで自動制動、それ以外は解除
     */
    private static void applyBrake(EntityTrainBase train, boolean overPattern, double nextLimit) {
        if (overPattern) {
            train.engageAtsBrake((float) (nextLimit / SPEED_TO_KMH));
        } else {
            train.releaseAtsBrake();
        }
    }
    // ========================================================================
    // 6. 警報・制動
    // ========================================================================
    /**
     * キューに溜まったチャイムを所定間隔で鳴らす
     */
    private static void consumeChimes(EntityTrainBase train, AtsState st) {
        if (st.pendingChimes <= 0) return;

        long now = train.world.getTotalWorldTime();
        if (now - st.lastChimeTick >= CHIME_INTERVAL_TICKS) {
            playChime(train);
            st.lastChimeTick = now;
            st.pendingChimes--;
        }
    }
    private static void playChime(EntityTrainBase train) {
        train.world.playSound(
                null,
                train.posX, train.posY, train.posZ,
                AppleSound.ATS_CHIME,
                SoundCategory.MASTER,
                1.0F, 1.0F);
    }
    // ========================================================================
    // 7. 表示同期
    // ========================================================================
    private static void syncDisplay(DataMap dm, double limit, double patternSpeed) {
        int newLimit = (limit >= 0.0) ? (int) Math.round(limit) : -1;
        int newPattern = (patternSpeed < Double.MAX_VALUE) ? (int) Math.round(patternSpeed) : -1;
        setIfChanged(dm, "atsLimit", newLimit);
        setIfChanged(dm, "atsPattern", newPattern);
    }

    private static void setIfChanged(DataMap dm, String key, int value) {
        if (dm.getInt(key) != value) {
            dm.setInt(key, value, DataMap.SYNC_FLAG);
        }
    }

    public void onAtsTick(EntityTrainBase train) {
        if (train == null) return;
        DataMap dm = train.getResourceState().getDataMap();

        // 0. ATS ON/OFF
        if (isDisabled(dm)) {
            reset(train, dm);
            return;
        }
        AtsState st = STATES.computeIfAbsent(train, t -> new AtsState());
        // 1. 区間と次信号
        SectionState section = ArisApiUtil.getSection(train);
        SignalState signal = (section != null) ? section.resolveNextSignal() : null;
        String signalId = (signal != null) ? signal.getSignalId() : null;

        double currentSpeed = Math.abs(train.getSpeed()) * SPEED_TO_KMH;
        double[] move = updateMoveVector(st, train);
        // 2. ラッチの更新
        releaseIgnoredIfSignalChanged(st, signalId);
        // 3. 次信号の目標
        Target target = resolveTarget(st, train, signal, signalId, move, currentSpeed);
        double nextLimit = target.limit;
        // 4. 現在の制限速度
        double limit = resolveLimit(st, section, nextLimit);
        saveSnapshot(st, section, limit, nextLimit);
        // 5. パターン速度
        double patternSpeed = computePatternSpeed(train, target.distance, nextLimit);
        boolean overPattern = patternSpeed < Double.MAX_VALUE
                && nextLimit >= 0.0
                && currentSpeed > patternSpeed;
        // 6. 警報・制動
        Double lastSpeed = st.lastSpeed;
        boolean accelerating = lastSpeed != null && currentSpeed > lastSpeed + ACCEL_DETECT_THRESHOLD;
        st.lastSpeed = currentSpeed;

        // 制限速度超過
        updateAccelWarning(st, currentSpeed, limit, nextLimit);

        if (!accelerating) {
            updateDecelStage(st, currentSpeed, patternSpeed, nextLimit);
        }
        consumeChimes(train, st);

        // 制動: パターン超過 or 制限速度超過。両方の場合は低い方の目標速度へ
        boolean overLimit = st.accelWarned;
        boolean holdStop = nextLimit == 0.0 && target.distance <= 0.0;
        boolean overNext = overPattern || holdStop;
        boolean brake = overNext || overLimit;
        double brakeTarget = nextLimit;
        if (overLimit) {
            brakeTarget = overNext ? Math.min(nextLimit, limit) : limit;
        }
        applyBrake(train, brake, brakeTarget);
        // 7. 表示同期
        syncDisplay(dm, limit, patternSpeed);
    }

    private static class AtsState {
        // --- 前tickのスナップショット ---
        boolean hasPrev = false;
        SectionState prevSection;
        double prevLimit = -1.0;
        double prevNextLimit = -1.0;
        /**
         * 接近中に最後に見えた「進行系」現示の制限速度(区間切替で引き継いで消費)
         */
        Double approachLimit;
        /**
         * 通過済み/間に合わない赤として無視中の信号ID
         */
        String ignoredSignal;
        /**
         * 前回位置 (x,z)
         */
        double[] lastPos;

        // --- 警報 ---
        int stage = 0;
        int pendingChimes = 0;
        long lastChimeTick = -CHIME_INTERVAL_TICKS;
        boolean accelWarned = false;
        Double lastSpeed;
    }
    /**
     * 次信号に対する目標
     */
    private static class Target {
        /**
         * 目標速度 (km/h)。無視中・信号なしは -1
         */
        final double limit;
        /**
         * 目標までの距離 (m)。無い場合は Double.MAX_VALUE
         */
        final double distance;

        Target(double limit, double distance) {
            this.limit = limit;
            this.distance = distance;
        }
    }
}
