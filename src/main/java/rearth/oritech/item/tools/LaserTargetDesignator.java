package rearth.oritech.item.tools;

import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.block.entity.machines.interaction.DronePortEntity;
import rearth.oritech.block.entity.machines.interaction.LaserArmBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ComponentContent;

import java.util.List;

public class LaserTargetDesignator extends Item {
    public LaserTargetDesignator(Settings settings) {
        super(settings);
    }
    
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) {
            return super.useOnBlock(context);
        }
        
        var targetPos = context.getBlockPos();
        
        var targetBlockState = context.getWorld().getBlockState(context.getBlockPos());
        if (targetBlockState.getBlock() instanceof MachineCoreBlock && targetBlockState.get(MachineCoreBlock.USED)) {
            // target the base instead (on laser arms)
            var machineEntity = MachineCoreBlock.getControllerEntity(context.getWorld(), context.getBlockPos());
            if (machineEntity instanceof LaserArmBlockEntity) {
                targetPos = context.getBlockPos().down();
                targetBlockState = context.getWorld().getBlockState(targetPos);
            }
        }
        
        if (targetBlockState.getBlock().equals(BlockContent.LASER_ARM_BLOCK)
              && context.getWorld().getBlockEntity(targetPos) instanceof LaserArmBlockEntity laserEntity
              && context.getStack().contains(ComponentContent.TARGET_POSITION)) {
            var target = context.getStack().get(ComponentContent.TARGET_POSITION);
            
            var success = laserEntity.setTargetFromDesignator(target);
            if (success)
                context.getPlayer().sendMessage(Text.literal("Position saved to machine"));
            return success ? ActionResult.SUCCESS : ActionResult.FAIL;
        }
        
        if (targetBlockState.getBlock().equals(BlockContent.DRONE_PORT_BLOCK)
              && context.getWorld().getBlockEntity(context.getBlockPos()) instanceof DronePortEntity dronePortEntity
              && context.getStack().contains(ComponentContent.TARGET_POSITION)) {
            var target = context.getStack().get(ComponentContent.TARGET_POSITION);
            
            var success = dronePortEntity.setTargetFromDesignator(target);
            if (success) {
                context.getPlayer().sendMessage(Text.literal("Position saved to machine"));
            } else {
                context.getPlayer().sendMessage(Text.literal("Invalid position for drone port, target port must be at least 50 blocks away"));
            }
            return success ? ActionResult.SUCCESS : ActionResult.FAIL;
        }
        
        if (!targetBlockState.getBlock().equals(Blocks.AIR)) {
            Oritech.LOGGER.debug(targetBlockState.toString());
            
            context.getStack().set(ComponentContent.TARGET_POSITION, context.getBlockPos());
            context.getPlayer().sendMessage(Text.literal("Position stored"));
            
            return ActionResult.SUCCESS;
        }
        
        return super.useOnBlock(context);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        
        if (stack.contains(ComponentContent.TARGET_POSITION)) {
            var data = stack.get(ComponentContent.TARGET_POSITION);
            tooltip.add(Text.of("Set to: [" + data.toShortString() + "]"));
        } else {
            tooltip.add(Text.of("No target set"));
        }
    }
}
