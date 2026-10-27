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

package jp.ngt.rtm.modelpack.cfg;

import java.util.Arrays;

public class TrainConfig extends VehicleBaseConfig implements IConfigWithType {
    private String trainName;
    private String trainType;

    private ModelSource trainModel2;
    private ModelSource[] bogieModel3;

    @Deprecated
    private ModelSource bogieModel2;
    @Deprecated
    private String trainModel;
    @Deprecated
    private String bogieModel;
    @Deprecated
    private String trainTexture;
    @Deprecated
    private String bogieTexture;

    public String sound_BrakeRelease;
    public String sound_BrakeRelease2;
    public boolean muteJointSound;
    public float[][] jointDelay;

    public boolean isSingleTrain;

    private float[][] bogiePos;
    public float trainDistance;
    /**
     * 加速度<br>
     * N km/h/s -> N x 0.0006944
     */
    public float accelerateion;

    /**
     * ノッチごとの速度上限(1~5段)
     */
    public float[] maxSpeed;

    public float rolling;

    public float[][] pantoPos;

    public float rollSpeedCoefficient;
    public float rollVariationCoefficient;
    public float rollWidthCoefficient;

    public float[] accelerateions;
    public float[] deccelerations;

    public boolean useVariableAcceleration;
    public boolean useVariableDeceleration;

    @Override
    public void init() {
        super.init();

        if (this.trainModel2 == null) {
            this.trainModel2 = new ModelSource();
            this.trainModel2.modelFile = this.trainModel;
            this.trainModel2.textures = new String[][]{{"default", this.trainTexture}};
        }

        if (this.bogieModel3 == null) {
            this.bogieModel3 = new ModelSource[2];
            if (this.bogieModel2 != null) {
                this.bogieModel3[0] = this.bogieModel3[1] = this.bogieModel2;
            } else {
                ModelSource model = new ModelSource();
                model.modelFile = this.bogieModel;
                model.textures = new String[][]{{"default", this.bogieTexture}};
                this.bogieModel3[0] = this.bogieModel3[1] = model;
            }
        }

        this.sound_BrakeRelease = this.fixSoundPath(this.sound_BrakeRelease);
        this.sound_BrakeRelease2 = this.fixSoundPath(this.sound_BrakeRelease2);

        if (this.bogiePos == null) {
            this.bogiePos = new float[][]{{0.0F, 0.0F, 7.125F}, {0.0F, 0.0F, -7.125F}};
        }

        if (this.trainDistance <= 0.0F) {
            this.trainDistance = 10.125F;
        }

        if (this.accelerateion <= 0.0F) {
            this.accelerateion = 0.001736F;
        }

        if (this.maxSpeed == null || this.maxSpeed.length < 5) {
            this.maxSpeed = new float[]{0.36F, 0.72F, 1.08F, 1.44F, 1.80F};
        }

        if (this.accelerateions == null || (!this.notDisplayCab && this.accelerateions.length != this.maxSpeed.length)) {
            this.accelerateions = new float[this.maxSpeed.length];
            Arrays.fill(this.accelerateions, this.accelerateion);
        }

        if (this.deccelerations == null || (!this.notDisplayCab && this.deccelerations.length != 9)) {
            this.deccelerations = new float[]{-0.0002F, -0.0005F, -0.001F, -0.0015F, -0.002F, -0.0025F, -0.003F, -0.0035F, -0.01F};
        }

        this.rolling *= 5.0F;

        if (this.jointDelay == null) {
            float f0 = 1.9F;
            this.jointDelay = new float[][]{{0.0F, f0}, {0.0F, f0}};
        }

        if (this.serverScriptPath == null) {
            this.useVariableAcceleration = false;
            this.useVariableDeceleration = false;
        }
    }

    public boolean isNotchInRange(int notch) {
        if (notch < 0)
            return -notch <= deccelerations.length - 1;
        else
            return notch <= maxSpeed.length;
    }

    @Override
    public String getName() {
        return this.trainName;
    }

    @Override
    public ModelSource getModel() {
        return this.trainModel2;
    }

    public ModelSource getBogieModel(int par1) {
        return this.bogieModel3[par1];
    }

    public float[][] getBogiePos() {
        return this.bogiePos;
    }

    public static TrainConfig getDummyConfig() {
        TrainConfig config = new TrainConfig();
        config.trainName = "Dummy";
        config.trainType = "N";
        config.init();
        return config;
    }

    @Override
    public String getSubType() {
        return this.trainType;
    }
}