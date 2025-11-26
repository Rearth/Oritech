package rearth.oritech.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.block.entity.addons.AddonBlockEntity;
import rearth.oritech.block.entity.addons.CombiAddonEntity;

import java.util.*;

public interface MachineAddonController {
    
    // list of where actually connected addons are
    List<BlockPos> getConnectedAddons();
    
    // a list of where addons could be placed
    List<BlockPos> getOpenAddonSlots();
    
    BlockPos getPosForAddon();
    
    Level getWorldForAddon();
    
    Direction getFacingForAddon();
    
    DynamicEnergyStorage getStorageForAddon();
    
    ItemApi.InventoryStorage getInventoryForAddon();
    
    ScreenProvider getScreenProvider();
    
    List<Vec3i> getAddonSlots();
    
    BaseAddonData getBaseAddonData();
    
    void setBaseAddonData(BaseAddonData data);
    
    long getDefaultCapacity();
    
    long getDefaultInsertRate();
    
    default float getCoreQuality() {
        return 1f;
    }
    
    // to initialize everything, should be called when right-clicked
    default void initAddons(BlockPos brokenAddon) {
        
        var foundAddons = getAllAddons(brokenAddon);
        
        gatherAddonStats(foundAddons);
        writeAddons(foundAddons);
        updateEnergyContainer();
        removeOldAddons(foundAddons);
        
        getConnectedAddons().clear();
        updateEnergyContainer();
        
        for (var addon : foundAddons) {
            getConnectedAddons().add(addon.pos());
        }
    }
    
    private void removeOldAddons(List<AddonBlock> foundAddons) {
        // remove/reset all old addons that are not connected anymore
        for (var addon : getConnectedAddons()) {
            if (foundAddons.stream().noneMatch(newAddon -> newAddon.pos().equals(addon))) {
                var state = Objects.requireNonNull(getWorldForAddon()).getBlockState(addon);
                if (state.getBlock() instanceof MachineAddonBlock) {
                    getWorldForAddon().setBlockAndUpdate(addon, state.setValue(MachineAddonBlock.ADDON_USED, false));
                    getWorldForAddon().updateNeighborsAt(addon, state.getBlock());
                }
            }
        }
    }
    
    default void initAddons() {
        initAddons(null);
    }
    
    // to be called if controller or one of the addons has been broken
    default void resetAddons() {
        
        for (var addon : getConnectedAddons()) {
            var state = Objects.requireNonNull(getWorldForAddon()).getBlockState(addon);
            if (state.getBlock() instanceof MachineAddonBlock) {
                getWorldForAddon().setBlockAndUpdate(addon, state.setValue(MachineAddonBlock.ADDON_USED, false));
                getWorldForAddon().updateNeighborsAt(addon, state.getBlock());
            }
        }
        
        getConnectedAddons().clear();
        updateEnergyContainer();
    }
    
