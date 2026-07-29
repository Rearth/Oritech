package rearth.oritech.block.entity.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.api.transfer.fluid.InOutFluidStorage;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.block.entity.addons.HeartOfTheMachineAddonEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.CentrifugeScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeInput;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.List;

public class CentrifugeBlockEntity extends MultiblockMachineEntity implements FluidProvider {

    @SyncField({SyncType.TICK, SyncType.GUI_TICK, SyncType.INITIAL, SyncType.GUI_OPEN})
    public final InOutFluidStorage fluidContainer = new InOutFluidStorage((int) (OritechConfig.processingMachines.centrifugeData.tankSizeInBuckets.get() * FluidType.BUCKET_VOLUME), this::setChanged, new ContainerSlotAssignment(0, 1, 1, 1));

    @SyncField({SyncType.GUI_OPEN, SyncType.INITIAL})
    public boolean hasFluidAddon = false;

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.CENTRIFUGE.get(), pos, state, OritechConfig.processingMachines.centrifugeData.energyPerTick.get());
    }

    @Override
    public long getDefaultCapacity() {
        return OritechConfig.processingMachines.centrifugeData.energyCapacity.get();
    }

    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.processingMachines.centrifugeData.maxEnergyInsertion.get();
    }

    @Override
    protected OritechRecipe loadRecipeFromInput(ServerLevel serverLevel, OritechRecipeInput recipeInput, RecipeType<OritechRecipe> type) {

        // try fluid first if applicable
        if (hasFluidAddon) {
            var fluidInput = getFluidRecipeInput();
            var candidate = super.loadRecipeFromInput(serverLevel, fluidInput, RecipeContent.CENTRIFUGE_FLUID.get());
            if (!candidate.isEmpty()) return candidate;
        }

        return super.loadRecipeFromInput(serverLevel, recipeInput, type);
    }

    protected OritechRecipeInput getFluidRecipeInput() {
        return new OritechRecipeInput(getInputView(), fluidContainer.getInStack());
    }

    @Override
    public boolean canOutputRecipe(OritechRecipe recipe) {
        var itemsMatch = super.canOutputRecipe(recipe);
        if (!itemsMatch || !hasFluidAddon) return itemsMatch;

        var outputInventory = fluidContainer.getOutputContainer();

        try (var simulated = Transaction.openRoot()) {
            for (var result : recipe.fluidOutputs()) {
                var inserted = outputInventory.insert(FluidResource.of(result), result.amount(), simulated);
                if (inserted != result.amount()) return false;
            }
        }

        return true;
    }

    @Override
    protected boolean removeCraftingInputs(Transaction transaction) {

        var fluidInput = currentRecipe.fluidInput();
        if (fluidInput.isPresent()) {
            // we assume that the fluid content matches here, as this was checked in earlier steps already
            var extracted = fluidContainer.getInputContainer().extract(FluidResource.of(fluidContainer.getInStack()), fluidInput.get().amount(), transaction);
            if (extracted != fluidInput.get().amount()) return false;
        }

        return super.removeCraftingInputs(transaction);
    }

    @Override
    protected boolean createCraftingOutputs(Transaction transaction) {

        for (var fluidOutput : currentRecipe.fluidOutputs()) {
            var inserted = fluidContainer.getOutputContainer().insert(FluidResource.of(fluidOutput), fluidOutput.amount(), transaction);
            if (inserted != fluidOutput.amount()) return false;
        }

        return super.createCraftingOutputs(transaction);
    }

    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_FLUID_ADDON.get()) || addonBlock.addonEntity() instanceof HeartOfTheMachineAddonEntity combi && combi.hasFluid()) {
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
            var coreCandidate = level.getBlockEntity(worldPosition.above(), BlockEntitiesContent.MACHINE_CORE.get());
            if (coreCandidate.isPresent()) {
                var core = coreCandidate.get();
                core.resetCaches();
            }

            level.invalidateCapabilities(worldPosition);
            level.invalidateCapabilities(worldPosition.above());

            // trigger block update to allow pipes to connect/disconnect
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.updateNeighborsAt(worldPosition.above(), level.getBlockState(worldPosition.above()).getBlock());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("fluidAddon", hasFluidAddon);
        fluidContainer.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        hasFluidAddon = input.getBooleanOr("fluidAddon", false);
        fluidContainer.deserialize(input);
    }

    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.CENTRIFUGE.get();
    }

    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 1, 1, 2);
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
        return ModScreens.CENTRIFUGE_SCREEN.get();
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
    public ResourceHandler<FluidResource> getFluidLookup(@Nullable Direction direction) {
        if (!hasFluidAddon) return null;
        return fluidContainer.getExternalAccess();
    }

    @Override
    public List<ResourceHandler<FluidResource>> getInteractableFluidStorages() {
        if (!hasFluidAddon) return List.of();
        return List.of(fluidContainer.getInputContainer(), fluidContainer.getOutputContainer());
    }
}
