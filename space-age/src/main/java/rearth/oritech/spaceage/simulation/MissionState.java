package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.spaceage.recipe.VacuumRecipe;

import java.util.*;

/**
 * A craft owns its mission and data independently of the assembler or any loaded station.
 */
public final class MissionState {

    UUID powerAction;
    double powerReadyAt;
    public SpacePowerService.Report powerReport = SpacePowerService.Report.EMPTY;

    public static final String STATUS_READY = "status.oritech_space_age.ready";
    public static final String STATUS_LAUNCHED = "status.oritech_space_age.launched";
    public static final String STATUS_COMPLETE = "status.oritech_space_age.mission_complete";
    public static final String STATUS_SCAN_WAITING = "status.oritech_space_age.scan_waiting";
    public static final String STATUS_SCANNING = "status.oritech_space_age.scanning";
    public static final String STATUS_INFORMATION_DELIVERED = "status.oritech_space_age.information_delivered";
    public static final String STATUS_TRANSMISSION_TIMED_OUT = "status.oritech_space_age.transmission_timed_out";
    public static final String STATUS_WAITING_FOR_LINK = "status.oritech_space_age.waiting_for_link";
    public static final String STATUS_ASTEROID_ATTACHED = "status.oritech_space_age.asteroid_already_attached";
    public static final String STATUS_PRECISE_REQUIRED = "status.oritech_space_age.precise_position_required";
    public static final String STATUS_SLOW_APPROACH = "status.oritech_space_age.slow_approach_required";
    public static final String STATUS_STATION_KEEPING_EXHAUSTED = "status.oritech_space_age.station_keeping_exhausted";
    public static final String STATUS_MAINTAINING_POSITION = "status.oritech_space_age.maintaining_position";
    public static final String STATUS_DISCARDED = "status.oritech_space_age.discarded";
    public static final String STATUS_TRANSFER = "status.oritech_space_age.transfer";
    public static final String STATUS_NO_FEASIBLE_TRANSFER = "status.oritech_space_age.no_feasible_transfer";
    public static final String STATUS_REPLANNING = "status.oritech_space_age.replanning";
    public static final String STATUS_IN_FLIGHT = "status.oritech_space_age.in_flight";
    public static final String STATUS_TRANSFER_BLOCKED = "status.oritech_space_age.transfer_blocked";
    public static final String STATUS_DESCENDING = "status.oritech_space_age.descending";
    public static final String STATUS_LANDING_UNLOADED = "status.oritech_space_age.landing_unloaded";
    public static final String STATUS_LANDING_UNAVAILABLE = "status.oritech_space_age.landing_unavailable";
    public static final String STATUS_DESTROYED = "status.oritech_space_age.destroyed";
    public static final String STATUS_RECOVERED = "status.oritech_space_age.recovered";
    public static final String STATUS_UPDATE_ACCEPTED = "status.oritech_space_age.update_accepted";
    public static final Codec<MissionState> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(m -> m.owner),
            ActiveRocketData.CODEC.fieldOf("rocket").forGetter(m -> m.rocket),
            SpaceSimulation.FlightPlan.CODEC.fieldOf("plan").forGetter(m -> m.plan),
            Position.CODEC.fieldOf("position").forGetter(m -> m.position),
            SurveyKnowledge.CODEC.fieldOf("knowledge").forGetter(m -> m.knowledge),
            Codec.LONG.fieldOf("action_ticks").forGetter(m -> m.serviceTicks),
            Codec.STRING.fieldOf("status").forGetter(m -> m.status),
            ActiveTransfer.CODEC.optionalFieldOf("transfer").forGetter(m -> Optional.ofNullable(m.transfer)),
            BlockPos.CODEC.fieldOf("launch").forGetter(m -> m.launch),
            SpaceSimulation.FlightPlanAction.CODEC.listOf().fieldOf("completed").forGetter(m -> m.completed),
            Codec.LONG.fieldOf("position_revision").forGetter(m -> m.positionRevision),
            Codec.INT.fieldOf("service_progress").forGetter(m -> m.serviceProgress)
    ).apply(i, MissionState::new));
    public final UUID owner;
    public final SurveyKnowledge knowledge;
    public final BlockPos launch;
    public final List<SpaceSimulation.FlightPlanAction> completed = new ArrayList<>();
    public ActiveRocketData rocket;
    public SpaceSimulation.FlightPlan plan;
    public Position position;
    private long serviceTicks;
    ActiveTransfer transfer;
    public long positionRevision;
    public int serviceProgress;
    public String status;
    public String route = "communication.oritech_space_age.no_route";
    public boolean connected;
    public SpaceCommunications.Connection communication = new SpaceCommunications.Connection(false, 0, 0,
            "communication.oritech_space_age.no_ground");
    boolean ended;
    private MissionState(UUID owner, ActiveRocketData rocket, SpaceSimulation.FlightPlan plan, Position position,
                         SurveyKnowledge knowledge, long ticks, String status,
                         Optional<ActiveTransfer> transfer, BlockPos launch, List<SpaceSimulation.FlightPlanAction> completed,
                         long positionRevision, int serviceProgress) {

        this.positionRevision = positionRevision;
        this.serviceProgress = serviceProgress;
        this.owner = owner;
        this.rocket = rocket;
        this.plan = plan;
        this.position = position;
        this.knowledge = knowledge;
        this.serviceTicks = ticks;
        this.status = status;
        this.completed.addAll(completed);
        this.transfer = transfer.orElse(null);
        this.launch = launch;
        rocket.setLaunchPosition(launch);
        this.ended = isRecovered(status) || status.equals(STATUS_DESTROYED) || status.equals(STATUS_DISCARDED);
    }

    public MissionState(UUID owner, ActiveRocketData rocket, SpaceSimulation.FlightPlan plan,
                        Position position, SurveyKnowledge knowledge, BlockPos launch, long tick) {

        this(owner, rocket, plan, position, knowledge, 0, STATUS_READY,
                Optional.empty(), launch, List.of(), 0, 0);
    }

    public static boolean isRecovered(String status) {

        return status.equals(STATUS_RECOVERED);
    }

    public static Component statusComponent(String status) {

        return status.startsWith("status.oritech_space_age.") ? Component.translatable(status) : Component.literal(status);
    }

    public static ActiveRocketData copyRocket(ActiveRocketData rocket) {

        var dynamic = new HashMap<UUID, DynamicRocketSegment>();
        rocket.getDynamicSegments().forEach((id, r) -> dynamic.put(id, r.copy()));
        var copy = new ActiveRocketData(rocket.getRocketId(), rocket.getOriginalSegments(), dynamic, rocket.getFlight());
        copy.copyLaunchFrom(rocket);
        return copy;
    }

    public MissionForecast.Navigation navigationForecast() {

        return navigationForecast(List.of());
    }

    public MissionForecast.Navigation navigationForecast(Collection<RecipeHolder<VacuumRecipe>> recipes) {

        return transfer == null || ended ? MissionForecast.Navigation.EMPTY : transfer.forecast(plan, recipes);
    }

    public long actionTicks() {

        return transfer == null ? serviceTicks : transfer.ticks;
    }

    long advanceAction() {

        return transfer == null ? ++serviceTicks : ++transfer.ticks;
    }

    void resetAction() {

        serviceTicks = 0;
        serviceProgress = 0;
        transfer = null;
    }

    public SpaceSimulation.FlightPlanAction action() {

        return plan.root().actions().stream().filter(a -> !a.isGenerated()).findFirst().orElse(null);
    }

    public boolean canUpdateMission() {

        return connected && !ended;
    }

    public boolean canDismiss() {

        return ended || action() == null || navigationBlocked();
    }

    public boolean navigationBlocked() {

        return status.equals(STATUS_NO_FEASIBLE_TRANSFER) || status.equals(STATUS_TRANSFER_BLOCKED);
    }

    public void complete() {

        var root = plan.root();
        var actions = new ArrayList<>(root.actions());
        if (!actions.isEmpty()) completed.add(actions.removeFirst());
        while (!actions.isEmpty() && actions.getFirst().isGenerated()) actions.removeFirst();
        plan = plan.withBranches(plan.branches().stream().map(b -> b.isRoot() ? b.withActions(actions) : b).toList());
        resetAction();
    }

    public Telemetry telemetry(long tick) {

        return new Telemetry(copyRocket(rocket), plan, position, tick, status, List.copyOf(completed), actionTicks(),
                actionDurationTicks(), serviceProgress, powerReport, true);
    }

    public Telemetry summary(long tick) {

        var structures = new HashMap<UUID, StaticRocketSegment>();
        var resources = new HashMap<UUID, DynamicRocketSegment>();
        rocket.getOriginalSegments().forEach((id, segment) -> {
            structures.put(id, new StaticRocketSegment(id, Set.of(), Map.of(), segment.staticWeight(), segment.engineCount(),
                    segment.initialRF(), segment.initialFuel(), segment.payloadEnergyJoules(), segment.planAnchor()));
            var original = rocket.getDynamicSegments().get(id);
            var summary = new DynamicRocketSegment(0, 0, original.currentFuelWeight, Set.of(), Map.of(), original.fuelCapacity, original.rfCapacity);
            summary.setResources(original.fuel(), original.rf());
            resources.put(id, summary);
        });
        var summaryRocket = new ActiveRocketData(rocket.getRocketId(), structures, resources, null);
        summaryRocket.copyLaunchFrom(rocket);
        return new Telemetry(summaryRocket, plan, position, tick, status,
                List.copyOf(completed), actionTicks(), actionDurationTicks(), serviceProgress, powerReport, false);
    }

    private long actionDurationTicks() {

        var action = action();
        if (action == null) return 0;
        if (action.type() == SpaceSimulation.ActionType.MAINTAIN_POSITION || action.type() == SpaceSimulation.ActionType.BEAM_POWER)
            return action.addons().stream().filter(addon -> addon.type() == SpaceSimulation.ActionAddonType.DURATION)
                    .mapToLong(addon -> addon.value() * 20L).findFirst().orElse(0);
        if (action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION) return action.service().timeoutTicks();
        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO || transfer == null || transfer.path == null) return 0;
        return transfer.path.paths().stream().filter(item -> item.branchId().equals(plan.root().id())).findFirst()
                .map(item -> Math.max(1L, Math.round(item.durationSeconds() * 20))).orElse(0L);
    }

    public record Position(double x, double y, double vx, double vy, UUID target, SpaceSimulation.OrbitBand orbit,
                           int slot, int stage, UUID asteroid, SpaceSimulation.SegmentRef anchor, double headingX,
                           double headingY) {

        public static final Codec<Position> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Position::x), Codec.DOUBLE.fieldOf("y").forGetter(Position::y),
                Codec.DOUBLE.fieldOf("vx").forGetter(Position::vx), Codec.DOUBLE.fieldOf("vy").forGetter(Position::vy),
                UUIDUtil.STRING_CODEC.fieldOf("target").forGetter(Position::target),
                SpaceSimulation.OrbitBand.CODEC.fieldOf("orbit").forGetter(Position::orbit),
                Codec.INT.fieldOf("slot").forGetter(Position::slot), Codec.INT.fieldOf("stage").forGetter(Position::stage),
                UUIDUtil.STRING_CODEC.fieldOf("asteroid").forGetter(Position::asteroid),
                SpaceSimulation.SegmentRef.CODEC.fieldOf("anchor").forGetter(Position::anchor),
                Codec.DOUBLE.optionalFieldOf("headingX", 0.0).forGetter(Position::headingX),
                Codec.DOUBLE.optionalFieldOf("headingY", 0.0).forGetter(Position::headingY)
        ).apply(i, Position::new));

        public Position(double x, double y, double vx, double vy, UUID target, SpaceSimulation.OrbitBand orbit,
                        int slot, int stage, UUID asteroid, SpaceSimulation.SegmentRef anchor) {

            this(x, y, vx, vy, target, orbit, slot, stage, asteroid, anchor, vx, vy);
        }

        public static Position surface() {

            return new Position(-3_060_000, 0, 0, 0, SpaceObjects.EARTH_ID, SpaceSimulation.OrbitBand.SURFACE,
                    -1, 1, SpaceSimulation.FlightPlanAction.NO_TARGET, new SpaceSimulation.SegmentRef(BlockPos.ZERO));
        }
    }

    public record Telemetry(ActiveRocketData rocket, SpaceSimulation.FlightPlan plan, Position position,
                            long tick, String status, List<SpaceSimulation.FlightPlanAction> completed,
                            long actionTicks,
                            long actionDurationTicks, int serviceProgress, SpacePowerService.Report power, boolean fullLayout) {

        public static final Codec<Telemetry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ActiveRocketData.CODEC.fieldOf("rocket").forGetter(Telemetry::rocket),
                SpaceSimulation.FlightPlan.CODEC.fieldOf("plan").forGetter(Telemetry::plan),
                Position.CODEC.fieldOf("position").forGetter(Telemetry::position),
                Codec.LONG.fieldOf("tick").forGetter(Telemetry::tick), Codec.STRING.fieldOf("status").forGetter(Telemetry::status),
                SpaceSimulation.FlightPlanAction.CODEC.listOf().fieldOf("completed").forGetter(Telemetry::completed),
                Codec.LONG.optionalFieldOf("action_ticks", 0L).forGetter(Telemetry::actionTicks),
                Codec.LONG.optionalFieldOf("action_duration_ticks", 0L).forGetter(Telemetry::actionDurationTicks),
                Codec.INT.fieldOf("service_progress").forGetter(Telemetry::serviceProgress),
                SpacePowerService.Report.CODEC.optionalFieldOf("power", SpacePowerService.Report.EMPTY).forGetter(Telemetry::power),
                Codec.BOOL.optionalFieldOf("full_layout", true).forGetter(Telemetry::fullLayout)
        ).apply(i, Telemetry::new));

        public Telemetry(ActiveRocketData rocket, SpaceSimulation.FlightPlan plan, Position position, long tick, String status,
                         List<SpaceSimulation.FlightPlanAction> completed, long actionTicks, long actionDurationTicks, int serviceProgress) {

            this(rocket, plan, position, tick, status, completed, actionTicks, actionDurationTicks, serviceProgress, SpacePowerService.Report.EMPTY, true);
        }

        public Telemetry(ActiveRocketData rocket, SpaceSimulation.FlightPlan plan, Position position, long tick, String status,
                         List<SpaceSimulation.FlightPlanAction> completed, long actionTicks, long actionDurationTicks) {

            this(rocket, plan, position, tick, status, completed, actionTicks, actionDurationTicks, 0);
        }

        public Telemetry(ActiveRocketData rocket, SpaceSimulation.FlightPlan plan, Position position, long tick, String status,
                         List<SpaceSimulation.FlightPlanAction> completed) {

            this(rocket, plan, position, tick, status, completed, 0, 0);
        }

        public Telemetry(ActiveRocketData rocket, SpaceSimulation.FlightPlan plan, Position position, long tick, String status,
                         List<SpaceSimulation.FlightPlanAction> completed, long actionTicks) {

            this(rocket, plan, position, tick, status, completed, actionTicks, 0);
        }

        public Telemetry(ActiveRocketData rocket, SpaceSimulation.FlightPlan plan, Position position, long tick, String status) {

            this(rocket, plan, position, tick, status, List.of());
        }
    }
}
