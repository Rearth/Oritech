package rearth.oritech.api.transfer.fluid;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.networking.UpdatableField;

// simple one-stack fluid storage
public class SimpleFluidStorage extends FluidStacksResourceHandler implements UpdatableField<Void, FluidStack> {
    
    private final Runnable onUpdate;
    
    public SimpleFluidStorage(int capacity, Runnable onUpdate) {
        super(1, capacity);
        this.onUpdate = onUpdate;
    }
    
    public FluidStack getContent() {
        return this.stacks.get(0);
    }
    
    public long getCapacity() {
        return super.capacity;
    }
    
    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        super.onContentsChanged(index, previousContents);
        onUpdate.run();
    }
    
    @Override
    public FluidStack getDeltaData() {
        return this.stacks.get(0);
    }
    
    @Override
    public Void getFullData() {
        return null;
    }
    
    @Override
    public StreamCodec<? extends ByteBuf, FluidStack> getDeltaCodec() {
        return FluidStack.OPTIONAL_STREAM_CODEC;
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
    public void handleDeltaUpdate(FluidStack updatedData) {
        this.set(0, FluidResource.of(updatedData), updatedData.amount());
    }
}
