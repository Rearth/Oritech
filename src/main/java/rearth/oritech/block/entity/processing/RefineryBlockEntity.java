package rearth.oritech.block.entity.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.api.transfer.fluid.InOutFluidStorage;
import rearth.oritech.api.transfer.fluid.SimpleFluidStorage;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.block.entity.arcane.EnchantmentCatalystBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.RefineryScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeInput;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.Geometry;

import java.util.List;
import java.util.Optional;

public class RefineryBlockEntity extends MultiblockMachineEntity implements FluidProvider {

    // own storage is exposed through this multiblock, the other storages are exposed through the respective modules
    @SyncField({SyncType.GUI_TICK, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public final InOutFluidStorage ownStorage = new InOutFluidStorage(8 * FluidType.BUCKET_VOLUME, this::setChanged, new ContainerSlotAssignment(0, 1, 1, 1));
    @SyncField({SyncType.GUI_TICK, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public final SimpleFluidStorage nodeA = new SimpleFluidStorage(4 * FluidType.BUCKET_VOLUME, this::setChanged);
    @SyncField({SyncType.GUI_TICK, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public final SimpleFluidStorage nodeB = new SimpleFluidStorage(4 * FluidType.BUCKET_VOLUME, this::setChanged);

    @SyncField(SyncType.GUI_OPEN)
    private int moduleCount;    // range 0-2

    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REFINERY_ENTITY.get(), pos, state, OritechConfig.processingMachines.refineryData.energyPerTick.get());
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        super.serverTick(serverLevel, pos, state, blockEntity);

        if (serverLevel.getGameTime() % 25 == 0) {
            refreshModules();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ownStorage.serialize(output);
        nodeA.serialize(output);
        nodeB.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ownStorage.deserialize(input);
        nodeA.deserialize(input);
        nodeB.deserialize(input);
    }

    private void refreshModules() {
        if (level == null) return;
        moduleCount = 0;
        var startPos = worldPosition.above(2);

        for (int i = 0; i <= 1; i++) {
            var candidatePos = startPos.offset(0, i, 0);
            var candidate = level.getBlockEntity(candidatePos, BlockEntitiesContent.REFINERY_MODULE_ENTITY.get());
            if (candidate.isEmpty() || !candidate.get().isActive(candidate.get().getBlockState())) break;

            moduleCount++;
            candidate.get().setOwningRefinery(this);
        }
    }

    public int getModuleCount() {
        return moduleCount;
    }

    @Override
    protected OritechRecipeInput getRecipeInput() {
        return new OritechRecipeInput(getInputView(), ownStorage.getInStack());
    }

    @Override
    protected boolean createCraftingOutputs(Transaction transaction) {
        return createFluidResults(transaction) && super.createCraftingOutputs(transaction);
    }

    @Override
    protected boolean removeCraftingInputs(Transaction transaction) {
        return removeFluidInputs(transaction) && super.removeCraftingInputs(transaction);
    }

    @Override
    public List<ItemStackTemplate> getCraftingResults(OritechRecipe activeRecipe) {
        var results = activeRecipe.itemResults();
        if (results.isEmpty()) return List.of();
        var first = results.getFirst();
        return List.of(first.withCount(first.count() * getItemOutputMultiplier(activeRecipe)));
    }

    private boolean createFluidResults(Transaction transaction) {

        var outputs = calculateOutputFluids(currentRecipe);
        for (int i = 0; i < outputs.size(); i++) {
            var output = outputs.get(i);
            if (output.isEmpty()) continue;

            var outputTank = getOutputLookup(i);
            var inserted = outputTank.insert(FluidResource.of(output), output.getAmount(), transaction);
            if (inserted != output.getAmount()) return false;
        }

        return true;
    }

    private boolean removeFluidInputs(Transaction transaction) {
        var input = currentRecipe.fluidInput();
        if (input.isEmpty()) return true;

        var inputTank = ownStorage.getInputContainer();
        var inputResource = inputTank.getResource(0);
        if (inputResource.isEmpty()) return false;

        var extracted = inputTank.extract(0, inputResource, input.get().amount(), transaction);
        return extracted == input.get().amount();
    }

    private List<FluidStack> calculateOutputFluids(OritechRecipe recipe) {
        // if no modules are installed, output twice the resulting items and fluids
        // if one module is installed, output twice the output A
        // if both are installed, output all as normal
        // if the recipe also only less than 2 fluid outputs, output normal

        if (recipe.fluidOutputs().isEmpty()) return List.of();
        var outA = recipe.fluidOutputs().getFirst().create();

        if (recipe.fluidOutputs().size() == 1) return List.of(outA);
        var outB = recipe.fluidOutputs().get(1).create();

        return switch (moduleCount) {
            case 0 -> List.of(outA.copyWithAmount(outA.getAmount() * 2));
            case 1 -> List.of(outA, outB.copyWithAmount(outB.getAmount() * 2));
            case 2 -> recipe.fluidOutputs().stream().map(FluidStackTemplate::create).toList();
            default -> throw new IllegalStateException("more than 2 modules is not supported/allowed");
        };
    }

    private int getItemOutputMultiplier(OritechRecipe recipe) {
        if (recipe.fluidOutputs().size() <= 1) return 1;
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
            if (fluidOutput.isEmpty()) continue;
            if (!canInsertFluid(getOutputLookup(i), fluidOutput)) return false;
        }

        return super.canOutputRecipe(recipe);
    }

    private boolean canInsertFluid(ResourceHandler<FluidResource> storage, FluidStack fluidOutput) {
        try (var transaction = Transaction.openRoot()) {
            var inserted = storage.insert(FluidResource.of(fluidOutput), fluidOutput.getAmount(), transaction);
            return inserted == fluidOutput.getAmount();
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new RefineryScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public BarConfiguration getFluidConfiguration() {
        return new BarConfiguration(30, 6, 21, 74);
    }

    @Override
    public List<ResourceHandler<FluidResource>> getInteractableFluidStorages() {
        return List.of(
                ownStorage.getInputContainer(),
                ownStorage.getOutputContainer(),
                nodeA,
                nodeB);
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
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.REFINERY.get();
    }

    @Override
    protected void onProgressed() {
        super.onProgressed();

        if (level == null || level.getRandom().nextFloat() > 0.8f) return;
        // emit particles
        var facing = getFacing();
        var offsetLocal = Geometry.rotatePosition(new Vec3(0.3, 0.5, 0.3), facing);
        var emitPosition = Vec3.atCenterOf(worldPosition).add(offsetLocal);

        if (level instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.SNOWFLAKE, emitPosition.x, emitPosition.y, emitPosition.z, 1, 1.2, 1.2, 1.2, 0);

    }

    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 1, 1, 1);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 62, 8),
                new GuiSlot(1, 62, 61, true));
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.REFINERY_SCREEN.get();
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
    public ResourceHandler<FluidResource> getFluidLookup(@Nullable Direction direction) {
        return ownStorage.getExternalAccess();
    }

    public SimpleFluidStorage getFluidStorageForModule(BlockPos modulePos) {
        var yDist = modulePos.getY() - this.worldPosition.getY();
        if (yDist == 2) return nodeA;
        if (yDist == 3) return nodeB;
        throw new IllegalStateException("Module needs to be either 1 or 2 blocks above");
    }

    @Override
    public List<Tuple<Component, Component>> getExtraExtensionLabels() {
        return List.of(new Tuple<>(Component.literal("\uD83D\uDCE6: " + moduleCount), Component.translatable("tooltip.oritech.refinery_module_count")));
    }

    @Override
    public BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(8, 7, 18, 71);
    }

    private ResourceHandler<FluidResource> getOutputLookup(int i) {
        if (i == 0) return ownStorage.getOutputContainer();
        if (i == 1) return nodeA;
        if (i == 2) return nodeB;
        throw new IllegalArgumentException("Only has 2 storage modules, tried accessing: " + i);
    }

    public FluidStack getOutputFluid(int i) {
        if (i == 0) return ownStorage.getOutStack();
        if (i == 1) return nodeA.getContent();
        if (i == 2) return nodeB.getContent();
        throw new IllegalArgumentException("Only has 2 storage modules, tried accessing: " + i);
    }

    public long getOutputCapacity(int i) {
        if (i == 0) return ownStorage.getCapacity();
        if (i == 1) return nodeA.getCapacity();
        if (i == 2) return nodeB.getCapacity();
        throw new IllegalArgumentException("Only has 2 storage modules, tried accessing: " + i);
    }

    @Override
    public ColorVariant getDefaultColor() {
        return ColorVariant.FLUXITE;
    }

    // checks if there is an arcane catalyst nearby with at least 1 soul in it
    public Optional<EnchantmentCatalystBlockEntity> getNearbyNonEmptyCatalyst() {
        if (level == null) return Optional.empty();

        for (var checkPos : BlockPos.withinManhattan(worldPosition, 6, 5, 6)) {
            var checkState = level.getBlockState(checkPos);
            if (checkState.getBlock().equals(BlockContent.ENCHANTMENT_CATALYST_BLOCK.get())) {
                var checkEntity = level.getBlockEntity(checkPos, BlockEntitiesContent.ENCHANTMENT_CATALYST_BLOCK_ENTITY.get());
                if (checkEntity.isPresent() && checkEntity.get().collectedSouls > 0) return checkEntity;
            }
        }

        return Optional.empty();
    }

    public void taintTransform() {
        if (level == null) return;

        // remove main cores
        for (var coreBlock : getConnectedCores()) {
            level.removeBlock(coreBlock, false);
        }

        // remove tanks (indexed 1 + 2)
        for (int i = 1; i <= moduleCount; i++) {
            var tankCandidatePos = worldPosition.above(1 + i);
            var tankEntityCandidate = level.getBlockEntity(tankCandidatePos, BlockEntitiesContent.REFINERY_MODULE_ENTITY.get());
            if (tankEntityCandidate.isPresent()) {
                var tankEntity = tankEntityCandidate.get();
                for (var coreBlock : tankEntity.getConnectedCores())
                    level.removeBlock(coreBlock, false);

                level.removeBlock(tankCandidatePos, false);
            }
        }

        level.removeBlock(worldPosition, false);
    }
}
    

