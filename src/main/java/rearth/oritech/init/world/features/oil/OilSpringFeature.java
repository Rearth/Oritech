package rearth.oritech.init.world.features.oil;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import rearth.oritech.config.OritechConfig;

public class OilSpringFeature extends Feature<OilSpringFeatureConfig> {
    private static final int MIN_SURFACE_PATCHES = 3;
    private static final int MAX_SURFACE_PATCHES = 5;
    private static final int MIN_PATCH_DISTANCE = 3;
    private static final int MAX_PATCH_DISTANCE = 6;

    public OilSpringFeature(Codec<OilSpringFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OilSpringFeatureConfig> context) {

        var level = context.level();
        var origin = context.origin();

        if (level.isClientSide()) return false;


        var testPos = new BlockPos(origin);
        for (int y = 0; y < level.getHeight(); y++) {
            testPos = testPos.above();

            if (level.getBlockState(testPos).is(BlockTags.DIRT) || level.getBlockState(testPos).is(BlockTags.SAND)) {
                if (level.getBlockState(testPos.above()).is(Blocks.AIR)) {
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
        var state = BuiltInRegistries.BLOCK.get(config.blockId()).get().value().defaultBlockState();
        var level = context.level();

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
                        level.setBlock(point, state, 0x10);
                    } else if (distance <= height + noiseOffset) {
                        level.setBlock(point, Blocks.STONE.defaultBlockState(), 0x10);
                    }
                }
            }
        }

        // fountain up
        if (OritechConfig.easyFindFeatures.get()) {
            for (int i = 0; i < height; i++) {
                level.setBlock(surfacePos.above(i), state, 0x10);
            }

            placeSurfaceBasin(surfacePos, context);

            var topPos = surfacePos.above(height - 1);
            level.scheduleTick(topPos, state.getFluidState().getType(), 1);
        }

        // down
        for (int i = 1; i < depth + 5; i++) {
            level.setBlock(surfacePos.below(i), state, 0x10);
        }

        placeSurfacePatches(surfacePos, state, context);
    }

    private void placeSurfaceBasin(BlockPos surfacePos, FeaturePlaceContext<OilSpringFeatureConfig> context) {

        var level = context.level();

        for (var direction : Direction.Plane.HORIZONTAL) {
            var basinPos = surfacePos.relative(direction);
            var basinState = level.getBlockState(basinPos);

            if ((basinState.is(BlockTags.DIRT) || basinState.is(BlockTags.SAND))
                    && level.getBlockState(basinPos.above()).isAir()) {
                level.setBlock(basinPos, Blocks.AIR.defaultBlockState(), 0x10);
            }
        }
    }

    private void placeSurfacePatches(BlockPos surfacePos, BlockState oilState, FeaturePlaceContext<OilSpringFeatureConfig> context) {

        var level = context.level();
        var random = context.random();
        var patchCount = random.nextIntBetweenInclusive(MIN_SURFACE_PATCHES, MAX_SURFACE_PATCHES);

        for (int patch = 0; patch < patchCount; patch++) {
            int offsetX;
            int offsetZ;

            do {
                offsetX = random.nextIntBetweenInclusive(-MAX_PATCH_DISTANCE, MAX_PATCH_DISTANCE);
                offsetZ = random.nextIntBetweenInclusive(-MAX_PATCH_DISTANCE, MAX_PATCH_DISTANCE);
            } while (offsetX * offsetX + offsetZ * offsetZ < MIN_PATCH_DISTANCE * MIN_PATCH_DISTANCE);

            var patchCenter = surfacePos.offset(offsetX, 0, offsetZ);
            var radius = random.nextIntBetweenInclusive(1, 2);

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z > radius * radius || random.nextFloat() < 0.15f) continue;

                    var columnPos = patchCenter.offset(x, 0, z);
                    var surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, columnPos.getX(), columnPos.getZ()) - 1;
                    var targetPos = new BlockPos(columnPos.getX(), surfaceY, columnPos.getZ());
                    var targetState = level.getBlockState(targetPos);

                    if ((targetState.is(BlockTags.DIRT) || targetState.is(BlockTags.SAND))
                            && level.getBlockState(targetPos.above()).isAir()) {
                        level.setBlock(targetPos, oilState, 0x10);
                    }
                }
            }
        }
    }
}
