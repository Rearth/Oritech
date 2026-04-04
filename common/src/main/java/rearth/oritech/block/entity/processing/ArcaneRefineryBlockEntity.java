package rearth.oritech.block.entity.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleInOutFluidStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.UpgradableOritechScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.*;

// todo:
// screen
// renderer (fluid contents + status / variant)
// name (tainted refinery)
// texture colors X
// processing logic X
// fluid output selection
// environment scanning X
//  - sculk increases yield X
//  - arcane increases energy efficiency (and thus speed) X
// bonus calculation X
// increased energy intake, falloff scaling related to speed (max 50M, capacity scales with actual used speed, takes in 0 when not active)
// Custom widget for energy intake.
// energy intake, arcane, sculk progress sliders like in buddy drones?
// occasional particle intake from surround bonus blocks
// recipe viewer category for supported blocks

public class ArcaneRefineryBlockEntity extends MultiblockMachineEntity implements FluidApi.BlockProvider {
    
    @SyncField({SyncType.GUI_TICK, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public final SimpleInOutFluidStorage ownStorage = new SimpleInOutFluidStorage(16 * FluidStackHooks.bucketAmount(), this::setChanged);
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public EnvironmentFactor arcaneFactor = EnvironmentFactor.DEFAULT;
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public EnvironmentFactor sculkFactor = EnvironmentFactor.DEFAULT;
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public int selectedOutput = 0;  // can be 0, 1 or 2 (clickable/changeable via gui). Non-matching outputs are ignored
    
    public ArcaneRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ARCANE_REFINERY_ENTITY, pos, state, OritechConfig.processingMachines.refineryData.energyPerTick.get());
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        // enabled later in this method if working
        energyStorage.setMaxInsert(0);
        
        if (!isActive(state) || disabledViaRedstone) return;
        
        // if a recipe is found, this means the input items are all available
        var recipeCandidate = getRecipe();
        if (recipeCandidate.isEmpty())
            currentRecipe = OritechRecipe.DUMMY;     // reset recipe when invalid or no input is given
        
        
        if (recipeCandidate.isPresent() && canOutputRecipe(recipeCandidate.get().value()) && canProceed(recipeCandidate.get().value())) {
            
            // allow more energy in when working
            energyStorage.setMaxInsert(getDefaultInsertRate());
            
            // reset when recipe was switched while running
            if (currentRecipe != recipeCandidate.get().value()) resetProgress();
            
            // this is separate so that progress is not reset when out of energy
            if (energyStorage.getAmount() > OritechConfig.processingMachines.refineryData.energyPerTick.get()) {   // needs a min energy amount to work at all
                var activeRecipe = recipeCandidate.get().value();
                currentRecipe = activeRecipe;
                lastWorkedAt = world.getGameTime();
                
                // use all energy, calculate progression based on amount (and arcane factor)
                var steps = getAndDrainProgress();
                
                // increase progress
                progress += steps;
                
                if (checkCraftingFinished(activeRecipe)) {
                    craftItem(activeRecipe, getOutputView(), getInputView());
                    resetProgress();
                }
                
                setChanged();
            }
            
        } else {
            // this happens if either the input slot is empty, or the output slot is blocked
            if (progress > 0) resetProgress();
        }
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
        return List.of(results.getFirst().copyWithCount(results.getFirst().getCount() * getOutputMultiplier()));
    }
    
    private int getOutputMultiplier() {
        // range 1-3 based on sculk factor as yield
        return (int) (1 + (sculkFactor.result * 2.1f));
        
    }
    
    private void craftFluids(OritechRecipe activeRecipe) {
        // create outputs, remove inputs
        
        // remove input fluid
        ownStorage.getInputContainer().extract(ownStorage.getInputContainer().getStack().copyWithAmount(activeRecipe.getFluidInput().amount()), false);
        
        // create output fluids
        var fluidOutput = calculateOutputFluid(activeRecipe);
        ownStorage.getOutputContainer().insert(fluidOutput, false);
        
    }
    
