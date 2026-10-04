package rearth.oritech.item.other;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

import java.util.function.Consumer;

public class MobCaptureItem extends Item {
    
    public MobCaptureItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {

        var resultingItem = ItemContent.UNHOLY_INTELLIGENCE.asItem();
        if (entity.isDeadOrDying() || user.level().isClientSide()) return InteractionResult.PASS;

        if (entity.is(TagContent.FLYING_MOBS)) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                user.setItemInHand(hand, ItemStack.EMPTY);
            } else {
                user.setItemInHand(hand, stack);
            }

            entity.kill((ServerLevel) user.level());

            user.level().addFreshEntity(new ItemEntity(user.level(), entity.getX(), entity.getY(), entity.getZ(), new ItemStack(resultingItem)));

            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }


    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        var showExtra = Minecraft.getInstance().hasControlDown();

        if (showExtra) {
            builder.accept(Component.translatable("tooltip.oritech.capture_item_desc_1"));
            builder.accept(Component.translatable("tooltip.oritech.capture_item_desc_2"));
            builder.accept(Component.translatable("tooltip.oritech.capture_item_desc_3"));
        } else {
            builder.accept(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
    }
}
