package rearth.oritech.block.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.EnergyApi.EnergyStorage;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.block.blocks.interaction.LaserArmBlock;
import rearth.oritech.block.entity.interaction.DestroyerBlockEntity;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.block.entity.processing.AtomicForgeBlockEntity;
import rearth.oritech.block.entity.storage.UnstableContainerBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.TagContent;

public class LaserArmBlockBehavior {
    static private LaserArmBlockBehavior noop;
    static private LaserArmBlockBehavior transferPowerBehavior;
    static private LaserArmBlockBehavior energizeBuddingBehavior;
    
    /**
     * Perform laser behavior on block
     */
    public boolean fireAtBlock(Level world, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        if (laserEntity.hasCropFilterAddon && DestroyerBlockEntity.isImmatureCrop(blockState))
            return false;
        
        // has an energy storage, try to transfer power to it
        var storageCandidate = EnergyApi.BLOCK.find(world, blockPos, blockState, blockEntity, null);
        // if the storage is not exposed (e.g. catalyst / deep drill / atomic forge), get it directly
        if (storageCandidate == null && blockEntity instanceof EnergyApi.BlockProvider provider)
            storageCandidate = provider.getEnergyStorage(null);
        if (storageCandidate != null)
            return transferPowerBehavior.fireAtBlock(world, laserEntity, block, blockPos, blockState, blockEntity);
        
        // an unregistered budding block, attempt to energize it
        if (blockState.is(TagContent.LASER_ACCELERATED))
            return energizeBuddingBehavior.fireAtBlock(world, laserEntity, block, blockPos, blockState, blockEntity);
        
        // passes through, stop targetting this block
        if (blockState.is(TagContent.LASER_PASSTHROUGH))
            return false;
        
        laserEntity.addBlockBreakProgress(laserEntity.energyRequiredToFire());
        if (laserEntity.getBlockBreakProgress() >= laserEntity.getTargetBlockEnergyNeeded())
            laserEntity.finishBlockBreaking(blockPos, blockState);
        return true;
    }
    
    public static void registerDefaults() {
        noop = new LaserArmBlockBehavior() {
            @Override
            public boolean fireAtBlock(Level world, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
                // don't do anything, and don't keep targetting this block
                return false;
            }
        };
        LaserArmBlock.registerBlockBehavior(Blocks.TARGET, noop);
        LaserArmBlock.registerBlockBehavior(Blocks.BEDROCK, noop);
        
        transferPowerBehavior = new LaserArmBlockBehavior() {
            @Override
            public boolean fireAtBlock(Level world, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
                var storageCandidate = EnergyApi.BLOCK.find(world, blockPos, blockState, blockEntity, null);
                
                if (storageCandidate == null && blockEntity instanceof EnergyApi.BlockProvider energyProvider)
                    storageCandidate = energyProvider.getEnergyStorage(null);
                
                if (blockEntity instanceof UnstableContainerBlockEntity unstableContainerBlockEntity)
                    storageCandidate = unstableContainerBlockEntity.laserInputStorage;
                
                var insertAmount = storageCandidate.getCapacity() - storageCandidate.getAmount();
                if (insertAmount <= 0)
                    return false;
                
                var transferCapacity = Math.min(insertAmount, laserEntity.energyRequiredToFire());
                
                if (storageCandidate instanceof DynamicEnergyStorage dynamicStorage) {
                    var inserted = dynamicStorage.insertIgnoringLimit(transferCapacity, true);
                    if (inserted > 0 && inserted <= transferCapacity) {
                        dynamicStorage.insertIgnoringLimit(transferCapacity, false);
                        dynamicStorage.update();
                        
                        if (blockEntity instanceof AtomicForgeBlockEntity atomicForgeBlock)
                            atomicForgeBlock.lastWorkedAt = world.getGameTime();
                        
                        return true;
                    }
                    return false;
                } else {
                    var inserted = storageCandidate.insert(transferCapacity, true);
                    if (inserted > 0 && inserted <= transferCapacity) {
                        storageCandidate.insert(transferCapacity, false);
                        storageCandidate.update();
                        return true;
                    }
                    return false;
                }
            }
        };
        LaserArmBlock.registerBlockBehavior(BlockContent.ATOMIC_FORGE_BLOCK, transferPowerBehavior);
        LaserArmBlock.registerBlockBehavior(BlockContent.DEEP_DRILL_BLOCK, transferPowerBehavior);
        LaserArmBlock.registerBlockBehavior(BlockContent.ENCHANTMENT_CATALYST_BLOCK, transferPowerBehavior);
        
        energizeBuddingBehavior = new LaserArmBlockBehavior() {
            @Override
            public boolean fireAtBlock(Level world, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
                
                if (world.getGameTime() % 40 == 0) {    // periodically reset target
                    return false;
                }
                if (blockState.isAir() || blockState.getFluidState().isSource()) return false;
                
                blockState.randomTick((ServerLevel) world, blockPos, world.random);
                ParticleContent.ACCELERATING.spawn(world, Vec3.atLowerCornerOf(blockPos));
                
                return true;
            }
        };
        
        LaserArmBlock.registerBlockBehavior(Blocks.BUDDING_AMETHYST, energizeBuddingBehavior);
    }
}
