package rearth.oritech.api.splicer;

import java.util.Optional;

/**
 * Finds something a splicer can compact. Register implementations with {@link SplicerBehaviors#register}.
 */
@FunctionalInterface
public interface SplicerBehavior {

    /**
     * Called on the server whenever the splicer refreshes its target (on GUI open, addon changes, periodically while the GUI is open, and before compacting).
     *
     * @return an operation if this behavior found a valid target, or empty to let the next behavior try
     */
    Optional<SplicerOperation> findOperation(SplicerContext context);

}
