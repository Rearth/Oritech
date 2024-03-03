package rearth.oritech.block.blocks.machines.worldInteraction;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class MachineFrameBlock extends Block {
    
    public static final BooleanProperty NORTH = ConnectingBlock.NORTH;
    public static final BooleanProperty EAST = ConnectingBlock.EAST;
    
    public MachineFrameBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(NORTH, false).with(EAST, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var baseState = super.getPlacementState(ctx);
        
        var northConnected = ctx.getWorld().getBlockState(ctx.getBlockPos().north()).getBlock() == this;
        var eastConnected = ctx.getWorld().getBlockState(ctx.getBlockPos().east()).getBlock() == this;
        
        return Objects.requireNonNull(baseState).with(NORTH, northConnected).with(EAST, eastConnected);
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        
        var northConnected = world.getBlockState(pos.north()).getBlock() == this;
        var eastConnected = world.getBlockState(pos.east()).getBlock() == this;
        
        return state.with(NORTH, northConnected).with(EAST, eastConnected);
        
    }
}
