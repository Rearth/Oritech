package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import io.wispforest.owo.ui.core.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix4f;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;

public class SmallTankItemRenderer {
    
    private BakedModel tankVisualModel;
    private final ResourceLocation TANK_VISUAL_MODEL_ID;
    
    public SmallTankItemRenderer(ResourceLocation tankVisualModelId) {
        TANK_VISUAL_MODEL_ID = tankVisualModelId;
    }
    
    public void loadModels() {
        if (tankVisualModel == null) {
            this.tankVisualModel = Minecraft.getInstance().getModelManager().getModel(new ModelResourceLocation(TANK_VISUAL_MODEL_ID, ""));
            if (this.tankVisualModel == Minecraft.getInstance().getModelManager().getMissingModel()) {
                this.tankVisualModel = null; // Ensure it's null if missing
                Oritech.LOGGER.warn("Unable to load model for portable tank renderer: {}. Model not found.", TANK_VISUAL_MODEL_ID);
            }
        }
    }
    
    public void render(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        
        matrices.pushPose();
        matrices.translate(0, 0.25, 0);
        matrices.scale(0.84f, 0.84f, 0.84f);
        
        matrices.pushPose();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(0.9f, 0.9f, 0.9f);
        
        if (tankVisualModel == null || tankVisualModel == Minecraft.getInstance().getModelManager().getMissingModel()) {
            loadModels();
        }
        
        // render the original tank model
        if (this.tankVisualModel != null && this.tankVisualModel != Minecraft.getInstance().getModelManager().getMissingModel()) {
            Minecraft.getInstance().getItemRenderer().render(
              stack,
              ItemDisplayContext.NONE,
              false,
              matrices,
              vertexConsumers,
              light,
              overlay,
              this.tankVisualModel
            );
        }
        
        matrices.popPose();
        
        var storage = stack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty());
        if (storage.isEmpty()) {
            matrices.popPose();
            return;
        }
        
        var fluid = storage.getFluid();
        var fill = storage.getAmount() / (float) (Oritech.CONFIG.portableTankCapacityBuckets() * FluidStackHooks.bucketAmount());
        
        var sprite = FluidStackHooks.getStillTexture(fluid);
        var spriteColor = FluidStackHooks.getColor(fluid);
        var consumer = vertexConsumers.getBuffer(RenderType.translucent());
        
        var parsedColor = Color.ofArgb(spriteColor);
        var opaqueColor = new Color(parsedColor.red(), parsedColor.green(), parsedColor.blue(), 1f);
        spriteColor = opaqueColor.argb();
        
        matrices.pushPose();
        matrices.translate(0.126, 0.126, 0.126);
        matrices.scale(0.745f, 0.745f * fill, 0.745f);
        
        var entry = matrices.last();
        var modelMatrix = entry.pose();
        
        // Draw the cube using quads
        for (Direction direction : Direction.values()) {
            if (direction.equals(Direction.DOWN)) continue; // skip bottom, as it's never visible
            SmallTankRenderer.drawQuad(direction, consumer, modelMatrix, entry, sprite, spriteColor, light, overlay);
        }
        
        matrices.popPose();
        matrices.popPose();
    }
}
