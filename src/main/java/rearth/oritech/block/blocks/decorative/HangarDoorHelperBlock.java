package rearth.oritech.block.blocks.decorative;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.decorative.HangarDoorBlock.*;

public class HangarDoorHelperBlock extends Block {
    
    public static final IntegerProperty PART = IntegerProperty.create("part", 1, 2);
    
    public HangarDoorHelperBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(PART, 1).setValue(SURFACE, Direction.NORTH).setValue(OPENED, false).setValue(ROTATED, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, SURFACE, OPENED, ROTATED);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return HangarDoorBlock.getClosedShape(state.getValue(SURFACE), state.getValue(ROTATED));
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPENED) ? Shapes.empty() : HangarDoorBlock.getClosedShape(state.getValue(SURFACE), state.getValue(ROTATED));
    }
    
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        var anchorPos = HangarDoorBlock.getAnchorPos(pos, state);
        var anchorState = level.getBlockState(anchorPos);
        return anchorState.is(BlockContent.HANGAR_DOOR);
    }
    
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            var anchorPos = HangarDoorBlock.getAnchorPos(pos, state);
            var anchorState = level.getBlockState(anchorPos);
            if (anchorState.is(BlockContent.HANGAR_DOOR)) {
                HangarDoorBlock.removeFullStructure(level, anchorPos, anchorState, !player.isCreative());
            }
        }
        
        return super.playerWillDestroy(level, pos, state, player);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
    
    @Override
    protected MapCodec<? extends Block> codec() {
        return null;
    }
}
