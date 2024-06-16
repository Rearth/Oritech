package rearth.oritech.block.blocks.machines.storage;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.entity.machines.storage.SmallStorageBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.MachineAddonController;

import java.util.List;
import java.util.Objects;

import static rearth.oritech.util.TooltipHelper.addMachineTooltip;

public class SmallStorageBlock extends Block implements BlockEntityProvider {
    
    public static final DirectionProperty TARGET_DIR = DirectionProperty.of("target_dir");
    
    public SmallStorageBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(TARGET_DIR, Direction.NORTH));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(TARGET_DIR);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(TARGET_DIR, ctx.getPlayerLookDirection().getOpposite());
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SmallStorageBlockEntity(pos, state);
    }
    
    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }
    
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        
        if (world.isClient) return;
        
        var isPowered = world.isReceivingRedstonePower(pos);
        
        var storageEntity = (ExpandableEnergyStorageBlockEntity) world.getBlockEntity(pos);
        storageEntity.setRedstonePowered(isPowered);
        
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof MachineAddonController machineEntity)) {
                return ActionResult.SUCCESS;
            }
            
            machineEntity.initAddons();
            
            var handler = (ExtendedScreenHandlerFactory) world.getBlockEntity(pos);
            player.openHandledScreen(handler);
            
        }
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        
        if (!world.isClient()) {
            
            var entity = world.getBlockEntity(pos);
            if (entity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }
            
            if (entity instanceof ExpandableEnergyStorageBlockEntity storageBlock) {
                var stacks = storageBlock.inventory.heldStacks;
                for (var heldStack : stacks) {
                    if (!heldStack.isEmpty()) {
                        var itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), heldStack);
                        world.spawnEntity(itemEntity);
                    }
                }
                
                // drop block with nbt
                if (state.getBlock().equals(BlockContent.SMALL_STORAGE_BLOCK)) {
                    var stack = new ItemStack(BlockContent.SMALL_STORAGE_BLOCK.asItem());
                    var storedAmount = storageBlock.getStorageForAddon().amount;
                    if (storedAmount > 0) {
                        var nbt = new NbtCompound();
                        storageBlock.writeNbt(nbt);
                        stack.setNbt(nbt);
                    }
                    if (!player.isCreative())
                        Block.dropStack(world, pos, stack);
                }
            }
        }
        
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        
        var storageEntity = (ExpandableEnergyStorageBlockEntity) world.getBlockEntity(pos);
        var nbt = itemStack.getNbt();
        if (nbt != null)
            storageEntity.readNbt(nbt);
        
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, world, tooltip, options);
        
        if (stack.hasNbt()) {
            var storedEnergy = stack.getNbt().getLong("energy_stored");
            if (storedEnergy != 0) {
                var text = Text.literal(String.format("Stored: %d RF", storedEnergy));
                tooltip.add(text.formatted(Formatting.GOLD));
            }
        }
        
        addMachineTooltip(tooltip, this, this);
    }
    
}
