package rearth.oritech.block.entity.processing;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.CentrifugeScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.fluid.FluidApi;
import rearth.oritech.util.fluid.containers.SimpleInOutFluidStorage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CentrifugeBlockEntity extends MultiblockMachineEntity implements FluidApi.BlockProvider {
    
    public final SimpleInOutFluidStorage fluidContainer = new SimpleInOutFluidStorage(Oritech.CONFIG.processingMachines.centrifugeData.tankSizeInBuckets() * FluidStackHooks.bucketAmount(), this::markDirty);
    
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
        var output = recipe.getFluidOutput();
        if (output != null && !output.isEmpty()) { // only verify fluid output if fluid output exists
            
            if (fluidContainer.getOutStack().getAmount() + output.getAmount() > fluidContainer.getCapacity())
                return false; // output too full
            
            if (!fluidContainer.getOutStack().isEmpty() && !output.isFluidEqual(fluidContainer.getOutStack()))
                return false;   // output type mismatch
        }
        
        return true;
        
    }
    
    @Override
    protected Optional<RecipeEntry<OritechRecipe>> getRecipe() {
        
        if (!hasFluidAddon)
            return super.getRecipe();
        
        // get recipes matching input items
        var candidates = Objects.requireNonNull(world).getRecipeManager().getAllMatches(getOwnRecipeType(), getInputInventory(), world);
        // filter out recipes based on input tank
        var fluidRecipe = candidates.stream().filter(candidate -> recipeInputMatchesTank(fluidContainer.getInStack(), candidate.value())).findAny();
        if (fluidRecipe.isPresent()) {
            return fluidRecipe;
        }
        
        return getNormalRecipe();
    }
    
    // this is provided as fallback for fluid centrifuges that may still process normal stuff
    private Optional<RecipeEntry<OritechRecipe>> getNormalRecipe() {
        return world.getRecipeManager().getFirstMatch(RecipeContent.CENTRIFUGE, getInputInventory(), world);
    }
    
    public static boolean recipeInputMatchesTank(FluidStack available, OritechRecipe recipe) {
        
        var recipeNeedsFluid = recipe.getFluidInput() != null && recipe.getFluidInput().getAmount() > 0;
        if (!recipeNeedsFluid) return true;
        
        var isTankEmpty = available.isEmpty();
        if (isTankEmpty) return false;
        
        var recipeFluid = recipe.getFluidInput();
        return recipeFluid.isFluidEqual(available) && available.getAmount() >= recipe.getFluidInput().getAmount();
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
        var output = activeRecipe.getFluidOutput();
        
        if (input != null && input.getAmount() > 0)
            fluidContainer.getInputContainer().extract(input, false);
        if (output != null && output.getAmount() > 0)
            fluidContainer.getOutputContainer().insert(output, false);
        
    }
    
    @Override
    public void initAddons() {
        super.initAddons();
        // trigger block update to allow pipes to connect
        world.updateNeighbors(pos, getCachedState().getBlock());
        world.updateNeighbors(pos.up(), world.getBlockState(pos.up()).getBlock());
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
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("fluidAddon", hasFluidAddon);
        fluidContainer.writeNbt(nbt, "");
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        
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
    public ScreenHandlerType<?> getScreenHandlerType() {
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
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CentrifugeScreenHandler(syncId, playerInventory, this, getUiData(), getCoreQuality());
    }
    
    @Override
    public int getAnimationDuration() {
        return 20 * 9;
    }
    
    @Override
    protected void sendNetworkEntry() {
        super.sendNetworkEntry();
        
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(
          new NetworkContent.CentrifugeFluidSyncPacket(
            pos,
            hasFluidAddon,
            Registries.FLUID.getId(fluidContainer.getInStack().getFluid()).toString(),
            fluidContainer.getInStack().getAmount(),
            Registries.FLUID.getId(fluidContainer.getOutStack().getFluid()).toString(),
            fluidContainer.getOutStack().getAmount()));
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(@Nullable Direction direction) {
        return fluidContainer.getStorageForDirection(direction);
    }
}
