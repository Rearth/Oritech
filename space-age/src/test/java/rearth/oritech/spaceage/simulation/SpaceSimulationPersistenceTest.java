package rearth.oritech.spaceage.simulation;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.networking.ReflectiveCodecBuilder;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpaceSimulationPersistenceTest {

    @Test
    void savesSettingsTargetsAndBranches(@TempDir Path directory) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var data = new SpaceSimulationSavedData();
        var player = UUID.randomUUID();
        var assembler = GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 70, 30));
        var simulation = data.getOrCreate(player);
        assertTrue(data.isDirty());
        data.setDirty(false);
        var rocketId = UUID.randomUUID();
        var initial = simulation.createFlightPlannerSnapshot(assembler, rocketId);
        var target = initial.objects().stream().filter(object -> object.type() == SpaceObjects.ObjectType.ASTEROID)
                .findFirst().orElseThrow();

        var coreId = UUID.randomUUID();
        var boosterId = UUID.randomUUID();
        var core = new StaticRocketSegment(coreId,
                Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, Blocks.STONE.defaultBlockState())),
                Map.of(boosterId, Set.of()), 25, 1);
        var booster = new StaticRocketSegment(boosterId,
                Set.of(new StaticRocketSegment.BlockData(new BlockPos(2, 0, 0), Blocks.STONE.defaultBlockState())),
                Map.of(coreId, Set.of()), 25, 1);
        var rocket = new ActiveRocketData(Map.of(coreId, core, boosterId, booster), Map.of(
                coreId, new DynamicRocketSegment(0, 2_000_000, 0, Set.of(boosterId)),
                boosterId, new DynamicRocketSegment(0, 2_000_000, 0, Set.of(coreId))));
        var coreRef = SpaceSimulation.SegmentRef.of(core);
        var boosterRef = SpaceSimulation.SegmentRef.of(booster);
        var navigate = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(target.id()).withOrbit(SpaceSimulation.OrbitBand.TIGHT)
                .withVelocity(SpaceSimulation.ArrivalVelocityMode.CUSTOM, 50).withMaxSpeed(500);
        var separate = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.DECOUPLE)
                .withSegments(List.of(coreRef, boosterRef));
        var child = new SpaceSimulation.FlightPlanBranch(UUID.randomUUID(), separate.id(),
                List.of(SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.DISCARD_CRAFT)));
        var plan = new SpaceSimulation.FlightPlan(List.of(initial.plan().root().withActions(List.of(navigate, separate)), child),
                List.of(new SpaceSimulation.SegmentConfiguration(coreRef, "Explorer", false, List.of(2)),
                        new SpaceSimulation.SegmentConfiguration(boosterRef, "Booster", true, List.of(1))));
        data.updateFlightPlan(player, assembler, plan, rocket);
        assertTrue(data.isDirty());
        data.setDirty(false);
        data.updateFlightPlan(player, assembler, plan, rocket);
        assertFalse(data.isDirty(), "unchanged settings need no save");

        var otherAssembler = GlobalPos.of(Level.NETHER, assembler.pos());
        data.updateFlightPlan(player, otherAssembler, SpaceSimulation.FlightPlan.empty(), rocket);
        var before = simulation.createFlightPlannerSnapshot(assembler, rocketId);
        assertEquals(plan, before.plan());
        var file = directory.resolve("space_simulations.dat");
        var encoded = SpaceSimulationSavedData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        NbtIo.writeCompressed((CompoundTag) encoded, file);
        var loaded = SpaceSimulationSavedData.CODEC.parse(NbtOps.INSTANCE,
                NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap())).getOrThrow();
        assertEquals(before, loaded.getOrCreate(player).createFlightPlannerSnapshot(assembler, rocketId));
        assertTrue(loaded.getOrCreate(player).createFlightPlannerSnapshot(otherAssembler, rocketId)
                .plan().root().actions().isEmpty(), "dimensions have separate plans");
        assertFalse(loaded.isDirty(), "reading a saved system should not regenerate it");
        assertEquals(encoded, SpaceSimulationSavedData.CODEC.encodeStart(NbtOps.INSTANCE, loaded).getOrThrow());
        assertNotEquals(before.simulationId(), new SpaceSimulationSavedData().getOrCreate(player)
                .createFlightPlannerSnapshot(assembler, rocketId).simulationId(), "worlds own their systems");

        // Use the same record codec as the planner packets, including the speed cap.
        NetworkManager.loadDefaultCodecs();
        var codec = ReflectiveCodecBuilder.create(SpaceSimulation.FlightPlan.class);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        try {
            codec.encode(buffer, plan);
            assertEquals(plan, codec.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
