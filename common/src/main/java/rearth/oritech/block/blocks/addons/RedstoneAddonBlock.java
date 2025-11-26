package rearth.oritech.block.blocks.addons;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.util.ComparatorOutputProvider;

public class RedstoneAddonBlock extends MachineAddonBlock {
    
    public RedstoneAddonBlock(Properties settings, AddonSettings addonSettings) {
        super(settings, addonSettings);
        this.registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.POWERED);
    }
    
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }
    
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
    
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return ((ComparatorOutputProvider) world.getBlockEntity(pos)).getComparatorOutput();
    }
    
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);
        
        if (world.isClientSide) return;
        
        var isPowered = world.hasNeighborSignal(pos);
        
        var addonEntity = (RedstoneAddonBlockEntity) world.getBlockEntity(pos);
        addonEntity.setRedstonePowered(isPowered);
        
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        
        var isPowered = world.hasNeighborSignal(pos);
        var poweredState = state.setValue(BlockStateProperties.POWERED, isPowered);
        
        return super.updateShape(poweredState, direction, neighborState, world, pos, neighborPos);
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide) {
            var handler = (ExtendedMenuProvider) world.getBlockEntity(pos);
                MenuRegistry.openExtendedMenu((ServerPlayer) player, handler);
            
        }
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return RedstoneAddonBlockEntity.class;
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
    
}
