package rearth.oritech.block.behavior;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BuddingAmethystBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.machines.interaction.LaserArmBlock;
import rearth.oritech.block.entity.machines.interaction.LaserArmBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.datagen.data.TagContent;
import rearth.oritech.util.DynamicEnergyStorage;
import rearth.oritech.util.EnergyProvider;
import team.reborn.energy.api.EnergyStorage;

public class LaserArmBlockBehavior {
    static private LaserArmBlockBehavior noop;
    static private LaserArmBlockBehavior transferPowerBehavior;
    static private LaserArmBlockBehavior energizeBuddingBehavior;

    /**
     * Perform laser behavior on block
     */
    public boolean fireAtBlock(World world, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        if (laserEntity.hasCropFilterAddon && block instanceof CropBlock crop && !crop.isMature(blockState))
            return false;

        // has an energy storage, try to transfer power to it
        var storageCandidate = EnergyStorage.SIDED.find(world, blockPos, blockState, blockEntity, null);
        if (storageCandidate != null)
            return transferPowerBehavior.fireAtBlock(world, laserEntity, block, blockPos, blockState, blockEntity);
        
        // an unregistered budding block, attempt to energize it
        if (!blockState.isIn(ConventionalBlockTags.BUDDING_BLOCKS))
            return energizeBuddingBehavior.fireAtBlock(world, laserEntity, block, blockPos, blockState, blockEntity);
        
        // passes through, stop targetting this block
        if (blockState.isIn(TagContent.LASER_PASSTHROUGH))
            return false;

        laserEntity.addBlockBreakProgress(laserEntity.energyRequiredToFire());
        if (laserEntity.getBlockBreakProgress() >= laserEntity.getTargetBlockEnergyNeeded())
            laserEntity.finishBlockBreaking(blockPos, blockState);
        return true;
    }

    public static void registerDefaults() {
        noop = new LaserArmBlockBehavior() {
            @Override
            public boolean fireAtBlock(World world, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
                // don't do anything, and don't keep targetting this block
                return false;
            }
        };
        LaserArmBlock.registerBlockBehavior(Blocks.TARGET, noop);
        LaserArmBlock.registerBlockBehavior(Blocks.BEDROCK, noop);

        transferPowerBehavior = new LaserArmBlockBehavior() {
            @Override
            public boolean fireAtBlock(World world, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
                var storageCandidate = EnergyStorage.SIDED.find(world, blockPos, blockState, blockEntity, null);
                if (storageCandidate == null && blockEntity instanceof EnergyProvider energyProvider)
                    storageCandidate = energyProvider.getStorage(null);
                var insertAmount = storageCandidate.getCapacity() - storageCandidate.getAmount();
                if (insertAmount < 10)
                    return false;
                var transferCapacity = Math.min(insertAmount, laserEntity.energyRequiredToFire());

                if (storageCandidate instanceof DynamicEnergyStorage dynamicStorage) {
                    dynamicStorage.amount += transferCapacity;  // direct transfer, allowing to insert into any container, even when inserting isnt allowed (e.g. atomic forge)
                    dynamicStorage.onFinalCommit(); // gross abuse of transaction system to force it to sync
                } else {
                    try (var tx = Transaction.openOuter()) {
                        long inserted = storageCandidate.insert(transferCapacity, tx);
                        if (inserted == transferCapacity) {
                            tx.commit();
                        } else {
                            // inserted amount didn't match the expected inserted amount
                            return false;
                        }
                    }
                }
                return true;
            }
        };
        LaserArmBlock.registerBlockBehavior(BlockContent.ATOMIC_FORGE_BLOCK, transferPowerBehavior);
        LaserArmBlock.registerBlockBehavior(BlockContent.DEEP_DRILL_BLOCK, transferPowerBehavior);
        LaserArmBlock.registerBlockBehavior(BlockContent.ENCHANTMENT_CATALYST_BLOCK, transferPowerBehavior);

        energizeBuddingBehavior = new LaserArmBlockBehavior() {
            @Override
            public boolean fireAtBlock(World world, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
                if (buddingAmethystCanGrow(world, blockState, blockPos)) {
                    try {
                        // Using reflection instead of typecasting so that other "budding" geodes will work with the laser as long as they're tagged with c:budding_blocks
                        Method randomTick = block.getClass().getMethod("randomTick", BlockState.class, ServerWorld.class, BlockPos.class, Random.class);
                        randomTick.invoke(blockState, (ServerWorld)world, blockPos, world.random);
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
                        // weird state, probably a modded "budding" block that doesn't subclass BuddingAmethystBlock or have a similar randomTick method
                        Oritech.LOGGER.error("laser attempting to overload budding block {} at {}, but does not recognize block", block, blockPos);
                        return false;
                    }
                    ParticleContent.ACCELERATING.spawn(world, Vec3d.of(blockPos));
                    return true;
                }
                return false;
            }
        };
        LaserArmBlock.registerBlockBehavior(Blocks.BUDDING_AMETHYST, energizeBuddingBehavior);
    }

    private static boolean buddingAmethystCanGrow(World world, BlockState blockState, BlockPos pos) {
        if (!blockState.isIn(ConventionalBlockTags.BUDDING_BLOCKS))
            return false;
        
        // returning true means the laser will keep firing at the budding amethyst block
        // this means that a laser arm will fire at a budding amethyst block for up to 20 ticks even if the clusters are already fully grown
        // it also means that it will only check the blockstates of the surrounding 6 blocks every 20 ticks instead of every tick
        if (world.getTime() % 20 != 0) {
            return true;
        }

        for (var direction : Direction.values()) {
            var growingPos = pos.offset(direction);
            var growingState = world.getBlockState(growingPos);
            if (BuddingAmethystBlock.canGrowIn(growingState) || blockState.isIn(ConventionalBlockTags.BUDS))
                return true;
        }

        return false;
    }   
}
