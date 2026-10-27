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

package jp.ngt.ngtlib.item.craft;

import com.anatawa12.fixRtm.UtilsKt;
import com.anatawa12.fixRtm.ngtlib.item.craft.OreDictionaryUtil;
import jp.ngt.ngtlib.item.ItemUtil;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class ShapedRecipes55 extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    private final int recipeWidth;
    private final int recipeHeight;
    private final ItemStack[] recipeItems;
    private final ItemStack recipeOutput;

    public ShapedRecipes55(int width, int height, ItemStack[] items, ItemStack output) {
        this.recipeWidth = width;
        this.recipeHeight = height;
        this.recipeItems = items;
        this.recipeOutput = output;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.recipeOutput;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            nonnulllist.set(i, ForgeHooks.getContainerItem(itemstack));
        }

        return nonnulllist;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public boolean canFit(int width, int height) {
        return width >= this.recipeWidth && height >= this.recipeHeight;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        for (int i = 0; i <= 5 - this.recipeWidth; ++i) {
            for (int j = 0; j <= 5 - this.recipeHeight; ++j) {
                if (this.checkMatch(inv, i, j, true)) {
                    return true;
                }

                if (this.checkMatch(inv, i, j, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkMatch(InventoryCrafting inv, int startX, int startY, boolean mirror) {
        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 5; ++j) {
                int k = i - startX;
                int l = j - startY;
                ItemStack itemstack = ItemStack.EMPTY;

                if (k >= 0 && l >= 0 && k < this.recipeWidth && l < this.recipeHeight) {
                    if (mirror) {
                        itemstack = this.recipeItems[this.recipeWidth - k - 1 + l * this.recipeWidth];
                    } else {
                        itemstack = this.recipeItems[k + l * this.recipeWidth];
                    }
                }

                ItemStack itemstack1 = inv.getStackInRowAndColumn(i, j);

                if (!this.itemMatches(itemstack, itemstack1)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack itemstack = this.getRecipeOutput().copy();
        return itemstack;
    }

    public ItemStack[] getRecipeItems() {
        ItemStack[] aitemstack = UtilsKt.arrayOfItemStack(25);

        for (int i = 0; i < 5 && i < this.recipeHeight; ++i) {
            for (int j = 0; j < 5 && j < this.recipeWidth; ++j) {
                aitemstack[i * 5 + j] = this.recipeItems[i * this.recipeWidth + j];
            }
        }

        return aitemstack;
    }

    private boolean itemMatches(ItemStack target, ItemStack inInventory) {
        if ((inInventory != null || target == null) && (inInventory == null || target != null)) {
            if (target.getItem() == Items.DYE) {
                return ItemUtil.isItemEqual(target, inInventory);
            } else {
                int[] aint = OreDictionaryUtil.getOreIDs(target);
                int[] aint1 = OreDictionaryUtil.getOreIDs(inInventory);
                if (aint.length > 0 && aint1.length > 0) {
                    for (int i = 0; i < aint.length; ++i) {
                        for (int j = 0; j < aint1.length; ++j) {
                            if (aint[i] == aint1[j]) {
                                return true;
                            }
                        }
                    }

                    return false;
                } else {
                }
                return ItemUtil.isItemEqual(target, inInventory);
            }
        } else {
            return false;
        }
    }
}