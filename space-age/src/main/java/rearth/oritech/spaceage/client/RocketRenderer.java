package rearth.oritech.spaceage.client;

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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.RocketSimulationController;

import java.util.*;

public final class RocketRenderer {

    private static final ContextKey<RocketRenderData> ROCKET_DATA = new ContextKey<>(OritechSpaceAge.id("rockets"));
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    public static void onExtractRenderState(ExtractLevelRenderStateEvent event) {

        var level = event.getLevel();
        var partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var gameTime = level.getGameTime() + partialTick;

        RocketClientController.unloadRocketsInSpace(level, gameTime);

        var minecraft = Minecraft.getInstance();
        var modelResolver = minecraft.getBlockModelResolver();
        var blockEntityDispatcher = minecraft.getBlockEntityRenderDispatcher();
        var modelCache = new HashMap<BlockState, BlockModelRenderState>();
        var rockets = new ArrayList<RenderedRocket>();

        for (var rocket : RocketClientController.getActiveRockets()) {
            var flight = rocket.getFlight();
            if (flight == null || !flight.canReachOrbit() || !flight.dimension().equals(level.dimension())) continue;

            var position = RocketSimulationController.getRocketPosition(flight, gameTime);
            var blocks = collectBlocks(rocket, position, partialTick, level, modelResolver, blockEntityDispatcher, modelCache);
            var light = LevelRenderer.getLightCoords(level, BlockPos.containing(position));
            rockets.add(new RenderedRocket(position, light, blocks));
        }

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

    private static List<RenderedBlock> collectBlocks(ActiveRocketData rocket, Vec3 rocketPosition, float partialTick, ClientLevel level, BlockModelResolver modelResolver, BlockEntityRenderDispatcher blockEntityDispatcher, Map<BlockState, BlockModelRenderState> modelCache) {
        var blocks = new ArrayList<RenderedBlock>();
        var renderedPositions = new HashSet<BlockPos>();
        var rocketBlockPosition = BlockPos.containing(rocketPosition);

        for (var entry : rocket.getStaticSegments().entrySet()) {
            var segment = entry.getValue();
            for (var block : segment.blocks()) {
                addBlock(blocks, renderedPositions, block.relativePos(), block.state(), rocketBlockPosition, partialTick, level, modelResolver, blockEntityDispatcher, modelCache);
            }

            // couplings are separate from segment blocks and only exist while the segments are connected
            var dynamicSegment = rocket.getDynamicSegments().get(entry.getKey());
            for (var connectedSegment : dynamicSegment.getConnectedSegments()) {
                var couplings = segment.getCouplingsToSegment(connectedSegment);
                if (couplings == null) continue;
                for (var coupling : couplings) {
                    addBlock(blocks, renderedPositions, coupling.relativePos(), SpaceAgeBlocks.ROCKET_COUPLING.get().defaultBlockState(), rocketBlockPosition, partialTick, level, modelResolver, blockEntityDispatcher, modelCache);
                }
            }
        }

        return blocks;
    }

    private static void addBlock(List<RenderedBlock> blocks, HashSet<BlockPos> renderedPositions, BlockPos position, BlockState state, BlockPos rocketBlockPosition, float partialTick, ClientLevel level, BlockModelResolver modelResolver, BlockEntityRenderDispatcher blockEntityDispatcher, Map<BlockState, BlockModelRenderState> modelCache) {
        if (state.isAir() || !renderedPositions.add(position)) return;

        var model = modelCache.computeIfAbsent(state, ignored -> {
            var result = new BlockModelRenderState();
            modelResolver.update(result, state, DISPLAY_CONTEXT);
            return result;
        });

        BlockEntityRenderState blockEntityState = null;
        if (state.getBlock() instanceof EntityBlock entityBlock) {
            var blockEntity = entityBlock.newBlockEntity(rocketBlockPosition.offset(position), state);
            if (blockEntity != null) {
                blockEntity.setLevel(level);
                blockEntityState = blockEntityDispatcher.tryExtractRenderState(blockEntity, partialTick, null, null);
            }
        }

        blocks.add(new RenderedBlock(position, model, blockEntityState));
    }

    private record RocketRenderData(List<RenderedRocket> rockets, Vec3 cameraPosition) {
    }

    private record RenderedRocket(Vec3 position, int light, List<RenderedBlock> blocks) {
    }

    private record RenderedBlock(BlockPos relativePosition, BlockModelRenderState model,
                                 @Nullable BlockEntityRenderState blockEntity) {
    }
}
