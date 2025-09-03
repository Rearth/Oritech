package rearth.oritech.block.entity.processing;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.OritechPlatform;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleInOutFluidStorage;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.CentrifugeScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.FluidIngredient;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;

public class CentrifugeBlockEntity extends MultiblockMachineEntity implements FluidApi.BlockProvider {
    
    @SyncField(SyncType.GUI_TICK)
    public final SimpleInOutFluidStorage fluidContainer = new SimpleInOutFluidStorage(Oritech.CONFIG.processingMachines.centrifugeData.tankSizeInBuckets() * FluidStackHooks.bucketAmount(), this::setChanged);
    
    @SyncField(SyncType.GUI_OPEN)
    public boolean hasFluidAddon = false;
    
    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.CENTRIFUGE_ENTITY, pos, state, Oritech.CONFIG.processingMachines.centrifugeData.energyPerTick());
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.processingMachines.centrifugeData.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.processingMachines.centrifugeData.maxEnergyInsertion();
    }
    
    @Override
    protected boolean canProceed(OritechRecipe recipe) {
        
        if (!hasFluidAddon) return super.canProceed(recipe);
        
        if (!recipeInputMatchesTank(fluidContainer.getInStack(), recipe)) return false;
        
        // check if output fluid would fit
        var output = recipe.getFluidOutputs().isEmpty() ? null : recipe.getFluidOutputs().getFirst();
        if (output != null && !output.isEmpty()) { // only verify fluid output if fluid output exists
            
            if (fluidContainer.getOutStack().getAmount() + output.getAmount() > fluidContainer.getCapacity())
                return false; // output too full
            
            if (!fluidContainer.getOutStack().isEmpty() && !output.isFluidEqual(fluidContainer.getOutStack()))
                return false;   // output type mismatch
        }
        
        return true;
        
    }
    
    @Override
    protected Optional<RecipeHolder<OritechRecipe>> getRecipe() {
        
        if (!hasFluidAddon)
            return super.getRecipe();
        
        // get recipes matching input items
        var candidates = Objects.requireNonNull(level).getRecipeManager().getRecipesFor(getOwnRecipeType(), getInputInventory(), level);
        // filter out recipes based on input tank
        var fluidRecipe = candidates.stream().filter(candidate -> recipeInputMatchesTank(fluidContainer.getInStack(), candidate.value())).findAny();
        if (fluidRecipe.isPresent()) {
            return fluidRecipe;
        }
        
        return getNormalRecipe();
    }
    
    // this is provided as fallback for fluid centrifuges that may still process normal stuff
    private Optional<RecipeHolder<OritechRecipe>> getNormalRecipe() {
        return level.getRecipeManager().getRecipeFor(RecipeContent.CENTRIFUGE, getInputInventory(), level);
    }
    
    public static boolean recipeInputMatchesTank(FluidStack available, OritechRecipe recipe) {
        
        var recipeNeedsFluid = recipe.getFluidInput() != null && recipe.getFluidInput().amount() > 0;
        if (!recipeNeedsFluid) return true;
        
        var isTankEmpty = available.isEmpty();
        if (isTankEmpty) return false;
        
        var recipeFluid = recipe.getFluidInput();
        return recipeFluid.matchesFluid(available) && available.getAmount() >= recipe.getFluidInput().amount();
    }
    
    @Override
    protected void craftItem(OritechRecipe activeRecipe, List<ItemStack> outputInventory, List<ItemStack> inputInventory) {
        
        var chamberCount = getBaseAddonData().extraChambers() + 1;
        
        for (int i = 0; i < chamberCount; i++) {
            var newRecipe = getRecipe();
            if (newRecipe.isEmpty() || !newRecipe.get().value().equals(currentRecipe) || !canOutputRecipe(activeRecipe) || !canProceed(activeRecipe)) break;
            super.craftItem(activeRecipe, outputInventory, inputInventory);
            
            if (hasFluidAddon) {
                craftFluids(activeRecipe);
            }
        }
    }
    
    @Override
    public boolean supportExtraChambersAuto() {
        return false;
    }
    
    private void craftFluids(OritechRecipe activeRecipe) {
        
        var input = activeRecipe.getFluidInput();
        var output = activeRecipe.getFluidOutputs().isEmpty() ? null : activeRecipe.getFluidOutputs().getFirst();
        
        if (input != null && input.amount() > 0)
            fluidContainer.getInputContainer().extract(fluidContainer.getInStack().copyWithAmount(input.amount()), false);
        if (output != null && output.getAmount() > 0)
            fluidContainer.getOutputContainer().insert(output, false);
        
    }
    
    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_FLUID_ADDON)) {
            hasFluidAddon = true;
        }
    }
    
    @Override
    public void resetAddons() {
        super.resetAddons();
        hasFluidAddon = false;
    }
    
    @Override
    public void initAddons(BlockPos brokenAddon) {
        
        var hadAddon = hasFluidAddon;
        hasFluidAddon = false;
        super.initAddons(brokenAddon);
        
        if (hasFluidAddon != hadAddon && level instanceof ServerLevel serverLevel) {
            
            // reset cache of core above
            var coreCandidate = level.getBlockEntity(worldPosition.above(), BlockEntitiesContent.MACHINE_CORE_ENTITY);
            if (coreCandidate.isPresent()) {
                var core = coreCandidate.get();
                core.resetCaches();
            }
            
            OritechPlatform.INSTANCE.resetCapabilities(serverLevel, worldPosition);
            OritechPlatform.INSTANCE.resetCapabilities(serverLevel, worldPosition.above());
            
            // trigger block update to allow pipes to connect/disconnect
            level.blockUpdated(worldPosition, getBlockState().getBlock());
            level.blockUpdated(worldPosition.above(), level.getBlockState(worldPosition.above()).getBlock());
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        nbt.putBoolean("fluidAddon", hasFluidAddon);
        fluidContainer.writeNbt(nbt, "");
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        
        hasFluidAddon = nbt.getBoolean("fluidAddon");
        fluidContainer.readNbt(nbt, "");
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        if (hasFluidAddon) return RecipeContent.CENTRIFUGE_FLUID;
        return RecipeContent.CENTRIFUGE;
    }
    
    @Override
    public InventorySlotAssignment getSlotAssignments() {
        return new InventorySlotAssignment(0, 1, 1, 2);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 56, 38),
          new GuiSlot(1, 113, 38, true),
          new GuiSlot(2, 113, 56, true));
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.CENTRIFUGE_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 3;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1, 0)
        );
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        
        return List.of(
          new Vec3i(0, 0, -1),
          new Vec3i(0, 0, 1)
        );
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new CentrifugeScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public int getAnimationDuration() {
        return 20 * 9;
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(@Nullable Direction direction) {
        if (!hasFluidAddon) return null;
        return fluidContainer;
    }
}
