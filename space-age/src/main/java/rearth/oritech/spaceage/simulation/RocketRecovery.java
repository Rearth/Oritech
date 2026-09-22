package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import rearth.oritech.init.recipes.RecipeContent;
import java.util.*;
import java.util.function.IntBinaryOperator;

/** Restore captured inventories first, then remove the fuel and RF actually spent in flight. */
public final class RocketRecovery {
    public static boolean restore(ServerLevel level, ActiveRocketData rocket, BlockPos origin) {
        var blocks = restorationBlocks(rocket);
        for (var block : blocks) {
            var pos = origin.offset(block.relativePos());
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.getBlockState(pos).canBeReplaced()) return false;
        }
        for (var block : blocks) {
            var pos = origin.offset(block.relativePos());
            level.setBlock(pos, block.state(), 3);
            var entity = level.getBlockEntity(pos);
            if (entity != null && !block.entityData().isEmpty()) {
                entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), block.entityData()));
                entity.setChanged();
            }
        }
        for (var entry : rocket.getStaticSegments().entrySet()) {
            var segment = entry.getValue(); var resources = rocket.getDynamicSegments().get(entry.getKey());
            double energyFraction = segment.initialRF() == 0 ? 1 : resources.availableRF / (double) segment.initialRF();
            double fuelFraction = segment.initialFuel() == 0 ? 1 : resources.availableFuelBurnTimeTicks / (double) segment.initialFuel();
            for (var block : segment.blocks()) {
                var pos = origin.offset(block.relativePos());
                try (var transaction = Transaction.openRoot()) {
                    var energy = level.getCapability(Capabilities.Energy.BLOCK, pos, null);
                    if (energy instanceof rearth.oritech.api.transfer.energy.DynamicEnergyStorage storage) {
                        storage.internalExtract(Math.round(storage.getAmountAsLong() * (1 - energyFraction)), transaction);
                    } else if (energy != null) {
                        var remaining = Math.round(energy.getAmountAsLong() * (1 - energyFraction));
                        while (remaining > 0) {
                            var taken = energy.extract((int) Math.min(Integer.MAX_VALUE, remaining), transaction);
                            if (taken == 0) break;
                            remaining -= taken;
                        }
                    }
                    var fluid = level.getCapability(Capabilities.Fluid.BLOCK, pos, null);
                    if (fluid != null) {
                        var fuels = new HashSet<FluidResource>();
                        level.recipeAccess().recipeMap().byType(RecipeContent.FUEL_GENERATOR.get()).forEach(recipe ->
                                recipe.value().fluidInput().get().ingredient().fluids().forEach(f -> fuels.add(FluidResource.of(f))));
                        for (var fuel : fuels) {
                            long amount;
                            try (var probe = Transaction.open(transaction)) { amount = fluid.extract(fuel, Integer.MAX_VALUE, probe); }
                            fluid.extract(fuel, (int) Math.round(amount * (1 - fuelFraction)), transaction);
                        }
                    }
                    transaction.commit();
                }
            }
        }
        return true;
    }

    public static BlockPos landingPosition(ServerLevel level, ActiveRocketData rocket, int x, int z) {
        return new BlockPos(x, landingOriginY(rocket, x, z,
                (worldX, worldZ) -> level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        worldX, worldZ)), z);
    }

    static int landingOriginY(ActiveRocketData rocket, int x, int z, IntBinaryOperator surfaceHeight) {
        int originY = Integer.MIN_VALUE;
        for (var block : restorationBlocks(rocket)) {
            var relative = block.relativePos();
            originY = Math.max(originY, surfaceHeight.applyAsInt(x + relative.getX(), z + relative.getZ()) - relative.getY());
        }
        return originY;
    }

    private static ArrayList<StaticRocketSegment.BlockData> restorationBlocks(ActiveRocketData rocket) {
        var blocks = new ArrayList<>(rocket.getStaticSegments().values().stream().flatMap(s -> s.blocks().stream()).toList());
        var couplings = new HashSet<BlockPos>();
        rocket.getStaticSegments().forEach((id, segment) -> segment.originalCouplings().forEach((other, links) -> {
            if (rocket.getDynamicSegments().get(id).getConnectedSegments().contains(other))
                links.forEach(link -> couplings.add(link.relativePos()));
        }));
        couplings.forEach(pos -> blocks.add(new StaticRocketSegment.BlockData(pos,
                rearth.oritech.spaceage.init.SpaceAgeBlocks.ROCKET_COUPLING.get().defaultBlockState())));
        return blocks;
    }
    private RocketRecovery() { }
}
