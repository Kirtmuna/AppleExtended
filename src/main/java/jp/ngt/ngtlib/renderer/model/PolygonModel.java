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

package jp.ngt.ngtlib.renderer.model;

import com.anatawa12.fixRtm.ngtlib.renderer.model.PolygonModelCharsetDetector;
import jp.ngt.ngtlib.renderer.NGTTessellator;
import kotlin.Pair;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@SideOnly(Side.CLIENT)
public abstract class PolygonModel implements IModelNGT {
    protected String fileName;
    protected int drawMode;
    public VecAccuracy accuracy;
    protected float[] sizeBox = new float[6];

    public final List<Vertex> vertices = new ArrayList<>(1024);
    public final List<GroupObject> groupObjects = new ArrayList<>(16);
    protected GroupObject currentGroupObject;

    protected PolygonModel() {
    }

    protected PolygonModel(String name, int mode, VecAccuracy par3) {
        this.fileName = name;
        this.drawMode = mode;
        this.accuracy = par3;
    }

    public PolygonModel(InputStream[] is, String name, int mode, VecAccuracy par3) throws ModelFormatException {
        this.fileName = name;
        this.drawMode = mode;
        this.accuracy = par3;
        this.init(is);
        this.calcVertexNormals();
        this.vertices.clear();
    }

    protected void init(InputStream[] is) throws ModelFormatException {
        this.loadModel(is[0]);
    }

    int lineCount;
    static Pattern repS = Pattern.compile("\\s+");

    private void loadModel(InputStream inputStream) {
        Pair<Charset, InputStream> pair = PolygonModelCharsetDetector.INSTANCE.detectCharset(inputStream);
        inputStream = pair.component2();
        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(inputStream, pair.component1()));
        Stream<String> stream = bufferedreader.lines();
        stream.forEachOrdered(line -> {
            line = repS.matcher(line).replaceAll(" ").trim();
            this.parseLine(line, ++this.lineCount);
        });
        stream.close();
        this.postInit();
    }

    protected abstract void parseLine(String currentLine, int lineCount);

    protected abstract void postInit();

    private final void calcVertexNormals() {
        for (GroupObject obj : this.groupObjects) {
            obj.calcVertexNormals(this.accuracy);
        }
    }

    protected final void calcSizeBox(Vertex vtx) {
        if (vtx.getX() < this.sizeBox[0]) {
            this.sizeBox[0] = vtx.getX();
        } else if (vtx.getX() > this.sizeBox[3]) {
            this.sizeBox[3] = vtx.getX();
        }

        if (vtx.getY() < this.sizeBox[1]) {
            this.sizeBox[1] = vtx.getY();
        } else if (vtx.getY() > this.sizeBox[4]) {
            this.sizeBox[4] = vtx.getY();
        }

        if (vtx.getZ() < this.sizeBox[2]) {
            this.sizeBox[2] = vtx.getZ();
        } else if (vtx.getZ() > this.sizeBox[5]) {
            this.sizeBox[5] = vtx.getZ();
        }
    }

    @Override
    public final float[] getSize() {
        return this.sizeBox;
    }

    @Override
    public final void renderAll(boolean smoothing) {
        if (smoothing) {
            GL11.glShadeModel(GL11.GL_SMOOTH);
        }

        NGTTessellator tessellator = NGTTessellator.instance;
        tessellator.startDrawing(this.drawMode);
        this.tessellateAll(tessellator, smoothing);
        tessellator.draw();

        if (smoothing) {
            GL11.glShadeModel(GL11.GL_FLAT);
        }
    }

    public final void tessellateAll(NGTTessellator tessellator, boolean smoothing) {
        for (GroupObject groupObject : this.groupObjects) {
            groupObject.render(tessellator, smoothing);
        }
    }

    @Override
    public final void renderOnly(boolean smoothing, String... groupNames) {
        if (smoothing) {
            GL11.glShadeModel(GL11.GL_SMOOTH);
        }

        for (GroupObject groupObject : this.groupObjects) {
            for (String groupName : groupNames) {
                if (groupName.equalsIgnoreCase(groupObject.name)) {
                    groupObject.render(smoothing);
                }
            }
        }

        if (smoothing) {
            GL11.glShadeModel(GL11.GL_FLAT);
        }
    }

    @Override
    public final void renderPart(boolean smoothing, String partName) {
        if (smoothing) {
            GL11.glShadeModel(GL11.GL_SMOOTH);
        }

        for (GroupObject groupObject : this.groupObjects) {
            if (partName.equalsIgnoreCase(groupObject.name)) {
                groupObject.render(smoothing);
                return;
            }
        }

        if (smoothing) {
            GL11.glShadeModel(GL11.GL_FLAT);
        }
    }

    @Override
    public final int getDrawMode() {
        return this.drawMode;
    }

    @Override
    public final List<GroupObject> getGroupObjects() {
        return this.groupObjects;
    }

    protected final float getFloat(String s) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return 0.0F;
        }
    }

    private final List<String> tempList = new ArrayList<>();

    protected final String[] split(String target, char regex) {
        this.tempList.clear();
        int index = 0;
        while (index >= 0) {
            int nextHit = target.indexOf(regex, index);
            if (nextHit < 0) {
                this.tempList.add(target.substring(index, target.length()));
                break;
            } else if (index < nextHit) {
                this.tempList.add(target.substring(index, nextHit));
            }
            index = nextHit + 1;
        }
        return this.tempList.toArray(new String[this.tempList.size()]);
    }

    protected final String[] split(String target, String regex) {
        this.tempList.clear();
        int index = 0;
        while (index >= 0) {
            int nextHit = target.indexOf(regex, index);
            if (nextHit < 0) {
                this.tempList.add(target.substring(index, target.length()));
                break;
            } else if (index < nextHit) {
                this.tempList.add(target.substring(index, nextHit));
            }
            index = nextHit + regex.length();
        }
        return this.tempList.toArray(new String[this.tempList.size()]);
    }
}