package rearth.oritech.block.entity.processing;

import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
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
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.UpgradableOritechScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ArcaneRefineryBlockEntity extends MultiblockMachineEntity implements FluidApi.BlockProvider {
    
    @SyncField({SyncType.GUI_TICK, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public final SimpleInOutFluidStorage ownStorage = new SimpleInOutFluidStorage(16 * FluidStackHooks.bucketAmount(), this::setChanged);
    
    public ArcaneRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ARCANE_REFINERY_ENTITY, pos, state, OritechConfig.processingMachines.refineryData.energyPerTick.get());
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        super.serverTick(world, pos, state, blockEntity);
    }
    
    // todo
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ownStorage.writeNbt(nbt, "main");
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ownStorage.readNbt(nbt, "main");
    }
    
    // todo
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
        return OritechConfig.processingMachines.refineryData.energyCapacity.get();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.processingMachines.refineryData.maxEnergyInsertion.get();
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
        
        if (level instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SNOWFLAKE, emitPosition.x, emitPosition.y, emitPosition.z, 1, 1.2, 1.2, 1.2, 0);
        
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
}
    

