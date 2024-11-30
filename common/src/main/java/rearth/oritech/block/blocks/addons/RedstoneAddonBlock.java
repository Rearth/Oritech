package rearth.oritech.block.blocks.addons;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;

public class RedstoneAddonBlock extends MachineAddonBlock {
    
    public RedstoneAddonBlock(Settings settings, AddonSettings addonSettings) {
        super(settings, addonSettings);
        this.setDefaultState(getDefaultState().with(Properties.POWERED, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.POWERED);
    }
    
    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }
    
    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }
    
    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ((RedstoneAddonBlockEntity) world.getBlockEntity(pos)).currentOutput;
    }
    
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        
        if (world.isClient) return;
        
        var isPowered = world.isReceivingRedstonePower(pos);
        
        var addonEntity = (RedstoneAddonBlockEntity) world.getBlockEntity(pos);
        addonEntity.setRedstonePowered(isPowered);
        
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        
        var isPowered = world.isReceivingRedstonePower(pos);
        var poweredState = state.with(Properties.POWERED, isPowered);
        
        return super.getStateForNeighborUpdate(poweredState, direction, neighborState, world, pos, neighborPos);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            var handler = (ExtendedScreenHandlerFactory) world.getBlockEntity(pos);
            player.openHandledScreen(handler);
            
        }
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return RedstoneAddonBlockEntity.class;
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
}
