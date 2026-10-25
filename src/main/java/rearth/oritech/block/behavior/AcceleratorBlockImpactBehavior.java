package rearth.oritech.block.behavior;

import rearth.oritech.Oritech;
import rearth.oritech.api.accelerator.ParticleImpactBehavior;
import rearth.oritech.api.accelerator.ParticleImpactBehaviors;
import rearth.oritech.api.accelerator.ParticleImpactContext;
import rearth.oritech.api.accelerator.ParticleImpactResult;

import java.util.Optional;

/**
 * Default particle impact behavior: creates black holes from portals and breaks blocks softer than the remaining momentum.
 */
public class AcceleratorBlockImpactBehavior implements ParticleImpactBehavior {

    public static void registerDefaults() {
        ParticleImpactBehaviors.register(Oritech.id("block_impact"), 0, new AcceleratorBlockImpactBehavior());
    }

    @Override
    public Optional<ParticleImpactResult> onImpact(ParticleImpactContext context) {
        var used = context.accelerator().handleParticleBlockCollision(context.hitPos(), context.speed(), context.hitState());
        return Optional.of(new ParticleImpactResult((long) Math.ceil(used), false));
    }
}
