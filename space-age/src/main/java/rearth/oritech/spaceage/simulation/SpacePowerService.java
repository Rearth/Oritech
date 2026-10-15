package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

/** Debit every sender before crediting any receiver, so a hop always takes at least one tick. */
public final class SpacePowerService {

    private SpacePowerService() {
    }

    public static void generate(MissionSavedData data) {

        SunOrbitRules.validate(data);
        for (var state : data.craft.values()) {
            var solar = SolarPower.stationary(state) ? SolarPower.output(state) : 0;
            state.powerStartRF = state.rocket.getDynamicSegments().values().stream().mapToDouble(DynamicRocketSegment::rf).sum();
            state.solarAccepted = SolarPower.receive(state.rocket, solar);
            state.powerReport = new Report(solar, 0, 0, 0, 0, 0, "");
        }
    }

    public static void beams(MinecraftServer server, MissionSavedData data, boolean physicalTick) {

        var deliveries = new ArrayList<Delivery>();
        var receiverRemaining = new HashMap<UUID, Double>();
        for (var state : data.craft.values().stream().sorted(Comparator.comparing(s -> s.rocket.getRocketId())).toList()) {
            var action = state.action();
            if (state.ended || action == null || action.type() != ActionType.BEAM_POWER) continue;
            if (!SolarPower.stationary(state)) {
                state.status = "status.oritech_space_age.beam_stationary_required";
                continue;
            }
            var timeout = action.addons().stream().filter(a -> a.type() == ActionAddonType.DURATION).findFirst().orElse(null);
            var condition = action.addons().stream().filter(a -> a.type().isStationaryCondition()).findFirst().orElse(null);
            if (timeout != null && state.actionTicks() >= timeout.value() * 20L
                    || condition != null && StationServiceRules.conditionReached(StationServiceRules.budgets(state.rocket), condition, 0, state.actionTicks() / 20d)) {
                state.complete();
                continue;
            }
            if (!StationServiceRules.available(state)) {
                state.complete();
                state.status = MissionState.STATUS_STATION_KEEPING_EXHAUSTED;
                continue;
            }
            if (state.advanceAction() % SpaceBalance.STATION_KEEPING_INTERVAL_TICKS == 0)
                StationServiceRules.upkeep(state, RocketDocking.mass(data, state), SpaceBalance.STATION_KEEPING_INTERVAL_TICKS);
            var hardware = state.rocket.powerHardware();
            var ground = action.targetId().equals(SpaceObjects.EARTH_ID);
            var target = data.craft.get(action.targetId());
            if (!ground && (target == null || target == state || !target.owner.equals(state.owner) || !SolarPower.stationary(target))) {
                state.status = "status.oritech_space_age.beam_target_unavailable";
                continue;
            }
            if (ground && !physicalTick) continue;
            var distance = ground ? Math.max(0, Math.hypot(state.position.x() + 3_000_000, state.position.y()) - 60_000)
                    : Math.hypot(state.position.x() - target.position.x(), state.position.y() - target.position.y());
            var radius = BeamAperture.footprint(distance, hardware.focus(), ground);
            if (ground && !SurfaceBeamData.loaded(server.overworld(), action.landingX(), action.landingZ(), radius)) {
                state.status = "status.oritech_space_age.beam_target_unloaded";
                continue;
            }
            var settings = action.settings().beam();
            var stored = state.rocket.getDynamicSegments().values().stream().mapToDouble(DynamicRocketSegment::rf).sum();
            var capacity = state.rocket.getDynamicSegments().values().stream().mapToDouble(s -> s.rfCapacity).sum();
            var sent = BeamAperture.availablePower(stored, capacity, hardware.throughput(), settings.reservePercent());
            var remaining = sent;
            for (var id : state.rocket.getDynamicSegments().keySet().stream().sorted().toList()) {
                var segment = state.rocket.getDynamicSegments().get(id);
                var debit = Math.min(segment.rf(), remaining);
                segment.spendRF(debit);
                remaining -= debit;
            }
            deliveries.add(new Delivery(state, target, sent, sent * BeamAperture.TRANSMISSION_EFFICIENCY, radius));
            state.status = hardware.throughput() <= 0 ? "status.oritech_space_age.beam_no_aperture"
                    : sent < hardware.throughput() ? "status.oritech_space_age.beam_source_limited" : "status.oritech_space_age.beaming";
        }
        for (var delivery : deliveries) {
            var source = delivery.source;
            var action = source.action();
            double accepted;
            if (delivery.target == null) {
                accepted = SurfaceBeamData.get(server.overworld()).deliver(server.overworld(), action.landingX(), action.landingZ(), delivery.radius, delivery.delivered);
            } else {
                var target = delivery.target;
                var id = target.rocket.getRocketId();
                var remaining = receiverRemaining.computeIfAbsent(id, ignored -> target.rocket.powerHardware().throughput());
                var collected = delivery.delivered * BeamAperture.collectionFraction(target.rocket.powerHardware().area(), delivery.radius);
                accepted = SolarPower.receive(target.rocket, Math.min(remaining, collected));
                receiverRemaining.put(id, remaining - accepted);
                var report = target.powerReport;
                target.powerReport = new Report(report.solar, report.incoming + accepted, report.sent, report.delivered, report.accepted, report.overflow, report.reason);
            }
            if (delivery.target == null && delivery.delivered > accepted) source.status = "status.oritech_space_age.surface_overflow";
            var report = source.powerReport;
            source.powerReport = new Report(report.solar, report.incoming, delivery.sent, delivery.delivered, accepted,
                    delivery.delivered - accepted, delivery.target == null ? "screen.oritech_space_age.beam.ground_overflow" : "screen.oritech_space_age.beam.space_overflow");
        }
    }

    public static void finishAccounting(MissionSavedData data) {

        for (var state : data.craft.values()) {
            var report = state.powerReport;
            var rf = state.rocket.getDynamicSegments().values().stream().mapToDouble(DynamicRocketSegment::rf).sum();
            var net = rf - state.powerStartRF;
            var services = Math.max(0, state.solarAccepted + report.incoming - report.sent - net);
            state.powerReport = new Report(report.solar, report.incoming, report.sent, report.delivered, report.accepted, report.overflow,
                    report.reason, services, net);
        }
    }

    private record Delivery(MissionState source, MissionState target, double sent, double delivered, double radius) {
    }

    public record Report(double solar, double incoming, double sent, double delivered, double accepted, double overflow, String reason, double services, double net) {

        public Report(double solar, double incoming, double sent, double delivered, double accepted, double overflow, String reason) {

            this(solar, incoming, sent, delivered, accepted, overflow, reason, 0, 0);
        }

        public static final Codec<Report> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("solar").forGetter(Report::solar), Codec.DOUBLE.fieldOf("incoming").forGetter(Report::incoming),
                Codec.DOUBLE.fieldOf("sent").forGetter(Report::sent), Codec.DOUBLE.fieldOf("delivered").forGetter(Report::delivered),
                Codec.DOUBLE.fieldOf("accepted").forGetter(Report::accepted), Codec.DOUBLE.fieldOf("overflow").forGetter(Report::overflow),
                Codec.STRING.fieldOf("reason").forGetter(Report::reason),
                Codec.DOUBLE.fieldOf("services").forGetter(Report::services), Codec.DOUBLE.fieldOf("net").forGetter(Report::net)
        ).apply(i, Report::new));
        public static final Report EMPTY = new Report(0, 0, 0, 0, 0, 0, "");
    }
}
