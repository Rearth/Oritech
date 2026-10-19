package rearth.oritech.spaceage.robot;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.spaceage.robot.RobotProgram.StepSignal;

import java.util.ArrayList;
import java.util.List;

public final class RobotMenu extends AbstractContainerMenu {

    public final RobotArmBlockEntity arm;
    private final Player player;
    private final InteractionHand hand;
    private final ItemStack card;
    public RobotProgram program = RobotProgram.EMPTY;
    public int cursor;
    public boolean running;
    public boolean repeat;
    public boolean skip;
    public int redstone;
    public long energy;
    public String status = "idle";
    public int revision;
    private RobotNetworking.Snapshot lastSent;

    public RobotMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {

        this(id, inventory, buffer.readBoolean(), buffer);
    }

    private RobotMenu(int id, Inventory inventory, boolean machine, RegistryFriendlyByteBuf buffer) {

        this(id, inventory, machine ? (RobotArmBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()) : null,
                machine ? InteractionHand.MAIN_HAND : buffer.readEnum(InteractionHand.class));
    }

    public RobotMenu(int id, Inventory inventory, RobotArmBlockEntity arm) {

        this(id, inventory, arm, InteractionHand.MAIN_HAND);
    }

    public RobotMenu(int id, Inventory inventory, InteractionHand hand) {

        this(id, inventory, null, hand);
    }

    private RobotMenu(int id, Inventory inventory, RobotArmBlockEntity arm, InteractionHand hand) {

        super(SpaceAgeMenus.ROBOT.get(), id);
        this.arm = arm;
        this.player = inventory.player;
        this.hand = hand;
        this.card = arm == null ? player.getItemInHand(hand) : ItemStack.EMPTY;
        if (arm == null) {
            program = card.getOrDefault(SpaceAgeComponents.ROBOT_PROGRAM.get(), RobotProgram.EMPTY);
            return;
        }
        addSlot(new ResourceHandlerSlot(arm.inventory, arm.inventory::set, 0, 18, 181) {
            @Override
            public boolean mayPlace(ItemStack stack) {

                return stack.getItem() instanceof ActionCardItem;
            }
        });
        for (int i = 0; i < 18; i++) addSlot(new ResourceHandlerSlot(arm.inventory, arm.inventory::set, i + 1,
                18 + i % 9 * 18, 205 + i / 9 * 18));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 216 + col * 18, 181 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 216 + col * 18, 239));
    }

    @Override
    public boolean stillValid(Player player) {

        return arm == null ? player.getItemInHand(hand) == card && card.getItem() instanceof ActionCardItem
                : !arm.isRemoved() && player.level().getBlockEntity(arm.getBlockPos()) == arm
                && player.distanceToSqr(arm.getBlockPos().getCenter()) <= 64;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {

        if (!stillValid(player)) return false;
        var data = arm == null ? card.getOrDefault(SpaceAgeComponents.ROBOT_PROGRAM.get(), RobotProgram.EMPTY) : arm.program;
        if (button == 0 && arm != null) arm.loadCard();
        else if (button == 1 && arm != null) arm.running = !arm.running;
        else if (button == 2 && arm != null) arm.restart();
        else if (button == 3 && arm != null) arm.repeat = !arm.repeat;
        else if (button == 4 && arm != null) arm.skipBlocked = !arm.skipBlocked;
        else if (button == 5 && arm != null) arm.redstoneMode = (arm.redstoneMode + 1) % 4;
        else if (button == 6 && arm == null) card.set(SpaceAgeComponents.ROBOT_PROGRAM.get(), data.withActions(List.of()));
        else if (button >= 1000 && button < 1000 + RobotProgram.LIMIT && arm == null) {
            var index = button - 1000;
            if (index >= data.actions().size()) return false;
            var actions = new ArrayList<>(data.actions());
            actions.remove(index);
            card.set(SpaceAgeComponents.ROBOT_PROGRAM.get(), data.withActions(actions));
        } else if (button >= 2000 && button < 2000 + RobotProgram.LIMIT && arm != null) {
            var index = button - 2000;
            if (index >= data.actions().size() || arm.running) return false;
            var actions = new ArrayList<>(data.actions());
            var action = actions.get(index);
            actions.set(index, action.withSignal(StepSignal.values()[(action.signal().ordinal() + 1) % 3]));
            arm.program = data.withActions(actions);
        } else return false;
        if (arm != null) arm.setChanged();
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {

        super.broadcastChanges();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        var snapshot = arm == null
                ? new RobotNetworking.Snapshot(containerId, card.getOrDefault(SpaceAgeComponents.ROBOT_PROGRAM.get(), RobotProgram.EMPTY), 0, false, false, false, 0, 0, "ready")
                : new RobotNetworking.Snapshot(containerId, arm.program, arm.cursor, arm.running, arm.repeat, arm.skipBlocked,
                arm.redstoneMode, arm.energyStorage.getAmountAsLong(), arm.status);
        if (!snapshot.equals(lastSent)) {
            lastSent = snapshot;
            PacketDistributor.sendToPlayer(serverPlayer, snapshot);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {

        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        var stack = slot.getItem();
        var original = stack.copy();
        if (index < 19) {
            if (!moveItemStackTo(stack, 19, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof ActionCardItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 19, false)) return ItemStack.EMPTY;
        slot.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack);
        return original;
    }
}
