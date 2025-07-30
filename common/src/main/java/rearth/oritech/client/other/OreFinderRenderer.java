package rearth.oritech.client.other;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

import static net.minecraft.client.renderer.RenderStateShard.*;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

@SuppressWarnings("DataFlowIssue")
public class OreFinderRenderer {
    
    public static List<BlockPos> renderedBlocks;
    public static long receivedAt;
    
    private static final RenderType OVERLAY = RenderType.create("testoverlay", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 786432, true, false, RenderType.CompositeState.builder().setLightmapState(NO_LIGHTMAP).setShaderState(RENDERTYPE_SOLID_SHADER).setTextureState(BLOCK_SHEET).setDepthTestState(NO_DEPTH_TEST).setOutputState(OUTLINE_TARGET).setCullState(CULL).createCompositeState(false));
    
    public static void doRender(PoseStack matrices, Camera camera, MultiBufferSource vertexConsumers) {
        var world = Minecraft.getInstance().level;
        if (world == null || renderedBlocks == null) return;
        var age = world.getGameTime() - receivedAt;
        
        if (age > 15) return;
        
        for (var pos : renderedBlocks) {
            var state = world.getBlockState(pos);
            
            matrices.pushPose();
            //Offset by the camera position so that the render is relative to the camera
            matrices.translate(pos.getX() - camera.getPosition().x, pos.getY() - camera.getPosition().y, pos.getZ() - camera.getPosition().z);
            
            var renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
            var vertexProvider = vertexConsumers.getBuffer(OVERLAY);
            
            renderer.tesselateBlock(world, Minecraft.getInstance().getBlockRenderer().getBlockModel(state), state, pos, matrices, vertexProvider, false, world.random, 0, 0);
            
            matrices.popPose();
        }
    }
}
