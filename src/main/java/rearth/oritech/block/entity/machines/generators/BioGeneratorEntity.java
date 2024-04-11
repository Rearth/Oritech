package rearth.oritech.block.entity.machines.generators;

import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.block.base.entity.MultiblockGeneratorBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;

public class BioGeneratorEntity extends MultiblockGeneratorBlockEntity {
    public BioGeneratorEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BIO_GENERATOR_ENTITY, pos, state, 30);
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.BIO_GENERATOR;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 1, 1, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 75, 21),
          new GuiSlot(1, 75, 59));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.BIO_GENERATOR_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 2;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        
        return List.of(
          new Vec3i(1, 0, 0),
          new Vec3i(1, 1, 0)
        );
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1, 0)
        );
    }
}
