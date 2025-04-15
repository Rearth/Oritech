package rearth.oritech.util.fluid.containers;

import dev.architectury.fluid.FluidStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.util.fluid.FluidApi;

import java.util.List;
import java.util.function.Consumer;

public class SimpleInOutFluidStorage extends FluidApi.InOutSlotStorage {
    
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
    
    public SimpleInOutFluidStorage(Long capacity) {
        this(capacity, () -> {
        });
    }
    
    @Override
    public void setInStack(FluidStack stack) {
        contentIn = stack;
    }
    
    @Override
    public FluidStack getInStack() {
        return contentIn;
    }
    
    @Override
    public void setOutStack(FluidStack stack) {
        contentOut = stack;
    }
    
    @Override
    public FluidStack getOutStack() {
        return contentOut;
    }
    
    @Override
    public long getCapacity() {
        return capacity;
    }
    
    @Override
    public long insert(FluidStack toInsert, boolean simulate) {
        return insertTo(toInsert, simulate, capacity, contentIn, this::setInStack);
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
        
        if (direction == null) return this;
        
        if (direction.equals(Direction.UP)) return inputContainer;
        if (direction.equals(Direction.DOWN)) return outputContainer;
        return this;
    }
    
    public void writeNbt(NbtCompound nbt, String suffix) {
        FluidStack.CODEC.encodeStart(NbtOps.INSTANCE, contentIn).result().ifPresent(tag -> nbt.put("fluidin" + suffix, tag));
        FluidStack.CODEC.encodeStart(NbtOps.INSTANCE, contentOut).result().ifPresent(tag -> nbt.put("fluidout" + suffix, tag));
    }
    
    public void readNbt(NbtCompound nbt, String suffix) {
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
}
