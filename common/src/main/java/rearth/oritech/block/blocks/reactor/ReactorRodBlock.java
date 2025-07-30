package rearth.oritech.block.blocks.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rearth.oritech.init.BlockContent;

public class ReactorRodBlock extends BaseReactorBlock {
    
    private final int rodCount;
    private final int internalPulseCount;
    
    private static final VoxelShape SOLO_SHAPE = Block.box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape DUO_SHAPE = Block.box(2, 0, 2, 14, 16, 14);
    private static final VoxelShape QUAD_SHAPE = Block.box(1, 0, 1, 15, 16, 15);
    
    public ReactorRodBlock(Properties settings, int rodCount, int internalPulseCount) {
        super(settings);
        this.rodCount = rodCount;
        this.internalPulseCount = internalPulseCount;
        this.registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.LIT);
    }
    
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (rodCount) {
            case 1 -> SOLO_SHAPE;
            case 2 -> DUO_SHAPE;
            case 4 -> QUAD_SHAPE;
            default -> SOLO_SHAPE;
        };
    }
    
    public int getRodCount() {
        return rodCount;
    }
    
    public int getInternalPulseCount() {
        return internalPulseCount;
    }
    
    @Override
    public Block requiredStackCeiling() {
        return BlockContent.REACTOR_FUEL_PORT;
    }
}
