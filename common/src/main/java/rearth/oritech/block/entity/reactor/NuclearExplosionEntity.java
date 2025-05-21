package rearth.oritech.block.entity.reactor;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.block.blocks.reactor.NuclearExplosionBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.SoundContent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public void tick(World world, BlockPos pos, BlockState state, NuclearExplosionEntity blockEntity) {
        if (world.isClient) return;

        var initialRadius = size;

        if (startTime == -1) {
            startTime = world.getTime();
            explosionSphere(initialRadius + 7, 200, pos);
            world.playSound(null, pos, SoundContent.NUKE_EXPLOSION, SoundCategory.BLOCKS, 30f, 1f);
        }

        var age = world.getTime() - startTime;

        if (age == 1) {
            createExplosionWaves(initialRadius);
        }

        if (age > 1) {
            waves.forEach(DirectionExplosionWave::nextGeneration);
            processBorderBlocks(initialRadius * initialRadius);
        }

        if (age > initialRadius * 2) {
            // done
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
        }

    }

    private void createExplosionWaves(int initialRadius) {

        var rayCount = initialRadius / 2 + 3;
        var directions = getRandomRayDirections(rayCount);
        for (var direction : directions) {
            var data = new DirectionExplosionWave(initialRadius, addRandomOffset(direction, 0.15f), pos.add(0, world.random.nextBetween(-initialRadius / 2, initialRadius / 2), 0).toImmutable());
            waves.add(data);
        }
    }

    private void processBorderBlocks(int maxDist) {

        borderBlocks.forEach(target -> {
            if (removedBlocks.contains(target)) return;
            var distSq = target.getSquaredDistance(pos);
            var targetBlock = world.getBlockState(target);
            var percentageDist = distSq / (maxDist * maxDist) * 8;
            var percentageVaried = percentageDist * (world.random.nextFloat() * 0.6 - 0.3 + 1);

            var replaced = false;
            var replacementState = Blocks.AIR.getDefaultState();

            if (targetBlock.isIn(BlockTags.LOGS)) {
                replaced = true;
                replacementState = world.random.nextFloat() < 0.8 ? Blocks.BASALT.getDefaultState() : Blocks.MAGMA_BLOCK.getDefaultState();
                if (percentageVaried < 0.4f) replacementState = Blocks.AIR.getDefaultState();
            } else if (targetBlock.isIn(BlockTags.LEAVES)) {
                replaced = true;
                replacementState = world.random.nextFloat() > 0.4 ? Blocks.MANGROVE_ROOTS.getDefaultState() : Blocks.AIR.getDefaultState();
                if (percentageVaried < 0.6f) replacementState = Blocks.AIR.getDefaultState();
            } else if (targetBlock.isIn(BlockTags.SAPLINGS) || targetBlock.isOf(Blocks.SHORT_GRASS)) {
                replaced = true;
                replacementState = world.random.nextFloat() > 0.4 ? Blocks.DEAD_BUSH.getDefaultState() : Blocks.AIR.getDefaultState();
                if (percentageVaried < 0.5f) replacementState = Blocks.AIR.getDefaultState();
            } else if (targetBlock.isOf(Blocks.GRASS_BLOCK)) {
                replaced = true;
                if (percentageVaried < 0.05) {
                    replacementState = world.random.nextFloat() > 0.5 ? Blocks.TUFF.getDefaultState() : Blocks.MAGMA_BLOCK.getDefaultState();
                } else if (percentageVaried < 0.3) {
                    replacementState = world.random.nextFloat() > 0.2 ? Blocks.TUFF.getDefaultState() : Blocks.MAGMA_BLOCK.getDefaultState();
                } else if (percentageVaried < 0.55) {
                    replacementState = world.random.nextFloat() > 0.1 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.MAGMA_BLOCK.getDefaultState();
                } else {
                    replacementState = Blocks.DIRT.getDefaultState();
                }

                if (world.random.nextFloat() > 0.7) replaced = false;
            } else if (targetBlock.isOf(Blocks.DIRT)) {
                replaced = true;
                if (percentageVaried < 0.15) {
                    replacementState = world.random.nextFloat() > 0.6 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.MAGMA_BLOCK.getDefaultState();
                } else if (percentageVaried < 0.3) {
                    replacementState = world.random.nextFloat() > 0.3 ? Blocks.TUFF.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState();
                } else if (percentageVaried < 0.65) {
                    replacementState = world.random.nextFloat() > 0.2 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.TUFF.getDefaultState();
                } else {
                    replaced = false;
                }

                if (world.random.nextFloat() > 0.1) replaced = false;
            } else if (targetBlock.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
                replaced = true;
                if (percentageVaried < 0.3) {
                    replacementState = world.random.nextFloat() > 0.5 ? Blocks.DEEPSLATE.getDefaultState() : Blocks.MAGMA_BLOCK.getDefaultState();
                } else if (percentageVaried < 0.5) {
                    replacementState = world.random.nextFloat() > 0.3 ? Blocks.STONE.getDefaultState() : Blocks.MAGMA_BLOCK.getDefaultState();
                } else if (percentageVaried < 0.7) {
                    replacementState = world.random.nextFloat() > 0.2 ? Blocks.GRANITE.getDefaultState() : Blocks.MAGMA_BLOCK.getDefaultState();
                } else {
                    replaced = false;
                }
            } else if (targetBlock.isIn(BlockTags.SAND) || targetBlock.isOf(Blocks.SANDSTONE)) {
                replaced = true;
                if (percentageVaried < 0.2) {
                    replacementState = world.random.nextFloat() > 0.7 ? Blocks.SANDSTONE.getDefaultState() : Blocks.MAGMA_BLOCK.getDefaultState();
                } else {
                    replacementState = Blocks.GLASS.getDefaultState();
                }

                if (percentageVaried > 0.8) replaced = false;
            }

            if (replaced) {
                world.setBlockState(target, replacementState, Block.SKIP_DROPS | Block.NOTIFY_LISTENERS, 1);

                // random fire chance
                if (world.getBlockState(target.up()).isReplaceable() && world.random.nextFloat() > 0.97) {
                    world.setBlockState(target.up(), Blocks.FIRE.getDefaultState(), Block.SKIP_DROPS | Block.NOTIFY_LISTENERS, 0);
                }
            }
        });

        borderBlocks.clear();
    }

    private void collectExtraEdgeBlocks(BlockPos center) {
        BlockPos.iterate(center.add(-8, -8, -8), center.add(8, 8, 8)).forEach(target -> {
            if (removedBlocks.contains(target)) return;
            var targetState = world.getBlockState(target);
            if (targetState.isAir()) return;
            borderBlocks.add(target.toImmutable());
        });
    }

    // remove all blocks in X radius below hardness 'power', return amount of hardness used in total
    // also damage entities
    private int explosionSphere(int radius, int power, BlockPos pos) {

        var radiusSq = radius * radius;
        var radiusSqExtra = (radius + 3) * (radius + 3);
        var usedPower = 0;
        var hardBusters = radius;

        for (var target : BlockPos.iterateOutwards(pos, radius + 3, radius + 3, radius + 3)) {
            if (removedBlocks.contains(target)) continue;
            var distSq = target.getSquaredDistance(pos);

            if (distSq > radiusSq) {
                if (distSq <= (radiusSqExtra)) {
                    // border block, was almost destroyed
                    borderBlocks.add(target.toImmutable());
                }
                continue;
            }

            // if less than half dist, 100%, then slowly ramp up to 0%
            var removalPercentage = (distSq - radiusSq / 2f) / radiusSq;
            if (world.random.nextFloat() < removalPercentage - 0.2) {
                borderBlocks.add(target.toImmutable());
                continue;
            }

            var targetState = world.getBlockState(target);
            var targetBlock = targetState.getBlock();
            var targetHardness = targetBlock.getBlastResistance();

            if (targetBlock instanceof NuclearExplosionBlock || targetState.isAir() && !targetState.getFluidState().isStill() || targetState.getHardness(world, pos) < 0)
                continue;

            // skip too hard blocks (except for the first few)
            if (targetHardness > power && hardBusters-- < 0) continue;

            usedPower += targetHardness;

            // todo find all onBreak overrides in project and move to onBroken
            targetBlock.onBroken(world, pos, targetState);
            world.setBlockState(target, Blocks.AIR.getDefaultState(), Block.SKIP_DROPS | Block.NOTIFY_LISTENERS, 0);
            removedBlocks.add(target.toImmutable());
            borderBlocks.remove(target.toImmutable());

            // damages all entities in radius based on distance
            var entityCandidates = world.getEntitiesByClass(
                    LivingEntity.class,
                    new Box(pos.subtract(new Vec3i(radius, radius, radius)).toCenterPos(), pos.add(new Vec3i(radius, radius, radius)).toCenterPos()),
                    EntityPredicates.VALID_LIVING_ENTITY.and(EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR));

            entityCandidates.forEach(entity -> {
                var entityDist = entity.squaredDistanceTo(pos.toCenterPos());
                var distPercentage = entityDist / radiusSq;
                var damage = radiusSq / distPercentage; // closer entities take much more damage
                System.out.println(entityDist + ":" + damage);
                entity.damage(new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.EXPLOSION)), (float) damage);
            });

        }

        return usedPower;
    }

    private List<Vec3d> getRandomRayDirections(int count) {
        List<Vec3d> rayDirections = new ArrayList<>(count);

        // Divide the circle into 12 equal parts
        var angleIncrement = 2 * Math.PI / count; // 360 degrees / 12

        for (int i = 0; i < count; i++) {
            // Calculate the base angle for this ray
            var baseAngle = i * angleIncrement;

            // Add a small random perturbation to the angle
            var randomPerturbation = (world.random.nextFloat() - 0.5) * (angleIncrement / 2);

            // Final angle with randomness
            var angle = baseAngle + randomPerturbation;

            // Calculate the direction vector
            var x = Math.cos(angle);
            var z = Math.sin(angle);

            rayDirections.add(new Vec3d(x, 0, z)); // Horizontal direction
        }

        return rayDirections;
    }

    private Vec3d addRandomOffset(Vec3d direction, float amount) {
        return direction.add(world.random.nextFloat() * amount - amount / 2, world.random.nextFloat() * amount - amount / 2, world.random.nextFloat() * amount - amount / 2);
    }

    private class DirectionExplosionWave {

        private final Vec3d direction;

        private int lastRadius;
        private BlockPos lastPosition;
        private int lastRadiusReduction;

        private DirectionExplosionWave(int initialRadius, Vec3d direction, BlockPos pos) {
            this.direction = direction;
            this.lastRadius = initialRadius;
            this.lastPosition = pos;
            this.lastRadiusReduction = 1;
        }

        private void nextGeneration() {
            var currentRadius = lastRadius - lastRadiusReduction;
            if (currentRadius <= 1) return;
            var rayOffset = direction.multiply(currentRadius);
            var target = lastPosition.add(BlockPos.ofFloored(rayOffset));
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
                collectExtraEdgeBlocks(target.add(BlockPos.ofFloored(rayOffset.multiply(3))));

        }
    }
}
