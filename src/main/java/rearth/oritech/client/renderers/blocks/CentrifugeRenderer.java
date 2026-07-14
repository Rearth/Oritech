package rearth.oritech.client.renderers.blocks;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.entity.processing.CentrifugeBlockEntity;
import rearth.oritech.client.renderers.blocks.SmallTankRenderer.FluidCube;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.util.ColorHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class CentrifugeRenderer<R extends BlockEntityRenderState & GeoRenderState> extends MachineRenderer<CentrifugeBlockEntity, R> {

    private static final DataTicket<CentrifugeFluidData> FLUID_DATA = DataTicket.create("centrifuge_fluid", CentrifugeFluidData.class);
    private static final List<String> GLASS_BONES = List.of("bone", "bone2", "bone3", "bone4", "bone5", "bone6");
    private static final CentrifugeFluidData EMPTY_FLUID_DATA = new CentrifugeFluidData(List.of(), false);

    public CentrifugeRenderer(BlockEntityRendererProvider.Context context, String modelPath) {
        super(context, modelPath);
        withRenderLayer(new FluidLayer<>(this));
    }

    @Override
    public void addRenderData(CentrifugeBlockEntity animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);

        var input = animatable.fluidContainer.getInStack();
        if (input.isEmpty()) {
            renderState.addGeckolibData(FLUID_DATA, EMPTY_FLUID_DATA);
            return;
        }

        var fill = input.getAmount() / (float) animatable.fluidContainer.getCapacity();
        var cube = new FluidCube(
                new Vector3f(-1.5f / 16f, -6.125f / 16f, -1.5f / 16f),
                new Vector3f(3 / 16f, 8.25f / 16f, 3 / 16f),
                fill,
                RenderHelpers.getFluidSprite(input.getFluid()),
                ColorHelper.makeOpaque(ColorHelper.getFluidTint(input)),
                null);

        renderState.addGeckolibData(FLUID_DATA, new CentrifugeFluidData(List.of(cube), animatable.isActivelyWorking()));
    }

    private static class FluidLayer<R extends BlockEntityRenderState & GeoRenderState> extends GeoRenderLayer<CentrifugeBlockEntity, Void, R> {

        private final Map<Long, Long> nextParticleTicks = new HashMap<>();
        private @Nullable ClientLevel particleLevel;

        protected FluidLayer(CentrifugeRenderer<R> renderer) {
            super(renderer);
        }

        @Override
        public void preRender(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
            var data = renderPassInfo.getGeckolibData(FLUID_DATA);
            var level = Minecraft.getInstance().level;
            if (data == null || data.cubes().isEmpty() || !data.active() || level == null) return;

            if (particleLevel != level) {
                nextParticleTicks.clear();
                particleLevel = level;
            }

            var blockPos = renderPassInfo.renderState().blockPos;
            var gameTime = level.getGameTime();
            if (gameTime < nextParticleTicks.getOrDefault(blockPos.asLong(), 0L)) return;

            var random = level.getRandom();
            nextParticleTicks.put(blockPos.asLong(), gameTime + 2 + random.nextInt(3));

            var boneName = GLASS_BONES.get(random.nextInt(GLASS_BONES.size()));
            var cube = data.cubes().getFirst();
            renderPassInfo.addBonePositionListener(boneName, (worldPos, ignoredModelPos, ignoredLocalPos) -> {
                if (worldPos != null) spawnDroplet(level, worldPos, Vec3.atCenterOf(blockPos), cube);
            });
        }

        @Override
        public void addPerBoneRender(RenderPassInfo<R> renderPassInfo, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
            var data = renderPassInfo.getGeckolibData(FLUID_DATA);
            if (!renderPassInfo.willRender() || data == null || data.cubes().isEmpty()) return;

            for (var boneName : GLASS_BONES) {
                renderPassInfo.model().getBone(boneName).ifPresent(bone ->
                        consumer.accept(bone, (passInfo, ignored, renderTasks) -> submitFluid(passInfo, renderTasks, data.cubes())));
            }
        }

        private static <R extends GeoRenderState> void submitFluid(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks, List<FluidCube> cubes) {
            SmallTankRenderer.submitFluidCubes(renderTasks, renderPassInfo.poseStack(), cubes, renderPassInfo.packedLight(), OverlayTexture.NO_OVERLAY);
        }

        private static void spawnDroplet(ClientLevel level, Vec3 position, Vec3 machineCenter, FluidCube cube) {
            var random = level.getRandom();
            var radial = position.subtract(machineCenter).multiply(1, 0, 1).normalize();
            var tangent = new Vec3(-radial.z, 0, radial.x);
            var velocity = tangent.scale(0.015 + random.nextDouble() * 0.05)
                    .add(radial.scale(0.145 + random.nextDouble() * 0.03))
                    .add(0, 0.01 + random.nextDouble() * 0.025, 0);

            Minecraft.getInstance().particleEngine.add(new FluidDropletParticle(level, position.subtract(0.5f, 0.5f, 0.5f), velocity, cube.sprite(), cube.color()));
        }
    }

    private static class FluidDropletParticle extends SingleQuadParticle {

        protected FluidDropletParticle(ClientLevel level, Vec3 position, Vec3 velocity, TextureAtlasSprite sprite, int color) {
            super(level, position.x, position.y, position.z, sprite);

            this.xd = velocity.x;
            this.yd = velocity.y;
            this.zd = velocity.z;
            this.quadSize = 0.02f + random.nextFloat() * 0.015f;
            this.gravity = 0.2f;
            this.friction = 0.92f;
            this.lifetime = 12 + random.nextInt(9);
            this.hasPhysics = false;
            setColor((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f);
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT_TERRAIN;
        }
    }

    private record CentrifugeFluidData(List<FluidCube> cubes, boolean active) {
    }
}
