package rearth.oritech.block.blocks.storage;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.entity.storage.LargeStorageBlockEntity;
import rearth.oritech.util.MultiblockMachineController;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;


public class LargeStorageBlock extends SmallStorageBlock {
    
    public LargeStorageBlock(Properties settings) {
        super(settings.lightLevel(value -> 2));
        registerDefaultState(defaultBlockState().setValue(ASSEMBLED, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ASSEMBLED);
    }
    
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeStorageBlockEntity(pos, state);
    }
    
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(TARGET_DIR, ctx.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof MultiblockMachineController machineEntity)) {
                return InteractionResult.SUCCESS;
            }
            
            var wasAssembled = state.getValue(ASSEMBLED);
            
            if (!wasAssembled) {
                var corePlaced = machineEntity.tryPlaceNextCore(player);
                if (corePlaced) return InteractionResult.SUCCESS;
            }
            
            var isAssembled = machineEntity.initMultiblock(state);
            
            // first time created
            if (isAssembled && !wasAssembled) {
                // NetworkContent.MACHINE_CHANNEL.serverHandle(machineEntity).send(new NetworkContent.MachineSetupEventPacket(pos));
                return InteractionResult.SUCCESS;
            }
            
            if (!isAssembled) {
                player.sendSystemMessage(Component.translatable("message.oritech.machine.missing_core"));
                return InteractionResult.SUCCESS;
            }
            
        }
        
        return super.useWithoutItem(state, world, pos, player, hit);
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        
        if (!world.isClientSide() && state.getValue(ASSEMBLED)) {
            
            var entity = world.getBlockEntity(pos);
            if (entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }
            
            if (entity instanceof ExpandableEnergyStorageBlockEntity storageBlock) {
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
}