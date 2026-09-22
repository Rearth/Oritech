package rearth.oritech.spaceage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.init.*;
import rearth.oritech.spaceage.simulation.MissionState;
import java.util.*;

public class MissionControlMenu extends RocketAssemblerMenu {
    public UUID selected;
    public MissionState.Position knownPosition;
    public java.util.List<rearth.oritech.spaceage.network.MissionNetworking.FleetEntry> fleet = List.of();
    public int fleetRevision;
    public int telemetryRevision;
    public boolean connected;
    public rearth.oritech.spaceage.simulation.SpaceCommunications.NetworkStatus network =
            new rearth.oritech.spaceage.simulation.SpaceCommunications.NetworkStatus(0, 0, 0, 0, 0, 1);
    public List<rearth.oritech.spaceage.simulation.SpaceCommunications.Node> networkNodes = List.of();
    public MissionState.Telemetry selectedTelemetry;
    public long simulationTick;
    public long receivedWorldTick;
    public int debugSpeed = 1;
    public List<rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction> completed = List.of();
    public UUID currentAction;
    public String missionStatus = "";
    public rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan acceptedPlan;
    public long currentMissionTick(long worldTick) { return simulationTick + (worldTick - receivedWorldTick) * debugSpeed; }
    public rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlannerSnapshot fleetSnapshot;
    public MissionControlMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos()); }
    public MissionControlMenu(int id, Inventory inventory, BlockPos pos) { super(SpaceAgeMenus.MISSION_CONTROL.get(), id, pos); }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(blockPos).is(SpaceAgeBlocks.MISSION_CONTROL)
                && player.distanceToSqr(blockPos.getX() + .5, blockPos.getY() + .5, blockPos.getZ() + .5) <= 64;
    }
}
