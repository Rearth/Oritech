package rearth.oritech.api.transfer.fluid;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.networking.UpdatableField;
import rearth.oritech.api.transfer.SlotRangeResourceHandler;
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.List;

// this storage itself is fully unrestricted (like a plain tank): any slot can be inserted into or
// extracted from directly. That's what's needed for the machine's own internal logic and the GUI.
// externally exposed access (e.g. to pipes) should go through getExternalAccess() instead, which
// restricts insertion to input slots and extraction to output slots.
public class InOutFluidStorage extends FluidStacksResourceHandler implements UpdatableField<Void, List<FluidStack>> {

    private final Runnable onUpdate;
    private final ResourceHandler<FluidResource> inputContainer;
    private final ResourceHandler<FluidResource> outputContainer;
    private final ResourceHandler<FluidResource> externalAccess;

    public InOutFluidStorage(int capacity, Runnable onUpdate, ContainerSlotAssignment slotAssignment) {
        super(2, capacity);
        this.onUpdate = onUpdate;

        inputContainer = new SlotRangeResourceHandler<>(this, slotAssignment.inputStart(), slotAssignment.inputCount(), this::insert, this::extract);
        outputContainer = new SlotRangeResourceHandler<>(this, slotAssignment.outputStart(), slotAssignment.outputCount(), this::insert, this::extract);

        externalAccess = new DelegatingResourceHandler<>(this) {
            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isInput(index)) return 0;
                return super.insert(index, resource, amount, transaction);
            }

            @Override
            public int insert(FluidResource resource, int amount, TransactionContext transaction) {
                var inserted = 0;
                var end = slotAssignment.inputStart() + slotAssignment.inputCount();
                for (int index = slotAssignment.inputStart(); index < end && inserted < amount; index++) {
                    inserted += insert(index, resource, amount - inserted, transaction);
                }
                return inserted;
            }

            @Override
            public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isOutput(index)) return 0;
                return super.extract(index, resource, amount, transaction);
            }

            @Override
            public int extract(FluidResource resource, int amount, TransactionContext transaction) {
                var extracted = 0;
                var end = slotAssignment.outputStart() + slotAssignment.outputCount();
                for (int index = slotAssignment.outputStart(); index < end && extracted < amount; index++) {
                    extracted += extract(index, resource, amount - extracted, transaction);
                }
                return extracted;
            }
        };
    }

    public ResourceHandler<FluidResource> getInputContainer() {
        return inputContainer;
    }

    public ResourceHandler<FluidResource> getOutputContainer() {
        return outputContainer;
    }

    // restricted view for externally exposed access (e.g. pipes): insertion only allowed into
    // input slots, extraction only from output slots.
    public ResourceHandler<FluidResource> getExternalAccess() {
        return externalAccess;
    }

    public FluidStack getInStack() {
        return stacks.get(0);
    }

    public FluidStack getOutStack() {
        return stacks.get(1);
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        super.onContentsChanged(index, previousContents);
        onUpdate.run();
    }

    @Override
    public List<FluidStack> getDeltaData() {
        return stacks;
    }

    @Override
    public Void getFullData() {
        return null;
    }

    @Override
    public StreamCodec<? extends ByteBuf, List<FluidStack>> getDeltaCodec() {
        return FluidStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list());
    }

    @Override
    public StreamCodec<? extends ByteBuf, Void> getFullCodec() {
        return null;
    }

    @Override
    public boolean useDeltaOnly(SyncType type) {
        return true;
    }

    @Override
    public void handleFullUpdate(Void updatedData) {

    }

    public int getCapacity() {
        return this.capacity;
    }

    @Override
    public void handleDeltaUpdate(List<FluidStack> updatedData) {
        this.setStacks(NonNullList.copyOf(updatedData));
    }

    public void serialize(ValueOutput output) {
        output.store("fluids", FluidStack.OPTIONAL_CODEC.listOf(), stacks);
    }

    public void deserialize(ValueInput input) {
        input.read("fluids", FluidStack.OPTIONAL_CODEC.listOf()).ifPresent(this::handleDeltaUpdate);
    }
}
