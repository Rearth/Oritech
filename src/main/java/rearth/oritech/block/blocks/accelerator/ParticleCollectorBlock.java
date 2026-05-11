package rearth.oritech.block.blocks.accelerator;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.accelerator.ParticleCollectorBlockEntity;
import rearth.oritech.util.TooltipHelper;

import java.util.List;
import java.util.Objects;

public class ParticleCollectorBlock extends DirectionalBlock implements EntityBlock {
    
    public ParticleCollectorBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DirectionalBlock.FACING);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(DirectionalBlock.FACING, ctx.getNearestLookingDirection().getOpposite());
    }
    
    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return null;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ParticleCollectorBlockEntity(pos, state);
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
        var showExtra = Screen.hasControlDown();
        if (showExtra) {
            tooltip.add(Component.translatable("tooltip.oritech.particle_collector").withStyle(ChatFormatting.GRAY));
        }
        
        TooltipHelper.addMachineTooltip(tooltip, this, this);
    }
}
