package rearth.oritech.block.blocks.reactor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;

public abstract class BaseReactorBlock extends Block implements TooltipProvider {

    public BaseReactorBlock(Properties settings) {
        super(settings);
    }

    public boolean validForWalls() {
        return false;
    }

    public Block requiredStackCeiling() {
        return Blocks.AIR;
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {

        var showExtra = Minecraft.getInstance().hasControlDown();

        if (showExtra && dataComponentGetter instanceof ItemStack stack) {
            var machineId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            consumer.accept(Component.translatable("tooltip.oritech." + machineId));

            for (int i = 0; i < 6; i++) {
                var key = "tooltip.oritech." + machineId + "." + i;
                if (I18n.exists(key)) {
                    consumer.accept(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            }
        } else {
            consumer.accept(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }

    }
}
