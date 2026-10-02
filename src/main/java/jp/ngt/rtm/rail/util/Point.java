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

package jp.ngt.rtm.rail.util;

import jp.ngt.ngtlib.math.PooledVec3;
import jp.ngt.ngtlib.math.Vec3;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 分岐レールの各構成点
 */
public final class Point {
    /**
     * 分岐切替速度(tick)
     */
    private static final int MAX_COUNT = 4 * 20;

    /**
     * 分岐の根本
     */
    public final RailPosition rpRoot;
    /**
     * 本線
     */
    public final RailMapSwitch rmMain;
    /**
     * 支線
     */
    public final RailMapSwitch rmBranch;
    /**
     * rmMainに対するrmBranchの分岐方向, ->DIR_C,DIR_L,DIR_R
     */
    public final RailDir branchDir;
    /**
     * rpRootがRailMapの始点かどうか
     */
    public final boolean mainDirIsPositive, branchDirIsPositive;

    /**
     * 動き具合(本線視点)
     */
    private int moveCount;
    /**
     * レールが重なる地点
     */
    private int crossPosMain = -1;
    private int crossPosBranch = -1;
    private float crossAngle = 0.0F;

    /**
     * 分岐あり点
     * APIによる強制転換状態。null=RS入力に従う、true=REVERSE、false=NORMAL
     */
    @Nullable
    private Boolean forcedReversed = null;

    public Point(RailPosition railPos, RailMapSwitch rms1, RailMapSwitch rms2) {
        this.rpRoot = railPos;
        boolean b0 = rms1.getLength() <= rms2.getLength();//短い方を本線、長い方を支線に
        this.rmMain = b0 ? rms1 : rms2;
        this.rmBranch = b0 ? rms2 : rms1;
        this.branchDir = this.getDir(this.rpRoot, this.rmMain, this.rmBranch);

        this.mainDirIsPositive = (this.rmMain.getStartRP() == this.rpRoot);
        this.branchDirIsPositive = (this.rmBranch.getStartRP() == this.rpRoot);
    }

    /**
     * 分岐なし点
     */
    public Point(RailPosition railPos, RailMapSwitch rms1) {
        this.rpRoot = railPos;
        this.rmMain = rms1;
        this.rmBranch = null;
        this.branchDir = RailDir.NONE;

        this.mainDirIsPositive = (rms1.getStartRP() == railPos);
        this.branchDirIsPositive = false;
    }

    /** APIからの強制転換を設定する。null でRS入力に戻す */
    public void setForcedReversed(@Nullable Boolean reversed) {
        this.forcedReversed = reversed;
    }
    
    @Nullable
    public Boolean getForcedReversed() {
        return this.forcedReversed;
    }

    /** 実際に分岐が「反位」と見なされるか。API優先、なければRS入力 */
    private boolean resolveReversed(World world) {
        if (this.forcedReversed != null) {
            return this.forcedReversed;
        }
        return this.rpRoot.checkRSInput(world);
    }
    /**
     * rms1に対するrms2の向き
     */
    private RailDir getDir(RailPosition rpr, RailMapSwitch rms1, RailMapSwitch rms2) {
        RailPosition rp1 = (rms1.startRP == rpr) ? rms1.endRP : rms1.startRP;
        RailPosition rp2 = (rms2.startRP == rpr) ? rms2.endRP : rms2.startRP;
        return rpr.getDir(rp1, rp2);
    }

    /**
     * TileEntity.updateEntity()のタイミングで呼ばれる
     */
    public void onUpdate(World world) {
        boolean hasRSInput = this.resolveReversed(world);

        if (hasRSInput) {
            if (this.moveCount < MAX_COUNT) {
                ++this.moveCount;
            }
        } else {
            if (this.moveCount > 0) {
                --this.moveCount;
            }
        }
    }

    public float getMovement() {
        return (float) this.moveCount / (float) MAX_COUNT;
    }

    public RailMap getActiveRailMap(World world) {
        if (this.branchDir == RailDir.NONE) {
            return this.rmMain;
        } else {
            return this.resolveReversed(world) ? this.rmBranch : this.rmMain;
        }
    }

    public int getCrossPosMain(float hGauge, int splitM, int splitB) {
        if (this.crossPosMain < 0) {
            this.crossPosMain = this.getCrossPos(hGauge, splitM, splitB, true);
        }
        return this.crossPosMain;
    }

    public int getCrossPosBranch(float hGauge, int splitM, int splitB) {
        if (this.crossPosBranch < 0) {
            this.crossPosBranch = this.getCrossPos(hGauge, splitM, splitB, false);
        }
        return this.crossPosBranch;
    }

    public int getCrossPos(float hGauge, int splitM, int splitB, boolean isMain) {
        float vxM = hGauge * (((this.mainDirIsPositive && this.branchDir == RailDir.LEFT) || (!this.mainDirIsPositive && this.branchDir == RailDir.RIGHT)) ? 1.0F : -1.0F);
        float vxB = hGauge * (((this.branchDirIsPositive && this.branchDir == RailDir.RIGHT) || (!this.branchDirIsPositive && this.branchDir == RailDir.LEFT)) ? 1.0F : -1.0F);
        int indexM = 0;
        int indexB = 0;
        double distanceSq = Double.MAX_VALUE;

        for (int i = 0; i <= splitM; ++i) {
            double[] posM = this.rmMain.getRailPos(splitM, i);
            float yawM = this.rmMain.getRailRotation(splitM, i);
            Vec3 vecM = PooledVec3.create(vxM, 0.0D, 0.0D);
            vecM = vecM.rotateAroundY(yawM);

            for (int j = 0; j <= splitB; ++j) {
                double[] posB = this.rmBranch.getRailPos(splitB, j);
                float yawB = this.rmBranch.getRailRotation(splitB, j);
                Vec3 vecB = PooledVec3.create(vxB, 0.0D, 0.0D);
                vecB = vecB.rotateAroundY(yawB);

                double difX = (posM[1] + vecM.getX()) - (posB[1] + vecB.getX());
                double difZ = (posM[0] + vecM.getZ()) - (posB[0] + vecB.getZ());
                double d0 = difX * difX + difZ * difZ;

                if (d0 < distanceSq) {
                    this.crossAngle = MathHelper.wrapDegrees(yawM - yawB) % 90.0F;
                    indexM = i;
                    indexB = j;
                    distanceSq = d0;
                }
            }
        }

        return isMain ? indexM : indexB;
    }

    public float getCrossAngle() {
        return this.crossAngle;
    }
}