package rearth.oritech.spaceage.block.mission_control;

import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class SpaceScannerBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<SpaceScannerBlock> CODEC = simpleCodec(SpaceScannerBlock::new);
    private static final VoxelShape FLOOR_SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15),
            Block.box(2, 2, 2, 14, 5, 14),
            Block.box(1, 5, 1, 15, 7, 15),
            Block.box(6, 0, 13, 10, 8, 16),
            Block.box(0, 0, 6, 3, 8, 10),
            Block.box(6, 0, 0, 10, 8, 3),
            Block.box(13, 0, 6, 16, 8, 10),
            Block.box(3, 7, 3, 13, 9, 13),
            Block.box(7, 9, 7, 9, 15, 9),
            Block.box(7.75, 12, 4, 8.25, 14, 12),
            Block.box(4, 12, 7.75, 12, 14, 8.25),
            Block.box(3, 10, 7.5, 4, 16, 8.5),
            Block.box(7.5, 10, 3, 8.5, 16, 4),
            Block.box(12, 10, 7.5, 13, 16, 8.5),
            Block.box(7.5, 10, 12, 8.5, 16, 13));
    // The shape starts on the floor, while vanilla's attachment helper expects a wall shape.
    private static final Map<AttachFace, Map<Direction, VoxelShape>> SHAPES = Shapes.rotateAttachFace(FLOOR_SHAPE, OctahedralGroup.BLOCK_ROT_X_90);

    public SpaceScannerBlock(Properties properties) {

        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACE, AttachFace.FLOOR).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<SpaceScannerBlock> codec() {

        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(FACE, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return SHAPES.get(state.getValue(FACE)).get(state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return new GroundStationBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {

        super.setPlacedBy(level, pos, state, placer, stack);
        SurveyModuleBlock.assignOwner(level, pos, placer);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {

        return SurveyModuleBlock.stationTicker(level);
    }
}
