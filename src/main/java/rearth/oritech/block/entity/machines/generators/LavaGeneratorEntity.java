package rearth.oritech.block.entity.machines.generators;

import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.ArrayList;
import java.util.List;

public class LavaGeneratorEntity extends UpgradableGeneratorBlockEntity {
    public LavaGeneratorEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.LAVA_GENERATOR_ENTITY, pos, state, 30);
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.TEST_GENERATOR;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 1, 1, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 75, 11),
          new GuiSlot(1, 75, 59));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.TEST_GENERATOR_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 2;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return new ArrayList<>();
    }
}
