package rearth.oritech.block.entity.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.List;

public class FoundryBlockEntity extends MultiblockMachineEntity {
    
    public FoundryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FOUNDRY_ENTITY, pos, state, OritechConfig.processingMachines.foundryData.energyPerTick.get());
    }
    
    @Override
    public long getDefaultCapacity() {
        return OritechConfig.processingMachines.foundryData.energyCapacity.get();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.processingMachines.foundryData.maxEnergyInsertion.get();
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.FOUNDRY;
    }
    
    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 2, 2, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 56, 26),
          new GuiSlot(1, 56, 44),
          new GuiSlot(2, 117, 36, true));
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
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
