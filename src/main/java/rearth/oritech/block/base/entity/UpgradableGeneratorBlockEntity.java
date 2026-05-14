package rearth.oritech.block.base.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Tuple;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
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
import rearth.oritech.init.recipes.OritechRecipeInput;
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
    public final InOutFluidStorage boilerStorage = new InOutFluidStorage((int) (OritechConfig.generators.steamEngineData.steamBoilerCapacityBuckets.get() * 1000), this::setChanged, new ContainerSlotAssignment(0, 1, 1, 1)) {
        
        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (!boilerAcceptsInput(resource)) return 0;
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
    public void serverTick(Level level, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
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
    
    private boolean removeRecipeInputs(Transaction transaction) {
        
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
            
            var extracted = boilerStorage.getInputContainer().extract(FluidStack.create(Fluids.WATER.getSource(), Math.round(produced)), false);
            boilerStorage.getOutputContainer().insert(FluidStack.create(SteamEngineEntity.getUsedSteamFluid(), extracted), false);
            
        } else {
            var inserted = energyStorage.internalInsert(produced, transaction);
            if (inserted < 1) return false; // allows it to fully fill, potentially loosing some RF, but failing if nothing was inserted
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
        if (addonBlock.state().getBlock() == BlockContent.STEAM_BOILER_ADDON) {
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
    public List<FluidApi.SingleSlotStorage> getInteractableFluidStorages() {
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
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        nbt.putInt("storedBurn", currentMaxBurnTime);
        boilerStorage.writeNbt(nbt, "");
        nbt.putBoolean("steamAddon", isProducingSteam);
        
        var resList = new ListTag();
        for (var stack : pendingOutputs) {
            var data = stack.save(registryLookup);
            resList.add(data);
        }
        nbt.put("pendingResults", resList);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        currentMaxBurnTime = nbt.getInt("storedBurn");
        boilerStorage.readNbt(nbt, "");
        isProducingSteam = nbt.getBoolean("steamAddon");
        
        var storedResults = nbt.getList("pendingResults", Tag.TAG_COMPOUND);
        for (var elem : storedResults) {
            var compound = (CompoundTag) elem;
            var stack = ItemStack.parse(registryLookup, compound).get();
            pendingOutputs.add(stack);
        }
    }
    
    @Override
    protected OritechRecipeInput getRecipeInput() {
        return super.getRecipeInput();
    }
    
    protected abstract Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level level);
    
    protected void outputEnergy() {
        if (energyStorage.getAmount() <= 0) return;
        
        var moved = 0L;
        
        if (cachedOutputTargets.isEmpty()) {
            cachedOutputTargets = getOutputTargets(worldPosition, level).stream()
                                    .map(target -> EnergyApi.BLOCK.createCache(level, target.getA(), target.getB()))
                                    .toList();
        }
        
        for (var target : cachedOutputTargets) {
            var candidate = target.find();
            if (candidate != null)
                moved += EnergyApi.transfer(energyStorage, candidate, Long.MAX_VALUE, false);
        }
        
        if (moved > 0)
            this.setChanged();
        
    }
    
    public boolean boilerAcceptsInput(FluidResource fluid) {
        return fluid.is(Fluids.WATER);
    }
    
    @Override
    public float getProgress() {
        return 1 - ((float) progress / currentMaxBurnTime);
    }
    
    public int getCurrentMaxBurnTime() {
        return currentMaxBurnTime;
    }
    
    public void setCurrentMaxBurnTime(int currentMaxBurnTime) {
        this.currentMaxBurnTime = currentMaxBurnTime;
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
