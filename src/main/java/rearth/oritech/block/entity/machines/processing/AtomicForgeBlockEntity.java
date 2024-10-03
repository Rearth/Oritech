package rearth.oritech.block.entity.machines.processing;

import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.List;

public class AtomicForgeBlockEntity extends MultiblockMachineEntity {
    
    public AtomicForgeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ATOMIC_FORGE_ENTITY, pos, state, Oritech.CONFIG.processingMachines.atomicForgeData.energyPerTick());
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.processingMachines.atomicForgeData.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.processingMachines.atomicForgeData.maxEnergyInsertion();
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.ATOMIC_FORGE;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 3, 3, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 56, 38),            
          new GuiSlot(1, 83, 21),
          new GuiSlot(2, 83, 54),
          new GuiSlot(3, 117, 36, true));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.ATOMIC_FORGE_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 4;
    }
    
    @Override
    public EnergyStorage getEnergyStorageForLink() {
        return null;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(1, 0,1),
          new Vec3i(1, 0,0),
          new Vec3i(1, 0,-1),
          new Vec3i(0, 0,1),
          new Vec3i(0, 0,-1),
          new Vec3i(-1, 0,1),
          new Vec3i(-1, 0,0),
          new Vec3i(-1, 0,-1)
        );
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return new ArrayList<>();
    }
    
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        sendNetworkEntry();
        return new ModScreens.BasicData(pos);
    }
}
