package rearth.oritech.block.blocks.machines.accelerator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.accelerator.AcceleratorControllerBlockEntity;

import java.util.Objects;

public class AcceleratorRingBlock extends AcceleratorPassthroughBlock {
    
    public static final IntProperty BENT = IntProperty.of("bent", 0, 2);    // 0 = straight, 1 = left, 2 = right
    public static final IntProperty REDSTONE_STATE = IntProperty.of("redstone_state", 0, 3);    // 0-2 = same as bent, 3 = was never powered
    
    public AcceleratorRingBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(BENT, 0).with(REDSTONE_STATE, 3));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(BENT, REDSTONE_STATE);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(BENT, 0).with(REDSTONE_STATE, 3);
    }
    
    // allow redstone to connect
    @Override
    protected boolean emitsRedstonePower(BlockState state) {
        return true;
    }
    
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        
        if (world.isClient) return;
        
        var isPowered = world.isReceivingRedstonePower(pos);
        var lastRedstone = state.get(REDSTONE_STATE);
        var lastBent = state.get(BENT);
        
        // straight pipes don't react to redstone
        if (lastBent == 0 && lastRedstone == 3) return;
        
        // on new redstone signal (redstone stored is not bent)
        if (isPowered && (lastRedstone == 0 || lastRedstone == 3)) {
            // store bent state and set straight
            world.setBlockState(pos, state.with(REDSTONE_STATE, lastBent).with(BENT, 0), Block.NOTIFY_LISTENERS, 1);
            AcceleratorControllerBlockEntity.resetCachedGate(pos);
        } else if (!isPowered && lastRedstone != 3 && lastRedstone != 0) {   // on redstone disabled
            // set bent to lastbent, set redstone to straight
            world.setBlockState(pos, state.with(REDSTONE_STATE, 0).with(BENT, lastRedstone), Block.NOTIFY_LISTENERS, 1);
            AcceleratorControllerBlockEntity.resetCachedGate(pos);
        }
        
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        var newBent = (state.get(BENT) + 1) % 3;
        world.setBlockState(pos, state.with(BENT, newBent).with(REDSTONE_STATE, 3));
        AcceleratorControllerBlockEntity.resetCachedGate(pos);
        
        return ActionResult.SUCCESS;
    }
}
