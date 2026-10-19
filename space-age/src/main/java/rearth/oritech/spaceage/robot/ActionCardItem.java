package rearth.oritech.spaceage.robot;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import rearth.oritech.spaceage.init.SpaceAgeComponents;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class ActionCardItem extends Item {

    public ActionCardItem(Properties properties) {

        super(properties);
    }

    public static void bind(Player player, ItemStack stack, RobotArmBlockEntity arm) {

        var binding = GlobalPos.of(player.level().dimension(), arm.getBlockPos());
        var previous = stack.getOrDefault(SpaceAgeComponents.ROBOT_PROGRAM.get(), RobotProgram.EMPTY);
        stack.set(SpaceAgeComponents.ROBOT_PROGRAM.get(), new RobotProgram(Optional.of(binding),
                previous.binding().filter(binding::equals).isPresent() ? previous.actions() : List.of()));
        player.sendSystemMessage(Component.translatable("message.oritech_space_age.robot_bound", arm.getBlockPos().toShortString()));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        return use(context.getLevel(), context.getPlayer(), context.getHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new RobotMenu(id, inventory, hand), getName(player.getItemInHand(hand))), buffer -> {
                buffer.writeBoolean(false);
                buffer.writeEnum(hand);
            });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {

        var program = stack.getOrDefault(SpaceAgeComponents.ROBOT_PROGRAM.get(), RobotProgram.EMPTY);
        tooltip.accept(Component.translatable("tooltip.oritech_space_age.action_card"));
        program.binding().ifPresent(binding -> tooltip.accept(Component.literal(binding.pos().toShortString())));
        tooltip.accept(Component.translatable("screen.oritech_space_age.robot.actions", program.actions().size()));
    }
}
