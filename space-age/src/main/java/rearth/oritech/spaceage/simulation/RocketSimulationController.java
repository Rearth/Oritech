package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2i;
import rearth.oritech.block.entity.reactor.NuclearExplosionEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.network.RocketNetworking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

public final class RocketSimulationController {

    // config
    public static final int TICKS_PER_SECOND = 20;
    public static final int ORBIT_HEIGHT_BLOCKS = 1_000;
    public static final int ORBIT_WAIT_TICKS = 10 * TICKS_PER_SECOND;

    // physics settings
    private static final double STANDARD_GRAVITY = 9.80665;
    private static final double KILOGRAMS_PER_WEIGHT_UNIT = 1_000;
    private static final double ENGINE_THRUST_NEWTONS = 250_000;
    private static final double ENGINE_SPECIFIC_IMPULSE_SECONDS = 300;
    private static final long RF_PER_ENGINE_TICK = 1_000;
    private static final double MINIMUM_IMPACT_RADIUS = 125;
    private static final double MAXIMUM_IMPACT_RADIUS = 175;
    private static final float TAKEOFF_EXPLOSION_STRENGTH = 6;
    private static final double REFERENCE_IMPACT_ENERGY_JOULES = 100_000_000;
    private static final int MINIMUM_NUCLEAR_STRENGTH = 1;  // todo increase again
    private static final int MAXIMUM_NUCLEAR_STRENGTH = 1;
    private static final double ROCKET_PACKET_RANGE_BLOCKS = 500;
    private static final double ROCKET_PACKET_RANGE_SQUARED = ROCKET_PACKET_RANGE_BLOCKS * ROCKET_PACKET_RANGE_BLOCKS;

    // actual data
    private static final Map<UUID, ActiveRocketData> ACTIVE_ROCKETS = new HashMap<>();

    // calculates the flight and adds the rocket to the saved active rockets
    public static void launchRocket(ServerLevel level, ActiveRocketData rocket, RocketFlightPlan flightPlan) {
        OritechSpaceAge.LOGGER.debug("Planning rocket launch {} from {} in {} with {} segments", rocket.getRocketId(), flightPlan.worldStart(), level.dimension().identifier(), rocket.getStaticSegments().size());

        var savedData = getSavedData(level);
        var flight = calculateFlight(rocket, flightPlan, ThreadLocalRandom.current(), level.dimension());
        OritechSpaceAge.LOGGER.debug("Rocket {} performance: wetMass={}kg, engines={}, thrust={}N, acceleration={}m/s², availableDeltaV={}m/s", rocket.getRocketId(), flight.performance().wetMassKilograms(), flight.performance().engineCount(), flight.performance().thrustNewtons(), flight.performance().liftoffAccelerationMetersPerSecondSquared(), flight.performance().availableDeltaVMetersPerSecond());

        if (flight.canReachOrbit()) {
            flight = handleImpactCollisions(level, flight);
            flight = handleTakeoffCollisions(level, rocket, flight);
            flight = flight.scheduledAt(level.getGameTime());
            OritechSpaceAge.LOGGER.debug("Rocket {} launched: orbitTick={}, reentryTick={}, impactTick={}, impactPos={}, impactSpeed={}m/s, ascentCollision={}", rocket.getRocketId(), flight.orbitArrivalTick(), flight.reentryTick(), flight.impactTick(), flight.impactPosition(), flight.impactSpeedMetersPerSecond(), flight.takeoffCollisionPosition());
        } else {
            OritechSpaceAge.LOGGER.debug("Rocket {} launch failed: {}", rocket.getRocketId(), flight.failureReason());
        }

        rocket.setFlight(flight);
        ACTIVE_ROCKETS.put(rocket.getRocketId(), rocket);
        savedData.setDirty();

        if (flight.canReachOrbit()) sendTakeoffDataToClients(level, rocket);
    }

    public static Map<UUID, ActiveRocketData> getActiveRockets(ServerLevel level) {
        getSavedData(level);
        return Map.copyOf(ACTIVE_ROCKETS);
    }

    public static void markDirty(ServerLevel level) {
        getSavedData(level).setDirty();
    }

