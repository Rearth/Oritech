package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.Heightmap;
import rearth.oritech.spaceage.simulation.RocketControllerState.Job;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.HardwareRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;

import java.util.*;

/** Piecewise-linear power network. Only boundary/input events settle batteries and solve new flow rates. */
public final class SpacePowerService {

    private static final double EPSILON = 1e-6;
    private final Map<UUID, Input> inputs = new LinkedHashMap<>();
    private final Map<UUID, Account> accounts = new LinkedHashMap<>();
    private double now;
    private double epoch;
    private double groundEpoch;
    private final Map<GroundLink, Double> groundEnergy = new LinkedHashMap<>();
    private double nextEvent = Double.POSITIVE_INFINITY;
    private boolean dirty = true;
    private long revisions;
    private MissionSavedData context;
    private MinecraftServer server;
    private int speed = 1;
    private long groundCheckTick = Long.MIN_VALUE;

    public long revisions() {

        return revisions;
    }

    public double nextEvent() {

        return nextEvent;
    }

    /** Clock advancement is O(1) between events; RF reads project the established linear rates. */
    public void advance(double tick) {

        while (!dirty && nextEvent <= tick) {
            now = nextEvent;
            if (settle()) {
                dirty = true;
                accounts.clear();
                update(server, context, now);
            } else solve();
        }
        now = tick;
    }

    public void update(MinecraftServer server, MissionSavedData data, double tick) {

        this.context = data;
        this.server = server;
        advance(tick);
        var next = new LinkedHashMap<UUID, Input>();
        for (var state : data.craft.values().stream().sorted(Comparator.comparing(s -> s.rocket.getRocketId())).toList()) {
            if (!SolarPower.stationary(state)) continue;
            var jobs = new LinkedHashMap<HardwareRef, Job>();
            for (var ref : state.rocket.processingModules()) {
                var machine = RocketProcessingService.controller(state.rocket, ref);
                if (machine.job != null && (server == null || VacuumProcessing.zeroG(state.position.x(), state.position.y(),
                        SpaceSimulationSavedData.get(server).getOrCreate(state.owner).truth()))) jobs.put(ref, machine.job);
            }
            next.put(state.rocket.getRocketId(), new Input(state, state.rocket, state.position, state.action(), state.rocket.powerHardware(),
                    state.rocket.hardware().values().stream().mapToInt(RocketHardware::antennas).sum(), Map.copyOf(jobs),
                    state.rocket.getDynamicSegments().values().stream().mapToDouble(s -> s.rfCapacity).sum()));
        }
        if (!next.equals(inputs) || speed != data.debugSpeed) {
            changed();
            for (var entry : inputs.entrySet())
                if (!next.containsKey(entry.getKey())) entry.getValue().state.powerReport = Report.EMPTY;
            inputs.clear();
            inputs.putAll(next);
            speed = data.debugSpeed;
        }
        // World availability changes once per world tick, independently of Creative simulation speed.
        if (server != null && groundCheckTick != server.overworld().getGameTime()) {
            groundCheckTick = server.overworld().getGameTime();
            var availabilityChanged = accounts.values().stream().anyMatch(account -> account.ground
                    && account.loaded != SurfaceBeamData.loaded(server.overworld(), account.action.landingX(), account.action.landingZ(), account.radius));
            if (availabilityChanged) changed();
        }
        if (!dirty) return;
        // New links start now; never integrate them over the time before their first solve.
        accumulateGround();
        accounts.clear();
        SunOrbitRules.validate(data);
        for (var input : inputs.values()) {
            if (input.state.ended || !SolarPower.stationary(input.state)) continue;
            accounts.put(input.rocket.getRocketId(), new Account(input));
        }
        for (var account : accounts.values()) {
            if (account.action == null || account.action.type() != ActionType.BEAM_POWER) continue;
            account.ground = account.action.targetId().equals(SpaceObjects.EARTH_ID);
            account.target = accounts.get(account.action.targetId());
            if (!account.ground && (account.target == null || account.target == account
                    || !account.target.state.owner.equals(account.state.owner))) continue;
            var position = account.state.position;
            var distance = account.ground ? Math.max(0, Math.hypot(position.x() + 3_000_000, position.y()) - 60_000)
                    : Math.hypot(position.x() - account.target.state.position.x(), position.y() - account.target.state.position.y());
            account.radius = BeamAperture.footprint(distance, account.hardware.focus(), account.ground);
            account.loaded = !account.ground || server != null && SurfaceBeamData.loaded(server.overworld(), account.action.landingX(), account.action.landingZ(), account.radius);
        }
        solve();
    }

