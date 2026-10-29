package rearth.oritech.spaceage.block.mission_control;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.spaceage.network.MissionNetworking;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.RocketPayloads;
import rearth.oritech.spaceage.simulation.survey.SpaceCommunications;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class MissionControlMenu extends RocketAssemblerMenu {

    public UUID selected;
    public UUID detailTarget;
    public MissionState.Position knownPosition;
    public List<MissionNetworking.FleetEntry> fleet = List.of();
    public int fleetRevision;
    public int telemetryRevision;
    public boolean connected;
    public SpaceCommunications.NetworkStatus network =
            new SpaceCommunications.NetworkStatus(0, 0, 0, 0);
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

    public boolean updateMission(MissionState.Telemetry telemetry, boolean commandsAvailable) {

        // Compare against the previous accepted program before applying mission progress.
        var draft = getDraftFlightPlan();
        var edited = hasMissionChanges(draft);
        var current = telemetry.plan().currentAction();
        var nextAction = current == null ? null : current.id();
        var changed = selectedTelemetry == null || !Objects.equals(currentAction, nextAction)
                || !completed.equals(telemetry.completed()) || connected != commandsAvailable
                || !Objects.equals(acceptedPlan, telemetry.plan())
                || !selectedTelemetry.rocket().getStaticSegments().keySet().equals(telemetry.rocket().getStaticSegments().keySet());
        updatePreviewData(telemetry.rocket());
        selectedTelemetry = telemetry;
        completed = telemetry.completed();
        currentAction = nextAction;
        missionStatus = telemetry.status();
        acceptedPlan = telemetry.plan();
        knownPosition = telemetry.position();
        connected = commandsAvailable;
        if (edited) {
            setDraftFlightPlan(draft);
        } else {
            setDraftFlightPlan(acceptedPlan);
            markDraftFlightPlanSaved();
        }
        return changed;
    }

    @Override
    public void setDraftFlightPlan(FlightPlan plan) {

        // A popup can commit after a telemetry update completed one of its cards.
        var finished = completed.stream().map(FlightPlanAction::id).collect(Collectors.toSet());
        super.setDraftFlightPlan(RocketFlightPlanRules.normalize(plan.withBranches(plan.branches().stream()
                .map(branch -> branch.withActions(branch.actions().stream().filter(action -> !finished.contains(action.id())).toList()))
                .toList())));
    }

    public FlightPlan displayedPlan() {

        return !connected && acceptedPlan != null ? acceptedPlan : getDraftFlightPlan();
    }

    public boolean hasMissionChanges(FlightPlan draft) {

        return draft != null && acceptedPlan != null && !editableProgram(draft).equals(editableProgram(acceptedPlan));
    }

    private FlightPlan editableProgram(FlightPlan plan) {

        // Derived branches and detached segment settings are not user edits.
        var branches = RocketFlightPlanRules.normalize(plan).branches().stream()
                .map(branch -> branch.withActions(branch.actions().stream().filter(action -> !action.isGenerated()).toList()))
                .filter(branch -> branch.isRoot() || !branch.actions().isEmpty()).toList();
        var segments = RocketPayloads.planView(getRocket()).getStaticSegments().values().stream().map(SegmentRef::of).toList();
        return new FlightPlan(branches, plan.segmentConfigurations().stream()
                .filter(configuration -> segments.contains(configuration.segment())).toList(), plan.name(), plan.category());
    }

    @Override
    public boolean savesOnClose() {

        return false;
    }

    @Override
    public boolean canLaunch() {

        return false;
    }

    @Override
    public void saveFlightPlan(ServerPlayer player, SpaceSimulation.FlightPlan plan) {

        MissionNetworking.submit(player, this, plan);
    }

    @Override
    public boolean stillValid(Player player) {

        return player.level().getBlockState(blockPos).is(SpaceAgeBlocks.MISSION_CONTROL)
                && player.distanceToSqr(blockPos.getX() + .5, blockPos.getY() + .5, blockPos.getZ() + .5) <= 64;
    }
}
