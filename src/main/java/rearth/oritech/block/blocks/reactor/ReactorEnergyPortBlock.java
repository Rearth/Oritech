package rearth.oritech.block.blocks.reactor;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.reactor.ReactorEnergyPortEntity;
import rearth.oritech.config.OritechConfig;

import java.util.Objects;
import java.util.function.Consumer;

public class ReactorEnergyPortBlock extends BaseReactorBlock implements EntityBlock, TooltipProvider {
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
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {

        var showExtra = Minecraft.getInstance().hasControlDown();

        if (showExtra) {
            consumer.accept(Component.translatable("tooltip.oritech.reactor_energy_port.rate", OritechConfig.reactorMaxEnergyOutput.get()));
        }

    }
}
