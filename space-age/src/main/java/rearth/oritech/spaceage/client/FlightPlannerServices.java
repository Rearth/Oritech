package rearth.oritech.spaceage.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.DockingTarget;
import rearth.oritech.spaceage.simulation.NavigationComputerRules;
import rearth.oritech.spaceage.simulation.RocketDocking;
import rearth.oritech.spaceage.simulation.RocketLayout;
import rearth.oritech.spaceage.simulation.RocketProcessingService;
import rearth.oritech.spaceage.simulation.RocketServiceSettings;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.Direction;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.Exchange;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.HardwareRef;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.Processing;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.ResourceExchange;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.SourceScope;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ServiceSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Small selectors for explicit services; every edit remains in the shared draft.
 */
final class FlightPlannerServices {

    private final FlightPlannerEditors editors;

    FlightPlannerServices(FlightPlannerEditors editors) {

        this.editors = editors;
    }

    private static <T> T next(List<T> values, T current) {

        return values.get((values.indexOf(current) + 1) % values.size());
    }

    private static List<HardwareRef> nextSelection(List<HardwareRef> available, List<HardwareRef> selected) {

        if (selected.isEmpty()) return List.of(available.getFirst());
        var index = available.indexOf(selected.getFirst()) + 1;
        return index >= available.size() ? List.of() : List.of(available.get(index));
    }

    private Component text(String key, Object... args) {

        return Component.translatable("screen.oritech_space_age.service." + key, args);
    }

    private ButtonWidget button(int x, int y, int width, Component text, boolean active, Runnable change) {

        var button = SpaceAgeButtons.panel(x, y - 4, width, 18, text, ignored -> change.run());
        button.setActive(active);
        editors.addPopupComponent(button.withZIndex(9_001));
        return button;
    }

    private List<DockingTarget> hosts() {

        return editors.currentDraftSnapshot().stations().stream().filter(t -> !t.id().equals(editors.flightPlanRocket().getRocketId())).toList();
    }

    private DockingTarget host(UUID id) {

        return hosts().stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }

    private ActiveRocketData rocket(UUID id) {

        var host = host(id);
        return host == null ? editors.flightPlanRocket() : host.rocket();
    }

    private Component hostName(UUID id) {

        var host = host(id);
        return host == null ? text("local") : Component.literal(host.name());
    }

    private DockingTarget exchangeHost(FlightPlanAction action) {

        if (action.type() == ActionType.DOCK) return host(action.settings().docking().host());
        var branch = editors.findBranchContaining(action);
        UUID id = FlightPlanAction.NO_TARGET;
        // An Undock card edits the host selected by the preceding Dock, or the live link when replanning.
        if (branch != null) {
            for (var prior : branch.actions()) {
                if (prior.id().equals(action.id())) break;
                if (prior.type() == ActionType.DOCK) id = prior.settings().docking().host();
                if (prior.type() == ActionType.UNDOCK) id = FlightPlanAction.NO_TARGET;
            }
        }
        var selected = host(id);
        return selected != null ? selected : hosts().stream().filter(t -> t.visitors().contains(editors.flightPlanRocket().getRocketId())).findFirst().orElse(null);
    }

    private Component selection(List<HardwareRef> refs, ActiveRocketData rocket) {

        return refs.isEmpty() ? text("all_cargo") : module(refs.getFirst(), rocket);
    }

    private void resource(FlightPlanAction action, Exchange exchange, boolean fuel, int x, int y, int width) {

        var resources = fuel ? exchange.fuel() : exchange.rf();
        button(x, y, width / 3 - 2, text(fuel ? "fuel_exchange" : "rf_exchange", text("direction_" + resources.direction().name().toLowerCase(Locale.ROOT))), true,
                () -> resourceSet(action, exchange, fuel, new ResourceExchange(next(List.of(Direction.values()), resources.direction()), resources.limit(), resources.reserve())));
        button(x + width / 3, y, width / 3 - 2, resources.limit() == Long.MAX_VALUE ? text("fill") : text("limit", resources.limit()), true,
                () -> editors.editResourceAmount(action, fuel, false)).withTooltip(text("resource_limit_help"));
        button(x + width * 2 / 3, y, width / 3, text("reserve", resources.reserve()), resources.direction() == Direction.SEND,
                () -> editors.editResourceAmount(action, fuel, true))
                .withTooltip(text("resource_reserve_help"));
    }

    private void resourceSet(FlightPlanAction action, Exchange exchange, boolean fuel, ResourceExchange resources) {

        set(action, action.settings().withExchange(new Exchange(exchange.cargo(), exchange.local(), exchange.remote(), exchange.filter(),
                fuel ? resources : exchange.fuel(), fuel ? exchange.rf() : resources)));
    }

