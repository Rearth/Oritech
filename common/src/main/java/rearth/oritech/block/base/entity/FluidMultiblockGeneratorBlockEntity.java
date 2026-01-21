package rearth.oritech.block.base.entity;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.StackContext;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public abstract class FluidMultiblockGeneratorBlockEntity extends MultiblockGeneratorBlockEntity implements FluidApi.BlockProvider {
    
    @SyncField
    public final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(4 * FluidStackHooks.bucketAmount(), this::setChanged) {
        @Override
        public long insert(FluidStack toInsert, boolean simulate) {
            if (toInsert.getFluid().equals(Fluids.WATER)) return 0L;    // to avoid mixups with players inserting water for boiler into main storage
            return super.insert(toInsert, simulate);
        }
    };
    
    public FluidMultiblockGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        if (bucketInputAllowed() && !world.isClientSide && isActive(state)) {
            processBuckets();
        }
        
        super.serverTick(world, pos, state, blockEntity);
    }
    
    private void processBuckets() {
        
        var inStack = inventory.getItem(0);
        var canFill = this.fluidStorage.getAmount() < this.fluidStorage.getCapacity();
        
        if (!canFill || inStack.isEmpty() || inStack.getCount() > 1) return;
        
        var stackRef = new StackContext(inStack, updated -> inventory.setItem(0, updated));
        var candidate = FluidApi.ITEM.find(stackRef);
        if (candidate == null || !candidate.supportsExtraction()) return;
        
        var moved = FluidApi.transferFirst(candidate, fluidStorage, fluidStorage.getCapacity(), false);
        
        if (moved == 0) {
            // move stack
            var outStack = inventory.getItem(1);
            if (outStack.isEmpty()) {
                inventory.setItem(1, stackRef.getValue());
                inventory.setItem(0, ItemStack.EMPTY);
            } else if (outStack.getItem().equals(stackRef.getValue().getItem()) && outStack.getCount() < outStack.getMaxStackSize()) {
                outStack.grow(1);
                inventory.setItem(0, ItemStack.EMPTY);
            }
        }
    }
    
    @Override
    protected void tryConsumeInput() {
        
        if (isProducingSteam && (boilerStorage.getInStack().getAmount() == 0 || boilerStorage.getOutStack().getAmount() >= boilerStorage.getCapacity())) return;
        
        var recipeCandidate = getRecipe();
        if (recipeCandidate.isEmpty())
            currentRecipe = OritechRecipe.DUMMY;     // reset recipe when invalid or no input is given
        
        
        if (recipeCandidate.isPresent()) {
            // this is separate so that progress is not reset when out of energy
            var activeRecipe = recipeCandidate.get().value();
            currentRecipe = activeRecipe;
            consumeFluidRecipeInput(activeRecipe);
            
        }
    }
    
    protected void consumeFluidRecipeInput(OritechRecipe activeRecipe) {
        var recipeTime = (int) (currentRecipe.getTime() * getSpeedMultiplier() * (1 / getEfficiencyMultiplier()));
        progress = recipeTime;
        setCurrentMaxBurnTime(recipeTime);
        
        // remove inputs
        // correct amount and variant is already validated in getRecipe, so we can directly remove it
        var fluidStack = fluidStorage.getStack().copyWithAmount(activeRecipe.getFluidInput().amount());
        fluidStorage.extract(fluidStack, false);
    }
    
    // gets all recipe of target type, and only checks for matching liquids
    @Override
    protected Optional<RecipeHolder<OritechRecipe>> getRecipe() {
        return getRecipe(fluidStorage);
    }
    
    protected Optional<RecipeHolder<OritechRecipe>> getRecipe(SimpleFluidStorage checkedTank) {
        return getRecipe(checkedTank, level, getOwnRecipeType());
    }
    
    public static Optional<RecipeHolder<OritechRecipe>> getRecipe(FluidApi.SingleSlotStorage checkedTank, Level world, OritechRecipeType ownType) {
        
        if (checkedTank.getStack().isEmpty()) return Optional.empty();
        
        var availableRecipes = world.getRecipeManager().getAllRecipesFor(ownType);
        for (var recipeEntry : availableRecipes) {
            var recipe = recipeEntry.value();
            var recipeFluid = recipe.getFluidInput();
            if (recipeFluid.matchesFluid(checkedTank.getStack()) && checkedTank.getStack().getAmount() >= recipeFluid.amount())
                return Optional.of(recipeEntry);
        }
        
        return Optional.empty();
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        fluidStorage.writeNbt(nbt, "");
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        fluidStorage.readNbt(nbt, "");
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 55, 35), new GuiSlot(1, 112, 35, true));
    }
    
    @Override
    public InventorySlotAssignment getSlotAssignments() {
        return new InventorySlotAssignment(0, 1, 1, 1);
    }
    
    public boolean bucketInputAllowed() {
        return true;
    }
    
    @Override
    public int getInventorySize() {
        return 2;
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(@Nullable Direction direction) {
        return fluidStorage;
    }
}