    @Override
    public boolean canOutputRecipe(OritechRecipe recipe) {
        
        var fluidOutput = calculateOutputFluid(recipe);
        if (!fluidOutput.isEmpty()) {
            var inserted = ownStorage.getOutputContainer().insert(fluidOutput, true);
            if (inserted != fluidOutput.getAmount()) return false;
        }
        
        return super.canOutputRecipe(recipe);
    }
    
    // includes the sculk yield bonus
    private FluidStack calculateOutputFluid(OritechRecipe recipe) {
        
        var fluidOutputs = recipe.getFluidOutputs();
        if (fluidOutputs.size() >= selectedOutput) {
            var result = fluidOutputs.get(selectedOutput);
            return result.copyWithAmount(result.getAmount() * getOutputMultiplier());
        }
        return FluidStack.empty();
        
    }
    
    private int getAndDrainProgress() {
        var availableEnergy = (float) energyStorage.getAmount();
        energyStorage.setAmount(0);
        
        // (remapped from 0-1 to 1-8)
        var energyFactor = (sculkFactor.result * 8) + 1;
        availableEnergy *= energyFactor;
        
        return (int) Math.round(0.2f * Math.pow(availableEnergy, 0.5f));
    }
    
    // todo
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ownStorage.writeNbt(nbt, "main");
        nbt.putInt("output", selectedOutput);
        EnvironmentFactor.toNbt(nbt, "arcane_factor", arcaneFactor);
        EnvironmentFactor.toNbt(nbt, "sculk_factor", sculkFactor);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ownStorage.readNbt(nbt, "main");
        selectedOutput = nbt.getInt("output");
        arcaneFactor = EnvironmentFactor.fromNbt(nbt, "arcane_factor");
        sculkFactor = EnvironmentFactor.fromNbt(nbt, "sculk_factor");
    }
    
    @Override
    protected Optional<RecipeHolder<OritechRecipe>> getRecipe() {
        
        // get recipes matching input items
        var candidates = Objects.requireNonNull(level).getRecipeManager().getRecipesFor(getOwnRecipeType(), getInputInventory(), level);
        
        // filter out recipes based on input tank. Have the ones with input items first.
        return candidates
                 .stream()
                 .filter(candidate -> CentrifugeBlockEntity.recipeInputMatchesTank(ownStorage.getInputContainer().getStack(), candidate.value()))
                 .findAny();
        
    }
    
    @Override
    public boolean initMultiblock(BlockState state) {
        scanEnv();
        return super.initMultiblock(state);
    }
    
    private void scanEnv() {
        
        if (level == null) {
            return;
        }
        
        var range = 16;
        
        // factors are best if 16 blocks are present (calculated independent for each type)
        // with at least 4 different block types present. Max factor is 1. E.g. 16 blocks with 3 types would be less, but adding more blocks can even it out.
        // There is a penalty if not enough types are present, but that penalty can be overcome with mass.
        var targetCount = 16;
        var targetTypes = 4;
        
        var differentSculkTypes = new HashSet<Block>();
        var sculkPositions = new ArrayList<BlockPos>();
        
        var differentArcaneTypes = new HashSet<Block>();
        var arcanePositions = new ArrayList<BlockPos>();
        
        for (var pos : BlockPos.withinManhattan(worldPosition, range, range, range)) {
            if (pos.equals(worldPosition)) continue;
            
            var elemState = level.getBlockState(pos);
            if (elemState.is(TagContent.REFINERY_SCULK_BLOCKS) && isVisible(pos)) {
                differentSculkTypes.add(elemState.getBlock());
                sculkPositions.add(pos);
            } else if (elemState.is(TagContent.REFINERY_ARCANE_BLOCKS) && isVisible(pos)) {
                differentArcaneTypes.add(elemState.getBlock());
                arcanePositions.add(pos);
                
            }
        }
        
        sculkFactor = calculateEnvFactor(sculkPositions.size(), differentSculkTypes.size(), targetCount, targetTypes, sculkPositions);
        arcaneFactor = calculateEnvFactor(arcanePositions.size(), differentArcaneTypes.size(), targetCount, targetTypes, arcanePositions);
        setChanged();
        
        System.out.println("sculk: " + sculkFactor);
        System.out.println("arcane: " + arcaneFactor);
        
    }
    
    private static EnvironmentFactor calculateEnvFactor(int blockCount, int typeCount, int targetCount, int targetTypes, List<BlockPos> sources) {
        if (blockCount <= 0 || typeCount <= 0) return new EnvironmentFactor(0, 0, sources);
        return new EnvironmentFactor(Math.min(1f, blockCount / (float) targetCount * (typeCount / (float) targetTypes)), typeCount, sources);
    }
    
    private boolean isVisible(BlockPos targetPos) {
        var origin = Vec3.atCenterOf(worldPosition.above());
        var target = Vec3.atCenterOf(targetPos);
        var direction = target.subtract(origin);
        var distance = direction.length();
        
        if (distance <= 1.0) return true;
        
        var step = direction.normalize().scale(0.3);
        var checkedPositions = new HashSet<BlockPos>();
        
        for (var travelled = 0.3; travelled < distance; travelled += 0.3) {
            var checkPos = BlockPos.containing(origin.add(step.scale(travelled / 0.3)));
            
            if (checkPos.equals(worldPosition) || checkPos.equals(targetPos) || !checkedPositions.add(checkPos))
                continue;
            
            var checkState = level.getBlockState(checkPos);
            if (checkState.isSolidRender(level, checkPos) && !(checkState.getBlock() instanceof MachineCoreBlock))
                return false;
        }
        
        return true;
    }
    
    // todo
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new UpgradableOritechScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public BarConfiguration getFluidConfiguration() {
        return new BarConfiguration(30, 6, 21, 74);
    }
    
    @Override
    public long getDefaultCapacity() {
        return 50_000_000;
    }
    
    @Override
    public long getDefaultInsertRate() {
        return 50_000_000;
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
        
        if (level instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.SNOWFLAKE, emitPosition.x, emitPosition.y, emitPosition.z, 1, 1.2, 1.2, 1.2, 0);
        
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
        return ModScreens.ARCANE_REFINERY_SCREEN;
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
    
    
    // todo
    @Override
    public List<Tuple<Component, Component>> getExtraExtensionLabels() {
        return super.getExtraExtensionLabels();
    }
    
    @Override
    public BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(8, 7, 18, 71);
    }
    
    @Override
    public ColorVariant getDefaultColor() {
        return super.getDefaultColor();
    }
    
    public record EnvironmentFactor(float result, int variants, List<BlockPos> sources) {
        
        public static final Codec<EnvironmentFactor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.FLOAT.fieldOf("result").forGetter(EnvironmentFactor::result),
          Codec.INT.fieldOf("variants").forGetter(EnvironmentFactor::variants),
          BlockPos.CODEC.listOf().fieldOf("sources").forGetter(EnvironmentFactor::sources)
        ).apply(instance, EnvironmentFactor::new));
        
        public static final EnvironmentFactor DEFAULT = new EnvironmentFactor(0, 0, List.of());
        
        public static void toNbt(CompoundTag nbt, String key, EnvironmentFactor factor) {
            CODEC.encodeStart(NbtOps.INSTANCE, factor)
              .resultOrPartial(error -> Oritech.LOGGER.error("Failed to encode {}: {}", key, error))
              .ifPresent(tag -> nbt.put(key, tag));
        }
        
        public static EnvironmentFactor fromNbt(CompoundTag nbt, String key) {
            if (!nbt.contains(key)) {
                return DEFAULT;
            }
            
            return CODEC.parse(NbtOps.INSTANCE, nbt.get(key))
                     .resultOrPartial(error -> Oritech.LOGGER.error("Failed to decode {}: {}", key, error))
                     .orElse(DEFAULT);
        }
        
        @Override
        public String toString() {
            return "EnvironmentFactor{" +
                     "result=" + result +
                     ", blockCount=" + sources.size() +
                     ", variants=" + variants +
                     '}';
        }
    }
}
    

