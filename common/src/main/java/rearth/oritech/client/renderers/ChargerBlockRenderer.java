package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.block.entity.interaction.ChargerBlockEntity;

public class ChargerBlockRenderer implements BlockEntityRenderer<ChargerBlockEntity> {
    
    @Override
    public void render(ChargerBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        
        var inputStack = entity.inventory.getItem(0);
        if (inputStack.isEmpty()) return;
        
        matrices.pushPose();
        matrices.translate(0.5f, 8/16f, 0.5f);
        
        Minecraft.getInstance().getItemRenderer().renderStatic(
          inputStack,
          ItemDisplayContext.GROUND,
          light,
          overlay,
          matrices,
          vertexConsumers,
          entity.getLevel(),
          0
        );
        
        matrices.popPose();
        
    }
}
