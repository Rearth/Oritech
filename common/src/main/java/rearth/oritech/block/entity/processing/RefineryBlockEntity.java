package rearth.oritech.block.entity.processing;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.api.fluid.containers.SimpleInOutFluidStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.RefineryScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RefineryBlockEntity extends MultiblockMachineEntity implements FluidApi.BlockProvider {
    
    // own storage is exposed through this multiblock, the other storages are exposed through the respective modules
    @SyncField({SyncType.GUI_TICK, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public final SimpleInOutFluidStorage ownStorage = new SimpleInOutFluidStorage(64 * FluidStackHooks.bucketAmount(), this::setChanged);
    @SyncField({SyncType.GUI_TICK, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public final SimpleFluidStorage nodeA = new SimpleFluidStorage(4 * FluidStackHooks.bucketAmount(), this::setChanged);
    @SyncField({SyncType.GUI_TICK, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public final SimpleFluidStorage nodeB = new SimpleFluidStorage(4 * FluidStackHooks.bucketAmount(), this::setChanged);
    
    @SyncField(SyncType.GUI_OPEN)
    private int moduleCount;    // range 0-2
    
    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REFINERY_ENTITY, pos, state, Oritech.CONFIG.processingMachines.refineryData.energyPerTick());
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        super.serverTick(world, pos, state, blockEntity);
        
        if (world.getGameTime() % 25 == 0) {
            refreshModules();
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ownStorage.writeNbt(nbt, "main");
        nodeA.writeNbt(nbt, "a");
        nodeB.writeNbt(nbt, "b");
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ownStorage.readNbt(nbt, "main");
        nodeA.readNbt(nbt, "a");
        nodeB.readNbt(nbt, "b");
    }
    
    private void refreshModules() {
        moduleCount = 0;
        var startPos = worldPosition.above(2);
        
        for (int i = 0; i <= 1; i++) {
            var candidatePos = startPos.offset(0, i, 0);
            var candidate = level.getBlockEntity(candidatePos, BlockEntitiesContent.REFINERY_MODULE_ENTITY);
            if (candidate.isEmpty() || !candidate.get().isActive(candidate.get().getBlockState())) break;
            
            moduleCount++;
            candidate.get().setOwningRefinery(this);
        }
    }
    
    public int getModuleCount() {
        return moduleCount;
    }
    
    @Override
    protected Optional<RecipeHolder<OritechRecipe>> getRecipe() {
        
        // get recipes matching input items
        var candidates = Objects.requireNonNull(level).getRecipeManager().getRecipesFor(getOwnRecipeType(), getInputInventory(), level);
        // filter out recipes based on input tank. Have the ones with input items first.
        var fluidRecipe = candidates
                            .stream()
                            .filter(candidate -> CentrifugeBlockEntity.recipeInputMatchesTank(ownStorage.getInputContainer().getStack(), candidate.value()))
                            .sorted(Comparator.comparingInt(a -> -a.value().getInputs().size()))
                            .findAny();
        if (fluidRecipe.isPresent()) {
            return fluidRecipe;
        }
        
        return Optional.empty();
    }
    
    @Override
    protected void craftItem(OritechRecipe activeRecipe, List<ItemStack> outputInventory, List<ItemStack> inputInventory) {
        super.craftItem(activeRecipe, outputInventory, inputInventory);
        craftFluids(activeRecipe);
    }
    
    @Override
    public List<ItemStack> getCraftingResults(OritechRecipe activeRecipe) {
        var results = activeRecipe.getResults();
        if (results.isEmpty()) return List.of();
        return List.of(results.getFirst().copyWithCount(results.getFirst().getCount() * getItemOutputMultiplier(activeRecipe)));
    }
    
    private void craftFluids(OritechRecipe activeRecipe) {
        // create outputs, remove inputs
        
        // remove input fluid
        ownStorage.getInputContainer().extract(ownStorage.getInputContainer().getStack().copyWithAmount(activeRecipe.getFluidInput().amount()), false);
        
        // create output fluids
        var outputs = calculateOutputFluids(activeRecipe);
        for (int i = 0; i < outputs.size(); i++) {
            var output = outputs.get(i);
            var outputTank = getOutputStorage(i);
            outputTank.insert(output, false);
        }
        
    }
    
    private List<FluidStack> calculateOutputFluids(OritechRecipe recipe) {
        // if no modules are installed, output twice the resulting items and fluids
        // if one module is installed, output twice the output A
        // if both are installed, output all as normal
        // if the recipe also only less than 2 fluid outputs, output normal
        
        if (recipe.getFluidOutputs().isEmpty()) return List.of();
        var outA = recipe.getFluidOutputs().get(0);
        
        if (recipe.getFluidOutputs().size() == 1) return List.of(outA);
        var outB = recipe.getFluidOutputs().get(1);
        
        return switch (moduleCount) {
            case 0 -> List.of(outA.copyWithAmount(outA.getAmount() * 2));
            case 1 -> List.of(outA, outB.copyWithAmount(outB.getAmount() * 2));
            case 2 -> recipe.getFluidOutputs();
            default -> throw new IllegalStateException("more than 2 modules is not supported/allowed");
        };
    }
    
    private int getItemOutputMultiplier(OritechRecipe recipe) {
        if (recipe.getFluidOutputs().size() <= 1) return 1;
        return getModuleCount() == 0 ? 2 : 1;
    }
    
    @Override
    public boolean canOutputRecipe(OritechRecipe recipe) {
        
        // 0 = base output, 1&2 = module outputs
        // checks if all fluid outputs for the active modules fit
        var fluidOutputs = calculateOutputFluids(recipe);
        for (int i = 0; i <= moduleCount; i++) {
            if (i >= fluidOutputs.size()) break;
            var fluidOutput = fluidOutputs.get(i);
            if (fluidOutput == null || fluidOutput.isEmpty()) continue;
            var storage = getOutputStorage(i);
            var inserted = storage.insert(fluidOutput, true);
            if (inserted != fluidOutput.getAmount()) return false;
        }
        
        return super.canOutputRecipe(recipe);
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new RefineryScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public BarConfiguration getFluidConfiguration() {
        return new BarConfiguration(28, 6, 21, 74);
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.processingMachines.refineryData.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.processingMachines.refineryData.maxEnergyInsertion();
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.REFINERY;
    }
    
    @Override
    protected void useEnergy() {
        super.useEnergy();
        
        if (level.random.nextFloat() > 0.8) return;
        // emit particles
        var facing = getFacing();
        var offsetLocal = Geometry.rotatePosition(new Vec3(0.3, 0.5, 0.3), facing);
        var emitPosition = Vec3.atCenterOf(worldPosition).add(offsetLocal);
        
        ParticleContent.COOLER_WORKING.spawn(level, emitPosition, 1);
        
    }
    
    @Override
    public InventorySlotAssignment getSlotAssignments() {
        return new InventorySlotAssignment(0, 1, 1, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 62, 8),
          new GuiSlot(1, 62, 61, true));
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.REFINERY_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 2;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    // x = back
    // y = up
    // z = left
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1, 0),    // middle
          new Vec3i(0, 0, -1),    // right
          new Vec3i(0, 1, -1),
          new Vec3i(1, 0, -1),    // back right
          new Vec3i(1, 1, -1),
          new Vec3i(1, 0, 0),    // back middle
          new Vec3i(1, 1, 0),
          new Vec3i(2, 0, -1),    // backer middle
          new Vec3i(2, 1, -1)
        );
    }
    
    @Override
    public ArrowConfiguration getIndicatorConfiguration() {
        return new ArrowConfiguration(
          Oritech.id("textures/gui/modular/arrow_empty.png"),
          Oritech.id("textures/gui/modular/arrow_full.png"),
          54, 35, 29, 16, true);
    }
    
    // x = back, // z = left
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of();
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(@Nullable Direction direction) {
        return ownStorage;
    }
    
    public FluidApi.FluidStorage getFluidStorageForModule(BlockPos modulePos) {
        var yDist = modulePos.getY() - this.worldPosition.getY();
        if (yDist == 2) return nodeA;
        if (yDist == 3) return nodeB;
        throw new IllegalStateException("Module needs to be either 1 or 2 blocks above");
    }
    
    @Override
    public List<Tuple<Component, Component>> getExtraExtensionLabels() {
        return List.of(new Tuple<>(Component.literal("\uD83D\uDCE6: " + moduleCount), Component.translatable("tooltip.oritech.refinery_module_count")));
    }
    
    public FluidApi.SingleSlotStorage getOutputStorage(int i) {
        if (i == 0) return ownStorage.getOutputContainer();
        if (i == 1) return nodeA;
        if (i == 2) return nodeB;
        throw new IllegalArgumentException("Only has 2 storage modules, tried accessing: " + i);
    }
    
    @Override
    public ColorVariant getDefaultColor() {
        return ColorVariant.FLUXITE;
    }
}
    

