package rearth.oritech.block.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.api.transfer.energy.EnergyProvider;
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
    public boolean fireAtBlock(Level level, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        if (laserEntity.hasCropFilterAddon && DestroyerBlockEntity.isImmatureCrop(blockState, level, blockPos))
            return false;

        // has an energy storage, try to transfer power to it
        var storageCandidate = level.getCapability(Capabilities.Energy.BLOCK, blockPos, blockState, blockEntity, null);
        // if the storage is not exposed (e.g. catalyst / deep drill / atomic forge), get it directly
        if (storageCandidate == null && blockEntity instanceof EnergyProvider provider)
            storageCandidate = provider.getEnergyLookup(null);
        if (storageCandidate != null)
            return transferPowerBehavior.fireAtBlock(level, laserEntity, block, blockPos, blockState, blockEntity);

        // an unregistered budding block, attempt to energize it
        if (blockState.is(TagContent.LASER_ACCELERATED))
            return energizeBuddingBehavior.fireAtBlock(level, laserEntity, block, blockPos, blockState, blockEntity);

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
            public boolean fireAtBlock(Level level, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
                // don't do anything, and don't keep targetting this block
                return false;
            }
        };
        LaserArmBlock.registerBlockBehavior(Blocks.TARGET, noop);
        LaserArmBlock.registerBlockBehavior(Blocks.BEDROCK, noop);

        transferPowerBehavior = new LaserArmBlockBehavior() {
            @Override
            public boolean fireAtBlock(Level level, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
                var storageCandidate = level.getCapability(Capabilities.Energy.BLOCK, blockPos, blockState, blockEntity, null);

                if (storageCandidate == null && blockEntity instanceof EnergyProvider energyProvider)
                    storageCandidate = energyProvider.getEnergyLookup(null);

                if (blockEntity instanceof UnstableContainerBlockEntity unstableContainerBlockEntity)
                    storageCandidate = unstableContainerBlockEntity.laserInputStorage;

                var insertAmount = storageCandidate.getCapacityAsLong() - storageCandidate.getAmountAsLong();
                if (insertAmount <= 0 || storageCandidate.getCapacityAsLong() <= 1)
                    return false;

                var transferCapacity = (int) Math.min(Integer.MAX_VALUE, Math.min(insertAmount, laserEntity.energyRequiredToFire()));

                try (var transaction = Transaction.openRoot()) {
                    var inserted = storageCandidate.insert(transferCapacity, transaction);
                    if (inserted > 0) {
                        transaction.commit();

                        if (blockEntity instanceof AtomicForgeBlockEntity atomicForgeBlock)
                            atomicForgeBlock.lastWorkedAt = level.getGameTime();

                        return true;
                    }
                }
                return false;
            }
        };

        LaserArmBlock.registerBlockBehavior(BlockContent.ATOMIC_FORGE_BLOCK.get(), transferPowerBehavior);
        LaserArmBlock.registerBlockBehavior(BlockContent.DEEP_DRILL_BLOCK.get(), transferPowerBehavior);
        LaserArmBlock.registerBlockBehavior(BlockContent.ENCHANTMENT_CATALYST_BLOCK.get(), transferPowerBehavior);

        energizeBuddingBehavior = new LaserArmBlockBehavior() {
            @Override
            public boolean fireAtBlock(Level level, LaserArmBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {

                if (level.getGameTime() % 40 == 0) {    // periodically reset target
                    return false;
                }
                if (blockState.isAir() || !blockState.getFluidState().isEmpty()) return false;

                blockState.randomTick((ServerLevel) level, blockPos, level.getRandom());
                ParticleContent.Accelerating(level, Vec3.atLowerCornerOf(blockPos));

                return true;
            }
        };

        LaserArmBlock.registerBlockBehavior(Blocks.BUDDING_AMETHYST, energizeBuddingBehavior);
    }
}
