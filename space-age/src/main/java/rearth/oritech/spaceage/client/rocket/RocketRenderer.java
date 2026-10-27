package rearth.oritech.spaceage.client.rocket;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;
import rearth.oritech.client.ui.render.BlockPreviewEntities;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.RocketSimulationController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RocketRenderer {

    private static final ContextKey<RocketRenderData> ROCKET_DATA = new ContextKey<>(OritechSpaceAge.id("rockets"));
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Map<UUID, Map<BlockPos, BlockEntity>> VISUAL_ENTITIES = new HashMap<>();

    static void clearVisualEntities() {

        VISUAL_ENTITIES.clear();
    }

    public static void onExtractRenderState(ExtractLevelRenderStateEvent event) {

        var level = event.getLevel();
        var partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var gameTime = level.getGameTime() + partialTick;

        var minecraft = Minecraft.getInstance();
        var modelResolver = minecraft.getBlockModelResolver();
        var blockEntityDispatcher = minecraft.getBlockEntityRenderDispatcher();
        var modelCache = new HashMap<BlockState, BlockModelRenderState>();
        var rockets = new ArrayList<RenderedRocket>();
        var visibleRockets = new HashSet<UUID>();

        for (var rocket : RocketClientController.getActiveRockets()) {
            var flight = rocket.getFlight();
            if (flight == null || !flight.canReachOrbit() || !flight.dimension().equals(level.dimension()) || flight.isInSpace(gameTime))
                continue;

            var position = RocketSimulationController.getRocketPosition(flight, gameTime);
            visibleRockets.add(rocket.getRocketId());
            var visualEntities = VISUAL_ENTITIES.computeIfAbsent(rocket.getRocketId(), ignored -> new HashMap<>());
            var blocks = collectBlocks(rocket, position, partialTick, level, modelResolver, blockEntityDispatcher, modelCache, visualEntities);
            if (flight.impactSpeedMetersPerSecond() <= 12 && flight.reentryTick() >= 0 && gameTime >= flight.reentryTick()) {
                // Placeholder folded hardware opens into a broad white canopy with four ropes.
                var seen = new HashSet<BlockPos>();
                for (var segment : rocket.getStaticSegments().values())
                    for (var block : segment.blocks()) {
                        if (!block.state().is(SpaceAgeBlocks.PARACHUTE)) continue;
                        var canopy = block.relativePos().above(5);
                        for (int x = -2; x <= 2; x++)
                            for (int z = -2; z <= 2; z++)
                                addBlock(blocks, seen, canopy.offset(x, Math.abs(x) == 2 || Math.abs(z) == 2 ? 0 : 1, z),
                                        Blocks.WHITE_CARPET.defaultBlockState(), BlockPos.containing(position), partialTick,
                                        level, modelResolver, blockEntityDispatcher, modelCache, visualEntities);
                        for (int y = 1; y < 5; y++)
                            for (int x : new int[]{-1, 1})
                                for (int z : new int[]{-1, 1})
                                    addBlock(blocks, seen, block.relativePos().offset(x, y, z),
                                            Blocks.IRON_CHAIN.defaultBlockState(), BlockPos.containing(position), partialTick,
                                            level, modelResolver, blockEntityDispatcher, modelCache, visualEntities);
                    }
            }
            var light = LevelRenderer.getLightCoords(level, BlockPos.containing(position));
            rockets.add(new RenderedRocket(position, light, blocks));
        }

        VISUAL_ENTITIES.keySet().retainAll(visibleRockets);
        if (!rockets.isEmpty()) {
            event.getRenderState().setRenderData(ROCKET_DATA, new RocketRenderData(rockets, event.getCamera().position()));
        }
    }

    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {

        var data = event.getLevelRenderState().getRenderData(ROCKET_DATA);
        if (data == null) return;

        var poseStack = event.getPoseStack();
        var collector = event.getSubmitNodeCollector();
        var blockEntityDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        var cameraState = event.getLevelRenderState().cameraRenderState;

        for (var rocket : data.rockets()) {
            var offset = rocket.position().subtract(data.cameraPosition());
            poseStack.pushPose();
            poseStack.translate(offset.x - 0.5, offset.y, offset.z - 0.5);

            for (var block : rocket.blocks()) {
                poseStack.pushPose();
                poseStack.translate(block.relativePosition().getX(), block.relativePosition().getY(), block.relativePosition().getZ());
                block.model().submitMultiLayer(poseStack, collector, rocket.light(), OverlayTexture.NO_OVERLAY, 0);
                if (block.blockEntity() != null) {
                    blockEntityDispatcher.submit(block.blockEntity(), poseStack, collector, cameraState);
                }
                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }

    private static List<RenderedBlock> collectBlocks(ActiveRocketData rocket, Vec3 rocketPosition, float partialTick,
                                                     ClientLevel level, BlockModelResolver modelResolver,
                                                     BlockEntityRenderDispatcher blockEntityDispatcher,
                                                     Map<BlockState, BlockModelRenderState> modelCache, Map<BlockPos, BlockEntity> visualEntities) {

        var blocks = new ArrayList<RenderedBlock>();
        var renderedPositions = new HashSet<BlockPos>();
        var rocketBlockPosition = BlockPos.containing(rocketPosition);

        for (var entry : rocket.getStaticSegments().entrySet()) {
            var segment = entry.getValue();
            for (var block : segment.blocks()) {
                addBlock(blocks, renderedPositions, block.relativePos(), block.state(), rocketBlockPosition, partialTick, level, modelResolver, blockEntityDispatcher, modelCache, visualEntities);
            }

            // couplings are separate from segment blocks and only exist while the segments are connected
            var dynamicSegment = rocket.getDynamicSegments().get(entry.getKey());
            for (var connectedSegment : dynamicSegment.getConnectedSegments()) {
                var couplings = segment.getCouplingsToSegment(connectedSegment);
                if (couplings == null) continue;
                for (var coupling : couplings) {
                    addBlock(blocks, renderedPositions, coupling.relativePos(), coupling.state(), rocketBlockPosition, partialTick, level, modelResolver,
                            blockEntityDispatcher, modelCache, visualEntities);
                }
            }
        }

        visualEntities.keySet().retainAll(renderedPositions);
        return blocks;
    }

    private static void addBlock(List<RenderedBlock> blocks, HashSet<BlockPos> renderedPositions, BlockPos position,
                                  BlockState state, BlockPos rocketBlockPosition, float partialTick, ClientLevel level,
                                  BlockModelResolver modelResolver, BlockEntityRenderDispatcher blockEntityDispatcher,
                                  Map<BlockState, BlockModelRenderState> modelCache, Map<BlockPos, BlockEntity> visualEntities) {

        if (state.isAir() || !renderedPositions.add(position)) return;

        var model = modelCache.computeIfAbsent(state, ignored -> {
            var result = new BlockModelRenderState();
            if (state.getRenderShape() != RenderShape.INVISIBLE)
                modelResolver.update(result, state, DISPLAY_CONTEXT);
            return result;
        });

        var blockEntity = visualEntities.get(position);
        if (blockEntity == null || blockEntity.getLevel() != level || blockEntity.getBlockState() != state) {
            blockEntity = BlockPreviewEntities.create(rocketBlockPosition.offset(position), state, level);
            if (blockEntity != null) visualEntities.put(position, blockEntity);
            else visualEntities.remove(position);
        }
        var blockEntityState = extractBlockEntity(blockEntity, rocketBlockPosition.offset(position), partialTick,
                level, blockEntityDispatcher);

        blocks.add(new RenderedBlock(position, model, blockEntityState));
    }

    private static @Nullable BlockEntityRenderState extractBlockEntity(@Nullable BlockEntity entity, BlockPos position,
                                                                      float partialTick, ClientLevel level,
                                                                      BlockEntityRenderDispatcher dispatcher) {

        if (entity == null) return null;
        var renderer = dispatcher.<BlockEntity, BlockEntityRenderState>getRenderer(entity);
        if (renderer == null) return null;

        // Detached entities keep a fixed animation identity; visibility and lighting follow the moving rocket.
        var state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, partialTick, Minecraft.getInstance().gameRenderer.getMainCamera().position(), null);
        state.blockPos = position;
        state.lightCoords = LevelRenderer.getLightCoords(level, position);
        if (state instanceof GeoRenderState geo) {
            geo.addGeckolibData(DataTickets.PACKED_LIGHT, state.lightCoords);
            geo.addGeckolibData(DataTickets.POSITION, Vec3.atCenterOf(position));
        }
        return state;
    }

    private record RocketRenderData(List<RenderedRocket> rockets, Vec3 cameraPosition) {

    }

    private record RenderedRocket(Vec3 position, int light, List<RenderedBlock> blocks) {

    }

    private record RenderedBlock(BlockPos relativePosition, BlockModelRenderState model,
                                 @Nullable BlockEntityRenderState blockEntity) {

    }
}
