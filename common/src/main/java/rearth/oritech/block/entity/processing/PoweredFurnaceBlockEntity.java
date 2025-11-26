package rearth.oritech.block.entity.processing;

import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkedBlockEntity;
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
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

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
    protected float calculateEnergyUsage() {
        return energyPerTick * getEfficiencyMultiplier() * (1 / getSpeedMultiplier()) / 2;
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        if (!isActive(state)) return;
        
        var recipeCandidate = world.getRecipeManager().getRecipeFor(RecipeType.SMELTING, getFurnaceInput(), world);
        
        if (recipeCandidate.isPresent() && canAddToSlot(recipeCandidate.get().value().getResultItem(world.registryAccess()), inventory.heldStacks.get(1))) {
            if (hasEnoughEnergy()) {
                
                var activeRecipe = recipeCandidate.get().value();
                useEnergy();
                progress++;
                lastWorkedAt = world.getGameTime();
                
                if (world.random.nextFloat() > 0.8)
                    ParticleContent.FURNACE_BURNING.spawn(world, Vec3.atLowerCornerOf(pos), 1);
                
                if (furnaceCraftingFinished(activeRecipe)) {
                    craftFurnaceItem(activeRecipe);
                    
                    for (int i = 0; i < this.getBaseAddonData().extraChambers(); i++) {
                        if (!canAddToSlot(recipeCandidate.get().value().getResultItem(world.registryAccess()), inventory.heldStacks.get(1)) || inventory.heldStacks.get(0).isEmpty()) break;
                        craftFurnaceItem(activeRecipe);
                    }
                    
                    resetProgress();
                }
                
                setChanged();
                
            }
        } else {
            // this happens if either the input slot is empty, or the output slot is blocked
            if (progress > 0) resetProgress();
        }
        
        addBurstTicks();
        
        if (world.getGameTime() % 18 == 0)
            updateFurnaceState(state);
        
    }
    
    private void updateFurnaceState(BlockState state) {
        var wasLit = state.getValue(BlockStateProperties.LIT);
        var isLit = isActivelyWorking();
        
        if (wasLit != isLit) {
            level.setBlockAndUpdate(worldPosition, state.setValue(BlockStateProperties.LIT, isLit));
        }
        
    }
    
    private void craftFurnaceItem(SmeltingRecipe activeRecipe) {
        var result = activeRecipe.getResultItem(level.registryAccess());
        var outSlot = inventory.heldStacks.get(1);
        var inSlot = inventory.heldStacks.get(0);
        
        inSlot.shrink(1);
        if (outSlot.isEmpty()) {
            inventory.heldStacks.set(1, result.copy());
        } else {
            outSlot.grow(result.getCount());
        }
        
    }
    
    private boolean furnaceCraftingFinished(SmeltingRecipe activeRecipe) {
        return progress >= activeRecipe.getCookingTime() * getSpeedMultiplier();
    }
    
    private SingleRecipeInput getFurnaceInput() {
        return new SingleRecipeInput(getInputView().get(0));
    }
    
    @SuppressWarnings("OptionalIsPresent")
    @Override
    public float getProgress() {
        if (progress == 0) return 0;
        
        var recipeCandidate = Objects.requireNonNull(level).getRecipeManager().getRecipeFor(RecipeType.SMELTING, getFurnaceInput(), level);
        if (recipeCandidate.isPresent()) {
            return (float) progress / getRecipeDuration();
        }
        
        return 0;
    }
    
    @SuppressWarnings("OptionalIsPresent")
    @Override
    protected int getRecipeDuration() {
        var recipeCandidate = Objects.requireNonNull(level).getRecipeManager().getRecipeFor(RecipeType.SMELTING, getFurnaceInput(), level);
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
    public InventorySlotAssignment getSlotAssignments() {
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
    public MenuType<?> getScreenHandlerType() {
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
