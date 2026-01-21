package rearth.oritech.init.world.features.uranium;

import com.mojang.serialization.Codec;
import org.joml.Vector2d;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.Geometry;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class UraniumPatchFeature extends Feature<UraniumPatchFeatureConfig> {
    public UraniumPatchFeature(Codec<UraniumPatchFeatureConfig> configCodec) {
        super(configCodec);
    }
    
    private static boolean isAirOrWater(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }
    
    @Override
    public boolean place(FeaturePlaceContext<UraniumPatchFeatureConfig> context) {
        
        var world = context.level();
        var origin = context.origin();
        
        if (world.isClientSide()) return false;
        
        var testPos = new BlockPos(origin.below(3));
        if (isAirOrWater(world.getBlockState(testPos)))
            placeStructure(testPos, context);
        
        return false;
    }
    
    private void placeStructure(BlockPos pos, FeaturePlaceContext<UraniumPatchFeatureConfig> context) {
        
        var random = context.random();
        var config = context.config();
        var state = BuiltInRegistries.BLOCK.get(config.blockId()).defaultBlockState();
        var crystalBlock = BuiltInRegistries.BLOCK.get(config.crystalId());
        var world = context.level();
        
        var range = config.number();
        var closestWall = pos;
        
        // find closest wall
        for (var candidate : BlockPos.withinManhattan(pos, range, range, range)) {
            var candidateState = world.getBlockState(candidate);
            if (isAirOrWater(candidateState)) continue;
            closestWall = candidate;
            break;
        }
        
        if (closestWall.equals(pos)) return;
        
        var closestWallDir = closestWall.subtract(pos);
        var forward = getBiggestDirection(closestWallDir);
        var facing = Direction.fromDelta(forward.getX(), forward.getY(), forward.getZ());
        
        if (facing == null) return;
        
        var right = Geometry.getRight(facing);
        var up = Geometry.getUp(facing);
        
        var veinCount = 3;
        for (int i = 0; i < veinCount; i++) {
            var randomDir = new Vector2d(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).normalize();
            var veinLength = random.nextIntBetweenInclusive(5, 9);
            
            // move along vein
            for (int j = 0; j < veinLength; j++) {
                var test = pos.offset(right.multiply((int) (randomDir.x * j))).offset(up.multiply((int) (randomDir.y * j)));
                var test2 = pos.offset(right.multiply((int) (randomDir.x * j + 0.5))).offset(up.multiply((int) (randomDir.y * j + 0.5)));
                
                // project onto first non-air block in forward direction
                for (int k = 0; k < 5; k++) {
                    var projected = test.offset(forward.multiply(k));
                    var projected2 = test2.offset(forward.multiply(k));
                    var testState = world.getBlockState(projected);
                    var testState2 = world.getBlockState(projected2);
                    if (isValidReplacementBloc(testState)) {
                        createCrystals(projected, world, random, crystalBlock);
                        world.setBlock(projected, state, Block.UPDATE_CLIENTS, 0);
                        break;
                    }
                    if (isValidReplacementBloc(testState2)) {
                        world.setBlock(projected2, state, Block.UPDATE_CLIENTS, 0);
                        break;
                    }
                    
                }
                
                randomDir = randomDir.add(random.nextFloat() * 0.2, random.nextFloat() * 0.2).normalize();
            }
            
        }
    }
    
    private boolean isValidReplacementBloc(BlockState state) {
        return state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || state.is(BlockTags.STONE_ORE_REPLACEABLES);
    }
    
    private void createCrystals(BlockPos pos, WorldGenLevel world, RandomSource random, Block crystal) {
        for (var neighborPos : getNeighbors(pos)) {
            var neighborState = world.getBlockState(neighborPos);
            
            var isValid = neighborState.isAir() || neighborState.is(Blocks.WATER);
            if (!isValid || random.nextFloat() < 0.7) continue;
            
            var waterLogged = neighborState.is(Blocks.WATER);
            var facing = Geometry.fromVector(neighborPos.subtract(pos));
            if (facing == null) continue;
            var targetState = crystal.defaultBlockState()
                                .setValue(AmethystClusterBlock.WATERLOGGED, waterLogged)
                                .setValue(AmethystClusterBlock.FACING, facing);
            world.setBlock(neighborPos, targetState, Block.UPDATE_CLIENTS, 0);
        }
    }
    
    private List<BlockPos> getNeighbors(BlockPos pos) {
        return List.of(pos.below(), pos.above(), pos.north(), pos.east(), pos.south(), pos.west());
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
