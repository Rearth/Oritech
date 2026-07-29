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
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.block.blocks.interaction.EndericLaserBlock;
import rearth.oritech.block.entity.interaction.DestroyerBlockEntity;
import rearth.oritech.block.entity.interaction.EndericLaserBlockEntity;
import rearth.oritech.block.entity.processing.AtomicForgeBlockEntity;
import rearth.oritech.block.entity.storage.SchrodingersSafeBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.TagContent;

public class EndericLaserBlockBehavior {
    static private EndericLaserBlockBehavior noop;
    static private EndericLaserBlockBehavior transferPowerBehavior;
    static private EndericLaserBlockBehavior energizeBuddingBehavior;

    /**
     * Perform laser behavior on block
     */
    public boolean fireAtBlock(Level level, EndericLaserBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
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
        noop = new EndericLaserBlockBehavior() {
            @Override
            public boolean fireAtBlock(Level level, EndericLaserBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
                // don't do anything, and don't keep targetting this block
                return false;
            }
        };
        EndericLaserBlock.registerBlockBehavior(Blocks.TARGET, noop);
        EndericLaserBlock.registerBlockBehavior(Blocks.BEDROCK, noop);

        transferPowerBehavior = new EndericLaserBlockBehavior() {
            @Override
            public boolean fireAtBlock(Level level, EndericLaserBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
                var storageCandidate = level.getCapability(Capabilities.Energy.BLOCK, blockPos, blockState, blockEntity, null);

                if (storageCandidate == null && blockEntity instanceof EnergyProvider energyProvider)
                    storageCandidate = energyProvider.getEnergyLookup(null);

                if (blockEntity instanceof SchrodingersSafeBlockEntity schrodingersSafeBlockEntity)
                    storageCandidate = schrodingersSafeBlockEntity.laserInputStorage;

                var insertAmount = storageCandidate.getCapacityAsLong() - storageCandidate.getAmountAsLong();
                if (insertAmount <= 0 || storageCandidate.getCapacityAsLong() <= 1)
                    return false;

                var transferCapacity = (int) Math.min(insertAmount, laserEntity.energyRequiredToFire());

                try (var transaction = Transaction.openRoot()) {
                    long inserted;
                    if (storageCandidate instanceof DynamicEnergyStorage dynamicStorage) {
                        inserted = dynamicStorage.internalInsert(transferCapacity, transaction);
                    } else {
                        inserted = storageCandidate.insert(transferCapacity, transaction);
                    }
                    if (inserted > 0) {
                        transaction.commit();

                        if (blockEntity instanceof AtomicForgeBlockEntity atomicForge)
                            atomicForge.lastWorkedAt = level.getGameTime();

                        return true;
                    }
                }
                return false;
            }
        };

        EndericLaserBlock.registerBlockBehavior(BlockContent.ATOMIC_FORGE.get(), transferPowerBehavior);
        EndericLaserBlock.registerBlockBehavior(BlockContent.BEDROCK_EXTRACTOR.get(), transferPowerBehavior);
        EndericLaserBlock.registerBlockBehavior(BlockContent.ARCANE_CATALYST.get(), transferPowerBehavior);

        energizeBuddingBehavior = new EndericLaserBlockBehavior() {
            @Override
            public boolean fireAtBlock(Level level, EndericLaserBlockEntity laserEntity, Block block, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {

                if (level.getGameTime() % 40 == 0) {    // periodically reset target
                    return false;
                }
                if (blockState.isAir() || !blockState.getFluidState().isEmpty()) return false;

                blockState.randomTick((ServerLevel) level, blockPos, level.getRandom());
                ParticleContent.Accelerating(level, Vec3.atLowerCornerOf(blockPos));

                return true;
            }
        };

        EndericLaserBlock.registerBlockBehavior(Blocks.BUDDING_AMETHYST, energizeBuddingBehavior);
    }
}
