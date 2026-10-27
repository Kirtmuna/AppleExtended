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
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Predicate;

@SideOnly(Side.CLIENT)
public class HighlightEntry {
    public final Predicate<ItemStack> itemPredicate;
    public final Predicate<Block> blockPredicate;
    public final float r, g, b, a;
    public final float lineWidth;

    private HighlightEntry(Builder builder) {
        this.itemPredicate = builder.itemPredicate;
        this.blockPredicate = builder.blockPredicate;
        this.r = builder.r;
        this.g = builder.g;
        this.b = builder.b;
        this.a = builder.a;
        this.lineWidth = builder.lineWidth;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Predicate<ItemStack> itemPredicate;
        private Predicate<Block> blockPredicate;
        private float r = 0.0F, g = 1.0F, b = 0.0F, a = 0.6F;
        private float lineWidth = 2.0F;

        public Builder item(Predicate<ItemStack> predicate) {
            this.itemPredicate = predicate;
            return this;
        }

        public Builder block(Predicate<Block> predicate) {
            this.blockPredicate = predicate;
            return this;
        }

        public Builder color(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
            return this;
        }

        public Builder alpha(float a) {
            this.a = a;
            return this;
        }

        public Builder lineWidth(float width) {
            this.lineWidth = width;
            return this;
        }

        public HighlightEntry build() {
            if (itemPredicate == null) throw new IllegalStateException("itemPredicate is required");
            if (blockPredicate == null) throw new IllegalStateException("blockPredicate is required");
            return new HighlightEntry(this);
        }
    }
}
