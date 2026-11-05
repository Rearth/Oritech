package rearth.oritech.spaceage.client.rocket;

import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RocketClientController {

    private static final Map<UUID, ActiveRocketData> ACTIVE_ROCKETS = new HashMap<>();

    private RocketClientController() {
    }

    public static Collection<ActiveRocketData> getActiveRockets() {

        return ACTIVE_ROCKETS.values();
    }

    public static void receiveRocket(ActiveRocketData rocket, List<UUID> engineSegments) {

        RocketEffects.checkWorld();
        ACTIVE_ROCKETS.put(rocket.getRocketId(), rocket);
        RocketEffects.receive(rocket, engineSegments);
        OritechSpaceAge.LOGGER.debug("Received client rocket snapshot {} with {} segments", rocket.getRocketId(), rocket.getStaticSegments().size());
    }

    public static void removeRocket(UUID rocketId) {

        var removed = ACTIVE_ROCKETS.remove(rocketId);
        if (removed != null) {
            RocketEffects.removed(removed);
            OritechSpaceAge.LOGGER.debug("Removed rocket {} from the client", rocketId);
        }
    }

    public static void clearRockets() {

        ACTIVE_ROCKETS.clear();
        RocketRenderer.clearVisualEntities();
        RocketEffects.clear();
        OritechSpaceAge.LOGGER.debug("Cleared client rocket snapshots");
    }
}
