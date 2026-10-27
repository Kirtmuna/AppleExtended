package jp.apple.appleextended.script.api.gif;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GifAnimation {
    private final List<BufferedImage> rawFrames = new ArrayList<>();
    private final List<DynamicTexture> frames = new ArrayList<>();
    private final List<Integer> delays = new ArrayList<>();
    private int totalDuration = 0;
    private boolean uploaded = false;

    public GifAnimation(InputStream stream) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(stream);
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) throw new IOException("No GIF reader found");

        ImageReader reader = readers.next();
        reader.setInput(iis);

        int numImages = reader.getNumImages(true);

        BufferedImage masterImage = null;
        Graphics2D g2d = null;

        String prevDisposalMethod = "none";
        int prevLeft = 0, prevTop = 0, prevWidth = 0, prevHeight = 0;

        for (int i = 0; i < numImages; i++) {
            BufferedImage image = reader.read(i);

            IIOMetadata metadata = reader.getImageMetadata(i);
            String metaFormat = metadata.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormat);

            IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
            String disposalMethod = "none";
            int delay = 100;

            if (gce != null) {
                disposalMethod = gce.getAttribute("disposalMethod");
                int d = Integer.parseInt(gce.getAttribute("delayTime"));
                delay = (d <= 1) ? 100 : d * 10;
            }

            int left = 0, top = 0;
            if (root.getElementsByTagName("ImageDescriptor").getLength() > 0) {
                IIOMetadataNode idNode = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
                left = Integer.parseInt(idNode.getAttribute("imageLeftPosition"));
                top = Integer.parseInt(idNode.getAttribute("imageTopPosition"));
            }

            if (i == 0) {
                masterImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                g2d = masterImage.createGraphics();
            } else {
                if ("restoreToBackgroundColor".equals(prevDisposalMethod)) {
                    g2d.setComposite(AlphaComposite.Clear);
                    g2d.fillRect(prevLeft, prevTop, prevWidth, prevHeight);
                    g2d.setComposite(AlphaComposite.SrcOver);
                }
            }

            g2d.drawImage(image, left, top, null);

            BufferedImage frameCopy = new BufferedImage(masterImage.getWidth(), masterImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D copyG = frameCopy.createGraphics();
            copyG.drawImage(masterImage, 0, 0, null);
            copyG.dispose();
            rawFrames.add(frameCopy);

            this.delays.add(delay);
            this.totalDuration += delay;

            prevDisposalMethod = disposalMethod;
            prevLeft = left;
            prevTop = top;
            prevWidth = image.getWidth();
            prevHeight = image.getHeight();
        }

        if (g2d != null) {
            g2d.dispose();
        }

        reader.dispose();
        iis.close();
    }

    private void ensureUploaded() {
        if (uploaded) return;
        for (BufferedImage img : rawFrames) {
            frames.add(new DynamicTexture(img));
        }
        rawFrames.clear();
        uploaded = true;
    }

    public void render(float[][] vertices) {
        if (this.totalDuration == 0 || vertices == null || vertices.length < 4) return;
        for (int i = 0; i < 4; i++) {
            if (vertices[i] == null || vertices[i].length < 3) return;
        }

        ensureUploaded();
        if (frames.isEmpty()) return;

        long relTime = System.currentTimeMillis() % this.totalDuration;
        int currentFrame = frames.size() - 1;
        long sum = 0;
        for (int i = 0; i < this.delays.size(); i++) {
            sum += this.delays.get(i);
            if (relTime < sum) {
                currentFrame = i;
                break;
            }
        }

        DynamicTexture texture = this.frames.get(currentFrame);

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.bindTexture(texture.getGlTextureId());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buf = tessellator.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_TEX);
        buf.pos(vertices[0][0], vertices[0][1], vertices[0][2]).tex(0.0, 0.0).endVertex();
        buf.pos(vertices[1][0], vertices[1][1], vertices[1][2]).tex(1.0, 0.0).endVertex();
        buf.pos(vertices[2][0], vertices[2][1], vertices[2][2]).tex(1.0, 1.0).endVertex();
        buf.pos(vertices[3][0], vertices[3][1], vertices[3][2]).tex(0.0, 1.0).endVertex();
        tessellator.draw();

        GlStateManager.bindTexture(0);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}