package rearth.oritech.block.entity.interaction;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.energy.DelegatingEnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.DynamicStatisticEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.UpgradableOritechScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.SoundContent;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.MultiblockMachineController;
import rearth.oritech.util.ScreenProvider;

import java.util.*;

public class PowerPoleEntity extends NetworkedBlockEntity implements MultiblockMachineController, MenuProvider,
        ScreenProvider, EnergyProvider {

    // stores data per dimension
    public static final HashMap<Identifier, PoleNetworkData> POLE_NETWORK_DATA = new HashMap<>();

    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private float coreQuality = 1f;
    @SyncField({SyncType.INITIAL, SyncType.CUSTOM})
    private final Set<ConnectionTarget> connections = new HashSet<>();

    private PoleNetworkData netDataRef = null;

    // storage
    @SyncField(SyncType.GUI_TICK)
    public DynamicStatisticEnergyStorage.EnergyStatistics currentStats = DynamicStatisticEnergyStorage.EnergyStatistics.EMPTY;
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    protected final PowerPoleEnergyStorage energyStorage = new PowerPoleEnergyStorage();

    private final EnergyHandler outputStorage = new DelegatingEnergyHandler(energyStorage) {
        @Override
        public int insert(int amount, TransactionContext transaction) {
            return 0;
        }
    };

    private BlockCapabilityCache<EnergyHandler, Direction> cachedOutputTarget;

    private final SimpleInventoryStorage basicInv = new SimpleInventoryStorage(0, this::setChanged);

    public PowerPoleEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.POWER_POLE_ENTITY.get(), pos, state);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {

        outputEnergy();

        energyStorage.tick(level.getGameTime());

        if (level.getRandom().nextFloat() > 0.95f) {

            var stats = this.energyStorage.getCurrentStatistics(level.getGameTime());
            var moved = stats.insertedLastTickTotal() + stats.extractedLastTickTotal();

            if (moved > 10 && level instanceof ServerLevel serverLevel) {
                var at = worldPosition.getCenter().add(level.getRandom().nextFloat() * 0.4, level.getRandom().nextFloat() * 0.4, level.getRandom().nextFloat() * 0.4);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 2, level.getRandom().nextFloat(), level.getRandom().nextFloat(), level.getRandom().nextFloat(), 0.15f);
            }
        }

    }

    @Override
    public void preNetworkUpdate(SyncType type) {
        super.preNetworkUpdate(type);
        currentStats = energyStorage.getCurrentStatistics(level.getGameTime());
    }

    private void outputEnergy() {
        if (!isConnected() || energyStorage.getAmountAsLong() <= 0 || !(level instanceof ServerLevel serverLevel))
            return;

        if (cachedOutputTarget == null) {
            var target = ExpandableEnergyStorageBlockEntity.getOutputPosition(worldPosition, getFacingForMultiblock().getCounterClockWise());
            cachedOutputTarget = BlockCapabilityCache.create(Capabilities.Energy.BLOCK, serverLevel, target.getB(), target.getA().getOpposite());
        }

        var candidate = cachedOutputTarget.getCapability();
        if (candidate != null) {
            var available = (int) Math.min(Integer.MAX_VALUE, energyStorage.getAmountAsLong());
            try (var transaction = Transaction.openRoot()) {
                var inserted = candidate.insert(available, transaction);
                if (inserted <= 0) return;
                energyStorage.extract(inserted, transaction);
                transaction.commit();
            }
        }
    }

    public void assignNewTarget(BlockPos target, Player player) {
        Oritech.LOGGER.info("Assigning new power pole target");

        // adjust for core blocks
        var targetState = level.getBlockState(target);
        if (targetState.getBlock() instanceof MachineCoreBlock && targetState.getValue(MachineCoreBlock.USED)) {
            target = MachineCoreBlock.getControllerPos(level, target);
        }

        var pitch = 0.85f + level.getRandom().nextFloat() * 0.3f;
        level.playSound(null, worldPosition, SoundContent.ELECTRIC_SHOCK.value(), SoundSource.PLAYERS, 0.7f, pitch);

        var dist = target.distManhattan(worldPosition);

        if (dist < OritechConfig.poleConfig.minRange.get() || dist > OritechConfig.poleConfig.maxRange.get()) {
            player.sendSystemMessage(Component.translatable("message.oritech.target_designator.pole_dist_invalid", OritechConfig.poleConfig.minRange.get(), OritechConfig.poleConfig.maxRange.get(), dist));
            return;
        }

        var targetEntityCandidate = level.getBlockEntity(target, BlockEntitiesContent.POWER_POLE_ENTITY.get());
        if (targetEntityCandidate.isEmpty() || target.equals(worldPosition)) {
            player.sendSystemMessage(Component.translatable("message.oritech.target_designator.pole_position_invalid"));
            return;
        }

        var targetEntity = targetEntityCandidate.get();

        if (this.connections.stream().anyMatch(elem -> elem.pos().equals(targetEntity.getBlockPos()))) {
            this.removeIncomingConnection(target);
            targetEntity.removeIncomingConnection(worldPosition);

            var netData = getCachedNetData();
            var net = netData.getNetwork(worldPosition);
            this.updateConnectionsInState(net);
            targetEntity.updateConnectionsInState(net);

            netData.updateNetworkSplit(Set.of(worldPosition, target), getNetwork());
            player.sendSystemMessage(Component.translatable("message.oritech.target_designator.removing_pole_connection"));
            return;
        }

        connections.add(targetEntity.getConnectionData());
        targetEntity.assignIncomingConnection(this);

        var allNetworks = getCachedNetData();

        var ownNet = getNetwork();
        var isConnected = isConnected();
        var targetNet = targetEntity.getNetwork();
        var targetConnected = targetEntity.isConnected();

        if (!isConnected && targetConnected) {
            // join network of target
            joinNetwork(targetNet, allNetworks);
        } else if (isConnected && !targetConnected) {
            // join target into own network
            targetEntity.joinNetwork(ownNet, allNetworks);
        } else if (!isConnected && !targetConnected) {
            // neither connected, create new network, then let both join
            var newNet = createNetwork(allNetworks);
            this.joinNetwork(newNet, allNetworks);
            targetEntity.joinNetwork(newNet, allNetworks);
        } else if (isConnected && targetConnected) {
            if (targetNet == ownNet) {
                // in same network, nothing to do
            } else {
                // merge networks
                allNetworks.mergeNetworks(ownNet, targetNet);
            }
        } else {
            throw new IllegalStateException("This should never happen");
        }

        allNetworks.setDirty();

        updateConnectionsInState(Objects.requireNonNull(getNetwork()));
        targetEntity.updateConnectionsInState(getNetwork());

        this.setChanged(false);
        this.sendUpdate(SyncType.CUSTOM);

        player.sendSystemMessage(Component.translatable("message.oritech.target_designator.connected_poles"));
    }

    private void joinNetwork(PoleNetwork target, PoleNetworkData data) {
        data.activeNetworks.put(worldPosition, target);
    }

    private void updateConnectionsInState(PoleNetwork network) {
        network.setPole(worldPosition, connections);
    }

    private PoleNetwork createNetwork(PoleNetworkData data) {
        return new PoleNetwork();
    }

    public void assignIncomingConnection(PowerPoleEntity from) {
        this.connections.add(from.getConnectionData());
        this.setChanged(false);
        this.sendUpdate(SyncType.CUSTOM);
    }

    public void removeIncomingConnection(BlockPos source) {

        var removed = this.connections.stream().filter(elem -> elem.pos().equals(source)).toList();

        removed.forEach(this.connections::remove);

        this.setChanged(false);
        this.sendUpdate(SyncType.CUSTOM);
    }

    public ConnectionTarget getConnectionData() {
        return new ConnectionTarget(worldPosition, getFacingForMultiblock());
    }

    public Set<ConnectionTarget> getConnections() {
        return connections;
    }

    public PoleNetworkData getCachedNetData() {
        if (netDataRef == null) {
            netDataRef = POLE_NETWORK_DATA.computeIfAbsent(level.dimension().identifier(), data -> new PoleNetworkData());
        }

        return netDataRef;
    }

    public void onRemoved() {

        // remove connection from targets
        for (var target : connections) {
            if (level.getBlockEntity(target.pos) instanceof PowerPoleEntity powerPole) {
                powerPole.removeIncomingConnection(worldPosition);
            }
        }

        var allNetworks = getCachedNetData();
        allNetworks.removePole(worldPosition);
        allNetworks.setDirty();

        this.setChanged(false);

    }

    @Override
    public void setChanged(boolean updateComparator) {
        super.setChanged(updateComparator);
        getCachedNetData().setDirty();
    }

    private boolean isConnected() {
        return getNetwork() != null;
    }

    private PoleNetwork getNetwork() {
        return getCachedNetData().getNetwork(worldPosition);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        serializeMultiblock(output);

        var connectionList = output.childrenList("connectionData");
        for (var connection : connections) {
            var child = connectionList.addChild();
            child.store("pos", BlockPos.CODEC, connection.pos());
            child.putInt("direction", connection.facing.ordinal());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        deserializeMultiblock(input);

        connections.clear();
        for (var connectionData : input.childrenListOrEmpty("connectionData")) {
            var pos = connectionData.read("pos", BlockPos.CODEC);
            var directionIndex = connectionData.getIntOr("direction", -1);
            if (pos.isPresent() && directionIndex >= 0 && directionIndex < Direction.values().length) {
                connections.add(new ConnectionTarget(pos.get(), Direction.values()[directionIndex]));
            }

        }
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(getBlockPos());
        this.sendUpdate(SyncType.GUI_OPEN);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new UpgradableOritechScreenHandler(containerId, playerInventory, this);
    }

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {

        if (direction != null && direction.equals(getFacingForMultiblock().getCounterClockWise()))
            return outputStorage;

        return energyStorage;
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0));
    }

    @Override
    public Direction getFacingForMultiblock() {
        return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
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
        this.coreQuality = quality;
    }

    @Override
    public float getCoreQuality() {
        return coreQuality;
    }

    @Override
    public StacksResourceHandler<ItemStack, ItemResource> getInventoryForMultiblock() {
        return basicInv;
    }

    @Override
    public DynamicEnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return energyStorage;
    }

    @Override
    public void triggerSetupAnimation() {

    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of();
    }

    @Override
    public float getDisplayedEnergyUsage() {
        return 0;
    }

    @Override
    public float getDisplayedEnergyTransfer() {
        return OritechConfig.poleConfig.energyCapacity.get();
    }

    @Override
    public BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(8, 6, 18, 54 + 18);
    }

    @Override
    public float getProgress() {
        return 0;
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
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }

    @Override
    public StacksResourceHandler<ItemStack, ItemResource> getDisplayedInventory() {
        return basicInv;
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.POWER_POLE_SCREEN.get();
    }

    protected class PowerPoleEnergyStorage extends DynamicEnergyStorage {

        private long clientShownEnergy;

        public PowerPoleEnergyStorage() {
            super(0, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, PowerPoleEntity.this::setChanged, false);
        }

        private boolean isValid() {
            return level != null && PowerPoleEntity.this.isConnected();
        }

        @Override
        public int insert(int maxAmount, TransactionContext transaction) {
            if (!isValid()) return 0;

            var insertAmount = (int) Math.min(maxAmount, getCapacityAsLong() - getAmountAsLong());

            if (insertAmount > 0) {
                var newAmount = getAmountAsLong() + insertAmount;
                set(newAmount);
                getNetwork().inserted.add((long) insertAmount);
            }

            return insertAmount;
        }

        @Override
        public int extract(int maxAmount, TransactionContext transaction) {
            if (!isValid()) return 0;

            var extractAmount = (int) Math.min(maxAmount, this.getAmountAsLong());

            if (extractAmount > 0) {
                var newAmount = getAmountAsLong() - extractAmount;
                set(newAmount);
                getNetwork().extracted.add((long) extractAmount);
            }

            return extractAmount;
        }

        @Override
        public void set(long amount) {
            if (!isValid()) return;

            if (amount > getCapacityAsLong() || amount < 0) {
                Oritech.LOGGER.error("tried setting invalid amount for pole network: " + amount);
                return;
            }

            var network = PowerPoleEntity.this.getNetwork();
            if (network == null) {
                Oritech.LOGGER.error("Invalid set network state for power pole entity at: {}", worldPosition);
                return;
            }

            network.storedEnergy = amount;

        }

        @Override
        public long getAmountAsLong() {
            if (level.isClientSide()) return clientShownEnergy;

            if (!isValid()) return 0;

            var network = PowerPoleEntity.this.getNetwork();
            if (network == null) {
                Oritech.LOGGER.error("Invalid get network state for power pole entity at: {}", worldPosition);
                return 0;
            }

            return network.storedEnergy;
        }

        @Override
        public long getCapacityAsLong() {
            return OritechConfig.poleConfig.energyCapacity.get();
        }

        public void update() {
            if (!isValid()) return;
            PowerPoleEntity.this.setChanged(false);
        }

        public void tick(long worldTicks) {
            var net = getNetwork();

            if (worldTicks <= net.lastTickedAt) return;
            var index = (int) (worldTicks % 20);
            net.historicInsert[index] = net.inserted.stream().mapToLong(Long::longValue).sum();
            net.historicExtract[index] = net.extracted.stream().mapToLong(Long::longValue).sum();
            net.currentInsertSources = net.inserted.size();

            net.inserted.clear();
            net.extracted.clear();
            net.lastTickedAt = worldTicks;
        }

        public DynamicStatisticEnergyStorage.EnergyStatistics getCurrentStatistics(long worldTicks) {
            var index = (int) (worldTicks % 20);
            var net = getNetwork();

            return new DynamicStatisticEnergyStorage.EnergyStatistics(
                    (float) Arrays.stream(net.historicInsert).mapToLong(Long::longValue).average().orElse(0),
                    (float) Arrays.stream(net.historicExtract).mapToLong(Long::longValue).average().orElse(0),
                    net.historicInsert[index],
                    net.historicExtract[index],
                    net.currentInsertSources,
                    Arrays.stream(net.historicInsert).mapToLong(Long::longValue).max().orElse(0),
                    Arrays.stream(net.historicExtract).mapToLong(Long::longValue).max().orElse(0)
            );

        }

        @Override
        public Long getDeltaData() {
            return getAmountAsLong();
        }

        @Override
        public PowerPoleEnergyStorage getFullData() {
            return this;
        }

        @Override
        public StreamCodec<? extends ByteBuf, Long> getDeltaCodec() {
            return ByteBufCodecs.VAR_LONG;
        }

        @Override
        public void handleDeltaUpdate(Long updatedData) {
            this.clientShownEnergy = updatedData;
        }
    }

    public record ConnectionTarget(BlockPos pos, Direction facing) {
    }

    // this is kept separate from the block entities (and fully decoupled) so it works well across unloaded areas,
    // even if some poles are in the middle of it
    public static class PoleNetworkData extends SavedData {

        // runtime lookup map. Pole positions are also stored in the network, so for saving the keys can be reconstructed later here
        private final Map<BlockPos, PoleNetwork> activeNetworks = new HashMap<>();

        public @NotNull PoleNetwork getNetwork(BlockPos pos) {
            return activeNetworks.computeIfAbsent(pos, elem -> {
                var data = new HashMap<BlockPos, Set<BlockPos>>();
                data.put(elem, Set.of());
                return new PoleNetwork(data, 0);
            });
        }

        public static Factory<PoleNetworkData> TYPE = new Factory<>(PoleNetworkData::new, PoleNetworkData::fromNbt, null);

        @Override
        public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registryLookup) {

            var networksList = new ListTag();

            var uniqueNetworks = new HashSet<>(activeNetworks.values());

            // Iterate the unique networks and save them
            for (var network : uniqueNetworks) {

                var networkCompound = new CompoundTag();
                networkCompound.putLong("energy", network.storedEnergy);

                var poleList = new ListTag();
                for (var polePair : network.poles.entrySet()) {
                    var data = new CompoundTag();
                    data.putLong("pos", polePair.getKey().asLong());
                    data.putLongArray("cons", polePair.getValue().stream().mapToLong(BlockPos::asLong).toArray());
                    poleList.add(data);
                }

                networkCompound.put("poles", poleList);
                networksList.add(networkCompound);
            }

            tag.put("networks", networksList);
            return tag;
        }

        public static PoleNetworkData fromNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {

            var data = new PoleNetworkData();

            if (!nbt.contains("networks")) return data;

            var networksList = nbt.getList("networks").orElse(new ListTag());

            for (var networkTag : networksList) {

                var tag = (CompoundTag) networkTag;

                var energy = tag.getLong("energy").orElse(0L);
                var poles = new HashMap<BlockPos, Set<BlockPos>>();
                var poleDataList = tag.getList("poles").orElse(new ListTag());
                for (var poleDataTag : poleDataList) {
                    var poleData = (CompoundTag) poleDataTag;
                    var polePos = BlockPos.of(poleData.getLong("pos").orElse(0L));
                    var poleConnections = new HashSet<>(Arrays.stream(poleData.getLongArray("cons").orElse(new long[0])).mapToObj(BlockPos::of).toList());
                    poles.put(polePos, poleConnections);
                }

                var network = new PoleNetwork(poles, energy);

                for (var polePos : network.getPoles())
                    data.activeNetworks.put(polePos, network);
            }

            return data;
        }

        protected void mergeNetworks(PoleNetwork netA, PoleNetwork netB) {

            // move all from netB to netA
            netA.storedEnergy = Math.min(OritechConfig.poleConfig.energyCapacity.get(), netA.storedEnergy + netB.storedEnergy);

            netA.poles.putAll(netB.poles);

            for (var polePos : netB.getPoles()) {
                activeNetworks.put(polePos, netA);
            }

        }

        public void updateNetworkSplit(Set<BlockPos> removedConnections, PoleNetwork existingNet) {

            var newNets = new HashSet<Map<BlockPos, Set<BlockPos>>>();

            for (var deletedConnection : removedConnections) {
                var newConnectionNet = FloodFillNetwork(existingNet, deletedConnection);
                newNets.add(newConnectionNet);

            }

            if (newNets.size() == 1) return;    // no split needed, there's other connections doing the same

            var newNetCount = newNets.size();
            var newNetPower = existingNet.storedEnergy / newNetCount;

            for (var newNetData : newNets) {
                var newNet = new PoleNetwork(newNetData, newNetPower);
                for (var polePos : newNet.getPoles())
                    activeNetworks.put(polePos, newNet);
            }
        }

        public void removePole(BlockPos removeAt) {

            // first, updating connections in network data
            var existingNet = activeNetworks.get(removeAt);
            if (existingNet == null) return;

            activeNetworks.remove(removeAt);

            var removedPoleConnections = existingNet.poles.remove(removeAt);

            if (removedPoleConnections.size() <= 1) return; // no split needed

            updateNetworkSplit(removedPoleConnections, existingNet);

        }

        // the network is potentially split at this stage. Returns all poles connected to the marked start
        private static Map<BlockPos, Set<BlockPos>> FloodFillNetwork(PoleNetwork existing, BlockPos startAt) {

            var maxIterations = 200;
            var result = new HashMap<BlockPos, Set<BlockPos>>();

            var openChecks = Set.of(startAt);

            // basically a while loop, but with an extra safety check
            for (int i = 0; i < maxIterations; i++) {

                var next = new HashSet<BlockPos>();
                for (var openPole : openChecks) {
                    var connections = existing.getConnections(openPole);
                    if (connections == null) continue;
                    result.put(openPole, connections);

                    // add all connections that we dont have already
                    next.addAll(connections.stream().filter(elem -> !result.containsKey(elem)).toList());
                }

                if (next.isEmpty()) break;

                openChecks = next;
            }

            return result;
        }

    }

    // stores the energy in a network. Also includes a list of poles and their connection (only used for floodfill when splitting networks)
    public static class PoleNetwork {

        // contains all poles as key, and 0-N positions as value
        private final Map<BlockPos, Set<BlockPos>> poles;

        public long storedEnergy = 0L;

        // network stats
        private final List<Long> inserted = new ArrayList<>();  // just for this tick
        private final List<Long> extracted = new ArrayList<>();
        private final Long[] historicInsert = new Long[20];
        private final Long[] historicExtract = new Long[20];
        private int currentInsertSources = 0;
        private long lastTickedAt = 0;

        // constructor for codec
        private PoleNetwork(Map<BlockPos, Set<BlockPos>> loadedPoles, long storedEnergy) {
            this.poles = new HashMap<>(loadedPoles);
            this.storedEnergy = storedEnergy;
            Arrays.fill(historicInsert, 0L);
            Arrays.fill(historicExtract, 0L);
        }

        // default constructor
        public PoleNetwork() {
            this.poles = new HashMap<>();
            Arrays.fill(historicInsert, 0L);
            Arrays.fill(historicExtract, 0L);
        }

        public Set<BlockPos> getPoles() {
            return poles.keySet();
        }

        public Set<BlockPos> getConnections(BlockPos polePos) {
            return poles.get(polePos);
        }

        // adds or updates a pole in a network
        public void setPole(BlockPos pole, Set<ConnectionTarget> connections) {
            poles.put(pole, new HashSet<>(connections.stream().map(elem -> elem.pos()).toList()));
        }
    }
}
