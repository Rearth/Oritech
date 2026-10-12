package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.HardwareRef;

import java.util.Optional;
import java.util.UUID;

/**
 * Persisted machine state. Inputs belong to source cells, never to an inventory.
 */
public final class RocketControllerState {

    public static final Codec<RocketControllerState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(c -> c.name), Job.CODEC.optionalFieldOf("job").forGetter(c -> Optional.ofNullable(c.job))
    ).apply(i, RocketControllerState::new));
    public String name;
    public Job job;

    public RocketControllerState() {

        this("", Optional.empty());
    }

    public RocketControllerState(String name, Optional<Job> job) {

        this.name = name;
        this.job = job.orElse(null);
    }

    public void reassignOperator(UUID previous, UUID next) {

        if (job != null && job.operator().equals(previous))
            job = new Job(job.recipe(), job.source(), job.first(), job.second(), job.result(), job.duration(), job.rfPerTick(), job.elapsed(), next);
    }

    public RocketControllerState copy() {

        return new RocketControllerState(name, Optional.ofNullable(job));
    }

    public record Pair(UUID rocket, HardwareRef controller, BlockPos left,
                       BlockPos right) {

        public static final Codec<Pair> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("rocket").forGetter(Pair::rocket), HardwareRef.CODEC.fieldOf("controller").forGetter(Pair::controller),
                BlockPos.CODEC.fieldOf("left").forGetter(Pair::left), BlockPos.CODEC.fieldOf("right").forGetter(Pair::right)
        ).apply(i, Pair::new));
    }

    public record Job(Identifier recipe, Pair source, BlockState first, BlockState second, BlockState result,
                      int duration, int rfPerTick, int elapsed, UUID operator) {

        public static final Codec<Job> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("recipe").forGetter(Job::recipe), Pair.CODEC.fieldOf("source").forGetter(Job::source),
                BlockState.CODEC.fieldOf("first").forGetter(Job::first), BlockState.CODEC.fieldOf("second").forGetter(Job::second),
                BlockState.CODEC.fieldOf("result").forGetter(Job::result), Codec.INT.fieldOf("duration").forGetter(Job::duration),
                Codec.INT.fieldOf("rf_per_tick").forGetter(Job::rfPerTick), Codec.INT.fieldOf("elapsed").forGetter(Job::elapsed),
                UUIDUtil.STRING_CODEC.fieldOf("operator").forGetter(Job::operator)
        ).apply(i, Job::new));

        public Job progressed(int ticks) {

            return new Job(recipe, source, first, second, result, duration, rfPerTick, elapsed + ticks, operator);
        }
    }
}