    // addon loading algorithm, called during init
    default List<AddonBlock> getAllAddons(BlockPos brokenAddon) {
        
        var useLayered = Oritech.CONFIG.layeredExtenders();
        
        var maxIterationCount = (int) getCoreQuality() + 1;
        
        // start with base slots (on machine itself)
        // repeat N times (dependent on core quality?):
        //   go through all slots
        //   check if slot is occupied by MachineAddonBlock, check if block is not used
        //   if valid and extender: add all neighboring positions to search set
        var world = getWorldForAddon();
        var pos = getPosForAddon();
        assert world != null;
        
        var openSlots = getOpenAddonSlots();
        openSlots.clear();
        
        var foundExtenders = 0;
        
        var baseSlots = getAddonSlots();    // available addon slots on machine itself (includes multiblocks)
        var searchedPositions = new HashSet<BlockPos>(baseSlots.size()); // all positions ever checked, to avoid adding duplicates
        var queuedPositions = new ArrayList<BlockPos>(baseSlots.size());
        var result = new ArrayList<AddonBlock>(baseSlots.size());   // results, unused addon blocks
        
        // fill initial spots
        for (var initialSpot : baseSlots) {
            queuedPositions.add((BlockPos) Geometry.offsetToWorldPosition(getFacingForAddon(), initialSpot, pos));
        }
        
        // to allow loops where we modify the content
        var toAdd = new HashSet<BlockPos>();
        var toRemove = new HashSet<BlockPos>();
        
        //everything done in world space
        for (int i = 0; i < maxIterationCount; i++) {
            if (queuedPositions.isEmpty()) break;
            
            for (var candidatePos : queuedPositions) {
                if (searchedPositions.contains(candidatePos)) continue;
                searchedPositions.add(candidatePos);
                toRemove.add(candidatePos);
                
                var candidate = world.getBlockState(candidatePos);
                var candidateEntity = world.getBlockEntity(candidatePos);
                
                // if the candidate is the broken addon, skip it
                if (candidatePos.equals(brokenAddon)) {
                    openSlots.add(candidatePos);
                    continue;
                }
                
                // if the candidate is not an addon
                if (!(candidate.getBlock() instanceof MachineAddonBlock addonBlock) || !(candidateEntity instanceof AddonBlockEntity candidateAddonEntity)) {
                    
                    // if the block is not part of the machine itself
                    if (!candidatePos.equals(pos))
                        openSlots.add(candidatePos);
                    continue;
                }
                
                // if the candidate is in use with another controller
                if (candidate.getValue(MachineAddonBlock.ADDON_USED) && !candidateAddonEntity.getControllerPos().equals(pos)) {
                    openSlots.add(candidatePos);
                    continue;
                }
                
                // if non-layered mode, check if we have too many extenders already
                if (addonBlock.getAddonSettings().extender() && !useLayered) {
                    if (foundExtenders < (maxIterationCount - 1)) {
                        foundExtenders++;
                    } else {
                        continue;
                    }
                }
                
                var entry = new AddonBlock(addonBlock, candidate, candidatePos, candidateAddonEntity);
                result.add(entry);
                
                if (addonBlock.getAddonSettings().extender()) {
                    var neighbors = getNeighbors(candidatePos);
                    for (var neighbor : neighbors) {
                        if (!searchedPositions.contains(neighbor)) toAdd.add(neighbor);
                    }
                }
                
                if (entry.addonEntity() instanceof CombiAddonEntity) {
                    toAdd.clear();
                    maxIterationCount = 0;
                    break;
                }
            }
            
            queuedPositions.addAll(toAdd);
            queuedPositions.removeAll(toRemove);
            toAdd.clear();
            toRemove.clear();
        }
        
        return result;
        
    }
    
    // can be overridden to allow custom addon loading (e.g. custom stat, or check for specific addon existence)
    default void gatherAddonStats(List<AddonBlock> addons) {
        
        if (addons.size() == 1 && addons.getFirst().addonEntity() instanceof CombiAddonEntity combiAddonEntity) {
            getAdditionalStatFromAddon(addons.getFirst());
            setBaseAddonData(combiAddonEntity.getBaseData());
            return;
        }
        
        var speed = 1f;
        var efficiency = 1f;
        var energyAmount = 0L;
        var energyInsert = 0L;
        var extraChambers = 0;
        var extraBurstTicks = 0;
        
        for (var addon : addons) {
            var addonSettings = addon.addonBlock().getAddonSettings();
            
            if (Oritech.CONFIG.additiveAddons()) {
                speed += 1 - addonSettings.speedMultiplier();
                efficiency += 1 - addonSettings.efficiencyMultiplier();
            } else {
                speed *= addonSettings.speedMultiplier();
                efficiency *= addonSettings.efficiencyMultiplier();
            }
            
            energyAmount += addonSettings.addedCapacity();
            energyInsert += addonSettings.addedInsert();
            extraChambers += addonSettings.chamberCount();
            extraBurstTicks += addonSettings.burstTicks();
            
            getAdditionalStatFromAddon(addon);
        }
        
        if (Oritech.CONFIG.additiveAddons()) {
            // convert addon numbers to base (e.g. +2 (+200%) speed bonus is actually a total multiplier of 0.5) (+2 would be a speed of 3, because we start at 1)
            // efficiency change of -100% would result in efficiency multiplier of 2. -400% would be 5
            // efficiency and speed numbers < 1 here make things better.
            
            speed = 1f / speed;
            
            var efficiencyChange = efficiency - 1;
            efficiency = 1f / efficiency;
            if (efficiencyChange < 0) {
                efficiency = 1 + Math.abs(efficiencyChange);   // yes this order looks stupid, but it's easier to understand like this for me
            }
        }
        
        var baseData = new BaseAddonData(speed, efficiency, energyAmount, energyInsert, extraChambers, extraBurstTicks);
        setBaseAddonData(baseData);
    }
    
