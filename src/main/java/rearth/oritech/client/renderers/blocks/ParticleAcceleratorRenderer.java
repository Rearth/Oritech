package rearth.oritech.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.entity.accelerator.ParticleAcceleratorBlockEntity;
import rearth.oritech.client.renderers.util.BeamRenderer;
import rearth.oritech.client.renderers.util.RenderHelpers;

import java.util.ArrayList;
import java.util.List;


public class ParticleAcceleratorRenderer implements BlockEntityRenderer<ParticleAcceleratorBlockEntity, ParticleAcceleratorRenderer.ParticleRenderState> {

    public ParticleAcceleratorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public ParticleRenderState createRenderState() {
        return new ParticleRenderState();
    }

    @Override
    public AABB getRenderBoundingBox(ParticleAcceleratorBlockEntity blockEntity) {
        return AABB.INFINITE;
    }

    @Override
    public void extractRenderState(ParticleAcceleratorBlockEntity entity, ParticleRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);

        var line = entity.displayTrail;

        // nothing to render this frame
        if (line == null || line.size() < 2) {
            state.particleLine = List.of();
            return;
        }

        // spawn a particle at the head of the trail
        var level = entity.getLevel();
        var head = line.getLast();

        if (level.getRandom().nextFloat() > 0.7f)
            level.addParticle(ParticleTypes.REVERSE_PORTAL,
                    head.x + (level.getRandom().nextDouble() - 0.5) * 0.4,
                    head.y + (level.getRandom().nextDouble() - 0.5) * 0.6,
                    head.z + (level.getRandom().nextDouble() - 0.5) * 0.4,
                    0, 0, 0);

        state.particleLine = new ArrayList<>(line);
    }

    @Override
    public void submit(ParticleRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {

        var line = state.particleLine;
        if (line == null || line.size() < 2) return;

        // convert world to local space
        var origin = Vec3.atLowerCornerOf(state.blockPos);
        var baseThickness = 0.07f;
        var beamType = RenderTypes.eyes(EndericLaserRenderer.BEAM_TEXTURE);

        // submit trail as custom geometry
        collector.submitCustomGeometry(poseStack, beamType, (pose, consumer) -> {
            for (int i = 0; i < line.size() - 1; i++) {
                var startLocal = line.get(i).subtract(origin);
                var delta = line.get(i + 1).subtract(line.get(i));

                // glowing core
                BeamRenderer.renderStraightBeam(
                        pose, consumer, startLocal, delta,
                        baseThickness * 0.3f,
                        RenderHelpers.FULL_BRIGHT,
                        EndericLaserRenderer.CORE_COLOR_START,
                        EndericLaserRenderer.CORE_COLOR_START
                );

                // outer glow
                BeamRenderer.renderStraightBeam(
                        pose, consumer, startLocal, delta,
                        baseThickness,
                        RenderHelpers.FULL_BRIGHT,
                        EndericLaserRenderer.GLOW_COLOR_START,
                        EndericLaserRenderer.GLOW_COLOR_START
                );
            }
        });
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    public static class ParticleRenderState extends BlockEntityRenderState {
        public List<Vec3> particleLine = new ArrayList<>();
    }

}
