package rearth.oritech.block.entity.machines.processing;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.CentrifugeScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.FluidProvider;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;

public class CentrifugeBlockEntity extends MultiblockMachineEntity implements FluidProvider {
    
    private static final long CAPACITY = Oritech.CONFIG.processingMachines.centrifugeData.tankSizeInBuckets() * FluidConstants.BUCKET;
    
    public final SingleVariantStorage<FluidVariant> inputStorage = createBasicTank();
    public final SingleVariantStorage<FluidVariant> outputStorage = createBasicTank();
    private final Storage<FluidVariant> exposedInput = FilteringStorage.insertOnlyOf(inputStorage);
    private final Storage<FluidVariant> exposedOutput = FilteringStorage.extractOnlyOf(outputStorage);
    private final Storage<FluidVariant> combinedTanks = new CombinedStorage<>(List.of(exposedInput, exposedOutput));
    
    public boolean hasFluidAddon;
    
    public final SimpleInventory bucketInventory = new SimpleInventory(2) {
        @Override
        public void markDirty() {
            CentrifugeBlockEntity.this.markDirty();
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            System.out.println(stack);
            return stack.getItem() instanceof BucketItem;
        }
    };
    public final InventoryStorage bucketStorage = InventoryStorage.of(bucketInventory, null);
    
    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.CENTRIFUGE_ENTITY, pos, state, Oritech.CONFIG.processingMachines.centrifugeData.energyPerTick());
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.processingMachines.centrifugeData.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.processingMachines.centrifugeData.maxEnergyInsertion();
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, MachineBlockEntity blockEntity) {
        
        if (hasFluidAddon && !world.isClient) {
            var bucketIn = bucketInventory.getStack(0);
            var bucketOut = bucketInventory.getStack(1);
            processBucket(bucketIn, bucketOut, inputStorage, outputStorage);
        }
        
        super.tick(world, pos, state, blockEntity);
    }
    
    private void processBucket(ItemStack inStack, ItemStack outStack, SingleVariantStorage<FluidVariant> inStorage, SingleVariantStorage<FluidVariant> outStorage) {
        
        if (inStack != ItemStack.EMPTY && inStack.getItem().equals(Items.BUCKET) && outStorage.amount >= FluidConstants.BUCKET && outStack == ItemStack.EMPTY) {
            // try to fill empty bucket
            var filledBucketType = outStorage.variant.getFluid().getBucketItem();
            if (filledBucketType == null) return;
            inStack.decrement(1);
            bucketInventory.heldStacks.set(1, new ItemStack(filledBucketType));
            bucketInventory.heldStacks.set(0, inStack);
            outStorage.amount -= FluidConstants.BUCKET;
            
            this.markDirty();
            markNetDirty();
            
        } else if (inStack != ItemStack.EMPTY && inStack.getItem() instanceof BucketItem && !inStack.getItem().equals(Items.BUCKET) && outputCanAcceptBucket(outStack)) {
            // from full input bucket
            
            // weird voodoo because the transaction APIs are weird and I have NO idea what this all is
            var context = ContainerItemContext.ofSingleSlot(bucketStorage.getSlot(0)).find(FluidStorage.ITEM);
            if (context == null) return;
            var variant = context.iterator().next().getResource();  // non empty iterator doesnt seem to do what it implies, so whatever
            if (variant == null) return;
            
            var bucketUsed = false;
            if ((inStorage.variant.isOf(variant.getFluid()) && inStorage.amount + FluidConstants.BUCKET <= inStorage.getCapacity())) {
                bucketUsed = true;
                inStorage.amount += FluidConstants.BUCKET;
            } else if (inStorage.amount == 0) {
                inStorage.variant = variant;
                inStorage.amount = FluidConstants.BUCKET;
                bucketUsed = true;
            }
            
            if (bucketUsed) {
                bucketInventory.setStack(0, ItemStack.EMPTY);
                var bucketCount = bucketInventory.getStack(1).getCount();
                bucketInventory.setStack(1, new ItemStack(Items.BUCKET, bucketCount + 1));
            }
            
            this.markDirty();
            markNetDirty();
        }
    }
    
    private boolean outputCanAcceptBucket(ItemStack slot) {
        if (slot == null) return true;
        if (slot.isEmpty()) return true;
        return slot.getItem().equals(Items.BUCKET) && slot.getCount() < slot.getMaxCount();
    }
    
    @Override
    protected boolean canProceed(OritechRecipe recipe) {
        
        if (!hasFluidAddon) return super.canProceed(recipe);
        
        // check if input is available
        var input = recipe.getFluidInput();
        if (input == null) return false;
        if (!input.variant().equals(inputStorage.variant) || input.amount() > inputStorage.amount) return false;
        
        // check if output fluid fits
        var output = recipe.getFluidOutput();
        if (output != null) {
            if (output.variant().getFluid().equals(Fluids.EMPTY) || outputStorage.amount == 0)
                return true;  // no output stored
            if (outputStorage.amount + output.amount() > outputStorage.getCapacity()) return false; // output full
            return outputStorage.variant.equals(output.variant());  // type check
        }
        
        return true;
        
    }
    
    @Override
    protected void craftItem(OritechRecipe activeRecipe, List<ItemStack> outputInventory, List<ItemStack> inputInventory) {
        super.craftItem(activeRecipe, outputInventory, inputInventory);
        
        if (hasFluidAddon)
            craftFluids(activeRecipe);
    }
    
    private void craftFluids(OritechRecipe activeRecipe) {
        
        var input = activeRecipe.getFluidInput();
        var output = activeRecipe.getFluidOutput();
        
        try (var tx = Transaction.openOuter()) {
            
            if (input != null)
                inputStorage.extract(input.variant(), input.amount(), tx);
            if (output != null)
                outputStorage.insert(output.variant(), output.amount(), tx);
            
            tx.commit();
            
        }
    }
    
    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_FLUID_ADDON)) {
            hasFluidAddon = true;
        }
    }
    
    @Override
    public void resetAddons() {
        super.resetAddons();
        hasFluidAddon = false;
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("fluidAddon", hasFluidAddon);
        
        nbt.put("fluidVariantIn", inputStorage.variant.toNbt());
        nbt.putLong("fluidAmountIn", inputStorage.amount);
        nbt.put("fluidVariantOut", outputStorage.variant.toNbt());
        nbt.putLong("fluidAmountOut", outputStorage.amount);
        
        var bucketStorageNbt = new NbtCompound();
        Inventories.writeNbt(bucketStorageNbt, bucketInventory.heldStacks, false);
        nbt.put("bucket", bucketStorageNbt);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        
        hasFluidAddon = nbt.getBoolean("fluidAddon");
        
        inputStorage.variant = FluidVariant.fromNbt(nbt.getCompound("fluidVariantIn"));
        inputStorage.amount = nbt.getLong("fluidAmountIn");
        outputStorage.variant = FluidVariant.fromNbt(nbt.getCompound("fluidVariantOut"));
        outputStorage.amount = nbt.getLong("fluidAmountOut");
        
        Inventories.readNbt(nbt.getCompound("bucket"), bucketInventory.heldStacks);
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        if (hasFluidAddon) return RecipeContent.CENTRIFUGE_FLUID;
        return RecipeContent.CENTRIFUGE;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 1, 1, 2);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 56, 38),
          new GuiSlot(1, 113, 38),
          new GuiSlot(2, 113, 56));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.CENTRIFUGE_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 3;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1, 0)
        );
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        
        return List.of(
          new Vec3i(0, 0, -1),
          new Vec3i(0, 0, 1)
        );
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CentrifugeScreenHandler(syncId, playerInventory, this, getUiData(), getCoreQuality());
    }
    
    @Override
    protected float getAnimationSpeed() {
        return super.getAnimationSpeed() * 3;
    }
    
    // this will allow full access on top and bottom to specific tank kinds (allowing both insertion and extraction)
    // sides can access both tanks, but insert only to input tank, and extract only from output tank
    @Override
    public Storage<FluidVariant> getFluidStorage(Direction direction) {
        if (!hasFluidAddon) return exposedOutput;
        if (direction == null) return combinedTanks;
        return switch (direction) {
            case DOWN -> outputStorage;
            case UP -> inputStorage;
            default -> combinedTanks;
        };
    }
    
    @Override
    public @Nullable SingleVariantStorage<FluidVariant> getForDirectFluidAccess() {
        return outputStorage;
    }
    
    @Override
    protected void sendNetworkEntry() {
        super.sendNetworkEntry();
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(
          new NetworkContent.CentrifugeFluidSyncPacket(
            pos,
            hasFluidAddon,
            Registries.FLUID.getId(inputStorage.variant.getFluid()).toString(),
            inputStorage.amount,
            Registries.FLUID.getId(outputStorage.variant.getFluid()).toString(),
            outputStorage.amount));
    }
    
    private SingleVariantStorage<FluidVariant> createBasicTank() {
        return new SingleVariantStorage<>() {
            @Override
            protected FluidVariant getBlankVariant() {
                return FluidVariant.blank();
            }
            
            @Override
            protected long getCapacity(FluidVariant variant) {
                return CAPACITY;
            }
            
            @Override
            protected void onFinalCommit() {
                super.onFinalCommit();
                CentrifugeBlockEntity.this.markDirty();
            }
        };
    }
}
