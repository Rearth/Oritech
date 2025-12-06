package rearth.oritech.block.entity.interaction;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.world.level.material.Fluid;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ColorableMachine;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class PumpBlockEntity extends NetworkedBlockEntity implements FluidApi.BlockProvider, EnergyApi.BlockProvider, GeoBlockEntity, ColorableMachine {
    
    private static final int MAX_SEARCH_COUNT = 100_000;
    private static final int ENERGY_USAGE = 512;   // per block pumped
    private static final int PUMP_RATE = 5; // pump every n ticks
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<PumpBlockEntity> animationController = getAnimationController();
    
    private final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(16 * FluidStackHooks.bucketAmount(), this::setChanged);
    
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(1000, 0, 20_000);
    private boolean initialized = false;
    private boolean toolheadLowered = false;
    private boolean searchActive = false;
    private BlockPos toolheadPosition;
    private FloodFillSearch searchInstance;
    private Deque<BlockPos> pendingLiquidPositions;
    
    @SyncField(SyncType.TICK)
    private long lastWorkTime;
    
    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorableMachine.ColorVariant currentColor = getDefaultColor();
    
    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PUMP_BLOCK, pos, state);
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        fluidStorage.writeNbt(nbt, "");
        nbt.putBoolean("initialized", initialized);
        nbt.putLong("energy", energyStorage.getAmount());
        addColorToNbt(nbt);
        
        if (pendingLiquidPositions != null)
            nbt.putLongArray("pendingTargets", pendingLiquidPositions.stream().mapToLong(BlockPos::asLong).toArray());
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        loadColorFromNbt(nbt);
        initialized = nbt.getBoolean("initialized");
        fluidStorage.readNbt(nbt, "");
        energyStorage.setAmount(nbt.getLong("energy"));
        pendingLiquidPositions = Arrays.stream(nbt.getLongArray("pendingTargets")).mapToObj(BlockPos::of).collect(Collectors.toCollection(ArrayDeque::new));
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        if ((initialized && pendingLiquidPositions.isEmpty() && world.getGameTime() % 62 == 0) || (!initialized && toolheadLowered && !searchActive && world.getGameTime() % 62 == 0)) {
            // reset
            initialized = false;
            toolheadLowered = false;
            searchActive = false;
            toolheadPosition = pos;
        }
        
        if (!initialized) {
            progressStartup();
            return;
        }
        
        if (world.getGameTime() % PUMP_RATE == 0 && hasEnoughEnergy() && world.getBestNeighborSignal(pos) <= 0) {
            
            if (pendingLiquidPositions.isEmpty() || tankIsFull()) return;
            
            var targetBlock = pendingLiquidPositions.peekLast();
            
            // Only drain the source (still) fluid, so it doesn't keep pumping infinitely
            if (!world.getBlockState(targetBlock).getFluidState().isSource()) {
                pendingLiquidPositions.pollLast();
                return;
            }
            
            var targetState = world.getFluidState(targetBlock);
            if (!targetState.getType().isSame(Fluids.WATER)) {
                drainSourceBlock(targetBlock);
                pendingLiquidPositions.pollLast();
            }
            
            addLiquidToTank(targetState);
            useEnergy();
            this.setChanged();
            lastWorkTime = world.getGameTime();
            
            
            var targetPos = pos.getCenter().offsetRandom(world.random, 0.5f);
            var targetType = targetState.getDripParticle();
            
            if (targetType != null && world instanceof ServerLevel serverWorld)
                serverWorld.sendParticles(targetType, targetPos.x(), targetPos.y(), targetPos.z(), 1, 0, 0, 0, 1);
        }
        
    }
    
    private boolean isBusy() {
        return level.getGameTime() - lastWorkTime < 40;
    }
    
    public void onUsed(Player player) {
        
        var message = Component.translatable("message.oritech.pump.starting");
        if (!initialized) {
            if (!toolheadLowered) {
                message = Component.translatable("message.oritech.pump.extending");
            } else if (searchActive) {
                message = Component.translatable("message.oritech.pump.initializing");
            } else {
                message = Component.translatable("message.oritech.pump.no_fluids");
            }
        } else if (isBusy()) {
            message = Component.translatable("message.oritech.pump.busy");
        } else if (!hasEnoughEnergy()) {
            message = Component.translatable("message.oritech.pump.low_energy");
        } else if (pendingLiquidPositions.isEmpty()) {
            message = Component.translatable("message.oritech.pump.pump_finished");
        } else if (tankIsFull()) {
            message = Component.translatable("message.oritech.pump.full");
        }
        
        player.displayClientMessage(message, true);
    }
    
    private boolean hasEnoughEnergy() {
        return energyStorage.getAmount() >= ENERGY_USAGE;
    }
    
    private void useEnergy() {
        energyStorage.extractIgnoringLimit(ENERGY_USAGE, false);
    }
    
    private boolean tankIsFull() {
        return fluidStorage.getAmount() > fluidStorage.getCapacity() - FluidStackHooks.bucketAmount();
    }
    
    private void addLiquidToTank(FluidState targetState) {
        fluidStorage.insert(FluidStack.create(targetState.getType(), FluidStackHooks.bucketAmount()), false);
    }
    
    private void drainSourceBlock(BlockPos targetBlock) {
        level.setBlockAndUpdate(targetBlock, Blocks.AIR.defaultBlockState());
    }
    
    private void progressStartup() {
        
        // startup sequence is:
        // move down until no longer in air
        // check if target is liquid
        // if liquid is water, consider as infinite
        // if liquid, start flood fill to find all liquid blocks. Add all found blocks to queue so that it can be soaked up in reverse
        // search all neighbors per tick
        // if more than N blocks are found, consider the search finished
        // mark startup as completed
        
        if (toolheadPosition == null) {
            toolheadPosition = worldPosition;
        }
        
        if (!toolheadLowered) {
            
            if (level.getGameTime() % 10 != 0)
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
            startLiquidSearch(toolheadPosition.below());
            return;
        }
        
        toolheadPosition = toolheadPosition.below();
        level.setBlockAndUpdate(toolheadPosition, BlockContent.PUMP_TRUNK_BLOCK.defaultBlockState());
    }
    
    private boolean checkToolheadEnd(BlockPos newPosition) {
        
        var posBelow = newPosition.below();
        var stateBelow = level.getBlockState(posBelow);
        var blockBelow = stateBelow.getBlock();
        
        var isAirOrTrunk = stateBelow.canBeReplaced() || blockBelow.equals(BlockContent.PUMP_TRUNK_BLOCK);
        var isFluid = !stateBelow.getFluidState().isEmpty();
        
        return isFluid || !isAirOrTrunk;
    }
    
    private void startLiquidSearch(BlockPos start) {
        
        var state = level.getFluidState(start);
        if (!state.isSource()) return;
        
        searchInstance = new FloodFillSearch(start, level, state.getType());
        searchActive = true;
        
        Oritech.LOGGER.debug("starting search at: " + start + " " + state.getType() + " " + state.isSource());
    }
    
    private void finishSearch() {
        Oritech.LOGGER.debug("search finished, found: " + searchInstance.foundTargets.size());
        pendingLiquidPositions = searchInstance.foundTargets;
        initialized = true;
        searchInstance = null;
    }
    
    @Override
    public ColorVariant getCurrentColor() {
        return currentColor;
    }
    
    @Override
    public void assignColor(ColorVariant color) {
        this.currentColor = color;
        
        if (this.level != null && !this.level.isClientSide()) {
            this.markDirty(false);
            this.sendUpdate(SyncType.SPARSE_TICK);
        }
    }
    
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(Direction direction) {
        return fluidStorage;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(animationController);
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    private AnimationController<PumpBlockEntity> getAnimationController() {
        return new AnimationController<>(this, state -> {
            if (isBusy()) {
                return state.setAndContinue(MachineBlockEntity.WORKING);
            } else {
                return state.setAndContinue(MachineBlockEntity.IDLE);
            }
        });
    }
    
    private static class FloodFillSearch {
        
        final HashSet<BlockPos> checkedPositions = new HashSet<>();
        final HashSet<BlockPos> nextTargets = new HashSet<>();
        final Deque<BlockPos> foundTargets = new ArrayDeque<>();
        final Level world;
        final Fluid fluidType;
        
        public FloodFillSearch(BlockPos startPosition, Level world, Fluid fluidType) {
            this.world = world;
            this.fluidType = fluidType;
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
            return world.getFluidState(target).getType().isSame(Fluids.WATER);
        }
        
        private boolean cutoffSearch() {
            return foundTargets.size() >= MAX_SEARCH_COUNT;
        }
        
        private boolean isValidTarget(BlockPos target) {
            var state = world.getFluidState(target);
            return !state.isEmpty() && state.is(fluidType);
        }
        
        private void addNeighborsToQueue(BlockPos self) {
            
            for (var neighbor : getNeighbors(self)) {
                if (checkedPositions.contains(neighbor)) continue;
                nextTargets.add(neighbor);
            }
            
        }
        
        // returns all neighboring positions except up
        private List<BlockPos> getNeighbors(BlockPos pos) {
            return List.of(pos.below(), pos.north(), pos.east(), pos.south(), pos.west());
        }
        
    }
}
