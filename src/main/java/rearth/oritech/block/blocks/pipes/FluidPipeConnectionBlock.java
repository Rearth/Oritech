package rearth.oritech.block.blocks.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.pipes.FluidPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.pipes.FluidPipeBlock.FLUID_PIPE_DATA;

public class FluidPipeConnectionBlock extends GenericPipeConnectionBlock {
    
    public static final BooleanProperty EXTRACT = BooleanProperty.of("extract");
    
    public FluidPipeConnectionBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(EXTRACT, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(EXTRACT);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        if (world.isClient) return ActionResult.SUCCESS;
        
        var oldExtract = state.get(EXTRACT);
        world.setBlockState(pos, state.with(EXTRACT, !oldExtract));
        Oritech.LOGGER.debug("changed extract to: " + !oldExtract);
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public BlockApiLookup<?, Direction> getSidesLookup() {
        return FluidStorage.SIDED;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeInterfaceEntity(pos, state);
    }
    
    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.FLUID_PIPE_CONNECTION.getDefaultState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.FLUID_PIPE.getDefaultState();
    }
    
    @Override
    public String getPipeTypeName() {
        return "fluid";
    }
    
    @Override
    public boolean connectToBlockType(Block block) {
        return block instanceof FluidPipeBlock || block instanceof FluidPipeConnectionBlock;
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world) {
        return FLUID_PIPE_DATA.getOrDefault(world.getRegistryKey().getValue(), new GenericPipeInterfaceEntity.PipeNetworkData());
    }
}
