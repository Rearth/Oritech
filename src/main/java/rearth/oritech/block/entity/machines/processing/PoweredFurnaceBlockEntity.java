package rearth.oritech.block.entity.machines.processing;

import net.minecraft.block.BlockState;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;
import java.util.Objects;

public class PoweredFurnaceBlockEntity extends MultiblockMachineEntity {
    
    private final float FURNACE_SPEED_MULTIPLIER = Oritech.CONFIG.processingMachines.furnaceData.speedMultiplier();
    
    public PoweredFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.POWERED_FURNACE_ENTITY, pos, state, Oritech.CONFIG.processingMachines.furnaceData.energyPerTick());
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.processingMachines.furnaceData.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.processingMachines.furnaceData.maxEnergyInsertion();
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.ASSEMBLER;
    }   // not used in this special case
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, MachineBlockEntity blockEntity) {
        
        if (world.isClient || !isActive(state)) return;
        
        var recipeCandidate = world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, getFurnaceInput(), world);
        
        if (recipeCandidate.isPresent() && canAddToSlot(recipeCandidate.get().value().getResult(world.getRegistryManager()), inventory.heldStacks.get(1))) {
            if (hasEnoughEnergy()) {
                
                var activeRecipe = recipeCandidate.get().value();
                useEnergy();
                progress++;
                lastWorkedAt = world.getTime();
                
                if (world.random.nextFloat() > 0.8)
                    ParticleContent.FURNACE_BURNING.spawn(world, Vec3d.of(pos), 1);
                
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
    
    private SingleStackRecipeInput getFurnaceInput() {
        return new SingleStackRecipeInput(getInputView().get(0));
    }
    
    @SuppressWarnings("OptionalIsPresent")
    @Override
    public float getProgress() {
        if (progress == 0) return 0;
        
        var recipeCandidate = Objects.requireNonNull(world).getRecipeManager().getFirstMatch(RecipeType.SMELTING, getFurnaceInput(), world);
        if (recipeCandidate.isPresent()) {
            return (float) progress / getRecipeDuration();
        }
        
        return 0;
    }
    
    @SuppressWarnings("OptionalIsPresent")
    @Override
    protected int getRecipeDuration() {
        var recipeCandidate = Objects.requireNonNull(world).getRecipeManager().getFirstMatch(RecipeType.SMELTING, getFurnaceInput(), world);
        if (recipeCandidate.isPresent()) {
            return (int) (recipeCandidate.get().value().getCookingTime() * getSpeedMultiplier());
        }
        
        return 120;
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
          new GuiSlot(0, 56, 38),
          new GuiSlot(1, 117, 38, true));
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
