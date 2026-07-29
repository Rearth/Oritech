package rearth.oritech.block.entity.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
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
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.TaintedRefineryScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeInput;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.Geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class TaintedRefineryBlockEntity extends MultiblockMachineEntity implements FluidProvider {

    @SyncField({SyncType.GUI_TICK, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public final InOutFluidStorage ownStorage = new InOutFluidStorage(16 * FluidType.BUCKET_VOLUME, this::setChanged, new ContainerSlotAssignment(0, 1, 1, 1));

    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public EnvironmentFactor arcaneFactor = EnvironmentFactor.DEFAULT;
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public EnvironmentFactor sculkFactor = EnvironmentFactor.DEFAULT;

    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public int selectedOutput = 0;  // can be 0, 1 or 2 (clickable/changeable via gui). Non-matching outputs are ignored

    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public long lastTickRFUsed = 0; // needed mainly for client UI

    public TaintedRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.TAINTED_REFINERY.get(), pos, state, OritechConfig.processingMachines.refineryData.energyPerTick.get());
    }

    @Override
    protected void workTick() {

        if (!canOutputRecipe(currentRecipe)) return;

        try (var transaction = Transaction.openRoot()) {

            // since we have a matching recipe, enable energy input again
            energyStorage.setMaxInsert(getDefaultInsertRate());
            lastTickRFUsed = energyStorage.getAmountAsLong();

            // needs a minimum amount of RF to work
            if (lastTickRFUsed < OritechConfig.processingMachines.refineryData.energyPerTick.get()) return;

            // use all energy, calculate progression based on amount (and arcane factor)
            var steps = getAndDrainProgress(transaction);

            progress.set(progress.get() + steps, transaction);

            var effectiveRecipeDuration = Math.round(getRecipeDuration() * getSpeedMultiplier());
            while (progress.get() >= effectiveRecipeDuration) {

                try (var inner = Transaction.open(transaction)) {

                    var crafted = onProgressCompleted(inner);

                    if (!crafted) {
                        // Do not queue work that could not be applied to the current inputs/outputs.
                        progress.reset(transaction);
                        break;
                    }

                    progress.set(progress.get() - effectiveRecipeDuration, inner);
                    inner.commit();
                }
            }

            transaction.commit();
            setChanged();
            onProgressed();
            lastWorkedAt = level.getGameTime();
        }


    }

    @Override
    public float getSpeedMultiplier() {
        return super.getSpeedMultiplier() * 0.5f;
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {

        // enabled later again if working
        energyStorage.setMaxInsert(0);
        lastTickRFUsed = 0;

        super.serverTick(serverLevel, pos, state, blockEntity);
    }

    @Override
    protected boolean createCraftingOutputs(Transaction transaction) {
        return createFluidOutputs(transaction) && super.createCraftingOutputs(transaction);
    }

    @Override
    protected boolean removeCraftingInputs(Transaction transaction) {
        return removeFluidInputs(transaction) && super.removeCraftingInputs(transaction);
    }

    @Override
    public List<ItemStackTemplate> getCraftingResults(OritechRecipe activeRecipe) {
        var results = activeRecipe.itemResults();
        if (results.isEmpty()) return List.of();
        return List.of(results.getFirst().withCount(results.getFirst().count() * getOutputMultiplier()));
    }

    public int getOutputMultiplier() {
        // range 1-3 based on sculk factor as yield
        return (int) (1 + (sculkFactor.result * 2.1f));

    }

    public float getArcaneEnergyMultiplier() {
        return (arcaneFactor.result * 8) + 1;
    }

    private boolean createFluidOutputs(Transaction transaction) {

        // create output fluids
        var fluidOutput = calculateOutputFluid(currentRecipe);

        var inserted = ownStorage.getOutputContainer().insert(FluidResource.of(fluidOutput), fluidOutput.amount(), transaction);

        return inserted == fluidOutput.amount();
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

    @Override
    public boolean canOutputRecipe(OritechRecipe recipe) {

        var fluidOutput = calculateOutputFluid(recipe);
        if (!fluidOutput.isEmpty()) {
            try (var simulated = Transaction.openRoot()) {
                var inserted = ownStorage.getOutputContainer().insert(FluidResource.of(fluidOutput), fluidOutput.amount(), simulated);
                if (inserted != fluidOutput.getAmount()) return false;
            }
        }

        return super.canOutputRecipe(recipe);
    }

    // includes the sculk yield bonus
    private FluidStack calculateOutputFluid(OritechRecipe recipe) {

        var fluidOutputs = recipe.fluidOutputs();
        if (fluidOutputs.size() > selectedOutput && !fluidOutputs.isEmpty()) {
            var result = fluidOutputs.get(selectedOutput).create();
            return result.copyWithAmount(result.getAmount() * getOutputMultiplier());
        }
        return FluidStack.EMPTY;

    }

    @Override
    public int getRecipeDuration() {
        return super.getRecipeDuration() * 2;
    }

    private int getAndDrainProgress(Transaction transaction) {
        var availableEnergy = (float) energyStorage.getAmountAsLong();
        energyStorage.set(0, transaction);

        // (remapped from 0-1 to 1-8)
        var energyFactor = getArcaneEnergyMultiplier();
        availableEnergy *= energyFactor;

        return getEnergyInputMapped((int) availableEnergy);
    }

    @Override
    protected OritechRecipeInput getRecipeInput() {
        return new OritechRecipeInput(getInputView(), ownStorage.getInStack());
    }

    @Override
    protected OritechRecipe loadRecipeFromInput(ServerLevel serverLevel, OritechRecipeInput recipeInput, RecipeType<OritechRecipe> type) {
        if (recipeInput.isEmpty()) return OritechRecipe.EMPTY.get();

        if (!currentRecipe.isEmpty() && currentRecipe.recipeType() == type && currentRecipe.matches(recipeInput, level))
            return currentRecipe;

        return serverLevel.getServer().getRecipeManager().recipeMap()
                .getRecipesFor(type, recipeInput, level)
                .max(Comparator.comparingInt(candidate -> candidate.value().itemInputs().size()))
                .map(candidate -> candidate.value())
                .orElse(OritechRecipe.EMPTY.get());
    }

    public int getEnergyInputMapped(long amount) {
        return Math.round(getEnergyFactor(amount));
    }

    public float getEnergyFactor(long amount) {
        return (float) (0.2f * Math.pow(amount, 0.45f));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ownStorage.serialize(output);
        output.putInt("output", selectedOutput);
        output.store("arcane_factor", EnvironmentFactor.CODEC, arcaneFactor);
        output.store("sculk_factor", EnvironmentFactor.CODEC, sculkFactor);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ownStorage.deserialize(input);
        selectedOutput = input.getIntOr("output", 0);
        arcaneFactor = input.read("arcane_factor", EnvironmentFactor.CODEC).orElse(EnvironmentFactor.DEFAULT);
        sculkFactor = input.read("sculk_factor", EnvironmentFactor.CODEC).orElse(EnvironmentFactor.DEFAULT);
    }

    public void afterCreation() {
        if (level == null || !(level instanceof ServerLevel serverLevel)) return;

        for (var targetMachinePosition : getCorePositions()) {
            var rotatedPos = Geometry.rotatePosition(targetMachinePosition, getFacingForMultiblock());
            var checkPos = worldPosition.offset(rotatedPos);
            var checkState = level.getBlockState(checkPos);

            if (checkState.hasBlockEntity()) {
                if (checkState.hasProperty(MachineCoreBlock.USED) && checkState.getValue(MachineCoreBlock.USED)) {
                    Oritech.LOGGER.warn("Unable to auto-create tainted refinery, blocked by block entity. This should never happen");
                    continue;
                }
            }

            level.setBlockAndUpdate(checkPos, BlockContent.COMPLEX_PLATING.get().defaultBlockState());
            level.invalidateCapabilities(checkPos);

        }

        initMultiblock(getBlockState());
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
                sculkPositions.add(pos.immutable());
            } else if (elemState.is(TagContent.REFINERY_ARCANE_BLOCKS) && isVisible(pos)) {
                differentArcaneTypes.add(elemState.getBlock());
                arcanePositions.add(pos.immutable());
            }
        }

        sculkFactor = calculateEnvFactor(sculkPositions.size(), differentSculkTypes.size(), targetCount, targetTypes, sculkPositions);
        arcaneFactor = calculateEnvFactor(arcanePositions.size(), differentArcaneTypes.size(), targetCount, targetTypes, arcanePositions);
        setChanged();

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
            if (checkState.isSolidRender() && !(checkState.getBlock() instanceof MachineCoreBlock))
                return false;
        }

        return true;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new TaintedRefineryScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public BarConfiguration getFluidConfiguration() {
        return new BarConfiguration(7, 6 + 22, 18, 52);
    }

    @Override
    public long getDefaultCapacity() {
        return 500_000_000;
    }

    @Override
    public long getDefaultInsertRate() {
        return 500_000_000;
    }

    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.REFINERY.get();
    }

    @Override
    protected void onProgressed() {
        super.onProgressed();

        if (level.getRandom().nextFloat() > 0.2) return;
        // emit particles
        var facing = getFacing();
        var offsetLocal = Geometry.rotatePosition(new Vec3(0.3, 0.5, -0.3), facing);
        var emitPosition = Vec3.atCenterOf(worldPosition).add(offsetLocal);

        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL, emitPosition.x, emitPosition.y, emitPosition.z, 1, 0.5, 0.5, 0.5, 0);
        }

        var spawnFromCandidates = new ArrayList<>(sculkFactor.sources);
        spawnFromCandidates.addAll(arcaneFactor.sources);

        if (!spawnFromCandidates.isEmpty()) {
            var spawnFrom = spawnFromCandidates.get(level.getRandom().nextInt(spawnFromCandidates.size()));
            ParticleContent.CatalystConnection(level, spawnFrom.getCenter(), emitPosition);
        }
    }

    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 1, 1, 1);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 8, 8),
                new GuiSlot(1, 67 + 1, 8, true));
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.TAINTED_REFINERY_SCREEN.get();
    }

    @Override
    public int getInventorySize() {
        return 2;
    }

    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }

    @Override
    public boolean showExpansionPanel() {
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
                30, 35, 29, 16, true);
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

    @Override
    public void onCoreBroken(BlockPos corePos) {
        onBroken(corePos);
    }

    @Override
    public void onControllerBroken() {
        onBroken(worldPosition);
    }

    private void onBroken(BlockPos eventSource) {

        for (var corePos : getConnectedCores()) {
            if (corePos.equals(eventSource)) continue;
            level.removeBlock(corePos, false);
        }

        if (!eventSource.equals(worldPosition)) {
            var spawnAt = this.worldPosition.getCenter().add(0, 1, 0);
            level.addFreshEntity(new ItemEntity(level, spawnAt.x, spawnAt.y, spawnAt.z, new ItemStack(BlockContent.REFINERY)));
            level.removeBlock(worldPosition, false);
        }

    }

    @Override
    public List<ResourceHandler<FluidResource>> getInteractableFluidStorages() {
        return List.of(ownStorage.getInputContainer(), ownStorage.getOutputContainer());
    }

    public static void handleTankPacket(RefineryTankSelectorPacket payload, IPayloadContext context) {
        var level = context.player().level();
        if (level == null) return;
        var refineryCandidate = level.getBlockEntity(payload.position(), BlockEntitiesContent.TAINTED_REFINERY.get());
        if (refineryCandidate.isEmpty()) return;

        var refinery = refineryCandidate.get();
        refinery.selectedOutput = payload.slot();
        refinery.setChanged();

    }

    // Client -> Server (e.g. from UI interactions)
    public record RefineryTankSelectorPacket(BlockPos position, int slot) implements CustomPacketPayload {

        public static final Type<RefineryTankSelectorPacket> PACKET_ID = new Type<>(Oritech.id("refinery_slot"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }

    public record EnvironmentFactor(float result, int variants, List<BlockPos> sources) {

        public static final Codec<EnvironmentFactor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("result").forGetter(EnvironmentFactor::result),
                Codec.INT.fieldOf("variants").forGetter(EnvironmentFactor::variants),
                BlockPos.CODEC.listOf().fieldOf("sources").forGetter(EnvironmentFactor::sources)
        ).apply(instance, EnvironmentFactor::new));

        public static final EnvironmentFactor DEFAULT = new EnvironmentFactor(0, 0, List.of());

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
    

