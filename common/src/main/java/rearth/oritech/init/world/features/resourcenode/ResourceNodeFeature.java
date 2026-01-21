package rearth.oritech.init.world.features.resourcenode;

import com.mojang.serialization.Codec;
import rearth.oritech.Oritech;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

public class ResourceNodeFeature extends Feature<ResourceNodeFeatureConfig> {
    
    public ResourceNodeFeature(Codec<ResourceNodeFeatureConfig> configCodec) {
        super(configCodec);
    }
    
    @Override
    public boolean place(FeaturePlaceContext<ResourceNodeFeatureConfig> context) {

        var world = context.level();
        var origin = context.origin();
        
        if (world.isClientSide()) return false;
        
        var solidBlockFound = false;
        var testPos = new BlockPos(origin);
        var deepNodePos = testPos;
        var boulderPos = testPos;

        for (int y = origin.getY(); y > world.getMinBuildHeight(); y--) {
            var downPos = testPos.below();
            var testState = world.getBlockState(downPos);
            if (testState.is(Blocks.BEDROCK)) {
                deepNodePos = testPos;
                break;
            } else if (testState.isRedstoneConductor(world, downPos) && !solidBlockFound) {
                boulderPos = testPos = downPos;
                solidBlockFound = true;
            } else {
                testPos = downPos;
            }
        }

        // edge case: if no solid block was found, or the boulder is too close to the deep node, don't generate
        if (!solidBlockFound || boulderPos.getY() < (deepNodePos.getY() + 10))
            return false;

        if (Oritech.CONFIG.easyFindFeatures())
            placeSurfaceBoulder(boulderPos, context);
        placeBedrockNode(deepNodePos, context);
        Oritech.LOGGER.debug("placing resource node at " + boulderPos + " with deep " + deepNodePos);
        return true;
        
    }
    
    private BlockState getRandomBlockFromList(List<ResourceLocation> list, RandomSource random) {
        return BuiltInRegistries.BLOCK.get(getRandomFromList(list, random)).defaultBlockState();
    }
    
    private ResourceLocation getRandomFromList(List<ResourceLocation> list, RandomSource random) {
        return list.get(random.nextInt(list.size()));
    }
    
    private void placeBedrockNode(BlockPos startPos, FeaturePlaceContext<ResourceNodeFeatureConfig> context) {
        
        var world = context.level();
        var random = context.random();
        var ores = context.config().nodeOres();
        
        var radius = context.config().nodeSize();
        var overlayBlock = BuiltInRegistries.BLOCK.get(context.config().overlayBlock()).defaultBlockState();
        var overlayHeight = context.config().overlayHeight();

        var noise = new ImprovedNoise(random);

        // the bottom of the "bowl" should start below the top layer of bedrock
        BlockPos centerPos = startPos.above(radius - 2);

        for (BlockPos pos : BlockPos.withinManhattan(centerPos, radius, radius, radius)) {
            // skip anything outside the radius, or outside the vertical cutoff
            if (Math.sqrt(pos.distSqr(centerPos)) + noise.noise(pos.getX(), pos.getY(), pos.getZ()) > radius
                || pos.getY() >= startPos.getY() + overlayHeight + 3 + noise.noise(pos.getX(), pos.getY() + 2, pos.getZ())) continue;
            // randomly replace some blocks below bedrock level with resource nodes
            if (pos.getY() <= startPos.getY() + 1 && random.nextDouble() <= context.config().nodeOreChance()) {
                world.setBlock(pos, getRandomBlockFromList(ores, random), 0x10);
            // set blocks between bedrock and bedrock + overlayHeight to overlayBlock
            } else if (pos.getY() > startPos.getY() + 1 && pos.getY() <= startPos.getY() + overlayHeight + 1) {
                world.setBlock(pos, overlayBlock, 0x10);
            // set anything between overlay and vertical cutoff to air
            } else if (pos.getY() > startPos.getY() + 1) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 0x10);
            }
        }
    }
    
    private void placeSurfaceBoulder(BlockPos startPos, FeaturePlaceContext<ResourceNodeFeatureConfig> context) {
        
        var world = context.level();
        var random = context.random();
        var radius = context.config().boulderRadius();
        var movedCenter = startPos.relative(Axis.getRandom(random), random.nextIntBetweenInclusive(0, radius-1));
        var ores = context.config().boulderOres();
        var noise = new ImprovedNoise(random);
        
        for (BlockPos pos : BlockPos.withinManhattan(movedCenter, radius, radius, radius)) {
            if (Math.sqrt(pos.distSqr(movedCenter)) > radius + noise.noise(pos.getX(), pos.getY(), pos.getZ())) continue;
            world.setBlock(pos, getRandomBlockFromList(ores, random), 0x10);
        }
    }
}
