package rearth.oritech.api.splicer;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Registry of all splicer behaviors. Behaviors are queried from highest to lowest priority, the first found operation is used.
 * The default oritech addon behavior is registered with priority 0.
 */
public final class SplicerBehaviors {

    private static final List<Entry> BEHAVIORS = new ArrayList<>();

    private SplicerBehaviors() {
    }

    /**
     * Registers a behavior. Registering again with the same id replaces the existing behavior.
     * Should be called during common setup (e.g. in `FMLCommonSetupEvent#enqueueWork`).
     */
    public static synchronized void register(Identifier id, int priority, SplicerBehavior behavior) {
        BEHAVIORS.removeIf(entry -> entry.id().equals(id));
        BEHAVIORS.add(new Entry(id, priority, behavior));
        BEHAVIORS.sort(Comparator.comparingInt(Entry::priority).reversed());
    }

    public static synchronized List<Entry> getBehaviors() {
        return List.copyOf(BEHAVIORS);
    }

    public static Optional<SplicerOperation> findOperation(SplicerContext context) {
        for (var entry : getBehaviors()) {
            var result = entry.behavior().findOperation(context);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    public record Entry(Identifier id, int priority, SplicerBehavior behavior) {
    }
}
