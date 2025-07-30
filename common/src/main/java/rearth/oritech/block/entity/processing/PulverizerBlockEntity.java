package rearth.oritech.block.entity.processing;

import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.UpgradableMachineBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PulverizerBlockEntity extends UpgradableMachineBlockEntity {
    
    public PulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PULVERIZER_ENTITY, pos, state, Oritech.CONFIG.processingMachines.pulverizerData.energyPerTick());
    }
    
    @Override
    protected void craftItem(OritechRecipe activeRecipe, List<ItemStack> outputInventory, List<ItemStack> inputInventory) {
        super.craftItem(activeRecipe, outputInventory, inputInventory);
        combineSmallDusts(outputInventory, level);
    }
    
    public static void combineSmallDusts(List<ItemStack> outputInventory, Level world) {
        // try compacting
        var smallDustStack = outputInventory.get(1);
        var baseResult = outputInventory.get(0);
        if (smallDustStack.isEmpty() || smallDustStack.getCount() < 9 || baseResult.getCount() >= baseResult.getMaxStackSize())
            return;
        
        var recipeInputStacks = new ArrayList<ItemStack>(9);
        for (int i = 0; i < 9; i++) {
            recipeInputStacks.add(i, smallDustStack.copyWithCount(1));
        }
        var craftingInv = CraftingInput.of(3, 3, recipeInputStacks);
        
        var matches = world.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, craftingInv, world);
        
        if (matches.isEmpty()) return;
        
        // gets the result stack of each entry, then filters if the type matches, and then checks if there is a result
        var foundResult = !matches
                             .stream()
                             .map(elem -> elem.value().getResultItem(null))
                             .filter(elem -> baseResult.getItem().equals(elem.getItem()))
                             .toList().isEmpty();
        
        if (foundResult) {
            smallDustStack.shrink(9);
            baseResult.grow(1);
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
    public InventorySlotAssignment getSlotAssignments() {
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
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.PULVERIZER_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 3;
    }
    
    @Override
    protected void useEnergy() {
        super.useEnergy();
        
        if (level.random.nextFloat() > 0.7 && !inventory.getItem(0).isEmpty()) {
            var effect = new ItemParticleOption(ParticleTypes.ITEM, inventory.getItem(0).copy());
            var spawnAt = worldPosition.getCenter().add(0, 0.3, 0);
            var offsetX = (level.random.nextFloat() - 0.5) * 0.1;
            var offsetY = (level.random.nextFloat()) * 0.1;
            var offsetZ = (level.random.nextFloat() - 0.5) * 0.1;
            ((ServerLevel) level).sendParticles(effect, spawnAt.x(), spawnAt.y(), spawnAt.z(), 3, offsetX, offsetY, offsetZ, 0.08);
        }
        
        
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
