package rearth.oritech.api.accelerator;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Registry of all particle impact behaviors. Behaviors are queried from highest to lowest priority, the first returned result is used.
 * The default oritech behavior (black holes from portals, breaking blocks) is registered with priority 0.
 */
public final class ParticleImpactBehaviors {

    private static final List<Entry> BEHAVIORS = new ArrayList<>();

    private ParticleImpactBehaviors() {
    }

    /**
     * Registers a behavior. Registering again with the same id replaces the existing behavior.
     * Should be called during common setup (e.g. in `FMLCommonSetupEvent#enqueueWork`).
     */
    public static synchronized void register(Identifier id, int priority, ParticleImpactBehavior behavior) {
        BEHAVIORS.removeIf(entry -> entry.id().equals(id));
        BEHAVIORS.add(new Entry(id, priority, behavior));
        BEHAVIORS.sort(Comparator.comparingInt(Entry::priority).reversed());
    }

    public static synchronized List<Entry> getBehaviors() {
        return List.copyOf(BEHAVIORS);
    }

    public static Optional<ParticleImpactResult> onImpact(ParticleImpactContext context) {
        for (var entry : getBehaviors()) {
            var result = entry.behavior().onImpact(context);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    public record Entry(Identifier id, int priority, ParticleImpactBehavior behavior) {
    }
}
