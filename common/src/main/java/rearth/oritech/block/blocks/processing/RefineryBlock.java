package rearth.oritech.block.blocks.processing;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.processing.RefineryBlockEntity;
import rearth.oritech.block.entity.processing.TaintedRefineryBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.BiConsumer;

public class RefineryBlock extends MultiblockMachine implements EntityBlock {
    
    public static Queue<Runnable> DELAYED_TAINT_EVENTS = new ArrayDeque<>();
    
    public RefineryBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return RefineryBlockEntity.class;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            tooltip.add(Component.translatable("tooltip.oritech.refinery_block").withStyle(ChatFormatting.GRAY));
        }
    }
    
    @Override
    public void onExplosionHit(BlockState state, Level world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {
        
        var refineryEntity = world.getBlockEntity(pos, BlockEntitiesContent.REFINERY_ENTITY);
        
        if (world.isClientSide() || refineryEntity.isEmpty()) {
            super.onExplosionHit(state, world, pos, explosion, stackMerger);
            return;
        }
        
        var crystalCandidate = refineryEntity.get().getNearbyNonEmptyCatalyst();
        if (crystalCandidate.isEmpty()) {
            super.onExplosionHit(state, world, pos, explosion, stackMerger);
            return;
        }
        
        refineryEntity.get().taintTransform();
        var color = refineryEntity.get().currentColor;
        
        // custom merger to void refinery self drop
        super.onExplosionHit(state, world, pos, explosion, ((itemStack, blockPos) -> {}));
        
        // explode crystal
        crystalCandidate.get().doExplosion();
        
        var targetPos = pos.immutable();
        
        // run in next tick to avoid explosion block weirdness
        DELAYED_TAINT_EVENTS.add(() -> {
            // create + init refinery
            world.setBlockAndUpdate(targetPos,
              BlockContent.TAINTED_REFINERY_BLOCK.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, state.getValue(BlockStateProperties.HORIZONTAL_FACING))
            );
            
            if (world.getBlockEntity(targetPos) instanceof TaintedRefineryBlockEntity taintedRefinery) {
                taintedRefinery.afterCreation();
                taintedRefinery.assignColor(color);
            }
        });
        
        // idea / potential todo: particles released from catalyst to refinery (along random offset paths?)
        
    }
    
    public static void updateTaintEvents() {
        for (var elem : DELAYED_TAINT_EVENTS) {
            elem.run();;
        }
        DELAYED_TAINT_EVENTS.clear();
    }
}
