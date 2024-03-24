package rearth.oritech.block.entity.machines.processing;

import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;

public class CentrifugeBlockEntity extends MultiblockMachineEntity {
    
    // todo create addon that enables different recipe type
    // add fluid processing option, adding 2 tanks for input and output
    // add different gui with two tanks
    
    // missing addon models:
    // block destroyer crop filter - done
    // fragment forge yield addon - done
    // centrifuge fluid addon - done
    
    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.CENTRIFUGE_ENTITY, pos, state, 32);
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.CENTRIFUGE;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 1, 1, 3);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 80, 11),
          new GuiSlot(1, 60, 59),
          new GuiSlot(2, 80, 59),
          new GuiSlot(3, 100, 59));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.CENTRIFUGE_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 4;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1,0)
        );
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        
        return List.of(
          new Vec3i(0, 0,-1),
          new Vec3i(0, 0,1)
        );
    }
}
