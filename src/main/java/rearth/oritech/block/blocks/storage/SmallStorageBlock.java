package rearth.oritech.block.blocks.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.entity.storage.SmallStorageBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.ComparatorOutputProvider;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.TooltipHelper;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


public class SmallStorageBlock extends Block implements EntityBlock, TooltipProvider {

    public static final EnumProperty<Direction> TARGET_DIR = BlockStateProperties.FACING;

    public SmallStorageBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(TARGET_DIR, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TARGET_DIR);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(TARGET_DIR, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmallStorageBlockEntity(pos, state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return ((ComparatorOutputProvider) level.getBlockEntity(pos)).getComparatorOutput();
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        if (level.isClientSide()) return;

        var isPowered = level.hasNeighborSignal(pos);

        var storageEntity = (ExpandableEnergyStorageBlockEntity) level.getBlockEntity(pos);
        storageEntity.setRedstonePowered(isPowered);

    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {

            var entity = level.getBlockEntity(pos);
            if (!(entity instanceof MachineAddonController machineEntity)) {
                return InteractionResult.SUCCESS;
            }

            machineEntity.initAddons();

            player.openMenu((MenuProvider) level.getBlockEntity(pos), pos);

        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var droppedStacks = super.getDrops(state, builder);

        var blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof SmallStorageBlockEntity storageEntity) {
            droppedStacks.addAll(storageEntity.inventory.getStacks());
            storageEntity.inventory.getStacks().clear();
        }

        return droppedStacks;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return getStackWithData(level, pos);
    }

    @NotNull
    private static ItemStack getStackWithData(LevelReader level, BlockPos pos) {
        var stack = new ItemStack(BlockContent.SMALL_STORAGE_BLOCK.asItem());

        var storageEntity = (SmallStorageBlockEntity) level.getBlockEntity(pos);
        if (storageEntity.energyStorage.energy > 0) {
            stack.set(ComponentContent.ENERGY.get(), (int) storageEntity.energyStorage.energy);
        }

        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, placer, itemStack);

        var storedEnergyInStack = itemStack.getOrDefault(ComponentContent.ENERGY, 0);

        if (storedEnergyInStack > 0) {
            var storageEntity = (ExpandableEnergyStorageBlockEntity) level.getBlockEntity(pos);
            storageEntity.energyStorage.set(storedEnergyInStack);
        }

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
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {

        if (!level.isClientSide()) {
            var entity = level.getBlockEntity(pos);
            if (entity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {

        TooltipHelper.addMachineTooltip(consumer, this, this);
    }

}
