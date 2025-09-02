package rearth.oritech.api.fluid.containers;

import dev.architectury.fluid.FluidStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.networking.UpdatableField;

import java.util.List;
import java.util.function.Consumer;

// a specific storage for a generic "one input slot -> one output slot".
// In is slot 0, out is slot 1.
public class SimpleInOutFluidStorage extends FluidApi.MultiSlotStorage implements UpdatableField<Void, Tuple<FluidStack, FluidStack>> {
    
    private FluidStack contentIn;
    private FluidStack contentOut;
    private final Long capacity;
    private final Runnable onUpdate;
    private final FluidApi.SingleSlotStorage inputContainer;
    private final FluidApi.SingleSlotStorage outputContainer;
    
    public SimpleInOutFluidStorage(Long capacity, Runnable onUpdate) {
        this.capacity = capacity;
        this.onUpdate = onUpdate;
        
        this.contentIn = FluidStack.empty();
        this.contentOut = FluidStack.empty();
        
        inputContainer = new FluidApi.SingleSlotStorage() {
            @Override
            public void setStack(FluidStack stack) {
                contentIn = stack;
            }
            
            @Override
            public FluidStack getStack() {
                return contentIn;
            }
            
            @Override
            public long getCapacity() {
                return capacity;
            }
            
            @Override
            public long insert(FluidStack toInsert, boolean simulate) {
                return insertTo(toInsert, simulate, capacity, contentIn, this::setStack);
            }
            
            @Override
            public long extract(FluidStack toExtract, boolean simulate) {
                return extractFrom(toExtract, simulate, contentIn);
            }
            
            @Override
            public List<FluidStack> getContent() {
                return List.of(contentIn);
            }
            
            @Override
            public void update() {
                onUpdate.run();
            }
        };
        outputContainer = new FluidApi.SingleSlotStorage() {
            @Override
            public void setStack(FluidStack stack) {
                contentOut = stack;
            }
            
            @Override
            public FluidStack getStack() {
                return contentOut;
            }
            
            @Override
            public long getCapacity() {
                return capacity;
            }
            
            @Override
            public long insert(FluidStack toInsert, boolean simulate) {
                return insertTo(toInsert, simulate, capacity, contentOut, this::setStack);
            }
            
            @Override
            public long extract(FluidStack toExtract, boolean simulate) {
                return extractFrom(toExtract, simulate, contentOut);
            }
            
            @Override
            public List<FluidStack> getContent() {
                return List.of(contentOut);
            }
            
            @Override
            public void update() {
                onUpdate.run();
            }
        };
    }
    
    @Override
    public long getCapacity() {
        return capacity;
    }
    
    @Override
    public long insert(FluidStack toInsert, boolean simulate) {
        return insertTo(toInsert, simulate, capacity, contentIn, stack -> setStack(0, stack));
    }
    
    @Override
    public long extract(FluidStack toExtract, boolean simulate) {
        return extractFrom(toExtract, simulate, contentOut);
    }
    
    @Override
    public List<FluidStack> getContent() {
        return List.of(contentIn, contentOut);
    }
    
    @Override
    public void update() {
        onUpdate.run();
    }
    
    public FluidApi.SingleSlotStorage getInputContainer() {
        return inputContainer;
    }
    
    public FluidApi.SingleSlotStorage getOutputContainer() {
        return outputContainer;
    }
    
    @Override
    public FluidApi.FluidStorage getStorageForDirection(@Nullable Direction direction) {
        return this;
    }
    
    @Override
    public void setStack(int slot, FluidStack stack) {
        if (slot == 0) {
            contentIn = stack;
        } else {
            contentOut = stack;
        }
    }
    
    @Override
    public FluidStack getStack(int slot) {
        if (slot == 0) {
            return contentIn;
        } else {
            return contentOut;
        }
    }
    
    @Override
    public int getSlotCount() {
        return 2;
    }
    
