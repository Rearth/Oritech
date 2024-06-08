package rearth.oritech.item.tools;

import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.block.entity.machines.interaction.DronePortEntity;
import rearth.oritech.block.entity.machines.interaction.LaserArmBlockEntity;
import rearth.oritech.init.BlockContent;

import java.util.List;
import java.util.Objects;

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
              && context.getStack().hasNbt()) {
            var target = BlockPos.fromLong(context.getStack().getNbt().getLong("target"));
            
            var success = laserEntity.setTargetFromDesignator(target);
            if (success)
                context.getPlayer().sendMessage(Text.literal("Position saved to machine"));
            return success ? ActionResult.SUCCESS : ActionResult.FAIL;
        }
        
        if (targetBlockState.getBlock().equals(BlockContent.DRONE_PORT_BLOCK)
              && context.getWorld().getBlockEntity(context.getBlockPos()) instanceof DronePortEntity dronePortEntity
              && context.getStack().hasNbt()) {
            var target = BlockPos.fromLong(context.getStack().getNbt().getLong("target"));
            
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
            
            var nbt = context.getStack().getOrCreateNbt();
            nbt.putLong("target", context.getBlockPos().asLong());
            context.getPlayer().sendMessage(Text.literal("Position stored"));
            
            return ActionResult.SUCCESS;
        }
        
        return super.useOnBlock(context);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (stack.hasNbt()) {
            var data = BlockPos.fromLong(Objects.requireNonNull(stack.getNbt()).getLong("target"));
            tooltip.add(Text.of("Set to: [" + data.toShortString() + "]"));
        } else {
            tooltip.add(Text.of("No target set"));
        }
    }
}
