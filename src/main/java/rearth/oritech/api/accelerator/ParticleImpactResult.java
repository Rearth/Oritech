package rearth.oritech.api.accelerator;

/**
 * The outcome of a handled particle impact.
 *
 * @param usedMomentum momentum absorbed by the hit block. The particle stops once its remaining momentum drops to about zero.
 * @param consumedItem whether the behavior took the particle item. Later blocks along the path then see an empty item.
 * @param consumeParticle whether to end the exit path and its rendered trail at this block, regardless of remaining momentum.
 */
public record ParticleImpactResult(long usedMomentum, boolean consumedItem, boolean consumeParticle) {

    /**
     * Stops the particle at this block.
     */
    public static ParticleImpactResult absorbed(ParticleImpactContext context, boolean consumedItem) {
        return new ParticleImpactResult(context.speed(), consumedItem, true);
    }
}
