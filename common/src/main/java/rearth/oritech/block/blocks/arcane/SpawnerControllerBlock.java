package rearth.oritech.block.blocks.arcane;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.arcane.SpawnerControllerBlockEntity;

import java.util.List;

public class SpawnerControllerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    
    public SpawnerControllerBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(world, pos, state, entity);
        
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof SpawnerControllerBlockEntity spawnerEntity) {
            spawnerEntity.onEntitySteppedOn(entity);
        }
        
    }
    
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }
    
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);
        
        if (world.isClientSide) return;
        
        var isPowered = world.hasNeighborSignal(pos);
        
        var entity = (SpawnerControllerBlockEntity) world.getBlockEntity(pos);
        entity.setRedstonePowered(isPowered);
        
    }
    
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
    
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return ((SpawnerControllerBlockEntity) world.getBlockEntity(pos)).getComparatorOutput();
    }
    
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {

        if (!world.isClientSide && world.getBlockEntity(pos) instanceof SpawnerControllerBlockEntity spawnerEntity) {
            spawnerEntity.onBlockInteracted(player);
        }

        return InteractionResult.SUCCESS;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpawnerControllerBlockEntity(pos, state);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        tooltip.add(Component.translatable("tooltip.oritech.spawner").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.oritech.spawner2").withStyle(ChatFormatting.GRAY));
    }
}
