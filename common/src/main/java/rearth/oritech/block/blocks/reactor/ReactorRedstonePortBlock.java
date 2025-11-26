package rearth.oritech.block.blocks.reactor;

import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ReactorRedstonePortBlock extends BaseReactorBlock {
    
    public static final IntegerProperty PORT_MODE = IntegerProperty.create("port_mode", 0, 2);  // 0 = temperature, 1 = fuel, 2 = power
    
    public ReactorRedstonePortBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH).setValue(PORT_MODE, 0).setValue(BlockStateProperties.POWER, 0));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.FACING);
        builder.add(PORT_MODE);
        builder.add(BlockStateProperties.POWER);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.FACING, ctx.getNearestLookingDirection().getOpposite());
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (world.isClientSide) return InteractionResult.SUCCESS;
        
        var lastMode = state.getValue(PORT_MODE);
        var cycledMode = (lastMode + 1) % 3;
        
        player.sendSystemMessage(Component.translatable("tooltip.oritech.reactor_port_mode." + cycledMode));
        
        var newState = state.setValue(PORT_MODE, cycledMode);
        world.setBlockAndUpdate(pos, newState);
        
        return InteractionResult.SUCCESS;
        
    }
    
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
    
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return state.getValue(BlockStateProperties.POWER);
    }
    
    @Override
    public boolean validForWalls() {
        return true;
    }
}
