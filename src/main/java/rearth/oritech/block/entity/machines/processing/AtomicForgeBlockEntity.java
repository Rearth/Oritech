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

import java.util.ArrayList;
import java.util.List;

public class AtomicForgeBlockEntity extends MultiblockMachineEntity {
    
    public AtomicForgeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ATOMIC_FORGE_ENTITY, pos, state, 1000);
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.ATOMIC_FORGE;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 2, 2, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 70, 11),
          new GuiSlot(1, 90, 11),
          new GuiSlot(2, 80, 59));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.ATOMIC_FORGE_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 3;
    }
    
    @Override
    public long getDefaultInsertRate() {
        return 0;
    }
    
    @Override
    public long getDefaultCapacity() {
        return 1000;
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
}
