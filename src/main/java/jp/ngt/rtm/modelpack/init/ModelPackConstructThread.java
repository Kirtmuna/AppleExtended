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

package jp.ngt.rtm.modelpack.init;

import com.anatawa12.fixRtm.rtm.modelpack.ModelState;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.util.NGTUtilClient;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.init.ProgressStateHolder.ProgressState;
import jp.ngt.rtm.modelpack.modelset.ResourceSet;
import net.minecraft.crash.CrashReport;
import net.minecraftforge.fml.relauncher.Side;

public class ModelPackConstructThread extends Thread {
    private final Side threadSide;
    private final ModelPackLoadThread parent;
    private boolean loading;
    private int index;

    private boolean barStateChanged;

    public ModelPackConstructThread(Side par1, ModelPackLoadThread par2) {
        super("RTM ModelPack Construct");
        this.threadSide = par1;
        this.parent = par2;
        this.loading = true;
    }

    @Override
    public void run() {
        try {
            this.runThread();
        } catch (Throwable e) {
            if (this.threadSide == Side.CLIENT) {
                CrashReport crashReport = CrashReport.makeCrashReport(e, "Constructing RTM ModelPack");
                crashReport.makeCategory("Initialization");
                crashReport = NGTUtilClient.getMinecraft().addGraphicsAndWorldToCrashReport(crashReport);
                NGTUtilClient.getMinecraft().displayCrashReport(crashReport);
            } else {
                e.printStackTrace();
            }
        }
    }

    private void runThread() throws InterruptedException {
        while (this.loading) {
            while (true) {
                ResourceSet set = ModelPackManager.INSTANCE.unconstructSets.poll();
                if (set == null) break;
                if (this.threadSide == Side.SERVER) {
                    set.constructOnServer();
                } else {
                    set.constructOnClient();
                    set.finishConstruct();
                }
                set.state = ModelState.CONSTRUCTED;
                NGTLog.debug("Construct Model : %s (%d / %d)", set.getConfig().getName(),
                        this.index + 1, ModelPackManager.INSTANCE.unconstructSets.size());
                ++this.index;

                if (this.parent.loadFinished) {
                    if (!this.barStateChanged) {
                        this.parent.setBarValue(ProgressStateHolder.BAR_MAIN, ProgressState.CONSTRUCTING_MODEL);
                        int size = ModelPackManager.INSTANCE.unconstructSets.size();
                        this.parent.setBarMaxValue(ProgressStateHolder.BAR_SUB, size, "");
                        this.barStateChanged = true;
                    }

                    this.parent.setBarValue(ProgressStateHolder.BAR_SUB, this.index, set.getConfig().getName());
                }
            }

            sleep(500L);
        }
    }

    public boolean setFinish() {
        if (ModelPackManager.INSTANCE.unconstructSets.size() == this.index) {
            ModelPackManager.INSTANCE.unconstructSets.clear();
            ModelPackManager.INSTANCE.clearCache();
            this.loading = false;
            return true;
        }
        return false;
    }
}