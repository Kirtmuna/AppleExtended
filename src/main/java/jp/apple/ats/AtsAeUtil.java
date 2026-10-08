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

import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetTrain;

public class AtsAeUtil {
    /**
     * 車両の常用最大減速度を取得するメソッド
     */
    public static double getMaxDeceleration(EntityTrainBase train) {
        if (train.getResourceState() != null && train.getResourceState().getResourceSet() != null) {
            ModelSetTrain modelSet = train.getResourceState().getResourceSet();
            if (modelSet.getConfig() != null) {
                TrainConfig config = modelSet.getConfig();
                if (config.deccelerations != null && config.deccelerations.length > 0) {
                    int index = config.deccelerations.length > 1 ? config.deccelerations.length - 2 : 0;
                    float rtmDecel = config.deccelerations[index];
                    return Math.abs(rtmDecel) * 400.0;
                }
            }
        }
        return 0.0;
    }
}
