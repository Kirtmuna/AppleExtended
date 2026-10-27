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

package jp.apple.appleextended.render.item;

import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.item.ItemWithModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class CustomIconModelHandler {

    private static final IBakedModel BUILTIN_EMPTY = new IBakedModel() {
        @Override
        public List<BakedQuad> getQuads(IBlockState s, EnumFacing side, long rand) {
            return Collections.emptyList();
        }

        @Override
        public boolean isAmbientOcclusion() {
            return false;
        }

        @Override
        public boolean isGui3d() {
            return false;
        }

        @Override
        public boolean isBuiltInRenderer() {
            return true;
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
        }

        @Override
        public ItemOverrideList getOverrides() {
            return ItemOverrideList.NONE;
        }

        @Override
        public ItemCameraTransforms getItemCameraTransforms() {
            return ItemCameraTransforms.DEFAULT;
        }
    };

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {
        for (Map.Entry<ModelResourceLocation, Item> entry : RTMItem.MODEL_TO_ITEM.entrySet()) {
            ModelResourceLocation mrl = entry.getKey();
            Item item = entry.getValue();
            if (!(item instanceof ItemWithModel)) continue;

            IBakedModel original = event.getModelRegistry().getObject(mrl);
            if (original == null) continue;

            event.getModelRegistry().putObject(mrl, new BakedModelWrapper<IBakedModel>(original) {
                @Override
                public ItemOverrideList getOverrides() {
                    return new ItemOverrideList(Collections.emptyList()) {
                        @Override
                        public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity) {
                            ResourceLocation tex = ((ItemWithModel<?>) stack.getItem()).getCustomIconTexture(stack);
                            return tex == null ? originalModel : BUILTIN_EMPTY;
                        }
                    };
                }
            });
        }
    }
}