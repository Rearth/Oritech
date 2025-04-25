package rearth.oritech.block.entity.interaction;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.network.NetworkContent;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.stream.Collectors;

public class PumpBlockEntity extends BlockEntity implements BlockEntityTicker<PumpBlockEntity>, FluidApi.BlockProvider, EnergyApi.BlockProvider, GeoBlockEntity {
    
    private static final int MAX_SEARCH_COUNT = 100_000;
    private static final int ENERGY_USAGE = 512;   // per block pumped
    private static final int PUMP_RATE = 5; // pump every n ticks
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<PumpBlockEntity> animationController = getAnimationController();
    
    // client only
    public Fluid lastPumpedVariant;
    public long lastPumpTime;
    
    private final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(16 * FluidStackHooks.bucketAmount(), this::markDirty);
    
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(1000, 0, 20_000);
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
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        fluidStorage.writeNbt(nbt, "");
        nbt.putBoolean("initialized", initialized);
        nbt.putLong("energy", energyStorage.getAmount());
        
        if (pendingLiquidPositions != null)
            nbt.putLongArray("pendingTargets", pendingLiquidPositions.stream().mapToLong(BlockPos::asLong).toArray());
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        initialized = nbt.getBoolean("initialized");
        fluidStorage.readNbt(nbt, "");
        energyStorage.setAmount(nbt.getLong("energy"));
        pendingLiquidPositions = Arrays.stream(nbt.getLongArray("pendingTargets")).mapToObj(BlockPos::fromLong).collect(Collectors.toCollection(ArrayDeque::new));
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, PumpBlockEntity blockEntity) {
        if (world.isClient) {
            if (isBusy())
                spawnWorkingParticles();
            return;
        }
        
        if (!initialized) {
            progressStartup();
            return;
        }
        
        if (world.getTime() % PUMP_RATE == 0 && hasEnoughEnergy()) {
            
            if (pendingLiquidPositions.isEmpty() || tankIsFull()) return;
            
            var targetBlock = pendingLiquidPositions.peekLast();
            
            if (!world.getBlockState(targetBlock).getFluidState().isStill()) {
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
            setLastPumpTime(world.getTime());
            updateNetwork();
        }
        
    }
    
    public void onUsed(PlayerEntity player) {
        
        var message = Text.translatable("message.oritech.pump.starting");
        if (!initialized) {
            if (!toolheadLowered) {
                message = Text.translatable("message.oritech.pump.extending");
            } else if (searchActive) {
                message= Text.translatable("message.oritech.pump.initializing");
            } else {
                message = Text.translatable("message.oritech.pump.no_fluids");
            }
        } else if (isBusy()) {
            message = Text.translatable("message.oritech.pump.busy");
        } else if (!hasEnoughEnergy()) {
            message = Text.translatable("message.oritech.pump.low_energy");
        } else if (pendingLiquidPositions.isEmpty()) {
            message = Text.translatable("message.oritech.pump.pump_finished");
        } else if (tankIsFull()) {
            message = Text.translatable("message.oritech.pump.full");
        }
        
        player.sendMessage(message, true);
    }
    
    private void spawnWorkingParticles() {
        if (world.getTime() % 5 != 0) return;
        
        var targetPos = pos.toCenterPos();
        var targetType = ParticleTypes.BUBBLE_COLUMN_UP;
        if (lastPumpedVariant.equals(Fluids.LAVA)) targetType = ParticleTypes.LAVA;
        if (lastPumpedVariant.equals(FluidContent.STILL_OIL)) targetType = ParticleTypes.FALLING_OBSIDIAN_TEAR;
        
        world.addParticle(targetType, targetPos.getX(), targetPos.getY(), targetPos.getZ(), 0, 0.3, 0);
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
        fluidStorage.insert(FluidStack.create(targetState.getFluid(), FluidStackHooks.bucketAmount()), false);
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
        // if more than N blocks are found, consider the search finished
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
        world.setBlockState(toolheadPosition, BlockContent.PUMP_TRUNK_BLOCK.getDefaultState());
    }
    
    private boolean checkToolheadEnd(BlockPos newPosition) {
        
        var posBelow = newPosition.down();
        var stateBelow = world.getBlockState(posBelow);
        var blockBelow = stateBelow.getBlock();
        
        var isAirOrTrunk = stateBelow.isReplaceable() || blockBelow.equals(BlockContent.PUMP_TRUNK_BLOCK);
        var isStillFluid = stateBelow.getFluidState().isStill();
        
        return isStillFluid || !isAirOrTrunk;
    }
    
    private void startLiquidSearch(BlockPos start) {
        
        var state = world.getFluidState(start);
        if (!state.isStill()) return;
        
        searchInstance = new FloodFillSearch(start, world);
        searchActive = true;
        
        Oritech.LOGGER.debug("starting search at: " + start + " " + state.getFluid() + " " + state.isStill());
    }
    
    private void finishSearch() {
        Oritech.LOGGER.debug("search finished, found: " + searchInstance.foundTargets.size());
        pendingLiquidPositions = searchInstance.foundTargets;
        initialized = true;
        searchInstance = null;
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
    
    private void updateNetwork() {
        var fluid = Registries.FLUID.getId(fluidStorage.getFluid()).toString();
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.PumpWorkSyncPacket(pos, fluid, world.getTime()));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    private boolean isBusy() {
        return world.getTime() - lastPumpTime < 40;
    }
    
    private AnimationController<PumpBlockEntity> getAnimationController() {
        return new AnimationController<>(this, state -> {
            if (isBusy()) return state.setAndContinue(MachineBlockEntity.WORKING);
            return PlayState.STOP;
        });
    }
    
    public void setLastPumpedVariant(Fluid lastPumpedVariant) {
        this.lastPumpedVariant = lastPumpedVariant;
    }
    
    public void setLastPumpTime(long lastPumpTime) {
        this.lastPumpTime = lastPumpTime;
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
