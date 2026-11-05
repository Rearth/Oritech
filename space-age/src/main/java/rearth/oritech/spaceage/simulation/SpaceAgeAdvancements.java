package rearth.oritech.spaceage.simulation;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.trophy.TrophyInscription;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionState;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class SpaceAgeAdvancements {

    public static final Identifier FULL_ORBIT = OritechSpaceAge.id("full_orbit");
    public static final String CRITERION = "filled_orbit";

    private SpaceAgeAdvancements() {
    }

    public static void tick(MinecraftServer server) {

        var advancement = server.getAdvancements().get(FULL_ORBIT);
        if (advancement == null) return;
        var missions = MissionSavedData.get(server).craft.values();
        for (var player : server.getPlayerList().getPlayers()) {
            if (player.getAdvancements().getOrStartProgress(advancement).isDone()) continue;
            var orbit = completedOrbit(player.getUUID(), missions);
            if (orbit != null && player.getAdvancements().award(advancement, CRITERION)) giveTrophy(player, orbit);
        }
    }

    public static @Nullable OrbitBand completedOrbit(UUID owner, Collection<MissionState> missions) {

        for (var orbit : List.of(OrbitBand.LOW, OrbitBand.HIGH, OrbitBand.SOLAR)) {
            var body = orbit == OrbitBand.SOLAR ? SpaceSimulation.SUN_ID : SpaceObjects.EARTH_ID;
            var slots = SpaceBalance.slots(body, orbit);
            var occupied = missions.stream().filter(craft -> craft.owner.equals(owner) && !craft.ended && craft.transfer == null
                    && craft.position.target().equals(body) && craft.position.orbit() == orbit
                    && craft.position.slot() >= 0 && craft.position.slot() < slots)
                    .map(craft -> craft.position.slot()).distinct().count();
            if (occupied == slots) return orbit;
        }
        return null;
    }

    private static void giveTrophy(ServerPlayer player, OrbitBand orbit) {

        var stack = new ItemStack(SpaceAgeBlocks.SPACE_TROPHY.get());
        stack.set(SpaceAgeComponents.TROPHY_INSCRIPTION.get(), new TrophyInscription(player.getGameProfile().name(), orbit));
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
