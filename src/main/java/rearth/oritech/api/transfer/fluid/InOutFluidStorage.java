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
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.List;

// sub-containers allow direct insert + extract access to each sub-container
public class InOutFluidStorage extends FluidStacksResourceHandler implements UpdatableField<Void, List<FluidStack>> {
    
    private final Runnable onUpdate;
    private final ContainerSlotAssignment slotAssignment;
    private final ResourceHandler<FluidResource> inputContainer;
    private final ResourceHandler<FluidResource> outputContainer;
    
    public InOutFluidStorage(int capacity, Runnable onUpdate, ContainerSlotAssignment slotAssignment) {
        super(1, capacity);
        this.onUpdate = onUpdate;
        this.slotAssignment = slotAssignment;
        
        inputContainer = new DelegatingResourceHandler<>(this) {
            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isInput(index)) return 0;
                return super.insert(index, resource, amount, transaction);
            }
            
            @Override
            public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isInput(index)) return 0;
                return super.extract(index, resource, amount, transaction);
            }
            
            @Override
            public int size() {
                return slotAssignment.inputCount();
            }
        };
        
        outputContainer = new DelegatingResourceHandler<>(this) {
            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isOutput(index)) return 0;
                return super.insert(index, resource, amount, transaction);
            }
            
            @Override
            public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isOutput(index)) return 0;
                return super.extract(index, resource, amount, transaction);
            }
            
            @Override
            public int size() {
                return slotAssignment.outputCount();
            }
        };
    }
    
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
    
    public ResourceHandler<FluidResource> getInputContainer() {
        return inputContainer;
    }
    
    public ResourceHandler<FluidResource> getOutputContainer() {
        return outputContainer;
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
