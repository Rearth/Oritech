package rearth.oritech.client.other;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;

import java.util.List;

import static net.minecraft.client.render.RenderPhase.*;

@SuppressWarnings("DataFlowIssue")
public class OreFinderRenderer {
    
    public static List<BlockPos> renderedBlocks;
    public static long receivedAt;
    
    private static final RenderLayer OVERLAY = RenderLayer.of("testoverlay", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 786432, true, false, RenderLayer.MultiPhaseParameters.builder().lightmap(ENABLE_LIGHTMAP).program(SOLID_PROGRAM).texture(BLOCK_ATLAS_TEXTURE).depthTest(ALWAYS_DEPTH_TEST).target(OUTLINE_TARGET).cull(ENABLE_CULLING).build(false));
    
    public static void doRender(WorldRenderContext worldRenderContext) {
        
        var world = MinecraftClient.getInstance().world;
        if (world == null || renderedBlocks == null) return;
        var age = world.getTime() - receivedAt;
        
        if (age > 20) return;
        
        for (var pos : renderedBlocks) {
            var state = world.getBlockState(pos);
            
            var matrices = worldRenderContext.matrixStack();
            var camera = worldRenderContext.camera();
            
            matrices.push();
            //Offset by the camera position so that the render is relative to the camera
            matrices.translate(pos.getX() - camera.getPos().x, pos.getY() - camera.getPos().y, pos.getZ() - camera.getPos().z);
            
            var vertexProvider = worldRenderContext.consumers().getBuffer(OVERLAY);
            var renderer = MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer();
            
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            renderer.renderFlat(world, MinecraftClient.getInstance().getBlockRenderManager().getModel(state), state, pos, matrices, vertexProvider, false, world.random, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            
            matrices.pop();
            ;
        }
        
    }
}
