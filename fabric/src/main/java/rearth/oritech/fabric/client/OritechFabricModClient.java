package rearth.oritech.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map.Entry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.client.renderers.BlockOutlineRenderer;
import rearth.oritech.client.renderers.PortalEntityRenderer;
import rearth.oritech.client.renderers.SmallTankItemRenderer;
import rearth.oritech.fabric.OritechPlatformFabric;
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
        
        // used for elytra jetpack cape rendering. No Neoforge equivalent, meaning neoforge just doesnt get fixed capes.
        LivingEntityFeatureRenderEvents.ALLOW_CAPE_RENDER.register(player -> !(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BaseJetpackItem));
        
        for (var entry : ModRenderers.RENDER_LAYERS.entrySet()) {
            BlockRenderLayerMap.INSTANCE.putBlock(entry.getKey(), entry.getValue());
        }
        
        EntityRendererRegistry.register(EntitiesContent.PORTAL_ENTITY, PortalEntityRenderer::new);
        
        for (var runnable : OritechPlatformFabric.PENDING_S2C_INITS) {
            runnable.run();
        }
        
        OritechPlatformFabric.PENDING_S2C_INITS.clear();
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
        
        private TankItemRenderer(ResourceLocation modelId) {
            this.itemRenderer = new SmallTankItemRenderer(modelId);
        }
        
        @Override
        public void render(ItemStack itemStack, ItemDisplayContext modelTransformationMode, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int light, int overlay) {
            itemRenderer.render(itemStack, modelTransformationMode, matrixStack, vertexConsumerProvider, light, overlay);
        }
    }
}
