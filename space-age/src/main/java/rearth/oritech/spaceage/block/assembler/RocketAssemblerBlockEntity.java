package rearth.oritech.spaceage.block.assembler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulationSavedData;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.mission.MissionController;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.mission.RocketDocking;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator;
import rearth.oritech.spaceage.simulation.rocket.RocketStructureScan;

import java.util.List;

public class RocketAssemblerBlockEntity extends BlockEntity implements MenuProvider {

    private String scanIssue = "";

    public RocketAssemblerBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.ROCKET_ASSEMBLER.get(), pos, state);
    }

    public String scanIssue() {

        return scanIssue;
    }

    public boolean assemble(ServerPlayer player, SpaceSimulation.FlightPlan plan) {

        OritechSpaceAge.LOGGER.debug("Starting assembling process");

        if (!(level instanceof ServerLevel serverLevel) || level.dimension() != Level.OVERWORLD)
            return false;

        // Launch readiness must be checked before blocks are removed. Scans are side-effect free.
        var scanned = scan(serverLevel);
        var preview = scanned.rocket();
        if (preview == null) {
            if (!scanIssue.isEmpty()) player.sendOverlayMessage(Component.translatable(scanIssue));
            return false;
        }
        var readiness = RocketPerformanceCalculator.getLaunchReadiness(preview, plan);
        if (readiness != RocketPerformanceCalculator.LaunchReadiness.READY) {
            OritechSpaceAge.LOGGER.warn("Rocket launch at {} failed: {}", worldPosition, readiness.failureReason());
            return false;
        }

        var system = SpaceSimulationSavedData.getForPlayer(player);
        var recipes = VacuumRecipe.all(player.level().getServer());
        var stations = RocketDocking.targets(MissionSavedData.get(player.level().getServer()), player.getUUID());
        var validation = RocketFlightPlanRules.inspect(plan, preview, system.createObjectData(), true, null, recipes, stations);
        if (!validation.valid()) {
            player.sendOverlayMessage(validation.issues().getFirst().description());
            return false;
        }
        var validated = validation.plan();
        if (validated == null || validated.root().actions().isEmpty()
                || validated.branches().stream().mapToInt(b -> b.actions().size()).sum() != plan.branches().stream().mapToInt(b -> b.actions().size()).sum())
            return false;
        validated = MissionController.resolveOrbitSlots(validated, MissionSavedData.get(player.level().getServer()), player.getUUID(), preview.getRocketId());
        var first = validated.root().actions().getFirst();
        if (first.type() != SpaceSimulation.ActionType.NAVIGATE_TO)
            return false;
        var firstPlan = validated.withBranches(List.of(validated.root().withActions(List.of(first))));
        var predicted = RocketFlightPathCalculator.calculateFrom(preview, system.createObjectData(), firstPlan, null, recipes, stations);
        if (predicted.paths().isEmpty() || predicted.paths().getFirst().terminalState().preventsLaunch()) return false;
        RocketStructureScan.removeBlocks(serverLevel, scanned);
        MissionController.launch(serverLevel, player.getUUID(), preview, validated, preview.getLaunchPosition());
        return true;
    }

    public ActiveRocketData createPreview() {

        return level instanceof ServerLevel serverLevel ? scan(serverLevel).rocket() : null;
    }

    private RocketStructureScan.Result scan(ServerLevel level) {

        var pad = worldPosition.relative(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));
        var result = RocketStructureScan.scan(level, pad, false);
        scanIssue = result.issue();
        return result;
    }

    @Override
    public Component getDisplayName() {

        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {

        return new RocketAssemblerMenu(syncId, playerInventory, this, createPreview());
    }

}
