package rearth.oritech.client.renderers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import rearth.oritech.block.entity.machines.interaction.ChargerBlockEntity;

public class ChargerBlockRenderer implements BlockEntityRenderer<ChargerBlockEntity> {
    
    @Override
    public void render(ChargerBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        
        var inputStack = entity.inventory.getStack(0);
        if (inputStack.isEmpty()) return;
        
        matrices.push();
        matrices.translate(0.5f, 8/16f, 0.5f);
        
        MinecraftClient.getInstance().getItemRenderer().renderItem(
          inputStack,
          ModelTransformationMode.GROUND,
          light,
          overlay,
          matrices,
          vertexConsumers,
          entity.getWorld(),
          0
        );
        
        matrices.pop();
        
    }
}
