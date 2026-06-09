package rearth.oritech.block.blocks.pipes.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.blocks.pipes.GenericPipeConnectionBlock;
import rearth.oritech.block.entity.pipes.EnergyPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock.SUPERCONDUCTOR_DATA;

public class SuperConductorConnectionBlock extends GenericPipeConnectionBlock {

    public SuperConductorConnectionBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((level, pos, direction) -> level.getCapability(Capabilities.Energy.BLOCK, pos, direction) != null);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyPipeInterfaceEntity(pos, state);
    }

    @Override
    protected VoxelShape[] createShapes() {
        return EXTRA_THICK_SHAPES;
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
    public boolean isCompatibleTarget(Block block) {
        return !block.equals(BlockContent.ENERGY_PIPE_CONNECTION.get());
    }

    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level level) {
        return SUPERCONDUCTOR_DATA.computeIfAbsent(level.dimension().identifier(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }

    public static class FramedSuperConductorConnectionBlock extends SuperConductorConnectionBlock {

        public FramedSuperConductorConnectionBlock(Properties settings) {
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
