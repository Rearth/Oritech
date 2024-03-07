package rearth.oritech.block.entity.machines.processing;

import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.block.base.entity.UpgradableMachineBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.*;

import java.util.List;

public class PulverizerBlockEntity extends UpgradableMachineBlockEntity {
    
    public PulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PULVERIZER_ENTITY, pos, state, 20);
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.PULVERIZER;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 1, 1, 2);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 80, 11),
          new GuiSlot(1, 70, 59),
          new GuiSlot(2, 90, 59));
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.PULVERIZER_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 3;
    }
    
    @Override
    protected void useEnergy() {
        super.useEnergy();
        if (world.random.nextFloat() > 0.8)
            ParticleContent.PULVERIZER_WORKING.spawn(world, Vec3d.of(pos), 1);
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(1, 0,0)
        );
    }
    
    @Override
    public float getCoreQuality() {
        return 2;
    }
}
