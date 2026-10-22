package rearth.oritech.spaceage.block.robot_arm;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import rearth.oritech.client.ui.UpgradableOritechScreenHandler;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram.Match;
import rearth.oritech.spaceage.network.RobotNetworking;

import java.util.Objects;

public final class RobotMenu extends UpgradableOritechScreenHandler implements RobotNetworking.Receiver {

    public static final int LOAD_BUTTON = 0;
    public static final int RUN_BUTTON = 1;
    public static final int RESTART_BUTTON = 2;
    public static final int REPEAT_BUTTON = 3;
    public static final int SKIP_BUTTON = 4;
    public static final int REDSTONE_BUTTON = 5;
    public static final int MATCH_BUTTON = 1000;

    public final RobotArmBlockEntity arm;
    public RobotProgram program = RobotProgram.EMPTY;
    public int cursor;
    public boolean running;
    public boolean repeat;
    public boolean skip;
    public int redstone;
    public String status = "idle";
    public int revision;
    private RobotNetworking.Snapshot lastSent;

    public RobotMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {

        this(id, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buffer.readBlockPos())));
    }

    public RobotMenu(int id, Inventory inventory, BlockEntity blockEntity) {

        super(id, inventory, blockEntity);
        arm = (RobotArmBlockEntity) blockEntity;
    }

    /** Encodes a match mode change for an action, shared by the arm and card menus. */
    public static int matchButton(int index, Match match) {

        return MATCH_BUTTON + index * Match.values().length + match.ordinal();
    }

    @Override
    public void addMachineSlot(int inventorySlot, int x, int y, boolean output) {

        if (inventorySlot != 0) {
            super.addMachineSlot(inventorySlot, x, y, output);
            return;
        }
        addSlot(new ResourceHandlerSlot(inventory, inventory::set, inventorySlot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {

                return stack.getItem() instanceof ActionCardItem;
            }
        });
    }

    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startX, int startY) {

        // The robot panel is wider than the standard machine background; the inventory sits below the playbook.
        super.addPlayerInventory(playerInventory, 47, 137);
    }

    @Override
    public boolean stillValid(Player player) {

        return super.stillValid(player) && player.level().getBlockEntity(blockPos) == arm;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {

        if (!stillValid(player)) return false;
        switch (button) {
            case LOAD_BUTTON -> arm.loadCard();
            case RUN_BUTTON -> arm.running = !arm.running;
            case RESTART_BUTTON -> arm.restart();
            case REPEAT_BUTTON -> arm.repeat = !arm.repeat;
            case SKIP_BUTTON -> arm.skipBlocked = !arm.skipBlocked;
            case REDSTONE_BUTTON -> arm.redstoneMode = (arm.redstoneMode + 1) % RobotArmBlockEntity.REDSTONE_MODES;
            default -> {
                var index = (button - MATCH_BUTTON) / Match.values().length;
                if (button < MATCH_BUTTON || index >= arm.program.actions().size()) return false;
                arm.program = arm.program.withMatch(index, Match.values()[(button - MATCH_BUTTON) % Match.values().length]);
            }
        }
        arm.setChanged();
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {

        super.broadcastChanges();
        if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) return;
        var snapshot = new RobotNetworking.Snapshot(containerId, arm.program, arm.cursor, arm.running, arm.repeat,
                arm.skipBlocked, arm.redstoneMode, arm.status);
        if (!snapshot.equals(lastSent)) {
            lastSent = snapshot;
            PacketDistributor.sendToPlayer(serverPlayer, snapshot);
        }
    }

    @Override
    public void receive(RobotNetworking.Snapshot snapshot) {

        program = snapshot.program();
        cursor = snapshot.cursor();
        running = snapshot.running();
        repeat = snapshot.repeat();
        skip = snapshot.skip();
        redstone = snapshot.redstone();
        status = snapshot.status();
        revision++;
    }
}
