package rearth.oritech.init.world.features.oil;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import rearth.oritech.Oritech;

public class OilSpringFeature extends Feature<OilSpringFeatureConfig> {
    public OilSpringFeature(Codec<OilSpringFeatureConfig> configCodec) {
        super(configCodec);
    }
    
    @Override
    public boolean place(FeaturePlaceContext<OilSpringFeatureConfig> context) {
        
        var world = context.level();
        var origin = context.origin();
        
        if (world.isClientSide()) return false;
        
        
        var testPos = new BlockPos(origin);
        for (int y = 0; y < world.getHeight(); y++) {
            testPos = testPos.above();
            
            if (world.getBlockState(testPos).is(BlockTags.DIRT) || world.getBlockState(testPos).is(BlockTags.SAND)) {
                if (world.getBlockState(testPos.above()).is(Blocks.AIR)) {
                    placeStructure(testPos, context);
                    return true;
                }
            }
            
        }
        
        return false;
    }
    
    private void placeStructure(BlockPos surfacePos, FeaturePlaceContext<OilSpringFeatureConfig> context) {
        
        var random = context.random();
        var config = context.config();
        var state = BuiltInRegistries.BLOCK.get(config.blockId()).defaultBlockState();
        var world = context.level();
        
        var variation = random.nextIntBetweenInclusive((int) (-config.number() * 0.5f), config.number());
        var height = Math.max(config.number() + variation, 13);
        var depth = height * 2;
        
        var bottomEnd = surfacePos.below(depth);
        var center = bottomEnd.offset(random.nextIntBetweenInclusive(-2, 2), random.nextIntBetweenInclusive(-3, 3), random.nextIntBetweenInclusive(0, height / 2));

        var perlinSampler = new ImprovedNoise(random);
        
        // iterate through a cube, calculate distance from center to get a good circle
        for (int x = 0; x < depth + 2; x++) {
            for (int y = 0; y < depth + 2; y++) {
                for (int z = 0; z < depth + 2; z++) {
                    var point = new BlockPos(x - height, y - height, z - height).offset(bottomEnd);
                    var distance = Math.sqrt(point.distSqr(center));
                    var noiseOffset = perlinSampler.noise(x, y, z);
                    if (distance <= height + noiseOffset - 2) {
                        world.setBlock(point, state, 0x10);
                    } else if (distance <= height + noiseOffset) {
                        world.setBlock(point, Blocks.STONE.defaultBlockState(), 0x10);
                    }
                }
            }
        }
        
        // fountain up
        if (Oritech.CONFIG.easyFindFeatures()) {
            for (int i = 0; i < height; i++) {
                world.setBlock(surfacePos.above(i), state, 0x10);
            }
        }
        
        // down
        for (int i = 1; i < depth + 5; i++) {
            world.setBlock(surfacePos.below(i), state, 0x10);
        }
        
    }
}
