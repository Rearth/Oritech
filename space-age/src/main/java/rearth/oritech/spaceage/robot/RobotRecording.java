package rearth.oritech.spaceage.robot;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.robot.RobotProgram.Action;
import rearth.oritech.spaceage.robot.RobotProgram.Kind;
import rearth.oritech.spaceage.robot.RobotProgram.StepSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RobotRecording {

    private static final ArrayList<Runnable> PENDING = new ArrayList<>();
    private static final Map<UUID, Click> CLICKS = new HashMap<>();

    private RobotRecording() {}

    public static void register() {

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RobotRecording::click);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RobotRecording::place);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RobotRecording::destroy);
        NeoForge.EVENT_BUS.addListener(RobotRecording::finish);
    }

    private static ItemStack card(Player player) {

        return player instanceof ServerPlayer && !(player instanceof FakePlayer)
                && player.getOffhandItem().getItem() instanceof ActionCardItem ? player.getOffhandItem() : ItemStack.EMPTY;
    }

    private static void append(Player player, ItemStack card, Action action) {

        if (player.getOffhandItem() != card) return;
        var data = card.get(SpaceAgeComponents.ROBOT_PROGRAM.get());
        if (data == null || data.binding().isEmpty() || data.actions().size() >= RobotProgram.LIMIT) return;
        var binding = data.binding().get();
        if (binding.dimension() != player.level().dimension() || !player.level().hasChunkAt(binding.pos())) return;
        if (!(player.level().getBlockEntity(binding.pos()) instanceof RobotArmBlockEntity arm) || !arm.inRange(action.pos())) return;
        var actions = new ArrayList<>(data.actions());
        actions.add(action);
        card.set(SpaceAgeComponents.ROBOT_PROGRAM.get(), data.withActions(actions));
    }

    private static void click(PlayerInteractEvent.RightClickBlock event) {

        var player = event.getEntity();
        if (event.isCanceled() || player.level().isClientSide() || player instanceof FakePlayer) return;
        if (event.getItemStack().getItem() instanceof ActionCardItem
                && player.level().getBlockEntity(event.getPos()) instanceof RobotArmBlockEntity arm) {
            ActionCardItem.bind(player, event.getItemStack(), arm);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        var card = card(player);
        if (card.isEmpty() || event.getHand() != InteractionHand.MAIN_HAND) return;
        var hit = event.getHitVec();
        var state = player.level().getBlockState(event.getPos());
        var click = new Click(new Action(Kind.INTERACT, event.getPos().immutable(), state, hit.getDirection(),
                hit.getLocation().subtract(Vec3.atLowerCornerOf(event.getPos())), player.getMainHandItem().copyWithCount(1),
                player.getYRot(), player.getXRot(), player.isShiftKeyDown(), StepSignal.IGNORED));
        CLICKS.put(player.getUUID(), click);
        var menu = player.containerMenu;
        PENDING.add(() -> {
            if (!event.isCanceled() && !click.placed && player.containerMenu == menu && state.getMenuProvider(player.level(), event.getPos()) == null)
                append(player, card, click.action);
        });
    }

    private static void place(BlockEvent.EntityPlaceEvent event) {

        if (event.isCanceled() || !(event.getEntity() instanceof Player player)) return;
        var card = card(player);
        if (card.isEmpty()) return;
        var click = CLICKS.get(player.getUUID());
        if (click != null) click.placed = true;
        var state = event.getPlacedBlock();
        var pos = event.getPos().immutable();
        var item = click == null ? new ItemStack(state.getBlock()) : click.action.item();
        var action = new Action(Kind.PLACE, pos, state, click == null ? Direction.UP : click.action.face(),
                click == null ? new Vec3(.5, .5, .5) : Vec3.atLowerCornerOf(click.action.pos()).add(click.action.hit()).subtract(Vec3.atLowerCornerOf(pos)), item.copyWithCount(1), player.getYRot(), player.getXRot(), player.isShiftKeyDown(), StepSignal.IGNORED);
        PENDING.add(() -> {
            if (!event.isCanceled() && player.level().getBlockState(pos).is(state.getBlock())) append(player, card, action);
        });
    }

    private static void destroy(BreakBlockEvent event) {

        if (event.isCanceled()) return;
        var player = event.getPlayer();
        var card = card(player);
        if (card.isEmpty()) return;
        var pos = event.getPos().immutable();
        var state = event.getState();
        var action = new Action(Kind.DESTROY, pos, state, Direction.UP, new Vec3(.5, .5, .5),
                ItemStack.EMPTY, player.getYRot(), player.getXRot(), player.isShiftKeyDown(), StepSignal.IGNORED);
        PENDING.add(() -> {
            if (!event.isCanceled() && !player.level().getBlockState(pos).equals(state)) append(player, card, action);
        });
    }

    private static void finish(ServerTickEvent.Post event) {

        var pending = new ArrayList<>(PENDING);
        PENDING.clear();
        pending.forEach(Runnable::run);
        CLICKS.clear();
    }

    private static final class Click {

        final Action action;
        boolean placed;

        Click(Action action) {

            this.action = action;
        }
    }
}
