package rearth.oritech.neoforge;

import com.google.auto.service.AutoService;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.item.BlockItemApi;
import rearth.oritech.util.StackContext;
import rearth.oritech.api.fluid.BlockFluidApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.FluidApi.FluidStorage;
import rearth.oritech.api.fluid.ItemFluidApi;
import rearth.oritech.api.fluid.containers.DelegatingFluidStorage;
import rearth.oritech.api.fluid.containers.SimpleItemFluidStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@AutoService({BlockFluidApi.class, ItemFluidApi.class})
public class NeoforgeFluidApiImpl implements BlockFluidApi, ItemFluidApi {
    
    private final List<Supplier<BlockEntityType<?>>> registeredBlockEntities = new ArrayList<>();
    private final List<Supplier<Item>> registeredItems = new ArrayList<>();
    
    @Override
    public void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier) {
        registeredBlockEntities.add(typeSupplier);
    }
    
    @SuppressWarnings("IfCanBeSwitch")
    public void registerEvent(RegisterCapabilitiesEvent event) {
        for (var supplied : registeredBlockEntities) {
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, supplied.get(), (entity, direction) -> {
                
                var storage = ((FluidApi.BlockProvider) entity).getFluidStorage(direction);
                
                if (storage == null) return null;
                
                if (storage instanceof FluidApi.MultiSlotStorage inOutContainer) {
                    return MultiSlotStorageWrapper.of(inOutContainer);
                } else if (storage instanceof FluidApi.SingleSlotStorage singleContainer) {
                    return SingleSlotContainerStorageWrapper.of(singleContainer);
                } else if (storage instanceof DelegatingFluidStorage delegatingFluidStorage) {
                    return new DelegatingContainerStorageWrapper(delegatingFluidStorage);
                }
                
                Oritech.LOGGER.error("Error during fluid provider registration, unable to register a fluid container");
                Oritech.LOGGER.error("Erroring container type is: {}", entity);
                
                return null;
            });
        }
        
        
        for (var supplied : registeredItems) {
            event.registerItem(Capabilities.FluidHandler.ITEM,
              (stack, ignored) -> FluidContainerItemWrapper.of(((FluidApi.ItemProvider) stack.getItem()).getFluidStorage(stack), stack),
              supplied.get());
        }
    }
    
    @Override
    public void registerForItem(Supplier<Item> itemSupplier) {
        registeredItems.add(itemSupplier);
    }
    
    @Override
    public FluidApi.FluidStorage find(Level world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        var candidate = world.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, entity, direction);
        return switch (candidate) {
            case null -> null;
            case SingleSlotContainerStorageWrapper wrapper -> wrapper.container;
            case MultiSlotStorageWrapper wrapper -> wrapper.container;
            default -> new NeoforgeStorageWrapper(candidate);
        };
    }
    
    @Override
    public FluidApi.FluidStorage find(Level world, BlockPos pos, @Nullable Direction direction) {
        return find(world, pos, null, null, direction);
    }
    
    @Override
    public FluidApi.FluidStorage find(StackContext stack) {
        if (stack.getValue().getCount() > 1) return null;
        var candidate = stack.getValue().getCapability(Capabilities.FluidHandler.ITEM);
        if (candidate == null) return null;
        if (candidate instanceof SingleSlotContainerStorageWrapper wrapper && wrapper.container instanceof SimpleItemFluidStorage itemContainer) return itemContainer.withCallback(ignored -> stack.sync());
        return new NeoforgeItemStorageWrapper(candidate, stack);
    }
    
    // used to interact with tanks from other mods
    public static class NeoforgeStorageWrapper extends FluidApi.FluidStorage {
        
        private final IFluidHandler storage;
        
        public NeoforgeStorageWrapper(IFluidHandler storage) {
            this.storage = storage;
        }
        
        @Override
        public long insert(FluidStack toInsert, boolean simulate) {
            var action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
            return storage.fill(FluidStackHooksForge.toForge(toInsert), action);
        }
        
        @Override
        public long extract(FluidStack toExtract, boolean simulate) {
            var action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
            return storage.drain(FluidStackHooksForge.toForge(toExtract), action).getAmount();
        }
        
        @Override
        public List<FluidStack> getContent() {
            
            var content = new ArrayList<FluidStack>();
            for (int i = 0; i < storage.getTanks(); i++) {
                var tank = storage.getFluidInTank(i);
                content.add(FluidStackHooksForge.fromForge(tank));
            }
            
            return content;
        }
        
        @Override
        public void update() {
        }
        
        @Override
        public long getCapacity() {
            Oritech.LOGGER.warn("tried to access capacity of external container");
            return 0L;
        }
    }
    
    // used to interact with items from other mods
    public static class NeoforgeItemStorageWrapper extends NeoforgeStorageWrapper {
        
        private final StackContext stack;
        private final IFluidHandlerItem handler;
        
        public NeoforgeItemStorageWrapper(IFluidHandlerItem storage, StackContext stack) {
            super(storage);
            this.stack = stack;
            this.handler = storage;
        }
        
        @Override
        public void update() {
            super.update();
            stack.setValue(handler.getContainer());
            stack.sync();
        }
    }
    
    // this is used by other mods to interact with the oritech fluid containers
    public static class SingleSlotContainerStorageWrapper implements IFluidHandler {
        
        public final FluidApi.SingleSlotStorage container;
        
        public static SingleSlotContainerStorageWrapper of(@Nullable FluidApi.SingleSlotStorage container) {
            if (container == null) return null;
            return new SingleSlotContainerStorageWrapper(container);
        }
        
        public SingleSlotContainerStorageWrapper(FluidApi.SingleSlotStorage container) {
            this.container = container;
        }
        
        
        @Override
        public int getTanks() {
            return 1;
        }
        
        @Override
        public net.neoforged.neoforge.fluids.@NotNull FluidStack getFluidInTank(int i) {
            return FluidStackHooksForge.toForge(container.getStack());
        }
        
        @Override
        public int getTankCapacity(int i) {
            return (int) container.getCapacity();
        }
        
        @Override
        public boolean isFluidValid(int i, net.neoforged.neoforge.fluids.@NotNull FluidStack fluidStack) {
            return true;
        }
        
        @Override
        public int fill(net.neoforged.neoforge.fluids.@NotNull FluidStack fluidStack, @NotNull FluidAction fluidAction) {
            
            var result = (int) container.insert(FluidStackHooksForge.fromForge(fluidStack), fluidAction.simulate());
            
            if (result > 0 && fluidAction.execute())
                container.update();
            
            return result;
        }
        
        @Override
        public net.neoforged.neoforge.fluids.@NotNull FluidStack drain(net.neoforged.neoforge.fluids.@NotNull FluidStack fluidStack, @NotNull FluidAction fluidAction) {
            var extractedAmount = container.extract(FluidStackHooksForge.fromForge(fluidStack), fluidAction.simulate());
            
            if (extractedAmount > 0 && fluidAction.execute())
                container.update();
            
            return new net.neoforged.neoforge.fluids.FluidStack(fluidStack.getFluid(), (int) extractedAmount);
        }
        
        @Override
        public net.neoforged.neoforge.fluids.@NotNull FluidStack drain(int i, @NotNull FluidAction fluidAction) {
            var extractedAmount =  container.extract(container.getStack().copyWithAmount(i), fluidAction.simulate());
            
            if (extractedAmount > 0 && fluidAction.execute())
                container.update();
            
            return new net.neoforged.neoforge.fluids.FluidStack(container.getStack().getFluid(), (int) extractedAmount);
        }
    }
    
    // this is used by other mods to access an oritech in/out container
    public static class MultiSlotStorageWrapper implements IFluidHandler {
        
        public final FluidApi.MultiSlotStorage container;
        
        public static MultiSlotStorageWrapper of(FluidApi.MultiSlotStorage container) {
            if (container == null) return null;
            return new MultiSlotStorageWrapper(container);
        }
        
        public MultiSlotStorageWrapper(FluidApi.MultiSlotStorage container) {
            this.container = container;
        }
        
        @Override
        public int getTanks() {
            return container.getSlotCount();
        }
        
        @Override
        public net.neoforged.neoforge.fluids.@NotNull FluidStack getFluidInTank(int i) {
            return FluidStackHooksForge.toForge(container.getStack(i));
        }
        
        @Override
        public int getTankCapacity(int i) {
            return (int) container.getCapacity();
        }
        
        @Override
        public boolean isFluidValid(int i, net.neoforged.neoforge.fluids.@NotNull FluidStack fluidStack) {
            return true;
        }
        
        @Override
        public int fill(net.neoforged.neoforge.fluids.@NotNull FluidStack fluidStack, FluidAction fluidAction) {
            var result = (int) container.insert(FluidStackHooksForge.fromForge(fluidStack), fluidAction.simulate());
            
            if (result > 0 && fluidAction.execute())
                container.update();
            
            return result;
        }
        
        @Override
        public net.neoforged.neoforge.fluids.@NotNull FluidStack drain(net.neoforged.neoforge.fluids.@NotNull FluidStack fluidStack, @NotNull FluidAction fluidAction) {
            var extractedAmount = container.extract(FluidStackHooksForge.fromForge(fluidStack), fluidAction.simulate());
            
            if (extractedAmount > 0 && fluidAction.execute())
                container.update();
            
            return new net.neoforged.neoforge.fluids.FluidStack(fluidStack.getFluid(), (int) extractedAmount);
        }
        
        @Override
        public net.neoforged.neoforge.fluids.@NotNull FluidStack drain(int i, @NotNull FluidAction fluidAction) {
            var extractedAmount =  container.extract(container.getStack(i).copyWithAmount(i), fluidAction.simulate());
            
            if (extractedAmount > 0 && fluidAction.execute())
                container.update();
            
            return new net.neoforged.neoforge.fluids.FluidStack(container.getStack(i).getFluid(), (int) extractedAmount);
        }
    }
    
    public static class DelegatingContainerStorageWrapper implements IFluidHandler {
        
        private final DelegatingFluidStorage container;
        
        public static DelegatingContainerStorageWrapper of(DelegatingFluidStorage container) {
            if (container == null) return null;
            return new DelegatingContainerStorageWrapper(container);
        }
        
        private DelegatingContainerStorageWrapper(DelegatingFluidStorage container) {
            this.container = container;
        }
        
        @Override
        public int getTanks() {
            return container.getContent().size();
        }
        
        @Override
        public net.neoforged.neoforge.fluids.@NotNull FluidStack getFluidInTank(int i) {
            return FluidStackHooksForge.toForge(container.getContent().get(i));
        }
        
        @Override
        public int getTankCapacity(int i) {
            return (int) container.getCapacity();
        }
        
        @Override
        public boolean isFluidValid(int i, net.neoforged.neoforge.fluids.@NotNull FluidStack fluidStack) {
            return true;
        }
        
        @Override
        public int fill(net.neoforged.neoforge.fluids.@NotNull FluidStack fluidStack, FluidAction fluidAction) {
            var result = (int) container.insert(FluidStackHooksForge.fromForge(fluidStack), fluidAction.simulate());
            
            if (result > 0 && fluidAction.execute())
                container.update();
            
            return result;
        }
        
        @Override
        public net.neoforged.neoforge.fluids.@NotNull FluidStack drain(net.neoforged.neoforge.fluids.@NotNull FluidStack fluidStack, @NotNull FluidAction fluidAction) {
            var extractedAmount = container.extract(FluidStackHooksForge.fromForge(fluidStack), fluidAction.simulate());
            
            if (extractedAmount > 0 && fluidAction.execute())
                container.update();
            
            return new net.neoforged.neoforge.fluids.FluidStack(fluidStack.getFluid(), (int) extractedAmount);
        }
        
        @Override
        public net.neoforged.neoforge.fluids.@NotNull FluidStack drain(int i, @NotNull FluidAction fluidAction) {
            
            if (container.getContent() == null || container.getContent().isEmpty()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            
            var extractedAmount =  container.extract(container.getContent().getLast().copyWithAmount(i), fluidAction.simulate());
            
            if (extractedAmount > 0 && fluidAction.execute())
                container.update();
            
            return new net.neoforged.neoforge.fluids.FluidStack(container.getContent().getLast().getFluid(), (int) extractedAmount);
        }
    }
    
    public static class FluidContainerItemWrapper extends SingleSlotContainerStorageWrapper implements IFluidHandlerItem {
        
        private final ItemStack stack;
        
        public static FluidContainerItemWrapper of(FluidApi.SingleSlotStorage container, ItemStack stack) {
            if (container == null || stack == null || stack.isEmpty()) return null;
            return new FluidContainerItemWrapper(container, stack);
        }
        
        public FluidContainerItemWrapper(FluidApi.SingleSlotStorage container, ItemStack stack) {
            super(container);
            this.stack = stack;
        }
        
        @Override
        public @NotNull ItemStack getContainer() {
            return stack;
        }
        
    }
}