    public void writeNbt(CompoundTag nbt, String suffix) {
        FluidStack.CODEC.encodeStart(NbtOps.INSTANCE, contentIn).result().ifPresent(tag -> nbt.put("fluidin" + suffix, tag));
        FluidStack.CODEC.encodeStart(NbtOps.INSTANCE, contentOut).result().ifPresent(tag -> nbt.put("fluidout" + suffix, tag));
    }
    
    public void readNbt(CompoundTag nbt, String suffix) {
        contentIn = FluidStack.CODEC.parse(NbtOps.INSTANCE, nbt.get("fluidin" + suffix)).result().orElse(FluidStack.empty());
        contentOut = FluidStack.CODEC.parse(NbtOps.INSTANCE, nbt.get("fluidout" + suffix)).result().orElse(FluidStack.empty());
    }
    
    public static Long insertTo(FluidStack toInsert, boolean simulate, long capacity, FluidStack content, Consumer<FluidStack> setFunction) {
        
        if (toInsert.isEmpty()) return 0L;
        
        if (content.isEmpty()) {
            var inserted = Math.min(toInsert.getAmount(), capacity);
            if (!simulate)
                setFunction.accept(toInsert.copyWithAmount(inserted));
            return inserted;
        }
        
        if ((content.isFluidEqual(toInsert) && content.isComponentEqual(toInsert))) {
            // types match
            var inserted = Math.min(toInsert.getAmount(), capacity - content.getAmount());
            
            if (!simulate)
                content.grow(inserted);
            
            return inserted;
            
        } else {
            return 0L;
        }
    }
    
    public static Long extractFrom(FluidStack toExtract, boolean simulate, FluidStack content) {
        
        if (content.isEmpty()) return 0L;
        
        if ((content.isFluidEqual(toExtract) && content.isComponentEqual(toExtract))) {
            // types match
            var extracted = Math.min(toExtract.getAmount(), content.getAmount());
            
            if (!simulate)
                content.shrink(extracted);
            
            return extracted;
            
        } else {
            return 0L;
        }
    }
    
    public FluidStack getInStack() {
        return contentIn;
    }
    
    public FluidStack getOutStack() {
        return contentOut;
    }
    
    @Override
    public FluidApi.FluidStorage getStorageForSlot(int slot) {
        return slot == 0 ? inputContainer : outputContainer;
    }
    
    @Override
    public Tuple<FluidStack, FluidStack> getDeltaData() {
        return new Tuple<>(contentIn, contentOut);
    }
    
    @Override
    public Void getFullData() {
        return null;
    }
    
    @Override
    public StreamCodec<? extends ByteBuf, Tuple<FluidStack, FluidStack>> getDeltaCodec() {
        return new StreamCodec<>() {
            @Override
            public Tuple<FluidStack, FluidStack> decode(ByteBuf buf) {
                if (buf instanceof RegistryFriendlyByteBuf registryByteBuf) {
                    var left = NetworkManager.FLUID_STACK_STREAM_CODEC.decode(registryByteBuf);
                    var right = NetworkManager.FLUID_STACK_STREAM_CODEC.decode(registryByteBuf);
                    return new Tuple<>(left, right);
                } else {
                    Oritech.LOGGER.error("Trying to decode storage data to non-registry buf! {}", buf);
                    return new Tuple<>(FluidStack.empty(), FluidStack.empty());
                }
            }
            
            @Override
            public void encode(ByteBuf buf, Tuple<FluidStack, FluidStack> value) {
                if (buf instanceof RegistryFriendlyByteBuf registryByteBuf) {
                    NetworkManager.FLUID_STACK_STREAM_CODEC.encode(registryByteBuf, value.getA());
                    NetworkManager.FLUID_STACK_STREAM_CODEC.encode(registryByteBuf, value.getB());
                } else {
                    Oritech.LOGGER.error("Trying to encode storage data to non-registry buf! {} in {}", buf, value);
                }
            }
        };
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
    public void handleDeltaUpdate(Tuple<FluidStack, FluidStack> updatedData) {
        this.contentIn = updatedData.getA();
        this.contentOut = updatedData.getB();
    }
}
