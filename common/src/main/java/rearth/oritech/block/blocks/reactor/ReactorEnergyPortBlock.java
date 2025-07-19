package rearth.oritech.block.blocks.reactor;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.reactor.ReactorEnergyPortEntity;

import java.util.List;
import java.util.Objects;

public class ReactorEnergyPortBlock extends BaseReactorBlock implements BlockEntityProvider {
    public ReactorEnergyPortBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(Properties.FACING, Direction.NORTH));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.FACING);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(Properties.FACING, ctx.getPlayerLookDirection().getOpposite());
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorEnergyPortEntity(pos, state);
    }
    
    @Override
    public boolean validForWalls() {
        return true;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            tooltip.add(Text.translatable("tooltip.oritech.reactor_energy_port.rate", Oritech.CONFIG.reactorMaxEnergyOutput()));
        }
        
    }
}
