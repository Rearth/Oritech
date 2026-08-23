package rearth.oritech.spaceage.client;

import net.minecraft.world.level.Level;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.ActiveRocketData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RocketClientController {

    private static final Map<UUID, ActiveRocketData> ACTIVE_ROCKETS = new HashMap<>();

    private RocketClientController() {
    }

    public static Collection<ActiveRocketData> getActiveRockets() {
        return ACTIVE_ROCKETS.values();
    }

    public static void receiveRocket(ActiveRocketData rocket) {
        ACTIVE_ROCKETS.put(rocket.getRocketId(), rocket);
        OritechSpaceAge.LOGGER.debug("Received client rocket snapshot {} with {} segments", rocket.getRocketId(), rocket.getStaticSegments().size());
    }

    public static void removeRocket(UUID rocketId) {
        if (ACTIVE_ROCKETS.remove(rocketId) != null)
            OritechSpaceAge.LOGGER.debug("Removed rocket {} from the client", rocketId);
    }

    // orbiting rockets stay on the server until they need client rendering again
    public static void unloadRocketsInSpace(Level level, double gameTime) {
        ACTIVE_ROCKETS.values().removeIf(rocket -> {
            var flight = rocket.getFlight();
            return flight != null && flight.dimension().equals(level.dimension()) && flight.isInSpace(gameTime);
        });
    }

    public static void clearRockets() {
        ACTIVE_ROCKETS.clear();
        OritechSpaceAge.LOGGER.debug("Cleared client rocket snapshots");
    }
}
