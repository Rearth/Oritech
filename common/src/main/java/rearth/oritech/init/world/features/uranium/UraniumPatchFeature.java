package rearth.oritech.init.world.features.uranium;

import com.mojang.serialization.Codec;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.joml.Vector2d;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.Geometry;

import java.util.List;

public class UraniumPatchFeature extends Feature<UraniumPatchFeatureConfig> {
    public UraniumPatchFeature(Codec<UraniumPatchFeatureConfig> configCodec) {
        super(configCodec);
    }
    
    private static boolean isAirOrWater(BlockState state) {
        return state.isAir() || state.isOf(Blocks.WATER);
    }
    
    @Override
    public boolean generate(FeatureContext<UraniumPatchFeatureConfig> context) {
        
        var world = context.getWorld();
        var origin = context.getOrigin();
        
        if (world.isClient()) return false;
        
        var testPos = new BlockPos(origin.down(3));
        if (isAirOrWater(world.getBlockState(testPos)))
            placeStructure(testPos, context);
        
        return false;
    }
    
    private void placeStructure(BlockPos pos, FeatureContext<UraniumPatchFeatureConfig> context) {
        
        var random = context.getRandom();
        var config = context.getConfig();
        var state = Registries.BLOCK.get(config.blockId()).getDefaultState();
        var world = context.getWorld();
        
        var range = config.number();
        var closestWall = pos;
        
        // find closest wall
        for (var candidate : BlockPos.iterateOutwards(pos, range, range, range)) {
            var candidateState = world.getBlockState(candidate);
            if (isAirOrWater(candidateState)) continue;
            closestWall = candidate;
            break;
        }
        
        if (closestWall.equals(pos)) return;
        
        var closestWallDir = closestWall.subtract(pos);
        var forward = getBiggestDirection(closestWallDir);
        var facing = Direction.fromVector(forward.getX(), forward.getY(), forward.getZ());
        
        if (facing == null) return;
        
        var right = Geometry.getRight(facing);
        var up = Geometry.getUp(facing);
        
        var veinCount = 3;
        for (int i = 0; i < veinCount; i++) {
            var randomDir = new Vector2d(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).normalize();
            var veinLength = random.nextBetween(5, 9);
            
            // move along vein
            for (int j = 0; j < veinLength; j++) {
                var test = pos.add(right.multiply((int) (randomDir.x * j))).add(up.multiply((int) (randomDir.y * j)));
                var test2 = pos.add(right.multiply((int) (randomDir.x * j + 0.5))).add(up.multiply((int) (randomDir.y * j + 0.5)));
                
                // project onto first non-air block in forward direction
                for (int k = 0; k < 5; k++) {
                    var projected = test.add(forward.multiply(k));
                    var projected2 = test2.add(forward.multiply(k));
                    var testState = world.getBlockState(projected);
                    var testState2 = world.getBlockState(projected2);
                    if (isValidReplacementBloc(testState)) {
                        createCrystals(projected, world, random);
                        world.setBlockState(projected, state, Block.NOTIFY_LISTENERS, 0);
                        break;
                    }
                    if (isValidReplacementBloc(testState2)) {
                        world.setBlockState(projected2, state, Block.NOTIFY_LISTENERS, 0);
                        break;
                    }
                    
                }
                
                randomDir = randomDir.add(random.nextFloat() * 0.2, random.nextFloat() * 0.2).normalize();
            }
            
        }
    }
    
    private boolean isValidReplacementBloc(BlockState state) {
        return state.isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }
    
    private void createCrystals(BlockPos pos, StructureWorldAccess world, Random random) {
        for (var neighborPos : getNeighbors(pos)) {
            var neighborState = world.getBlockState(neighborPos);
            
            var isValid = neighborState.isAir() || neighborState.isOf(Blocks.WATER);
            if (!isValid || random.nextFloat() < 0.7) continue;
            
            var waterLogged = neighborState.isOf(Blocks.WATER);
            var facing = Geometry.fromVector(neighborPos.subtract(pos));
            if (facing == null) continue;
            var targetState = BlockContent.URANIUM_CRYSTAL.getDefaultState()
                                .with(AmethystClusterBlock.WATERLOGGED, waterLogged)
                                .with(AmethystClusterBlock.FACING, facing);
            world.setBlockState(neighborPos, targetState, Block.NOTIFY_LISTENERS, 0);
        }
    }
    
    private List<BlockPos> getNeighbors(BlockPos pos) {
        return List.of(pos.down(), pos.up(), pos.north(), pos.east(), pos.south(), pos.west());
    }
    
    private Vec3i getBiggestDirection(Vec3i source) {
        var x = Math.abs(source.getX());
        var y = Math.abs(source.getY());
        var z = Math.abs(source.getZ());
        
        if (x > y && x > z) {
            return new Vec3i(Math.clamp(source.getX(), -1, 1), 0, 0);
        } else if (y > x && y > z) {
            return new Vec3i(0, Math.clamp(source.getY(), -1, 1), 0);
        } else {
            return new Vec3i(0, 0, Math.clamp(source.getZ(), -1, 1));
        }
        
    }
}
