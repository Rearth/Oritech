package rearth.oritech.block.blocks.storage;

import rearth.oritech.util.CollisionShapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.storage.SchrodingersSafeBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;

public class SchrodingersSafeBlock extends Block implements EntityBlock {

    public static final BooleanProperty SETUP_DONE = BooleanProperty.create("setup");

    // Use the authored shape for both targeting and vanilla collision; keep the existing fallback.
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var shape = CollisionShapes.getShape(state, level, pos);
        return shape != null ? shape : super.getShape(state, level, pos, context);
    }

    public SchrodingersSafeBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(ASSEMBLED, false).setValue(SETUP_DONE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ASSEMBLED);
        builder.add(SETUP_DONE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SchrodingersSafeBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {

        super.setPlacedBy(level, pos, state, placer, itemStack);

        if (level.isClientSide()) {
            return;
        }

        var machineCandidate = level.getBlockEntity(pos, BlockEntitiesContent.SCHRODINGERS_SAFE_BLOCK.get());
        if (machineCandidate.isEmpty()) return;
        var machine = machineCandidate.get();
        var corePositions = machine.getCorePositions();

        for (var coreOffset : corePositions) {
            var coreWorldPos = pos.offset(coreOffset);
            var coreState = level.getBlockState(coreWorldPos);
            if (!coreState.isAir()) {
                var breakingPlayer = placer instanceof Player ? (Player) placer : null;
                coreState.getBlock().playerWillDestroy(level, coreWorldPos, coreState, breakingPlayer);
                level.destroyBlock(coreWorldPos, true, placer, 1);
            }
            level.setBlockAndUpdate(coreWorldPos, BlockContent.COMPLEX_PLATING.get().defaultBlockState());
        }

        machine.initMultiblock(state);

    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {
            player.openMenu((MenuProvider) level.getBlockEntity(pos), pos);
        }

        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
}
