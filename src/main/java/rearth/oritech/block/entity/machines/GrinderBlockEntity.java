package rearth.oritech.block.entity.machines;

import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;

public class GrinderBlockEntity extends MultiblockMachineEntity {
    
    public GrinderBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.GRINDER_ENTITY, pos, state, 50);
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
    
    // x = back
    // y = up
    // z = left
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1,0),    // middle
          new Vec3i(0, 2,0),
          new Vec3i(0, 0,1),    // left
          new Vec3i(0, 1,1),
          new Vec3i(0, 2,1),
          new Vec3i(0, 0,-1),    // right
          new Vec3i(0, 1,-1),
          new Vec3i(0, 2,-1),
          new Vec3i(1, 0,1),    // middle left
          new Vec3i(1, 1,1),
          new Vec3i(1, 2,1),
          new Vec3i(1, 0, 0),    // middle middle
          new Vec3i(1, 1,0),
          new Vec3i(1, 2,0),
          new Vec3i(1, 0, -1),    // middle right
          new Vec3i(1, 1,-1),
          new Vec3i(1, 2,-1),
          new Vec3i(2, 0,1),    // back left
          new Vec3i(2, 1,1),
          new Vec3i(2, 2,1),
          new Vec3i(2, 0, 0),    // back middle
          new Vec3i(2, 1,0),
          new Vec3i(2, 2,0),
          new Vec3i(2, 0, -1),    // back right
          new Vec3i(2, 1,-1),
          new Vec3i(2, 2,-1)
        );
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, 0,2),
          new Vec3i(1, 0,2),
          new Vec3i(2, 0,2),
          new Vec3i(1, 1,2),
          new Vec3i(2, 1,2),
          new Vec3i(0, 0,-2),
          new Vec3i(1, 0,-2),
          new Vec3i(2, 0,-2),
          new Vec3i(1, 1,-2),
          new Vec3i(2, 1,-2)
        );
    }
}
