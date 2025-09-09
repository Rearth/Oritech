package rearth.oritech.block.blocks.processing;

import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.MachineBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.interaction.DeepDrillEntity;
import rearth.oritech.util.MultiblockMachineController;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
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
import rearth.oritech.block.base.block.MachineBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.interaction.DeepDrillEntity;
import rearth.oritech.util.MultiblockMachineController;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class MachineCoreBlock extends Block implements EntityBlock {
    
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        tooltip.add(Component.translatable("tooltip.oritech.machine_core_block").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, options);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(USED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        onBlockRemoved(state, world, pos);
        return super.playerWillDestroy(world, pos, state, player);
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
    protected void onExplosionHit(BlockState state, Level world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {
        onBlockRemoved(state, world, pos);
        super.onExplosionHit(state, world, pos, explosion, stackMerger);
    }
    
    private static void onBlockRemoved(BlockState state, LevelAccessor world, BlockPos pos) {
        if (!world.isClientSide() && state.getValue(USED) && world.getBlockEntity(pos) instanceof MachineCoreEntity coreEntity) {
            var controllerPos = coreEntity.getControllerPos();
            if (controllerPos != null && world.getBlockEntity(controllerPos) instanceof MultiblockMachineController machineEntity) {
                machineEntity.onCoreBroken(pos);
            }
        }
    }
    
    @NotNull
    public static BlockPos getControllerPos(LevelAccessor world, BlockPos pos) {
        var coreEntity = (MachineCoreEntity) world.getBlockEntity(pos);
        return Objects.requireNonNull(coreEntity).getControllerPos();
    }
    
    @Nullable
    public static BlockEntity getControllerEntity(LevelAccessor world, BlockPos pos) {
        return world.getBlockEntity(getControllerPos(world, pos));
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!state.getValue(USED)) return InteractionResult.PASS;
        
        if (!world.isClientSide) {
            var controllerPos = getControllerPos(world, pos);
            var controllerBlock = world.getBlockState(controllerPos);
            var controllerEntity = world.getBlockEntity(controllerPos);
            if (controllerEntity instanceof DeepDrillEntity deepDrill && !deepDrill.init(true)) {
                player.sendSystemMessage(Component.translatable("message.oritech.deep_drill.ore_placement"));
                return InteractionResult.SUCCESS;
            } else {
                return controllerBlock.useWithoutItem(world, player, new BlockHitResult(hit.getLocation(), hit.getDirection(), controllerPos, hit.isInside()));
            }
        }
        
        return InteractionResult.SUCCESS;
        
    }
    
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        
        if (!state.getValue(USED)) super.useItemOn(stack, state, world, pos, player, hand, hit);
        
        if (!world.isClientSide) {
            var controllerPos = getControllerPos(world, pos);
            var controllerBlock = world.getBlockState(controllerPos);
            if (controllerBlock.getBlock() instanceof MachineBlock machineBlock) {
                return machineBlock.useItemOn(stack, state, world, pos, player, hand, hit);
            } else if (controllerBlock.getBlock() instanceof RefineryModuleBlock machineBlock) {
                return machineBlock.useItemOn(stack, state, world, pos, player, hand, hit);
            }
        }
        
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCoreEntity(pos, state);
    }
}
