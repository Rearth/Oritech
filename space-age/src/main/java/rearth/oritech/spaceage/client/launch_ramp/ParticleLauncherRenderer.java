package rearth.oritech.spaceage.client.launch_ramp;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.client.renderers.util.BeamRenderer;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampBlockEntity;

public final class ParticleLauncherRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<LaunchRampBlockEntity, R> {

    private static final DataTicket<Double> SHOT_AGE = DataTicket.create("particle_launcher_shot_age", Double.class);
    private static final Identifier BEAM = Identifier.withDefaultNamespace("textures/block/white_concrete.png");

    public ParticleLauncherRenderer(BlockEntityRendererProvider.Context context) {

        super(context, new Model());
        withRenderLayer(new BeamLayer<>(this));
    }

    @Override
    protected void tryRotateByBlockstate(RenderPassInfo<R> pass, PoseStack stack) {

        super.tryRotateByBlockstate(pass, stack);
        // The supplied model faces south; GeckoLib's block convention faces north.
        stack.mulPose(Axis.YP.rotationDegrees(180));
    }

    @Override
    public void addRenderData(LaunchRampBlockEntity launcher, Void related, R state, float partialTick) {

        var age = launcher.lastShot < 0 || launcher.getLevel() == null || !launcher.isAssembled(launcher.getBlockState())
                ? -1d : launcher.getLevel().getGameTime() + partialTick - launcher.lastShot - LaunchRampBlockEntity.BEAM_DELAY;
        state.addGeckolibData(SHOT_AGE, age);
    }

    @Override
    public AABB getRenderBoundingBox(LaunchRampBlockEntity launcher) {

        var start = Vec3.atLowerCornerOf(launcher.getBlockPos()).add(launcher.muzzleOffset());
        var end = start.add(launcher.launchDirection().scale(36));
        return new AABB(start, end).inflate(2).minmax(new AABB(launcher.getBlockPos()).inflate(2));
    }

    private static final class BeamLayer<R extends BlockEntityRenderState & GeoRenderState> extends GeoRenderLayer<LaunchRampBlockEntity, Void, R> {

        private BeamLayer(GeoBlockRenderer<LaunchRampBlockEntity, R> renderer) {

            super(renderer);
        }

        @Override
        public void submitRenderTask(RenderPassInfo<R> pass, SubmitNodeCollector collector) {

            var age = pass.getGeckolibData(SHOT_AGE);
            if (age == null || age < 0 || age > LaunchRampBlockEntity.BEAM_TICKS) return;
            var direction = new Vec3(0, Math.sin(LaunchRampBlockEntity.ELEVATION), Math.cos(LaunchRampBlockEntity.ELEVATION));
            var muzzle = LaunchRampBlockEntity.muzzleOffset(Direction.SOUTH).subtract(.5, 0, .5);
            // A short pulse grows out of the muzzle, then travels away at twelve blocks/second.
            var head = age * .6;
            var tail = Math.max(0, head - 5);
            var start = muzzle.add(direction.scale(tail));
            var delta = direction.scale(head - tail);
            var alpha = (int) (255 * Math.min(1, (LaunchRampBlockEntity.BEAM_TICKS - age) / 10));
            collector.submitCustomGeometry(pass.poseStack(), RenderTypes.eyes(BEAM), (pose, consumer) -> {
                BeamRenderer.renderStraightBeam(pose, consumer, start, delta, .035f, RenderHelpers.FULL_BRIGHT,
                        alpha << 24 | 0xD8FAFF, alpha << 24 | 0xFFFFFF);
                BeamRenderer.renderOuterBeam(pose, consumer, start, delta, .12f, RenderHelpers.FULL_BRIGHT,
                        (alpha / 3) << 24 | 0x38BDFF, (alpha / 3) << 24 | 0x67DEFF);
            });
        }
    }

    private static final class Model extends GeoModel<LaunchRampBlockEntity> {

        @Override
        public Identifier getModelResource(GeoRenderState state) {

            return OritechSpaceAge.id("particle_launcher");
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {

            return OritechSpaceAge.id("textures/models/particle_launcher.png");
        }

        @Override
        public Identifier getAnimationResource(LaunchRampBlockEntity launcher) {

            return OritechSpaceAge.id("particle_launcher");
        }
    }
}
