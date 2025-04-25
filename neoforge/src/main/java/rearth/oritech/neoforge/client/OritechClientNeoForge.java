package rearth.oritech.neoforge.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.client.renderers.BlockOutlineRenderer;
import rearth.oritech.client.renderers.PortalEntityRenderer;
import rearth.oritech.init.EntitiesContent;
import rearth.oritech.init.FluidContent;

@Mod(value = Oritech.MOD_ID, dist = Dist.CLIENT)
public class OritechClientNeoForge {
    
    public OritechClientNeoForge(IEventBus eventBus) {
        
        eventBus.register(new EventHandler());
        
        OritechClient.initialize();
    }
    
    @EventBusSubscriber(modid = Oritech.MOD_ID)
    public static class CustomEvents {
        
        @SubscribeEvent
        public static void onWorldRender(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                OreFinderRenderer.doRender(event.getPoseStack(), event.getCamera(), MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers());
            }
        }
        
        @SubscribeEvent
        public static void onOutlineRender(RenderHighlightEvent.Block event) {
            BlockOutlineRenderer.render(MinecraftClient.getInstance().world, event.getCamera(), event.getPoseStack(), event.getMultiBufferSource());
        }
        
    }
    
    static class EventHandler {
        
        @SubscribeEvent
        public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            OritechClient.registerRenderers();
            event.registerEntityRenderer(EntitiesContent.PORTAL_ENTITY, PortalEntityRenderer::new);
            
            for (var entry : ModRenderers.RENDER_LAYERS.entrySet()) {
                RenderLayers.setRenderLayer(entry.getKey(), entry.getValue());
            }
        }
        
        @SubscribeEvent
        public void initializeClient(RegisterClientExtensionsEvent event) {
            
            FluidContent.FLUID_ATTRIBUTES.forEach(attribute -> event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public @NotNull Identifier getStillTexture() {
                    return attribute.getSourceTexture();
                }
                
                @Override
                public @NotNull Identifier getFlowingTexture() {
                    return attribute.getFlowingTexture();
                }
                
                @Override
                public int getTintColor() {
                    return attribute.getColor();
                }
            }, attribute.getSourceFluid().getFluidType()));
            
        }
        
    }
    
}
