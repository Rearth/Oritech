package rearth.oritech.item.other;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.util.ColorableMachine;

public class ColorCartridgeItem extends Item {
    
    public final ColorableMachine.ColorVariant variant;
    
    public ColorCartridgeItem(Properties properties, ColorableMachine.ColorVariant variant) {
        super(properties);
        this.variant = variant;
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        
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
            
            return InteractionResult.CONSUME;
        }
        
        return super.useOn(context);
    }
}
