package rearth.oritech.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.client.cablesurfer.ActiveCableRenderer;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.client.renderers.BlockOutlineRenderer;
import rearth.oritech.client.renderers.PortalEntityRenderer;
import rearth.oritech.client.renderers.SmallTankItemRenderer;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.EntitiesContent;

@Mod(value = Oritech.MOD_ID, dist = Dist.CLIENT)
public class OritechClientNeoForge {
    
    public OritechClientNeoForge(IEventBus eventBus) {
        
        eventBus.register(new EventHandler());
        
        OritechClient.initialize();
    }
    
    @EventBusSubscriber(modid = Oritech.MOD_ID, value = Dist.CLIENT)
    public static class CustomEvents {
        
        @SubscribeEvent
        public static void onWorldRender(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                OreFinderRenderer.doRender(event.getPoseStack(), event.getCamera(), Minecraft.getInstance().renderBuffers().bufferSource());
                ActiveCableRenderer.render(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource());
            }
        }
        
        @SubscribeEvent
        public static void onOutlineRender(RenderHighlightEvent.Block event) {
            BlockOutlineRenderer.render(Minecraft.getInstance().level, event.getCamera(), event.getPoseStack(), event.getMultiBufferSource());
        }
        
        @SubscribeEvent
        public static void onMouseLaserInput(InputEvent.MouseButton.Pre event) {
            var client = Minecraft.getInstance();
            var handled = OritechClient.handleMouseClicked(client, event.getButton(), event.getAction(), event.getModifiers());
            if (handled) event.setCanceled(true);
            
        }
        
    }
    
    static class EventHandler {
        
        @SubscribeEvent
        public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            OritechClient.registerRenderers();
            event.registerEntityRenderer(EntitiesContent.PORTAL_ENTITY, PortalEntityRenderer::new);
            
            for (var entry : ModRenderers.RENDER_LAYERS.entrySet()) {
                ItemBlockRenderTypes.setRenderLayer(entry.getKey(), entry.getValue());
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"}) // due to how the event and generics work we cannot compile-guarantee typing
        @SubscribeEvent
        public void addJetpackElytraLayer(EntityRenderersEvent.AddLayers event) {
            // add to all player models
            for (PlayerSkin.Model skin : event.getSkins())
                if (event.getSkin(skin) instanceof PlayerRenderer pr)
                    pr.addLayer(new OritechElytraLayer<>(pr, event.getEntityModels()));
            // add to all humanoid entities (which have vanilla's elytra layer by default)
            for (EntityType<?> entityType : event.getEntityTypes())
                if (event.getRenderer(entityType) instanceof HumanoidMobRenderer<?,?> hmr)
                    hmr.addLayer(new OritechElytraLayer(hmr, event.getEntityModels()));
            // add to armor stands
            if (event.getRenderer(EntityType.ARMOR_STAND) instanceof LivingEntityRenderer<?,?> ler)
                ler.addLayer(new OritechElytraLayer(ler, event.getEntityModels()));
        }

        @SubscribeEvent
        public void initializeClient(RegisterClientExtensionsEvent event) {
            
//            FluidContent.FLUID_ATTRIBUTES.forEach(attribute -> event.registerFluidType(new IClientFluidTypeExtensions() {
//                @Override
//                public @NotNull ResourceLocation getStillTexture() {
//                    return attribute.getSourceTexture();
//                }
//
//                @Override
//                public @NotNull ResourceLocation getFlowingTexture() {
//                    return attribute.getFlowingTexture();
//                }
//
//                @Override
//                public int getTintColor() {
//                    return attribute.getColor();
//                }
//            }, attribute.getSourceFluid().getFluidType()));
            
            event.registerItem(new TankItemExtensions(Oritech.id("tank_item_model")), BlockContent.SMALL_TANK_ITEM);
            event.registerItem(new TankItemExtensions(Oritech.id("creative_tank_item_model")), BlockContent.CREATIVE_TANK_ITEM);
            
        }
        
    }
    
    private static class TankItemRenderer extends BlockEntityWithoutLevelRenderer {
        
        private final SmallTankItemRenderer itemRenderer;
        
        public TankItemRenderer(ResourceLocation modelId) {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
            this.itemRenderer = new SmallTankItemRenderer(modelId);
        }
        
        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
            // super.render(stack, mode, matrices, vertexConsumers, light, overlay);
            itemRenderer.render(stack, mode, matrices, vertexConsumers, light, overlay);
        }
    }
    
    private static class TankItemExtensions implements IClientItemExtensions {
        private final TankItemRenderer renderer;
        
        private TankItemExtensions(ResourceLocation modelId) {
            this.renderer = new TankItemRenderer(modelId);
        }
        
        @Override
        public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return renderer;
        }
    }
    
}
