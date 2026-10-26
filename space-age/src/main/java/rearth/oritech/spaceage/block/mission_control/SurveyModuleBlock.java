package rearth.oritech.spaceage.block.mission_control;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SurveyModuleBlock extends Block implements EntityBlock {

    public static final VoxelShape ANTENNA_SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 1, 13),
            Block.box(5, 1, 5, 11, 3, 11),
            Block.box(3.5, .5, 3.5, 4.5, 1.5, 4.5),
            Block.box(11.5, .5, 3.5, 12.5, 1.5, 4.5),
            Block.box(3.5, .5, 11.5, 4.5, 1.5, 12.5),
            Block.box(11.5, .5, 11.5, 12.5, 1.5, 12.5),
            Block.box(7, 3, 7, 9, 16, 9),
            Block.box(0, 14, 7.75, 16, 16, 8.25),
            Block.box(0, 10, 7.75, 16, 12, 8.25),
            Block.box(0, 6, 7.75, 16, 8, 8.25));

    public static final VoxelShape SCANNER_SHAPE = Shapes.or(
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

    private final VoxelShape shape;

    public SurveyModuleBlock(Properties properties) {

        this(properties, Shapes.block());
    }

    public SurveyModuleBlock(Properties properties, VoxelShape shape) {

        super(properties);
        this.shape = shape;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return new GroundStationBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {

        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer != null && level.getBlockEntity(pos) instanceof GroundStationBlockEntity station) {
            station.owner = placer.getUUID();
            station.setChanged();
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {

        return level.isClientSide() ? null : (world, pos, block, entity) -> ((GroundStationBlockEntity) entity).tick();
    }
}
