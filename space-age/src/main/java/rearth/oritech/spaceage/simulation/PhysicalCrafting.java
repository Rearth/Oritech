package rearth.oritech.spaceage.simulation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.HardwareRef;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.Processing;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.SourceScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pair discovery and ownership are shared by live services and isolated forecasts.
 */
public final class PhysicalCrafting {

    private PhysicalCrafting() {
    }

    public static List<RocketControllerState.Pair> candidates(ActiveRocketData requester, ActiveRocketData machineRocket, Processing settings,
                                                              Collection<RecipeHolder<VacuumRecipe>> recipes) {

        return candidates(requester, machineRocket, null, settings, recipes);
    }

    public static List<RocketControllerState.Pair> candidates(ActiveRocketData requester, ActiveRocketData machineRocket, ActiveRocketData partner, Processing settings,
                                                              Collection<RecipeHolder<VacuumRecipe>> recipes) {

        var result = new ArrayList<RocketControllerState.Pair>();
        // Local work faces have priority over freight, even for a remote station crafter.
        for (var machine : settings.modules()) {
            if (RocketProcessingService.controller(machineRocket, machine) != null)
                add(result, machineRocket, RocketLayout.pair(machineRocket, machine), recipes);
        }

        // Requester is first for remote service; for local service it is also the machine rocket.
        if (settings.scope() != SourceScope.MACHINE) {
            for (var cargo : RocketLayout.controllers(requester, false))
                add(result, requester, RocketLayout.pair(requester, cargo), recipes);
        }
        if (settings.scope() == SourceScope.MACHINE || requester != machineRocket && settings.scope() != SourceScope.REQUESTER) {
            for (var cargo : RocketLayout.controllers(machineRocket, false))
                add(result, machineRocket, RocketLayout.pair(machineRocket, cargo), recipes);
        }
        if (requester == machineRocket && partner != null && settings.scope() == SourceScope.BOTH) {
            for (var cargo : RocketLayout.controllers(partner, false))
                add(result, partner, RocketLayout.pair(partner, cargo), recipes);
        }

        return List.copyOf(result);
    }

    private static void add(List<RocketControllerState.Pair> result, ActiveRocketData rocket, RocketControllerState.Pair pair, Collection<RecipeHolder<VacuumRecipe>> recipes) {

        if (RocketLayout.busy(rocket, pair) || !match(rocket, pair, recipes).valid()) return;
        if (result.stream().anyMatch(p -> overlap(p, pair))) return;
        result.add(pair);
    }

    public static boolean overlap(RocketControllerState.Pair a, RocketControllerState.Pair b) {

        return a.rocket().equals(b.rocket()) && a.controller().segment().equals(b.controller().segment())
                && (a.left().equals(b.left()) || a.left().equals(b.right()) || a.right().equals(b.left()) || a.right().equals(b.right()));
    }

    public static boolean canUse(ActiveRocketData machines, HardwareRef machine, RocketControllerState.Pair pair) {

        // Other selected crafters keep their own work faces; cargo is the shared source.
        return !pair.rocket().equals(machines.getRocketId())
                || !RocketLayout.state(machines, pair.controller().segment(), pair.controller().position()).is(SpaceAgeBlocks.VACUUM_CRAFTER)
                || pair.controller().equals(machine);
    }

    public static VacuumProcessing.Match match(ActiveRocketData source, RocketControllerState.Pair pair, Collection<RecipeHolder<VacuumRecipe>> recipes) {

        return VacuumProcessing.match(RocketLayout.state(source, pair.controller().segment(), pair.left()),
                RocketLayout.state(source, pair.controller().segment(), pair.right()), recipes);
    }

    public static String start(ActiveRocketData machines, HardwareRef machine, ActiveRocketData source, RocketControllerState.Pair pair,
                               Collection<RecipeHolder<VacuumRecipe>> recipes, UUID operator) {

        var controller = RocketProcessingService.controller(machines, machine);
        if (controller == null) return "module_missing";
        if (controller.job != null || RocketLayout.busy(source, pair)) return "module_busy";
        var match = match(source, pair, recipes);
        if (!match.valid()) return match.issue();
        if (!passive(match.recipe().resultState())) return "invalid_cargo_block";
        var first = RocketLayout.state(source, pair.controller().segment(), pair.left());
        var second = RocketLayout.state(source, pair.controller().segment(), pair.right());
        if (!passive(first) || !passive(second)) return "invalid_cargo_block";
        // Keep the inputs for cancellation and mass accounting while their physical cells are empty.
        var resources = source.getDynamicSegments().get(RocketLayout.segmentId(source, pair.controller().segment()));
        resources.reserved.put(pair.left(), first);
        resources.reserved.put(pair.right(), second);
        RocketLayout.setPair(source, pair, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState());
        var recipe = match.recipe();
        controller.job = new RocketControllerState.Job(match.id(), pair, first, second, recipe.resultState(), recipe.ticks(), recipe.rfPerTick(), 0, operator);
        return "";
    }

    public static boolean passive(BlockState state) {

        // Freight carries block states only; block entities and rocket hardware have separate ownership.
        return !state.hasBlockEntity() && !BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("oritech_space_age");
    }

    public static void finish(RocketControllerState machine, ActiveRocketData source, boolean cancel) {

        var job = machine.job;
        if (job == null) return;
        var id = RocketLayout.segmentId(source, job.source().controller().segment());
        if (id != null) {
            RocketLayout.setPair(source, job.source(), cancel ? job.first() : job.result(), cancel ? job.second() : job.result());
            var reserved = source.getDynamicSegments().get(id).reserved;
            reserved.remove(job.source().left());
            reserved.remove(job.source().right());
        }
        machine.job = null;
    }

    public static boolean spendRF(ActiveRocketData machines, HardwareRef machine, long amount) {

        if (machines.getDynamicSegments().values().stream().mapToLong(r -> r.availableRF).sum() < amount) return false;

        // Drain the machine's segment first, then the rest in stable order for matching forecasts.
        var local = RocketProcessingService.resources(machines, machine);
        var taken = Math.min(local.availableRF, amount);
        local.availableRF -= taken;
        amount -= taken;
        for (var entry : machines.getDynamicSegments().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            taken = Math.min(entry.getValue().availableRF, amount);
            entry.getValue().availableRF -= taken;
            amount -= taken;
        }
        return true;
    }

    public static int work(ActiveRocketData machines, HardwareRef ref, ActiveRocketData source, int ticks) {

        var machine = RocketProcessingService.controller(machines, ref);
        var job = machine.job;
        var available = machines.getDynamicSegments().values().stream().mapToLong(r -> r.availableRF).sum();
        var remaining = job.duration() - job.elapsed();
        var affordable = job.rfPerTick() == 0 ? remaining : available / job.rfPerTick();
        var performed = (int) Math.min(Math.min(ticks, remaining), affordable);
        if (performed == 0) return 0;
        spendRF(machines, ref, (long) performed * job.rfPerTick());
        machine.job = job.progressed(performed);
        if (machine.job.elapsed() >= job.duration()) finish(machine, source, false);
        return performed;
    }
}
