package rearth.oritech.block.entity.reactor;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.EnergyApi.EnergyStorage;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.blocks.reactor.*;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.ReactorScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.SoundContent;

import rearth.oritech.util.Geometry;

import java.util.*;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class ReactorControllerBlockEntity extends NetworkedBlockEntity implements EnergyApi.BlockProvider, ExtendedMenuProvider {
    
    public static final int MAX_SIZE = Oritech.CONFIG.maxSize();
    public static final int RF_PER_PULSE = Oritech.CONFIG.rfPerPulse();
    public static final int ABSORBER_RATE = Oritech.CONFIG.absorberRate();
    public static final int VENT_BASE_RATE = Oritech.CONFIG.ventBaseRate();
    public static final int VENT_RELATIVE_RATE = Oritech.CONFIG.ventRelativeRate();
    public static final int MAX_HEAT = Oritech.CONFIG.maxHeat();
    public static final int MAX_UNSTABLE_TICKS = Oritech.CONFIG.maxUnstableTicks();
    
    private final HashMap<Vector2i, BaseReactorBlock> activeComponents = new HashMap<>();   // 2d local position on the first layer containing the reactor blocks
    private final HashMap<Vector2i, ReactorFuelPortEntity> fuelPorts = new HashMap<>();     // same grid, but contains a reference to the port at the ceiling
    private final HashMap<Vector2i, ReactorAbsorberPortEntity> absorberPorts = new HashMap<>(); // same
    private final HashMap<Vector2i, Integer> componentHeats = new HashMap<>();              // same grid, contains the current heat of the component
    private final HashSet<Tuple<BlockPos, Direction>> energyPorts = new HashSet<>();   // list of all energy port outputs (e.g. the targets to output to)
    private final HashSet<BlockPos> redstonePorts = new HashSet<>();   // list of all redstone ports
    
    @SyncField(SyncType.GUI_TICK)
    public final HashMap<Vector2i, ComponentStatistics> componentStats = new HashMap<>(); // mainly for client displays, same grid
    
    @SyncField(SyncType.GUI_TICK)
    public SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(0, Oritech.CONFIG.reactorMaxEnergyStored(), Oritech.CONFIG.reactorMaxEnergyStored(), this::setChanged);
    
    public boolean active = false;
    
    @SyncField(SyncType.GUI_OPEN)
    public BlockPos areaMin;
    @SyncField(SyncType.GUI_OPEN)
    public BlockPos areaMax;
    
    private int reactorStackHeight;
    private boolean disabledViaRedstone = false;
    private int unstableTicks = 0;
    public long disabledUntil = 0;
    
    private boolean doAutoInit = false; // used to auto-init when save is being loaded
    
    public ReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REACTOR_CONTROLLER_BLOCK_ENTITY, pos, state);
    }
    
    // heat is only used for reactor rods and heat pipes
    // rods generate heat. Multi-cores and reflectors (expensive) change this
    // heat pipes move heat to themselves
    // vents remove heat from the hottest neighbor component
    // absorbers remove fixed heat amount from all neighboring blocks
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        if (!active && doAutoInit) {
            doAutoInit = false;
            init(null);
        }
        
        if (!active || activeComponents.isEmpty()) return;
        
        var activeRods = 0;
        var hottestHeat = 0;
        
        for (var entry : activeComponents.entrySet()) {
            var localPos = entry.getKey();
            var component = entry.getValue();
            var componentHeat = componentHeats.get(localPos);
            
            if (component instanceof ReactorRodBlock rodBlock) {
                
                var ownRodCount = rodBlock.getRodCount();
                var receivedPulses = rodBlock.getInternalPulseCount();
                
                var portEntity = fuelPorts.get(localPos);
                if (portEntity == null || portEntity.isRemoved()) {
                    continue;
                }
                
                var hasFuel = portEntity.tryConsumeFuel(ownRodCount * reactorStackHeight, isDisabled() || disabledViaRedstone);
                var heatCreated = 0;
                
                setRodBlockState(localPos, hasFuel);
                
                if (hasFuel) {
                    // check how many pulses are received from neighbors / reflectors
                    for (var neighborPos : getNeighborsInBounds(localPos, activeComponents.keySet())) {
                        
                        var neighbor = activeComponents.get(neighborPos);
                        if (neighbor instanceof ReactorRodBlock neighborRod) {
                            receivedPulses += neighborRod.getRodCount();
                        } else if (neighbor instanceof ReactorReflectorBlock reflectorBlock) {
                            receivedPulses += rodBlock.getRodCount();
                        }
                    }
                    
                    if (!isDisabled()) {
                        activeRods++;
                        energyStorage.insertIgnoringLimit(RF_PER_PULSE * receivedPulses * reactorStackHeight, false);
                    }
                    
                    // generate heat per pulse
                    heatCreated = (receivedPulses / 2 * receivedPulses + 4);
                    componentHeat += heatCreated;
                    
                    if (componentHeat > MAX_HEAT * 0.85) {
                        playMeltdownAnimation(portEntity.getBlockPos());
                    }
                    
                } else {
                    receivedPulses = 0;
                }
                
                componentStats.put(localPos, new ComponentStatistics((short) receivedPulses, componentHeat, (short) heatCreated));
                
            } else if (component instanceof ReactorHeatPipeBlock heatPipeBlock) {
                
                var sumGainedHeat = 0;
                
                // take heat in from neighbors
                for (var neighbor : getNeighborsInBounds(localPos, activeComponents.keySet())) {
                    var neighborHeat = componentHeats.get(neighbor);
                    if (neighborHeat <= componentHeat) continue;
                    var diff = neighborHeat - componentHeat;
                    var gainedHeat = Math.min(diff / 4 + 10, diff);
                    neighborHeat -= gainedHeat;
                    componentHeats.put(neighbor, neighborHeat);
                    componentHeat += gainedHeat;
                    sumGainedHeat += gainedHeat;
                }
                
                componentStats.put(localPos, new ComponentStatistics((short) 0, componentHeat, (short) sumGainedHeat));
                
            } else if (component instanceof ReactorAbsorberBlock absorberBlock) {
                
                var sumRemovedHeat = 0;
                var portEntity = absorberPorts.get(localPos);
                if (portEntity == null || portEntity.isRemoved()) {
                    continue;
                }
                var fuelAvailable = portEntity.getAvailableFuel();
                
                if (fuelAvailable >= reactorStackHeight) {
                    // take heat in from neighbors and remove it
                    for (var neighbor : getNeighborsInBounds(localPos, activeComponents.keySet())) {
                        var neighborHeat = componentHeats.get(neighbor);
                        if (neighborHeat <= 0) continue;
                        neighborHeat -= ABSORBER_RATE;
                        sumRemovedHeat += ABSORBER_RATE;
                        componentHeats.put(neighbor, neighborHeat);
                    }
                } else if (fuelAvailable > 0) {
                    // remove last small unusable part
                    portEntity.consumeFuel(fuelAvailable);
                }
                
                if (sumRemovedHeat > 0) {
                    portEntity.consumeFuel(reactorStackHeight);
                }
                
                componentStats.put(localPos, new ComponentStatistics((short) 0, 0, (short) sumRemovedHeat));
            } else if (component instanceof ReactorHeatVentBlock ventBlock) {
                
                // remove heat from hottest neighbor
                
                var hottestPos = localPos;
                var max = 0;
                for (var neighbor : getNeighborsInBounds(localPos, activeComponents.keySet())) {
                    var neighborHeat = componentHeats.get(neighbor);
                    if (neighborHeat <= max) continue;
                    hottestPos = neighbor;
                    max = neighborHeat;
                }
                
                var removed = 0;
                if (max != 0) {
                    var neighborHeat = max;
                    removed = Math.min(neighborHeat / VENT_RELATIVE_RATE + VENT_BASE_RATE, neighborHeat);
                    neighborHeat -= removed;
                    componentHeats.put(hottestPos, neighborHeat);
                }
                
                componentStats.put(localPos, new ComponentStatistics((short) 0, 0, (short) removed));
                
            }
            
            componentHeats.put(localPos, componentHeat);
            
            if (componentHeat > hottestHeat)
                hottestHeat = componentHeat;
            
        }
        
        outputEnergy();
        updateRedstonePorts(hottestHeat, activeRods);
        
        if (activeRods > 0)
            playAmbientSound();
        
        if (activeRods > 0 && hottestHeat > MAX_HEAT * 0.8f) {
            playWarningSound();
        }
        
        if (hottestHeat > MAX_HEAT && activeRods > 0) {
            unstableTicks++;
            if (unstableTicks > MAX_UNSTABLE_TICKS)
                doReactorExplosion(activeRods * reactorStackHeight);
        } else {
            unstableTicks = 0;
        }
        
    }
    
    @Override
    public void preNetworkUpdate(SyncType type) {
        super.preNetworkUpdate(type);
        
        if (type != SyncType.GUI_TICK) return;
        for (var port : fuelPorts.values()) port.updateNetwork();
        for (var port : absorberPorts.values()) port.updateNetwork();
    }
    
    private boolean isDisabled() {
        return level.getGameTime() < disabledUntil;
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        
        nbt.putLong("energy_stored", energyStorage.getAmount());
        nbt.putBoolean("was_active", active);
        nbt.putBoolean("redstone_disabled", disabledViaRedstone);
        
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        
        energyStorage.setAmount(nbt.getLong("energy_stored"));
        doAutoInit = nbt.getBoolean("was_active");
        disabledViaRedstone = nbt.getBoolean("redstone_disabled");
    }
    
    private void playMeltdownAnimation(BlockPos port) {
        ParticleContent.MELTDOWN_IMMINENT.spawn(level, port.getCenter().add(0, 0.3, 0), 5);
    }
    
    private void playAmbientSound() {
        var soundDuration = 250;
        
        if (level.getGameTime() % soundDuration == 0)
            level.playSound(null, worldPosition, SoundContent.REACTOR, SoundSource.BLOCKS, 0.7f, 0.8f);
    }
    
    
    private void playWarningSound() {
        var soundDuration = 50;
        
        if (level.getGameTime() % soundDuration == 0)
            level.playSound(null, worldPosition, SoundContent.REACTOR_WARNING, SoundSource.BLOCKS, 4f, 0.8f);
    }
    
    // strength is the amount of total active rods (e.g. activeRods * stackHeight)
    private void doReactorExplosion(int strength) {
        
        if (Oritech.CONFIG.safeMode()) {
            disableReactor();
            return;
        }
        
        var spawnedBlock = BlockContent.REACTOR_EXPLOSION_SMALL;
        if (strength > 8 && strength <= 25) {
            spawnedBlock = BlockContent.REACTOR_EXPLOSION_MEDIUM;
        } else if (strength > 25) {
            spawnedBlock = BlockContent.REACTOR_EXPLOSION_LARGE;
        }
        
        level.setBlockAndUpdate(worldPosition, spawnedBlock.defaultBlockState());
    }
    
    private void disableReactor() {
        this.disabledUntil = level.getGameTime() + Oritech.CONFIG.safeModeCooldown();
    }
    
    public void init(@Nullable Player player) {
        
        active = false;
        
        // find low and high corners of reactor
        var cornerA = worldPosition;
        cornerA = expandWall(cornerA, new Vec3i(0, -1, 0), true);   // first go down through other wall blocks
        cornerA = expandWall(cornerA, new Vec3i(0, 0, -1));
        cornerA = expandWall(cornerA, new Vec3i(-1, 0, 0));
        cornerA = expandWall(cornerA, new Vec3i(0, 0, -1)); // expand z again to support all rotations
        
        var cornerB = cornerA;
        cornerB = expandWall(cornerB, new Vec3i(0, 1, 0));
        cornerB = expandWall(cornerB, new Vec3i(0, 0, 1));
        cornerB = expandWall(cornerB, new Vec3i(1, 0, 0));
        
        if (cornerA == worldPosition || cornerB == worldPosition || cornerA == cornerB || onSameAxis(cornerA, cornerB)) {
            if (player != null)
                player.sendSystemMessage(Component.translatable("message.oritech.reactor_edge_invalid"));
            return;
        }
        
        // verify and load all blocks in reactor area
        var finalCornerA = cornerA;
        var finalCornerB = cornerB;
        
        // these get loaded in the next step
        energyPorts.clear();
        redstonePorts.clear();
        
        // verify edges
        var wallsValid = BlockPos.betweenClosedStream(cornerA, cornerB).allMatch(pos -> {
            if (isAtEdgeOfBox(pos, finalCornerA, finalCornerB)) {
                var block = level.getBlockState(pos).getBlock();
                return block instanceof ReactorWallBlock;
            } else if (isOnWall(pos, finalCornerA, finalCornerB)) {
                var state = level.getBlockState(pos);
                var block = state.getBlock();
                
                // load wall energy ports
                if (block instanceof ReactorEnergyPortBlock) {
                    var facing = state.getValue(BlockStateProperties.FACING);
                    var blockInFront = pos.offset(Geometry.getForward(facing));
                    energyPorts.add(new Tuple<>(blockInFront, Direction.fromDelta(Geometry.getBackward(facing).getX(), Geometry.getBackward(facing).getY(), Geometry.getBackward(facing).getZ())));
                } else if (block instanceof ReactorRedstonePortBlock) {
                    redstonePorts.add(pos.immutable());
                }
                
                return !(block instanceof BaseReactorBlock reactorBlock) || reactorBlock.validForWalls();
            }
            
            return true;
        });
        
        if (!wallsValid) {
            if (player != null)
                player.sendSystemMessage(Component.translatable("message.oritech.reactor_wall_invalid"));
            return;
        }
        
        // verify interior is identical in all layers
        var interiorHeight = cornerB.getY() - cornerA.getY() - 1;
        var cornerAFlat = cornerA.offset(1, 1, 1);
        var cornerBFlat = new BlockPos(cornerB.getX() - 1, cornerA.getY() + 1, cornerB.getZ() - 1);
        
        // these get loaded in the next step
        fuelPorts.clear();
        absorberPorts.clear();
        reactorStackHeight = interiorHeight;
        
        var interiorStackedRight = BlockPos.betweenClosedStream(cornerAFlat, cornerBFlat).allMatch(pos -> {
            
            var offset = pos.subtract(cornerAFlat);
            var localPos = new Vector2i(offset.getX(), offset.getZ());
            
            var block = level.getBlockState(pos).getBlock();
            if (!(block instanceof BaseReactorBlock reactorBlock)) return true;
            
            for (int i = 1; i < interiorHeight; i++) {
                var candidatePos = pos.offset(0, i, 0);
                var candidate = level.getBlockState(candidatePos);
                if (!candidate.getBlock().equals(block))
                    return false;
            }
            
            var requiredCeiling = reactorBlock.requiredStackCeiling();
            if (requiredCeiling != Blocks.AIR) {
                var ceilingPos = pos.offset(0, interiorHeight, 0);
                var ceilingBlock = level.getBlockState(ceilingPos).getBlock();
                if (!requiredCeiling.equals(ceilingBlock)) return false;
                
                if (block instanceof ReactorRodBlock) {
                    fuelPorts.put(localPos, (ReactorFuelPortEntity) level.getBlockEntity(ceilingPos));
                } else if (block instanceof ReactorAbsorberBlock) {
                    absorberPorts.put(localPos, (ReactorAbsorberPortEntity) level.getBlockEntity(ceilingPos));
                }
                
            }
            activeComponents.put(localPos, reactorBlock);
            componentHeats.putIfAbsent(localPos, 0);
            
            return true;
        });
        
        if (!interiorStackedRight) {
            if (player != null)
                player.sendSystemMessage(Component.translatable("message.oritech.reactor_interior_issues"));
            return;
        }
        
        areaMin = finalCornerA;
        areaMax = finalCornerB;
        active = true;
        
    }
    
    private void setRodBlockState(Vector2i localPos, boolean on) {
        if (level.getGameTime() % 10 != 0) return;
        var stackTop = fuelPorts.get(localPos).getBlockPos();
        
        for (int i = 1; i <= reactorStackHeight; i++) {
            var candidatePos = stackTop.below(i);
            var candidateState = level.getBlockState(candidatePos);
            if (!(candidateState.getBlock() instanceof ReactorRodBlock)) continue;
            var oldLit = candidateState.getValue(BlockStateProperties.LIT);
            if (oldLit != on) {
                // update only when changed
                level.setBlock(candidatePos, candidateState.setValue(BlockStateProperties.LIT, on), Block.UPDATE_CLIENTS, 0);
            }
        }
    }
    
    private static Set<Vector2i> getNeighborsInBounds(Vector2i pos, Set<Vector2i> keys) {
        
        var res = new HashSet<Vector2i>(4);
        
        var a = new Vector2i(pos).add(-1, 0);
        if (keys.contains(a)) res.add(a);
        var b = new Vector2i(pos).add(0, 1);
        if (keys.contains(b)) res.add(b);
        var c = new Vector2i(pos).add(1, 0);
        if (keys.contains(c)) res.add(c);
        var d = new Vector2i(pos).add(0, -1);
        if (keys.contains(d)) res.add(d);
        
        return res;
    }
    
    private static boolean onSameAxis(BlockPos A, BlockPos B) {
        return A.getX() == B.getX() || A.getY() == B.getY() || A.getZ() == B.getZ();
    }
    
    private static boolean isOnWall(BlockPos pos, BlockPos min, BlockPos max) {
        return onSameAxis(pos, min) || onSameAxis(pos, max);
    }
    
    private static boolean isAtEdgeOfBox(BlockPos pos, BlockPos min, BlockPos max) {
        int planesAligned = 0;
        
        if (pos.getX() == min.getX() || pos.getX() == max.getX()) planesAligned++;
        if (pos.getY() == min.getY() || pos.getY() == max.getY()) planesAligned++;
        if (pos.getZ() == min.getZ() || pos.getZ() == max.getZ()) planesAligned++;
        
        return planesAligned >= 2;
    }
    
    private BlockPos expandWall(BlockPos from, Vec3i direction) {
        return expandWall(from, direction, false);
    }
    
    private BlockPos expandWall(BlockPos from, Vec3i direction, boolean allReactorBlocks) {
        
        var result = from;
        for (int i = 1; i < MAX_SIZE; i++) {
            var candidate = from.offset(direction.multiply(i));
            var candidateBlock = level.getBlockState(candidate).getBlock();
            
            if (!allReactorBlocks && !(candidateBlock instanceof ReactorWallBlock)) return result;
            if (allReactorBlocks && !(candidateBlock instanceof BaseReactorBlock)) return result;
            
            result = candidate;
        }
        
        return result;
        
    }
    
    private void updateRedstonePorts(int hottestTemp, int filledRods) {
        
        disabledViaRedstone = false;
        
        for (var pos : redstonePorts) {
            var state = level.getBlockState(pos);
            if (!state.getBlock().equals(BlockContent.REACTOR_REDSTONE_PORT)) continue;
            
            var resOutput = 0;
            
            var mode = state.getValue(ReactorRedstonePortBlock.PORT_MODE);
            if (mode == 0 && hottestTemp > 0) {    // temp of hottest component
                resOutput = (int) ((hottestTemp / (float) MAX_HEAT) * 15);
                resOutput = Math.max(resOutput, 1);  // ensure at least level 1 if any component has heat
            } else if (mode == 1) { // amount of rods with fuel
                resOutput = Math.min(filledRods, 15);
            } else if (mode == 2 && energyStorage.getAmount() > 0) { // amount of energy stored
                var fillPercentage = energyStorage.getAmount() / (float) energyStorage.getCapacity();
                resOutput = (int) (1 + fillPercentage * 14);
            }
            
            resOutput = Math.min(resOutput, 15);
            
            var lastLevel = state.getValue(BlockStateProperties.POWER);
            if (lastLevel != resOutput) {
                level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.POWER, resOutput));
                level.blockEntityChanged(pos);
            }
            
            if (level.hasNeighborSignal(pos)) {
                disabledViaRedstone = true;
            }
            
        }
        
    }
    
    private void outputEnergy() {
        
        var totalMoved = 0;
        var maxRatePerSlot = Oritech.CONFIG.reactorMaxEnergyOutput();
        
        var randomOrderedList = new ArrayList<>(energyPorts);
        Collections.shuffle(randomOrderedList);
        
        for (var candidateData : randomOrderedList) {
            var candidate = EnergyApi.BLOCK.find(level, candidateData.getA(), candidateData.getB());
            if (candidate == null) continue;
            var moved = EnergyApi.transfer(energyStorage, candidate, maxRatePerSlot, false);
            
            if (moved > 0)
                candidate.update();
            
            totalMoved += moved;
        }
        
        if (totalMoved > 0)
            energyStorage.update();
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new ReactorScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
    }
    
    public record ComponentStatistics(short receivedPulses, int storedHeat, short heatChanged) {
        public static final ComponentStatistics EMPTY = new ComponentStatistics((short) 0, -1, (short) 0);
    }
}
