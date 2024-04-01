package rearth.oritech.block.entity.machines.interaction;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.EnergyProvider;
import rearth.oritech.util.FluidProvider;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.*;
import java.util.stream.Collectors;

public class PumpBlockEntity extends BlockEntity implements BlockEntityTicker<PumpBlockEntity>, FluidProvider, EnergyProvider {
    
    private static final int MAX_SEARCH_COUNT = 100000;
    private static final int ENERGY_USAGE = 1000;   // per block pumped
    private static final int PUMP_RATE = 5; // pump every n ticks
    
    private final SingleVariantStorage<FluidVariant> fluidStorage = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }
        
        @Override
        protected long getCapacity(FluidVariant variant) {
            return (16 * FluidConstants.BUCKET);
        }
        
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            PumpBlockEntity.this.markDirty();
        }
    };
    
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(20000, 1000, 0);
    private boolean initialized = false;
    private boolean toolheadLowered = false;
    private boolean searchActive = false;
    private BlockPos toolheadPosition;
    private FloodFillSearch searchInstance;
    private Deque<BlockPos> pendingLiquidPositions;
    
    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PUMP_BLOCK, pos, state);
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("initialized", initialized);
        nbt.put("fluidVariant", fluidStorage.variant.toNbt());
        nbt.putLong("amount", fluidStorage.amount);
        nbt.putLong("energy", energyStorage.getAmount());
        
        if (pendingLiquidPositions != null)
            nbt.putLongArray("pendingTargets", pendingLiquidPositions.stream().mapToLong(BlockPos::asLong).toArray());
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        initialized = nbt.getBoolean("initialized");
        fluidStorage.variant = FluidVariant.fromNbt(nbt.getCompound("fluidVariant"));
        fluidStorage.amount = nbt.getLong("amount");
        energyStorage.amount = nbt.getLong("energy");
        pendingLiquidPositions = Arrays.stream(nbt.getLongArray("pendingTargets")).mapToObj(BlockPos::fromLong).collect(Collectors.toCollection(ArrayDeque::new));
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, PumpBlockEntity blockEntity) {
        if (world.isClient) return;
        
        if (!initialized) {
            progressStartup();
            return;
        }
        
        if (world.getTime() % PUMP_RATE == 0 && hasEnoughEnergy()) {
            
            if (pendingLiquidPositions.isEmpty() || tankIsFull()) return;
            
            var targetBlock = pendingLiquidPositions.peekLast();
            
            if (!world.getBlockState(targetBlock).isLiquid()) {
                pendingLiquidPositions.pollLast();
                return;
            }
            
            var targetState = world.getFluidState(targetBlock);
            if (!targetState.getFluid().matchesType(Fluids.WATER)) {
                drainSourceBlock(targetBlock);
                pendingLiquidPositions.pollLast();
            }
            
            addLiquidToTank(targetState);
            useEnergy();
            this.markDirty();
        }
        
    }
    
    private boolean hasEnoughEnergy() {
        return energyStorage.getAmount() >= ENERGY_USAGE;
    }
    
    private void useEnergy() {
        energyStorage.amount -= ENERGY_USAGE;
    }
    
    private boolean tankIsFull() {
        return fluidStorage.amount > fluidStorage.getCapacity() - FluidConstants.BUCKET;
    }
    
    private void addLiquidToTank(FluidState targetState) {
        try (var tx = Transaction.openOuter()) {
            fluidStorage.insert(FluidVariant.of(targetState.getFluid()), FluidConstants.BUCKET, tx);
            tx.commit();
        }
    }
    
    private void drainSourceBlock(BlockPos targetBlock) {
        world.setBlockState(targetBlock, Blocks.AIR.getDefaultState());
    }
    
    private void progressStartup() {
        
        // startup sequence is:
        // move down until no longer in air
        // check if target is liquid
        // if liquid is water, consider as infinite
        // if liquid, start flood fill to find all liquid blocks. Add all found blocks to queue so that it can be soaked up in reverse
        // search all neighbors per tick
        // if more than 10000 blocks are found, consider as infinite and stop search
        // mark startup as completed
        
        if (toolheadPosition == null) {
            toolheadPosition = pos;
        }
        
        if (!toolheadLowered) {
            
            if (world.getTime() % 10 != 0)
                moveToolheadDown();
            
            return;
        }
        
        if (searchActive) {
            if (searchInstance.nextGeneration()) {
                finishSearch();
                searchActive = false;
            }
        }
    }
    
    private void moveToolheadDown() {
        toolheadLowered = checkToolheadEnd(toolheadPosition);
        if (toolheadLowered) {
            startLiquidSearch(toolheadPosition.down());
            return;
        }
        
        toolheadPosition = toolheadPosition.down();
        world.setBlockState(toolheadPosition, BlockContent.BANANA_BLOCK.getDefaultState());
    }
    
    private boolean checkToolheadEnd(BlockPos newPosition) {
        
        var posBelow = newPosition.down();
        var stateBelow = world.getBlockState(posBelow);
        var blockBelow = stateBelow.getBlock();
        
        return !(blockBelow.equals(Blocks.AIR) || blockBelow.equals(BlockContent.BANANA_BLOCK));
    }
    
    private void startLiquidSearch(BlockPos start) {
        
        var state = world.getFluidState(start);
        if (!state.isStill()) return;
        
        searchInstance = new FloodFillSearch(start, world);
        searchActive = true;
        
        System.out.println("starting search at: " + start + " " + state.getFluid() + " " + state.isStill());
    }
    
    private void finishSearch() {
        System.out.println("search finished, found: " + searchInstance.foundTargets.size());
        pendingLiquidPositions = searchInstance.foundTargets;
        initialized = true;
        searchInstance = null;
    }
    
    @Override
    public Storage<FluidVariant> getFluidStorage(Direction direction) {
        return fluidStorage;
    }
    
    @Override
    public EnergyStorage getStorage(Direction direction) {
        return energyStorage;
    }
    
    private static class FloodFillSearch {
        
        final HashSet<BlockPos> checkedPositions = new HashSet<>();
        final HashSet<BlockPos> nextTargets = new HashSet<>();
        final Deque<BlockPos> foundTargets = new ArrayDeque<>();
        final World world;
        
        public FloodFillSearch(BlockPos startPosition, World world) {
            this.world = world;
            nextTargets.add(startPosition);
        }
        
        // returns true when done
        @SuppressWarnings("unchecked")
        public boolean nextGeneration() {
            
            var currentGeneration = (HashSet<BlockPos>) nextTargets.clone();
            
            var earlyStop = false;
            
            for (var target : currentGeneration) {
                if (isValidTarget(target)) {
                    foundTargets.addLast(target);
                    addNeighborsToQueue(target);
                    if (checkForEarlyStop(target)) earlyStop = true;
                }
                
                checkedPositions.add(target);
                nextTargets.remove(target);
            }
            
            if (cutoffSearch() || earlyStop) nextTargets.clear();
            
            return nextTargets.isEmpty();
        }
        
        private boolean checkForEarlyStop(BlockPos target) {
            return world.getFluidState(target).getFluid().matchesType(Fluids.WATER);
        }
        
        private boolean cutoffSearch() {
            return foundTargets.size() >= MAX_SEARCH_COUNT;
        }
        
        private boolean isValidTarget(BlockPos target) {
            var state = world.getFluidState(target);
            return state.isStill();
        }
        
        private void addNeighborsToQueue(BlockPos self) {
            
            for (var neighbor : getNeighbors(self)) {
                if (checkedPositions.contains(neighbor)) continue;
                nextTargets.add(neighbor);
            }
            
        }
        
        // returns all neighboring positions except up
        private List<BlockPos> getNeighbors(BlockPos pos) {
            return List.of(pos.down(), pos.north(), pos.east(), pos.south(), pos.west());
        }
        
    }
}
