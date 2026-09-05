package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.network.RocketNetworking;

public final class RocketAssemblerClientController {

    private RocketAssemblerClientController() {
    }

    public static void receivePreview(BlockPos position, @Nullable ActiveRocketData rocket) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof RocketAssemblerMenu menu
                && menu.blockPos.equals(position)) {
            menu.setPreview(rocket);
        }
    }

    public static void receiveFlightPlanner(BlockPos position, SpaceSimulation.FlightPlannerSnapshot snapshot) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof RocketAssemblerMenu menu
                && menu.blockPos.equals(position)) {
            menu.setFlightPlannerSnapshot(snapshot);
        }
    }

    /** Both assembler tabs edit the same menu-owned draft, so tab switches never discard client changes. */
    public static void submitFlightPlanIfDirty(RocketAssemblerMenu menu) {
        var rocket = menu.getRocket();
        var plan = menu.getDraftFlightPlan();
        if (!menu.isDraftFlightPlanDirty() || rocket == null || plan == null) return;
        ClientPacketDistributor.sendToServer(new RocketNetworking.SubmitFlightPlanPayload(
                menu.blockPos, rocket.getRocketId(), plan));
        menu.markDraftFlightPlanSaved();
    }
}
