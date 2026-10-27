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

import com.anatawa12.fixRtm.FixRtm;
import com.anatawa12.fixRtm.rtm.modelpack.init.ExModelPackConstructThread;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.util.NGTUtilClient;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.init.ProgressStateHolder.ProgressState;
import jp.ngt.rtm.network.PacketModelPack;
import jp.ngt.rtm.network.PacketNotice;
import net.minecraft.crash.CrashReport;
import net.minecraftforge.fml.relauncher.Side;

import javax.swing.*;
import java.awt.*;

public final class ModelPackLoadThread extends Thread {
    private final Side threadSide;
    private final boolean displayWindow;

    private MPLFrame mainFrame;
    private MPLAdButton adButton;
    private JProgressBar[] bars;
    private JLabel[] labels;
    private int[] maxValue;

    public boolean finished;
    public boolean loadFinished;

    private boolean debug = false;

    public ModelPackLoadThread(Side par1) {
        super("RTM ModelPack Load");
        this.threadSide = par1;

        this.displayWindow = (par1 == Side.CLIENT) && !GraphicsEnvironment.isHeadless();
        if (this.displayWindow) {
            this.initWindow();
        }
    }

    private void initWindow() {
        int size = 2;
        this.bars = new JProgressBar[size];
        this.labels = new JLabel[size];
        this.maxValue = new int[size];

        int scWidth = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
        int scale = scWidth / 1024;
        float fontSize = 16.0F;

        this.mainFrame = new MPLFrame(scale);
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.BLACK);

        JPanel adPanel = new JPanel();
        adPanel.setBackground(Color.BLACK);
        adPanel.setLayout(new BoxLayout(adPanel, BoxLayout.X_AXIS));
        this.adButton = new MPLAdButton(scale);
        JButton buttonL = new MPLMoveButton(this.adButton, -1, scale);
        JButton buttonR = new MPLMoveButton(this.adButton, 1, scale);
        adPanel.add(buttonL);
        adPanel.add(this.adButton);
        adPanel.add(buttonR);
        mainPanel.add(adPanel);

        JPanel barPanel1 = new MPLBarPanel(
                this.labels[0] = new JLabel("Start Loading"),
                this.bars[0] = new JProgressBar(), scale);
        mainPanel.add(barPanel1);

        JPanel barPanel2 = new MPLBarPanel(
                this.labels[1] = new JLabel("Ready"),
                this.bars[1] = new JProgressBar(), scale);
        mainPanel.add(barPanel2);

        this.mainFrame.getContentPane().add(mainPanel);
    }

    @Override
    public void run() {
        if (this.displayWindow) {
            this.mainFrame.setVisible(true);
        }

        try {
            Thread winUpdate = new Thread() {
                private int count;

                @Override
                public void run() {
                    while (!finished) {
                        if (displayWindow) {
                            ++this.count;
                            if (this.count >= 30) {
                                adButton.changeImage(1);
                                this.count = 0;
                            }
                        }

                        try {
                            sleep(150L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            winUpdate.start();

            this.runThread();
        } catch (Throwable e) {
            this.finish();
            CrashReport crashreport = CrashReport.makeCrashReport(e, "Loading RTM ModelPack");
            crashreport.makeCategory("Initialization");
            if (this.threadSide == Side.CLIENT) {
                crashreport = NGTUtilClient.getMinecraft().addGraphicsAndWorldToCrashReport(crashreport);
                NGTUtilClient.getMinecraft().displayCrashReport(crashreport);
            } else {
                FixRtm.INSTANCE.reportCrash(crashreport);
            }
        } finally {
            if (this.displayWindow && !this.debug) {
                this.mainFrame.dispose();
            }
        }
    }

    private void runThread() throws InterruptedException {
        if (this.threadSide == Side.CLIENT && RTMCore.useServerModelPack) {
            this.setBarMaxValue(ProgressStateHolder.BAR_MAIN, 0, "Waiting for connecting to Server");
            this.setBarMaxValue(ProgressStateHolder.BAR_SUB, 0, "You can start game");

            while (RTMCore.proxy.getConnectionState() == 0) {
                RTMCore.NETWORK_WRAPPER.sendToServer(new PacketNotice(PacketNotice.Side_SERVER, "getModelPack"));
                sleep(500L);
            }

            while (!PacketModelPack.MP_WRITER.finish) {
                sleep(500L);
            }
        }

        this.setBarMaxValue(ProgressStateHolder.BAR_MAIN, ProgressState.values().length, "");


        NGTLog.startTimer();
        ExModelPackConstructThread thread2 = new ExModelPackConstructThread(this.threadSide, this);
        thread2.start();
        ModelPackManager.INSTANCE.load(this);
        while (!thread2.setFinish()) {
            sleep(500L);
        }
        this.finish();
        ModelPackManager.INSTANCE.modelConstructed = true;
        NGTLog.stopTimer("Model load time");
    }

    public void finish() {
        this.finished = true;
    }

    public void setBarMaxValue(int barId, int value, String label) {
        if (!this.displayWindow) {
            return;
        }

        if (value > 0) {
            if (this.bars[barId].isIndeterminate()) {
                this.bars[barId].setIndeterminate(false);
            }
            this.maxValue[barId] = value;
        } else {
            this.bars[barId].setIndeterminate(true);
        }

        if (label != null && label.length() > 0) {
            this.labels[barId].setText(label);
        }
    }

    public void setBarValue(int barId, ProgressState state) {
        this.setBarValue(barId, state.ordinal(), state.message);
    }

    public void setBarValue(int barId, int value, String label) {
        if (!this.displayWindow) {
            return;
        }

        int max = this.maxValue[barId];
        int i = (int) ((float) value / (float) max * 100.0F);
        this.bars[barId].setValue(i);
        this.bars[barId].setStringPainted(true);
        this.bars[barId].setString(String.format("%d/%d", value, max));

        if (label != null && label.length() > 0) {
            this.labels[barId].setText(label);
        }
    }
}