package rearth.oritech.spaceage.block.launch_ramp;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import rearth.oritech.api.accelerator.ParticleImpactBehaviors;
import rearth.oritech.api.accelerator.ParticleImpactContext;
import rearth.oritech.api.accelerator.ParticleImpactResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uses priorities above the built-in behaviors, which are registered while the mod loads for tests.
 */
class ParticleImpactBehaviorsTest {

    private static final Identifier FALLBACK = Identifier.fromNamespaceAndPath("test", "fallback");
    private static final Identifier CUSTOM = Identifier.fromNamespaceAndPath("test", "custom");
    private static final ParticleImpactContext CONTEXT = new ParticleImpactContext(null, null, null, null, null, null, 500, null, null);

    @AfterEach
    void reset() {

        ParticleImpactBehaviors.register(FALLBACK, Integer.MIN_VALUE, context -> Optional.empty());
        ParticleImpactBehaviors.register(CUSTOM, Integer.MIN_VALUE, context -> Optional.empty());
    }

    @Test
    void higherPriorityBehaviorsHandleImpactsFirst() {

        ParticleImpactBehaviors.register(FALLBACK, 100, context -> Optional.of(new ParticleImpactResult(1, false)));
        assertEquals(1, ParticleImpactBehaviors.onImpact(CONTEXT).orElseThrow().usedMomentum());
        ParticleImpactBehaviors.register(CUSTOM, 200, context -> Optional.of(ParticleImpactResult.absorbed(context, true)));
        var result = ParticleImpactBehaviors.onImpact(CONTEXT).orElseThrow();
        assertEquals(500, result.usedMomentum());
        assertTrue(result.consumedItem());
    }

    @Test
    void emptyResultsFallThroughAndReRegisteringReplaces() {

        ParticleImpactBehaviors.register(FALLBACK, 100, context -> Optional.of(new ParticleImpactResult(1, false)));
        ParticleImpactBehaviors.register(CUSTOM, 200, context -> Optional.empty());
        assertEquals(1, ParticleImpactBehaviors.onImpact(CONTEXT).orElseThrow().usedMomentum());
        ParticleImpactBehaviors.register(CUSTOM, 200, context -> Optional.of(new ParticleImpactResult(7, false)));
        assertEquals(7, ParticleImpactBehaviors.onImpact(CONTEXT).orElseThrow().usedMomentum());
        assertEquals(1, ParticleImpactBehaviors.getBehaviors().stream().filter(entry -> entry.id().equals(CUSTOM)).count());
    }
}
