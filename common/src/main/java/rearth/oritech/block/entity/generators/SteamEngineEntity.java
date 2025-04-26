package rearth.oritech.block.entity.generators;

import dev.architectury.fluid.FluidStack;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.base.entity.FluidMultiblockGeneratorBlockEntity;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.MultiblockGeneratorBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// progress is abused to sync active speed.
public class SteamEngineEntity extends MultiblockGeneratorBlockEntity implements FluidApi.BlockProvider {
    
    private static final int MAX_SPEED = 10;
    private static final int MAX_CHAIN_SIZE = 20;
    private static final float WATER_RATIO = 0.9f;
    
    // how chaining works:
    // (non-chained non-empty) generator checks neighbors in both sides in N dist
    // generator marks neighbors as chained, with timestamp
    // slaved generator shows only chain notice in popup, moves all inserted steam to master (get api returns master entries)
    // master processes at X rate, shows chained count in UI
    
    // progress is used to store/sync animation speed
    
    public long masterHeartbeat; // set from master, used by slave
    public SteamEngineEntity master;
    
    private final Set<SteamEngineEntity> slaves = new HashSet<>();
    
    // client only
    public NetworkContent.SteamEngineSyncPacket clientStats;
    
    public SteamEngineEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.STEAM_ENGINE_ENTITY, pos, state, Oritech.CONFIG.generators.steamEngineData.steamToRfRatio());
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, MachineBlockEntity blockEntity) {
        
        if (world.isClient || !isActive(state)) return;
        
        var slaved = inSlaveMode();
        var hasInput = !boilerStorage.getInStack().isEmpty();
        if (world.getTime() % 80 == 0 && !slaved && hasInput)
            setupMaster();
        
        if (!slaved && hasInput) tickMaster();
        
        if (slaved) tickSlave();
        
        
        outputEnergy();
        if (networkDirty)
            updateNetwork();
    }
    
    // this is only called when steam is available
    private void tickMaster() {
        
        var steamTank = boilerStorage.getInputContainer();
        var waterTank = boilerStorage.getOutputContainer();
        
        // optional config stops (energy full / water full)
        if (energyStorage.getAmount() >= energyStorage.getCapacity() && Oritech.CONFIG.generators.steamEngineData.stopOnEnergyFull())
            return;
        if (waterTank.getStack().getAmount() >= waterTank.getCapacity() && Oritech.CONFIG.generators.steamEngineData.stopOnWaterFull())
            return;
        
        // if not recipe is currently set, or it does not match the steam tank, search for a recipe
        if (currentRecipe == OritechRecipe.DUMMY || !currentRecipe.getFluidInput().matchesFluid(steamTank.getStack())) {
            var candidate = FluidMultiblockGeneratorBlockEntity.getRecipe(steamTank, world, getOwnRecipeType());
            candidate.ifPresent(recipe -> currentRecipe = recipe.value());
            if (candidate.isEmpty()) return;
            currentRecipe = candidate.get().value();
        }
        
        var speed = getSteamProcessingSpeed(steamTank);
        var workerCount = slaves.size() + 1;
        
        var consumedCount = currentRecipe.getFluidInput().amount() * speed * workerCount;
        var producedCount = consumedCount * WATER_RATIO;
        
        // update tanks
        steamTank.extract(steamTank.getStack().copyWithAmount((long) consumedCount), false);
        waterTank.insert(FluidStack.create(Fluids.WATER, (long) producedCount), false);
        
        // produce energy
        var energyEfficiency = getSteamEnergyEfficiency(speed);
        var energyProduced = consumedCount * energyEfficiency * energyPerTick;
        energyStorage.insertIgnoringLimit((long) energyProduced, false);
        
        spawnParticles();
        lastWorkedAt = world.getTime();
        
        // used for animation speed
        progress = (int) (speed * 100f);
        
        // order/data: speed, efficiency, rf produced, steam consumed, slave count
        clientStats = new NetworkContent.SteamEngineSyncPacket(pos, speed, energyEfficiency, (long) energyProduced, (long) consumedCount, slaves.size());
        this.markDirty();
        
    }
    
    private void tickSlave() {
        // check if master is actually working
        var masterStats = master.clientStats;
        var wasWorking = master.isActivelyWorking();
        var speed = masterStats.speed();
        
        if (wasWorking) {
            spawnParticles();
            this.lastWorkedAt = world.getTime();
            this.markDirty();
        }
        
        // used for animation speed
        progress = (int) (speed * 100f);
    }
    
    private void setupMaster() {
        slaves.clear();
        
        for (int direction = -1; direction <= 1; direction++) {
            if (direction == 0) continue;
            for (int i = 1; i <= MAX_CHAIN_SIZE; i++) {
                var checkPos = new BlockPos(Geometry.offsetToWorldPosition(getFacing(), new Vec3i(i * direction, 0, 0), pos));
                
                var coreCandidate = world.getBlockEntity(checkPos, BlockEntitiesContent.MACHINE_CORE_ENTITY);
                if (coreCandidate.isPresent() && coreCandidate.get().getCachedController() != null)
                    checkPos = coreCandidate.get().getControllerPos();
                
                var candidate = world.getBlockEntity(checkPos, BlockEntitiesContent.STEAM_ENGINE_ENTITY);
                if (candidate.isEmpty() || !candidate.get().isActive(candidate.get().getCachedState())) {
                    break;
                } else if (!candidate.get().boilerStorage.getInStack().isEmpty()) {
                    break;
                } else {
                    var slave = candidate.get();
                    slaves.add(slave);
                    slave.masterHeartbeat = world.getTime();
                    slave.master = this;
                }
            }
        }
    }
    
    public boolean inSlaveMode() {
        var heartbeatAge = world.getTime() - masterHeartbeat;
        return heartbeatAge <= 100 && master != null && !master.isRemoved();
    }
    
    @Override
    public boolean boilerAcceptsInput(Fluid fluid) {
        return fluid.equals(FluidContent.STILL_STEAM.get());
    }
    
    private void spawnParticles() {
        if (world.random.nextFloat() > 0.5) return;
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
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
    }
    
    private float getSteamProcessingSpeed(FluidApi.SingleSlotStorage usedTank) {
        var fillPercentage = usedTank.getStack().getAmount() / (float) usedTank.getCapacity();
        return fillPercentage * MAX_SPEED;
    }
    
    @Override
    protected float getAnimationSpeed() {
        if (progress == 0) return 1;
        return (float) progress / 100f;
    }
    
    @Override
    public BarConfiguration getFluidConfiguration() {
        return new BarConfiguration(149, 10, 18, 64);
    }
    
    @Override
    protected void sendNetworkEntry() {
        super.sendNetworkEntry();
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.GeneratorSteamSyncPacket(pos, boilerStorage.getInStack().getAmount(), boilerStorage.getOutStack().getAmount()));
        
        if (clientStats != null) NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(clientStats);
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.STEAM_ENGINE;
    }
    
    @Override
    public InventorySlotAssignment getSlotAssignments() {
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
    public long getDefaultCapacity() {
        return Oritech.CONFIG.generators.steamEngineData.energyCapacity();
    }
    
    @Override
    public long getDefaultExtractionRate() {
        return Oritech.CONFIG.generators.steamEngineData.maxEnergyExtraction();
    }
    
    @Override
    protected Set<Pair<BlockPos, Direction>> getOutputTargets(BlockPos pos, World world) {
        
        var res = new HashSet<Pair<BlockPos, Direction>>();
        
        var facing = getFacingForAddon();
        var posA = new Vec3i(0, 0, 1); // front
        var posB = new Vec3i(-1, 0, 0); // right
        var posC = new Vec3i(1, 0, 0);  // left
        var posD = new Vec3i(-1, 0, -1); // back left
        var posE = new Vec3i(1, 0, -1); // back right
        var posF = new Vec3i(0, 0, -2);  // back
        var worldPosA = (BlockPos) Geometry.offsetToWorldPosition(facing, posA, pos);
        var worldPosB = (BlockPos) Geometry.offsetToWorldPosition(facing, posB, pos);
        var worldPosC = (BlockPos) Geometry.offsetToWorldPosition(facing, posC, pos);
        var worldPosD = (BlockPos) Geometry.offsetToWorldPosition(facing, posD, pos);
        var worldPosE = (BlockPos) Geometry.offsetToWorldPosition(facing, posE, pos);
        var worldPosF = (BlockPos) Geometry.offsetToWorldPosition(facing, posF, pos);
        
        res.add(new Pair<>(worldPosA, Geometry.fromVector(Geometry.getForward(facing))));
        res.add(new Pair<>(worldPosB, Geometry.fromVector(Geometry.getLeft(facing))));
        res.add(new Pair<>(worldPosC, Geometry.fromVector(Geometry.getRight(facing))));
        res.add(new Pair<>(worldPosD, Geometry.fromVector(Geometry.getLeft(facing))));
        res.add(new Pair<>(worldPosE, Geometry.fromVector(Geometry.getRight(facing))));
        res.add(new Pair<>(worldPosF, Geometry.fromVector(Geometry.getBackward(facing))));
        
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
    public boolean showProgress() {
        return false;
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(@Nullable Direction direction) {
        if (inSlaveMode()) return master.boilerStorage.getStorageForDirection(direction);
        return boilerStorage.getStorageForDirection(direction);
    }
}
