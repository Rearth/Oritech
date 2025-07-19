package rearth.oritech.client.other;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

import java.util.List;

import static net.minecraft.client.render.RenderPhase.*;

@SuppressWarnings("DataFlowIssue")
public class OreFinderRenderer {
    
    public static List<BlockPos> renderedBlocks;
    public static long receivedAt;
    
    private static final RenderLayer OVERLAY = RenderLayer.of("testoverlay", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 786432, true, false, RenderLayer.MultiPhaseParameters.builder().lightmap(DISABLE_LIGHTMAP).program(SOLID_PROGRAM).texture(BLOCK_ATLAS_TEXTURE).depthTest(ALWAYS_DEPTH_TEST).target(OUTLINE_TARGET).cull(ENABLE_CULLING).build(false));
    
    public static void doRender(MatrixStack matrices, Camera camera, VertexConsumerProvider vertexConsumers) {
        var world = MinecraftClient.getInstance().world;
        if (world == null || renderedBlocks == null) return;
        var age = world.getTime() - receivedAt;
        
        if (age > 15) return;
        
        for (var pos : renderedBlocks) {
            var state = world.getBlockState(pos);
            
            matrices.push();
            //Offset by the camera position so that the render is relative to the camera
            matrices.translate(pos.getX() - camera.getPos().x, pos.getY() - camera.getPos().y, pos.getZ() - camera.getPos().z);
            
            var renderer = MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer();
            var vertexProvider = vertexConsumers.getBuffer(OVERLAY);
            
            renderer.render(world, MinecraftClient.getInstance().getBlockRenderManager().getModel(state), state, pos, matrices, vertexProvider, false, world.random, 0, 0);
            
            matrices.pop();
        }
    }
}
