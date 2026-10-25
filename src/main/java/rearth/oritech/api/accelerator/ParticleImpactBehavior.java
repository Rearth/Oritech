package rearth.oritech.api.accelerator;

import java.util.Optional;

/**
 * Reacts to an accelerator particle hitting a block after leaving the ring. Register implementations with {@link ParticleImpactBehaviors#register}.
 */
@FunctionalInterface
public interface ParticleImpactBehavior {

    /**
     * Called on the server for each targetable (non-air, non-passthrough) block along the exit path of a particle.
     *
     * @return a result if this behavior handled the impact, or empty to let the next behavior try
     */
    Optional<ParticleImpactResult> onImpact(ParticleImpactContext context);

}
