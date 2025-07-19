package rearth.oritech.block.entity.storage;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DelegatingEnergyStorage;
import rearth.oritech.api.energy.containers.DynamicStatisticEnergyStorage;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.blocks.storage.UnstableContainerBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class UnstableContainerBlockEntity extends NetworkedBlockEntity implements ScreenProvider, ExtendedMenuProvider,
                                                                           GeoBlockEntity, MultiblockMachineController, EnergyApi.BlockProvider {
    
    public static final RawAnimation SETUP = RawAnimation.begin().thenPlay("setup").thenPlay("idle");
    public static final RawAnimation IDLE = RawAnimation.begin().thenPlay("idle");
    
    public static final Long BASE_CAPACITY = Oritech.CONFIG.unstableContainerBaseCapacity();
    
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    @SyncField(SyncType.GUI_OPEN)
    public BlockState capturedBlock = Blocks.AIR.getDefaultState();
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    public float qualityMultiplier = 1f;
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    public DynamicStatisticEnergyStorage.EnergyStatistics currentStats;
    
    private long age = 0;
    private boolean dropped = false;
    
    // scaling storage
    public final SimpleEnergyStorage laserInputStorage = new SimpleEnergyStorage(100_000_000, 0, 100_000_000);
    
    //own storage
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    protected final DynamicStatisticEnergyStorage energyStorage = new DynamicStatisticEnergyStorage(20_000_000L, 20_000_000L, 20_000_000L, this::markDirty);
    
    private final EnergyApi.EnergyStorage outputStorage = new DelegatingEnergyStorage(energyStorage, null) {
        @Override
        public boolean supportsInsertion() {
            return false;
        }
    };
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    public UnstableContainerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.UNSTABLE_CONTAINER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void serverTick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        age++;
        if (age > 10 && !state.get(UnstableContainerBlock.SETUP_DONE)) {
            world.setBlockState(pos, state.with(UnstableContainerBlock.SETUP_DONE, true));
        }
        
        energyStorage.tick((int) world.getTime());
        
        adjustEnergyStorageSize();
        
        if (energyStorage.amount > 0)
            outputEnergy();
    }
    
    private void adjustEnergyStorageSize() {
        
        var targetMultiplier = 1 + Math.pow((double) laserInputStorage.getAmount() / Oritech.CONFIG.laserArmConfig.energyPerTick(), 2);
        targetMultiplier = Math.min(targetMultiplier, 5_000);
        laserInputStorage.setAmount(0);
        var targetAmount = BASE_CAPACITY * qualityMultiplier * targetMultiplier;
        var currentAmount = energyStorage.getCapacity();
        energyStorage.capacity = (long) MathHelper.lerp(0.005d, currentAmount, targetAmount);
        energyStorage.setMaxInsert((long) targetAmount);
        energyStorage.setMaxExtract((long) targetAmount);
        
        if (energyStorage.capacity < energyStorage.maxInsert * 0.9999) {
            // growing, spawn particles
            ParticleContent.UNSTABLE_CONTAINER_GROWING.spawn(world, pos.toCenterPos(), 2);
        }
        
        if (energyStorage.amount > energyStorage.capacity) {
            energyStorage.amount = energyStorage.capacity;
        }
        
        if (energyStorage.capacity != BASE_CAPACITY * qualityMultiplier)
            energyStorage.update();
        
    }
    
    private void outputEnergy() {
        var positions = List.of(new Vec3i(0, -3, 0), new Vec3i(0, 2, 0));
        for (var outputPos : positions) {
            var worldPos = pos.add(outputPos);
            var candidate = EnergyApi.BLOCK.find(world, worldPos, null);
            if (candidate != null) {
                EnergyApi.transfer(energyStorage, candidate, energyStorage.maxExtract, false);
            }
        }
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        addMultiblockToNbt(nbt);
        var blockId = Registries.BLOCK.getId(capturedBlock.getBlock());
        nbt.putString("captured", blockId.toString());
        nbt.putLong("energy_stored", energyStorage.amount);
        nbt.putLong("energy_capacity", energyStorage.capacity);
        nbt.putFloat("quality", qualityMultiplier);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        loadMultiblockNbtData(nbt);
        energyStorage.amount = nbt.getLong("energy_stored");
        energyStorage.capacity = nbt.getLong("energy_capacity");
        energyStorage.capacity = nbt.getLong("energy_capacity");
        qualityMultiplier = nbt.getFloat("quality");
        
        var blockId = nbt.getString("captured");
        if (!blockId.isBlank() && Registries.BLOCK.containsId(Identifier.of(blockId)))
            capturedBlock = Registries.BLOCK.get(Identifier.of(blockId)).getDefaultState();
        
    }
    
    @Override
    public void preNetworkUpdate(SyncType type) {
        super.preNetworkUpdate(type);
        currentStats = energyStorage.getCurrentStatistics(world.getTime());
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 0, state -> {
            if (state.getController().getAnimationState().equals(AnimationController.State.STOPPED)) {
                if (this.getCachedState().get(UnstableContainerBlock.SETUP_DONE)) {
                    return state.setAndContinue(IDLE);
                } else {
                    return state.setAndContinue(SETUP);
                }
            }
            return PlayState.CONTINUE;
        }).setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    
    @Override
    public List<Vec3i> getCorePositions() {
        return getCoreOffsets();
    }
    
    public static List<Vec3i> getCoreOffsets() {
        return List.of(
          new Vec3i(-1, -2, -1),
          new Vec3i(0, -2, -1),
          new Vec3i(1, -2, -1),
          new Vec3i(-1, -2, 0),
          new Vec3i(0, -2, 0),
          new Vec3i(1, -2, 0),
          new Vec3i(-1, -2, 1),
          new Vec3i(0, -2, 1),
          new Vec3i(1, -2, 1),
          new Vec3i(-1, -1, -1),
          new Vec3i(0, -1, -1),
          new Vec3i(1, -1, -1),
          new Vec3i(-1, -1, 0),
          new Vec3i(0, -1, 0),
          new Vec3i(1, -1, 0),
          new Vec3i(-1, -1, 1),
          new Vec3i(0, -1, 1),
          new Vec3i(1, -1, 1),
          new Vec3i(-1, 0, -1),
          new Vec3i(0, 0, -1),
          new Vec3i(1, 0, -1),
          new Vec3i(-1, 0, 0),
          new Vec3i(1, 0, 0),
          new Vec3i(-1, 0, 1),
          new Vec3i(0, 0, 1),
          new Vec3i(1, 0, 1),
          new Vec3i(0, 1, -1),
          new Vec3i(-1, 1, 0),
          new Vec3i(0, 1, 0),
          new Vec3i(1, 1, 0),
          new Vec3i(0, 1, 1)
        );
    }
    
    @Override
    public Direction getFacingForMultiblock() {
        return Direction.NORTH;
    }
    
    @Override
    public BlockPos getPosForMultiblock() {
        return pos;
    }
    
    @Override
    public World getWorldForMultiblock() {
        return world;
    }
    
    @Override
    public ArrayList<BlockPos> getConnectedCores() {
        return coreBlocksConnected;
    }
    
    @Override
    public void setCoreQuality(float quality) {
    
    }
    
    @Override
    public float getCoreQuality() {
        return 7;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryForMultiblock() {
        return null;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return getEnergyStorage(direction);
    }
    
    @Override
    public void triggerSetupAnimation() {}
    
    @Override
    public void onCoreBroken(BlockPos corePos) {
        onBroken(corePos);
    }
    
    @Override
    public void onControllerBroken() {
        onBroken(pos);
    }
    
    private void onBroken(BlockPos eventSource) {
        if (dropped) return;
        dropped = true;
        
        for (var corePos : coreBlocksConnected) {
            if (corePos.equals(eventSource)) continue;
            world.setBlockState(corePos, Blocks.AIR.getDefaultState());
        }
        
        world.setBlockState(pos, capturedBlock);
        
        var spawnAt = this.pos.toCenterPos().add(0, 1, 0);
        world.spawnEntity(new ItemEntity(world, spawnAt.x, spawnAt.y, spawnAt.z, new ItemStack(ItemContent.UNSTABLE_CONTAINER)));
        
    }
    
    public void setCapturedBlock(BlockState capturedBlock) {
        this.capturedBlock = capturedBlock;
        markDirty();
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        
        if (direction == null) return energyStorage;
        
        if (direction.equals(Direction.DOWN) || direction.equals(Direction.UP))
            return outputStorage;
        
        return energyStorage;
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of();
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return 0;   // todo
    }
    
    @Override
    public float getDisplayedEnergyTransfer() {
        return energyStorage.maxInsert;
    }
    
    @Override
    public BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(7, 6, 15, 54 + 18);
    }
    
    @Override
    public float getProgress() {
        return 0;
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }
    
    @Override
    public Inventory getDisplayedInventory() {
        return new SimpleInventory();
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.STORAGE_SCREEN;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
    
    @Override
    public boolean showExpansionPanel() {
        return false;
    }
    
    @Override
    public void saveExtraData(PacketByteBuf buf) {
        sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.literal("");
    }
    
    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this);
    }
}
