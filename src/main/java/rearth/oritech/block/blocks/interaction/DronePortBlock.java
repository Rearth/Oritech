package rearth.oritech.block.blocks.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.fluid.FluidContainerInteraction;
import rearth.oritech.block.entity.interaction.DronePortEntity;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.MultiblockMachineController;
import rearth.oritech.util.TooltipHelper;

import java.util.Objects;
import java.util.function.Consumer;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;


public class DronePortBlock extends Block implements EntityBlock, TooltipProvider {

    public DronePortBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(ASSEMBLED, false).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ASSEMBLED, BlockStateProperties.HORIZONTAL_FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {

            var entity = level.getBlockEntity(pos);
            if (!(entity instanceof DronePortEntity dronePort)) {
                return InteractionResult.SUCCESS;
            }

            var wasAssembled = state.getValue(ASSEMBLED);

            if (!wasAssembled) {
                var corePlaced = dronePort.tryPlaceNextCore(player);
                if (corePlaced) return InteractionResult.SUCCESS;
            }

            var isAssembled = dronePort.initMultiblock(state);

            // first time created
            if (isAssembled && !wasAssembled) {
                dronePort.triggerSetupAnimation();
                dronePort.initAddons();
                return InteractionResult.SUCCESS;
            }

            if (!isAssembled) {
                player.sendSystemMessage(Component.translatable("message.oritech.machine.missing_core"));
                return InteractionResult.SUCCESS;
            }

            dronePort.initAddons();
            player.openMenu((MenuProvider) level.getBlockEntity(pos), pos);

        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (FluidContainerInteraction.tryFluidBlockItemInteraction(level, pos, player, hand)) {
            return InteractionResult.SUCCESS;
        }

        return super.useItemOn(itemStack, state, level, pos, player, hand, hit);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {

        if (!level.isClientSide() && state.getValue(ASSEMBLED)) {

            var entity = level.getBlockEntity(pos);
            if (entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }


            if (entity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }

            if (entity instanceof DronePortEntity storageBlock) {
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
        return new DronePortEntity(pos, state);
    }

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
