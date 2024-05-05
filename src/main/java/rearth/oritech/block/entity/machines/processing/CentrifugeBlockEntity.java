package rearth.oritech.block.entity.machines.processing;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
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
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("fluidAddon", hasFluidAddon);
        
        nbt.put("fluidVariantIn", inputStorage.variant.toNbt());
        nbt.putLong("fluidAmountIn", inputStorage.amount);
        nbt.put("fluidVariantOut", outputStorage.variant.toNbt());
        nbt.putLong("fluidAmountOut", outputStorage.amount);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        
        hasFluidAddon = nbt.getBoolean("fluidAddon");
        
        inputStorage.variant = FluidVariant.fromNbt(nbt.getCompound("fluidVariantIn"));
        inputStorage.amount = nbt.getLong("fluidAmountIn");
        outputStorage.variant = FluidVariant.fromNbt(nbt.getCompound("fluidVariantOut"));
        outputStorage.amount = nbt.getLong("fluidAmountOut");
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
          new GuiSlot(2, 131, 38));
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
