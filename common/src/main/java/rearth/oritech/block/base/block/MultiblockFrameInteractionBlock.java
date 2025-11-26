package rearth.oritech.block.base.block;

import rearth.oritech.util.MultiblockMachineController;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public abstract class MultiblockFrameInteractionBlock extends FrameInteractionBlock {
    
    public MultiblockFrameInteractionBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(ASSEMBLED, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ASSEMBLED);
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
            if (!isAssembled) {
                player.sendSystemMessage(Component.translatable("message.oritech.machine.missing_core"));
                return InteractionResult.SUCCESS;
            }
            
            state = state.setValue(ASSEMBLED, true);
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
        }
        
        return super.playerWillDestroy(world, pos, state, player);
    }
}
