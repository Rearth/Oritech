package rearth.oritech.block.base.entity;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.processing.CentrifugeBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.energy.EnergyApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class UpgradableGeneratorBlockEntity extends UpgradableMachineBlockEntity {
    
    public int currentMaxBurnTime; // needed only for progress display
    private List<ItemStack> pendingOutputs = new ArrayList<>(); // used if a recipe produces a byproduct at the end
    
    public boolean isProducingSteam = false;
    public final SingleVariantStorage<FluidVariant> waterStorage = createBasicTank(FluidVariant.of(Fluids.WATER));
    public final SingleVariantStorage<FluidVariant> steamStorage = createBasicTank(FluidVariant.of(FluidContent.STILL_STEAM.get()));
    public final Storage<FluidVariant> waterWrapper = FilteringStorage.insertOnlyOf(waterStorage);
    public final Storage<FluidVariant> steamWrapper = FilteringStorage.extractOnlyOf(steamStorage);
    public final Storage<FluidVariant> exposedStorage = new CombinedStorage<>(List.of(steamWrapper, waterWrapper));
    
    // speed multiplier increases output rate and reduces burn time by same percentage
    // efficiency multiplier only increases burn time
    public UpgradableGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, MachineBlockEntity blockEntity) {
        
        // check remaining burn time
        // if burn time is zero, try to consume item thus adding burn time
        // if burn time is remaining, use up one tick of it
        
        if (world.isClient || !isActive(state) || disabledViaRedstone) return;
        
        // progress var is used as remaining burn time
        if (progress > 0) {
            if (canFitEnergy()) {
                
                progress--;
                produceEnergy();
                lastWorkedAt = world.getTime();
                
                if (progress == 0) {
                    burningFinished();
                }
                markDirty();
                markNetDirty();
            }
        } else if (canFitEnergy()) {
            // try consume new item
            tryConsumeInput();
        }
        
        if (networkDirty) {
            updateNetwork();
        }
        
        outputEnergy();
    }
    
    protected void tryConsumeInput() {
        
        if (isProducingSteam && (waterStorage.amount == 0 || steamStorage.amount == steamStorage.getCapacity())) return;
        
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
                var taken = Inventories.splitStack(getInputView(), i, 1);  // amount is not configurable, because ingredient doesn't parse amount in recipe
            }
            pendingOutputs = activeRecipe.getResults();
            
            markNetDirty();
            markDirty();
            
        }
    }
    
    protected void burningFinished() {
        produceResultItems();
    }
    
    protected void produceResultItems() {
        if (!pendingOutputs.isEmpty()) {
            for (var stack : pendingOutputs) {
                this.inventory.addStack(stack);
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
            world.updateNeighborsAlways(addonBlock.pos(), addonBlock.state().getBlock());
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
            produced *= Oritech.CONFIG.generators.rfToSteamRation();
            produced = Math.min(waterStorage.amount, produced);
            steamStorage.amount += produced;
            steamStorage.amount = Math.min(steamStorage.amount, steamStorage.getCapacity());
            waterStorage.amount -= produced;
            
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
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("storedBurn", currentMaxBurnTime);
        nbt.putLong("waterStored", waterStorage.amount);
        nbt.putLong("steamStored", steamStorage.amount);
        nbt.putBoolean("steamAddon", isProducingSteam);
        
        var resList = new NbtList();
        for (var stack : pendingOutputs) {
            var data = stack.encode(registryLookup);
            resList.add(data);
        }
        nbt.put("pendingResults", resList);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        currentMaxBurnTime = nbt.getInt("storedBurn");
        steamStorage.amount = nbt.getLong("steamStored");
        waterStorage.amount = nbt.getLong("waterStored");
        isProducingSteam = nbt.getBoolean("steamAddon");
        
        var storedResults = nbt.getList("pendingResults", NbtElement.COMPOUND_TYPE);
        for (var elem : storedResults) {
            var compound = (NbtCompound) elem;
            var stack = ItemStack.fromNbt(registryLookup, compound).get();
            pendingOutputs.add(stack);
        }
    }
    
    @Override
    protected void sendNetworkEntry() {
        super.sendNetworkEntry();
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.GeneratorUISyncPacket(getPos(), currentMaxBurnTime, isProducingSteam));
        
        if (isProducingSteam)
            NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.GeneratorSteamSyncPacket(pos, steamStorage.amount, waterStorage.amount));
    }
    
    protected abstract Set<Pair<BlockPos, Direction>> getOutputTargets(BlockPos pos, World world);
    
    protected void outputEnergy() {
        if (energyStorage.getAmount() <= 0) return;
        
        // todo caching for targets? Used to be BlockApiCache.create()
        for (var target : getOutputTargets(pos, world)) {
            var candidate = EnergyApi.BLOCK.find(world, target.getLeft(), target.getRight());
            if (candidate != null)
                EnergyApi.transfer(energyStorage, candidate, Long.MAX_VALUE, false);
        }
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
    protected float getAnimationSpeed() {
        
        if (currentMaxBurnTime <= 0) return 1;
        var recipeTicks = currentMaxBurnTime;
        var animationTicks = 60f;    // 3s, length which all animations are defined as
        return animationTicks / recipeTicks * Oritech.CONFIG.generators.animationSpeedMultiplier();
    }
    
    public SingleVariantStorage<FluidVariant> getSteamStorage() {
        return steamStorage;
    }
    
    public SingleVariantStorage<FluidVariant> getWaterStorage() {
        return waterStorage;
    }
    
    private SingleVariantStorage<FluidVariant> createBasicTank(FluidVariant fluidType) {
        return new SingleVariantStorage<>() {
            
            @Override
            protected FluidVariant getBlankVariant() {
                return fluidType;
            }
            
            @Override
            protected boolean canInsert(FluidVariant variant) {
                return variant.equals(fluidType);
            }
            
            @Override
            protected long getCapacity(FluidVariant variant) {
                return CentrifugeBlockEntity.CAPACITY;
            }
            
            @Override
            protected void onFinalCommit() {
                super.onFinalCommit();
                UpgradableGeneratorBlockEntity.this.markDirty();
            }
        };
    }
}
