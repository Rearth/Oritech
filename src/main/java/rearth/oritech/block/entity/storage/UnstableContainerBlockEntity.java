package rearth.oritech.block.entity.storage;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.energy.DelegatingEnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.DynamicStatisticEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;
import rearth.oritech.block.blocks.storage.UnstableContainerBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.UpgradableOritechScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.util.*;

import java.util.ArrayList;
import java.util.List;

public class UnstableContainerBlockEntity extends NetworkedBlockEntity implements ScreenProvider, MenuProvider, ColorableMachine,
        GeoBlockEntity, MultiblockMachineController, EnergyProvider {

    public static final RawAnimation SETUP = RawAnimation.begin().thenPlay("setup").thenPlay("idle");
    public static final RawAnimation IDLE = RawAnimation.begin().thenPlay("idle");

    public static final Long BASE_CAPACITY = OritechConfig.unstableContainerBaseCapacity.get();

    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();

    @SyncField(SyncType.GUI_OPEN)
    public BlockState capturedBlock = Blocks.AIR.defaultBlockState();
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    public float qualityMultiplier = 1f;
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    public DynamicStatisticEnergyStorage.EnergyStatistics currentStats;

    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorVariant currentColor = getDefaultColor();

    private long age = 0;
    private boolean dropped = false;

    // scaling storage
    public final DynamicEnergyStorage laserInputStorage = new DynamicEnergyStorage(100_000_000, 100_000_000, 0, 0, this::setChanged, false);

    //own storage
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    protected final DynamicStatisticEnergyStorage energyStorage = new DynamicStatisticEnergyStorage(20_000_000L, 20_000_000L, 20_000_000L, this::setChanged);

    private final EnergyHandler outputStorage = new DelegatingEnergyHandler(energyStorage) {
        @Override
        public int insert(int amount, TransactionContext transaction) {
            return 0;
        }
    };
    private final SimpleInventoryStorage emptyInventory = new SimpleInventoryStorage(0, this::setChanged);

    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    public UnstableContainerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.UNSTABLE_CONTAINER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {

        age++;
        if (age > 10 && !state.getValue(UnstableContainerBlock.SETUP_DONE)) {
            level.setBlockAndUpdate(pos, state.setValue(UnstableContainerBlock.SETUP_DONE, true));
        }

        energyStorage.tick((int) level.getGameTime());

        adjustEnergyStorageSize();

        if (energyStorage.energy > 0)
            outputEnergy();
    }

    private void adjustEnergyStorageSize() {

        var targetMultiplier = 1 + Math.pow((double) laserInputStorage.getAmountAsLong() / OritechConfig.laserArmConfig.energyPerTick.get(), 2);
        targetMultiplier = Math.min(targetMultiplier, 5_000);

        laserInputStorage.set(0);

        var targetAmount = BASE_CAPACITY * qualityMultiplier * targetMultiplier;
        var currentAmount = energyStorage.getCapacityAsLong();

        // no change needed
        if (Math.abs(targetAmount - currentAmount) < 1) return;

        energyStorage.setCapacity((long) Mth.lerp(0.005d, currentAmount, targetAmount));
        energyStorage.setMaxInsert((long) targetAmount);
        energyStorage.setMaxExtract((long) targetAmount);

        if (energyStorage.capacity < energyStorage.maxInsert * 0.9999) {
            // growing, spawn particles
            if (level instanceof ServerLevel sl) {
                var c = worldPosition.getCenter();
                sl.sendParticles(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER, c.x, c.y, c.z, 2, 2, 2, 2, 0);
            }
        }

        if (energyStorage.energy > energyStorage.capacity) {
            energyStorage.energy = energyStorage.capacity;
        }

        energyStorage.onEnergyChanged(currentAmount);

    }

    private void outputEnergy() {
        if (!(level instanceof ServerLevel)) return;

        // top and bottom
        var positions = List.of(new Vec3i(0, -3, 0), new Vec3i(0, 2, 0));

        // todo caching?
        for (var outputPos : positions) {
            var worldPos = worldPosition.offset(outputPos);
            var candidate = level.getCapability(Capabilities.Energy.BLOCK, worldPos, null);
            if (candidate != null) {
                try (var transaction = Transaction.openRoot()) {
                    var inserted = candidate.insert((int) Math.min(energyStorage.energy, energyStorage.maxExtract), transaction);  // no idea if this math.min is really needed
                    if (inserted > 0) {
                        energyStorage.internalExtract(inserted, transaction);
                        transaction.commit();
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        serializeMultiblock(output);
        serializeColor(output);

        energyStorage.serialize(output);

        var blockId = BuiltInRegistries.BLOCK.getKey(capturedBlock.getBlock());
        output.store("captured", Identifier.CODEC, blockId);

        output.putFloat("quality", qualityMultiplier);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        deserializeMultiblock(input);
        deserializeColor(input);

        energyStorage.deserialize(input);

        qualityMultiplier = input.getFloatOr("quality", 1f);

        input.read("captured", Identifier.CODEC)
                .flatMap(BuiltInRegistries.BLOCK::get)
                .ifPresent(block -> capturedBlock = block.value().defaultBlockState());

    }

    @Override
    public void preNetworkUpdate(SyncType type) {
        super.preNetworkUpdate(type);
        currentStats = energyStorage.getCurrentStatistics(level.getGameTime());
    }

    @Override
    public ColorVariant getCurrentColor() {
        return currentColor;
    }

    @Override
    public void assignColor(ColorVariant color) {
        this.currentColor = color;

        if (this.level != null && !this.level.isClientSide()) {
            this.setChanged(false);
            this.sendUpdate(SyncType.SPARSE_TICK);
        }
    }

    @Override
    public ColorVariant getDefaultColor() {
        return ColorVariant.CAMO;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("machine", state -> {
            if (this.getBlockState().getValue(UnstableContainerBlock.SETUP_DONE)) {
                return state.setAndContinue(IDLE);
            } else {
                return state.setAndContinue(SETUP);
            }
        }).setSoundKeyframeHandler(new MachineSoundHandler<>(() -> 1f)));
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
        return worldPosition;
    }

    @Override
    public Level getWorldForMultiblock() {
        return level;
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
    public StacksResourceHandler<ItemStack, ItemResource> getInventoryForMultiblock() {
        return emptyInventory;
    }

    @Override
    public DynamicEnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return energyStorage;
    }

    @Override
    public void triggerSetupAnimation() {
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
        if (dropped) return;
        dropped = true;

        for (var corePos : coreBlocksConnected) {
            if (corePos.equals(eventSource)) continue;
            level.setBlockAndUpdate(corePos, Blocks.AIR.defaultBlockState());
        }

        level.setBlockAndUpdate(worldPosition, capturedBlock);

        var spawnAt = this.worldPosition.getCenter().add(0, 1, 0);
        level.addFreshEntity(new ItemEntity(level, spawnAt.x, spawnAt.y, spawnAt.z, new ItemStack(ItemContent.UNSTABLE_CONTAINER.get())));

    }

    public void setCapturedBlock(BlockState capturedBlock) {
        this.capturedBlock = capturedBlock;
        setChanged();
    }

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {

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
        return new BarConfiguration(8, 6, 15, 54 + 18);
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
    public StacksResourceHandler<ItemStack, ItemResource> getDisplayedInventory() {
        return emptyInventory;
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.UNSTABLE_CONTAINER_SCREEN.get();
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
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(getBlockPos());
        sendUpdate(SyncType.GUI_OPEN);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new UpgradableOritechScreenHandler(syncId, playerInventory, this);
    }
}
