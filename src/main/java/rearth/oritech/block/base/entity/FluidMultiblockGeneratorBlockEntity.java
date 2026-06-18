package rearth.oritech.block.base.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.api.transfer.fluid.SimpleFluidStorage;
import rearth.oritech.init.recipes.OritechRecipeInput;
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.List;

public abstract class FluidMultiblockGeneratorBlockEntity extends MultiblockGeneratorBlockEntity implements FluidProvider {

    @SyncField
    public final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(4 * 1000, this::setChanged) {

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.is(Fluids.WATER)) return 0;
            return super.insert(index, resource, amount, transaction); // to avoid mixups with players inserting water for boiler into main storage (which should contain oil/lava/whatever)
        }
    };

    public FluidMultiblockGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }

    @Override
    protected OritechRecipeInput getRecipeInput() {
        return new OritechRecipeInput(getInputView(), fluidStorage.getContent());
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {

        if (bucketInputAllowed() && !serverLevel.isClientSide() && isAssembled(state)) {
            processBuckets();
        }

        super.serverTick(serverLevel, pos, state, blockEntity);
    }

    // tries to load content from buckets / fluid containers
    private void processBuckets() {

        var inStorage = inventory.getInputContainer();
        var canFill = this.fluidStorage.getContent().amount() < this.fluidStorage.getCapacity();

        if (!canFill) return;

        try (var transaction = Transaction.openRoot()) {

            var inResource = inStorage.getResource(0);
            var inStack = inResource.toStack();

            var candidate = inStack.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forHandlerIndexStrict(inStorage, 0));
            if (candidate == null) return;

            var resource = candidate.getResource(0);
            if (resource.isEmpty()) return;

            var maxTaken = Math.min(1000, fluidStorage.getCapacity() - fluidStorage.getAmountAsLong(0));
            var taken = candidate.extract(0, resource, (int) maxTaken, transaction);
            if (taken <= 0) return;

            var inserted = fluidStorage.insert(resource, taken, transaction);
            if (inserted == taken) {
                transaction.commit();
            }
        }

    }

    @Override
    protected boolean removeRecipeInputs(Transaction transaction) {
        var itemsTaken = super.removeRecipeInputs(transaction);
        if (!itemsTaken) return false;

        // we assume the fluid matches the ingredient, so we just remove the needed amount;
        var fluidInput = currentRecipe.fluidInput().get();
        var taken = fluidStorage.extract(0, fluidStorage.getResource(0), fluidInput.amount(), transaction);
        return taken == fluidInput.amount();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fluidStorage.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluidStorage.deserialize(input);
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
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 1, 1, 1);
    }

    public boolean bucketInputAllowed() {
        return true;
    }

    @Override
    public int getInventorySize() {
        return 2;
    }

    @Override
    public ResourceHandler<FluidResource> getFluidLookup(@Nullable Direction direction) {
        return fluidStorage;
    }
}
