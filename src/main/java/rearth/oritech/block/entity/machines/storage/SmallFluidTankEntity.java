package rearth.oritech.block.entity.machines.storage;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.FluidProvider;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.InventoryProvider;
import rearth.oritech.util.ScreenProvider;

import java.util.List;

public class SmallFluidTankEntity extends BlockEntity implements FluidProvider, InventoryProvider, ScreenProvider, ExtendedScreenHandlerFactory, BlockEntityTicker<SmallFluidTankEntity> {
    
    private boolean netDirty = false;
    
    protected final SimpleInventory inventory = new SimpleInventory(2) {
        @Override
        public void markDirty() {
            SmallFluidTankEntity.this.markDirty();
        }
    };
    
    private final InventoryStorage inventoryStorage = InventoryStorage.of(inventory, null);
    private final SingleVariantStorage<FluidVariant> fluidStorage = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }
        
        @Override
        protected long getCapacity(FluidVariant variant) {
            return (64 * FluidConstants.BUCKET);
        }
        
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            SmallFluidTankEntity.this.markDirty();
        }
    };
    
    public SmallFluidTankEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.SMALL_TANK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, SmallFluidTankEntity blockEntity) {
        // fill/drain buckets
        
        if (world.isClient) return;
        
        var inStack = inventory.getStack(0);
        var outStack = inventory.getStack(1);
        
        if (inStack != ItemStack.EMPTY && inStack.getItem().equals(Items.BUCKET) && fluidStorage.amount >= FluidConstants.BUCKET && outStack == ItemStack.EMPTY) {
            // try fill bucket
            var filledBucketType = fluidStorage.variant.getFluid().getBucketItem();
            if (filledBucketType == null) return;
            inStack.decrement(1);
            inventory.heldStacks.set(1, new ItemStack(filledBucketType));
            inventory.heldStacks.set(0, inStack);
            fluidStorage.amount -= FluidConstants.BUCKET;
            this.markDirty();
        } else if (inStack != ItemStack.EMPTY && inStack.getItem() instanceof BucketItem && !inStack.getItem().equals(Items.BUCKET) && outputCanAcceptBucket(outStack)) {
            // empty input bucket
            
            // weird voodoo because the transaction APIs are weird and I have NO idea what this all is
            var context = ContainerItemContext.ofSingleSlot(inventoryStorage.getSlot(0)).find(FluidStorage.ITEM);
            if (context == null) return;
            var variant = context.iterator().next().getResource();  // non empty iterator doesnt seem to do what it implies, so whatever
            if (variant == null) return;
            
            var bucketUsed = false;
            if ((fluidStorage.variant.isOf(variant.getFluid()) && fluidStorage.amount + FluidConstants.BUCKET <= fluidStorage.getCapacity())) {
                bucketUsed = true;
                fluidStorage.amount += FluidConstants.BUCKET;
            } else if (fluidStorage.amount == 0) {
                fluidStorage.variant = variant;
                fluidStorage.amount = FluidConstants.BUCKET;
                bucketUsed = true;
            }
            
            if (bucketUsed) {
                inventory.setStack(0, ItemStack.EMPTY);
                try (var tx = Transaction.openOuter()) {
                    var slot = inventoryStorage.getSlot(1);
                    slot.insert(ItemVariant.of(Items.BUCKET), 1, tx);
                    tx.commit();
                }
            }
            
            this.markDirty();
        }
        
        if (netDirty) {
            updateNetwork();
        }
        
    }
    
    private void updateNetwork() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.SingleVariantFluidSyncPacket(pos, Registries.FLUID.getId(fluidStorage.variant.getFluid()).toString(), fluidStorage.amount));
    }
    
    private boolean outputCanAcceptBucket(ItemStack slot) {
        if (slot == null) return true;
        if (slot.isEmpty()) return true;
        return slot.getItem().equals(Items.BUCKET) && slot.getCount() < slot.getMaxCount();
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        this.netDirty = true;
    }
    
    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.getPos());
    }
    
    @Override
    public Text getDisplayName() {
        return Text.of("invalid");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        this.markDirty();
        return new BasicMachineScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public InventoryStorage getInventory(Direction direction) {
        return inventoryStorage;
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 50, 40), new GuiSlot(1, 100, 40));
    }
    
    @Override
    public BarConfiguration getFluidConfiguration() {
        return new BarConfiguration(70, 18, 21, 60);
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return 0;
    }
    
    @Override
    public float getProgress() {
        return 0;
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }
    
    @Override
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.TANK_SCREEN;
    }
    
    @Override
    public Storage<FluidVariant> getFluidStorage(Direction direction) {
        return fluidStorage;
    }
    
    @Override
    public @Nullable SingleVariantStorage<FluidVariant> getForDirectFluidAccess() {
        return fluidStorage;
    }
    
    @Override
    public boolean showEnergy() {
        return false;
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
}
