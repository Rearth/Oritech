package rearth.oritech.block.entity.machines;

import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;

public class GrinderBlockEntity extends MachineBlockEntity {
    
    public GrinderBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.GRINDER_ENTITY, pos, state);
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.GRINDER;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 4, 4, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 80, 11),
          new GuiSlot(1, 100, 11),
          new GuiSlot(2, 120, 11),
          new GuiSlot(3, 140, 11),
          new GuiSlot(4, 80, 59));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.GRINDER_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 5;
    }
}
