package rearth.oritech.block.base.entity;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.FluidProvider;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;
import java.util.Optional;

public abstract class FluidMultiblockGeneratorBlockEntity extends MultiblockGeneratorBlockEntity implements FluidProvider {
    
    public final SingleVariantStorage<FluidVariant> inputTank = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }
        
        @Override
        protected long getCapacity(FluidVariant variant) {
            return (4 * FluidConstants.BUCKET);
        }
        
        @Override
        public boolean supportsExtraction() {
            return false;
        }
        
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            FluidMultiblockGeneratorBlockEntity.this.markDirty();
        }
    };
    
    public FluidMultiblockGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, MachineBlockEntity blockEntity) {
        
        if (bucketInputAllowed() && !world.isClient && isActive(state) && !inventory.getStack(0).isEmpty()) {
            processBuckets();
        }
        
        super.tick(world, pos, state, blockEntity);
    }
    
    private void processBuckets() {
        var inStack = inventory.getStack(0);
        var emptyBucket = ItemVariant.of(Items.BUCKET, inStack.getComponentChanges()).toStack();
        if (inStack.getItem() instanceof BucketItem && outputCanAcceptBucket(emptyBucket)) {
            var bucketFluid = ((BucketItemAccessor) inStack.getItem()).fabric_getFluid();
            if (bucketFluid == Fluids.EMPTY) return;
            
            try (var tx = Transaction.openOuter()) {
                var inserted = inputTank.insert(FluidVariant.of(bucketFluid), FluidConstants.BUCKET, tx);
                if (inserted != FluidConstants.BUCKET) return;
                
                inStack.decrement(1);
                if (inventory.getStack(1).isEmpty()) {
                    inventory.heldStacks.set(1, emptyBucket);
                } else {
                    inventory.getStack(1).increment(1);
                }
                inventory.heldStacks.set(0, inStack);
                tx.commit();
            }
        }
    }
    
    private boolean outputCanAcceptBucket(ItemStack bucket) {
        var slot = inventory.getStack(1);
        return (slot.isEmpty() || (slot.isStackable() && ItemStack.areItemsAndComponentsEqual(slot, bucket) && slot.getCount() < slot.getMaxCount()));
    }
    
    @Override
    protected void tryConsumeInput() {
        
        if (isProducingSteam && (waterStorage.amount == 0 || steamStorage.amount == steamStorage.getCapacity())) return;
        
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
        var fluidStack = activeRecipe.getFluidInput();
        inputTank.amount -= fluidStack.amount();
        
        markNetDirty();
        markDirty();
    }
    
    // gets all recipe of target type, and only checks for matching liquids
    @Override
    protected Optional<RecipeEntry<OritechRecipe>> getRecipe() {
        return getRecipe(inputTank);
    }
    
    protected Optional<RecipeEntry<OritechRecipe>> getRecipe(SingleVariantStorage<FluidVariant> checkedTank) {
        
        if (checkedTank.isResourceBlank() || checkedTank.amount <= 0) return Optional.empty();
        
        var availableRecipes = world.getRecipeManager().listAllOfType(getOwnRecipeType());
        for (var recipeEntry : availableRecipes) {
            var recipe = recipeEntry.value();
            var recipeFluid = recipe.getFluidInput();
            if (recipeFluid.variant().equals(checkedTank.variant) && checkedTank.amount >= recipeFluid.amount())
                return Optional.of(recipeEntry);
        }
        
        return Optional.empty();
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        SingleVariantStorage.writeNbt(inputTank, FluidVariant.CODEC, nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        SingleVariantStorage.readNbt(inputTank, FluidVariant.CODEC, FluidVariant::blank, nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
    }
    
    @Override
    protected void sendNetworkEntry() {
        super.sendNetworkEntry();
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.SingleVariantFluidSyncPacket(pos, Registries.FLUID.getId(inputTank.variant.getFluid()).toString(), inputTank.amount));
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
    public InventorySlotAssignment getSlots() {
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
    public Storage<FluidVariant> getFluidStorage(Direction direction) {
        return inputTank;
    }
    
    @Override
    public @Nullable SingleVariantStorage<FluidVariant> getForDirectFluidAccess() {
        return inputTank;
    }
}
