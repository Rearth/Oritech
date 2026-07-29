package rearth.oritech.block.base.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.DynamicStatisticEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;
import rearth.oritech.block.blocks.storage.PortableEnergyStorageBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.UpgradableOritechScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.ItemContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.ScreenProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ExpandableEnergyStorageBlockEntity extends NetworkedBlockEntity implements ItemProvider, EnergyProvider, MachineAddonController,
        ScreenProvider, MenuProvider {

    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> openSlots = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private BaseAddonData addonData = BaseAddonData.DEFAULT_ADDON_DATA;

    @SyncField(SyncType.GUI_TICK)
    private boolean redstonePowered;

    @SyncField(SyncType.GUI_TICK)
    public DynamicStatisticEnergyStorage.EnergyStatistics currentStats;

    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(1, this::setChanged);

    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public int rfOutputOverride = -1;

    //own storage
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public final DynamicStatisticEnergyStorage energyStorage = new DynamicStatisticEnergyStorage(
            getDefaultCapacity(),
            getDefaultInsertRate(),
            getDefaultExtractionRate(),
            this::setChanged) {
        @Override
        public int extract(int amount, TransactionContext transaction) {
            if (rfOutputOverride > 0) {
                amount = Math.min(rfOutputOverride, amount);
            }
            return super.extract(amount, transaction);
        }
    };

    private BlockCapabilityCache<EnergyHandler, Direction> cachedOutputTarget;

    public ExpandableEnergyStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (serverLevel.isClientSide()) return;

        energyStorage.tick((int) serverLevel.getGameTime());

        if (!redstonePowered)
            outputEnergy();

        inputFromCrystal();
    }

    private void inputFromCrystal() {
        if (energyStorage.energy >= energyStorage.capacity || inventory.getResource(0).isEmpty()) return;

        if (!inventory.getResource(0).getItem().equals(ItemContent.OVERCHARGED_CRYSTAL.get())) return;

        try (var transaction = Transaction.openRoot()) {
            var inserted = energyStorage.insert(OritechConfig.overchargedCrystalChargeRate.get(), transaction);
            if (inserted > 0) transaction.commit();
        }
    }

    private void outputEnergy() {

        if (energyStorage.getAmountAsLong() <= 0 || !(level instanceof ServerLevel serverLevel)) return;

        chargeItems();

        if (cachedOutputTarget == null) {
            var target = getOutputPosition(worldPosition, getFacing());
            cachedOutputTarget = BlockCapabilityCache.create(Capabilities.Energy.BLOCK, serverLevel, target.getB(), target.getA().getOpposite());
        }

        var available = Math.min(energyStorage.getAmountAsLong(), energyStorage.getMaxExtract());
        if (rfOutputOverride > 0) {
            available = Math.min(available, rfOutputOverride);
        }

        try (var transaction = Transaction.openRoot()) {
            var candidate = cachedOutputTarget.getCapability();
            if (candidate != null) {
                var inserted = candidate.insert((int) available, transaction);
                if (inserted <= 0) return;

                energyStorage.extract(inserted, transaction);
                transaction.commit();
            }
        }
    }

    private void chargeItems() {

        var heldStack = inventory.getStacks().get(0);
        if (heldStack.isEmpty() || heldStack.getCount() > 1) return;

        try (var transaction = Transaction.openRoot()) {

            var candidate = heldStack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forHandlerIndexStrict(inventory, 0));
            if (candidate == null) return;

            var available = Math.min(energyStorage.getAmountAsLong(), energyStorage.getMaxExtract());
            var inserted = candidate.insert((int) available, transaction);
            if (inserted <= 0) return;

            energyStorage.internalExtract(inserted, transaction);

            transaction.commit();

        }
    }

    public static Tuple<Direction, BlockPos> getOutputPosition(BlockPos pos, Direction facing) {
        var blockInFront = (BlockPos) Geometry.offsetToWorldPosition(facing, new Vec3i(-1, 0, 0), pos);
        var worldOffset = blockInFront.subtract(pos);
        var direction = Direction.getApproximateNearest(worldOffset.getX(), worldOffset.getY(), worldOffset.getZ());

        return new Tuple<>(direction, blockInFront);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        serializeAddonData(output);

        energyStorage.serialize(output);
        inventory.serialize(output);

        output.putBoolean("redstone", redstonePowered);
        output.putInt("rfOutputOverride", rfOutputOverride);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        deserializeAddonData(input);

        energyStorage.deserialize(input);
        inventory.deserialize(input);

        redstonePowered = input.getBooleanOr("redstone", false);
        rfOutputOverride = input.getIntOr("rfOutputOverride", -1);
    }

    @Override
    public void preNetworkUpdate(SyncType type) {
        super.preNetworkUpdate(type);
        currentStats = energyStorage.getCurrentStatistics(level.getGameTime());
    }

    public Direction getFacing() {
        return getBlockState().getValue(PortableEnergyStorageBlock.TARGET_DIR);
    }

    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        return inventory;
    }

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        if (direction == null)
            return energyStorage;

        if (direction.equals(getFacing())) {
            return energyStorage.getOutputStorage();
        } else {
            return energyStorage.getInputStorage();
        }
    }

    @Override
    public List<BlockPos> getConnectedAddons() {
        return connectedAddons;
    }

    @Override
    public List<BlockPos> getOpenAddonSlots() {
        return openSlots;
    }

    @Override
    public Direction getFacingForAddon() {
        var facing = Objects.requireNonNull(level).getBlockState(getBlockPos()).getValue(PortableEnergyStorageBlock.TARGET_DIR);

        if (facing.equals(Direction.UP) || facing.equals(Direction.DOWN))
            return Direction.NORTH;

        return facing;
    }

    @Override
    public DynamicEnergyStorage getStorageForAddon() {
        return energyStorage;
    }

    @Override
    public StacksResourceHandler<ItemStack, ItemResource> getInventoryForAddon() {
        return inventory;
    }

    @Override
    public ScreenProvider getScreenProvider() {
        return this;
    }

    @Override
    public BaseAddonData getBaseAddonData() {
        return addonData;
    }

    @Override
    public void setBaseAddonData(BaseAddonData data) {
        this.addonData = data;
    }

    @Override
    public void updateEnergyContainer() {
        MachineAddonController.super.updateEnergyContainer();
        energyStorage.maxExtract = getDefaultExtractionRate() + addonData.energyBonusTransfer();

    }

    @Override
    public float getDisplayedEnergyTransfer() {
        return energyStorage.maxInsert;
    }

    public abstract long getDefaultExtractionRate();


    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        this.sendUpdate(SyncType.GUI_OPEN);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new UpgradableOritechScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 40, 38));
    }

    @Override
    public float getDisplayedEnergyUsage() {
        return 0;
    }

    @Override
    public float getProgress() {
        return 0;
    }


    @Override
    public BlockPos getPosForAddon() {
        return getBlockPos();
    }

    @Override
    public Level getWorldForAddon() {
        return getLevel();
    }

    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }

    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }

    @Override
    public StacksResourceHandler<ItemStack, ItemResource> getDisplayedInventory() {
        return inventory;
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.STORAGE_SCREEN.get();
    }

    @Override
    public boolean showProgress() {
        return false;
    }

    @Override
    public Property<Direction> getBlockFacingProperty() {
        return PortableEnergyStorageBlock.TARGET_DIR;
    }

    public void setRedstonePowered(boolean isPowered) {
        this.redstonePowered = isPowered;
    }

    @Override
    public boolean hasRedstoneControlAvailable() {
        return true;
    }

    @Override
    public int receivedRedstoneSignal() {
        if (redstonePowered) return 15;
        return level.getBestNeighborSignal(worldPosition);
    }

    @Override
    public String currentRedstoneEffect() {
        if (receivedRedstoneSignal() > 0) return "tooltip.oritech.redstone_disabled_storage";
        return "tooltip.oritech.redstone_enabled_direct";
    }

    @Override
    public BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(8, 24, 17, 54 + 20);
    }

    public static void handleLimitPacket(StorageLimitPacket payload, IPayloadContext context) {
        var level = context.player().level();
        if (level == null) return;
        var storageCandidate = level.getBlockEntity(payload.position());
        if (!(storageCandidate instanceof ExpandableEnergyStorageBlockEntity storageEntity)) return;

        storageEntity.rfOutputOverride = payload.limit();
        storageEntity.setChanged();

    }

    public record StorageLimitPacket(BlockPos position, int limit) implements CustomPacketPayload {

        public static final Type<StorageLimitPacket> PACKET_ID = new Type<>(Oritech.id("storage_limit"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
