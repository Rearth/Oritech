package rearth.oritech.init.world.features.resourcenode;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.Oritech;

// WeightedBlockPool is a list of valid registered blockstates with their weights
public class WeightedBlockPool {
    private final List<Entry> entries;
    private final int totalWeight;

    public WeightedBlockPool(List<WeightedBlock> weightedBlocks) {

        this.entries = weightedBlocks.stream()
            .map(WeightedBlockPool::resolve)
            .flatMap(Optional::stream)
            .toList();

        if (this.entries.isEmpty()) {
            throw new IllegalStateException("No valid entries in resource node block pool");
        }

        this.totalWeight = this.entries.stream().mapToInt(Entry::weight).sum();
    }

    private static Optional<Entry> resolve(WeightedBlock weightedBlock) {
        var holder = BuiltInRegistries.BLOCK.get(weightedBlock.block());

        if (holder.isEmpty()) {
            if (weightedBlock.required()) {
                throw new IllegalStateException("Required block does not exist: " + weightedBlock.block());
            }
            Oritech.LOGGER.debug("Skipping optional unknown block: {}", weightedBlock.block());
            return Optional.empty();
        }

        boolean requirementsMet = weightedBlock.requiresAny().isEmpty()
            || weightedBlock.requiresAny().stream()
                .anyMatch(blockId -> BuiltInRegistries.BLOCK.get(blockId).isPresent());

        if (!requirementsMet) {
            if (weightedBlock.required()) {
                throw new IllegalStateException("Required block has unmet requirements: " + weightedBlock.block() + ", " + weightedBlock.requiresAny());
            }
            Oritech.LOGGER.debug("Skipping optional block with unmet requirements: {} requires one of {}",
                weightedBlock.block(),
                weightedBlock.requiresAny());
            return Optional.empty();
        }

        return Optional.of(new Entry(holder.get().value().defaultBlockState(), weightedBlock.weight()));
    }

    // Gets a random blockstate from the pool, respecting block weights
    public BlockState getRandom(RandomSource random) {
        int roll = random.nextInt(this.totalWeight);

        for (var entry : this.entries) {
            roll -= entry.weight();

            if (roll < 0) {
                return entry.state();
            }
        }

        // Should never happen, but just to be safe
        return entries.getLast().state();
    }

    private record Entry(BlockState state, int weight) {}
}
