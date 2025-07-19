package rearth.oritech.api.fluid.containers;

import dev.architectury.fluid.FluidStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.component.ComponentChanges;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.PacketCodec;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.networking.UpdatableField;

import java.util.List;

public class SimpleFluidStorage extends FluidApi.SingleSlotStorage implements UpdatableField<Void, FluidStack> {
    
    public static Long transfer(SimpleFluidStorage from, SimpleFluidStorage to, long maxAmount, boolean simulate) {
        
        var extracted = from.extract(FluidStack.create(from.getFluid(), maxAmount, from.getChanges()), true);   // check how much we could extract at most
        var inserted = to.insert(FluidStack.create(from.getFluid(), extracted, from.getChanges()), simulate);   // insert max extraction amount
        extracted = from.extract(FluidStack.create(from.getFluid(), inserted, from.getChanges()), simulate);    // extract only how much was actually inserted
        
        if (extracted > 0 && !simulate) {
            from.update();
            to.update();
        }
        
        return extracted;
    }
    
    private FluidStack content;
    private final Long capacity;
    private final Runnable onUpdate;
    
    public SimpleFluidStorage(Long capacity, Runnable onUpdate) {
        this.capacity = capacity;
        this.onUpdate = onUpdate;
        this.content = FluidStack.create(getEmptyVariant(), 0);
    }
    
    public Fluid getEmptyVariant() {
        return Fluids.EMPTY;
    }
    
    public SimpleFluidStorage(Long capacity) {
        this(capacity, () -> {});
    }
    
    
    @Override
    public long insert(FluidStack toInsert, boolean simulate) {
        return SimpleInOutFluidStorage.insertTo(toInsert, simulate, capacity, content, stack -> this.content = stack);
    }
    
    @Override
    public long extract(FluidStack toExtract, boolean simulate) {
        return SimpleInOutFluidStorage.extractFrom(toExtract, simulate, content);
    }
    
    @Override
    public List<FluidStack> getContent() {
        return List.of(content);
    }
    
    public void writeNbt(NbtCompound nbt, String suffix) {
        FluidStack.CODEC.encodeStart(NbtOps.INSTANCE, content).result().ifPresent(tag -> nbt.put("fluid" + suffix, tag));
    }
    
    public void readNbt(NbtCompound nbt, String suffix) {
        content = FluidStack.CODEC.parse(NbtOps.INSTANCE, nbt.get("fluid" + suffix)).result().orElse(FluidStack.empty());
    }
    
    public void setAmount(long amount) {
        content.setAmount(amount);
    }
    
    public long getAmount() {
        return content.getAmount();
    }
    
    public void setFluid(Fluid fluid) {
        content = FluidStack.create(fluid, getAmount(), getChanges());
    }
    
    public Fluid getFluid() {
        return content.getFluid();
    }
    
    public void setChanges(ComponentChanges data) {
        content = FluidStack.create(getFluid(), getAmount(), data);
    }
    
    public ComponentChanges getChanges() {
        return content.getPatch();
    }
    
    @Override
    public long getCapacity() {
        return capacity;
    }
    
    @Override
    public void update() {
        onUpdate.run();
    }
    
    @Override
    public void setStack(FluidStack stack) {
        content = stack.copy();
    }
    
    @Override
    public FluidStack getStack() {
        return content;
    }
    
    @Override
    public FluidStack getDeltaData() {
        return this.content;
    }
    
    @Override
    public Void getFullData() {
        return null;
    }
    
    @Override
    public boolean useDeltaOnly(SyncType type) {
        return true;
    }
    
    @Override
    public PacketCodec<? extends ByteBuf, FluidStack> getDeltaCodec() {
        return NetworkManager.FLUID_STACK_STREAM_CODEC;
    }
    
    @Override
    public PacketCodec<? extends ByteBuf, Void> getFullCodec() {
        return null;
    }
    
    @Override
    public void handleFullUpdate(Void updatedData) { }
    
    @Override
    public void handleDeltaUpdate(FluidStack updatedData) {
        this.setStack(updatedData);
    }
}
