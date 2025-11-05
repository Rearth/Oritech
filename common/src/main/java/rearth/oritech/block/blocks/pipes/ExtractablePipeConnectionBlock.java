package rearth.oritech.block.blocks.pipes;

import rearth.oritech.block.entity.pipes.ExtractablePipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity.PipeNetworkData;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.SoundContent;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public abstract class ExtractablePipeConnectionBlock extends GenericPipeConnectionBlock {
    
    public static final int EXTRACT = 2;
    
    // 0 = no connection, 1 = normal connection, 2 = extractable connection
    public static final IntegerProperty NORTH = IntegerProperty.create("north", 0, 2);
    public static final IntegerProperty EAST = IntegerProperty.create("east", 0, 2);
    public static final IntegerProperty SOUTH = IntegerProperty.create("south", 0, 2);
    public static final IntegerProperty WEST = IntegerProperty.create("west", 0, 2);
    public static final IntegerProperty UP = IntegerProperty.create("up", 0, 2);
    public static final IntegerProperty DOWN = IntegerProperty.create("down", 0, 2);
    
    public ExtractablePipeConnectionBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isHolding(ItemContent.WRENCH)) return InteractionResult.PASS;
        if (world.isClientSide) return InteractionResult.SUCCESS;
        
        var interactDir = getInteractDirection(state, pos, player);
        if (!hasMachineInDirection(interactDir, world, pos, apiValidationFunction()))
            return InteractionResult.PASS;
        
        var property = directionToProperty(interactDir);
        var connection = state.getValue(property);
        world.setBlock(pos, state.setValue(property, connection != EXTRACT ? EXTRACT : CONNECTION), Block.UPDATE_KNOWN_SHAPE, 0);
        
        world.playSound(null, pos, SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.9f, 1.2f);
        
        // Invalidate cache
        invalidateTargetCache(world, pos);
        
        return InteractionResult.SUCCESS;
    }
    
    public boolean hasExtractingSide(BlockState state) {
        for (var direction : UPDATE_SHAPE_ORDER) {
            var property = directionToProperty(direction);
            if (state.getValue(property) == EXTRACT) return true;
        }
        
        return false;
    }
    
    /**
     * Invalidates the target cache of the block entity at the given position
     *
     * @param world the world
     * @param pos   the position
     */
    protected void invalidateTargetCache(Level world, BlockPos pos) {
        var data = getNetworkData(world);
        var network = data.pipeNetworkLinks.getOrDefault(pos, null);
        if (network != null) {
            var checked = new HashSet<BlockPos>();
            
            // Invalidate all pipe connection nodes in the network
            for (var pipeInterface : data.pipeNetworkInterfaces.get(network)) {
                // Skip node if already checked (node has multiple interface connections)
                var pipePos = pipeInterface.getA().relative(pipeInterface.getB());
                if (checked.contains(pipePos)) continue;
                
                checked.add(pipePos);
                var pipeEntity = world.getBlockEntity(pipePos);
                if (pipeEntity instanceof ExtractablePipeInterfaceEntity)
                    ((ExtractablePipeInterfaceEntity) pipeEntity).invalidateTargetCache();
            }
        }
    }
    
    @Override
    public BlockState addConnectionStates(BlockState state, Level world, BlockPos pos, boolean createConnection) {
        
        state = addFluidState(state, pos, world);
        
        for (var direction : Direction.values()) {
            var property = directionToProperty(direction);
            var connection = shouldConnect(state, direction, pos, world, createConnection);
            
            if (connection && state.getValue(property) == EXTRACT) continue; // don't override extractable connections
            state = state.setValue(property, connection ? CONNECTION : NO_CONNECTION);
        }
        
        return addStraightState(state);
    }
    
    @Override
    public BlockState addConnectionStates(BlockState state, Level world, BlockPos pos, Direction createDirection) {
        
        state = addFluidState(state, pos, world);
        
        for (var direction : Direction.values()) {
            var property = directionToProperty(direction);
            var connection = shouldConnect(state, direction, pos, world, direction.equals(createDirection));
            var newValue = connection ? isSideExtractable(state, direction) ? EXTRACT : CONNECTION : NO_CONNECTION;
            state = state.setValue(property, newValue);
        }
        return addStraightState(state);
    }
    
    /**
     * Checks if the block state is extractable from any side
     *
     * @param state the block state
     * @return true if the block state is extractable from any side
     */
    public boolean isExtractable(BlockState state) {
        for (Direction side : Direction.values()) {
            if (isSideExtractable(state, side))
                return true;
        }
        
        return false;
    }
    
    /**
     * Checks if the block state is extractable from a specific side
     *
     * @param state the block state
     * @param side  the side to check
     * @return true if the block state is extractable from the side
     */
    public boolean isSideExtractable(BlockState state, Direction side) {
        return directionToPropertyValue(state, side) == EXTRACT;
    }
    
    @Override
    public IntegerProperty getNorthProperty() {
        return NORTH;
    }
    
    @Override
    public IntegerProperty getEastProperty() {
        return EAST;
    }
    
    @Override
    public IntegerProperty getSouthProperty() {
        return SOUTH;
    }
    
    @Override
    public IntegerProperty getWestProperty() {
        return WEST;
    }
    
    @Override
    public IntegerProperty getUpProperty() {
        return UP;
    }
    
    @Override
    public IntegerProperty getDownProperty() {
        return DOWN;
    }
}
