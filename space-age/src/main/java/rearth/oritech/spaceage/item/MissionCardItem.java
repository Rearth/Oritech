package rearth.oritech.spaceage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import java.util.function.Consumer;

public final class MissionCardItem extends Item {
    public MissionCardItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        var mission = stack.get(SpaceAgeComponents.MISSION.get());
        if (mission == null) {
            tooltip.accept(Component.translatable("tooltip.oritech_space_age.mission_card.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        var steps = mission.plan().branches().stream().flatMap(branch -> branch.actions().stream()).filter(action -> !action.isGenerated()).count();
        tooltip.accept(Component.translatable("tooltip.oritech_space_age.mission_card.saved").withStyle(ChatFormatting.GREEN));
        tooltip.accept(Component.translatable("tooltip.oritech_space_age.mission_card.summary", steps, mission.plan().branches().size())
                .withStyle(ChatFormatting.GRAY));
    }
}
