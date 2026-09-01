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
        this.rocket = rocket;
        this.previewLoaded = true;
        this.previewRevision++;
    }

    public @Nullable SpaceSimulation.FlightPlannerSnapshot getFlightPlannerSnapshot() {
        return flightPlannerSnapshot;
    }

    public int getFlightPlannerRevision() {
        return flightPlannerRevision;
    }

    public void setFlightPlannerSnapshot(SpaceSimulation.FlightPlannerSnapshot snapshot) {
        this.flightPlannerSnapshot = snapshot;
        this.flightPlannerRevision++;
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
