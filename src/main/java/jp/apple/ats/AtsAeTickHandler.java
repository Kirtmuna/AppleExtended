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

package jp.apple.ats;

import jp.apple.AppleSound;
import jp.apple.aris.api.ArisApiUtil;
import jp.apple.aris.ctc.state.SectionState;
import jp.apple.aris.ctc.state.SignalState;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.WeakHashMap;

public class AtsAeTickHandler {
    /** 停止目標とする、信号手前の余裕距離 (m) */
    private static final double STOP_MARGIN = 20.0;
    /**
     * 段階ごとの速度比 (現在速度 / パターン速度) 閾値
     * 0.60 = ん？ / 0.70 = うおw / 1.00 = 何やってんだよ
     */
    private static final double[] STAGE_THRESHOLDS = { 0.60, 0.70, 1.00 };
    /** 段階ごとの鳴らす回数 */
    private static final int[] STAGE_COUNTS = { 1, 2, 3 };
    /** 段階の解除マージン */
    private static final double STAGE_HYSTERESIS = 0.03;
    /** 連続再生の間隔 (tick) */
    private static final int CHIME_INTERVAL_TICKS = 5;
    /** 加速危険時のチャイム回数 (固定) */
    private static final int ACCEL_WARN_CHIME_COUNT = 3;
    /** 加速危険と判定する、制限速度への余裕 (km/h) */
    private static final double ACCEL_DANGER_MARGIN = 5.0;
    /** 加速中と判定する最小速度変化 (km/h/tick) */
    private static final double ACCEL_DETECT_THRESHOLD = 0.1;
    /** 車両ごとの現在段階 */
    private static final Map<EntityTrainBase, Integer> CURRENT_STAGE = new WeakHashMap<>();
    /** 車両ごとの残りチャイム回数 */
    private static final Map<EntityTrainBase, Integer> PENDING_CHIMES = new WeakHashMap<>();
    /** 車両ごとの最後にチャイムを鳴らした tick */
    private static final Map<EntityTrainBase, Long> LAST_CHIME_TICK = new WeakHashMap<>();
    /** 車両ごとの区間スナップショット */
    private static final Map<EntityTrainBase, SectionSnapshot> SNAPSHOT = new WeakHashMap<>();
    /** 加速危険の警告を鳴らし終えたか */
    private static final Map<EntityTrainBase, Boolean> ACCEL_WARNED = new WeakHashMap<>();
    /** 車両ごとの前tick速度 */
    private static final Map<EntityTrainBase, Double> LAST_SPEED = new WeakHashMap<>();

    private static class SectionSnapshot {
        SectionState section;
        double limit;
        double nextLimit;
    }

