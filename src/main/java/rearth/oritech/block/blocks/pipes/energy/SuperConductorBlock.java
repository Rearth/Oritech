package rearth.oritech.block.blocks.pipes.energy;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.apache.commons.lang3.function.TriFunction;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;

import java.util.HashMap;
import java.util.function.Consumer;

public class SuperConductorBlock extends GenericPipeBlock {

    public static HashMap<Identifier, GenericPipeInterfaceEntity.PipeNetworkData> SUPERCONDUCTOR_DATA = new HashMap<>();

    public SuperConductorBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((level, pos, direction) -> level.getCapability(Capabilities.Energy.BLOCK, pos, direction) != null);
    }

    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.SUPERCONDUCTOR_CONNECTION.get().defaultBlockState();
    }

    @Override
    public BlockState getNormalBlock() {
        return BlockContent.SUPERCONDUCTOR.get().defaultBlockState();
    }

    @Override
    public SavedDataType<GenericPipeInterfaceEntity.PipeNetworkData> getNetworkDataType() {
        return GenericPipeInterfaceEntity.PipeNetworkData.SUPERCONDUCTOR_TYPE;
    }

    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof SuperConductorBlock || block instanceof SuperConductorConnectionBlock || block instanceof SuperConductorDuctBlock;
    }

    @Override
    protected VoxelShape[] createShapes() {
        return EXTRA_THICK_SHAPES;
    }

    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level level) {
        return SUPERCONDUCTOR_DATA.computeIfAbsent(level.dimension().identifier(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }

    @Override
    public boolean isCompatibleTarget(Block block) {
        return !block.equals(BlockContent.ENERGY_PIPE_CONNECTION.get());
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        var text = Component.translatable("tooltip.oritech.energy_max_transfer").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable("tooltip.oritech.energy_transfer_rate", OritechConfig.superConductorTransferRate.get()).withStyle(ChatFormatting.GOLD));
        consumer.accept(text);
        consumer.accept(Component.translatable("tooltip.oritech.superconductor").withStyle(ChatFormatting.GRAY));
    }

    public static class FramedSuperConductorBlock extends SuperConductorBlock {

        public FramedSuperConductorBlock(Properties settings) {
            super(settings);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.block();
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getShape(level, pos);
        }

        @Override
        public BlockState getNormalBlock() {
            return BlockContent.FRAMED_SUPERCONDUCTOR.get().defaultBlockState();
        }

        @Override
        public BlockState getConnectionBlock() {
            return BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION.get().defaultBlockState();
        }
    }
}
