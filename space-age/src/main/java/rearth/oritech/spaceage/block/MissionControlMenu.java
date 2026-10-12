package rearth.oritech.spaceage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.spaceage.network.MissionNetworking;
import rearth.oritech.spaceage.simulation.MissionState;
import rearth.oritech.spaceage.simulation.SpaceCommunications;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.List;
import java.util.UUID;

public class MissionControlMenu extends RocketAssemblerMenu {

    public UUID selected;
    public MissionState.Position knownPosition;
    public List<MissionNetworking.FleetEntry> fleet = List.of();
    public int fleetRevision;
    public int telemetryRevision;
    public boolean connected;
    public SpaceCommunications.NetworkStatus network =
            new SpaceCommunications.NetworkStatus(0, 0, 0, 0, 0, 1);
    public List<SpaceCommunications.Node> networkNodes = List.of();
    public MissionState.Telemetry selectedTelemetry;
    public long simulationTick;
    public long receivedWorldTick;
    public int debugSpeed = 1;
    public List<SpaceSimulation.FlightPlanAction> completed = List.of();
    public UUID currentAction;
    public String missionStatus = "";
    public SpaceSimulation.FlightPlan acceptedPlan;
    public SpaceSimulation.FlightPlannerSnapshot fleetSnapshot;

    public MissionControlMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {

        this(id, inventory, buffer.readBlockPos());
    }

    public MissionControlMenu(int id, Inventory inventory, BlockPos pos) {

        super(SpaceAgeMenus.MISSION_CONTROL.get(), id, pos);
    }

    public long currentMissionTick(long worldTick) {

        return simulationTick + (worldTick - receivedWorldTick) * debugSpeed;
    }

    @Override
    public boolean stillValid(Player player) {

        return player.level().getBlockState(blockPos).is(SpaceAgeBlocks.MISSION_CONTROL)
                && player.distanceToSqr(blockPos.getX() + .5, blockPos.getY() + .5, blockPos.getZ() + .5) <= 64;
    }
}
