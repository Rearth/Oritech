package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2i;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.network.RocketNetworking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class RocketSimulationController {

    // config
    public static final int TICKS_PER_SECOND = RocketPerformanceCalculator.TICKS_PER_SECOND;
    public static final int ORBIT_HEIGHT_BLOCKS = RocketPerformanceCalculator.LAUNCH_ORBIT_HEIGHT_BLOCKS;

    // physics settings
    private static final double STANDARD_GRAVITY = RocketPerformanceCalculator.STANDARD_GRAVITY;
    private static final float TAKEOFF_EXPLOSION_STRENGTH = 6;
    private static final double ROCKET_PACKET_RANGE_BLOCKS = 500;
    private static final double ROCKET_PACKET_RANGE_SQUARED = ROCKET_PACKET_RANGE_BLOCKS * ROCKET_PACKET_RANGE_BLOCKS;

    public static void launchMissionVisual(ServerLevel level, ActiveRocketData rocket, BlockPos launchPosition) {
        var performance = RocketPerformanceCalculator.calculate(rocket);
        var ascentSeconds = Math.sqrt(2 * ORBIT_HEIGHT_BLOCKS / Math.max(0.1, performance.liftoffAccelerationMetersPerSecondSquared() - STANDARD_GRAVITY));
        var flight = new RocketFlight(true, null, level.dimension(), performance, launchPosition,
                launchPosition.above(ORBIT_HEIGHT_BLOCKS), launchPosition, new Vector2i(0, ORBIT_HEIGHT_BLOCKS),
                0, 0, secondsToTicks(ascentSeconds), -1, -1, null, -1);
        flight = handleTakeoffCollisions(level, rocket, flight).scheduledAt(level.getGameTime());
        rocket.setFlight(flight);
        var saved = getSavedData(level); saved.rockets.put(rocket.getRocketId(), rocket); saved.setDirty();
        sendTakeoffDataToClients(level, rocket);
    }

    public static void beginReentry(ServerLevel level, ActiveRocketData rocket, BlockPos landing, long ticks, double speed) {
        var now = level.getGameTime();
        if (speed <= 12) ticks = Math.max(200, ticks);
        rocket.setFlight(new RocketFlight(true, null, level.dimension(), RocketPerformanceCalculator.calculate(rocket),
                landing, landing.above(ORBIT_HEIGHT_BLOCKS), landing, new Vector2i(0, 0), 0, speed,
                now - 1, now, now + ticks, null, -1));
        var data = getSavedData(level); data.rockets.put(rocket.getRocketId(), rocket); data.setDirty();
        sendReentryDataToClients(level, rocket);
    }

    public static Map<UUID, ActiveRocketData> getActiveRockets(ServerLevel level) {
        return Map.copyOf(getSavedData(level).rockets);
    }

    public static void markDirty(ServerLevel level) {
        getSavedData(level).setDirty();
    }

    public static void syncActiveRocketsToPlayer(ServerPlayer player) {
        var savedData = getSavedData(player.level());
        RocketNetworking.clearRockets(player);
        var selectedRockets = 0;
        for (var rocket : savedData.rockets.values()) {
            if (sendActiveRocketDataToClient(player, rocket)) selectedRockets++;
        }
        OritechSpaceAge.LOGGER.debug("Selected {} of {} active rockets for client sync to {}", selectedRockets, savedData.rockets.size(), player.getGameProfile().name());
    }

    // processes scheduled collisions and flight events once per server tick
    public static void tick(MinecraftServer server) {
        var savedData = getSavedData(server);
        var changed = false;
        var iterator = savedData.rockets.entrySet().iterator();

        while (iterator.hasNext()) {
            var rocket = iterator.next().getValue();
            var flight = rocket.getFlight();
            var level = server.getLevel(flight.dimension());
            if (level == null) continue;
            var gameTime = level.getGameTime();

            if (flight.takeoffCollisionPosition() != null && gameTime >= flight.takeoffCollisionTick()) {
                OritechSpaceAge.LOGGER.debug("Rocket {} hit an ascent obstruction at {} on tick {}", rocket.getRocketId(), flight.takeoffCollisionPosition(), gameTime);
                sendTakeoffCollisionDataToClients(level, rocket, flight.takeoffCollisionPosition(), TAKEOFF_EXPLOSION_STRENGTH);
                explodeOnTakeoffCollision(level, flight.takeoffCollisionPosition());
                var mission = MissionSavedData.get(server).craft.get(rocket.getRocketId());
                if (mission != null) {
                    mission.ended = true; mission.status = MissionState.STATUS_DESTROYED;
                    mission.report(gameTime); MissionSavedData.get(server).receive(mission);
                }
                iterator.remove();
                changed = true;
            } else if (flight.canReachOrbit() && flight.impactTick() >= 0 && gameTime >= flight.impactTick()) {
                unloadOrbitRocketFromClients(level, rocket);
                iterator.remove();
                changed = true;
            } else if (flight.canReachOrbit()) {
                if (gameTime >= flight.orbitArrivalTick() && flight.reentryTick() < 0) {
                    OritechSpaceAge.LOGGER.debug("Rocket {} reached orbit {} on tick {}", rocket.getRocketId(), flight.targetOrbit(), gameTime);
                    unloadOrbitRocketFromClients(level, rocket);
                    iterator.remove(); changed = true;
                }
                if (gameTime == flight.reentryTick()) {
                    OritechSpaceAge.LOGGER.debug("Rocket {} began reentry toward {} on tick {}", rocket.getRocketId(), flight.impactPosition(), gameTime);
                    sendReentryDataToClients(level, rocket);
                }
            }
        }

        if (changed) {
            savedData.setDirty();
            OritechSpaceAge.LOGGER.debug("Active rocket data changed; {} rockets remain", savedData.rockets.size());
        }
    }

    // rocket data is shared between dimensions and stored in the overworld data storage
    private static ActiveRocketSavedData getSavedData(ServerLevel level) {
        return getSavedData(level.getServer());
    }

    private static ActiveRocketSavedData getSavedData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ActiveRocketSavedData.TYPE);
    }

    private static long secondsToTicks(double seconds) {
        return (long) Math.ceil(seconds * TICKS_PER_SECOND);
    }

    // clients use the saved event ticks to reproduce the same ascent and reentry movement
    public static Vec3 getRocketPosition(RocketFlight flight, double gameTime) {
        var launchPosition = Vec3.atBottomCenterOf(flight.launchPosition());
        var orbitPosition = Vec3.atBottomCenterOf(flight.orbitPosition());
        var impactPosition = Vec3.atBottomCenterOf(flight.impactPosition());

        if (!flight.canReachOrbit()) {
            return launchPosition;
        }

        if (gameTime < flight.orbitArrivalTick()) {
            var netAcceleration = flight.performance().liftoffAccelerationMetersPerSecondSquared() - STANDARD_GRAVITY;
            var ascentHeight = Math.max(0, flight.orbitPosition().getY() - flight.launchPosition().getY());
            var ascentSeconds = Math.sqrt(2 * ascentHeight / netAcceleration);
            var launchTick = flight.orbitArrivalTick() - secondsToTicks(ascentSeconds);
            var progress = Mth.clamp((gameTime - launchTick) / Math.max(1, flight.orbitArrivalTick() - launchTick), 0, 1);
            return launchPosition.lerp(orbitPosition, progress * progress);
        }

        if (flight.isInSpace(gameTime)) {
            return orbitPosition;
        }

        var progress = Mth.clamp((gameTime - flight.reentryTick()) / Math.max(1, flight.impactTick() - flight.reentryTick()), 0, 1);
        var duration = Math.max(1, flight.impactTick() - flight.reentryTick()) / 20.0;
        var height = Math.max(1, orbitPosition.y - impactPosition.y);
        return orbitPosition.lerp(impactPosition, descentProgress(progress, height, duration, flight.impactSpeedMetersPerSecond()));
    }

    static double descentProgress(double progress, double height, double seconds, double arrivalSpeed) {
        if (arrivalSpeed > 12) return progress * progress;
        // Constant braking: derivative at touchdown matches the requested speed (zero for a gentle landing).
        var finalSlope = Math.clamp(arrivalSpeed * seconds / height, 0, 2);
        return (2 - finalSlope) * progress + (finalSlope - 1) * progress * progress;
    }

    // scans the vertical area swept by the rocket and plans the first found collision
    private static RocketFlight handleTakeoffCollisions(ServerLevel level, ActiveRocketData rocket, RocketFlight flight) {
        var leadingBlocks = new HashMap<BlockPos, Integer>();
        for (var segment : rocket.getStaticSegments().values()) {
            for (var block : segment.blocks()) {
                var relativePos = block.relativePos();
                var originalWorldPos = flight.launchPosition().offset(relativePos);
                if (block.state().getCollisionShape(level, originalWorldPos).isEmpty()) continue;
                leadingBlocks.merge(new BlockPos(relativePos.getX(), 0, relativePos.getZ()), relativePos.getY(), Math::max);
            }
            for (var couplingSet : segment.originalCouplings().values()) {
                for (var coupling : couplingSet) {
                    var relativePos = coupling.relativePos();
                    leadingBlocks.merge(new BlockPos(relativePos.getX(), 0, relativePos.getZ()), relativePos.getY(), Math::max);
                }
            }
        }

        BlockPos collisionPosition = null;
        var collisionDistance = ORBIT_HEIGHT_BLOCKS + 1;
        for (var entry : leadingBlocks.entrySet()) {
            var column = entry.getKey();
            var rocketTopY = flight.launchPosition().getY() + entry.getValue();
            var worldX = flight.launchPosition().getX() + column.getX();
            var worldZ = flight.launchPosition().getZ() + column.getZ();
            var maxY = Math.min(level.getMaxY() - 1, rocketTopY + ORBIT_HEIGHT_BLOCKS);

            for (var y = rocketTopY + 1; y <= maxY && y - rocketTopY < collisionDistance; y++) {
                var checkedPos = new BlockPos(worldX, y, worldZ);
                var checkedState = level.getBlockState(checkedPos);
                if (!checkedState.getCollisionShape(level, checkedPos).isEmpty()) {
                    collisionDistance = y - rocketTopY;
                    collisionPosition = checkedPos;
                    break;
                }
            }
        }

        if (collisionPosition == null) {
            OritechSpaceAge.LOGGER.debug("Rocket {} has a clear ascent corridor", rocket.getRocketId());
            return flight;
        }

        var netAcceleration = flight.performance().liftoffAccelerationMetersPerSecondSquared() - STANDARD_GRAVITY;
        var collisionSeconds = Math.sqrt(2 * collisionDistance / netAcceleration);
        OritechSpaceAge.LOGGER.debug("Rocket {} ascent collision planned at {} after {} blocks / {} seconds", rocket.getRocketId(), collisionPosition, collisionDistance, collisionSeconds);
        return flight.withTakeoffCollision(collisionPosition, Math.max(1, secondsToTicks(collisionSeconds)));
    }

    private static void explodeOnTakeoffCollision(ServerLevel level, BlockPos position) {
        level.explode(null, position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5, TAKEOFF_EXPLOSION_STRENGTH, false, Level.ExplosionInteraction.BLOCK);
    }

    private static void sendTakeoffDataToClients(ServerLevel level, ActiveRocketData rocket) {
        var position = rocket.getFlight().launchPosition();
        var recipients = sendToPlayersNearRocket(level, position, player -> RocketNetworking.sendRocket(player, rocket));
        logClientSelection("takeoff", rocket, position, recipients);
    }

    private static void unloadOrbitRocketFromClients(ServerLevel level, ActiveRocketData rocket) {
        var position = rocket.getFlight().orbitPosition();
        var recipients = sendToPlayersNearRocket(level, position, player -> RocketNetworking.unloadRocket(player, rocket.getRocketId()));
        logClientSelection("orbit unload", rocket, position, recipients);
    }

    private static void sendReentryDataToClients(ServerLevel level, ActiveRocketData rocket) {
        var position = rocket.getFlight().orbitPosition();
        var recipients = sendToPlayersNearRocket(level, position, player -> RocketNetworking.sendRocket(player, rocket));
        logClientSelection("reentry", rocket, position, recipients);
    }

    private static void sendTakeoffCollisionDataToClients(ServerLevel level, ActiveRocketData rocket, BlockPos collisionPosition, float explosionStrength) {
        var recipients = sendToPlayersNearRocket(level, collisionPosition, player -> RocketNetworking.sendCollision(player, rocket.getRocketId(), collisionPosition, 0, explosionStrength, false));
        logClientSelection("takeoff collision", rocket, collisionPosition, recipients);
    }

    private static boolean sendActiveRocketDataToClient(ServerPlayer player, ActiveRocketData rocket) {
        var flight = rocket.getFlight();
        var gameTime = player.level().getGameTime();
        if (!flight.canReachOrbit() || !flight.dimension().equals(player.level().dimension()) || flight.isInSpace(gameTime))
            return false;

        var rocketPosition = BlockPos.containing(getRocketPosition(flight, gameTime));
        if (!isPlayerWithinRocketPacketRange(player, rocketPosition)) return false;

        RocketNetworking.sendRocket(player, rocket);
        OritechSpaceAge.LOGGER.debug("Sent client snapshot for rocket {} at {} to player {}", rocket.getRocketId(), rocketPosition, player.getGameProfile().name());
        return true;
    }

    // returns the number of nearby players
    private static int sendToPlayersNearRocket(ServerLevel level, BlockPos rocketPosition, Consumer<ServerPlayer> packetSender) {
        var recipients = 0;
        for (var player : level.players()) {
            if (isPlayerWithinRocketPacketRange(player, rocketPosition)) {
                packetSender.accept(player);
                recipients++;
            }
        }
        return recipients;
    }

    private static void logClientSelection(String event, ActiveRocketData rocket, BlockPos position, int recipients) {
        OritechSpaceAge.LOGGER.debug("Client {} sync for rocket {} selected {} players within {} blocks of {}", event, rocket.getRocketId(), recipients, ROCKET_PACKET_RANGE_BLOCKS, position);
    }

    private static boolean isPlayerWithinRocketPacketRange(ServerPlayer player, BlockPos rocketPosition) {
        var deltaX = player.getX() - (rocketPosition.getX() + 0.5);
        var deltaZ = player.getZ() - (rocketPosition.getZ() + 0.5);
        return deltaX * deltaX + deltaZ * deltaZ <= ROCKET_PACKET_RANGE_SQUARED;
    }

    private static final class ActiveRocketSavedData extends SavedData {

        private static final Codec<ActiveRocketSavedData> CODEC =
                ActiveRocketData.CODEC
                        .listOf()
                        .xmap(ActiveRocketSavedData::new, data -> List.copyOf(data.rockets.values()));

        private static final SavedDataType<ActiveRocketSavedData> TYPE = new SavedDataType<>(OritechSpaceAge.id("active_rockets"), ActiveRocketSavedData::new, CODEC, null);

        // State belongs to this server's overworld storage, never to the JVM running multiple worlds.
        private final Map<UUID, ActiveRocketData> rockets = new HashMap<>();

        private ActiveRocketSavedData() {
            OritechSpaceAge.LOGGER.debug("Initialized empty active rocket saved data");
        }

        private ActiveRocketSavedData(List<ActiveRocketData> rockets) {
            rockets.forEach(rocket -> this.rockets.put(rocket.getRocketId(), rocket));
            OritechSpaceAge.LOGGER.debug("Loaded {} active rockets from saved data", rockets.size());
        }
    }
}
