package rearth.oritech.init.world.features.resourcenode;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import rearth.oritech.Oritech;

import java.util.List;

public class ResourceNodeFeature extends Feature<ResourceNodeFeatureConfig> {
    
    public ResourceNodeFeature(Codec<ResourceNodeFeatureConfig> configCodec) {
        super(configCodec);
    }
    
    @Override
    public boolean generate(FeatureContext<ResourceNodeFeatureConfig> context) {
        
        var world = context.getWorld();
        var origin = context.getOrigin();
        
        if (world.isClient()) return false;
        
        var bedrockFound = false;
        var testPos = new BlockPos(origin);
        var deepNodePos = testPos;
        for (int y = world.getBottomY(); y < world.getHeight(); y++) {
            testPos = testPos.up();
            var testState = world.getBlockState(testPos);
            
            if (!testState.isOf(Blocks.BEDROCK) && !bedrockFound) {
                deepNodePos = testPos;
                bedrockFound = true;
            } else if (bedrockFound && testState.isOf(Blocks.BEDROCK)) {
                bedrockFound = false;
                // reset if another bedrock layer occurs
            }
            
            if (testState.isIn(BlockTags.DIRT) || testState.isIn(BlockTags.SAND)) {
                if (world.getBlockState(testPos.up()).isOf(Blocks.AIR)) {
                    if (Oritech.CONFIG.easyFindFeatures())
                        placeSurfaceBoulder(testPos, context);
                    placeBedrockNode(deepNodePos, context);
                    Oritech.LOGGER.debug("placing resource node at " + testPos + " with deep " + deepNodePos);
                    return true;
                }
            }
            
        }
        
        return false;
    }
    
    private BlockState getRandomBlockFromList(List<Identifier> list, Random random) {
        return Registries.BLOCK.get(getRandomFromList(list, random)).getDefaultState();
    }
    
    private Identifier getRandomFromList(List<Identifier> list, Random random) {
        return list.get(random.nextInt(list.size()));
    }
    
    private void placeBedrockNode(BlockPos startPos, FeatureContext<ResourceNodeFeatureConfig> context) {
        
        var world = context.getWorld();
        var random = context.getRandom();
        var ores = context.getConfig().nodeOres();
        
        var radius = context.getConfig().nodeSize();
        var overlayBlock = Registries.BLOCK.get(context.getConfig().overlayBlock()).getDefaultState();
        
        for (int x = 0; x < radius; x++) {
            for (int y = 0; y < radius; y++) {
                var pos = startPos.add(x, 0, y);
                world.setBlockState(pos, getRandomBlockFromList(ores, random), 0x10);
            }
        }
        
        // overlay it with something
        for (int x = -1; x <= radius; x++) {
            for (int y = -1; y <= radius; y++) {
                var pos = startPos.add(x, 0, y);
                for (int i = 0; i < context.getConfig().overlayHeight(); i++) {
                    world.setBlockState(pos.up(1 + i), overlayBlock, 0x10);
                }
            }
        }
    }
    
    private void placeSurfaceBoulder(BlockPos startPos, FeatureContext<ResourceNodeFeatureConfig> context) {
        
        var world = context.getWorld();
        var random = context.getRandom();
        var movedCenter = new Vec3d(startPos.getX() - random.nextFloat(), startPos.getY() - random.nextFloat(), startPos.getZ() - random.nextFloat());
        var radius = context.getConfig().boulderRadius();
        var ores = context.getConfig().boulderOres();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    var pos = startPos.add(x, y, z);
                    var distance = movedCenter.distanceTo(pos.toCenterPos());
                    if (distance > radius) continue;
                    world.setBlockState(pos, getRandomBlockFromList(ores, random), 0x10);
                }
            }
        }
    }
}
