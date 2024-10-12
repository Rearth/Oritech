package rearth.oritech.block.entity.machines.generators;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.base.entity.FluidMultiblockGeneratorBlockEntity;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventorySlotAssignment;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.List;

// progress is abused to sync active speed.
public class SteamEngineEntity extends FluidMultiblockGeneratorBlockEntity {
    
    private static final int MAX_SPEED = 10;
    
    private final Storage<FluidVariant> waterOutputWrapper = FilteringStorage.extractOnlyOf(waterStorage);
    private final Storage<FluidVariant> steamWrapperInput = FilteringStorage.insertOnlyOf(inputTank);
    private final Storage<FluidVariant> exposedSteamEngineStorage = new CombinedStorage<>(List.of(steamWrapperInput, waterOutputWrapper));
    
    private final ArrayList<SteamEngineEntity> connectedTanks = new ArrayList<>();
    private SteamEngineEntity cachedTargetTank = null;
    
    public int energyProducedTick = 0;
    
    public SteamEngineEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.STEAM_ENGINE_ENTITY, pos, state, Oritech.CONFIG.generators.steamEngineData.energyPerTick());
    }
    
    @Override
    public void initAddons() {
        setupTankCache();
        cachedTargetTank = reloadTargetTankFromCache();
    }
    
    @Override
    public boolean initMultiblock(BlockState state) {
        setupTankCache();
        return super.initMultiblock(state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, MachineBlockEntity blockEntity) {
        
        if (world.isClient || !isActive(state)) return;
        
        progress = 0;
        var usedTankEntity = cachedTargetTank;
        
        if (usedTankEntity == null) {
            if (world.getTime() % 40 == 0) {
                setupTankCache();
                cachedTargetTank = reloadTargetTankFromCache();
            }
            return;
        }
        
        var usedSteamTank = usedTankEntity.inputTank;
        var usedWaterTank = usedTankEntity.waterStorage;
        var usedEnergyStorage = usedTankEntity.energyStorage;
        
        if (usedSteamTank.amount == 0 || usedWaterTank.amount == usedWaterTank.getCapacity()) return;
        
        if (currentRecipe == OritechRecipe.DUMMY) {
            var candidate = getRecipe(usedSteamTank);
            if (candidate.isEmpty()) return;
            var recipe = candidate.get().value();
            if (usedSteamTank.variant != recipe.getFluidInput().variant()) return;
            currentRecipe = recipe;
        }
        
        var speed = getSteamProcessingSpeed(usedSteamTank);
        
        var consumed = Math.max(1, currentRecipe.getFluidInput().amount() * speed);
        usedSteamTank.amount -= consumed;
        usedWaterTank.amount += consumed * 0.9f;
        usedWaterTank.amount = Math.min(usedWaterTank.amount, usedWaterTank.getCapacity());
        progress = (int) (speed * 100);
        
        var energyEfficiency = getSteamEnergyEfficiency(speed);
        var energyProduced = consumed * energyEfficiency * energyPerTick;
        usedEnergyStorage.amount = (long) Math.min(usedEnergyStorage.amount + energyProduced, usedEnergyStorage.capacity);
        usedTankEntity.energyProducedTick += (int) energyProduced;
        
        setBaseAddonData(new BaseAddonData(1 / (speed), 1 / energyEfficiency, 0, 0));
        
        spawnParticles();
        lastWorkedAt = world.getTime();
        
        markDirty();
        markNetDirty();
        outputEnergy();
        
        if (networkDirty) {
            updateNetwork();
        }
    }
    
    private void spawnParticles() {
        if (world.random.nextFloat() > 0.4) return;
        // emit particles
        var facing = getFacing();
        var offsetLocal = Geometry.rotatePosition(new Vec3d(0, 0, -0.5), facing);
        var emitPosition = Vec3d.ofCenter(pos).add(offsetLocal);
        
        ParticleContent.STEAM_ENGINE_WORKING.spawn(world, emitPosition, 1);
    }
    
    private float getSteamEnergyEfficiency(float x) {
        // basically a curve that goes through 0:0.5, 7:1 and 10:0.2
        return (float) (0.5f - 0.1966667f * x + 0.09166667f * Math.pow(x, 2) - 0.0075f * Math.pow(x, 3)) + 0.4f;
    }
    
    private void setupTankCache() {
        connectedTanks.clear();
        var facing = getFacing();
        
        // collect tanks (in both directions)
        for (int i = 1; i <= 10; i++) {
            if (!tryAddTank(facing, i)) break;
        }
        
        for (int i = 1; i <= 10; i++) {
            if (!tryAddTank(facing, -i)) break;
        }
    }
    
    private boolean tryAddTank(Direction facing, int i) {
        var checkPos = new BlockPos(Geometry.offsetToWorldPosition(facing, new Vec3i(i, 0, 0), pos));
        var state = world.getBlockState(checkPos);
        
        // redirect in case of machine core
        if (state.getBlock() instanceof MachineCoreBlock) {
            checkPos = MachineCoreBlock.getControllerPos(world, checkPos);
            state = world.getBlockState(checkPos);
        }
        
        if (state.getBlock().equals(BlockContent.STEAM_ENGINE_BLOCK) && state.get(MultiblockMachine.ASSEMBLED)) {
            connectedTanks.add(((SteamEngineEntity) world.getBlockEntity(new BlockPos(checkPos))));
        } else {
            return false;
        }
        return true;
    }
    
    private SteamEngineEntity reloadTargetTankFromCache() {
        var res = getBestInputFromConnectedEngine();
        if (res.inputTank.amount == 0) return null;
        return res;
    }
    
    private SteamEngineEntity getBestInputFromConnectedEngine() {
        
        var res = this;
        var highest = inputTank.amount;
        
        for (var tank : connectedTanks) {
            
            if (tank == null) {
                connectedTanks.clear();
                connectedTanks.add(this);
                return this;
            }
            
            var tankAmount = tank.inputTank.amount;
            if (tankAmount > highest) {
                highest = tankAmount;
                res = tank;
            }
        }
        
        return res;
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
    }
    
    private float getSteamProcessingSpeed(SingleVariantStorage<FluidVariant> usedTank) {
        var fillPercentage = usedTank.amount / (float) usedTank.getCapacity();
        return fillPercentage * MAX_SPEED;
    }
    
    @Override
    protected float getAnimationSpeed() {
        if (progress == 0) return 1;
        return (float) progress / 100;
    }
    
    @Override
    public BarConfiguration getFluidConfiguration() {
        return new BarConfiguration(149, 24, 15, 54);
    }
    
    @Override
    protected void sendNetworkEntry() {
        super.sendNetworkEntry();
        var data = getBaseAddonData();
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.SteamEnginePacket(pos, data.speed(), data.efficiency(), waterStorage.amount, energyProducedTick));
        energyProducedTick = 0;
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.STEAM_ENGINE;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 0, 0, 0);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of();
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.STEAM_ENGINE_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 0;
    }
    
    @Override
    public boolean bucketInputAllowed() {
        return false;
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.generators.steamEngineData.energyCapacity();
    }
    
    @Override
    public long getDefaultExtractionRate() {
        return Oritech.CONFIG.generators.steamEngineData.maxEnergyExtraction();
    }
    
    @Override
    protected Multimap<Direction, BlockApiCache<EnergyStorage, Direction>> getNeighborCaches(BlockPos pos, World world) {
        
        var facing = getFacing();
        
        var res = ArrayListMultimap.<Direction, BlockApiCache<EnergyStorage, Direction>>create();
        var northCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.north());
        res.put(Direction.NORTH, northCache);
        var eastCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.east());
        res.put(Direction.EAST, eastCache);
        var southCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.south());
        res.put(Direction.SOUTH, southCache);
        var westCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.west());
        res.put(Direction.WEST, westCache);
        
        res.removeAll(facing.rotateYCounterclockwise());
        
        return res;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of();
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1, 0),
          new Vec3i(0, 0, -1),
          new Vec3i(0, 1, -1)
        );
    }
    
    @Override
    public Storage<FluidVariant> getFluidStorage(Direction direction) {
        return exposedSteamEngineStorage;
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
}