    private Component module(HardwareRef ref, ActiveRocketData rocket) {

        if (ref == null || ref.equals(HardwareRef.NONE)) return text("choose_module");
        var resources = RocketProcessingService.resources(rocket, ref);
        var controller = resources == null ? null : resources.crafters.get(ref.position());
        if (controller != null && !controller.name.isBlank())
            return Component.literal(controller.name + " (" + ref.position().toShortString() + ")");
        return text("module", ref.position().toShortString());
    }

    private void set(FlightPlanAction action, RocketServiceSettings settings) {

        editors.changeServiceSettings(action, settings);
    }

    private void timeout(FlightPlanAction action, int x, int y, int width) {

        var computer = NavigationComputerRules.computers(editors.flightPlanRocket(), NavigationComputerRules.allSegments(editors.flightPlanRocket())) > 0;
        var button = SpaceAgeButtons.panel(x, y - 4, width, 18, text("timeout", action.service().timeoutTicks() / 20), ignored ->
                editors.changeService(action, new ServiceSettings(action.service().durationTicks(), action.service().timeoutTicks() == 0 ? 1200 : 0, action.service().slot())));
        button.setActive(computer);
        button.withTooltip(computer ? text("timeout_help") : Component.translatable("screen.oritech_space_age.validation.computer_required"));
        editors.addPopupComponent(button.withZIndex(9_001));
    }

