package rearth.oritech.block.entity.machines.storage;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.impl.transfer.context.SingleSlotContainerItemContext;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.machines.storage.SmallFluidTank;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.FluidProvider;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.InventoryProvider;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;
import rearth.oritech.util.SimpleSidedInventory;

import java.util.Arrays;
import java.util.List;

public class CreativeFluidTankEntity extends BlockEntity implements FluidProvider, InventoryProvider, ScreenProvider, ExtendedScreenHandlerFactory, BlockEntityTicker<CreativeFluidTankEntity> {

    private boolean netDirty = false;
    private int lastComparatorOutput = 0;
    private boolean hasFluid = false;

    public final SimpleSidedInventory inventory = new SimpleSidedInventory(2, new InventorySlotAssignment(0, 1, 1, 1)) {
        @Override
        public void markDirty() {
            CreativeFluidTankEntity.this.markDirty();
        }
    };

    private final SingleVariantStorage<FluidVariant> fluidStorage = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return Long.MAX_VALUE;
        }

        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            CreativeFluidTankEntity.this.markDirty();
        }
    };

    public CreativeFluidTankEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.CREATIVE_TANK_ENTITY, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        SingleVariantStorage.writeNbt(fluidStorage, FluidVariant.CODEC, nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        SingleVariantStorage.readNbt(fluidStorage, FluidVariant.CODEC, FluidVariant::blank, nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        // set blockstate when placing a tank
        markDirty();
    }

    @Override
    protected void addComponents(ComponentMap.Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state, CreativeFluidTankEntity blockEntity) {
        // fill/drain buckets

        if (world.isClient) return;

        if (world.getTime() % 100 == 0) netDirty = true;    // to ensure this syncs when no charges are triggered, and inventory isn't opened

        // automatically refill or empty
        if (hasFluid) fluidStorage.amount = fluidStorage.getCapacity() - FluidConstants.BUCKET;  //
        else fluidStorage.amount = 0;

        processBuckets();

        if ((world.getTime() + this.pos.getY()) % 20 == 0 && fluidStorage.amount > 0)
            outputToBelow();

        if (netDirty) {
            updateComparators(world, pos, state);
            updateNetwork();
        }

    }

    private void outputToBelow() {
        var tankCandidate = world.getBlockEntity(pos.down(), BlockEntitiesContent.SMALL_TANK_ENTITY);

        if (tankCandidate.isEmpty()) return;
        var belowTank = tankCandidate.get();
        var ownTank = this.fluidStorage;
        var targetTank = belowTank.getOwnFluidStorage();

        try (var tx = Transaction.openOuter()) {
            var transferAmount = targetTank.insert(ownTank.variant, ownTank.amount, tx);
            var extracted = ownTank.extract(ownTank.variant, transferAmount, tx);
            if (transferAmount > 0 && transferAmount == extracted)
                tx.commit();
        }

    }


    private void updateComparators(World world, BlockPos pos, BlockState state) {
        var previous = lastComparatorOutput;
        lastComparatorOutput = getComparatorOutput();

        if (previous != lastComparatorOutput) {
            world.updateComparators(pos, state.getBlock());
        }
    }

    private void processBuckets() {
        var inStack = inventory.getStack(0);

        if (!inStack.isEmpty() && inStack.isOf(Items.BUCKET) && fluidStorage.amount >= FluidConstants.BUCKET) {
            // try fill bucket
            var filledBucket = ItemVariant.of(fluidStorage.variant.getFluid().getBucketItem(), inStack.getComponentChanges()).toStack();
            if (!outputCanAcceptBucket(filledBucket)) return;
            if (filledBucket == null) return;

            try (var tx = Transaction.openOuter()) {
                long extracted = fluidStorage.extract(fluidStorage.getResource(), FluidConstants.BUCKET, tx);
                if (extracted != FluidConstants.BUCKET) return;

                inStack.decrement(1);
                // In theory, the slot should be empty at this point, but this should still work
                // if some mod has done something weird like making lava buckets stackable.
                if (inventory.getStack(1).isEmpty()) {
                    inventory.heldStacks.set(1, filledBucket);
                } else {
                    inventory.getStack(1).increment(1);
                }
                inventory.heldStacks.set(0, inStack);
                tx.commit();
            }
        } else if (inStack != ItemStack.EMPTY && inStack.getItem() instanceof BucketItem) {
            // empty input bucket
            var emptyBucket = ItemVariant.of(Items.BUCKET, inStack.getComponentChanges()).toStack();
            if (!outputCanAcceptBucket(emptyBucket)) return;
            Fluid bucketFluid = ((BucketItemAccessor) inStack.getItem()).fabric_getFluid();
            if (bucketFluid == Fluids.EMPTY) return;

            try (var tx = Transaction.openOuter()) {
                long inserted = fluidStorage.insert(FluidVariant.of(bucketFluid), FluidConstants.BUCKET, tx);
                if (inserted != FluidConstants.BUCKET) return;

                inStack.decrement(1);
                if (inventory.getStack(1).isEmpty()) {
                    inventory.heldStacks.set(1, emptyBucket);
                } else {
                    inventory.getStack(1).increment(1);
                }
                inventory.heldStacks.set(0, inStack);
                tx.commit();
            }

            // shouldn't be necessary, since tx.commit should already be marking this dirty
            this.markDirty();
        }
    }

    private void updateNetwork() {
        netDirty = false;
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.SingleVariantFluidSyncPacket(pos, Registries.FLUID.getId(fluidStorage.variant.getFluid()).toString(), fluidStorage.amount));
    }

    private boolean outputCanAcceptBucket(ItemStack bucket) {
        var slot = inventory.getStack(1);
        return (slot.isEmpty() || (slot.isStackable() && ItemStack.areItemsAndComponentsEqual(slot, bucket) && slot.getCount() < slot.getMaxCount()));
    }

    public int getComparatorOutput() {
        if (fluidStorage.isResourceBlank()) return 0;

        var fillPercentage = fluidStorage.amount / (float) fluidStorage.getCapacity();
        return (int) (1 + fillPercentage * 14);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        this.netDirty = true;
        if (world != null) {
            var blockState = world.getBlockState(getPos());
            world.setBlockState(getPos(), blockState.with(SmallFluidTank.LIT, isGlowingFluid()));
        }
    }

    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        return new ModScreens.BasicData(pos);
    }

    @Override
    public Text getDisplayName() {
        return Text.of("");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        this.markDirty();
        return new BasicMachineScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public InventoryStorage getInventory(Direction direction) {
        return InventoryStorage.of(inventory, direction);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 50, 40), new GuiSlot(1, 100, 40, true));
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

    public boolean isGlowingFluid() {
        return fluidStorage.amount > 0 && fluidStorage.variant.isOf(Fluids.LAVA);
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

    public void setFluid(Fluid fluid) {
        try (var tx = Transaction.openOuter()) {
            fluidStorage.variant = FluidVariant.of(fluid);
            hasFluid = true;
            tx.commit();
        }
        this.markDirty();
    }

    public void removeFluid() {
        try (var tx = Transaction.openOuter()) {
            fluidStorage.variant = FluidVariant.blank();
            hasFluid = false;
            tx.commit();
        }
        this.markDirty();
    }


}
