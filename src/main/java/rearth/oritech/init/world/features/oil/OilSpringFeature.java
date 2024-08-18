package rearth.oritech.init.world.features.oil;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import rearth.oritech.Oritech;

public class OilSpringFeature extends Feature<OilSpringFeatureConfig> {
    public OilSpringFeature(Codec<OilSpringFeatureConfig> configCodec) {
        super(configCodec);
    }
    
    @Override
    public boolean generate(FeatureContext<OilSpringFeatureConfig> context) {
        
        var world = context.getWorld();
        var origin = context.getOrigin();
        
        if (world.isClient()) return false;
        
        
        var testPos = new BlockPos(origin);
        for (int y = 0; y < world.getHeight(); y++) {
            testPos = testPos.up();
            
            if (world.getBlockState(testPos).isIn(BlockTags.DIRT) || world.getBlockState(testPos).isIn(BlockTags.SAND)) {
                if (world.getBlockState(testPos.up()).isOf(Blocks.AIR)) {
                    placeStructure(testPos, context);
                    return true;
                }
            }
            
        }
        
        return false;
    }
    
    private void placeStructure(BlockPos surfacePos, FeatureContext<OilSpringFeatureConfig> context) {
        
        var random = context.getRandom();
        var config = context.getConfig();
        var state = Registries.BLOCK.get(config.blockId()).getDefaultState();
        var world = context.getWorld();
        
        var variation = random.nextBetween((int) (-config.number() * 0.5f), config.number());
        var height = Math.max(config.number() + variation, 13);
        var depth = height * 2;
        
        var bottomEnd = surfacePos.down(depth);
        var center = bottomEnd.add(random.nextBetween(-2, 2), random.nextBetween(-3, 3), random.nextBetween(0, height / 2));

        var perlinSampler = new PerlinNoiseSampler(random);
        
        // iterate through a cube, calculate distance from center to get a good circle
        for (int x = 0; x < depth + 2; x++) {
            for (int y = 0; y < depth + 2; y++) {
                for (int z = 0; z < depth + 2; z++) {
                    var point = new BlockPos(x - height, y - height, z - height).add(bottomEnd);
                    var distance = Math.sqrt(point.getSquaredDistance(center));
                    var noiseOffset = perlinSampler.sample(x, y, z);
                    if (distance <= height + noiseOffset - 2) {
                        world.setBlockState(point, state, 0x10);
                    } else if (distance <= height + noiseOffset) {
                        world.setBlockState(point, Blocks.STONE.getDefaultState(), 0x10);
                    }
                }
            }
        }
        
        // fountain up
        if (Oritech.CONFIG.easyFindFeatures()) {
            for (int i = 0; i < height; i++) {
                world.setBlockState(surfacePos.up(i), state, 0x10);
            }
        }
        
        // down
        for (int i = 1; i < depth + 5; i++) {
            world.setBlockState(surfacePos.down(i), state, 0x10);
        }
        
    }
}
