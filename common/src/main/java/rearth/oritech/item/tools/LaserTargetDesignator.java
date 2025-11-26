package rearth.oritech.item.tools;

import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.interaction.DronePortEntity;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ComponentContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LaserTargetDesignator extends Item {
    public LaserTargetDesignator(Properties settings) {
        super(settings);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        var targetPos = context.getClickedPos();
        
        var targetBlockState = context.getLevel().getBlockState(context.getClickedPos());
        if (targetBlockState.getBlock() instanceof MachineCoreBlock && targetBlockState.getValue(MachineCoreBlock.USED)) {
            // target the base instead (on laser arms)
            var machineEntity = MachineCoreBlock.getControllerEntity(context.getLevel(), context.getClickedPos());
            if (machineEntity instanceof LaserArmBlockEntity) {
                targetPos = context.getClickedPos().below();
                targetBlockState = context.getLevel().getBlockState(targetPos);
            }
        }
        
        if (targetBlockState.getBlock().equals(BlockContent.LASER_ARM_BLOCK)
              && context.getLevel().getBlockEntity(targetPos) instanceof LaserArmBlockEntity laserEntity) {
            
            if (laserEntity.hunterAddons > 0) {
                laserEntity.cycleHunterTargetMode();
                context.getPlayer().sendSystemMessage(Component.translatable("message.oritech.target_designator.hunter_target", Component.translatable(laserEntity.hunterTargetMode.message)));
                return InteractionResult.SUCCESS;
            } else if (context.getItemInHand().has(ComponentContent.TARGET_POSITION.get())) {
                var target = context.getItemInHand().get(ComponentContent.TARGET_POSITION.get());

                var success = laserEntity.setTargetFromDesignator(target);
                if (success)
                    context.getPlayer().sendSystemMessage(Component.translatable("message.oritech.target_designator.position_saved"));
                return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
        }
        
        if (targetBlockState.getBlock().equals(BlockContent.DRONE_PORT_BLOCK)
              && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof DronePortEntity dronePortEntity
              && context.getItemInHand().has(ComponentContent.TARGET_POSITION.get())) {
            var target = context.getItemInHand().get(ComponentContent.TARGET_POSITION.get());
            
            var success = dronePortEntity.setTargetFromDesignator(target);
            if (success) {
                context.getPlayer().sendSystemMessage(Component.translatable("message.oritech.target_designator.position_saved"));
            } else {
                context.getPlayer().sendSystemMessage(Component.translatable("message.oritech.target_designator.position_invalid"));
            }
            return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        
        if (!targetBlockState.getBlock().equals(Blocks.AIR)) {
            Oritech.LOGGER.debug(targetBlockState.toString());
            
            context.getItemInHand().set(ComponentContent.TARGET_POSITION.get(), context.getClickedPos());
            context.getPlayer().sendSystemMessage(Component.translatable("message.oritech.target_designator.position_stored"));
            
            return InteractionResult.SUCCESS;
        }
        
        return super.useOn(context);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);
        
        if (stack.has(ComponentContent.TARGET_POSITION.get())) {
            var data = stack.get(ComponentContent.TARGET_POSITION.get());
            tooltip.add(Component.translatable("tooltip.oritech.target_designator.set_to", data.toShortString()));
        } else {
            tooltip.add(Component.translatable("tooltip.oritech.target_designator.no_target"));
        }
    }
}