    public void onAtsTick(EntityTrainBase train) {
        if (train == null) return;
        // === 1. 区間と次信号 ===
        SectionState section = ArisApiUtil.getSection(train);
        SignalState signal = (section != null) ? section.resolveNextSignal() : null;
        int nextAspect = (signal != null) ? signal.getCurrentAspect() : -1;

        double nextLimit = aspectToLimit(nextAspect);

        // === 2. 現在の制限速度 ===
        SectionSnapshot prev = SNAPSHOT.get(train);
        double limit;
        if (prev == null || prev.section != section) {
            if (prev != null && prev.nextLimit >= 0.0) {
                limit = prev.nextLimit;
            } else {
                SignalState startSig = (section != null) ? section.getStartSignal() : null;
                int startAspect = (startSig != null) ? startSig.getCurrentAspect() : -1;
                limit = aspectToLimit(startAspect);
            }
        } else {
            limit = prev.limit;
        }
        SectionSnapshot snap = new SectionSnapshot();
        snap.section = section;
        snap.limit = limit;
        snap.nextLimit = nextLimit;
        SNAPSHOT.put(train, snap);

        // === 3. 次信号までの残距離 ===
        double distance = Double.MAX_VALUE;
        if (signal != null) {
            BlockPos[] positions = signal.getSignalPositions();
            if (positions != null && positions.length > 0 && positions[0] != null) {
                BlockPos pos = positions[0];
                double dx = (pos.getX() + 0.5) - train.posX;
                double dz = (pos.getZ() + 0.5) - train.posZ;
                double rawDistance = Math.sqrt(dx * dx + dz * dz);
                distance = rawDistance - STOP_MARGIN;
                if (distance < 0) distance = 0;
            }
        }
        // === 4. パターン速度 ===
        double currentSpeed = Math.abs(train.getSpeed()) * 72.0;
        double patternSpeed = computePatternSpeed(train, distance, nextLimit);

        boolean overPattern = patternSpeed < Double.MAX_VALUE
                && nextLimit >= 0.0
                && currentSpeed > patternSpeed;
        // === 5. 加速中判定 ===
        Double lastSpeedBox = LAST_SPEED.get(train);
        double lastSpeed = (lastSpeedBox != null) ? lastSpeedBox : currentSpeed;
        boolean isAccelerating = currentSpeed > lastSpeed + ACCEL_DETECT_THRESHOLD;
        LAST_SPEED.put(train, currentSpeed);
        // === 6. 加速中は加速警報のみ ===
        if (isAccelerating) {
            boolean accelDanger =
                    (limit >= 0.0 && currentSpeed >= (limit - ACCEL_DANGER_MARGIN))
                            || (nextLimit >= 0.0 && currentSpeed >= (nextLimit - ACCEL_DANGER_MARGIN));

            Boolean accelWarnedBox = ACCEL_WARNED.get(train);
            boolean accelWarned = (accelWarnedBox != null && accelWarnedBox);

            if (accelDanger) {
                if (!accelWarned) {
                    CURRENT_STAGE.put(train, 0);
                    PENDING_CHIMES.put(train, ACCEL_WARN_CHIME_COUNT);
                    ACCEL_WARNED.put(train, true);
                }
            } else {
                if (accelWarned) ACCEL_WARNED.put(train, false);
            }
            consumeChimes(train);
            
            if (overPattern) {
                train.engageAtsBrake((float) (nextLimit / 72.0));
            } else {
                train.releaseAtsBrake();
            }
            return;
        }
        ACCEL_WARNED.put(train, false);
        // === 7. 減速パターンの段階判定 ===
        double approachRatio = (patternSpeed < Double.MAX_VALUE && patternSpeed > 0.0)
                ? (currentSpeed / patternSpeed) : 0.0;

        Integer curStageBox = CURRENT_STAGE.get(train);
        int curStage = (curStageBox != null) ? curStageBox : 0;

        int newStage = 0;
        if (patternSpeed < Double.MAX_VALUE && nextLimit >= 0.0) {
            for (int i = 0; i < STAGE_THRESHOLDS.length; i++) {
                int stage = i + 1;
                double threshold = (stage <= curStage)
                        ? (STAGE_THRESHOLDS[i] - STAGE_HYSTERESIS)
                        : STAGE_THRESHOLDS[i];
                if (approachRatio >= threshold) {
                    newStage = stage;
                }
            }
        }
        if (newStage > curStage) {
            int count = STAGE_COUNTS[newStage - 1];
            Integer pending = PENDING_CHIMES.get(train);
            PENDING_CHIMES.put(train, (pending != null ? pending : 0) + count);
        }
        CURRENT_STAGE.put(train, newStage);
        // === 8. チャイム消化 ===
        consumeChimes(train);
        // === 9. 自動制動 ===
        if (overPattern) {
            train.engageAtsBrake((float) (nextLimit / 72.0));
        } else {
            train.releaseAtsBrake();
        }
    }
    /** キューに溜まったチャイムを所定間隔で鳴らす */
    private static void consumeChimes(EntityTrainBase train) {
        Integer pendingBox = PENDING_CHIMES.get(train);
        int pending = (pendingBox != null) ? pendingBox : 0;
        if (pending <= 0) return;

        long now = train.world.getTotalWorldTime();
        Long last = LAST_CHIME_TICK.get(train);
        if (last == null || (now - last) >= CHIME_INTERVAL_TICKS) {
            playChimeNow(train);
            LAST_CHIME_TICK.put(train, now);
            PENDING_CHIMES.put(train, pending - 1);
        }
    }
    /**
     * パターン速度を km/h で返す。
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
    private static double aspectToLimit(int aspect) {
        switch (aspect) {
            case 0: return 0.0;
            case 1: return 25.0;
            case 2: return 45.0;
            case 3: return 75.0;
            case 4: return 130.0;
            case 5: return 160.0;
            default: return -1.0;
        }
    }
    private static void playChimeNow(EntityTrainBase train) {
        train.world.playSound(
                null,
                train.posX, train.posY, train.posZ,
                AppleSound.ATS_CHIME,
                SoundCategory.MASTER,
                1.0F, 1.0F);
    }
}
