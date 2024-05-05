package rearth.oritech.block.entity.machines.processing;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
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
    public void resetAddons() {
        super.resetAddons();
        hasByproductAddon = false;
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("byproductAddon", hasByproductAddon);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        hasByproductAddon = nbt.getBoolean("byproductAddon");
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
          new GuiSlot(1, 117, 20),
          new GuiSlot(2, 117, 38),
          new GuiSlot(3, 117, 56));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.GRINDER_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 4;
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
