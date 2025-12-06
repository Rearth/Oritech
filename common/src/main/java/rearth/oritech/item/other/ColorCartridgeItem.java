package rearth.oritech.item.other;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.util.ColorableMachine;

import java.util.List;

public class ColorCartridgeItem extends Item {
    
    public final ColorableMachine.ColorVariant variant;
    
    public ColorCartridgeItem(Properties properties, ColorableMachine.ColorVariant variant) {
        super(properties);
        this.variant = variant;
    }
    
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        
        tooltipComponents.add(Component.translatable("tooltip.oritech.paint.1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.translatable("tooltip.oritech.paint.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
    
    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        
        if (context.getLevel().isClientSide())
            return super.useOn(context);
        
        var targetBlock = context.getClickedPos();
        var targetEntity = context.getLevel().getBlockEntity(targetBlock);
        
        if (targetEntity instanceof MachineCoreEntity machineCore && machineCore.getCachedController() != null) {
            targetEntity = (net.minecraft.world.level.block.entity.BlockEntity) machineCore.getCachedController();
            targetBlock = targetEntity.getBlockPos();
        }
        
        if (targetEntity instanceof ColorableMachine colorableMachine) {
            
            if (colorableMachine.getCurrentColor().equals(variant)) return super.useOn(context);
            
            Oritech.LOGGER.info("assigning color {} to {}", variant, targetBlock);
            colorableMachine.assignColor(variant);
            
            var stack = context.getItemInHand();
            stack.shrink(1);
            
            context.getPlayer().setItemInHand(context.getHand(), stack);
            
            context.getLevel().playSound(null, targetBlock, SoundEvents.AXOLOTL_SPLASH, SoundSource.PLAYERS, 1f, 0.6f);
            
            return InteractionResult.CONSUME;
        }
        
        return super.useOn(context);
    }
}