    int build(FlightPlanAction action, int px, int y, int popupWidth, int x, int width, int rowSpacing) {

        var targets = hosts();
        if (action.type() == ActionType.PROCESS) {
            var processing = action.settings().processing();
            var hostIds = new ArrayList<UUID>();
            hostIds.add(FlightPlanAction.NO_TARGET);
            targets.forEach(t -> hostIds.add(t.id()));
            button(x, y, width, hostName(processing.host()), hostIds.size() > 1, () -> {
                var id = next(hostIds, processing.host());
                var modules = RocketProcessingService.modules(rocket(id));
                set(action, action.settings().withProcessing(new Processing(modules.isEmpty() ? List.of() : List.of(modules.getFirst()), processing.scope(), id)));
            });
            y += rowSpacing;
            var modules = RocketProcessingService.modules(rocket(processing.host()));
            var selected = processing.modules().isEmpty() ? null : processing.modules().getFirst();
            button(x, y, width, module(selected, rocket(processing.host())), !modules.isEmpty(), () ->
                    set(action, action.settings().withProcessing(new Processing(List.of(next(modules, selected)), processing.scope(), processing.host()))));
            y += rowSpacing;
            button(x, y, width, text("parallel", processing.modules().size()), modules.size() > 1, () -> {
                var selection = processing.modules().size() == modules.size() ? List.of(modules.getFirst()) : modules;
                set(action, action.settings().withProcessing(new Processing(selection, processing.scope(), processing.host())));
            });
            y += rowSpacing;
            button(x, y, width, text("scope", text("scope_" + processing.scope().name().toLowerCase(Locale.ROOT))), true,
                    () -> set(action, action.settings().withProcessing(new Processing(processing.modules(), next(List.of(SourceScope.values()),
                            processing.scope()), processing.host())))).withTooltip(text("finite_pass"));
            y += rowSpacing;
            var estimate = editors.calculatedFlight().processingEstimates().stream().filter(result -> result.actionId().equals(action.id())).findFirst().orElse(null);
            Component cost;
            if (estimate == null) {
                cost = text("conditional");
            } else if (!estimate.issue().isEmpty()) {
                cost = Component.translatable("status.oritech_space_age." + estimate.issue());
            } else {
                var products = estimate.products().stream()
                        .map(id -> new ItemStack(BuiltInRegistries.ITEM.getValue(id)).getHoverName().getString())
                        .collect(Collectors.joining(", "));
                cost = text("cost", products, (int) Math.ceil(estimate.ticks() / 20), estimate.rf());
            }
            var details = new ArrayList<Component>();
            details.add(estimate != null && estimate.conditional() ? text("host_budget", estimate.remainingModuleCraftRF()) : text("local_budget"));
            if (estimate != null) {
                estimate.assignments().forEach(a -> details.add(text("assignment", a.machine().position().toShortString(), a.source().controller().position().toShortString(),
                        a.source().rocket().equals(editors.flightPlanRocket().getRocketId()) ? text("local") : text("station"))));
                estimate.skipped().forEach(issue -> details.add(Component.literal(issue)));
            }
            editors.addPopupComponent(new LabelWidget(px + 10, y, popupWidth - 20, cost).withBrightColor().withTooltip(details).withZIndex(9_001));
            y += rowSpacing;
            timeout(action, x, y, width);
            y += rowSpacing;
        } else if (action.type() == ActionType.DOCK) {
            var docking = action.settings().docking();
            var host = host(docking.host());
            var localPorts = RocketDocking.ports(editors.flightPlanRocket());
            // The previous navigation chooses the host; this card only configures attachment and exchange.
            editors.addPopupComponent(new LabelWidget(x, y, width, host == null ? text("choose_host") : Component.literal(host.name()))
                    .withBrightColor().withTooltip(Component.translatable("screen.oritech_space_age.action.dock_requires_arrival")).withZIndex(9_001));
            y += rowSpacing;
            button(x, y, width, text("local_port", docking.localPort().position().toShortString()), !localPorts.isEmpty() && host != null,
                    () -> set(action, action.settings().withDocking(host.rendezvous(next(localPorts, docking.localPort()), docking.hostPort()))));
            y += rowSpacing;
            button(x, y, width, text("host_port", docking.hostPort().position().toShortString()), host != null && !host.ports().isEmpty(),
                    () -> set(action, action.settings().withDocking(host.rendezvous(docking.localPort(), next(host.ports(), docking.hostPort())))));
            y += rowSpacing;
            editors.addPopupComponent(new LabelWidget(px + 10, y, popupWidth - 20, text("frozen",
                    docking.revision())).withBrightColor().withTooltip(text("frozen_help")).withZIndex(9_001));
            y += rowSpacing;
        } else if (action.type() == ActionType.UNDOCK) {
            editors.addPopupComponent(new LabelWidget(px + 10, y, popupWidth - 20, text("undock_help")).withBrightColor().withZIndex(9_001));
            y += rowSpacing;
        }
        if (action.type() == ActionType.DOCK || action.type() == ActionType.UNDOCK) {
            var exchange = action.settings().exchange();
            button(x, y, width / 2 - 2, text("cargo_exchange", text("direction_" + exchange.cargo().name().toLowerCase(Locale.ROOT))), true,
                    () -> set(action, action.settings().withExchange(new Exchange(next(List.of(Direction.values()), exchange.cargo()), exchange.local(),
                            exchange.remote(), exchange.filter(), exchange.fuel(), exchange.rf()))))
                    .withTooltip(text("cargo_exchange_help"));
            var filters = new ArrayList<Identifier>();
            filters.add(Identifier.parse("minecraft:air"));
            for (var rocket : Stream.concat(Stream.of(editors.flightPlanRocket()), targets.stream().map(DockingTarget::rocket)).toList())
                rocket.getStaticSegments().values().stream().flatMap(seg -> seg.blocks().stream()).filter(block -> !block.state().isAir())
                        .map(block -> BuiltInRegistries.BLOCK.getKey(block.state().getBlock())).distinct().forEach(id -> {
                            if (!filters.contains(id)) filters.add(id);
                        });
            button(x + width / 2 + 2, y, width / 2 - 2, text("filter", exchange.filter().getPath()), true,
                    () -> set(action, action.settings().withExchange(new Exchange(exchange.cargo(), exchange.local(), exchange.remote(), next(filters,
                            exchange.filter()), exchange.fuel(), exchange.rf()))));
            y += rowSpacing;
            var local = RocketLayout.controllers(editors.flightPlanRocket(), false);
            var host = exchangeHost(action);
            var remote = host == null ? List.<HardwareRef>of() : RocketLayout.controllers(host.rocket(), false);
            button(x, y, width / 2 - 2, text("cargo_local", selection(exchange.local(), editors.flightPlanRocket())), !local.isEmpty(), () -> set(action,
                    action.settings().withExchange(
                    new Exchange(exchange.cargo(), nextSelection(local, exchange.local()), exchange.remote(), exchange.filter(), exchange.fuel(), exchange.rf()))));
            var remoteRocket = host == null ? editors.flightPlanRocket() : host.rocket();
            button(x + width / 2 + 2, y, width / 2 - 2, text("cargo_remote", selection(exchange.remote(), remoteRocket)), !remote.isEmpty(), () -> set(action,
                    action.settings().withExchange(
                    new Exchange(exchange.cargo(), exchange.local(), nextSelection(remote, exchange.remote()), exchange.filter(), exchange.fuel(), exchange.rf()))));
            y += rowSpacing;
            resource(action, exchange, true, x, y, width);
            y += rowSpacing;
            resource(action, exchange, false, x, y, width);
            y += rowSpacing;
        }

        return y;
    }
}
