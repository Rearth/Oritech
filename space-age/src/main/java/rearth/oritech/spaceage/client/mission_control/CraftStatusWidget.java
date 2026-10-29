package rearth.oritech.spaceage.client.mission_control;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.data.EnergyDisplayWidget;
import rearth.oritech.api.screen.data.FluidDisplayWidget;
import rearth.oritech.init.FluidContent;
import rearth.oritech.spaceage.block.mission_control.MissionControlMenu;
import rearth.oritech.spaceage.client.starmap.StarMapLineRenderer;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.FlightMotion;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator;
import rearth.oritech.util.TooltipHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Logarithmic ribbons show current supply and demand around the battery pool.
 */
final class CraftStatusWidget extends UIComponent {

    private static final int SOLAR = 0xFFD29427;
    private static final int BEAM = 0xFF358FA6;
    private static final int LOAD = 0xFF8765B3;
    private final MissionControlMenu menu;
    private final long animationStart = System.nanoTime();
    private int revision = -1;
    private double propulsion;

    CraftStatusWidget(int x, int y, int width, int height, MissionControlMenu menu) {

        super(x, y, width, height);
        this.menu = menu;
    }

    static List<String> supportedCategories(ActiveRocketData rocket) {

        var result = new ArrayList<String>();
        var power = rocket.powerHardware();
        if (power.solar() > 0) result.add("solar");
        if (power.throughput() > 0) result.add("incoming");
        if (rocket.hardware().values().stream().anyMatch(hardware -> hardware.antennas() > 0)) result.add("radio");
        if (!rocket.processingModules().isEmpty()) result.add("processing");
        if (rocket.hardware().values().stream().anyMatch(hardware -> hardware.ion() > 0)) result.add("propulsion");
        if (power.throughput() > 0) result.add("outgoing");
        return result;
    }

    static double propulsionDemand(MissionState.Telemetry telemetry, List<CraftPath> paths) {

        var rocket = telemetry.rocket();
        if (telemetry.status().equals(MissionState.STATUS_MAINTAINING_POSITION)) {
            var performance = RocketPerformanceCalculator.calculate(rocket);
            var correction = SpaceBalance.stationKeepingBurnSecondsPerSecond(performance.thrustNewtons(), performance.wetMassKilograms());
            return rocket.hardware().values().stream().mapToDouble(hardware -> hardware.rfCost(correction) / 20).sum();
        }
        if (!telemetry.status().equals(MissionState.STATUS_IN_FLIGHT) && !telemetry.status().equals(MissionState.STATUS_TRANSFER))
            return 0;
        var path = paths.stream().filter(candidate -> candidate.branchId().equals(telemetry.plan().root().id())).findFirst().orElse(null);
        if (path == null || path.samples().isEmpty()) return 0;
        var time = telemetry.actionTicks() / 20d;
        if (time >= path.durationSeconds()) return 0;
        var sample = path.samples().get(FlightMotion.endIndex(path.samples(), time));
        if (sample.phase() == PathPhase.COAST || sample.phase() == PathPhase.PARACHUTE) return 0;
        return rocket.getStaticSegments().entrySet().stream()
                .filter(entry -> sample.firingSegments().contains(SegmentRef.of(entry.getValue())))
                .filter(entry -> rocket.getDynamicSegments().get(entry.getKey()).rf() > 0)
                .mapToDouble(entry -> rocket.hardware().get(entry.getKey()).rfCost(1) / 20).sum();
    }

    static float flowWidth(double rate) {

        return rate <= 0 ? 1.8f : (float) Math.min(32, 3 + 3.8 * Math.log10(1 + rate));
    }

    private static void curve(List<StarMapLineRenderer.Line> lines, int fromX, int fromY, int toX, int toY, double rate, int color, double pulse) {

        var previousX = (double) fromX;
        var previousY = (double) fromY;
        for (var step = 1; step <= 40; step++) {
            var t = step / 40d;
            var px = fromX + (toX - fromX) * t;
            var py = fromY + (toY - fromY) * t * t * (3 - 2 * t);
            var distance = t - .0125 - pulse;
            var strength = rate > 0 ? Math.exp(-distance * distance / .006) : 0;
            var lineWidth = flowWidth(rate) * (float) (1 + .18 * strength);
            lines.add(new StarMapLineRenderer.Line(previousX, previousY, px, py,
                    rate > 0 ? pulseColor(color, strength) : 0xFF999999, lineWidth, true));
            previousX = px;
            previousY = py;
        }
    }

