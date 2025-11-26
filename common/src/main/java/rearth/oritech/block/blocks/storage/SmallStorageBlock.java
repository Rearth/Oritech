package rearth.oritech.block.blocks.storage;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.entity.storage.SmallStorageBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.ComparatorOutputProvider;
import rearth.oritech.util.MachineAddonController;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import static rearth.oritech.util.TooltipHelper.addMachineTooltip;


public class SmallStorageBlock extends Block implements EntityBlock {
    
    public static final DirectionProperty TARGET_DIR = DirectionProperty.create("target_dir");
    
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
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return ((ComparatorOutputProvider) world.getBlockEntity(pos)).getComparatorOutput();
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
        
        var storageEntity = (ExpandableEnergyStorageBlockEntity) world.getBlockEntity(pos);
        storageEntity.setRedstonePowered(isPowered);
        
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof MachineAddonController machineEntity)) {
                return InteractionResult.SUCCESS;
            }
            
            machineEntity.initAddons();
            
            var handler = (ExtendedMenuProvider) world.getBlockEntity(pos);
                MenuRegistry.openExtendedMenu((ServerPlayer) player, handler);
            
        }
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var droppedStacks = super.getDrops(state, builder);

        var blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof SmallStorageBlockEntity storageEntity) {
            droppedStacks.addAll(storageEntity.inventory.getHeldStacks());
            storageEntity.inventory.clearContent();
        }

        return droppedStacks;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        return getStackWithData(world, pos);
    }
    
    @NotNull
    private static ItemStack getStackWithData(LevelReader world, BlockPos pos) {
        var stack = new ItemStack(BlockContent.SMALL_STORAGE_BLOCK.asItem());
        
        var storageEntity = (SmallStorageBlockEntity) world.getBlockEntity(pos);
        if (storageEntity.getEnergyStorage(null).getAmount() > 0) {
            stack.set(EnergyApi.ITEM.getEnergyComponent(), storageEntity.getEnergyStorage(null).getAmount());
        }
        
        return stack;
    }
    
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        
        var storedEnergyInStack = itemStack.getOrDefault(EnergyApi.ITEM.getEnergyComponent(), 0L);
        
        if (storedEnergyInStack > 0) {
            var storageEntity = (ExpandableEnergyStorageBlockEntity) world.getBlockEntity(pos);
            storageEntity.energyStorage.setAmount(storedEnergyInStack);
        }
        
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
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        
        if (!world.isClientSide) {
            var entity = world.getBlockEntity(pos);
            if (entity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }
        }
        
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        
        addMachineTooltip(tooltip, this, this);
    }
    
}
