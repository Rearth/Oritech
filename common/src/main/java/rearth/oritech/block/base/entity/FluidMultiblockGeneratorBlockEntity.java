package rearth.oritech.block.base.entity;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
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

public abstract class FluidMultiblockGeneratorBlockEntity extends MultiblockGeneratorBlockEntity implements FluidApi.BlockProvider {
    
    @SyncField
    public final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(4 * FluidStackHooks.bucketAmount(), this::markDirty) {
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
    public void serverTick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        if (bucketInputAllowed() && !world.isClient && isActive(state)) {
            processBuckets();
        }
        
        super.serverTick(world, pos, state, blockEntity);
    }
    
    private void processBuckets() {
        
        var inStack = inventory.getStack(0);
        var canFill = this.fluidStorage.getAmount() < this.fluidStorage.getCapacity();
        
        if (!canFill || inStack.isEmpty() || inStack.getCount() > 1) return;
        
        var stackRef = new StackContext(inStack, updated -> inventory.setStack(0, updated));
        var candidate = FluidApi.ITEM.find(stackRef);
        if (candidate == null || !candidate.supportsExtraction()) return;
        
        var moved = FluidApi.transferFirst(candidate, fluidStorage, fluidStorage.getCapacity(), false);
        
        if (moved == 0) {
            // move stack
            var outStack = inventory.getStack(1);
            if (outStack.isEmpty()) {
                inventory.setStack(1, stackRef.getValue());
                inventory.setStack(0, ItemStack.EMPTY);
            } else if (outStack.getItem().equals(stackRef.getValue().getItem()) && outStack.getCount() < outStack.getMaxCount()) {
                outStack.increment(1);
                inventory.setStack(0, ItemStack.EMPTY);
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
    protected Optional<RecipeEntry<OritechRecipe>> getRecipe() {
        return getRecipe(fluidStorage);
    }
    
    protected Optional<RecipeEntry<OritechRecipe>> getRecipe(SimpleFluidStorage checkedTank) {
        return getRecipe(checkedTank, world, getOwnRecipeType());
    }
    
    public static Optional<RecipeEntry<OritechRecipe>> getRecipe(FluidApi.SingleSlotStorage checkedTank, World world, OritechRecipeType ownType) {
        
        if (checkedTank.getStack().isEmpty()) return Optional.empty();
        
        var availableRecipes = world.getRecipeManager().listAllOfType(ownType);
        for (var recipeEntry : availableRecipes) {
            var recipe = recipeEntry.value();
            var recipeFluid = recipe.getFluidInput();
            if (recipeFluid.matchesFluid(checkedTank.getStack()) && checkedTank.getStack().getAmount() >= recipeFluid.amount())
                return Optional.of(recipeEntry);
        }
        
        return Optional.empty();
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        fluidStorage.writeNbt(nbt, "");
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        fluidStorage.readNbt(nbt, "");
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
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
