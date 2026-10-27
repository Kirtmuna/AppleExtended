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
import jp.ngt.ngtlib.item.ItemUtil;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.NonNullList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.*;

public final class RecipeManager {
    public static final RecipeManager INSTANCE = new RecipeManager();

    private List<IRecipe> recipes = new ArrayList<>();

    private RecipeManager() {
    }

    @SubscribeEvent
    public void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        IRecipe[] array = this.recipes.toArray(new IRecipe[this.recipes.size()]);
        event.getRegistry().registerAll(array);
    }

    public static ShapedRecipes55 addRecipe(ItemStack output, Object... objs) {
        String pattern = "";
        int i = 0;
        int w = 0;
        int h = 0;

        if (objs[i] instanceof String[]) {
            String[] astring = (String[]) ((String[]) objs[i++]);

            for (int l = 0; l < astring.length; ++l) {
                String s1 = astring[l];
                ++h;
                w = s1.length();
                pattern = pattern + s1;
            }
        } else {
            while (objs[i] instanceof String) {
                String s2 = (String) objs[i++];
                ++h;
                w = s2.length();
                pattern = pattern + s2;
            }
        }

        Map<Character, ItemStack> map = new HashMap();
        for (; i < objs.length; i += 2) {
            Character character = (Character) objs[i];
            ItemStack itemstack1 = null;

            if (objs[i + 1] instanceof Item) {
                itemstack1 = new ItemStack((Item) objs[i + 1]);
            } else if (objs[i + 1] instanceof Block) {
                itemstack1 = new ItemStack((Block) objs[i + 1], 1, 32767);
            } else if (objs[i + 1] instanceof ItemStack) {
                itemstack1 = (ItemStack) objs[i + 1];
            }

            map.put(character, itemstack1);
        }

        ItemStack[] stacks = new ItemStack[w * h];

        for (int i1 = 0; i1 < w * h; ++i1) {
            char c0 = pattern.charAt(i1);

            if (map.containsKey(c0)) {
                stacks[i1] = map.get(c0).copy();
            } else {
                stacks[i1] = ItemStack.EMPTY;
            }
        }

        ShapedRecipes55 recipe = new ShapedRecipes55(w, h, stacks, output);
        RecipeManager.INSTANCE.addRecipeToManager(recipe);
        return recipe;
    }

    public IRecipe addRecipeToManager(IRecipe recipe) {
        this.recipes.add(recipe);
        return recipe;
    }

    public List<IRecipe> getRecipeList() {
        return this.recipes;
    }

    public IRecipe getRecipe(ItemStack par1) {
        Iterator<IRecipe> iterator = CraftingManager.REGISTRY.iterator();
        while (iterator.hasNext()) {
            IRecipe recipe = iterator.next();
            ItemStack output = recipe.getRecipeOutput();
            if (output != null && ItemUtil.isItemEqual(output, par1)) {
                return recipe;
            }
        }
        return null;
    }

    public ItemStack[] getRecipeItems(IRecipe par1) {
        ItemStack[] items = UtilsKt.arrayOfItemStack(25);

        if (par1 instanceof ShapedRecipes55) {
            ShapedRecipes55 recipe = (ShapedRecipes55) par1;
            return recipe.getRecipeItems();
        } else if (par1 instanceof RepairRecipe) {
            RepairRecipe recipe = (RepairRecipe) par1;
            return recipe.getToolAndMaterial(25);
        } else if (par1 instanceof ShapedRecipes) {
            ShapedRecipes recipe = (ShapedRecipes) par1;
            for (int i = 0; i < 5; ++i) {
                if (i < recipe.recipeHeight) {
                    for (int j = 0; j < 5; ++j) {
                        if (j < recipe.recipeWidth) {
                            int index = i * recipe.recipeWidth + j;
                            items[i * 5 + j] = recipe.recipeItems.get(index).getMatchingStacks()[0];
                        }
                    }
                }
            }
            return items;
        } else if (par1 instanceof ShapelessRecipes) {
            ShapelessRecipes recipe = (ShapelessRecipes) par1;
            ArrayList arraylist = new ArrayList(recipe.recipeItems);

            for (int i = 0; i < 5; ++i) {
                for (int j = 0; j < 5; ++j) {
                    if (arraylist.isEmpty()) {
                        return items;
                    }

                    int k = i * 5 + j;
                    items[k] = (ItemStack) arraylist.get(0);
                    arraylist.remove(0);
                }
            }
        } else if (par1 instanceof ShapedOreRecipe) {
            ShapedOreRecipe recipe = (ShapedOreRecipe) par1;
            for (int i = 0; i < 5; ++i) {
                if (i < 3) {
                    for (int j = 0; j < 5; ++j) {
                        if (j < 3) {
                            int k = i * 3 + j;
                            NonNullList<Ingredient> list = recipe.getIngredients();
                            if (k < list.size()) {
                                items[i * 5 + j] = list.get(k).getMatchingStacks()[0];
                            }
                        }
                    }
                }
            }
        } else if (par1 instanceof ShapelessOreRecipe) {
            ShapelessOreRecipe recipe = (ShapelessOreRecipe) par1;
            List<Ingredient> list = new ArrayList<>();
            NonNullList<Ingredient> list2 = recipe.getIngredients();
            list.addAll(list2);

            for (int i = 0; i < 5; ++i) {
                for (int j = 0; j < 5; ++j) {
                    int k = i * 5 + j;
                    if (list.isEmpty() || k >= list.size()) {
                        return items;
                    }

                    items[k] = list.get(k).getMatchingStacks()[0];
                    list.remove(k);
                }
            }
        }

        return items;
    }
}