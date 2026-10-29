package rearth.oritech.spaceage.simulation.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import rearth.oritech.spaceage.block.cubesat.CubeSatBlockEntity;
import rearth.oritech.spaceage.block.vacuum_crafter.VacuumCrafterBlockEntity;
import rearth.oritech.spaceage.simulation.service.PhysicalCrafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.IntBinaryOperator;

/**
 * Ordinary hardware lands as freshly placed blocks. Only explicitly supported cargo is restored.
 */
public final class RocketRecovery {

    private RocketRecovery() {
    }

    public static boolean restore(ServerLevel level, ActiveRocketData rocket, BlockPos origin) {

        // An unfinished local recipe lands with its original inputs; spent RF is not refunded.
        rocket.getDynamicSegments().values().forEach(resources -> resources.crafters.values().forEach(machine -> {
            if (machine.job != null && machine.job.source().rocket().equals(rocket.getRocketId()))
                PhysicalCrafting.finish(machine, rocket, true);
        }));
        var blocks = restorationBlocks(rocket);
        // Check the whole footprint before placing anything, including reserved recipe inputs.
        for (var block : blocks) {
            var pos = origin.offset(block.relativePos());
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.getBlockState(pos).canBeReplaced())
                return false;
        }
        for (var block : blocks) {
            var pos = origin.offset(block.relativePos());
            level.setBlock(pos, block.state(), 3);
        }
        // Generic block entities are freshly placed. Only controller names and undeployed cube-sats are explicitly restored.
        rocket.getDynamicSegments().values().forEach(resources -> {
            resources.crafters.forEach((relative, machine) -> {
                if (level.getBlockEntity(origin.offset(relative)) instanceof VacuumCrafterBlockEntity controller)
                    controller.setName(machine.name);
            });
            resources.payloads.forEach((relative, cube) -> {
                if (level.getBlockEntity(origin.offset(relative)) instanceof CubeSatBlockEntity entity)
                    entity.setData(cube);
            });
        });
        return true;
    }

    public static int highestBlockY(ActiveRocketData rocket, BlockPos origin) {

        return origin.getY() + restorationBlocks(rocket).stream().mapToInt(block -> block.relativePos().getY()).max().orElse(0);
    }

    public static BlockPos landingPosition(ServerLevel level, ActiveRocketData rocket, int x, int z) {

        return landingPosition(rocket, x, z, (worldX, worldZ) -> level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ));
    }

    public static BlockPos landingPosition(ActiveRocketData rocket, int x, int z, IntBinaryOperator surfaceHeight) {

        var centre = footprintCenter(rocket);
        var originX = x - centre.getX();
        var originZ = z - centre.getZ();
        return new BlockPos(originX, landingOriginY(rocket, originX, originZ, surfaceHeight), originZ);
    }

    public static BlockPos footprintCenter(ActiveRocketData rocket) {

        return center(restorationBlocks(rocket).stream().map(StaticRocketSegment.BlockData::relativePos).toList());
    }

    /**
     * Nearest block-grid centre; differing even/odd footprints can only align to half a block.
     */
    public static BlockPos center(Collection<BlockPos> positions) {

        if (positions.isEmpty()) return BlockPos.ZERO;
        var x = positions.stream().mapToInt(BlockPos::getX).summaryStatistics();
        var y = positions.stream().mapToInt(BlockPos::getY).summaryStatistics();
        var z = positions.stream().mapToInt(BlockPos::getZ).summaryStatistics();
        return new BlockPos(Math.floorDiv(x.getMin() + x.getMax(), 2), Math.floorDiv(y.getMin() + y.getMax(), 2),
                Math.floorDiv(z.getMin() + z.getMax(), 2));
    }

    public static int landingOriginY(ActiveRocketData rocket, int x, int z, IntBinaryOperator surfaceHeight) {

        var originY = Integer.MIN_VALUE;
        for (var block : restorationBlocks(rocket)) {
            var relative = block.relativePos();
            originY = Math.max(originY, surfaceHeight.applyAsInt(x + relative.getX(), z + relative.getZ()) - relative.getY());
        }
        return originY;
    }

    private static ArrayList<StaticRocketSegment.BlockData> restorationBlocks(ActiveRocketData rocket) {

        var blocks = new ArrayList<>(rocket.getStaticSegments().values().stream().flatMap(s -> s.blocks().stream()).filter(b -> !b.state().isAir()).toList());
        rocket.getDynamicSegments().values().forEach(resources -> resources.reserved.forEach((pos, state) ->
                blocks.add(new StaticRocketSegment.BlockData(pos, state))));
        var couplings = new HashMap<BlockPos, BlockState>();
        rocket.getStaticSegments().forEach((id, segment) -> segment.originalCouplings().forEach((other, links) -> {
            if (rocket.getDynamicSegments().get(id).getConnectedSegments().contains(other))
                links.forEach(link -> couplings.put(link.relativePos(), link.state()));
        }));
        couplings.forEach((pos, state) -> blocks.add(new StaticRocketSegment.BlockData(pos, state)));
        return blocks;
    }
}
