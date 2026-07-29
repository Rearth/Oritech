package rearth.oritech.block.blocks.processing;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.fluid.FluidContainerInteraction;
import rearth.oritech.block.base.block.MachineBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.interaction.BedrockExtractorEntity;
import rearth.oritech.util.MultiblockMachineController;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MachineCoreBlock extends Block implements EntityBlock, TooltipProvider {

    public static final BooleanProperty USED = BooleanProperty.create("core_used");

    private final float coreQuality;

    public MachineCoreBlock(Properties settings, float coreQuality) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(USED, false));
        this.coreQuality = coreQuality;
    }

    public float getCoreQuality() {
        return coreQuality;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(USED);
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        consumer.accept(Component.translatable("tooltip.oritech.machine_core_block").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(USED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        onBlockRemoved(state, level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        onBlockRemoved(state, level, pos);
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        onBlockRemoved(state, level, pos);
        super.destroy(level, pos, state);
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {

        if (state.getValue(USED)) {
            var controller = getControllerPos(level, pos);
            var controllerState = level.getBlockState(controller);
            onBlockRemoved(state, level, pos);

            // forward explosion to refinery
            if (controllerState.getBlock() instanceof RefineryBlock refinery) {
                refinery.onExplosionHit(controllerState, level, controller, explosion, onHit);
                return;
            }
        }
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    private static void onBlockRemoved(BlockState state, LevelAccessor level, BlockPos pos) {
        if (!level.isClientSide() && state.getValue(USED) && level.getBlockEntity(pos) instanceof MachineCoreEntity coreEntity) {
            var controllerPos = coreEntity.getControllerPos();
            if (controllerPos != null && level.getBlockEntity(controllerPos) instanceof MultiblockMachineController machineEntity) {
                machineEntity.onCoreBroken(pos);
            }
        }
    }

    @NotNull
    public static BlockPos getControllerPos(LevelAccessor level, BlockPos pos) {
        var coreEntity = (MachineCoreEntity) level.getBlockEntity(pos);
        return Objects.requireNonNull(coreEntity).getControllerPos();
    }

    @Nullable
    public static BlockEntity getControllerEntity(LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(getControllerPos(level, pos));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!state.getValue(USED)) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            var controllerPos = getControllerPos(level, pos);
            var controllerBlock = level.getBlockState(controllerPos);
            var controllerEntity = level.getBlockEntity(controllerPos);
            if (controllerEntity instanceof BedrockExtractorEntity bedrockExtractor && !bedrockExtractor.init(true)) {
                player.sendSystemMessage(Component.translatable("message.oritech.bedrock_extractor.ore_placement"));
                return InteractionResult.SUCCESS;
            } else {
                return controllerBlock.useWithoutItem(level, player, new BlockHitResult(hit.getLocation(), hit.getDirection(), controllerPos, hit.isInside()));
            }
        }

        return InteractionResult.SUCCESS;

    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (!state.getValue(USED)) return super.useItemOn(stack, state, level, pos, player, hand, hit);

        var controllerPos = getControllerPos(level, pos);
        var controllerEntity = level.getBlockEntity(controllerPos);
        if (FluidContainerInteraction.tryFluidBlockItemInteraction(level, controllerPos, controllerEntity, player, hand)) {
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide()) {
            var controllerState = level.getBlockState(controllerPos);
            var forwardedHit = new BlockHitResult(hit.getLocation(), hit.getDirection(), controllerPos, hit.isInside());

            if (controllerState.getBlock() instanceof MachineBlock machineBlock) {
                return machineBlock.useItemOn(stack, controllerState, level, controllerPos, player, hand, forwardedHit);
            } else if (controllerState.getBlock() instanceof RefineryChamberModuleBlock machineBlock) {
                return machineBlock.useItemOn(stack, controllerState, level, controllerPos, player, hand, forwardedHit);
            }
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCoreEntity(pos, state);
    }
}
