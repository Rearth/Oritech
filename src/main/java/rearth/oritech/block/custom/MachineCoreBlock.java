package rearth.oritech.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MachineCoreBlock extends Block {
    
    public static final BooleanProperty USED = BooleanProperty.of("core_used");
    
    public MachineCoreBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(USED, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(USED);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return state.get(USED) ? BlockRenderType.INVISIBLE : BlockRenderType.MODEL;
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        if (!world.isClient) {
            world.setBlockState(pos, state.with(USED, !state.get(USED)));
        }
        
        return ActionResult.SUCCESS;
    }
}
