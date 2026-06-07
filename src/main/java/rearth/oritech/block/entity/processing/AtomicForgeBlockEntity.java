package rearth.oritech.block.entity.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.ArrayList;
import java.util.List;

public class AtomicForgeBlockEntity extends MultiblockMachineEntity {

    public AtomicForgeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ATOMIC_FORGE_ENTITY.get(), pos, state, OritechConfig.processingMachines.atomicForgeData.energyPerTick.get());
    }

    @Override
    protected float calculateEnergyUsage() {
        return energyStorage.capacity > 10 ? energyStorage.capacity : OritechConfig.processingMachines.atomicForgeData.energyPerTick.get();
    }

    @Override
    protected boolean checkCraftingFinished(OritechRecipe activeRecipe) {
        return progress.get() > 0;
    }

    @Override
    protected void resetProgress() {
        super.resetProgress();

        if (currentRecipe.isEmpty()) {
            energyStorage.setCapacity(1);
            energyStorage.set(0);
        } else {
            energyStorage.setCapacity((long) OritechConfig.processingMachines.atomicForgeData.energyPerTick.get() * currentRecipe.time());
        }
    }

    @Override
    public void updateEnergyContainer() {
    } // energy storage is updated by this class (based on the recipe amount), not the usual methods

    @Override
    public boolean canEnergyStorageChangeWhileGUIOpen() {
        return true;// tells the storage to always sync the full data to the client gui
    }

    @Override
    public BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(8, 7, 18, 71);
    }

    @Override
    public boolean showEnergyTransfer() {
        return false;
    }

    @Override
    public boolean showEnergyUsage() {
        return false;
    }

    @Override
    public float getProgress() {
        return (float) energyStorage.getAmountAsLong() / energyStorage.getCapacityAsLong();
    }

    @Override
    public long getDefaultCapacity() {
        return OritechConfig.processingMachines.atomicForgeData.energyCapacity.get();
    }

    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.processingMachines.atomicForgeData.maxEnergyInsertion.get();
    }

    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.ATOMIC_FORGE.get();
    }

    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 3, 3, 1);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 50, 36),
                new GuiSlot(1, 74, 17),
                new GuiSlot(2, 74, 55),
                new GuiSlot(3, 117, 36, true));
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.ATOMIC_FORGE_SCREEN.get();
    }

    @Override
    public int getInventorySize() {
        return 4;
    }

    @Override
    public DynamicEnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return null;
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
                new Vec3i(1, 0, 1),
                new Vec3i(1, 0, 0),
                new Vec3i(1, 0, -1),
                new Vec3i(0, 0, 1),
                new Vec3i(0, 0, -1),
                new Vec3i(-1, 0, 1),
                new Vec3i(-1, 0, 0),
                new Vec3i(-1, 0, -1)
        );
    }

    @Override
    public List<Vec3i> getAddonSlots() {
        return new ArrayList<>();
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
    }

    @Override
    public float getDisplayedEnergyTransfer() {
        return energyStorage.getCapacityAsLong();
    }

    @Override
    public float getDisplayedEnergyUsage() {
        return energyStorage.getCapacityAsLong();
    }
}
