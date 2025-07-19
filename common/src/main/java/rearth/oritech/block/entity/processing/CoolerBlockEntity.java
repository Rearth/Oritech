package rearth.oritech.block.entity.processing;

import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CoolerBlockEntity extends MultiblockMachineEntity implements FluidApi.BlockProvider {
    
    private boolean inColdArea;
    private boolean initialized = false;
    
    @SyncField(SyncType.GUI_TICK)
    public final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(4 * FluidStackHooks.bucketAmount(), this::markDirty);
    
    public CoolerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.COOLER_ENTITY, pos, state, Oritech.CONFIG.processingMachines.coolerData.energyPerTick());
    }
    
    @Override
    public void serverTick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        super.serverTick(world, pos, state, blockEntity);
        
        if (!initialized) {
            initialized = true;
            var biome = world.getBiome(pos);
            inColdArea = biome.isIn(TagContent.CONVENTIONAL_COLD);
        }
        
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        fluidStorage.writeNbt(nbt, "");
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        fluidStorage.readNbt(nbt, "");
    }
    
    @Override
    protected void useEnergy() {
        super.useEnergy();
        
        var progress = getProgress();
        if (progress < 0.35 || progress > 0.65) return;
        
        if (world.random.nextFloat() > 0.4) return;
        // emit particles
        var emitPosition = Vec3d.ofCenter(pos);
        
        ParticleContent.COOLER_WORKING.spawn(world, emitPosition, 2);
        
    }
    
    @Override
    protected Optional<RecipeEntry<OritechRecipe>> getRecipe() {
        
        // get recipes matching input items
        var candidates = Objects.requireNonNull(world).getRecipeManager().getAllMatches(getOwnRecipeType(), getInputInventory(), world);
        // filter out recipes based on input tank
        var fluidRecipe = candidates.stream().filter(candidate -> CentrifugeBlockEntity.recipeInputMatchesTank(fluidStorage.getStack(), candidate.value())).findAny();
        if (fluidRecipe.isPresent()) {
            return fluidRecipe;
        }
        
        return super.getRecipe();
    }
    
    @Override
    protected void craftItem(OritechRecipe activeRecipe, List<ItemStack> outputInventory, List<ItemStack> inputInventory) {
        
        if (!processCraftInstance(activeRecipe)) return;
        
        if (supportExtraChambersAuto()) {
            var chamberCount = getBaseAddonData().extraChambers();
            
            // remove extra fluid if more chambers are active
            for (int i = 0; i < chamberCount; i++) {
                if (!processCraftInstance(activeRecipe)) break;
            }
        }
        
    }
    
    // returns true if crafting has been successful
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean processCraftInstance(OritechRecipe activeRecipe) {
        
        var results = getCraftingResults(activeRecipe);
        if (results.isEmpty()) return false;
        var result = results.getFirst();
        
        // try removing input fluid if output item would fit
        if (inventory.heldStacks.getFirst().getCount() + result.getCount() > 64) return false;
        
        var input = activeRecipe.getFluidInput();
        var extracted = fluidStorage.extract(fluidStorage.getStack().copyWithAmount(input.amount()), true);
        if (extracted == activeRecipe.getFluidInput().amount()) {
            // fluid is available, and item fits.
            fluidStorage.extract(fluidStorage.getStack().copyWithAmount(input.amount()), false);
            if (inventory.heldStacks.getFirst().isEmpty()) {
                inventory.heldStacks.set(0, result.copy());
            } else {
                inventory.heldStacks.getFirst().increment(result.getCount());
            }
            
            return true;
        }
        
        return false;
    }
    
    @Override
    public float getSpeedMultiplier() {
        var biomeBonus = inColdArea ? 0.5f : 1f;
        return super.getSpeedMultiplier() * biomeBonus;
    }
    
    @Override
    public float getEfficiencyMultiplier() {
        var biomeBonus = inColdArea ? 0.5f : 1f;
        return super.getEfficiencyMultiplier() * biomeBonus;
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.processingMachines.coolerData.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.processingMachines.coolerData.maxEnergyInsertion();
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.COOLER;
    }
    
    @Override
    public InventorySlotAssignment getSlotAssignments() {
        return new InventorySlotAssignment(0, 0, 0, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 117, 36, true));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.COOLER_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 1;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 0, -1)
        );
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        
        return List.of(
          new Vec3i(0, 0, -2)
        );
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(@Nullable Direction direction) {
        return fluidStorage;
    }
}
