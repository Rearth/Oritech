package rearth.oritech.spaceage.block.assembler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

public class RocketAssemblerMenu extends AbstractContainerMenu {

    public final BlockPos blockPos;
    private @Nullable ActiveRocketData rocket;
    private boolean previewLoaded;
    private int previewRevision;
    private @Nullable SpaceSimulation.FlightPlannerSnapshot flightPlannerSnapshot;
    private int flightPlannerRevision;
    private @Nullable SpaceSimulation.FlightPlan draftFlightPlan;
    private @Nullable java.util.UUID draftRocketId;
    private boolean draftFlightPlanDirty;

    public RocketAssemblerMenu(int syncId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        super(SpaceAgeMenus.ROCKET_ASSEMBLER.get(), syncId);
        this.blockPos = buffer.readBlockPos();
    }

    public RocketAssemblerMenu(int syncId, Inventory inventory, RocketAssemblerBlockEntity assembler,
                               @Nullable ActiveRocketData rocket) {
        super(SpaceAgeMenus.ROCKET_ASSEMBLER.get(), syncId);
        this.blockPos = assembler.getBlockPos();
        this.rocket = rocket;
        this.previewLoaded = true;
    }

    public @Nullable ActiveRocketData getRocket() {
        return rocket;
    }

    public boolean isPreviewLoaded() {
        return previewLoaded;
    }

    public int getPreviewRevision() {
        return previewRevision;
    }

    public void setPreview(@Nullable ActiveRocketData rocket) {
        boolean changedRocket = this.rocket == null || rocket == null
                || !this.rocket.getRocketId().equals(rocket.getRocketId());
        this.rocket = rocket;
        this.previewLoaded = true;
        this.previewRevision++;
        if (changedRocket) {
            flightPlannerSnapshot = null;
            draftFlightPlan = null;
            draftRocketId = null;
            draftFlightPlanDirty = false;
            flightPlannerRevision++;
        }
    }

    public @Nullable SpaceSimulation.FlightPlannerSnapshot getFlightPlannerSnapshot() {
        return flightPlannerSnapshot;
    }

    public int getFlightPlannerRevision() {
        return flightPlannerRevision;
    }

    public void setFlightPlannerSnapshot(SpaceSimulation.FlightPlannerSnapshot snapshot) {
        this.flightPlannerSnapshot = snapshot;
        if (draftFlightPlan == null || !snapshot.rocketId().equals(draftRocketId) || !draftFlightPlanDirty) {
            draftFlightPlan = snapshot.plan();
            draftRocketId = snapshot.rocketId();
            draftFlightPlanDirty = false;
        }
        this.flightPlannerRevision++;
    }

    public @Nullable SpaceSimulation.FlightPlan getDraftFlightPlan() {
        return draftFlightPlan;
    }

    public void setDraftFlightPlan(SpaceSimulation.FlightPlan plan) {
        draftFlightPlan = plan;
        draftRocketId = rocket == null ? null : rocket.getRocketId();
        draftFlightPlanDirty = true;
    }

    public boolean isDraftFlightPlanDirty() {
        return draftFlightPlanDirty;
    }

    public void markDraftFlightPlanSaved() {
        draftFlightPlanDirty = false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(blockPos).is(SpaceAgeBlocks.ROCKET_ASSEMBLER)
                && player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0;
    }
}
