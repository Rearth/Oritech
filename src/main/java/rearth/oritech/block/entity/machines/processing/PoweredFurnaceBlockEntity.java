package rearth.oritech.block.entity.machines.processing;

import net.minecraft.block.BlockState;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;

import java.util.List;
import java.util.Objects;

public class PoweredFurnaceBlockEntity extends MultiblockMachineEntity {
    
    private final float FURNACE_SPEED_MULTIPLIER = 0.5f;
    
    public PoweredFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.POWERED_FURNACE_ENTITY, pos, state, 30);
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.ASSEMBLER;
    }   // not used in this special case
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, MachineBlockEntity blockEntity) {
        
        if (world.isClient || !isActive(state)) return;
        
        var recipeCandidate = world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, getInputInventory(), world);
        
        if (recipeCandidate.isPresent() && canAddToSlot(recipeCandidate.get().value().getResult(world.getRegistryManager()), inventory.heldStacks.get(1))) {
            if (hasEnoughEnergy()) {
                
                var activeRecipe = recipeCandidate.get().value();
                useEnergy();
                progress++;
                
                if (furnaceCraftingFinished(activeRecipe)) {
                    craftFurnaceItem(activeRecipe);
                    resetProgress();
                }
                
                markNetDirty();
                
            }
        } else {
            // this happens if either the input slot is empty, or the output slot is blocked
            if (progress > 0) resetProgress();
        }
        
        if (networkDirty) {
            updateNetwork();
        }
    }
    
    private void craftFurnaceItem(SmeltingRecipe activeRecipe) {
        var result = activeRecipe.getResult(world.getRegistryManager());
        var outSlot = inventory.heldStacks.get(1);
        var inSlot = inventory.heldStacks.get(0);
        
        inSlot.decrement(1);
        if (outSlot.isEmpty()) {
            inventory.heldStacks.set(1, result.copy());
        } else {
            outSlot.increment(result.getCount());
        }
        
    }
    
    private boolean furnaceCraftingFinished(SmeltingRecipe activeRecipe) {
        return progress >= activeRecipe.getCookingTime() * getSpeedMultiplier();
    }
    
    @SuppressWarnings("OptionalIsPresent")
    @Override
    public float getProgress() {
        if (progress == 0) return 0;
        
        var recipeCandidate = Objects.requireNonNull(world).getRecipeManager().getFirstMatch(RecipeType.SMELTING, getInputInventory(), world);
        if (recipeCandidate.isPresent()) {
            return (float) progress / getRecipeDuration();
        }
        
        return 0;
    }
    
    @SuppressWarnings("OptionalIsPresent")
    @Override
    protected int getRecipeDuration() {
        var recipeCandidate = Objects.requireNonNull(world).getRecipeManager().getFirstMatch(RecipeType.SMELTING, getInputInventory(), world);
        if (recipeCandidate.isPresent()) {
            return (int) (recipeCandidate.get().value().getCookingTime() * getSpeedMultiplier());
        }
        
        return 1;
    }
    
    @Override
    public float getSpeedMultiplier() {
        return super.getSpeedMultiplier() * FURNACE_SPEED_MULTIPLIER;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 1, 1, 1);
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 80, 11),
          new GuiSlot(1, 80, 59));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.POWERED_FURNACE_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 2;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1,0)
        );
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, -1,0)
        );
    }
}
