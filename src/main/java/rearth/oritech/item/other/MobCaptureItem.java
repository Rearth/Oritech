package rearth.oritech.item.other;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import rearth.oritech.init.ItemContent;

import java.util.List;

public class MobCaptureItem extends Item {

    public final List<EntityType<?>> targets;

    public MobCaptureItem(Properties settings, List<EntityType<?>> targets) {
        super(settings);
        this.targets = targets;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {

        var resultingItem = ItemContent.UNHOLY_INTELLIGENCE;
        if (entity.isDeadOrDying()) return InteractionResult.PASS;

        for (var target : targets) {
            if (entity.getType().equals(target)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    user.setItemInHand(hand, ItemStack.EMPTY);
                } else {
                    user.setItemInHand(hand, stack);
                }

                entity.kill();

                user.level().addFreshEntity(new ItemEntity(user.level(), entity.getX(), entity.getY(), entity.getZ(), new ItemStack(resultingItem)));

                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);

        var showExtra = Minecraft.getInstance().hasControlDown();

        if (showExtra) {
            consumer.accept(Component.translatable("tooltip.oritech.capture_item_desc_1"));
            consumer.accept(Component.translatable("tooltip.oritech.capture_item_desc_2"));
            consumer.accept(Component.translatable("tooltip.oritech.capture_item_desc_3"));
        } else {
            consumer.accept(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
    }
}
