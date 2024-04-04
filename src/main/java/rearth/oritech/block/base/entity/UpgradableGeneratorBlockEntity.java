package rearth.oritech.block.base.entity;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.network.NetworkContent;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class UpgradableGeneratorBlockEntity extends UpgradableMachineBlockEntity {
    
    private int currentMaxBurnTime; // needed only for progress display
    private List<ItemStack> pendingOutputs = new ArrayList<>(); // used if a recipe produces a byproduct at the end
    private HashMap<Direction, BlockApiCache<EnergyStorage, Direction>> directionCaches;
    
    // speed multiplier increases output rate and reduces burn time by same percentage
    // efficiency multiplier only increases burn time
    public UpgradableGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }
    
    private static HashMap<Direction, BlockApiCache<EnergyStorage, Direction>> getNeighborCaches(BlockPos pos, World world) {
        
        var res = new HashMap<Direction, BlockApiCache<EnergyStorage, Direction>>(6);
        
        var topCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.up());
        res.put(Direction.DOWN, topCache);
        var botCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.down());
        res.put(Direction.UP, botCache);
        var northCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.north());
        res.put(Direction.SOUTH, northCache);
        var eastCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.east());
        res.put(Direction.WEST, eastCache);
        var southCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.south());
        res.put(Direction.NORTH, southCache);
        var westCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.west());
        res.put(Direction.EAST, westCache);
        
        return res;
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, MachineBlockEntity blockEntity) {
        
        // check remaining burn time
        // if burn time is zero, try consume item thus adding burn time
        // if burn time is remaining, use up one tick of it
        
        if (world.isClient || !isActive(state)) return;
        
        // progress var is used as remaining burn time
        if (progress > 0) {
            if (canFitEnergy()) {
                
                progress--;
                produceEnergy();
                
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
        var produced = calculateEnergyUsage();
        return energyStorage.capacity >= energyStorage.amount + produced;
    }
    
    // gives energy in this case
    @SuppressWarnings("lossy-conversions")
    protected void produceEnergy() {
        energyStorage.amount += calculateEnergyUsage();
    }
    
    // returns energy production in this case
    @Override
    protected float calculateEnergyUsage() {
        return energyPerTick * (1 / getSpeedMultiplier());
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("storedBurn", currentMaxBurnTime);
        
        var resList = new NbtList();
        for (var stack : pendingOutputs) {
            var data = stack.writeNbt(new NbtCompound());
            resList.add(data);
        }
        nbt.put("pendingResults", resList);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        currentMaxBurnTime = nbt.getInt("currentMaxBurnTime");
        
        var storedResults = nbt.getList("pendingResults", NbtElement.COMPOUND_TYPE);
        for (var elem : storedResults) {
            var compound = (NbtCompound) elem;
            var stack = ItemStack.fromNbt(compound);
            pendingOutputs.add(stack);
        }
        
        if (world != null)
            directionCaches = getNeighborCaches(pos, world);
    }
    
    @Override
    protected void sendNetworkEntry() {
        super.sendNetworkEntry();
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.GeneratorUISyncPacket(getPos(), currentMaxBurnTime));
    }
    
    private void outputEnergy() {
        if (energyStorage.amount <= 0) return;
        var availableOutput = Math.min(energyStorage.amount, energyStorage.maxExtract);
        var totalInserted = 0L;
        
        if (directionCaches == null) directionCaches = getNeighborCaches(pos, world);
        
        try (var tx = Transaction.openOuter()) {
            for (var entry : directionCaches.entrySet()) {
                var insertDirection = entry.getKey().getOpposite();
                var targetCandidate = entry.getValue().find(insertDirection);
                if (targetCandidate == null) continue;
                var inserted = targetCandidate.insert(availableOutput, tx);
                availableOutput -= inserted;
                totalInserted += inserted;
                if (availableOutput <= 0) break;
            }
            energyStorage.extract(totalInserted, tx);
            tx.commit();
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
    public long getDefaultCapacity() {
        return 5000;
    }
    
    @Override
    public long getDefaultInsertRate() {
        return 0;
    }
    
    @Override
    public long getDefaultExtractionRate() {
        return 128;
    }
}
