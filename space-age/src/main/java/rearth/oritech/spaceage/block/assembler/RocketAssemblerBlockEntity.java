package rearth.oritech.spaceage.block.assembler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
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
import java.util.UUID;

public class RocketAssemblerBlockEntity extends BlockEntity implements MenuProvider {

    private String scanIssue = "";
    // The plan of a redstone launch comes from this player's simulation, as the last one to save or launch a plan here.
    private @Nullable UUID owner;
    private boolean powered;

    public RocketAssemblerBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.ROCKET_ASSEMBLER.get(), pos, state);
    }

    public String scanIssue() {

        return scanIssue;
    }

    public void setOwner(ServerPlayer player) {

        if (player.getUUID().equals(owner)) return;
        owner = player.getUUID();
        setChanged();
    }

    /** A rising redstone edge launches the rocket on the pad with the plan stored for this assembler. */
    public void updateRedstone(boolean signal) {

        if (signal == powered) return;
        powered = signal;
        setChanged();
        if (!signal || owner == null || !(level instanceof ServerLevel serverLevel)) return;
        var plan = SpaceSimulationSavedData.get(serverLevel.getServer()).getOrCreate(owner)
                .flightPlan(GlobalPos.of(serverLevel.dimension(), worldPosition));
        if (plan.root().actions().isEmpty()) return;
        assemble(owner, null, plan);
    }

    public boolean assemble(ServerPlayer player, SpaceSimulation.FlightPlan plan) {

        setOwner(player);
        return assemble(player.getUUID(), player, plan);
    }

    /**
     * @param player receives failure messages, if anyone is there to read them
     */
    private boolean assemble(UUID owner, @Nullable ServerPlayer player, SpaceSimulation.FlightPlan plan) {

        OritechSpaceAge.LOGGER.debug("Starting assembling process");

        if (!(level instanceof ServerLevel serverLevel) || level.dimension() != Level.OVERWORLD)
            return false;
        var server = serverLevel.getServer();

        // Launch readiness must be checked before blocks are removed. Scans are side-effect free.
        var scanned = scan(serverLevel);
        var preview = scanned.rocket();
        if (preview == null) {
            if (player != null && !scanIssue.isEmpty()) player.sendOverlayMessage(Component.translatable(scanIssue));
            return false;
        }
        var readiness = RocketPerformanceCalculator.getLaunchReadiness(preview, plan);
        if (readiness != RocketPerformanceCalculator.LaunchReadiness.READY) {
            OritechSpaceAge.LOGGER.warn("Rocket launch at {} failed: {}", worldPosition, readiness.failureReason());
            return false;
        }

        var system = SpaceSimulationSavedData.get(server).getOrCreate(owner);
        var recipes = VacuumRecipe.all(server);
        var stations = RocketDocking.targets(MissionSavedData.get(server), owner);
        var validation = RocketFlightPlanRules.inspect(plan, preview, system.createObjectData(), true, null, recipes, stations);
        if (!validation.valid()) {
            if (player != null) player.sendOverlayMessage(validation.issues().getFirst().description());
            return false;
        }
        var validated = validation.plan();
        if (validated == null || validated.root().actions().isEmpty()
                || validated.branches().stream().mapToInt(b -> b.actions().size()).sum() != plan.branches().stream().mapToInt(b -> b.actions().size()).sum())
            return false;
        validated = MissionController.resolveOrbitSlots(validated, MissionSavedData.get(server), owner, preview.getRocketId());
        var first = validated.root().actions().getFirst();
        if (first.type() != SpaceSimulation.ActionType.NAVIGATE_TO)
            return false;
        var firstPlan = validated.withBranches(List.of(validated.root().withActions(List.of(first))));
        var predicted = RocketFlightPathCalculator.calculateFrom(preview, system.createObjectData(), firstPlan, null, recipes, stations);
        if (predicted.paths().isEmpty() || predicted.paths().getFirst().terminalState().preventsLaunch()) return false;
        RocketStructureScan.removeBlocks(serverLevel, scanned);
        MissionController.launch(serverLevel, owner, preview, validated, preview.getLaunchPosition());
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
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
        output.putBoolean("powered", powered);
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        powered = input.getBooleanOr("powered", false);
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
