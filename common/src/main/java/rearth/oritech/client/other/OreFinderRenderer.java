package rearth.oritech.client.other;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.BitSet;
import java.util.List;

import static net.minecraft.client.renderer.LevelRenderer.DIRECTIONS;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class OreFinderRenderer {
    
    public static List<BlockPos> renderedBlocks;
    public static long receivedAt;
    
    private static final RenderType OVERLAY = RenderType.create("overlay",
      DefaultVertexFormat.BLOCK,
      VertexFormat.Mode.QUADS,
      4194304,
      true,
      false,
      RenderType.CompositeState.builder()
        .setLightmapState(LIGHTMAP)
        .setShaderState(RENDERTYPE_SOLID_SHADER)
        .setTextureState(BLOCK_SHEET_MIPPED)
        .setCullState(CULL)
        .setOutputState(OUTLINE_TARGET)
        .createCompositeState(false));
    
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
            
            OreFinderRenderer.tesselateWithoutAO(renderer, world, Minecraft.getInstance().getBlockRenderer().getBlockModel(state), state, pos, matrices, vertexProvider, false, world.random, 0, 0);
            
            matrices.popPose();
        }
    }
    
    
    public static void tesselateWithoutAO(ModelBlockRenderer renderer, BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random, long seed, int packedOverlay) {
        BitSet bitSet = new BitSet(3);
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
        
        for(Direction direction : DIRECTIONS) {
            random.setSeed(seed);
            List<BakedQuad> list = model.getQuads(state, direction, random);
            if (!list.isEmpty()) {
                mutableBlockPos.setWithOffset(pos, direction);
                if (!checkSides || Block.shouldRenderFace(state, level, pos, direction, mutableBlockPos)) {
                    int i = LightTexture.FULL_BRIGHT;
                    renderer.renderModelFaceFlat(level, state, pos, i, packedOverlay, false, poseStack, consumer, list, bitSet);
                }
            }
        }
        
        random.setSeed(seed);
        List<BakedQuad> list2 = model.getQuads(state, null, random);
        if (!list2.isEmpty()) {
            int i = LightTexture.FULL_BRIGHT;
            renderer.renderModelFaceFlat(level, state, pos, i, packedOverlay, true, poseStack, consumer, list2, bitSet);
        }
        
    }
}