    private static int pulseColor(int color, double strength) {

        // A soft highlight travels with the flow; idle links keep their static gray color.
        var highlight = .24 * strength;
        var red = (color >> 16) & 255;
        var green = (color >> 8) & 255;
        var blue = color & 255;
        return 0xFF000000 | ((int) (red + (255 - red) * highlight) << 16)
                | ((int) (green + (255 - green) * highlight) << 8) | (int) (blue + (255 - blue) * highlight);
    }

    private static void alignedText(GuiGraphicsExtractor graphics, Component value, int anchor, int y, int width, int color, boolean right) {

        var font = Minecraft.getInstance().font;
        var text = font.plainSubstrByWidth(value.getString(), Math.max(0, width));
        graphics.text(font, text, anchor - (right ? font.width(text) : font.width(text) / 2), y, color, false);
    }

    private static double fraction(double value, double capacity) {

        return capacity <= 0 ? 0 : Math.clamp(value / capacity, 0, 1);
    }

    private static int percent(double value, double capacity) {

        return (int) Math.round(fraction(value, capacity) * 100);
    }

    private static Component label(String key) {

        return Component.translatable("screen.oritech_space_age.craft." + key);
    }

    private static void text(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {

        var font = Minecraft.getInstance().font;
        graphics.text(font, font.plainSubstrByWidth(text.getString(), Math.max(0, width)), x, y, color, color == 0xFFFFFFFF);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

        setTooltip(List.of());
        var telemetry = menu.selectedTelemetry;
        if (telemetry == null) return;
        var rocket = telemetry.rocket();
        var power = telemetry.power().perWorldTick(menu.debugSpeed);
        if (revision != menu.telemetryRevision) {
            revision = menu.telemetryRevision;
            var entry = menu.fleet.stream().filter(craft -> craft.id().equals(menu.selected)).findFirst().orElse(null);
            propulsion = propulsionDemand(telemetry, entry == null ? List.of() : entry.navigation().paths()) * menu.debugSpeed;
        }
        var resources = rocket.getDynamicSegments().values();
        var charge = resources.stream().mapToDouble(r -> r.rf()).sum();
        var capacity = resources.stream().mapToDouble(r -> r.rfCapacity).sum();
        var fuel = resources.stream().mapToDouble(r -> r.fuel()).sum();
        var fuelCapacity = resources.stream().mapToDouble(r -> r.fuelCapacity).sum();
        var fuelWidth = 28;
        var fuelHeight = 100;
        var fuelX = x + width - fuelWidth - 8;
        var fuelY = y + height - fuelHeight - 8;
        FluidDisplayWidget.renderGauge(graphics, fuelX, fuelY, fuelWidth, fuelHeight,
                (float) fraction(fuel, fuelCapacity), new FluidStack(FluidContent.STILL_FUEL.get(), 1));
        var graphTop = y + 24;
        var graphBottom = fuelY - 24;
        var graphHeight = Math.max(40, graphBottom - graphTop);
        var center = x + width / 2;
        var barWidth = Math.clamp(width / 10, 18, 42);
        var barLeft = center - barWidth / 2;
        var barRight = barLeft + barWidth;
        var nodeWidth = Math.max(45, width / 4);
        var rightX = x + width - nodeWidth - 8;
        var rates = Map.of("solar", power.solar(), "incoming", power.incoming(),
                "radio", rocket.hardware().values().stream().mapToDouble(h -> h.antennas() * SpaceBalance.ANTENNA_RF).sum() * menu.debugSpeed,
                "processing", resources.stream().flatMap(r -> r.crafters.values().stream()).filter(c -> c.job != null)
                        .mapToDouble(c -> c.job.rfPerTick()).sum() * menu.debugSpeed,
                "propulsion", propulsion, "outgoing", power.sent());
        var lines = new ArrayList<StarMapLineRenderer.Line>();
        var pulse = ((System.nanoTime() - animationStart) / 2_800_000_000d) % 1;
        var supported = supportedCategories(rocket);
        var inputs = List.of("solar", "incoming").stream().filter(supported::contains).toList();
        var outputs = List.of("radio", "processing", "propulsion", "outgoing").stream().filter(supported::contains).toList();
        for (var index = 0; index < inputs.size(); index++) {
            var name = inputs.get(index);
            var rate = rates.get(name);
            var cy = graphTop + graphHeight * (index * 2 + 1) / (inputs.size() * 2);
            var color = name.equals("solar") ? SOLAR : BEAM;
            curve(lines, x + nodeWidth + 8, cy, barLeft - 2, graphTop + graphHeight * (index + 1) / (inputs.size() + 1), rate, color, pulse);
            node(graphics, x + 8, cy, nodeWidth, name, rate, true, mouseX, mouseY);
        }
        for (var index = 0; index < outputs.size(); index++) {
            var name = outputs.get(index);
            var rate = rates.get(name);
            var cy = graphTop + graphHeight * (index * 2 + 1) / (outputs.size() * 2);
            curve(lines, barRight + 2, graphTop + graphHeight * (index + 1) / (outputs.size() + 1), rightX, cy, rate, LOAD, pulse);
            node(graphics, rightX, cy, nodeWidth, name, rate, false, mouseX, mouseY);
        }
        if (!inputs.isEmpty())
            alignedText(graphics, label("supply"), x + nodeWidth + 8, graphTop - 16, nodeWidth, 0xFF606060, true);
        if (!outputs.isEmpty()) text(graphics, label("demand"), rightX, graphTop - 16, nodeWidth, 0xFF606060);
        StarMapLineRenderer.submitConnected(graphics, lines, x, y, width, height);
        OritechSurface.PANEL_INSET.render(graphics, barLeft - 2, graphTop - 2, barWidth + 4, graphHeight + 4);
        EnergyDisplayWidget.renderBar(graphics, barLeft, graphTop, barWidth, graphHeight, (float) fraction(charge, capacity));
        text(graphics, Component.literal("RF"), barLeft, graphTop - 16, barWidth, 0xFF404040);
        if (mouseX >= barLeft && mouseX <= barRight && mouseY >= graphTop && mouseY <= graphBottom) {
            var tooltip = List.<Component>of(Component.translatable("screen.oritech_space_age.power.stored",
                            TooltipHelper.getEnergyText((long) charge), TooltipHelper.getEnergyText((long) capacity), percent(charge, capacity)),
                    Component.translatable(Math.abs(power.net()) < .001 ? "screen.oritech_space_age.power.steady"
                                    : power.net() > 0 ? "screen.oritech_space_age.power.charging" : "screen.oritech_space_age.power.draining",
                            TooltipHelper.getEnergyText((long) Math.abs(power.net()))));
            setTooltip(tooltip);
        }
        if (mouseX >= fuelX && mouseX < fuelX + fuelWidth && mouseY >= fuelY && mouseY < fuelY + fuelHeight)
            setTooltip(List.of(Component.translatable("screen.oritech_space_age.craft.fuel", percent(fuel, fuelCapacity)), label("fuel_help"),
                    Component.literal(String.format(Locale.ROOT, "%,.0f / %,.0f", fuel, fuelCapacity))));
    }

    private void node(GuiGraphicsExtractor graphics, int nx, int cy, int nodeWidth, String name, double rate,
                      boolean input, int mouseX, int mouseY) {

        if (mouseX >= nx && mouseX < nx + nodeWidth && mouseY >= cy - 12 && mouseY < cy + 12)
            setTooltip(flowTooltip(name, rate));
        var iconX = input ? nx + nodeWidth - CraftFlowIcons.SIZE : nx;
        var textOffset = CraftFlowIcons.SIZE + 5;
        CraftFlowIcons.draw(graphics, name, iconX, cy - CraftFlowIcons.SIZE / 2);
        var value = Component.literal(TooltipHelper.getEnergyText((long) rate) + "/t");
        if (input) {
            alignedText(graphics, label(name), iconX - 5, cy - 9, nodeWidth - textOffset, 0xFF404040, true);
            alignedText(graphics, value, iconX - 5, cy + 2, nodeWidth - textOffset, 0xFF606060, true);
        } else {
            text(graphics, label(name), nx + textOffset, cy - 9, nodeWidth - textOffset, 0xFF404040);
            text(graphics, value, nx + textOffset, cy + 2, nodeWidth - textOffset, 0xFF606060);
        }
    }

    private List<Component> flowTooltip(String name, double rate) {

        var telemetry = menu.selectedTelemetry;
        var power = telemetry.power().perWorldTick(menu.debugSpeed);
        var details = new ArrayList<Component>();
        details.add(label(name));
        details.add(Component.literal(String.format(Locale.ROOT, "%,.2f RF/t", rate)));
        if (name.equals("solar")) {
            details.add(Component.translatable("screen.oritech_space_age.power.solar_rating",
                    TooltipHelper.getEnergyText((long) (telemetry.rocket().powerHardware().solar() * menu.debugSpeed))));
            var curtailed = power.solar() + power.incoming() - power.services() - power.sent() - power.net();
            if (curtailed > .001)
                details.add(Component.translatable("screen.oritech_space_age.power.curtailed", TooltipHelper.getEnergyText((long) curtailed)));
        } else if (name.equals("incoming") || name.equals("outgoing")) {
            var receiving = name.equals("incoming");
            var beam = receiving ? power.receiving() : power.outgoing();
            if (beam.incident() > 0) {
                details.add(Component.translatable("screen.oritech_space_age.power.beam_collection",
                        String.format(Locale.ROOT, "%.1f", beam.coverage() * 100), TooltipHelper.getEnergyText((long) beam.accepted())));
                details.add(Component.translatable("screen.oritech_space_age.power.beam_losses", TooltipHelper.getEnergyText((long) beam.missed()),
                        TooltipHelper.getEnergyText((long) beam.capacityLoss()), TooltipHelper.getEnergyText((long) beam.storageLoss())));
            }
            if (receiving && power.incoming() > 0 && power.net() < 0)
                details.add(Component.translatable("screen.oritech_space_age.beam.relay_draining"));
            if (!receiving) {
                var action = telemetry.plan().currentAction();
                if (action != null && action.type() == ActionType.BEAM_POWER) {
                    var reserve = action.settings().beam().reservePercent();
                    var capacity = telemetry.rocket().getDynamicSegments().values().stream().mapToDouble(r -> r.rfCapacity).sum();
                    details.add(Component.translatable("screen.oritech_space_age.power.reserve", reserve,
                            TooltipHelper.getEnergyText((long) (capacity * reserve / 100d))));
                }
                if (power.overflow() > .001 && !power.reason().isEmpty())
                    details.add(Component.translatable(power.reason()));
            }
        } else if (name.equals("propulsion")) {
            details.add(label("propulsion_help"));
            if (power.solar() == 0) details.add(Component.translatable("screen.oritech_space_age.power.fuel_upkeep"));
        } else {
            var radioDemand = telemetry.rocket().hardware().values().stream()
                    .mapToDouble(h -> h.antennas() * SpaceBalance.ANTENNA_RF * menu.debugSpeed).sum();
            var used = name.equals("radio") ? Math.min(radioDemand, power.services()) : Math.max(0, power.services() - radioDemand);
            details.add(Component.translatable("screen.oritech_space_age.craft.service_used", TooltipHelper.getEnergyText((long) used)));
            if (name.equals("processing"))
                telemetry.rocket().getDynamicSegments().values().forEach(r -> r.crafters.forEach((pos, machine) -> {
                    if (machine.job != null)
                        details.add(Component.translatable("screen.oritech_space_age.pair.job_details",
                                machine.name.isBlank() ? pos.toShortString() : machine.name,
                                machine.job.source().controller().position().toShortString(), machine.job.elapsed() * 100 / machine.job.duration()));
                }));
        }
        if (menu.debugSpeed > 1)
            details.add(Component.translatable("screen.oritech_space_age.power.world_tick_rates", menu.debugSpeed));
        return details;
    }
}
