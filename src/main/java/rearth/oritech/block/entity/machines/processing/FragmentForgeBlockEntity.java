package rearth.oritech.block.entity.machines.processing;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.ArrayList;
import java.util.List;

public class FragmentForgeBlockEntity extends MultiblockMachineEntity {
    
    private boolean hasByproductAddon;
    
    public FragmentForgeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FRAGMENT_FORGE_ENTITY, pos, state, Oritech.CONFIG.processingMachines.fragmentForgeData.energyPerTick());
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.processingMachines.fragmentForgeData.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.processingMachines.fragmentForgeData.maxEnergyInsertion();
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.GRINDER;
    }
    
    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_YIELD_ADDON)) {
            hasByproductAddon = true;
        }
    }
    
    @Override
    protected void useEnergy() {
        super.useEnergy();
        
        if (world.random.nextFloat() > 0.8) return;
        // emit particles
        var facing = getFacing();
        var offsetLocal = Geometry.rotatePosition(new Vec3d(0.4, 0.6, 0.5), facing);
        var emitPosition = Vec3d.ofCenter(pos).add(offsetLocal);
        
        ParticleContent.GRINDER_WORKING.spawn(world, emitPosition, 1);
        
    }
    
    @Override
    public void resetAddons() {
        super.resetAddons();
        hasByproductAddon = false;
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("byproductAddon", hasByproductAddon);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        hasByproductAddon = nbt.getBoolean("byproductAddon");
    }
    
    @Override
    protected void craftItem(OritechRecipe activeRecipe, List<ItemStack> outputInventory, List<ItemStack> inputInventory) {
        super.craftItem(activeRecipe, outputInventory, inputInventory);
        PulverizerBlockEntity.combineSmallDusts(outputInventory, world);
    }
    
    @Override
    public List<ItemStack> getCraftingResults(OritechRecipe activeRecipe) {
        if (hasByproductAddon) {
            var result = new ArrayList<ItemStack>(activeRecipe.getResults().size());
            var source = activeRecipe.getResults();
            for (int i = 0; i < source.size(); i++) {
                var item = source.get(i);
                if (i == 0) {
                    result.add(item);
                } else {
                    var newCount = item.getCount() * 2;
                    var newItem = new ItemStack(item.getItem(), newCount);
                    result.add(newItem);
                }
            }
            return result;
        } else {
            return super.getCraftingResults(activeRecipe);
        }
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 1, 1, 3);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 56, 38),
          new GuiSlot(1, 117, 20, true),
          new GuiSlot(2, 117, 38, true),
          new GuiSlot(3, 117, 56, true));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.GRINDER_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 4;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    // x = back
    // y = up
    // z = left
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1,0),    // middle
          new Vec3i(0, 0,1),    // left
          new Vec3i(0, 1,1),
          new Vec3i(1, 0,1),    // back left
          new Vec3i(1, 1,1),
          new Vec3i(1, 0, 0),    // back middle
          new Vec3i(1, 1,0)
        );
    }
    
    // x = back, // z = left
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, 0,2),
          new Vec3i(1, 0,2),
          new Vec3i(2, 0,0),
          new Vec3i(1, 0,-1),
          new Vec3i(-1, 0,1)
        );
    }
}
