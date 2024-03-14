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
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        if (world.isClient) return ActionResult.SUCCESS;
        
        var oldExtract = state.get(EXTRACT);
        world.setBlockState(pos, state.with(EXTRACT, !oldExtract));
        System.out.println("changed extract to: " + !oldExtract);
        
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
    public BlockState getConnectionBlock() {
        return BlockContent.ITEM_PIPE_CONNECTION.getDefaultState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.ITEM_PIPE.getDefaultState();
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
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData() {
        return ITEM_PIPE_DATA;
    }
}
