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

package jp.apple.appleextended.highlight;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class BlockHighlightRenderer {

    private static final int SCAN_RADIUS = 16;

    private BlockHighlightRenderer() {
    }

    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        for (HighlightEntry entry : HighlightRegistry.getEntries()) {
            if (!isHoldingMatchingItem(mc.player, entry)) continue;

            List<AxisAlignedBB> boxes = collectBoxes(mc.world, mc.player.getPosition(), entry);
            if (boxes.isEmpty()) continue;

            renderHighlights(event.getPartialTicks(), mc, boxes, entry);
        }
    }

    private static boolean isHoldingMatchingItem(EntityPlayer player, HighlightEntry entry) {
        return matches(player.getHeldItemMainhand(), entry)
                || matches(player.getHeldItemOffhand(), entry);
    }

    private static boolean matches(ItemStack stack, HighlightEntry entry) {
        return !stack.isEmpty() && entry.itemPredicate.test(stack);
    }

    private static List<AxisAlignedBB> collectBoxes(World world, BlockPos center, HighlightEntry entry) {
        List<AxisAlignedBB> result = new ArrayList<>();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    Block block = world.getBlockState(pos).getBlock();
                    if (entry.blockPredicate.test(block)) {
                        result.add(world.getBlockState(pos)
                                .getBoundingBox(world, pos)
                                .offset(pos));
                    }
                }
            }
        }
        return result;
    }

    private static void renderHighlights(float partialTicks, Minecraft mc,
                                         List<AxisAlignedBB> boxes, HighlightEntry entry) {
        double cx = mc.player.prevPosX + (mc.player.posX - mc.player.prevPosX) * partialTicks;
        double cy = mc.player.prevPosY + (mc.player.posY - mc.player.prevPosY) * partialTicks;
        double cz = mc.player.prevPosZ + (mc.player.posZ - mc.player.prevPosZ) * partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-cx, -cy, -cz);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(entry.lineWidth);
        GlStateManager.color(entry.r, entry.g, entry.b, entry.a);

        for (AxisAlignedBB box : boxes) {
            drawAABBOutline(box);
        }

        GL11.glLineWidth(1.0F);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void drawAABBOutline(AxisAlignedBB b) {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(b.minX, b.minY, b.minZ);
        GL11.glVertex3d(b.maxX, b.minY, b.minZ);
        GL11.glVertex3d(b.maxX, b.minY, b.minZ);
        GL11.glVertex3d(b.maxX, b.minY, b.maxZ);
        GL11.glVertex3d(b.maxX, b.minY, b.maxZ);
        GL11.glVertex3d(b.minX, b.minY, b.maxZ);
        GL11.glVertex3d(b.minX, b.minY, b.maxZ);
        GL11.glVertex3d(b.minX, b.minY, b.minZ);
        GL11.glVertex3d(b.minX, b.maxY, b.minZ);
        GL11.glVertex3d(b.maxX, b.maxY, b.minZ);
        GL11.glVertex3d(b.maxX, b.maxY, b.minZ);
        GL11.glVertex3d(b.maxX, b.maxY, b.maxZ);
        GL11.glVertex3d(b.maxX, b.maxY, b.maxZ);
        GL11.glVertex3d(b.minX, b.maxY, b.maxZ);
        GL11.glVertex3d(b.minX, b.maxY, b.maxZ);
        GL11.glVertex3d(b.minX, b.maxY, b.minZ);
        GL11.glVertex3d(b.minX, b.minY, b.minZ);
        GL11.glVertex3d(b.minX, b.maxY, b.minZ);
        GL11.glVertex3d(b.maxX, b.minY, b.minZ);
        GL11.glVertex3d(b.maxX, b.maxY, b.minZ);
        GL11.glVertex3d(b.maxX, b.minY, b.maxZ);
        GL11.glVertex3d(b.maxX, b.maxY, b.maxZ);
        GL11.glVertex3d(b.minX, b.minY, b.maxZ);
        GL11.glVertex3d(b.minX, b.maxY, b.maxZ);
        GL11.glEnd();
    }
}