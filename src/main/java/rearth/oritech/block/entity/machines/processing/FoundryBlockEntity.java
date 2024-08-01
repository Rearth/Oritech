package rearth.oritech.block.entity.machines.processing;

import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;

public class FoundryBlockEntity extends MultiblockMachineEntity {
    
    public FoundryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FOUNDRY_ENTITY, pos, state, Oritech.CONFIG.processingMachines.foundryData.energyPerTick());
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.processingMachines.foundryData.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.processingMachines.foundryData.maxEnergyInsertion();
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.FOUNDRY;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 2, 2, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 56, 26),
          new GuiSlot(1, 56, 44),
          new GuiSlot(2, 117, 36, true));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.FOUNDRY_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 3;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 0,1),
          new Vec3i(0, 1,0),
          new Vec3i(0, 1,1)
        );
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        
        return List.of(
          new Vec3i(0, 0,-1),
          new Vec3i(0, 0,2)
        );
    }
}
