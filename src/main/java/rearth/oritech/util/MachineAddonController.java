package rearth.oritech.util;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.block.blocks.machines.addons.MachineAddonBlock;
import rearth.oritech.block.entity.machines.addons.AddonBlockEntity;

import java.util.*;

public interface MachineAddonController {
    
    List<BlockPos> getConnectedAddons();
    
    List<BlockPos> getOpenSlots();
    
    BlockPos getMachinePos();
    
    World getMachineWorld();
    
    Direction getFacingForAddon();
    
    DynamicEnergyStorage getStorageForAddon();
    
    SimpleInventory getInventoryForAddon();
    
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
    default void initAddons() {
        getConnectedAddons().clear();
        
        var foundAddons = getAllAddons();
        
        gatherAddonStats(foundAddons);
        writeAddons(foundAddons);
        updateEnergyContainer();
        
        for (var addon : foundAddons) {
            getConnectedAddons().add(addon.pos());
        }
    }
    
    // to be called if controller or one of the addons has been broken
    default void resetAddons() {
        
        for (var addon : getConnectedAddons()) {
            var state = Objects.requireNonNull(getMachineWorld()).getBlockState(addon);
            if (state.getBlock() instanceof MachineAddonBlock) {
                getMachineWorld().setBlockState(addon, state.with(MachineAddonBlock.ADDON_USED, false));
                getMachineWorld().updateNeighborsAlways(addon, state.getBlock());
            }
        }
        
        getConnectedAddons().clear();
        updateEnergyContainer();
    }
    
    // addon loading algorithm, called during init
    default List<AddonBlock> getAllAddons() {
        
        var maxIterationCount = (int) getCoreQuality() + 1;
        
        // start with base slots (on machine itself)
        // repeat N times (dependent on core quality?):
        //   go through all slots
        //   check if slot is occupied by MachineAddonBlock, check if block is not used
        //   if valid and extender: add all neighboring positions to search set
        var world = getMachineWorld();
        var pos = getMachinePos();
        assert world != null;
        
        var openSlots = getOpenSlots();
        openSlots.clear();
        
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
                
                // if the candidate is not an addon
                if (!(candidate.getBlock() instanceof MachineAddonBlock addonBlock) || !(candidateEntity instanceof AddonBlockEntity candidateAddonEntity)) {
                    
                    // if the block is not part of the machine itself
                    if (!candidatePos.equals(pos))
                        openSlots.add(candidatePos);
                    continue;
                }
                
                // if the candidate is in use with another controller
                if (candidate.get(MachineAddonBlock.ADDON_USED) && !candidateAddonEntity.getControllerPos().equals(pos)) {
                    openSlots.add(candidatePos);
                    continue;
                }
                
                var entry = new AddonBlock(addonBlock, candidate, candidatePos, candidateAddonEntity);
                result.add(entry);
                
                if (addonBlock.getAddonSettings().extender()) {
                    var neighbors = getNeighbors(candidatePos);
                    for (var neighbor : neighbors) {
                        if (!searchedPositions.contains(neighbor)) toAdd.add(neighbor);
                    }
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
        
        var speed = 1f;
        var efficiency = 1f;
        var energyAmount = 0L;
        var energyInsert = 0L;
        
        for (var addon : addons) {
            var addonSettings = addon.addonBlock().getAddonSettings();
            speed *= addonSettings.speedMultiplier();
            efficiency *= addonSettings.efficiencyMultiplier();
            
            energyAmount += addonSettings.addedCapacity();
            energyInsert += addonSettings.addedInsert();
            
            getAdditionalStatFromAddon(addon);
        }
        
        var baseData = new BaseAddonData(speed, efficiency, energyAmount, energyInsert);
        setBaseAddonData(baseData);
    }
    
    // used to check for specific addons, or do something if a specific addon has been found
    default void getAdditionalStatFromAddon(AddonBlock addonBlock) {
    
    }
    
    // update state of the found addons
    default void writeAddons(List<AddonBlock> addons) {
        
        var world = getMachineWorld();
        var pos = getMachinePos();
        assert world != null;
        
        for (var addon : addons) {
            var newState = addon.state()
                             .with(MachineAddonBlock.ADDON_USED, true);
            // Set controller before setting block state, otherwise the addon will think
            // it's not connected to a machine the first time neighbor blocks are being updated.
            addon.addonEntity().setControllerPos(pos);
            world.setBlockState(addon.pos(), newState);
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
    
    default void writeAddonToNbt(NbtCompound nbt) {
        var data = getBaseAddonData();
        nbt.putFloat("speed", data.speed);
        nbt.putFloat("efficiency", data.efficiency);
        nbt.putLong("energyBonusCapacity", data.energyBonusCapacity);
        nbt.putLong("energyBonusTransfer", data.energyBonusTransfer);
        
        var posList = new NbtList();
        for (var pos : getConnectedAddons()) {
            var posTag = new NbtCompound();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            posList.add(posTag);
        }
        nbt.put("connectedAddons", posList);
    }
    
    default void loadAddonNbtData(NbtCompound nbt) {
        var data = new BaseAddonData(nbt.getFloat("speed"), nbt.getFloat("efficiency"), nbt.getLong("energyBonusCapacity"), nbt.getLong("energyBonusTransfer"));
        setBaseAddonData(data);
        
        var posList = nbt.getList("connectedAddons", NbtElement.COMPOUND_TYPE);
        var connectedAddons = getConnectedAddons();
        
        for (var posTag : posList) {
            var posCompound = (NbtCompound) posTag;
            var x = posCompound.getInt("x");
            var y = posCompound.getInt("y");
            var z = posCompound.getInt("z");
            var pos = new BlockPos(x, y, z);
            connectedAddons.add(pos);
        }
    }
    
    default AddonUiData getUiData() {
        var data = getBaseAddonData();
        return new AddonUiData(getConnectedAddons(), getOpenSlots(), data.efficiency, data.speed, getMachinePos());
    }
    
    private static Set<BlockPos> getNeighbors(BlockPos pos) {
        return Set.of(
          pos.add(-1, 0, 0),
          pos.add(1, 0, 0),
          pos.add(0, 0, -1),
          pos.add(0, 0, 1),
          pos.add(0, -1, 0),
          pos.add(0, 1, 0)
        );
    }
    
    record AddonBlock(MachineAddonBlock addonBlock, BlockState state, BlockPos pos, AddonBlockEntity addonEntity) {
    }
    
    record BaseAddonData(float speed, float efficiency, long energyBonusCapacity, long energyBonusTransfer) {
    }
    
    record AddonUiData(List<BlockPos> positions, List<BlockPos> openSlots, float efficiency, float speed,
                       BlockPos ownPosition) {
    }
    
    BaseAddonData DEFAULT_ADDON_DATA = new BaseAddonData(1, 1, 0, 0);
    
    Endec<AddonUiData> ADDON_UI_ENDEC = StructEndecBuilder.of(
      MinecraftEndecs.BLOCK_POS.listOf().fieldOf("addon_positions", AddonUiData::positions),
      MinecraftEndecs.BLOCK_POS.listOf().fieldOf("open_slots", AddonUiData::openSlots),
      Endec.FLOAT.fieldOf("efficiency", AddonUiData::efficiency),
      Endec.FLOAT.fieldOf("speed", AddonUiData::speed),
      MinecraftEndecs.BLOCK_POS.fieldOf("ownPosition", AddonUiData::ownPosition),
      AddonUiData::new
    );
}
