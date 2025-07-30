package rearth.oritech.block.base.entity;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.EnergyApi.EnergyStorage;
import rearth.oritech.api.fluid.containers.SimpleInOutFluidStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.recipes.OritechRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Tuple;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public abstract class UpgradableGeneratorBlockEntity extends UpgradableMachineBlockEntity {
    
    @SyncField
    public int currentMaxBurnTime; // needed only for progress display and animation speed
    private List<ItemStack> pendingOutputs = new ArrayList<>(); // used if a recipe produces a byproduct at the end
    
    // this is used just for steam
    @SyncField(SyncType.GUI_OPEN)
    public boolean isProducingSteam = false;
    @SyncField(SyncType.GUI_TICK)
    public final SimpleInOutFluidStorage boilerStorage = new SimpleInOutFluidStorage(8 * FluidStackHooks.bucketAmount(), this::setChanged) {
        @Override
        public long insert(FluidStack toInsert, boolean simulate) {
            if (!boilerAcceptsInput(toInsert.getFluid())) return 0L;
            return super.insert(toInsert, simulate);
        }
    };
    
    // speed multiplier increases output rate and reduces burn time by same percentage
    // efficiency multiplier only increases burn time
    public UpgradableGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        // check remaining burn time
        // if burn time is zero, try to consume item thus adding burn time
        // if burn time is remaining, use up one tick of it
        
        if (world.isClientSide || !isActive(state) || disabledViaRedstone) return;
        
        // progress var is used as remaining burn time
        if (progress > 0) {
            if (canFitEnergy()) {
                
                progress--;
                produceEnergy();
                lastWorkedAt = world.getGameTime();
                
                if (progress == 0) {
                    burningFinished();
                }
                setChanged();
            }
        } else if (canFitEnergy()) {
            // try consume new item
            tryConsumeInput();
        }
        
        outputEnergy();
    }
    
    protected void tryConsumeInput() {
        
        if (isProducingSteam && (boilerStorage.getInStack().getAmount() == 0 || boilerStorage.getOutStack().getAmount() >= boilerStorage.getCapacity())) return;
        
        var recipeCandidate = getRecipe();
        if (recipeCandidate.isEmpty())
            currentRecipe = OritechRecipe.DUMMY;     // reset recipe when invalid or no input is given
        
        
        if (recipeCandidate.isPresent()) {
            // this is separate so that progress is not reset when out of energy
            var activeRecipe = recipeCandidate.get().value();
            currentRecipe = activeRecipe;
            
            // speed -> lower = faster, efficiency -> lower = better
            var recipeTime = (int) (currentRecipe.getTime() * getSpeedMultiplier() * (1 / getEfficiencyMultiplier()));
            progress = recipeTime;
            currentMaxBurnTime = recipeTime;
            
            // remove inputs
            for (int i = 0; i < activeRecipe.getInputs().size(); i++) {
                var taken = ContainerHelper.removeItem(getInputView(), i, 1);  // amount is not configurable, because ingredient doesn't parse amount in recipe
            }
            pendingOutputs = activeRecipe.getResults();
            
            setChanged();
            
        }
    }
    
    protected void burningFinished() {
        produceResultItems();
    }
    
    protected void produceResultItems() {
        if (!pendingOutputs.isEmpty()) {
            for (var stack : pendingOutputs) {
                this.inventory.insert(stack, false);
            }
        }
        
        pendingOutputs.clear();
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
    
    // check if the energy can fit
    protected boolean canFitEnergy() {
        if (isProducingSteam) return true;
        var produced = calculateEnergyUsage();
        return energyStorage.capacity >= energyStorage.amount + produced;
    }
    
    // gives energy in this case
    @SuppressWarnings("lossy-conversions")
    protected void produceEnergy() {
        var produced = calculateEnergyUsage();
        if (isProducingSteam) {
            // yes this will void excess steam. Generators will only stop producing when the RF storage is full, not the steam storage
            // this is by design and supposed to be one of the negatives of steam production
            produced *= Oritech.CONFIG.generators.steamEngineData.rfToSteamRatio();
            produced *= SteamEngineEntity.STEAM_AMOUNT_MULTIPLIER;
            
            var extracted = boilerStorage.getInputContainer().extract(FluidStack.create(Fluids.WATER.getSource(), Math.round(produced)), false);
            boilerStorage.getOutputContainer().insert(FluidStack.create(SteamEngineEntity.getUsedSteamFluid(), extracted), false);
        } else {
            energyStorage.amount += produced;
        }
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
    
    protected abstract Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level world);
    
    protected void outputEnergy() {
        if (energyStorage.getAmount() <= 0) return;
        
        var moved = 0L;
        
        // todo caching for targets? Used to be BlockApiCache.create()
        for (var target : getOutputTargets(worldPosition, level)) {
            var candidate = EnergyApi.BLOCK.find(level, target.getA(), target.getB());
            if (candidate != null)
                moved += EnergyApi.transfer(energyStorage, candidate, Long.MAX_VALUE, false);
        }
        
        if (moved > 0)
            this.setChanged();
        
    }
    
    public boolean boilerAcceptsInput(Fluid fluid ){
        return fluid.equals(Fluids.WATER);
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
        if (this.energyStorage.maxExtract <= 0 && !isProducingSteam) return false;
        return super.showEnergy();
    }
    
    @Override
    protected float getAnimationSpeed() {
        
        if (currentMaxBurnTime <= 0) return 1;
        var recipeTicks = currentMaxBurnTime;
        var animationTicks = 60f;    // 3s, length which all animations are defined as
        return animationTicks / recipeTicks * Oritech.CONFIG.generators.animationSpeedMultiplier();
    }
}
