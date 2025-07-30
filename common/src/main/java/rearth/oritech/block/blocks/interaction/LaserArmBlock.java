package rearth.oritech.block.blocks.interaction;

import dev.architectury.registry.menu.MenuRegistry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.behavior.LaserArmBlockBehavior;
import rearth.oritech.block.behavior.LaserArmEntityBehavior;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;

import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.MultiblockMachineController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.util.TooltipHelper.addMachineTooltip;


public class LaserArmBlock extends Block implements EntityBlock {

    private static final LaserArmBlockBehavior DEFAULT_BLOCK_BEHAVIOR = new LaserArmBlockBehavior();
    public static final Map<Block, LaserArmBlockBehavior> BLOCK_BEHAVIORS = new Object2ObjectOpenHashMap<>();
    private static final LaserArmEntityBehavior DEFAULT_ENTITY_BEHAVIOR = new LaserArmEntityBehavior();
    public static final Map<EntityType<?>, LaserArmEntityBehavior> ENTITY_BEHAVIORS = new Object2ObjectOpenHashMap<>();
    
    public LaserArmBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(ASSEMBLED, false).setValue(BlockStateProperties.FACING, Direction.UP));
        LaserArmBlockBehavior.registerDefaults();
        LaserArmEntityBehavior.registerDefaults();
    }

    public static void registerBlockBehavior(Block targetBlock, LaserArmBlockBehavior behavior) {
        BLOCK_BEHAVIORS.put(targetBlock, behavior);
    }

    public static void registerEntityBehavior(EntityType<?> entityType, LaserArmEntityBehavior behavior) {
        ENTITY_BEHAVIORS.put(entityType, behavior);
    }

    public static LaserArmBlockBehavior getBehaviorForBlock(Block targetBlock) {
        return BLOCK_BEHAVIORS.getOrDefault(targetBlock, DEFAULT_BLOCK_BEHAVIOR);
    }

    public static LaserArmEntityBehavior getBehaviorForEntity(EntityType<?> targetEntityType) {
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
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);
        
        if (world.isClientSide) return;
        
        var isPowered = world.hasNeighborSignal(pos);
        
        var laserEntity = (LaserArmBlockEntity) world.getBlockEntity(pos);
        laserEntity.setRedstonePowered(isPowered);
        
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof LaserArmBlockEntity laserArm)) {
                return InteractionResult.SUCCESS;
            }
            
            var wasAssembled = state.getValue(ASSEMBLED);
            
            if (!wasAssembled) {
                var corePlaced = laserArm.tryPlaceNextCore(player);
                if (corePlaced) return InteractionResult.SUCCESS;
            }
            
            var isAssembled = laserArm.initMultiblock(state);
            
            // first time created
            if (isAssembled && !wasAssembled) {
                laserArm.triggerSetupAnimation();
                laserArm.initAddons();
                return InteractionResult.SUCCESS;
            }
            
            if (!isAssembled) {
                player.sendSystemMessage(Component.translatable("message.oritech.machine.missing_core"));
                return InteractionResult.SUCCESS;
            }
            
            laserArm.initAddons();
            MenuRegistry.openExtendedMenu((ServerPlayer) player, laserArm);
            
        }
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        
        if (!world.isClientSide()) {
            
            var entity = world.getBlockEntity(pos);
            if (state.getValue(ASSEMBLED) && entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }
            
            if (entity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }
            
            if (entity instanceof LaserArmBlockEntity storageBlock) {
                var stacks = storageBlock.inventory.heldStacks;
                for (var heldStack : stacks) {
                    if (!heldStack.isEmpty()) {
                        var itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), heldStack);
                        world.addFreshEntity(itemEntity);
                    }
                }
                
                storageBlock.inventory.heldStacks.clear();
                storageBlock.inventory.setChanged();
            }
        }
        
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaserArmBlockEntity(pos, state);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        addMachineTooltip(tooltip, this, this);
    }
}
