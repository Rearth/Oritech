package rearth.oritech.block.base.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.fluid.InOutFluidStorage;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class UpgradableGeneratorBlockEntity extends UpgradableMachineBlockEntity {

    @SyncField
    public int currentMaxBurnTime; // needed only for progress display and animation speed

    // this is used just for steam
    @SyncField(SyncType.GUI_OPEN)
    public boolean isProducingSteam = false;
    @SyncField(SyncType.GUI_TICK)
    public final InOutFluidStorage boilerStorage = new InOutFluidStorage(OritechConfig.generators.steamEngineData.steamBoilerCapacityBuckets.get() * 1000, this::setChanged, new ContainerSlotAssignment(0, 1, 1, 1)) {

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (index == 0 && !boilerAcceptsInput(resource)) return 0;
            return super.insert(index, resource, amount, transaction);
        }
    };

    private List<BlockCapabilityCache<EnergyHandler, Direction>> cachedOutputTargets = List.of();

    // speed multiplier increases output rate and reduces burn time by same percentage
    // efficiency multiplier only increases burn time
    public UpgradableGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {

        // check remaining burn time
        // if burn time is zero, try to consume item thus adding burn time
        // if burn time is remaining, use up one tick of it

        if (!isAssembled(state) || disabledViaRedstone) return;

        workTick();

        outputEnergy();
    }

    @Override
    protected void workTick() {
        try (var transaction = Transaction.openRoot()) {

            // if not burning anything, try start new burn
            if (progress.isEmpty()) {
                if (!consumeInput(transaction)) return;
            }

            // produce energy
            if (!produceEnergy(transaction)) return;

            // use up stored burn progress
            progress.decrement(transaction);

            if (progress.isEmpty()) {
                if (!burningFinished(transaction)) return;
            }

            transaction.commit();
            setChanged();

        }
    }

    @SuppressWarnings("RedundantIfStatement")
    protected boolean consumeInput(Transaction transaction) {

        // steam input empty or output full
        if (isProducingSteam && (boilerStorage.getAmountAsInt(0) <= 0 || boilerStorage.getAmountAsInt(1) >= boilerStorage.getCapacity()))
            return false;

        currentRecipe = this.findActiveRecipe();

        if (currentRecipe.isEmpty()) return false;

        // speed -> lower = faster, efficiency -> lower = better
        var recipeTime = (int) (currentRecipe.time() * getSpeedMultiplier() * (1 / getEfficiencyMultiplier()));
        progress.set(recipeTime, transaction);
        currentMaxBurnTime = recipeTime;

        // take recipe inputs (could also be fluids)
        if (!removeRecipeInputs(transaction)) return false;

        return true;
    }

    protected boolean removeRecipeInputs(Transaction transaction) {

        // remove items from input
        var inputInv = inventory.getInputContainer();
        for (var ingredient : currentRecipe.itemInputs()) {

            var found = false;

            for (int i = 0; i < inputInv.size(); i++) {
                var slotItem = inputInv.getResource(i);
                if (ingredient.test(slotItem.toStack())) {
                    var taken = inputInv.extract(i, slotItem, 1, transaction);
                    if (taken != 1) return false;   // this should never happen
                    found = true;
                    break;
                }
            }

            if (!found) return false;

        }

        return true;
    }

    // gives energy in this case
    @SuppressWarnings("lossy-conversions")
    protected boolean produceEnergy(Transaction transaction) {
        var produced = (int) calculateEnergyUsage();


        if (isProducingSteam) {
            // yes this will void excess steam. Generators will only stop producing when the RF storage is full, not the steam storage
            // this is by design and supposed to be one of the negatives of steam production

            produced *= OritechConfig.generators.steamEngineData.rfToSteamRatio.get();
            produced *= SteamEngineEntity.STEAM_AMOUNT_MULTIPLIER;

            var extracted = boilerStorage.getInputContainer().extract(FluidResource.of(Fluids.WATER), Math.round(produced), transaction);
            boilerStorage.getOutputContainer().insert(SteamEngineEntity.getUsedSteamFluid(), extracted, transaction);

        } else {
            var inserted = energyStorage.internalInsert(produced, transaction);
            return inserted >= 1; // allows it to fully fill, potentially loosing some RF, but failing if nothing was inserted (e.g. inserted doesnt have to equal produced)
        }

        return true;
    }

    protected boolean burningFinished(Transaction transaction) {
        progress.reset(transaction);
        return true;
    }

    @Override
    public void gatherAddonStats(List<AddonBlock> addons) {
        isProducingSteam = false;
        super.gatherAddonStats(addons);
    }

    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        super.getAdditionalStatFromAddon(addonBlock);
        if (addonBlock.state().getBlock() == BlockContent.STEAM_BOILER_ADDON.get()) {
            isProducingSteam = true;
            level.updateNeighborsAt(addonBlock.pos(), addonBlock.state().getBlock());
        }
    }

    // ensure that insertion is disabled, and instead upgrade extraction rates
    @Override
    public void updateEnergyContainer() {
        super.updateEnergyContainer();

        var insert = energyStorage.maxInsert;
        energyStorage.maxExtract = getDefaultExtractionRate() + insert;
        energyStorage.maxInsert = 0;

    }

    @Override
    public List<ResourceHandler<FluidResource>> getInteractableFluidStorages() {
        if (!isProducingSteam)
            return super.getInteractableFluidStorages();

        var result = new ArrayList<>(super.getInteractableFluidStorages());
        result.add(boilerStorage.getInputContainer());
        result.add(boilerStorage.getOutputContainer());

        return result;
    }

    // returns energy production in this case
    @Override
    protected float calculateEnergyUsage() {
        return energyPerTick * (1 / getSpeedMultiplier());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putInt("storedBurn", currentMaxBurnTime);
        output.putBoolean("steamed", isProducingSteam);

        boilerStorage.serialize(output);

    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        currentMaxBurnTime = input.getIntOr("storedBurn", 0);
        isProducingSteam = input.getBooleanOr("steamed", false);
        boilerStorage.deserialize(input);

    }

    protected abstract Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level level);

    protected void outputEnergy() {
        if (energyStorage.getAmountAsLong() <= 0 || !(level instanceof ServerLevel serverLevel)) return;

        var moved = 0L;

        if (cachedOutputTargets.isEmpty()) {
            cachedOutputTargets = getOutputTargets(worldPosition, level).stream()
                    .map(target -> BlockCapabilityCache.create(Capabilities.Energy.BLOCK, serverLevel, target.getA(), target.getB()))
                    .toList();
        }

        var available = energyStorage.getAmountAsLong();

        try (var transaction = Transaction.openRoot()) {
            for (var target : cachedOutputTargets) {
                var candidate = target.getCapability();
                if (candidate != null) {
                    moved += candidate.insert((int) Math.min(available, energyStorage.maxExtract), transaction);
                    available -= moved;
                }

                if (available <= 0) break;
            }

            if (moved > 0) {
                energyStorage.internalExtract((int) moved, transaction);
                transaction.commit();
                this.setChanged();
            }
        }

    }

    public boolean boilerAcceptsInput(FluidResource fluid) {
        return fluid.is(Fluids.WATER);
    }

    @Override
    public float getProgress() {
        return 1 - ((float) progress.get() / currentMaxBurnTime);
    }

    @Override
    public long getDefaultInsertRate() {
        return 0;
    }

    @Override
    public float getDisplayedEnergyTransfer() {
        return energyStorage.maxExtract;
    }

    @Override
    public boolean showEnergy() {
        if (isProducingSteam) return false;
        return super.showEnergy();
    }

    @Override
    protected float getAnimationSpeed() {

        if (currentMaxBurnTime <= 0) return 1;
        var recipeTicks = currentMaxBurnTime;
        var animationTicks = 60f;    // 3s, length which all animations are defined as
        return animationTicks / recipeTicks * OritechConfig.generators.animationSpeedMultiplier.get().floatValue();
    }
}
