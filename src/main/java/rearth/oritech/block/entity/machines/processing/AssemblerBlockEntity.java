package rearth.oritech.block.entity.machines.processing;

import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;

public class AssemblerBlockEntity extends MultiblockMachineEntity {
    
    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ASSEMBLER_ENTITY, pos, state, Oritech.CONFIG.processingMachines.assemblerData.energyPerTick());
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.processingMachines.assemblerData.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.processingMachines.assemblerData.maxEnergyInsertion();
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.ASSEMBLER;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 4, 4, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 38, 26),
          new GuiSlot(1, 56, 26),
          new GuiSlot(2, 38, 44),
          new GuiSlot(3, 56, 44),
          new GuiSlot(4, 117, 36, true));
    }
    
    @Override
    protected void useEnergy() {
        super.useEnergy();
        
        if (world.random.nextFloat() > 0.4) return;
        // emit particles
        var facing = getFacing();
        var offsetLocal = Geometry.rotatePosition(new Vec3d(0, 0.6, 0.5), facing);
        var emitPosition = Vec3d.ofCenter(pos).add(offsetLocal);
        
        ParticleContent.ASSEMBLER_WORKING.spawn(world, emitPosition, 1);
        
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.ASSEMBLER_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 5;
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
          new Vec3i(0, 0,2),
          new Vec3i(1, 0,0)
        );
    }
}
