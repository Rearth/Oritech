package rearth.oritech.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.entity.storage.SmallTankEntity;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.util.ColorHelper;

import java.util.List;

public class SmallTankRenderer implements BlockEntityRenderer<SmallTankEntity, SmallTankRenderer.TankRenderState> {

    public SmallTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public TankRenderState createRenderState() {
        return new TankRenderState();
    }

    @Override
    public void extractRenderState(SmallTankEntity entity, TankRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);

        var storage = entity.fluidStorage;
        if (storage.getAmount() <= 0 || storage.getFluid().equals(Fluids.EMPTY)) {
            state.sprite = null;
            return;
        }

        var fluid = storage.getFluid();

        // resolve the still texture from the data-driven fluid models introduced in NeoForge 26.1
        state.fill = storage.getAmount() / (float) storage.getCapacity();
        state.sprite = RenderHelpers.getFluidSprite(fluid);
        state.color = ColorHelper.makeOpaque(ColorHelper.getFluidTint(storage.getContent()));
    }

    @Override
    public void submit(TankRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {

        var sprite = state.sprite;
        if (sprite == null) return;

        submitTankFluid(collector, poseStack, sprite, state.color, state.fill, state.lightCoords, OverlayTexture.NO_OVERLAY);
    }

    /**
     * Draws the tank's contained fluid as a translucent box (full-tank shape, scaled by fill) through the
     * NeoForge 26.1 submit pipeline. Shared by the tank block-entity renderer and the tank item renderer.
     */
    public static void submitTankFluid(SubmitNodeCollector collector, PoseStack poseStack, TextureAtlasSprite sprite, int color, float fill, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.126, 0.126, 0.126);
        poseStack.scale(0.745f, 0.745f * fill, 0.745f);

        // snapshot the transformed pose and defer the actual vertex emission to the submit pipeline
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, consumer) -> {
            for (var direction : Direction.values()) {
                if (direction.equals(Direction.DOWN)) continue; // skip bottom, as it's never visible
                drawQuad(direction, consumer, pose.pose(), pose, sprite, color, light, overlay);
            }
        });

        poseStack.popPose();
    }

    public static void drawQuad(Direction direction, VertexConsumer consumer, Matrix4f modelMatrix, PoseStack.Pose normalMatrix, TextureAtlasSprite sprite, int color, int light, int overlay) {
        // Define the vertices of the quad based on the direction it's facing

        var normal = direction.step();

        var positions = getQuadVerticesByDirection(direction);

        for (int i = positions.length - 1; i >= 0; i--) {

            var pos = positions[i];
            var u = sprite.getU(getFrameU()[i]);
            var v = sprite.getV(getFrameV()[i]);

            consumer.addVertex(modelMatrix, pos[0], pos[1], pos[2])
                    .setColor(color)
                    .setUv(u, v)
                    .setLight(light)
                    .setOverlay(overlay)
                    .setNormal(normalMatrix, normal.x, normal.y, normal.z);
        }

    }

    private static float[] getFrameU() {
        return new float[]{0, 1, 1, 0};
    }

    private static float[] getFrameV() {
        return new float[]{0, 0, 1, 1};
    }

    private static float[][] getQuadVerticesByDirection(Direction direction) {
        // Define the vertices for each face of the cube
        return switch (direction) {
            case UP -> new float[][]{
                    {0, 1, 0}, // Top-left
                    {1, 1, 0}, // Top-right
                    {1, 1, 1}, // Bottom-right
                    {0, 1, 1}  // Bottom-left
            };
            case DOWN -> new float[][]{
                    {0, 0, 1}, // Top-left
                    {1, 0, 1}, // Top-right
                    {1, 0, 0}, // Bottom-right
                    {0, 0, 0}  // Bottom-left
            };
            case NORTH -> new float[][]{
                    {1, 1, 0}, // Top-left
                    {0, 1, 0}, // Top-right
                    {0, 0, 0}, // Bottom-right
                    {1, 0, 0}  // Bottom-left
            };
            case SOUTH -> new float[][]{
                    {0, 1, 1}, // Top-left
                    {1, 1, 1}, // Top-right
                    {1, 0, 1}, // Bottom-right
                    {0, 0, 1}  // Bottom-left
            };
            case WEST -> new float[][]{
                    {0, 1, 0}, // Top-left
                    {0, 1, 1}, // Top-right
                    {0, 0, 1}, // Bottom-right
                    {0, 0, 0}  // Bottom-left
            };
            case EAST -> new float[][]{
                    {1, 1, 1}, // Top-left
                    {1, 1, 0}, // Top-right
                    {1, 0, 0}, // Bottom-right
                    {1, 0, 1}  // Bottom-left
            };
        };
    }

    public static class TankRenderState extends BlockEntityRenderState {
        public @Nullable TextureAtlasSprite sprite;
        public float fill;
        public int color;
    }

    /**
     * A single resolved fluid box to draw, expressed in the model space used by the host renderer.
     * Used by the refinery renderers to ship their fluid geometry through the GeckoLib DataTicket mechanism.
     *
     * @param min      the lower corner of the box (in 1/16th model units, matching the original constants)
     * @param size     the box dimensions
     * @param fill     0..1 fill level (scales the height)
     * @param sprite   the still fluid sprite
     * @param color    the ARGB tint
     * @param rotation an optional rotation applied around the model origin before translating to {@code min}
     */
    public record FluidCube(Vector3f min, Vector3f size, float fill, TextureAtlasSprite sprite, int color,
                            @Nullable Quaternionf rotation) {
    }

    /**
     * Emits a list of {@link FluidCube}s into the NeoForge 26.1 submit pipeline. Each cube is submitted as its own
     * custom-geometry node (the collector snapshots the pose at submit time), so per-cube transforms are independent.
     */
    public static void submitFluidCubes(SubmitNodeCollector collector, PoseStack poseStack, List<FluidCube> cubes, int light, int overlay) {
        for (var cube : cubes) {
            poseStack.pushPose();

            if (cube.rotation() != null) poseStack.mulPose(cube.rotation());

            poseStack.translate(cube.min().x + 0.01f, cube.min().y + 0.01f, cube.min().z + 0.01f);
            poseStack.scale(cube.size().x - 0.02f, cube.size().y * cube.fill() - 0.03f, cube.size().z - 0.02f);

            var sprite = cube.sprite();
            var color = cube.color();

            collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, consumer) -> {
                for (Direction direction : Direction.values()) {
                    if (direction.equals(Direction.DOWN)) continue; // skip bottom, as it's never visible
                    drawQuad(direction, consumer, pose.pose(), pose, sprite, color, light, overlay);
                }
            });

            poseStack.popPose();
        }
    }
}
