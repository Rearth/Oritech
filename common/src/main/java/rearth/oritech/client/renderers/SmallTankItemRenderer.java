package rearth.oritech.client.renderers;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import io.wispforest.owo.ui.core.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;

public class SmallTankItemRenderer {
    
    private BakedModel tankVisualModel;
    private final Identifier TANK_VISUAL_MODEL_ID;
    
    public SmallTankItemRenderer(Identifier tankVisualModelId) {
        TANK_VISUAL_MODEL_ID = tankVisualModelId;
    }
    
    public void loadModels() {
        if (tankVisualModel == null) {
            this.tankVisualModel = MinecraftClient.getInstance().getBakedModelManager().getModel(new ModelIdentifier(TANK_VISUAL_MODEL_ID, ""));
            if (this.tankVisualModel == MinecraftClient.getInstance().getBakedModelManager().getMissingModel()) {
                this.tankVisualModel = null; // Ensure it's null if missing
                Oritech.LOGGER.warn("Unable to load model for portable tank renderer: {}. Model not found.", TANK_VISUAL_MODEL_ID);
            }
        }
    }
    
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        
        matrices.push();
        matrices.translate(0, 0.25, 0);
        matrices.scale(0.84f, 0.84f, 0.84f);
        
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(0.9f, 0.9f, 0.9f);
        
        if (tankVisualModel == null || tankVisualModel == MinecraftClient.getInstance().getBakedModelManager().getMissingModel()) {
            loadModels();
        }
        
        // render the original tank model
        if (this.tankVisualModel != null && this.tankVisualModel != MinecraftClient.getInstance().getBakedModelManager().getMissingModel()) {
            MinecraftClient.getInstance().getItemRenderer().renderItem(
              stack,
              ModelTransformationMode.NONE,
              false,
              matrices,
              vertexConsumers,
              light,
              overlay,
              this.tankVisualModel
            );
        }
        
        matrices.pop();
        
        var storage = stack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty());
        if (storage.isEmpty()) {
            matrices.pop();
            return;
        }
        
        var fluid = storage.getFluid();
        var fill = storage.getAmount() / (float) (Oritech.CONFIG.portableTankCapacityBuckets() * FluidStackHooks.bucketAmount());
        
        var sprite = FluidStackHooks.getStillTexture(fluid);
        var spriteColor = FluidStackHooks.getColor(fluid);
        var consumer = vertexConsumers.getBuffer(RenderLayer.getTranslucent());
        
        var parsedColor = Color.ofArgb(spriteColor);
        var opaqueColor = new Color(parsedColor.red(), parsedColor.green(), parsedColor.blue(), 1f);
        spriteColor = opaqueColor.argb();
        
        matrices.push();
        matrices.translate(0.126, 0.126, 0.126);
        matrices.scale(0.745f, 0.745f * fill, 0.745f);
        
        var entry = matrices.peek();
        var modelMatrix = entry.getPositionMatrix();
        
        // Draw the cube using quads
        for (Direction direction : Direction.values()) {
            if (direction.equals(Direction.DOWN)) continue; // skip bottom, as it's never visible
            SmallTankRenderer.drawQuad(direction, consumer, modelMatrix, entry, sprite, spriteColor, light, overlay);
        }
        
        matrices.pop();
        matrices.pop();
    }
}
