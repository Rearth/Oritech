package rearth.oritech.block.blocks.augmenter;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.augmenter.AugmenterResearchStationBlockEntity;
import rearth.oritech.util.MultiblockMachineController;

import java.util.Objects;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;

public class AugmenterResearchStationBlock extends FacingBlock implements BlockEntityProvider {
    
    public AugmenterResearchStationBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(Properties.FACING, Direction.NORTH).with(ASSEMBLED, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.FACING, ASSEMBLED);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(Properties.FACING, ctx.getPlayerLookDirection().getOpposite());
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Override
    protected MapCodec<? extends FacingBlock> getCodec() {
        return null;
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof MultiblockMachineController machineEntity)) {
                return ActionResult.SUCCESS;
            }
            
            var wasAssembled = state.get(ASSEMBLED);
            
            if (!wasAssembled) {
                var corePlaced = machineEntity.tryPlaceNextCore(player);
                if (corePlaced) return ActionResult.SUCCESS;
            }
            
            var isAssembled = machineEntity.initMultiblock(state);
            
            // first time created
            if (isAssembled && !wasAssembled) {
                return ActionResult.SUCCESS;
            }
            
            if (!isAssembled) {
                player.sendMessage(Text.translatable("message.oritech.machine.missing_core"));
                return ActionResult.SUCCESS;
            }
            
        }
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        
        if (!world.isClient() && state.get(ASSEMBLED)) {
            
            var entity = world.getBlockEntity(pos);
            if (entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }
        }
        
        return super.onBreak(world, pos, state, player);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AugmenterResearchStationBlockEntity(pos, state);
    }
}