    /** External resource changes commit the old interval before changing its inputs. */
    void changed() {

        if (dirty) return;
        settle();
        accounts.clear();
        dirty = true;
    }

    private boolean settle() {

        accumulateGround();
        var completedJob = false;
        for (var account : accounts.values()) {
            for (var segment : account.segments) {
                var projected = account.rf(segment);
                segment.powerAccount = null;
                segment.materializeRF(projected);
            }
            completedJob |= account.settleJobs();
        }
        epoch = now;
        return completedJob;
    }

    private void solve() {

        revisions++;
        dirty = false;
        epoch = now;
        for (var account : accounts.values()) account.capture();
        // The least fixed point prevents unpowered relay cycles from creating circulating power.
        for (int iteration = 0; iteration < 512; iteration++) {
            var difference = 0d;
            for (var account : accounts.values()) {
                account.chooseRates();
                account.nextIncoming = 0;
                account.outgoing = BeamReception.EMPTY;
                account.receiving = BeamReception.EMPTY;
            }
            for (var source : accounts.values()) {
                if (source.target == null || source.ground || source.sent <= 0) continue;
                var target = source.target;
                var incident = source.sent;
                var collected = incident * BeamAperture.collectionFraction(target.hardware.area(), source.radius);
                var room = target.stored < target.capacity - EPSILON ? Double.POSITIVE_INFINITY
                        : Math.max(0, target.services + target.sent - target.solar);
                source.outgoing = BeamReception.limited(incident, collected, Math.max(0, target.hardware.throughput() - target.nextIncoming),
                        Math.max(0, room - target.nextIncoming), source.radius);
                target.nextIncoming += source.outgoing.accepted();
                target.receiving = target.receiving.plus(source.outgoing);
            }
            for (var account : accounts.values()) {
                difference = Math.max(difference, Math.abs(account.incoming - account.nextIncoming));
                account.incoming = account.nextIncoming;
            }
            if (difference < EPSILON) break;
        }
        nextEvent = Double.POSITIVE_INFINITY;
        for (var account : accounts.values()) {
            account.chooseRates();
            account.net = account.solar + account.incoming - account.services - account.sent;
            if (account.stored >= account.capacity - EPSILON) account.net = Math.min(0, account.net);
            if (account.stored <= EPSILON) account.net = Math.max(0, account.net);
            var boundary = account.net > EPSILON ? account.capacity : 0;
            if (account.net > EPSILON && account.stored < account.reserve - EPSILON) boundary = account.reserve;
            if (account.net < -EPSILON && account.stored > account.reserve + EPSILON) boundary = account.reserve;
            if (Math.abs(account.net) <= EPSILON) account.net = 0;
            if (Math.abs(account.net) > EPSILON) nextEvent = Math.min(nextEvent, now + Math.max(EPSILON, (boundary - account.stored) / account.net));
            for (var ref : account.jobSpeeds.keySet()) {
                var start = account.jobStarts.get(ref);
                nextEvent = Math.min(nextEvent, now + Math.max(EPSILON, (start.job.duration() - start.job.elapsed() - start.fraction) / account.jobSpeeds.get(ref)));
            }
            if (account.readyAt > now) nextEvent = Math.min(nextEvent, account.readyAt);
            account.publish();
            for (var segment : account.segments) {
                segment.powerAccount = account;
            }
        }
    }

