package rearth.oritech.spaceage.block.cubesat;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Vec3i;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import rearth.oritech.spaceage.SpaceAgeConfig;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.ParachuteLanding;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.CubeSatData;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformance;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator;
import rearth.oritech.util.TooltipHelper;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Summarizes the contained rocket, so players can see what a cube can do without unfolding it.
 */
public class CubeSatItem extends BlockItem {

    private Summary summary;

    public CubeSatItem(Block block, Properties properties) {

        super(block, properties);
    }

    private static boolean has(ActiveRocketData rocket, Block block) {

        return rocket.getStaticSegments().values().stream().anyMatch(segment -> segment.blocks().stream().anyMatch(cell -> cell.state().is(block)));
    }

    private static String capability(String key) {

        return Component.translatable("tooltip.oritech_space_age.cube_sat.capability." + key).getString();
    }

    private static String number(double value) {

        return String.format(Locale.ROOT, value >= 100 ? "%,.0f" : "%.1f", value);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {

        super.appendHoverText(stack, context, display, tooltip, flag);
        var data = stack.get(SpaceAgeComponents.CUBE_SAT.get());
        if (data == null) {
            tooltip.accept(Component.translatable("tooltip.oritech_space_age.cube_sat.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (summary == null || !summary.data().equals(data)) {
            var rocket = data.unfold(UUID.randomUUID(), null);
            summary = new Summary(data, rocket, RocketPerformanceCalculator.calculate(rocket), data.size(), data.blockCount());
        }
        var rocket = summary.rocket();
        var performance = summary.performance();
        var size = summary.size();
        tooltip.accept(Component.translatable("tooltip.oritech_space_age.cube_sat.mass", SpaceAgeConfig.cubeSatMass(),
                number(performance.wetMassKilograms() / 1000)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.oritech_space_age.cube_sat.delta_v",
                number(performance.availableDeltaVMetersPerSecond())).withStyle(ChatFormatting.GRAY));
        var selfLaunch = RocketPerformanceCalculator.getLaunchReadiness(performance) == RocketPerformanceCalculator.LaunchReadiness.READY;
        tooltip.accept(Component.translatable("tooltip.oritech_space_age.cube_sat.thrust_to_weight",
                number(performance.liftoffAccelerationMetersPerSecondSquared() / SpaceBalance.STANDARD_GRAVITY)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(selfLaunch ? "tooltip.oritech_space_age.cube_sat.self_launch"
                : "tooltip.oritech_space_age.cube_sat.needs_launcher").withStyle(selfLaunch ? ChatFormatting.GREEN : ChatFormatting.GOLD));

        var capabilities = new ArrayList<String>();
        var segments = rocket.getStaticSegments().values();
        if (segments.stream().anyMatch(segment -> RocketHardware.of(segment).antennas() > 0))
            capabilities.add(capability("radio"));
        if (segments.stream().anyMatch(segment -> RocketHardware.of(segment).scanners() > 0))
            capabilities.add(capability("survey"));
        if (rocket.powerHardware().solar() > 0) capabilities.add(capability("solar"));
        if (segments.stream().anyMatch(segment -> segment.payloadEnergyJoules() > 0))
            capabilities.add(capability("explosive"));
        if (segments.stream().anyMatch(segment -> ParachuteLanding.count(segment) > 0))
            capabilities.add(capability("parachute"));
        if (has(rocket, SpaceAgeBlocks.CARGO.get())) capabilities.add(capability("cargo"));
        var computers = segments.stream().mapToLong(segment -> segment.blocks().stream()
                .filter(block -> block.state().is(SpaceAgeBlocks.NAVIGATION_COMPUTER)).count()).sum();
        if (computers > 0)
            capabilities.add(Component.translatable("tooltip.oritech_space_age.cube_sat.capability.computers", computers).getString());
        if (!capabilities.isEmpty())
            tooltip.accept(Component.translatable("tooltip.oritech_space_age.cube_sat.capabilities", String.join(", ", capabilities)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.oritech_space_age.cube_sat.size", size.getX(), size.getY(), size.getZ(),
                summary.blocks()).withStyle(ChatFormatting.DARK_GRAY));

        if (!Minecraft.getInstance().hasShiftDown()) {
            tooltip.accept(Component.translatable("tooltip.oritech_space_age.cube_sat.shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        for (var ref : SpaceSimulation.SegmentRef.displayOrder(rocket)) {
            var id = rocket.getStaticSegments().entrySet().stream().filter(entry -> SpaceSimulation.SegmentRef.of(entry.getValue()).equals(ref))
                    .map(Map.Entry::getKey).findFirst().orElseThrow();
            var hardware = RocketHardware.of(rocket.getStaticSegments().get(id));
            var resources = rocket.getDynamicSegments().get(id);
            tooltip.accept(Component.translatable("tooltip.oritech_space_age.cube_sat.segment", SpaceSimulation.SegmentRef.defaultName(ref, rocket),
                    hardware.chemical(), hardware.ion(), resources.availableFuelBurnTimeTicks,
                    TooltipHelper.getEnergyText(resources.availableRF)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private record Summary(CubeSatData data, ActiveRocketData rocket, RocketPerformance performance, Vec3i size,
                           int blocks) {

    }
}
