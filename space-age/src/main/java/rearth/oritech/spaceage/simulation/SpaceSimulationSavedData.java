package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saves each player's system and assembler plans together so asteroid targets keep their IDs.
 */
public class SpaceSimulationSavedData extends SavedData {

    public static final Codec<SpaceSimulationSavedData> CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, SpaceSimulation.CODEC)
                    .xmap(SpaceSimulationSavedData::new, data -> data.simulations);
    private static final SavedDataType<SpaceSimulationSavedData> TYPE = new SavedDataType<>(
            OritechSpaceAge.id("space_simulations"), SpaceSimulationSavedData::new, CODEC, null);

    private final Map<UUID, SpaceSimulation> simulations = new HashMap<>();
    private List<SpaceSimulation.SpaceObjectData> resourceBodies = List.of();

    public SpaceSimulationSavedData() {
    }

    private SpaceSimulationSavedData(Map<UUID, SpaceSimulation> simulations) {

        this.simulations.putAll(simulations);
    }

    public static SpaceSimulationSavedData get(MinecraftServer server) {

        var data = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        data.resourceBodies = ResourceBodies.objects(server.registryAccess());
        return data;
    }

    private static SpaceSimulationSavedData get(ServerPlayer player) {

        return get(player.level().getServer());
    }

    public static SpaceSimulation getForPlayer(ServerPlayer player) {

        return get(player).getOrCreate(player.getUUID());
    }

    public static void updateFlightPlan(ServerPlayer player, GlobalPos assembler,
                                        SpaceSimulation.FlightPlan plan, ActiveRocketData rocket) {

        get(player).updateFlightPlan(player.getUUID(), assembler, plan, rocket);
    }

    public SpaceSimulation getOrCreate(UUID player) {

        var simulation = simulations.get(player);
        if (simulation == null) {
            simulation = new SpaceSimulation();
            simulations.put(player, simulation);
            setDirty();
        }
        simulation.setResourceBodies(resourceBodies);
        return simulation;
    }

    void updateFlightPlan(UUID player, GlobalPos assembler, SpaceSimulation.FlightPlan plan, ActiveRocketData rocket) {

        if (getOrCreate(player).updateFlightPlan(assembler, plan, rocket)) setDirty();
    }
}