    private void accumulateGround() {

        var elapsed = Math.max(0, now - groundEpoch);
        if (elapsed > 0) {
            for (var account : accounts.values()) {
                if (account.ground && account.loaded && account.sent > 0)
                    groundEnergy.merge(new GroundLink(account.state, account.action, account.radius), account.sent * elapsed, Double::sum);
            }
        }
        groundEpoch = now;
    }

    public void surface(MinecraftServer server) {

        accumulateGround();
        for (var entry : groundEnergy.entrySet()) {
            var link = entry.getKey();
            var incident = entry.getValue();
            var reception = SurfaceBeamData.get(server.overworld()).deliver(server.overworld(), link.action.landingX(), link.action.landingZ(), link.radius, incident);
            var current = accounts.get(link.source.rocket.getRocketId());
            if (current != null && link.action.equals(current.action)) {
                current.outgoing = incident > 0 ? reception.scaled(current.sent / incident) : BeamReception.EMPTY;
                current.publish();
                if (reception.accepted() < incident && current.sent > 0) current.state.status = "status.oritech_space_age.surface_overflow";
            }
        }
        groundEnergy.clear();
    }

    private record GroundLink(MissionState source, FlightPlanAction action, double radius) {}

    public List<SurfaceBeam> surfaceTargets(MinecraftServer server) {

        var result = new ArrayList<SurfaceBeam>();
        for (var account : accounts.values()) {
            if (!account.ground || !account.loaded || account.sent <= 0) continue;
            result.add(new SurfaceBeam(server.overworld().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                    new BlockPos(account.action.landingX(), 0, account.action.landingZ())), account.radius));
        }
        return result;
    }

    public record SurfaceBeam(BlockPos target, double radius) {}

    static void step(MissionSavedData data, MissionState state) {

        var action = state.action();
        if (!SolarPower.stationary(state)) {
            state.status = "status.oritech_space_age.beam_stationary_required";
            return;
        }
        var timeout = action.addons().stream().filter(a -> a.type() == ActionAddonType.DURATION).findFirst().orElse(null);
        var condition = action.addons().stream().filter(a -> a.type().isStationaryCondition()).findFirst().orElse(null);
        if (timeout != null && state.actionTicks() >= timeout.value() * 20L
                || condition != null && StationServiceRules.conditionReached(StationServiceRules.budgets(state.rocket), condition, 0, state.actionTicks() / 20d)) {
            state.complete();
            return;
        }
        if (!StationServiceRules.available(state)) {
            state.complete();
            state.status = MissionState.STATUS_STATION_KEEPING_EXHAUSTED;
            return;
        }
        if (state.advanceAction() % SpaceBalance.STATION_KEEPING_INTERVAL_TICKS == 0)
            StationServiceRules.upkeep(state, RocketDocking.mass(data, state), SpaceBalance.STATION_KEEPING_INTERVAL_TICKS);
    }

    public static Account account(ActiveRocketData rocket) {

        return rocket.getDynamicSegments().values().stream().map(s -> s.powerAccount).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private record Input(MissionState state, ActiveRocketData rocket, MissionState.Position position, FlightPlanAction action,
                         SpacePowerHardware hardware, int antennas, Map<HardwareRef, Job> jobs, double capacity) {}

    public final class Account {

        private final MissionState state;
        private final FlightPlanAction action;
        private final SpacePowerHardware hardware;
        private final List<DynamicRocketSegment> segments;
        private final Map<DynamicRocketSegment, Double> baseline = new HashMap<>();
        private final int antennas;
        private final Map<HardwareRef, Job> jobs;
        private final Map<HardwareRef, Double> jobSpeeds = new LinkedHashMap<>();
        private final Map<HardwareRef, JobStart> jobStarts = new LinkedHashMap<>();
        private final double capacity;
        private final double solar;
        private final double reserve;
        private double stored;
        private double incoming;
        private double nextIncoming;
        private double services;
        private double sent;
        private BeamReception outgoing = BeamReception.EMPTY;
        private BeamReception receiving = BeamReception.EMPTY;
        private double net;
        private double radius;
        private double readyAt;
        private boolean ground;
        private boolean loaded;
        private Account target;
        private int poweredAntennas;

        private Account(Input input) {

            state = input.state;
            action = input.action;
            hardware = input.hardware;
            capacity = input.capacity;
            antennas = input.antennas;
            jobs = input.jobs;
            segments = input.rocket.getDynamicSegments().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
            solar = SolarPower.output(state);
            reserve = capacity * (action != null && action.type() == ActionType.BEAM_POWER ? action.settings().beam().reservePercent() : 0) / 100d;
            if (action != null && action.type() == ActionType.BEAM_POWER && !action.id().equals(state.powerAction)) {
                state.powerAction = action.id();
                state.powerReadyAt = now + 1;
            }
            readyAt = action != null && action.type() == ActionType.BEAM_POWER ? state.powerReadyAt : now;
        }

        private void capture() {

            baseline.clear();
            jobStarts.clear();
            for (var ref : jobs.keySet()) {
                var machine = RocketProcessingService.controller(state.rocket, ref);
                if (machine == null || machine.job == null) continue;
                jobStarts.put(ref, new JobStart(machine.job, machine.workFraction));
                machine.powerClock = this;
                machine.powerRef = ref;
            }
            stored = 0;
            incoming = 0;
            for (var segment : segments) {
                var value = segment.rf();
                baseline.put(segment, value);
                stored += value;
            }
        }

        public double rf(DynamicRocketSegment segment) {

            var delta = net * Math.max(0, now - epoch);
            for (var current : segments) {
                var base = baseline.getOrDefault(current, current.availableRF + current.rfFraction());
                var change = delta >= 0 ? Math.min(delta, Math.max(0, current.rfCapacity - base)) : -Math.min(-delta, base);
                if (current == segment) return Math.max(0, base + change);
                delta -= change;
            }
            return segment.availableRF + segment.rfFraction();
        }

        public Job jobSnapshot(HardwareRef ref, Job current) {

            var start = jobStarts.get(ref);
            if (current == null || start == null || current != start.job) return current;
            var elapsed = start.job.elapsed() + start.fraction + (jobSpeeds.getOrDefault(ref, 0d) * Math.max(0, now - epoch));
            return start.job.progressed(Math.max(0, (int) Math.min(start.job.duration(), elapsed + 1e-9) - start.job.elapsed()));
        }

        public double jobFraction(HardwareRef ref, Job current) {

            var start = jobStarts.get(ref);
            if (current == null || start == null || current != start.job) return 0;
            var elapsed = start.job.elapsed() + start.fraction + (jobSpeeds.getOrDefault(ref, 0d) * Math.max(0, now - epoch));
            return Math.max(0, elapsed - Math.floor(elapsed + 1e-9));
        }

        private boolean settleJobs() {

            var completed = false;
            for (var entry : jobStarts.entrySet()) {
                var machine = RocketProcessingService.controller(state.rocket, entry.getKey());
                if (machine == null) continue;
                var projected = jobSnapshot(entry.getKey(), machine.job);
                var fraction = jobFraction(entry.getKey(), machine.job);
                machine.powerClock = null;
                if (projected == null) continue;
                machine.job = projected;
                machine.workFraction = Math.max(0, fraction);
                if (projected.elapsed() >= projected.duration()) {
                    var source = context.craft.get(projected.source().rocket());
                    if (source != null) PhysicalCrafting.finish(machine, source.rocket, false);
                    else machine.job = null;
                    machine.workFraction = 0;
                    completed = true;
                }
            }
            return completed;
        }

        private record JobStart(Job job, double fraction) {}

        void changed() {

            SpacePowerService.this.changed();
        }

        public double solarGeneration() {

            return solar;
        }

        public int poweredAntennas() {

            return poweredAntennas;
        }

        private void chooseRates() {

            var supply = solar + incoming;
            var available = stored > EPSILON ? Double.POSITIVE_INFINITY : supply;
            poweredAntennas = Math.min(antennas, (int) Math.min(Integer.MAX_VALUE, available / SpaceBalance.ANTENNA_RF));
            services = Math.min(antennas * SpaceBalance.ANTENNA_RF, available);
            // At brownout the existing load absorbs supply; it cannot claim a fully powered antenna/job.
            // This keeps an undersupplied idle network at a stable rate instead of chattering at zero RF.
            jobSpeeds.clear();
            for (var entry : jobs.entrySet()) {
                var cost = entry.getValue().rfPerTick();
                var rate = cost <= 0 ? 1 : Math.min(1, Math.max(0, available - services) / cost);
                if (rate <= 0) continue;
                services += cost * rate;
                jobSpeeds.put(entry.getKey(), rate);
            }
            sent = 0;
            if (action == null || action.type() != ActionType.BEAM_POWER || !loaded || !ground && target == null
                    || capacity <= 0 || reserve >= capacity) return;
            var limit = hardware.throughput() / (ground ? speed : 1);
            if (stored > reserve + EPSILON) sent = limit;
            else if (stored >= reserve - EPSILON && now >= readyAt) sent = Math.min(limit, Math.max(0, supply - services));
        }

        private void publish() {

            var reason = sent > 0 && outgoing.incident() > outgoing.accepted() + EPSILON
                    ? ground ? "screen.oritech_space_age.beam.ground_overflow" : "screen.oritech_space_age.beam.space_overflow" : "";
            state.powerReport = new Report(solar, incoming, sent, outgoing, receiving, reason, services, net);
            if (action != null && action.type() == ActionType.BEAM_POWER) state.status = !loaded ? ground
                    ? "status.oritech_space_age.beam_target_unloaded" : "status.oritech_space_age.beam_target_unavailable"
                    : hardware.throughput() <= 0 ? "status.oritech_space_age.beam_no_aperture"
                    : sent <= EPSILON ? "status.oritech_space_age.beam_reserve"
                    : sent < hardware.throughput() / (ground ? speed : 1) - EPSILON ? "status.oritech_space_age.beam_source_limited" : "status.oritech_space_age.beaming";
        }
    }

    public record Report(double solar, double incoming, double sent, BeamReception outgoing, BeamReception receiving, String reason, double services, double net) {

        public static final Codec<Report> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("solar").forGetter(Report::solar), Codec.DOUBLE.fieldOf("incoming").forGetter(Report::incoming),
                Codec.DOUBLE.fieldOf("sent").forGetter(Report::sent), BeamReception.CODEC.fieldOf("outgoing").forGetter(Report::outgoing),
                BeamReception.CODEC.fieldOf("receiving").forGetter(Report::receiving), Codec.STRING.fieldOf("reason").forGetter(Report::reason),
                Codec.DOUBLE.fieldOf("services").forGetter(Report::services), Codec.DOUBLE.fieldOf("net").forGetter(Report::net)
        ).apply(i, Report::new));
        public static final Report EMPTY = new Report(0, 0, 0, BeamReception.EMPTY, BeamReception.EMPTY, "", 0, 0);

        /** Convert simulation-tick rates to the physical world-tick rates used by block screens. */
        public Report perWorldTick(int simulationSpeed) {

            return new Report(solar * simulationSpeed, incoming * simulationSpeed, sent * simulationSpeed,
                    outgoing.scaled(simulationSpeed), receiving.scaled(simulationSpeed), reason,
                    services * simulationSpeed, net * simulationSpeed);
        }

        public double delivered() {

            return sent;
        }

        public double accepted() {

            return outgoing.accepted();
        }

        public double overflow() {

            return Math.max(0, outgoing.incident() - outgoing.accepted());
        }
    }
}