    public static void syncActiveRocketsToPlayer(ServerPlayer player) {
        getSavedData(player.level());
        RocketNetworking.clearRockets(player);
        var selectedRockets = 0;
        for (var rocket : ACTIVE_ROCKETS.values()) {
            if (sendActiveRocketDataToClient(player, rocket)) selectedRockets++;
        }
        OritechSpaceAge.LOGGER.debug("Selected {} of {} active rockets for client sync to {}", selectedRockets, ACTIVE_ROCKETS.size(), player.getGameProfile().name());
    }

    // processes scheduled collisions and flight events once per server tick
    public static void tick(MinecraftServer server) {
        var savedData = getSavedData(server);
        var changed = false;
        var iterator = ACTIVE_ROCKETS.entrySet().iterator();

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
                iterator.remove();
                changed = true;
            } else if (flight.canReachOrbit() && flight.impactTick() >= 0 && gameTime >= flight.impactTick()) {
                var explosionStrength = calculateImpactExplosionStrength(flight);
                OritechSpaceAge.LOGGER.debug("Rocket {} impacted at {} on tick {} with speed {}m/s; nuclear strength={}", rocket.getRocketId(), flight.impactPosition(), gameTime, flight.impactSpeedMetersPerSecond(), explosionStrength);
                sendImpactDataToClients(level, rocket, explosionStrength);
                explodeOnImpact(level, flight, explosionStrength);
                iterator.remove();
                changed = true;
            } else if (flight.canReachOrbit()) {
                if (gameTime == flight.orbitArrivalTick()) {
                    OritechSpaceAge.LOGGER.debug("Rocket {} reached orbit {} on tick {}", rocket.getRocketId(), flight.targetOrbit(), gameTime);
                    unloadOrbitRocketFromClients(level, rocket);
                }
                if (gameTime == flight.reentryTick()) {
                    OritechSpaceAge.LOGGER.debug("Rocket {} began reentry toward {} on tick {}", rocket.getRocketId(), flight.impactPosition(), gameTime);
                    sendReentryDataToClients(level, rocket);
                }
            }
        }

        if (changed) {
            savedData.setDirty();
            OritechSpaceAge.LOGGER.debug("Active rocket data changed; {} rockets remain", ACTIVE_ROCKETS.size());
        }
    }

    // rocket data is shared between dimensions and stored in the overworld data storage
    private static ActiveRocketSavedData getSavedData(ServerLevel level) {
        return getSavedData(level.getServer());
    }

    private static ActiveRocketSavedData getSavedData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ActiveRocketSavedData.TYPE);
    }

    static RocketFlight calculateFlight(ActiveRocketData rocket, RocketFlightPlan flightPlan, RandomGenerator random) {
        return calculateFlight(rocket, flightPlan, random, Level.OVERWORLD);
    }

    private static RocketFlight calculateFlight(ActiveRocketData rocket, RocketFlightPlan flightPlan, RandomGenerator random, ResourceKey<Level> dimension) {
        var performance = calculatePerformance(rocket);
        var launchPosition = flightPlan.worldStart();
        var orbitPosition = launchPosition.offset(0, ORBIT_HEIGHT_BLOCKS, 0);
        var impactPosition = calculateImpactPosition(launchPosition, random);
        var targetOrbit = new Vector2i(flightPlan.targetOrbit());

        if (performance.engineCount() == 0) {
            return RocketFlight.failed(dimension, performance, launchPosition, orbitPosition, impactPosition, targetOrbit, "Rocket has no engines");
        }
        if (performance.wetMassKilograms() <= 0) {
            return RocketFlight.failed(dimension, performance, launchPosition, orbitPosition, impactPosition, targetOrbit, "Rocket has no measurable mass");
        }
        if (performance.liftoffAccelerationMetersPerSecondSquared() <= STANDARD_GRAVITY) {
            return RocketFlight.failed(dimension, performance, launchPosition, orbitPosition, impactPosition, targetOrbit, "Rocket does not have enough thrust to lift off");
        }

        // use constant thrust and gravity, but include gravity loss during ascent
        var netAcceleration = performance.liftoffAccelerationMetersPerSecondSquared() - STANDARD_GRAVITY;
        var ascentSeconds = Math.sqrt(2 * ORBIT_HEIGHT_BLOCKS / netAcceleration);
        var requiredDeltaV = performance.liftoffAccelerationMetersPerSecondSquared() * ascentSeconds;

        if (performance.availableBurnSeconds() < ascentSeconds || performance.availableDeltaVMetersPerSecond() < requiredDeltaV) {
            return RocketFlight.failed(dimension, performance, launchPosition, orbitPosition, impactPosition, targetOrbit, "Rocket does not have enough fuel to reach orbit");
        }

        var ascentTicks = secondsToTicks(ascentSeconds);
        var reentryTick = ascentTicks + ORBIT_WAIT_TICKS;
        var descentSeconds = Math.sqrt(2 * ORBIT_HEIGHT_BLOCKS / STANDARD_GRAVITY);
        var impactSpeed = STANDARD_GRAVITY * descentSeconds;
        var impactTick = reentryTick + secondsToTicks(descentSeconds);

        return new RocketFlight(true, null, dimension, performance, launchPosition, orbitPosition, impactPosition, targetOrbit, requiredDeltaV, impactSpeed, ascentTicks, reentryTick, impactTick, null, -1);
    }

    // calculates the ideal performance of all currently connected segments
    public static RocketPerformance calculatePerformance(ActiveRocketData rocket) {
        long dryWeight = 0;
        long fuelWeight = 0;
        long fuelBurnTicks = 0;
        long availableRF = 0;
        int engineCount = 0;

        for (var entry : rocket.getStaticSegments().entrySet()) {
            var dynamicSegment = rocket.getDynamicSegments().get(entry.getKey());
            var staticSegment = entry.getValue();
            dryWeight += Math.max(0, staticSegment.staticWeight());
            fuelWeight += Math.max(0, dynamicSegment.currentFuelWeight);
            fuelBurnTicks += Math.max(0, dynamicSegment.availableFuelBurnTimeTicks);
            availableRF += Math.max(0, dynamicSegment.availableRF);
            engineCount += Math.max(0, staticSegment.engineCount());
        }

        var dryMass = dryWeight * KILOGRAMS_PER_WEIGHT_UNIT;
        var fuelMass = fuelWeight * KILOGRAMS_PER_WEIGHT_UNIT;
        var wetMass = dryMass + fuelMass;
        var thrust = engineCount * ENGINE_THRUST_NEWTONS;

        var fuelBurnSeconds = engineCount == 0 ? 0 : fuelBurnTicks / (double) engineCount / TICKS_PER_SECOND;
        var electricBurnSeconds = engineCount == 0 ? 0 : availableRF / (double) RF_PER_ENGINE_TICK / engineCount / TICKS_PER_SECOND;

        var chemicalDeltaV = dryMass > 0 && fuelMass > 0 && fuelBurnSeconds > 0 ? ENGINE_SPECIFIC_IMPULSE_SECONDS * STANDARD_GRAVITY * Math.log(wetMass / dryMass) : 0;
        // RF has no fuel mass, so it uses a constant-mass electric burn
        var electricDeltaV = dryMass > 0 ? thrust / dryMass * electricBurnSeconds : 0;
        var liftoffAcceleration = wetMass > 0 ? thrust / wetMass : 0;

        return new RocketPerformance(dryMass, fuelMass, wetMass, engineCount, thrust, fuelBurnSeconds + electricBurnSeconds, chemicalDeltaV + electricDeltaV, liftoffAcceleration);
    }

    static BlockPos calculateImpactPosition(BlockPos launchPosition, RandomGenerator random) {
        var angle = random.nextDouble() * Math.PI * 2;
        // square-root sampling distributes impacts evenly over the ring
        var radiusSquared = MINIMUM_IMPACT_RADIUS * MINIMUM_IMPACT_RADIUS + random.nextDouble() * (MAXIMUM_IMPACT_RADIUS * MAXIMUM_IMPACT_RADIUS - MINIMUM_IMPACT_RADIUS * MINIMUM_IMPACT_RADIUS);
        var radius = Math.sqrt(radiusSquared);

        return new BlockPos(launchPosition.getX() + (int) Math.round(Math.cos(angle) * radius), launchPosition.getY(), launchPosition.getZ() + (int) Math.round(Math.sin(angle) * radius));
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
        return orbitPosition.lerp(impactPosition, progress * progress);
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

    // resolves the ground height and updates the descent timing
    private static RocketFlight handleImpactCollisions(ServerLevel level, RocketFlight flight) {
        var impactPosition = flight.impactPosition();
        var surfaceY = Math.max(level.getMinY(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, impactPosition.getX(), impactPosition.getZ()) - 1);
        var surfacePosition = new BlockPos(impactPosition.getX(), surfaceY, impactPosition.getZ());
        var descentHeight = Math.max(0, flight.orbitPosition().getY() - surfacePosition.getY());
        var descentSeconds = Math.sqrt(2 * descentHeight / STANDARD_GRAVITY);
        var impactSpeed = STANDARD_GRAVITY * descentSeconds;
        var impactTick = flight.reentryTick() + secondsToTicks(descentSeconds);
        OritechSpaceAge.LOGGER.debug("Resolved rocket impact surface at {}: fallDistance={}, fallTime={}s, speed={}m/s", surfacePosition, descentHeight, descentSeconds, impactSpeed);
        return flight.withImpact(surfacePosition, impactSpeed, impactTick);
    }

    private static void explodeOnTakeoffCollision(ServerLevel level, BlockPos position) {
        level.explode(null, position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5, TAKEOFF_EXPLOSION_STRENGTH, false, Level.ExplosionInteraction.BLOCK);
    }

    private static int calculateImpactExplosionStrength(RocketFlight flight) {
        var mass = flight.performance().wetMassKilograms();
        var speed = flight.impactSpeedMetersPerSecond();
        var kineticEnergy = 0.5 * mass * speed * speed;
        // blast radius scales with the cube root of the impact energy; 100 MJ results in size 9
        return Mth.clamp((int) Math.round(9 * Math.cbrt(kineticEnergy / REFERENCE_IMPACT_ENERGY_JOULES)), MINIMUM_NUCLEAR_STRENGTH, MAXIMUM_NUCLEAR_STRENGTH);
    }

    private static void explodeOnImpact(ServerLevel level, RocketFlight flight, int strength) {
        var position = flight.impactPosition();
        var explosionState = BlockContent.REACTOR_EXPLOSION_SMALL.get().defaultBlockState();
        level.setBlockAndUpdate(position, explosionState);
        level.setBlockEntity(new NuclearExplosionEntity(position, explosionState, strength));
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

    private static void sendImpactDataToClients(ServerLevel level, ActiveRocketData rocket, int nuclearExplosionStrength) {
        var position = rocket.getFlight().impactPosition();
        var recipients = sendToPlayersNearRocket(level, position, player -> RocketNetworking.sendCollision(player, rocket.getRocketId(), position, rocket.getFlight().impactSpeedMetersPerSecond(), nuclearExplosionStrength, true));
        logClientSelection("impact", rocket, position, recipients);
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
                        .xmap(ActiveRocketSavedData::new, ignored -> List.copyOf(ACTIVE_ROCKETS.values()));

        private static final SavedDataType<ActiveRocketSavedData> TYPE = new SavedDataType<>(OritechSpaceAge.id("active_rockets"), ActiveRocketSavedData::new, CODEC, null);

        private ActiveRocketSavedData() {
            ACTIVE_ROCKETS.clear();
            OritechSpaceAge.LOGGER.debug("Initialized empty active rocket saved data");
        }

        private ActiveRocketSavedData(List<ActiveRocketData> rockets) {
            ACTIVE_ROCKETS.clear();
            rockets.forEach(rocket -> ACTIVE_ROCKETS.put(rocket.getRocketId(), rocket));
            OritechSpaceAge.LOGGER.debug("Loaded {} active rockets from saved data", rockets.size());
        }
    }
}
