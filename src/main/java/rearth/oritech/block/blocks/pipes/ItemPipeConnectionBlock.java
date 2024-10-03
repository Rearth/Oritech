package rearth.oritech.block.blocks.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.pipes.ItemPipeBlock.ITEM_PIPE_DATA;

public class ItemPipeConnectionBlock extends GenericPipeConnectionBlock {
    
    public static final BooleanProperty EXTRACT = BooleanProperty.of("extract");
    
    public ItemPipeConnectionBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(EXTRACT, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(EXTRACT);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (world.isClient) return ActionResult.SUCCESS;
        
        var oldExtract = state.get(EXTRACT);
        world.setBlockState(pos, state.with(EXTRACT, !oldExtract));
        Oritech.LOGGER.debug("changed extract to: " + !oldExtract);
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public BlockApiLookup<?, Direction> getSidesLookup() {
        return ItemStorage.SIDED;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeInterfaceEntity(pos, state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        var baseState = super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);

        return baseState.with(EXTRACT, state.get(EXTRACT));
    }
    
    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.ITEM_PIPE_CONNECTION.getDefaultState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.ITEM_PIPE.getDefaultState();
    }

    @Override
    protected VoxelShape[] createShapes() {
        VoxelShape inner = Block.createCuboidShape(6, 6, 6, 10, 10, 10);
        VoxelShape north = Block.createCuboidShape(6, 6, 0, 10, 10, 6);
        VoxelShape east = Block.createCuboidShape(0, 6, 6, 6, 10, 10);
        VoxelShape south = Block.createCuboidShape(6, 6, 10, 10, 10, 16);
        VoxelShape west = Block.createCuboidShape(10, 6, 6, 16, 10, 10);
        VoxelShape up = Block.createCuboidShape(6, 10, 6, 10, 16, 10);
        VoxelShape down = Block.createCuboidShape(6, 0, 6, 10, 6, 10);
        
        return new VoxelShape[]{inner, north, west, south, east, up, down};
    }
    
    @Override
    public String getPipeTypeName() {
        return "item";
    }
    
    @Override
    public boolean connectToBlockType(Block block) {
        return block instanceof ItemPipeBlock || block instanceof ItemPipeConnectionBlock;
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world) {
        return ITEM_PIPE_DATA.getOrDefault(world.getRegistryKey().getValue(), new GenericPipeInterfaceEntity.PipeNetworkData());
    }
}
