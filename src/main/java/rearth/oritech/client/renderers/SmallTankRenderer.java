package rearth.oritech.client.renderers;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.Fluids;
import rearth.oritech.block.entity.machines.storage.SmallFluidTankEntity;

public class SmallTankRenderer implements BlockEntityRenderer<SmallFluidTankEntity> {
    
    @Override
    public void render(SmallFluidTankEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        
        var storage = entity.getForDirectFluidAccess();
        if (storage.amount == 0 || storage.variant.isBlank()) return;
        
        var fluid = storage.variant.getFluid();
        fluid = Fluids.FLOWING_LAVA;    // for debug purposes
        var fluidState = fluid.getDefaultState();
        var blockState = fluidState.getBlockState();
        
        matrices.push();
        
        // this doesn't render
        FluidRenderHandlerRegistry.INSTANCE.get(fluid).renderFluid(
          entity.getPos(),
          entity.getWorld(),
          vertexConsumers.getBuffer(RenderLayers.getFluidLayer(fluidState)),
          blockState,
          fluidState
        );
        
        matrices.pop();
        
        var dirtState = Blocks.DIAMOND_BLOCK.getDefaultState();
        
        matrices.push();
        matrices.translate(0, 1, 0);
        
        // this renders
        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
          dirtState,
          entity.getPos(),
          entity.getWorld(),
          matrices,
          vertexConsumers.getBuffer(RenderLayers.getBlockLayer(dirtState)),
          false,
          entity.getWorld().random
        );
        
        matrices.pop();
        
    }
}
