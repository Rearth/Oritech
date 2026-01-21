package rearth.oritech.block.entity.reactor;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import rearth.oritech.block.blocks.reactor.NuclearExplosionBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.SoundContent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NuclearExplosionEntity extends BlockEntity implements BlockEntityTicker<NuclearExplosionEntity> {

    private long startTime = -1;
    private final Set<BlockPos> removedBlocks = new HashSet<>();
    private final Set<BlockPos> borderBlocks = new HashSet<>();
    private final Set<DirectionExplosionWave> waves = new HashSet<>();
    private final int size;

    public NuclearExplosionEntity(BlockPos pos, BlockState state, int size) {
        super(BlockEntitiesContent.REACTOR_EXPLOSION_ENTITY, pos, state);
        this.size = size;
    }

    public NuclearExplosionEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REACTOR_EXPLOSION_ENTITY, pos, state);
        this.size = 9;
    }

    @Override
    public void tick(Level world, BlockPos pos, BlockState state, NuclearExplosionEntity blockEntity) {
        if (world.isClientSide) return;

        var initialRadius = size;

        if (startTime == -1) {
            startTime = world.getGameTime();
            explosionSphere(initialRadius + 7, 200, pos);
            world.playSound(null, pos, SoundContent.NUKE_EXPLOSION, SoundSource.BLOCKS, 30f, 1f);
        }

        var age = world.getGameTime() - startTime;

        if (age == 1) {
            createExplosionWaves(initialRadius);
        }

        if (age > 1) {
            waves.forEach(DirectionExplosionWave::nextGeneration);
            processBorderBlocks(initialRadius * initialRadius);
        }

        if (age > initialRadius * 2) {
            // done
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }

    }

    private void createExplosionWaves(int initialRadius) {

        var rayCount = initialRadius / 2 + 3;
        var directions = getRandomRayDirections(rayCount);
        for (var direction : directions) {
            var data = new DirectionExplosionWave(initialRadius, addRandomOffset(direction, 0.15f), worldPosition.offset(0, level.random.nextIntBetweenInclusive(-initialRadius / 2, initialRadius / 2), 0).immutable());
            waves.add(data);
        }
    }

    private void processBorderBlocks(int maxDist) {

        borderBlocks.forEach(target -> {
            if (removedBlocks.contains(target)) return;
            var distSq = target.distSqr(worldPosition);
            var targetBlock = level.getBlockState(target);
            var percentageDist = distSq / (maxDist * maxDist) * 8;
            var percentageVaried = percentageDist * (level.random.nextFloat() * 0.6 - 0.3 + 1);
            
            if (Platform.isModLoaded("ftbchunks")) {
                var isClaimed = FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level, target)) != null;
                if (isClaimed) return;
            }

            var replaced = false;
            var replacementState = Blocks.AIR.defaultBlockState();

            if (targetBlock.is(BlockTags.LOGS)) {
                replaced = true;
                replacementState = level.random.nextFloat() < 0.8 ? Blocks.BASALT.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
                if (percentageVaried < 0.4f) replacementState = Blocks.AIR.defaultBlockState();
            } else if (targetBlock.is(BlockTags.LEAVES)) {
                replaced = true;
                replacementState = level.random.nextFloat() > 0.4 ? Blocks.MANGROVE_ROOTS.defaultBlockState() : Blocks.AIR.defaultBlockState();
                if (percentageVaried < 0.6f) replacementState = Blocks.AIR.defaultBlockState();
            } else if (targetBlock.is(BlockTags.SAPLINGS) || targetBlock.is(Blocks.SHORT_GRASS)) {
                replaced = true;
                replacementState = level.random.nextFloat() > 0.4 ? Blocks.DEAD_BUSH.defaultBlockState() : Blocks.AIR.defaultBlockState();
                if (percentageVaried < 0.5f) replacementState = Blocks.AIR.defaultBlockState();
            } else if (targetBlock.is(Blocks.GRASS_BLOCK)) {
                replaced = true;
                if (percentageVaried < 0.05) {
                    replacementState = level.random.nextFloat() > 0.5 ? Blocks.TUFF.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
                } else if (percentageVaried < 0.3) {
                    replacementState = level.random.nextFloat() > 0.2 ? Blocks.TUFF.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
                } else if (percentageVaried < 0.55) {
                    replacementState = level.random.nextFloat() > 0.1 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
                } else {
                    replacementState = Blocks.DIRT.defaultBlockState();
                }

                if (level.random.nextFloat() > 0.7) replaced = false;
            } else if (targetBlock.is(Blocks.DIRT)) {
                replaced = true;
                if (percentageVaried < 0.15) {
                    replacementState = level.random.nextFloat() > 0.6 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
                } else if (percentageVaried < 0.3) {
                    replacementState = level.random.nextFloat() > 0.3 ? Blocks.TUFF.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
                } else if (percentageVaried < 0.65) {
                    replacementState = level.random.nextFloat() > 0.2 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.TUFF.defaultBlockState();
                } else {
                    replaced = false;
                }

                if (level.random.nextFloat() > 0.1) replaced = false;
            } else if (targetBlock.is(BlockTags.BASE_STONE_OVERWORLD)) {
                replaced = true;
                if (percentageVaried < 0.3) {
                    replacementState = level.random.nextFloat() > 0.5 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
                } else if (percentageVaried < 0.5) {
                    replacementState = level.random.nextFloat() > 0.3 ? Blocks.STONE.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
                } else if (percentageVaried < 0.7) {
                    replacementState = level.random.nextFloat() > 0.2 ? Blocks.GRANITE.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
                } else {
                    replaced = false;
                }
            } else if (targetBlock.is(BlockTags.SAND) || targetBlock.is(Blocks.SANDSTONE)) {
                replaced = true;
                if (percentageVaried < 0.2) {
                    replacementState = level.random.nextFloat() > 0.7 ? Blocks.SANDSTONE.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
                } else {
                    replacementState = Blocks.GLASS.defaultBlockState();
                }

                if (percentageVaried > 0.8) replaced = false;
            }

            if (replaced) {
                level.setBlock(target, replacementState, Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_CLIENTS, 1);

                // random fire chance
                if (level.getBlockState(target.above()).canBeReplaced() && level.random.nextFloat() > 0.97) {
                    level.setBlock(target.above(), Blocks.FIRE.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_CLIENTS, 0);
                }
            }
        });

        borderBlocks.clear();
    }

    private void collectExtraEdgeBlocks(BlockPos center) {
        BlockPos.betweenClosed(center.offset(-8, -8, -8), center.offset(8, 8, 8)).forEach(target -> {
            if (removedBlocks.contains(target)) return;
            var targetState = level.getBlockState(target);
            if (targetState.isAir()) return;
            borderBlocks.add(target.immutable());
        });
    }

    // remove all blocks in X radius below hardness 'power', return amount of hardness used in total
    // also damage entities
    private int explosionSphere(int radius, int power, BlockPos pos) {

        var radiusSq = radius * radius;
        var radiusSqExtra = (radius + 3) * (radius + 3);
        var usedPower = 0;
        var hardBusters = radius;

        for (var target : BlockPos.withinManhattan(pos, radius + 3, radius + 3, radius + 3)) {
            if (removedBlocks.contains(target)) continue;
            var distSq = target.distSqr(pos);

            if (distSq > radiusSq) {
                if (distSq <= (radiusSqExtra)) {
                    // border block, was almost destroyed
                    borderBlocks.add(target.immutable());
                }
                continue;
            }

            // if less than half dist, 100%, then slowly ramp up to 0%
            var removalPercentage = (distSq - radiusSq / 2f) / radiusSq;
            if (level.random.nextFloat() < removalPercentage - 0.2) {
                borderBlocks.add(target.immutable());
                continue;
            }

            var targetState = level.getBlockState(target);
            var targetBlock = targetState.getBlock();
            var targetHardness = targetBlock.getExplosionResistance();

            if (targetBlock instanceof NuclearExplosionBlock || targetState.isAir() || targetState.getDestroySpeed(level, target) < 0)
                continue;

            // skip too hard blocks (except for the first few)
            if (targetHardness > power && hardBusters-- < 0) continue;

            usedPower += targetHardness;
            
            if (Platform.isModLoaded("ftbchunks")) {
                var isClaimed = FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level, target)) != null;
                if (isClaimed) return 1000;
            }
            
            targetBlock.destroy(level, pos, targetState);
            level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_CLIENTS, 0);
            removedBlocks.add(target.immutable());
            borderBlocks.remove(target.immutable());

            // damages all entities in radius based on distance
            var entityCandidates = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(pos.subtract(new Vec3i(radius, radius, radius)).getCenter(), pos.offset(new Vec3i(radius, radius, radius)).getCenter()),
                    EntitySelector.LIVING_ENTITY_STILL_ALIVE.and(EntitySelector.NO_CREATIVE_OR_SPECTATOR));

            entityCandidates.forEach(entity -> {
                var entityDist = entity.distanceToSqr(pos.getCenter());
                var distPercentage = entityDist / radiusSq;
                var damage = radiusSq / distPercentage; // closer entities take much more damage
                System.out.println(entityDist + ":" + damage);
                entity.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.EXPLOSION)), (float) damage);
            });

        }

        return usedPower;
    }

    private List<Vec3> getRandomRayDirections(int count) {
        List<Vec3> rayDirections = new ArrayList<>(count);

        // Divide the circle into 12 equal parts
        var angleIncrement = 2 * Math.PI / count; // 360 degrees / 12

        for (int i = 0; i < count; i++) {
            // Calculate the base angle for this ray
            var baseAngle = i * angleIncrement;

            // Add a small random perturbation to the angle
            var randomPerturbation = (level.random.nextFloat() - 0.5) * (angleIncrement / 2);

            // Final angle with randomness
            var angle = baseAngle + randomPerturbation;

            // Calculate the direction vector
            var x = Math.cos(angle);
            var z = Math.sin(angle);

            rayDirections.add(new Vec3(x, 0, z)); // Horizontal direction
        }

        return rayDirections;
    }

    private Vec3 addRandomOffset(Vec3 direction, float amount) {
        return direction.add(level.random.nextFloat() * amount - amount / 2, level.random.nextFloat() * amount - amount / 2, level.random.nextFloat() * amount - amount / 2);
    }

    private class DirectionExplosionWave {

        private final Vec3 direction;

        private int lastRadius;
        private BlockPos lastPosition;
        private int lastRadiusReduction;

        private DirectionExplosionWave(int initialRadius, Vec3 direction, BlockPos pos) {
            this.direction = direction;
            this.lastRadius = initialRadius;
            this.lastPosition = pos;
            this.lastRadiusReduction = 1;
        }

        private void nextGeneration() {
            var currentRadius = lastRadius - lastRadiusReduction;
            if (currentRadius <= 1) return;
            var rayOffset = direction.scale(currentRadius);
            var target = lastPosition.offset(BlockPos.containing(rayOffset));
            var power = currentRadius * 3;
            lastRadius = currentRadius;
            lastPosition = target;

            var usedPower = explosionSphere(currentRadius, power, target);
            var expectedPower = currentRadius * currentRadius * currentRadius * 3;
            if (usedPower > expectedPower) {
                lastRadiusReduction = 2;
            }

            var isLastGeneration = currentRadius - lastRadiusReduction <= 1;
            if (isLastGeneration)
                collectExtraEdgeBlocks(target.offset(BlockPos.containing(rayOffset.scale(3))));

        }
    }
}
