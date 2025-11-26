package rearth.oritech.block.blocks.reactor;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.reactor.ReactorEnergyPortEntity;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class ReactorEnergyPortBlock extends BaseReactorBlock implements EntityBlock {
    public ReactorEnergyPortBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.FACING);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.FACING, ctx.getNearestLookingDirection().getOpposite());
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorEnergyPortEntity(pos, state);
    }
    
    @Override
    public boolean validForWalls() {
        return true;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            tooltip.add(Component.translatable("tooltip.oritech.reactor_energy_port.rate", Oritech.CONFIG.reactorMaxEnergyOutput()));
        }
        
    }
}
