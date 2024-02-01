package rearth.oritech.block.base.entity;

import io.netty.buffer.Unpooled;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import net.minecraft.block.Block;
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
import rearth.oritech.block.custom.MachineAddonBlock;
import rearth.oritech.block.custom.MachineCoreBlock;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.*;

public abstract class UpgradableMachineBlockEntity extends MachineBlockEntity {
    
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    
    private float combinedSpeed = 1;
    private float combinedEfficiency = 1;
    
    public UpgradableMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
    }
    
    public void initAddons(BlockState state) {
        connectedAddons.clear();
        
        var foundAddons = getAllAddons();
        
        gatherAddonStats(foundAddons);
        writeAddons(foundAddons);
        
        for (var addon : foundAddons) {
            connectedAddons.add(addon.pos);
        }
        
        System.out.println(connectedAddons.size() + " addons connected");
        
    }
    
    public void resetAddons() {
        System.out.println("resetting addons");
        
        for (var addon : connectedAddons) {
            var state = Objects.requireNonNull(world).getBlockState(addon);
            if (state.getBlock() instanceof MachineAddonBlock) {
                world.setBlockState(addon, state.with(MachineAddonBlock.ADDON_USED, false));
            }
        }
        
        connectedAddons.clear();
    }
    
    private List<AddonBlock> getAllAddons() {
        
        var maxIterationCount = 6;
        
        // start with base slots (on machine itself)
        // repeat N times (dependant on core quality?):
        //   go through all slots
        //   check if slot is occupied by MachineAddonBlock, check if block is not used
        //   if valid and extender: add all neighboring positions to search set
        assert world != null;
        
        System.out.println("initializing addon slots");
        
        var baseSlots = getAddonSlots();    // available addon slots on machine itself (includes multiblocks)
        var searchedPositions = new HashSet<BlockPos>(baseSlots.size()); // all positions ever checked, to avoid adding duplicates
        var queuedPositions = new ArrayList<BlockPos>(baseSlots.size());
        var result = new ArrayList<AddonBlock>(baseSlots.size());   // results, unused addon blocks
        
        // fill initial spots
        for (var initialSpot : baseSlots) {
            queuedPositions.add((BlockPos) offsetToWorldPosition(getFacing(), initialSpot, pos));
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
                var candidateEntity = world.getBlockEntity(candidatePos, BlockEntitiesContent.ADDON_ENTITY);
                
                if (!(candidate.getBlock() instanceof MachineAddonBlock addonBlock) || candidateEntity.isEmpty()) continue;
                
                if (candidate.get(MachineAddonBlock.ADDON_USED) && !candidateEntity.get().getControllerPos().equals(pos)) continue;
                
                var entry = new AddonBlock(addonBlock, candidate, candidatePos, candidateEntity.get());
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
        
        for (var addon : addons) {
            speed *= addon.addonBlock.getSpeedMultiplier();
            efficiency *= addon.addonBlock.getEfficiencyMultiplier();
        }
        
        System.out.println("speed: " + speed + " efficiency: " + efficiency);
        
        this.combinedSpeed = speed;
        this.combinedEfficiency = efficiency;
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
    
    private static Vec3i offsetToWorldPosition(Direction facing, Vec3i offset, Vec3i ownPos) {
        var rotated = MultiblockMachineEntity.rotatePosition(offset, facing);
        return ownPos.add(rotated);
    }
    
    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        super.writeScreenOpeningData(player, buf);
        buf.write(ADDON_UI_ENDEC, getUiData());
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this, getUiData());
    }
    
    private AddonUiData getUiData() {
        return new AddonUiData(connectedAddons, combinedEfficiency, combinedSpeed, pos);
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
    
    public abstract List<Vec3i> getAddonSlots();
    
    private record AddonBlock(MachineAddonBlock addonBlock, BlockState state, BlockPos pos, AddonBlockEntity addonEntity) {}
    
    public record AddonUiData(List<BlockPos> positions, float efficiency, float speed, BlockPos ownPosition) {}
    
    public static Endec<AddonUiData> ADDON_UI_ENDEC = StructEndecBuilder.of(
      BuiltInEndecs.BLOCK_POS.listOf().fieldOf("addon_positions", AddonUiData::positions),
      Endec.FLOAT.fieldOf("efficiency", AddonUiData::efficiency),
      Endec.FLOAT.fieldOf("speed", AddonUiData::speed),
      BuiltInEndecs.BLOCK_POS.fieldOf("efficiency", AddonUiData::ownPosition),
      AddonUiData::new
    );
    
}
