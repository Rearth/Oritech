package rearth.oritech.block.base.entity;

import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.blocks.machines.addons.EnergyAddonBlock;
import rearth.oritech.block.blocks.machines.addons.MachineAddonBlock;
import rearth.oritech.block.entity.machines.addons.AddonBlockEntity;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.util.Geometry;

import java.util.*;

public abstract class UpgradableMachineBlockEntity extends MachineBlockEntity {
    
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    private final List<BlockPos> openSlots = new ArrayList<>();
    
    private float combinedSpeed = 1;
    private float combinedEfficiency = 1;
    private long combinedEnergyStorage = 0;
    private long combinedEnergyInsert = 0;
    
    public UpgradableMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        
        var posList = new NbtList();
        for (var pos : connectedAddons) {
            var posTag = new NbtCompound();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            posList.add(posTag);
        }
        nbt.put("connectedAddons", posList);
        
        nbt.putFloat("combined_speed", combinedSpeed);
        nbt.putFloat("combined_efficiency", combinedEfficiency);
        nbt.putLong("combined_capacity", combinedEnergyStorage);
        nbt.putLong("combined_insert", combinedEnergyInsert);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        
        var posList = nbt.getList("connectedAddons", NbtElement.COMPOUND_TYPE);
        
        for (var posTag : posList) {
            var posCompound = (NbtCompound) posTag;
            var x = posCompound.getInt("x");
            var y = posCompound.getInt("y");
            var z = posCompound.getInt("z");
            var pos = new BlockPos(x, y, z);
            connectedAddons.add(pos);
        }
        
        this.combinedSpeed = nbt.getFloat("combined_speed");
        this.combinedEfficiency = nbt.getFloat("combined_efficiency");
        this.combinedEnergyStorage = nbt.getLong("combined_capacity");
        this.combinedEnergyInsert = nbt.getLong("combined_insert");
        
        updateEnergyContainer();
    }
    
    private void updateEnergyContainer() {
        energyStorage.capacity = 5000 + combinedEnergyStorage;
        energyStorage.maxInsert = 100 + combinedEnergyInsert;
        
        energyStorage.amount = Math.min(energyStorage.amount, energyStorage.capacity);
    }
    
    public void initAddons() {
        connectedAddons.clear();
        
        var foundAddons = getAllAddons();
        
        gatherAddonStats(foundAddons);
        writeAddons(foundAddons);
        updateEnergyContainer();
        
        for (var addon : foundAddons) {
            connectedAddons.add(addon.pos);
        }
    }
    
    public void resetAddons() {
        
        for (var addon : connectedAddons) {
            var state = Objects.requireNonNull(world).getBlockState(addon);
            if (state.getBlock() instanceof MachineAddonBlock) {
                world.setBlockState(addon, state.with(MachineAddonBlock.ADDON_USED, false));
            }
        }
        
        connectedAddons.clear();
        updateEnergyContainer();
    }
    
    private List<AddonBlock> getAllAddons() {
        
        // make this number depend on machine core quality
        var maxIterationCount = (int) getCoreQuality();
        
        // start with base slots (on machine itself)
        // repeat N times (dependent on core quality?):
        //   go through all slots
        //   check if slot is occupied by MachineAddonBlock, check if block is not used
        //   if valid and extender: add all neighboring positions to search set
        assert world != null;
        
        openSlots.clear();
        
        var baseSlots = getAddonSlots();    // available addon slots on machine itself (includes multiblocks)
        var searchedPositions = new HashSet<BlockPos>(baseSlots.size()); // all positions ever checked, to avoid adding duplicates
        var queuedPositions = new ArrayList<BlockPos>(baseSlots.size());
        var result = new ArrayList<AddonBlock>(baseSlots.size());   // results, unused addon blocks
        
        // fill initial spots
        for (var initialSpot : baseSlots) {
            queuedPositions.add((BlockPos) Geometry.offsetToWorldPosition(getFacing(), initialSpot, pos));
        }
        
        // to allow loops where we modify the content basically
        var toAdd = new HashSet<BlockPos>();
        var toRemove = new HashSet<BlockPos>();
        
        //everything done in world space
        for (int i = 0; i < maxIterationCount; i++) {
            if (queuedPositions.isEmpty()) break;
            
            for(var candidatePos : queuedPositions) {
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
                
                if (addonBlock.isExtender()) {
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
    
    private void gatherAddonStats(List<AddonBlock> addons) {
        
        var speed = 1f;
        var efficiency = 1f;
        var energyAmount = 0L;
        var energyInsert = 0L;
        
        for (var addon : addons) {
            speed *= addon.addonBlock.getSpeedMultiplier();
            efficiency *= addon.addonBlock.getEfficiencyMultiplier();
            
            if (addon.addonBlock instanceof EnergyAddonBlock capacitorBlock) {
                energyAmount += capacitorBlock.getAddedCapacity();
                energyInsert += capacitorBlock.getAddedInsert();
            }
        }
        
        this.combinedSpeed = speed;
        this.combinedEfficiency = efficiency;
        this.combinedEnergyStorage = energyAmount;
        this.combinedEnergyInsert = energyInsert;
    }
    
    private void writeAddons(List<AddonBlock> addons) {
        assert world != null;
        
        for (var addon : addons) {
            var newState = addon.state
                             .with(MachineAddonBlock.ADDON_USED, true);
            world.setBlockState(addon.pos, newState);
            addon.addonEntity.setControllerPos(pos);
        }
        
    }
    
    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        super.writeScreenOpeningData(player, buf);
        buf.write(ADDON_UI_ENDEC, getUiData());
        buf.writeFloat(getCoreQuality());
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this, getUiData(), getCoreQuality());
    }
    
    private AddonUiData getUiData() {
        return new AddonUiData(connectedAddons, openSlots, combinedEfficiency, combinedSpeed, pos);
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
    
    @Override
    public float getSpeedMultiplier() {
        return combinedSpeed;
    }
    
    @Override
    public float getEfficiencyMultiplier() { return combinedEfficiency; }
    
    public void setCombinedSpeed(float combinedSpeed) {
        this.combinedSpeed = combinedSpeed;
    }
    
    public void setCombinedEfficiency(float combinedEfficiency) {
        this.combinedEfficiency = combinedEfficiency;
    }
    
    // 1 = basic, higher=better, always rounded down
    public float getCoreQuality() {
        return 1f;
    }
    
    public abstract List<Vec3i> getAddonSlots();
    
    private record AddonBlock(MachineAddonBlock addonBlock, BlockState state, BlockPos pos, AddonBlockEntity addonEntity) {}
    
    public record AddonUiData(List<BlockPos> positions, List<BlockPos> openSlots, float efficiency, float speed, BlockPos ownPosition) {}
    
    public static Endec<AddonUiData> ADDON_UI_ENDEC = StructEndecBuilder.of(
      BuiltInEndecs.BLOCK_POS.listOf().fieldOf("addon_positions", AddonUiData::positions),
      BuiltInEndecs.BLOCK_POS.listOf().fieldOf("open_slots", AddonUiData::openSlots),
      Endec.FLOAT.fieldOf("efficiency", AddonUiData::efficiency),
      Endec.FLOAT.fieldOf("speed", AddonUiData::speed),
      BuiltInEndecs.BLOCK_POS.fieldOf("ownPosition", AddonUiData::ownPosition),
      AddonUiData::new
    );
    
}
