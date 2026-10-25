package rearth.oritech.spaceage.block.launch_ramp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.block.cubesat.CubeSatBlock;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampBlockEntity.Outcome;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.spaceage.network.RocketNetworking;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.LaunchSpeedRules;
import rearth.oritech.spaceage.simulation.mission.MissionState;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Holds the pattern slot and shares the assembler's plan draft, so the ramp can reuse both assembler screens.
 * Previews start at the ramp with the expected particle speed.
 */
public class LaunchRampMenu extends RocketAssemblerMenu {

    // Slot positions in panel coordinates of the ramp tab.
    public static final int PATTERN_X = 21;
    public static final int PATTERN_Y = 62;
    public static final int INVENTORY_X = 21;
    public static final int INVENTORY_Y = 112;

    private final @Nullable LaunchRampBlockEntity ramp;
    private final Player player;
    private long sentPattern = -1;
    private @Nullable Status sentStatus;
    // Client state: synced ramp status, the expected particle speed (-1 uses the lowest working speed) and the speed window.
    public Status status = new Status(0, Outcome.NONE.ordinal(), "", false);
    public int statusRevision;
    public long expectedSpeed = -1;
    // Only the ramp tab shows the slots; the full-screen rocket and plan tabs hide them.
    public boolean slotsVisible = true;
    public @Nullable LaunchSpeedRules.Window speedWindow;
    public boolean speedWindowKnown;
    public int speedWindowRevision;
    public @Nullable SpaceSimulation.FlightPlan speedWindowPlan;
    public @Nullable CompletableFuture<LaunchSpeedRules.Window> speedWindowSearch;
    public int speedWindowDelay;
    public int speedWindowPreviewRevision = -1;

    public LaunchRampMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {

        this(id, inventory, buffer.readBlockPos(), new SimpleContainer(1), null);
    }

    public LaunchRampMenu(int id, Inventory inventory, LaunchRampBlockEntity ramp) {

        this(id, inventory, ramp.getBlockPos(), ramp.pattern, ramp);
    }

    private LaunchRampMenu(int id, Inventory inventory, BlockPos pos, Container pattern, @Nullable LaunchRampBlockEntity ramp) {

        super(SpaceAgeMenus.LAUNCH_RAMP.get(), id, pos);
        this.ramp = ramp;
        this.player = inventory.player;
        addSlot(new RampSlot(pattern, 0, PATTERN_X, PATTERN_Y) {

            @Override
            public boolean mayPlace(ItemStack stack) {

                return CubeSatBlock.data(stack) != null;
            }

            @Override
            public int getMaxStackSize() {

                return 1;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new RampSlot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new RampSlot(inventory, column, INVENTORY_X + column * 18, INVENTORY_Y + 58));
    }

    public ItemStack pattern() {

        return slots.getFirst().getItem();
    }

    /** Particle speed used for the planner preview. */
    public long previewSpeed() {

        return expectedSpeed >= 0 ? expectedSpeed : speedWindow == null ? 0 : speedWindow.min();
    }

    @Override
    public @Nullable MissionState.Position launchPosition(List<SpaceSimulation.SpaceObjectData> objects) {

        var rocket = getRocket();
        return rocket == null ? null : LaunchSpeedRules.position(objects, rocket.getLaunchPosition(), previewSpeed());
    }

    /**
     * Sends the pattern's rocket and plan again whenever the pattern changes, and the ramp status when it changes.
     */
    @Override
    public void broadcastChanges() {

        super.broadcastChanges();
        if (ramp == null || !(player instanceof ServerPlayer serverPlayer)) return;
        var data = CubeSatBlock.data(pattern());
        var fingerprint = data == null ? 0 : data.fingerprint();
        if (fingerprint != sentPattern) {
            sentPattern = fingerprint;
            setPreview(ramp.createPreview());
            RocketNetworking.sendAssemblerPreview(serverPlayer, blockPos, getRocket());
            if (getRocket() != null) RocketNetworking.sendFlightPlanner(serverPlayer, this);
        }
        var current = new Status(ramp.lastSpeed(), ramp.lastOutcome().ordinal(), ramp.ownerName(), ramp.planForPattern());
        if (!current.equals(sentStatus)) {
            sentStatus = current;
            PacketDistributor.sendToPlayer(serverPlayer, new RocketNetworking.RampStatusPayload(containerId, current));
        }
    }

    public void receiveStatus(Status status) {

        this.status = Objects.requireNonNull(status);
        statusRevision++;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {

        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var original = stack.copy();
        var moved = index == 0 ? moveItemStackTo(stack, 1, slots.size(), true) : moveItemStackTo(stack, 0, 1, false);
        if (!moved) return ItemStack.EMPTY;
        slot.setChanged();
        return original;
    }

    @Override
    public boolean canLaunch() {

        return false;
    }

    @Override
    public void saveFlightPlan(ServerPlayer player, SpaceSimulation.FlightPlan plan) {

        var rocket = getRocket();
        if (ramp != null && rocket != null) ramp.savePlan(player, rocket.getRocketId(), plan);
    }

    @Override
    public boolean stillValid(Player player) {

        return player.level().getBlockState(blockPos).is(SpaceAgeBlocks.LAUNCH_RAMP)
                && player.distanceToSqr(blockPos.getX() + .5, blockPos.getY() + .5, blockPos.getZ() + .5) <= 64;
    }

    /**
     * @param outcome     ordinal of {@link Outcome}
     * @param owner       name of the player who saved the plan, empty without one
     * @param planMatches whether the saved plan was made for the current pattern
     */
    public record Status(long lastSpeed, int outcome, String owner, boolean planMatches) {

    }

    private class RampSlot extends Slot {

        RampSlot(Container container, int index, int x, int y) {

            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {

            return slotsVisible;
        }
    }
}
