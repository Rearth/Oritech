package rearth.oritech.spaceage.block.robot_arm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram.Action;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram.Kind;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram.Match;
import rearth.oritech.spaceage.init.SpaceAgeComponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RobotRecording {

    private static final ArrayList<Runnable> PENDING = new ArrayList<>();
    private static final Map<UUID, Click> CLICKS = new HashMap<>();
    private static final Vec3 CENTER = new Vec3(.5, .5, .5);

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
        var arm = event.getHand() == InteractionHand.MAIN_HAND && event.getItemStack().getItem() instanceof ActionCardItem
                ? arm(player.level(), event.getPos()) : null;
        if (arm != null) {
            ActionCardItem.bind(player, event.getItemStack(), arm);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        var card = card(player);
        if (card.isEmpty() || event.getHand() != InteractionHand.MAIN_HAND) return;
        var hit = event.getHitVec();
        var state = player.level().getBlockState(event.getPos());
        var click = new Click(record(player, Kind.INTERACT, event.getPos().immutable(), state, hit.getDirection(),
                hit.getLocation().subtract(Vec3.atLowerCornerOf(event.getPos())), player.getMainHandItem()));
        CLICKS.put(player.getUUID(), click);
        var menu = player.containerMenu;
        PENDING.add(() -> {
            if (!event.isCanceled() && !click.placed && player.containerMenu == menu && state.getMenuProvider(player.level(), event.getPos()) == null)
                append(player, card, click.action);
        });
    }

    /** The arm at the clicked position, also when clicking one of its machine cores. */
    @Nullable
    private static RobotArmBlockEntity arm(Level level, BlockPos pos) {

        var entity = level.getBlockEntity(pos);
        if (entity instanceof MachineCoreEntity core && level.getBlockState(pos).getValue(MachineCoreBlock.USED))
            entity = level.getBlockEntity(core.getControllerPos());
        return entity instanceof RobotArmBlockEntity arm ? arm : null;
    }

    private static void place(BlockEvent.EntityPlaceEvent event) {

        if (event.isCanceled() || !(event.getEntity() instanceof Player player)) return;
        var card = card(player);
        if (card.isEmpty()) return;
        var click = CLICKS.get(player.getUUID());
        // Tool transformations also fire placement events; retain the original item-use action.
        if (click != null && !(click.action.item().getItem() instanceof BlockItem)) return;
        if (click != null) click.placed = true;
        var state = event.getPlacedBlock();
        var pos = event.getPos().immutable();
        // Placements keep the clicked face and hit point, relative to the placed block.
        var action = click == null
                ? record(player, Kind.PLACE, pos, state, Direction.UP, CENTER, new ItemStack(state.getBlock()))
                : record(player, Kind.PLACE, pos, state, click.action.face(),
                Vec3.atLowerCornerOf(click.action.pos()).add(click.action.hit()).subtract(Vec3.atLowerCornerOf(pos)), click.action.item());
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
        var action = record(player, Kind.DESTROY, pos, state, Direction.UP, CENTER, ItemStack.EMPTY);
        PENDING.add(() -> {
            if (!event.isCanceled() && !player.level().getBlockState(pos).equals(state)) append(player, card, action);
        });
    }

    private static Action record(Player player, Kind kind, BlockPos pos, BlockState state, Direction face, Vec3 hit, ItemStack item) {

        return new Action(kind, pos, state, face, hit, item.copyWithCount(1), player.getYRot(), player.getXRot(),
                player.isShiftKeyDown(), Match.IGNORE);
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
