package rearth.oritech.block.entity.processing;

import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
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
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CoolerBlockEntity extends MultiblockMachineEntity implements FluidApi.BlockProvider {
    
    private boolean inColdArea;
    private boolean initialized = false;
    
    public final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(4 * FluidStackHooks.bucketAmount(), this::markDirty);
    
    public CoolerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.COOLER_ENTITY, pos, state, Oritech.CONFIG.processingMachines.coolerData.energyPerTick());
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, MachineBlockEntity blockEntity) {
        super.tick(world, pos, state, blockEntity);
        
        if (!world.isClient && !initialized) {
            initialized = true;
            var biome = world.getBiome(pos);
            inColdArea = biome.isIn(TagContent.CONVENTIONAL_COLD);
        }
        
    }
    
    @Override
    public AddonUiData getUiData() {
        var base = super.getUiData();
        if (!inColdArea) return base;
        
        return new AddonUiData(base.positions(), base.openSlots(), base.efficiency() * 0.5f, base.speed() * 0.5f, base.ownPosition(), base.extraChambers());
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
    protected void sendNetworkEntry() {
        super.sendNetworkEntry();
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.SingleVariantFluidSyncPacketAPI(pos, Registries.FLUID.getId(fluidStorage.getFluid()).toString(), fluidStorage.getAmount()));
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
        super.craftItem(activeRecipe, outputInventory, inputInventory);
        
        var input = activeRecipe.getFluidInput();
        fluidStorage.extract(fluidStorage.getStack().copyWithAmount(input.amount()), false);
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
          new Vec3i(0, 0,-1)
        );
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        
        return List.of(
          new Vec3i(0, 0,-2)
        );
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(@Nullable Direction direction) {
        return fluidStorage;
    }
}
