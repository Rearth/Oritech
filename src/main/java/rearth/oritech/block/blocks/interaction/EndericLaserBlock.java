package rearth.oritech.block.blocks.interaction;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.behavior.EndericLaserBlockBehavior;
import rearth.oritech.block.behavior.EndericLaserEntityBehavior;
import rearth.oritech.block.entity.interaction.EndericLaserBlockEntity;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.MultiblockMachineController;
import rearth.oritech.util.TooltipHelper;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;


public class EndericLaserBlock extends Block implements EntityBlock, TooltipProvider {

    private static final EndericLaserBlockBehavior DEFAULT_BLOCK_BEHAVIOR = new EndericLaserBlockBehavior();
    public static final Map<Block, EndericLaserBlockBehavior> BLOCK_BEHAVIORS = new Object2ObjectOpenHashMap<>();
    private static final EndericLaserEntityBehavior DEFAULT_ENTITY_BEHAVIOR = new EndericLaserEntityBehavior();
    public static final Map<EntityType<?>, EndericLaserEntityBehavior> ENTITY_BEHAVIORS = new Object2ObjectOpenHashMap<>();

    public EndericLaserBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(ASSEMBLED, false).setValue(BlockStateProperties.FACING, Direction.UP));
    }

    public static void registerBlockBehavior(Block targetBlock, EndericLaserBlockBehavior behavior) {
        BLOCK_BEHAVIORS.put(targetBlock, behavior);
    }

    public static void registerEntityBehavior(EntityType<?> entityType, EndericLaserEntityBehavior behavior) {
        ENTITY_BEHAVIORS.put(entityType, behavior);
    }

    public static EndericLaserBlockBehavior getBehaviorForBlock(Block targetBlock) {
        return BLOCK_BEHAVIORS.getOrDefault(targetBlock, DEFAULT_BLOCK_BEHAVIOR);
    }

    public static EndericLaserEntityBehavior getBehaviorForEntity(EntityType<?> targetEntityType) {
        return ENTITY_BEHAVIORS.getOrDefault(targetEntityType, DEFAULT_ENTITY_BEHAVIOR);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.FACING, ctx.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ASSEMBLED);
        builder.add(BlockStateProperties.FACING);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @org.jspecify.annotations.Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        if (level.isClientSide()) return;

        var isPowered = level.hasNeighborSignal(pos);

        var laserEntity = (EndericLaserBlockEntity) level.getBlockEntity(pos);
        laserEntity.setRedstonePowered(isPowered);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {

            var entity = level.getBlockEntity(pos);
            if (!(entity instanceof EndericLaserBlockEntity endericLaser)) {
                return InteractionResult.SUCCESS;
            }

            var wasAssembled = state.getValue(ASSEMBLED);

            if (!wasAssembled) {
                var corePlaced = endericLaser.tryPlaceNextCore(player);
                if (corePlaced) return InteractionResult.SUCCESS;
            }

            var isAssembled = endericLaser.initMultiblock(state);

            // first time created
            if (isAssembled && !wasAssembled) {
                endericLaser.initAddons();
                return InteractionResult.SUCCESS;
            }

            if (!isAssembled) {
                player.sendSystemMessage(Component.translatable("message.oritech.machine.missing_core"));
                return InteractionResult.SUCCESS;
            }

            endericLaser.initAddons();
            player.openMenu(endericLaser, pos);

        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {

        if (!level.isClientSide()) {

            var entity = level.getBlockEntity(pos);
            if (state.getValue(ASSEMBLED) && entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }

            if (entity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }

            if (entity instanceof EndericLaserBlockEntity storageBlock) {
                var stacks = storageBlock.inventory.getStacks();
                for (var heldStack : stacks) {
                    if (!heldStack.isEmpty()) {
                        var itemEntity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), heldStack);
                        level.addFreshEntity(itemEntity);
                    }
                }

                storageBlock.inventory.getStacks().clear();
                storageBlock.setChanged();
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EndericLaserBlockEntity(pos, state);
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

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        TooltipHelper.addMachineTooltip(consumer, this, this);
    }
}
