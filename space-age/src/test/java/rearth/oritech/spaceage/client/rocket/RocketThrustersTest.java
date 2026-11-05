package rearth.oritech.spaceage.client.rocket;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2i;
import org.junit.jupiter.api.Test;
import net.neoforged.neoforge.network.connection.ConnectionType;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.networking.ReflectiveCodecBuilder;
import rearth.oritech.spaceage.block.basic.RocketEngineBlock.Type;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.RocketNetworking.SyncRocketPayload;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketAscent;
import rearth.oritech.spaceage.simulation.rocket.RocketFlight;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformance;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment.BlockData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketThrustersTest {

    @Test
    void worldSnapshotPreservesDisabledStagesThroughPacketRoundTrip() {

        var rocket = rocket();
        NetworkManager.loadDefaultCodecs();
        NetworkManager.registerCodec(ByteBufCodecs.fromCodecWithRegistries(ActiveRocketData.CODEC), ActiveRocketData.class);
        var codec = ReflectiveCodecBuilder.create(SyncRocketPayload.class);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        try {
            codec.encode(buffer, new SyncRocketPayload(rocket, List.of()));
            var decoded = codec.decode(buffer);
            assertEquals(rocket.getRocketId(), decoded.rocket().getRocketId());
            RocketEffects.receive(decoded.rocket(), decoded.engineSegments());
            assertTrue(RocketEffects.nozzles(decoded.rocket()).isEmpty());
            RocketEffects.receive(decoded.rocket(), List.copyOf(decoded.rocket().getStaticSegments().keySet()));
            assertEquals(2, RocketEffects.nozzles(decoded.rocket()).size());
        } finally {
            RocketEffects.clear();
            buffer.release();
        }
    }

    @Test
    void nozzlesFollowEngineBlocksAndIndependentFuelPools() {

        var rocket = rocket();
        var nozzles = RocketThrusters.nozzles(rocket);
        assertEquals(2, nozzles.size());
        var chemical = nozzles.stream().filter(n -> n.type() == Type.BASIC).findFirst().orElseThrow();
        var ion = nozzles.stream().filter(n -> n.type() == Type.ION).findFirst().orElseThrow();
        assertEquals(new Vec3(2, 2.5, 4), chemical.origin());
        assertEquals(new Vec3(0, -1, 0), chemical.direction());
        assertTrue(chemical.burnSeconds() > 0);
        assertTrue(ion.burnSeconds() > 0);
        var resources = rocket.getDynamicSegments().values().iterator().next();
        resources.setResources(0, 10_000);
        assertEquals(0, RocketThrusters.nozzles(rocket).stream().filter(n -> n.type() == Type.BASIC).findFirst().orElseThrow().burnSeconds());
        assertTrue(RocketThrusters.nozzles(rocket).stream().filter(n -> n.type() == Type.ION).findFirst().orElseThrow().burnSeconds() > 0);
        resources.layout.put(chemical.block(), Blocks.AIR.defaultBlockState());
        assertEquals(1, RocketThrusters.nozzles(rocket).size());
    }

    @Test
    void physicalEffectsStopBeforeLaunchAtOrbitAtImpactAndOnFuelExhaustion() {

        var flight = flight(0, -1, -1);
        var nozzle = RocketThrusters.nozzles(rocket()).getFirst();
        assertFalse(RocketThrusters.firing(nozzle, flight, flight.launchTick() - 1));
        assertTrue(RocketThrusters.firing(nozzle, flight, flight.launchTick()));
        var shortBurn = new RocketThrusters.Nozzle(nozzle.segment(), nozzle.block(), nozzle.origin(), nozzle.direction(), 1, nozzle.type(), .5);
        assertFalse(RocketThrusters.firing(shortBurn, flight, flight.launchTick() + 11));
        assertFalse(RocketThrusters.visible(flight, flight.orbitArrivalTick()));
        var descent = flight(3, 300, 500);
        assertTrue(RocketThrusters.firing(nozzle, descent, 400));
        assertFalse(RocketThrusters.visible(descent, 500));
        var crash = flight(80, 300, 500);
        assertTrue(RocketThrusters.crashing(crash, 400));
        assertFalse(RocketThrusters.firing(nozzle, crash, 400));
    }

    @Test
    void previewUsesTheEndingIntervalsFiringSegmentsAndStopsAtMissionEnd() {

        var rocket = rocket();
        var ref = SegmentRef.of(rocket.getStaticSegments().values().iterator().next());
        var plan = FlightPlan.empty();
        var telemetry = new MissionState.Telemetry(rocket, plan, MissionState.Position.surface(), 0, MissionState.STATUS_TRANSFER);
        var path = new CraftPath(plan.root().id(), Set.of(ref), List.of(
                sample(0, PathPhase.COAST, Set.of()), sample(2, PathPhase.ACCELERATE, Set.of(ref)),
                sample(4, PathPhase.COAST, Set.of()), sample(6, PathPhase.BRAKE, Set.of(ref))),
                List.of(), 6, 0, TerminalState.READY);
        assertEquals(Set.of(ref), RocketThrusters.firingSegments(telemetry, List.of(path), 20));
        assertEquals(Set.of(), RocketThrusters.firingSegments(telemetry, List.of(path), 60));
        assertEquals(Set.of(ref), RocketThrusters.firingSegments(telemetry, List.of(path), 100));
        assertEquals(Set.of(), RocketThrusters.firingSegments(telemetry, List.of(path), 120));
        var ended = new MissionState.Telemetry(rocket, plan, MissionState.Position.surface(), 0, MissionState.STATUS_RECOVERED);
        assertEquals(Set.of(), RocketThrusters.firingSegments(ended, List.of(path), 20));
    }

    @Test
    void shakeFallsOffSmoothlyAndEndsOutsideSixtyFourBlocks() {

        assertEquals(1, RocketEffects.attenuation(0));
        assertEquals(.25f, RocketEffects.attenuation(32));
        assertEquals(0, RocketEffects.attenuation(64));
        assertEquals(0, RocketEffects.attenuation(200));
    }

    private static ActiveRocketData rocket() {

        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(
                new BlockData(new BlockPos(2, 3, 4), SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState()),
                new BlockData(new BlockPos(-2, 3, 4), SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState())), Map.of(), 20, 2, 10_000, 10_000);
        return new ActiveRocketData(Map.of(id, segment), Map.of(id, new DynamicRocketSegment(10_000, 10_000, 1, Set.of())));
    }

    private static RocketFlight flight(double speed, long reentry, long impact) {

        return new RocketFlight(true, null, Level.OVERWORLD, new RocketPerformance(100, 10, 110, 1, 1000, 20, 200, 20),
                BlockPos.ZERO, new BlockPos(0, 100, 0), BlockPos.ZERO, new Vector2i(), 0, speed,
                200, reentry, impact, null, -1, new RocketAscent(0, 20));
    }

    private static PathSample sample(double time, PathPhase phase, Set<SegmentRef> firing) {

        return new PathSample(time, 0, 0, 1, 1, 0, phase, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET,
                1, firing, firing, FlightPlanAction.NO_TARGET);
    }
}
