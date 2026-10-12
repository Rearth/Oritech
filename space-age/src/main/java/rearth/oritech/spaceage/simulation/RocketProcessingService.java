package rearth.oritech.spaceage.simulation;

import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.HardwareRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Actions schedule finite passes; jobs advance once globally, even after a program edit.
 */
public final class RocketProcessingService {

    private RocketProcessingService() {
    }

    public static List<HardwareRef> modules(ActiveRocketData rocket) {

        return RocketLayout.controllers(rocket, true);
    }

    public static DynamicRocketSegment resources(ActiveRocketData rocket, HardwareRef ref) {

        return rocket.getDynamicSegments().get(RocketLayout.segmentId(rocket, ref.segment()));
    }

    public static RocketControllerState controller(ActiveRocketData rocket, HardwareRef ref) {

        if (!RocketLayout.state(rocket, ref.segment(), ref.position()).is(SpaceAgeBlocks.VACUUM_CRAFTER))
            return null;
        var resources = resources(rocket, ref);
        return resources == null ? null : resources.crafters.get(ref.position());
    }

    static DynamicRocketSegment passStorage(ActiveRocketData rocket) {

        return rocket.getDynamicSegments().entrySet().stream().min(Map.Entry.comparingByKey()).orElseThrow().getValue();
    }

    static void process(MissionSavedData data, MissionState state, SpaceSimulation system, FlightPlanAction action, Collection<RecipeHolder<VacuumRecipe>> recipes) {

        state.actionTicks++;
        if (action.service().timeoutTicks() > 0 && state.actionTicks > action.service().timeoutTicks()) {
            state.complete();
            state.status = "status.oritech_space_age.processing_timed_out";
            return;
        }
        var settings = action.settings().processing();
        var target = settings.host().equals(FlightPlanAction.NO_TARGET) ? state : RocketDocking.serviceHost(data, state, settings.host());
        if (target == null || settings.modules().isEmpty() || settings.modules().stream().anyMatch(ref -> controller(target.rocket, ref) == null)) {
            state.status = "status.oritech_space_age.module_missing";
            return;
        }
        if (!VacuumProcessing.zeroG(state.position.x(), state.position.y(), system.truth())) {
            state.status = "status.oritech_space_age.requires_zero_g";
            return;
        }
        var link = RocketDocking.visitorLink(data, state.rocket.getRocketId());
        if (Math.hypot(state.position.vx(), state.position.vy()) <= .001 && link == null
                && (state.actionTicks - 1) % SpaceBalance.STATION_KEEPING_INTERVAL_TICKS == 0
                && !StationServiceRules.upkeep(state, RocketDocking.mass(data, state), SpaceBalance.STATION_KEEPING_INTERVAL_TICKS)) {
            state.status = MissionState.STATUS_STATION_KEEPING_EXHAUSTED;
            return;
        }
        var storage = passStorage(state.rocket);
        if (storage.pass == null || !storage.pass.action().equals(action.id())) {
            var partner = link == null ? null : data.craft.get(link.host());
            var pairs = PhysicalCrafting.candidates(state.rocket, target.rocket, partner == null ? null : partner.rocket, settings, recipes);
            var running = (int) target.rocket.getDynamicSegments().values().stream().flatMap(r -> r.crafters.values().stream())
                    .filter(c -> c.job != null && c.job.operator().equals(state.rocket.getRocketId())).count();
            if (pairs.isEmpty() && running == 0) {
                state.status = "status.oritech_space_age.no_valid_input_pair";
                return;
            }
            storage.pass = new CraftPass(action.id(), pairs, pairs.size() + running);
        }
        var pass = storage.pass;
        var issue = "processing";
        for (var ref : settings.modules()) {
            var machine = controller(target.rocket, ref);
            if (machine.job != null) {
                issue = machine.job.operator().equals(state.rocket.getRocketId()) ? "processing" : "module_busy";
                continue;
            }
            // Give each selected machine its own local pair before it helps with the shared cargo queue.
            var local = RocketLayout.pair(target.rocket, ref);
            var pair = pass.pending().stream().filter(p -> PhysicalCrafting.overlap(p, local)).findFirst().orElseGet(() ->
                    pass.pending().stream().filter(p -> PhysicalCrafting.canUse(target.rocket, ref, p)).findFirst().orElse(null));
            if (pair == null) continue;
            var source = data.craft.get(pair.rocket());
            if (source == null || source.ended) {
                pass.pending().remove(pair);
                continue;
            }
            issue = PhysicalCrafting.start(target.rocket, ref, source.rocket, pair, recipes, state.rocket.getRocketId());
            if (!issue.equals("module_busy")) pass.pending().remove(pair);
        }
        var running = data.craft.values().stream().flatMap(m -> m.rocket.getDynamicSegments().values().stream()).flatMap(r -> r.crafters.values().stream())
                .anyMatch(c -> c.job != null && c.job.operator().equals(state.rocket.getRocketId()));
        state.serviceProgress = pass.total() - pass.pending().size();
        var needsPower = target.rocket.getDynamicSegments().values().stream().flatMap(resources -> resources.crafters.values().stream())
                .anyMatch(machine -> machine.job != null && machine.job.rfPerTick() > 0);
        if (target.rocket.getDynamicSegments().values().stream().mapToLong(r -> r.availableRF).sum() == 0 && needsPower)
            issue = "processing_no_power";
        state.status = "status.oritech_space_age." + (issue.isEmpty() ? "processing" : issue);
        if (pass.pending().isEmpty() && !running) {
            storage.pass = null;
            state.complete();
        } else if (action.service().timeoutTicks() > 0 && state.actionTicks >= action.service().timeoutTicks()) {
            state.complete();
            state.status = "status.oritech_space_age.processing_timed_out";
        }
    }

