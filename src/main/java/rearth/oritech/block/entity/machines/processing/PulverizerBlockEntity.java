package rearth.oritech.block.entity.machines.processing;

import net.minecraft.block.BlockState;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.UpgradableMachineBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.ArrayList;
import java.util.List;

public class PulverizerBlockEntity extends UpgradableMachineBlockEntity {
    
    public PulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PULVERIZER_ENTITY, pos, state, Oritech.CONFIG.processingMachines.pulverizerData.energyPerTick());
    }
    
    @Override
    protected void craftItem(OritechRecipe activeRecipe, List<ItemStack> outputInventory, List<ItemStack> inputInventory) {
        super.craftItem(activeRecipe, outputInventory, inputInventory);
        combineSmallDusts(outputInventory, world);
    }
    
    public static void combineSmallDusts(List<ItemStack> outputInventory, World world) {
        // try compacting
        var smallDustStack = outputInventory.get(1);
        var baseResult = outputInventory.get(0);
        if (smallDustStack.isEmpty() || smallDustStack.getCount() < 9 || baseResult.getCount() >= baseResult.getMaxCount())
            return;
        
        var recipeInputStacks = new ArrayList<ItemStack>(9);
        for (int i = 0; i < 9; i++) {
            recipeInputStacks.add(i, smallDustStack.copyWithCount(1));
        }
        var craftingInv = CraftingRecipeInput.create(3, 3, recipeInputStacks);
        
        var matches = world.getRecipeManager().getAllMatches(RecipeType.CRAFTING, craftingInv, world);
        
        if (matches.isEmpty()) return;
        
        // gets the result stack of each entry, then filters if the type matches, and then checks if there is a result
        var foundResult = !matches
                             .stream()
                             .map(elem -> elem.value().getResult(null))
                             .filter(elem -> baseResult.getItem().equals(elem.getItem()))
                             .toList().isEmpty();
        
        if (foundResult) {
            smallDustStack.decrement(9);
            baseResult.increment(1);
        }
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.processingMachines.pulverizerData.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.processingMachines.pulverizerData.maxEnergyInsertion();
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
          new GuiSlot(0, 56, 38),
          new GuiSlot(1, 117, 38, true),
          new GuiSlot(2, 135, 38, true));
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
        if (world.random.nextFloat() > 0.5)
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