    // used to check for specific addons, or do something if a specific addon has been found
    default void getAdditionalStatFromAddon(AddonBlock addonBlock) {
    
    }
    
    // update state of the found addons
    default void writeAddons(List<AddonBlock> addons) {
        
        var world = getWorldForAddon();
        var pos = getPosForAddon();
        assert world != null;
        
        for (var addon : addons) {
            var newState = addon.state()
                             .setValue(MachineAddonBlock.ADDON_USED, true);
            // Set controller before setting block state, otherwise the addon will think
            // it's not connected to a machine the first time neighbor blocks are being updated.
            addon.addonEntity().setControllerPos(pos);
            world.setBlockAndUpdate(addon.pos(), newState);
        }
    }
    
    // part of init/break, updates the energy container size
    default void updateEnergyContainer() {
        var energyStorage = getStorageForAddon();
        var addonData = getBaseAddonData();
        energyStorage.capacity = getDefaultCapacity() + addonData.energyBonusCapacity;
        energyStorage.maxInsert = getDefaultInsertRate() + addonData.energyBonusTransfer;
        energyStorage.amount = Math.min(energyStorage.amount, energyStorage.capacity);
    }
    
    default void writeAddonToNbt(CompoundTag nbt) {
        var data = getBaseAddonData();
        nbt.putFloat("speed", data.speed);
        nbt.putFloat("efficiency", data.efficiency);
        nbt.putLong("energyBonusCapacity", data.energyBonusCapacity);
        nbt.putLong("energyBonusTransfer", data.energyBonusTransfer);
        nbt.putInt("extraChambers", data.extraChambers);
        nbt.putInt("maxBurst", data.maxBurstTicks);
        
        var posList = new ListTag();
        for (var pos : getConnectedAddons()) {
            var posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            posList.add(posTag);
        }
        nbt.put("connectedAddons", posList);
    }
    
    default void loadAddonNbtData(CompoundTag nbt) {
        var data = new BaseAddonData(
          nbt.getFloat("speed"),
          nbt.getFloat("efficiency"),
          nbt.getLong("energyBonusCapacity"),
          nbt.getLong("energyBonusTransfer"),
          nbt.getInt("extraChambers"),
          nbt.getInt("maxBurst")
        );
        setBaseAddonData(data);
        
        var posList = nbt.getList("connectedAddons", Tag.TAG_COMPOUND);
        var connectedAddons = getConnectedAddons();
        
        for (var posTag : posList) {
            var posCompound = (CompoundTag) posTag;
            var x = posCompound.getInt("x");
            var y = posCompound.getInt("y");
            var z = posCompound.getInt("z");
            var pos = new BlockPos(x, y, z);
            connectedAddons.add(pos);
        }
    }
    
    private static Set<BlockPos> getNeighbors(BlockPos pos) {
        return Set.of(
          pos.offset(-1, 0, 0),
          pos.offset(1, 0, 0),
          pos.offset(0, 0, -1),
          pos.offset(0, 0, 1),
          pos.offset(0, -1, 0),
          pos.offset(0, 1, 0)
        );
    }
    
    record AddonBlock(MachineAddonBlock addonBlock, BlockState state, BlockPos pos, AddonBlockEntity addonEntity) {
    }
    
    record BaseAddonData(float speed, float efficiency, long energyBonusCapacity, long energyBonusTransfer,
                         int extraChambers, int maxBurstTicks) {
        
        public static final Codec<BaseAddonData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.FLOAT.fieldOf("speed").forGetter(BaseAddonData::speed),
          Codec.FLOAT.fieldOf("efficiency").forGetter(BaseAddonData::efficiency),
          Codec.LONG.fieldOf("energy_bonus_capacity").forGetter(BaseAddonData::energyBonusCapacity),
          Codec.LONG.fieldOf("energy_bonus_transfer").forGetter(BaseAddonData::energyBonusTransfer),
          Codec.INT.fieldOf("extra_chambers").forGetter(BaseAddonData::extraChambers),
          Codec.INT.fieldOf("max_burst_ticks").forGetter(BaseAddonData::maxBurstTicks)
        ).apply(instance, BaseAddonData::new));
        
        public static final BaseAddonData DEFAULT_ADDON_DATA = new BaseAddonData(1, 1, 0, 0, 0, 0);
    }
    
}
