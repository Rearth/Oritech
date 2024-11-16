package rearth.oritech.client.renderers;

import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;

public class BlockOutlineRenderer {
    @SuppressWarnings("resource")
    public static void render(ClientWorld world, Camera camera, RenderTickCounter counter, MatrixStack matrixStack, VertexConsumerProvider consumer, GameRenderer gameRenderer, Matrix4f matrix, LightmapTextureManager lightTexture, WorldRenderer worldRenderer) {
        if (world == null) return;

        var client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null || player.isSneaking()) return;

        var itemStack = player.getMainHandStack();
        if (!itemStack.isOf(ToolsContent.PROMETHIUM_PICKAXE)) return;

        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

        var blockPos = ((BlockHitResult)client.crosshairTarget).getBlockPos();
        var offsetBlocks = PromethiumPickaxeItem.getOffsetBlocks((World)world, (PlayerEntity)player, blockPos);

        matrixStack.push();
        var cameraPos = camera.getPos();
        matrixStack.translate(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());

        for (var offsetPos : offsetBlocks) {
            var offsetState = world.getBlockState(offsetPos);
            var renderShape = offsetState.getOutlineShape(world, offsetPos);

            matrixStack.push();
            matrixStack.translate(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ());
            WorldRenderer.drawCuboidShapeOutline(matrixStack, consumer.getBuffer(RenderLayer.getLines()), renderShape, 0, 0, 0, 0.0F, 0.0F, 0.0F, 0.35F);
            matrixStack.pop();
        }

        matrixStack.pop();
    }
}
