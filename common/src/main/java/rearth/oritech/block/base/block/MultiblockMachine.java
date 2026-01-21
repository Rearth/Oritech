package rearth.oritech.block.base.block;

import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.MultiblockMachineController;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public abstract class MultiblockMachine extends UpgradableMachineBlock {
    
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("machine_assembled");
    
    public MultiblockMachine(Properties settings) {
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
            
            // first time created
            if (isAssembled && !wasAssembled) {
                machineEntity.triggerSetupAnimation();
                if (entity instanceof MachineAddonController controllerEntity)
                    controllerEntity.initAddons();
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
    public @NotNull BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        resetMultiblock(state, world, pos);
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        resetMultiblock(state, level, pos);
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
    
    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        resetMultiblock(state, level, pos);
        super.destroy(level, pos, state);
    }
    
    @Override
    protected void onExplosionHit(BlockState state, Level world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {
        resetMultiblock(state, world, pos);
        super.onExplosionHit(state, world, pos, explosion, stackMerger);
    }
    
    private void resetMultiblock(BlockState state, LevelAccessor world, BlockPos pos) {
        if (!world.isClientSide() && state.getValue(ASSEMBLED)) {
            var entity = world.getBlockEntity(pos);
            if (entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }
        }
    }
}
