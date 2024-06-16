package rearth.oritech.block.base.entity;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.FluidProvider;

import java.util.Optional;

public abstract class FluidMultiblockGeneratorBlockEntity extends MultiblockGeneratorBlockEntity implements FluidProvider {
    
    private final SingleVariantStorage<FluidVariant> inputTank = new SingleVariantStorage<>() {
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
    protected void tryConsumeInput() {
        
        var recipeCandidate = getRecipe();
        if (recipeCandidate.isEmpty())
            currentRecipe = OritechRecipe.DUMMY;     // reset recipe when invalid or no input is given
        
        if (recipeCandidate.isPresent()) {
            // this is separate so that progress is not reset when out of energy
            var activeRecipe = recipeCandidate.get().value();
            currentRecipe = activeRecipe;
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
    }
    
    // gets all recipe of target type, and only checks for matching liquids
    @Override
    protected Optional<RecipeEntry<OritechRecipe>> getRecipe() {
        
        if (inputTank.isResourceBlank() || inputTank.amount <= 0) return Optional.empty();
        
        var availableRecipes = world.getRecipeManager().listAllOfType(getOwnRecipeType());
        for (var recipeEntry : availableRecipes) {
            var recipe = recipeEntry.value();
            var recipeFluid = recipe.getFluidInput();
            if (recipeFluid.variant().equals(inputTank.variant) && inputTank.amount >= recipeFluid.amount())
                return Optional.of(recipeEntry);
        }
        
        return Optional.empty();
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        SingleVariantStorage.writeNbt(inputTank, FluidVariant.CODEC, nbt, registryLookup);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        SingleVariantStorage.readNbt(inputTank, FluidVariant.CODEC, FluidVariant::blank, nbt, registryLookup);
    }
    
    @Override
    protected void sendNetworkEntry() {
        super.sendNetworkEntry();
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.SingleVariantFluidSyncPacket(pos, Registries.FLUID.getId(inputTank.variant.getFluid()).toString(), inputTank.amount));
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
