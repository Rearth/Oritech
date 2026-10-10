package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.spaceage.recipe.VacuumRecipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A craft owns its mission and data independently of the assembler or any loaded station.
 */
public final class MissionState {

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
            Codec.LONG.fieldOf("action_ticks").forGetter(m -> m.actionTicks),
            Codec.STRING.fieldOf("status").forGetter(m -> m.status),
            Telemetry.CODEC.optionalFieldOf("leg").forGetter(m -> Optional.ofNullable(m.leg)),
            BlockPos.CODEC.fieldOf("launch").forGetter(m -> m.launch),
            SpaceSimulation.FlightPlanAction.CODEC.listOf().fieldOf("completed").forGetter(m -> m.completed),
            BlockPos.CODEC.optionalFieldOf("landing").forGetter(m -> Optional.ofNullable(m.landing)),
            SpaceSimulation.SpaceObjectData.CODEC.listOf().fieldOf("leg_objects").forGetter(m -> m.legObjects),
            Codec.LONG.fieldOf("position_revision").forGetter(m -> m.positionRevision),
            Codec.INT.fieldOf("service_progress").forGetter(m -> m.serviceProgress),
            DockingTarget.CODEC.listOf().fieldOf("leg_stations").forGetter(m -> m.legStations)
    ).apply(i, MissionState::new));
    public final UUID owner;
    public final SurveyKnowledge knowledge;
    public final BlockPos launch;
    public final List<SpaceSimulation.FlightPlanAction> completed = new ArrayList<>();
    public ActiveRocketData rocket;
    public SpaceSimulation.FlightPlan plan;
    public Position position;
    public long actionTicks;
    public List<DockingTarget> legStations = List.of();
    public long positionRevision;
    public int serviceProgress;
    public String status;
    public Telemetry leg;
    public BlockPos landing;
    public List<SpaceSimulation.SpaceObjectData> legObjects = List.of();
    public String route = "communication.oritech_space_age.no_route";
    public boolean connected;
    public boolean canTransmit;
    public SpaceCommunications.Connection communication = new SpaceCommunications.Connection(false, false, 0, 0,
            "communication.oritech_space_age.no_ground");
    RocketFlightPathCalculator.FlightPath path;
    boolean ended;
    private Telemetry forecastLeg;
    private SpaceSimulation.FlightPlan forecastPlan;
    private Collection<RecipeHolder<VacuumRecipe>> forecastRecipes;
    private MissionForecast.Navigation navigationForecast = MissionForecast.Navigation.EMPTY;
    private MissionState(UUID owner, ActiveRocketData rocket, SpaceSimulation.FlightPlan plan, Position position,
                         SurveyKnowledge knowledge, long ticks, String status,
                         Optional<Telemetry> leg, BlockPos launch, List<SpaceSimulation.FlightPlanAction> completed,
                         Optional<BlockPos> landing, List<SpaceSimulation.SpaceObjectData> legObjects,
                         long positionRevision, int serviceProgress, List<DockingTarget> legStations) {

        this.legStations = List.copyOf(legStations);
        this.positionRevision = positionRevision;
        this.serviceProgress = serviceProgress;
        this.landing = landing.orElse(null);
        this.legObjects = List.copyOf(legObjects);
        this.owner = owner;
        this.rocket = rocket;
        this.plan = plan;
        this.position = position;
        this.knowledge = knowledge;
        this.actionTicks = ticks;
        this.status = status;
        this.completed.addAll(completed);
        this.leg = leg.orElse(null);
        this.launch = launch;
        this.ended = isRecovered(status) || status.equals(STATUS_DESTROYED) || status.equals(STATUS_DISCARDED);
    }

    public MissionState(UUID owner, ActiveRocketData rocket, SpaceSimulation.FlightPlan plan,
                        Position position, SurveyKnowledge knowledge, BlockPos launch, long tick) {

        this(owner, rocket, plan, position, knowledge, 0, STATUS_READY,
                Optional.empty(), launch, List.of(), Optional.empty(), List.of(), 0, 0, List.of());
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
        copy.setLaunchPosition(rocket.getLaunchPosition());
        return copy;
    }

    public MissionForecast.Navigation navigationForecast() {

        return navigationForecast(List.of(), List.of());
    }

    public MissionForecast.Navigation navigationForecast(Collection<RecipeHolder<VacuumRecipe>> recipes, List<DockingTarget> stations) {

        if (!legStations.isEmpty()) stations = legStations;
        if (leg == null || action() == null || action().type() != SpaceSimulation.ActionType.NAVIGATE_TO || ended)
            return MissionForecast.Navigation.EMPTY;
        if (forecastLeg != leg || !plan.equals(forecastPlan) || forecastRecipes != recipes) {
            var flight = RocketFlightPathCalculator.calculateFrom(leg.rocket(), legObjects, plan, leg.position(), recipes, stations);
            navigationForecast = MissionForecast.Navigation.forClient(flight);
            forecastLeg = leg;
            forecastPlan = plan;
            forecastRecipes = recipes;
        }
        return navigationForecast;
    }

    public SpaceSimulation.FlightPlanAction action() {

        return plan.root().actions().stream().filter(a -> !a.isGenerated()).findFirst().orElse(null);
    }

    public boolean canUpdateMission() {

        return connected && !ended;
    }

    public boolean canDismiss() {

        return ended || action() == null;
    }

    public void complete() {

        var root = plan.root();
        var actions = new ArrayList<>(root.actions());
        if (!actions.isEmpty()) completed.add(actions.removeFirst());
        while (!actions.isEmpty() && actions.getFirst().isGenerated()) actions.removeFirst();
        plan = plan.withBranches(plan.branches().stream().map(b -> b.isRoot() ? b.withActions(actions) : b).toList());
        actionTicks = 0;
        serviceProgress = 0;
        leg = null;
        path = null;
        landing = null;
        legStations = List.of();
    }

    public Telemetry telemetry(long tick) {

        return new Telemetry(copyRocket(rocket), plan, position, tick, status, List.copyOf(completed), actionTicks,
                actionDurationTicks(), serviceProgress);
    }

    private long actionDurationTicks() {

        var action = action();
        if (action == null) return 0;
        if (action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION) return action.service().timeoutTicks();
        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO || path == null) return 0;
        return path.paths().stream().filter(item -> item.branchId().equals(plan.root().id())).findFirst()
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
                            long actionDurationTicks, int serviceProgress) {

        public static final Codec<Telemetry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ActiveRocketData.CODEC.fieldOf("rocket").forGetter(Telemetry::rocket),
                SpaceSimulation.FlightPlan.CODEC.fieldOf("plan").forGetter(Telemetry::plan),
                Position.CODEC.fieldOf("position").forGetter(Telemetry::position),
                Codec.LONG.fieldOf("tick").forGetter(Telemetry::tick), Codec.STRING.fieldOf("status").forGetter(Telemetry::status),
                SpaceSimulation.FlightPlanAction.CODEC.listOf().fieldOf("completed").forGetter(Telemetry::completed),
                Codec.LONG.optionalFieldOf("action_ticks", 0L).forGetter(Telemetry::actionTicks),
                Codec.LONG.optionalFieldOf("action_duration_ticks", 0L).forGetter(Telemetry::actionDurationTicks),
                Codec.INT.fieldOf("service_progress").forGetter(Telemetry::serviceProgress)
        ).apply(i, Telemetry::new));

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
