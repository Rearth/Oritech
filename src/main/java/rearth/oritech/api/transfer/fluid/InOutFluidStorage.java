package rearth.oritech.api.transfer.fluid;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.networking.UpdatableField;
import rearth.oritech.api.transfer.SlotRangeResourceHandler;
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.List;

// in / out containers give fully unrestricted direct access to just the input or just the output slots.
// this is separate from the pipe-facing insert(index, ...)/extract(index, ...) below, which restrict
// insertion to input slots and extraction to output slots for externally exposed access.
public class InOutFluidStorage extends FluidStacksResourceHandler implements UpdatableField<Void, List<FluidStack>> {

    private final Runnable onUpdate;
    private final ContainerSlotAssignment slotAssignment;
    private final ResourceHandler<FluidResource> inputContainer;
    private final ResourceHandler<FluidResource> outputContainer;

    public InOutFluidStorage(int capacity, Runnable onUpdate, ContainerSlotAssignment slotAssignment) {
        super(2, capacity);
        this.onUpdate = onUpdate;
        this.slotAssignment = slotAssignment;

        inputContainer = new SlotRangeResourceHandler<>(this, slotAssignment.inputStart(), slotAssignment.inputCount(), this::rawInsert, this::rawExtract);
        outputContainer = new SlotRangeResourceHandler<>(this, slotAssignment.outputStart(), slotAssignment.outputCount(), this::rawInsert, this::rawExtract);
    }

    // externally exposed (e.g. pipe) access: insertion only allowed into input slots, extraction only from output slots
    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (!slotAssignment.isInput(index)) return 0;
        return super.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (!slotAssignment.isOutput(index)) return 0;
        return super.extract(index, resource, amount, transaction);
    }

    // raw, unrestricted index-based insert/extract - bypasses the restriction above.
    // backs the input/output sub-views used for internal logic (crafting, guis, checks, etc.)
    private int rawInsert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return super.insert(index, resource, amount, transaction);
    }

    private int rawExtract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return super.extract(index, resource, amount, transaction);
    }

    public ResourceHandler<FluidResource> getInputContainer() {
        return inputContainer;
    }

    public ResourceHandler<FluidResource> getOutputContainer() {
        return outputContainer;
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