    static void tickJobs(MissionSavedData data, Function<UUID, Collection<SpaceObjectData>> objects) {

        // Jobs belong to machines, so program edits must not stop or double-tick them.
        for (var host : data.craft.values()) {
            for (var ref : modules(host.rocket)) {
                var machine = controller(host.rocket, ref);
                var job = machine.job;
                if (job == null) continue;
                var source = data.craft.get(job.source().rocket());
                if (source == null || source.ended) {
                    machine.job = null;
                    continue;
                }
                if (host.ended) {
                    PhysicalCrafting.finish(machine, source.rocket, true);
                    continue;
                }
                if (source != host && !data.dockingLinks.stream().anyMatch(l -> l.host().equals(host.rocket.getRocketId()) && l.visitor().equals(source.rocket.getRocketId())
                        || l.visitor().equals(host.rocket.getRocketId()) && l.host().equals(source.rocket.getRocketId()))) {
                    PhysicalCrafting.finish(machine, source.rocket, true);
                    continue;
                }
                if (!VacuumProcessing.zeroG(host.position.x(), host.position.y(), objects.apply(host.owner))) continue;
                if (PhysicalCrafting.work(host.rocket, ref, source.rocket, 1) == 0)
                    host.status = "status.oritech_space_age.processing_no_power";
            }
        }

        // A destroyed/removed machine cannot leave surviving freight locked forever.
        var jobs = data.craft.values().stream().flatMap(m -> m.rocket.getDynamicSegments().values().stream())
                .flatMap(r -> r.crafters.values().stream()).filter(c -> c.job != null).map(c -> c.job).toList();
        for (var state : data.craft.values()) {
            if (state.ended) continue;
            for (var entry : state.rocket.getDynamicSegments().entrySet()) {
                var resources = entry.getValue();
                var orphaned = resources.reserved.keySet().stream().filter(pos -> jobs.stream().noneMatch(job ->
                        job.source().rocket().equals(state.rocket.getRocketId())
                                && entry.getKey().equals(RocketLayout.segmentId(state.rocket, job.source().controller().segment()))
                                && (pos.equals(job.source().left()) || pos.equals(job.source().right())))).toList();
                orphaned.forEach(pos -> resources.layout.put(pos, resources.reserved.remove(pos)));
            }
        }
    }

    static boolean remoteBusy(MissionSavedData data, UUID visitor) {

        return data.craft.values().stream().anyMatch(host -> host.rocket.getDynamicSegments().values().stream()
                .flatMap(r -> r.crafters.values().stream()).anyMatch(c -> c.job != null && !c.job.source().rocket().equals(host.rocket.getRocketId())
                        && (host.rocket.getRocketId().equals(visitor) || c.job.source().rocket().equals(visitor))));
    }

    public static void cancel(MissionSavedData data, UUID requester) {

        // Either participant may cancel remote work. Restore inputs on their source, not on the machine.
        for (var host : data.craft.values()) {
            for (var resources : host.rocket.getDynamicSegments().values()) {
                for (var machine : resources.crafters.values()) {
                    var job = machine.job;
                    if (job == null) continue;
                    if (!job.operator().equals(requester) && !job.source().rocket().equals(requester) && !host.rocket.getRocketId().equals(requester))
                        continue;

                    var source = data.craft.get(job.source().rocket());
                    if (source != null && !source.ended) PhysicalCrafting.finish(machine, source.rocket, true);
                    else machine.job = null;
                }
            }
        }

        var state = data.craft.get(requester);
        if (state != null) {
            passStorage(state.rocket).pass = null;
            if (state.action() != null && state.action().type() == ActionType.PROCESS) state.complete();
        }
        data.setDirty();
    }
}
