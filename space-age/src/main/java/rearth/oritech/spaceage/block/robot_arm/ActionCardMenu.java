package rearth.oritech.spaceage.block.robot_arm;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram.Match;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.spaceage.network.RobotNetworking;

import java.util.List;

/** Edits the playbook stored on a held action card. */
public final class ActionCardMenu extends AbstractContainerMenu implements RobotNetworking.Receiver {

    public static final int CLEAR_BUTTON = 0;
    public static final int DELETE_BUTTON = 2000;

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack card;
    public RobotProgram program;
    public int revision;
    private RobotNetworking.Snapshot lastSent;

    public ActionCardMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {

        this(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    public ActionCardMenu(int id, Inventory inventory, InteractionHand hand) {

        super(SpaceAgeMenus.ACTION_CARD.get(), id);
        this.player = inventory.player;
        this.hand = hand;
        this.card = player.getItemInHand(hand);
        this.program = card.getOrDefault(SpaceAgeComponents.ROBOT_PROGRAM.get(), RobotProgram.EMPTY);
    }

    public ItemStack getCard() {

        return card;
    }

    private RobotProgram stored() {

        return card.getOrDefault(SpaceAgeComponents.ROBOT_PROGRAM.get(), RobotProgram.EMPTY);
    }

    @Override
    public boolean stillValid(Player player) {

        return player.getItemInHand(hand) == card && card.getItem() instanceof ActionCardItem;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {

        if (!stillValid(player)) return false;
        var data = stored();
        var matchCount = Match.values().length;
        if (button == CLEAR_BUTTON) {
            card.set(SpaceAgeComponents.ROBOT_PROGRAM.get(), data.withActions(List.of()));
        } else if (button >= DELETE_BUTTON && button < DELETE_BUTTON + RobotProgram.LIMIT) {
            if (button - DELETE_BUTTON >= data.actions().size()) return false;
            card.set(SpaceAgeComponents.ROBOT_PROGRAM.get(), data.without(button - DELETE_BUTTON));
        } else if (button >= RobotMenu.MATCH_BUTTON && button < RobotMenu.MATCH_BUTTON + RobotProgram.LIMIT * matchCount) {
            var index = (button - RobotMenu.MATCH_BUTTON) / matchCount;
            if (index >= data.actions().size()) return false;
            card.set(SpaceAgeComponents.ROBOT_PROGRAM.get(), data.withMatch(index, Match.values()[(button - RobotMenu.MATCH_BUTTON) % matchCount]));
        } else return false;
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {

        super.broadcastChanges();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        var snapshot = new RobotNetworking.Snapshot(containerId, stored(), 0, false, false, false, 0, "ready");
        if (!snapshot.equals(lastSent)) {
            lastSent = snapshot;
            PacketDistributor.sendToPlayer(serverPlayer, snapshot);
        }
    }

    @Override
    public void receive(RobotNetworking.Snapshot snapshot) {

        program = snapshot.program();
        revision++;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {

        return ItemStack.EMPTY;
    }
}
