package rearth.oritech.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.client.renderers.BlockOutlineRenderer;
import rearth.oritech.client.renderers.PortalEntityRenderer;
import rearth.oritech.client.renderers.SmallTankItemRenderer;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.EntitiesContent;
import rearth.oritech.item.tools.armor.BaseJetpackItem;

public final class OritechFabricModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        
        OritechClient.initialize();
        OritechClient.registerRenderers();
        
        WorldRenderEvents.BLOCK_OUTLINE.register(OritechFabricModClient::renderBlockOutline);
        WorldRenderEvents.AFTER_ENTITIES.register(OritechFabricModClient::renderWorld);
        
        BuiltinItemRendererRegistry.INSTANCE.register(BlockContent.SMALL_TANK_ITEM, new TankItemRenderer(Oritech.id("tank_item_model")));
        BuiltinItemRendererRegistry.INSTANCE.register(BlockContent.CREATIVE_TANK_ITEM, new TankItemRenderer(Oritech.id("creative_tank_item_model")));
        
        // used for elytra jetpack cape rendering. No Neoforge equivalent.
        LivingEntityFeatureRenderEvents.ALLOW_CAPE_RENDER.register(player -> !(player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof BaseJetpackItem));
        
        for (var entry : ModRenderers.RENDER_LAYERS.entrySet()) {
            BlockRenderLayerMap.INSTANCE.putBlock(entry.getKey(), entry.getValue());
        }
        
        EntityRendererRegistry.register(EntitiesContent.PORTAL_ENTITY, PortalEntityRenderer::new);
    }
    
    private static boolean renderBlockOutline(WorldRenderContext worldRenderContext, WorldRenderContext.BlockOutlineContext blockOutlineContext) {
        BlockOutlineRenderer.render(worldRenderContext.world(), worldRenderContext.camera(), worldRenderContext.matrixStack(), worldRenderContext.consumers());
        return true;
    }
    
    private static void renderWorld(WorldRenderContext worldRenderContext) {
        
        var matrices = worldRenderContext.matrixStack();
        var camera = worldRenderContext.camera();
        var vertexConsumers = worldRenderContext.consumers();
        
        OreFinderRenderer.doRender(matrices, camera, vertexConsumers);
        
    }
    
    private static class TankItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
        
        private final SmallTankItemRenderer itemRenderer;
        
        private TankItemRenderer(Identifier modelId) {
            this.itemRenderer = new SmallTankItemRenderer(modelId);
        }
        
        @Override
        public void render(ItemStack itemStack, ModelTransformationMode modelTransformationMode, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, int overlay) {
            itemRenderer.render(itemStack, modelTransformationMode, matrixStack, vertexConsumerProvider, light, overlay);
        }
    }
}
