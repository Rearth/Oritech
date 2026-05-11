package rearth.oritech.item.other;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;
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
            
            // create particles
            var targetBlocks = new ArrayList<BlockPos>();
            targetBlocks.add(targetBlock);
            
            if (targetEntity instanceof MultiblockMachineController multiblockMachineController) {
                targetBlocks.addAll(multiblockMachineController.getConnectedCores());
            }
            
            var level = context.getLevel();
            if (context.getLevel() instanceof ServerLevel serverLevel) {
                for (var pos : targetBlocks) {
                    var at = pos.getCenter().add(level.random.nextFloat() * 0.1, level.random.nextFloat() * 0.1, level.random.nextFloat() * 0.1);
                    serverLevel.sendParticles(ParticleTypes.GUST, at.x, at.y, at.z, 1, level.random.nextFloat(), level.random.nextFloat(), level.random.nextFloat(), 0.15f);
                }
            }
            
            return InteractionResult.SUCCESS;
        }
        
        return super.useOn(context);
    }
}
