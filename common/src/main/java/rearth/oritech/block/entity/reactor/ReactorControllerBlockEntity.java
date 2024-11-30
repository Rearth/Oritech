package rearth.oritech.block.entity.reactor;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.joml.Vector2i;
import rearth.oritech.block.blocks.reactor.*;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ReactorControllerBlockEntity extends BlockEntity implements BlockEntityTicker<ReactorControllerBlockEntity> {
    
    public static final int MAX_SIZE = 64;
    
    private int reactorHeat;   // the heat of the entire casing
    private final HashMap<Vector2i, BaseReactorBlock> activeComponents = new HashMap<>();
    private final HashMap<Vector2i, Integer> componentHeats = new HashMap<>();
    
    public ReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REACTOR_CONTROLLER_BLOCK_ENTITY, pos, state);
    }
    
    // heat is only used for reactor rods and heat pipes (and the casing itself)
    // rods generate heat. Base rate pushes it to reactor at generation speed. Multi-cores and reflectors (expensive) change this
    // heat pipes move heat to them and to the reactor casing
    // vents remove heat from the reactor casing
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, ReactorControllerBlockEntity blockEntity) {
        if (world.isClient) return;
        
        for (var localPos : activeComponents.keySet()) {
            var component = activeComponents.get(localPos);
            var componentHeat = componentHeats.get(localPos);
            if (component instanceof ReactorRodBlock rodBlock) {
                
                reactorHeat += 4;
                
            } else if (component instanceof ReactorHeatPipeBlock heatPipeBlock) {
                
                // take heat in from neighbors
                for (var neighbor : getNeighborsInBounds(localPos, activeComponents.keySet())) {
                    var neighborHeat = componentHeats.get(neighbor);
                    if (neighborHeat <= componentHeat) continue;
                    var diff = neighborHeat - componentHeat;
                    var gainedHeat = diff / 100;
                    neighborHeat -= gainedHeat;
                    componentHeats.put(neighbor, neighborHeat);
                    componentHeat += gainedHeat;
                }
                
                // move heat to reactor
                var moveAmount = Math.min(10, componentHeat);
                reactorHeat += moveAmount;
                componentHeat -= moveAmount;
                
            } else if (component instanceof ReactorHeatVentBlock ventBlock) {
                
                reactorHeat = Math.max(reactorHeat - 4, 0);
                
            } else if (component instanceof ReactorReflectorBlock reflectorBlock) {
                
                // do extra ticks on neighboring rods
                for (var neighborPos : getNeighborsInBounds(localPos, activeComponents.keySet())) {
                    
                    var neighbor = activeComponents.get(neighborPos);
                    if (neighbor instanceof ReactorRodBlock neighborRod) {
                        var rodHeat = componentHeats.get(neighborPos);
                        rodHeat += 4;
                        componentHeats.put(neighborPos, rodHeat);
                    }
                }
                
            }
            
            componentHeats.put(localPos, componentHeat);
        }
        
        System.out.println(reactorHeat);
        System.out.println(Arrays.toString(componentHeats.entrySet().toArray()));
        
    }
    
    public void init(PlayerEntity player) {
        
        // find low and high corners of reactor
        var cornerA = pos;
        cornerA = expandWall(cornerA, new Vec3i(0, -1, 0), true);   // first go down through other wall blocks
        cornerA = expandWall(cornerA, new Vec3i(0, 0, -1));
        cornerA = expandWall(cornerA, new Vec3i(-1, 0, 0));
        cornerA = expandWall(cornerA, new Vec3i(0, 0, -1)); // expand z again to support all rotations
        
        var cornerB = cornerA;
        cornerB = expandWall(cornerB, new Vec3i(0, 1, 0));
        cornerB = expandWall(cornerB, new Vec3i(0, 0, 1));
        cornerB = expandWall(cornerB, new Vec3i(1, 0, 0));
        
        if (cornerA == pos || cornerB == pos || cornerA == cornerB || onSameAxis(cornerA, cornerB)) {
            player.sendMessage(Text.translatable("message.oritech.reactor_invalid"));
            return;
        }
        
        System.out.println("corners valid");
        
        // verify and load all blocks in reactor area
        var finalCornerA = cornerA;
        var finalCornerB = cornerB;
        
        // verify edges
        var wallsValid = BlockPos.stream(cornerA, cornerB).allMatch(pos -> {
            if (isAtEdgeOfBox(pos, finalCornerA, finalCornerB)) {
                var block = world.getBlockState(pos).getBlock();
                return block instanceof ReactorWallBlock;
            } else if (isOnWall(pos, finalCornerA, finalCornerB)) {
                var block = world.getBlockState(pos).getBlock();
                return !(block instanceof BaseReactorBlock reactorBlock) || reactorBlock.validForWalls();
            }
            
            return true;
        });
        
        if (!wallsValid) {
            player.sendMessage(Text.translatable("message.oritech.reactor_invalid"));
            return;
        }
        
        // verify interior is identical in all layers
        var interiorHeight = cornerB.getY() - cornerA.getY() - 1;
        var cornerAFlat = cornerA.add(1, 1, 1);
        var cornerBFlat = new BlockPos(cornerB.getX() - 1, cornerA.getY() + 1, cornerB.getZ() - 1);
        activeComponents.clear();
        
        var interiorStackedRight = BlockPos.stream(cornerAFlat, cornerBFlat).allMatch(pos -> {
            
            var block = world.getBlockState(pos).getBlock();
            if (!(block instanceof BaseReactorBlock reactorBlock)) return true;
            
            for (int i = 1; i < interiorHeight; i++) {
                var candidatePos = pos.add(0, i, 0);
                var candidate = world.getBlockState(candidatePos);
                if (!candidate.getBlock().equals(block))
                    return false;
            }
            
            var offset = pos.subtract(cornerAFlat);
            var localPos = new Vector2i(offset.getX(), offset.getZ());
            activeComponents.put(localPos, reactorBlock);
            componentHeats.put(localPos, 0);
            System.out.println(offset + ": " + reactorBlock);
            
            return true;
        });
        
        if (!interiorStackedRight) {
            player.sendMessage(Text.translatable("message.oritech.interior_invalid"));
            return;
        }
        
        System.out.println("walls: " + wallsValid + " interiorStack: " + interiorStackedRight + " height: " + interiorHeight);
        
    }
    
    private static Set<Vector2i> getNeighborsInBounds(Vector2i pos, Set<Vector2i> keys) {
        
        var res = new HashSet<Vector2i>(4);
        
        var a = new Vector2i(pos).add(-1, 0);
        if (keys.contains(a)) res.add(a);
        var b = new Vector2i(pos).add(0, 1);
        if (keys.contains(b)) res.add(b);
        var c = new Vector2i(pos).add(1, 0);
        if (keys.contains(c)) res.add(c);
        var d = new Vector2i(pos).add(0, -1);
        if (keys.contains(d)) res.add(d);
        
        return res;
    }
    
    private static boolean onSameAxis(BlockPos A, BlockPos B) {
        return A.getX() == B.getX() || A.getY() == B.getY() || A.getZ() == B.getZ();
    }
    
    private static boolean isOnWall(BlockPos pos, BlockPos min, BlockPos max) {
        return onSameAxis(pos, min) || onSameAxis(pos, max);
    }
    
    private static boolean isAtEdgeOfBox(BlockPos pos, BlockPos min, BlockPos max) {
        int planesAligned = 0;
        
        if (pos.getX() == min.getX() || pos.getX() == max.getX()) planesAligned++;
        if (pos.getY() == min.getY() || pos.getY() == max.getY()) planesAligned++;
        if (pos.getZ() == min.getZ() || pos.getZ() == max.getZ()) planesAligned++;
        
        return planesAligned >= 2;
    }
    
    private BlockPos expandWall(BlockPos from, Vec3i direction) {
        return expandWall(from, direction, false);
    }
    
    private BlockPos expandWall(BlockPos from, Vec3i direction, boolean allReactorBlocks) {
        
        var result = from;
        for (int i = 1; i < MAX_SIZE; i++) {
            var candidate = from.add(direction.multiply(i));
            var candidateBlock = world.getBlockState(candidate).getBlock();
            
            if (!allReactorBlocks && !(candidateBlock instanceof ReactorWallBlock)) return result;
            if (allReactorBlocks && !(candidateBlock instanceof BaseReactorBlock)) return result;
            
            result = candidate;
        }
        
        return result;
        
    }
    
}
