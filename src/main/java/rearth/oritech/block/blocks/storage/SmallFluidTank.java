package rearth.oritech.block.blocks.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.fluid.FluidContainerInteraction;
import rearth.oritech.block.entity.storage.SmallTankEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.ComparatorOutputProvider;

import java.util.List;

public class SmallFluidTank extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SmallFluidTank(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmallTankEntity(pos, state, false);
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
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {
            player.openMenu((MenuProvider) level.getBlockEntity(pos), pos);

        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (FluidContainerInteraction.tryFluidBlockItemInteraction(level, pos, player, hand)) {
            return InteractionResult.SUCCESS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var droppedStacks = super.getDrops(state, builder);

        var blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof SmallTankEntity tankEntity) {
            droppedStacks.addAll(tankEntity.inventory.getStacks());
            tankEntity.inventory.getStacks().clear();
        }

        return droppedStacks;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return getStackWithData(level, pos);
    }

    @NotNull
    private static ItemStack getStackWithData(LevelReader level, BlockPos pos) {
        var tankEntity = (SmallTankEntity) level.getBlockEntity(pos);
        var stack = getBasePickStack(tankEntity.isCreative);

        if (tankEntity.fluidStorage.getAmount() > 0) {
            var fluidStack = tankEntity.fluidStorage.getContent().copy();
            stack.set(ComponentContent.STORED_FLUID, SimpleFluidContent.copyOf(fluidStack));
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        }

        return stack;
    }

    public static ItemStack getBasePickStack(boolean creative) {
        return new ItemStack(creative ? BlockContent.CREATIVE_TANK_BLOCK.asItem() : BlockContent.SMALL_TANK_BLOCK.asItem());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, placer, itemStack);

        if (itemStack.has(ComponentContent.STORED_FLUID)) {
            var fluidStack = itemStack.get(ComponentContent.STORED_FLUID);
            var tankEntity = (SmallTankEntity) level.getBlockEntity(pos);
            tankEntity.fluidStorage.set(0, FluidResource.of(fluidStack.getFluid()), fluidStack.getAmount());
            tankEntity.setChanged();
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

}
