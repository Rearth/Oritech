package rearth.oritech.block.entity.augmenter;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.augmenter.AugmentResearchStationBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.OritechScreenHandler;
import rearth.oritech.client.ui.PlayerModifierScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.SoundContent;
import rearth.oritech.init.datapack.AugmentData;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.MachineSoundHandler;
import rearth.oritech.util.MultiblockMachineController;
import rearth.oritech.util.ScreenProvider;

import java.util.*;

public class AugmentApplicationEntity extends NetworkedBlockEntity implements MultiblockMachineController, GeoBlockEntity,
        MenuProvider, ItemProvider, EnergyProvider, ScreenProvider {

    // config
    public static long maxEnergyTransfer = OritechConfig.augmenterMaxEnergy.get() / 10;
    public static long maxEnergyStored = OritechConfig.augmenterMaxEnergy.get();

    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;

    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public final Set<Identifier> researchedAugments = new HashSet<>();
    // working state
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public final HashMap<Integer, ResearchState> availableStations = new HashMap<>();

    public boolean screenInvOverride = false;

    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(5, this::setChanged);

    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    private final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(maxEnergyStored, maxEnergyTransfer, maxEnergyStored, 0, this::setChanged, false);


    public AugmentApplicationEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PLAYER_MODIFIER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        screenInvOverride = false;

        // update research stations
        for (int i = 0; i < 3; i++) {
            var station = availableStations.getOrDefault(i, null);
            if (station == null) continue;
            if (station.working) {
                var isDone = serverLevel.getGameTime() > station.researchStartedAt + station.workTime;
                if (!isDone) continue;

                researchedAugments.add(station.selectedResearch);
                station.working = false;
                this.setChanged();
            }
        }
    }

    // persist researched augments, inventory, energy
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
        energyStorage.serialize(output);
        serializeMultiblock(output);

        var list = output.childrenList("researched");
        for (var augment : researchedAugments) {
            list.addChild().store("id", Identifier.CODEC, augment);
        }

        // also put in pending researches to avoid having to separately store them
        for (var station : availableStations.values()) {
            if (station == null) continue;
            if (station.working) {
                list.addChild().store("id", Identifier.CODEC, station.selectedResearch);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
        energyStorage.deserialize(input);
        deserializeMultiblock(input);

        researchedAugments.clear();

        for (var element : input.childrenListOrEmpty("researched")) {
            element.read("id", Identifier.CODEC).ifPresent(researchedAugments::add);
        }

    }

    public void researchAugment(Identifier augment, boolean creative, Player player) {
        var currentLevel = Objects.requireNonNull(level);

        if (PlayerAugments.getAugment(currentLevel.registryAccess(), augment) == null) {
            Oritech.LOGGER.error("Player augment with id {} not found. This should never happen", augment);
            return;
        }

        if (researchedAugments.contains(augment)) {
            Oritech.LOGGER.warn("Player tried to research already researched augment {}", augment);
            return;
        }

        var augmentData = getAugmentData(augment);
        if (augmentData == null) return;

        try (var transaction = Transaction.openRoot()) {

            var extracted = energyStorage.internalExtract(augmentData.rfCost(), transaction);
            if (extracted != augmentData.rfCost() && !creative) return;

            if (!consumeIngredients(augmentData.researchCost(), player, transaction) && !creative) return;

            transaction.commit();
        }

        // assign first idle station
        for (int i = 0; i < 3; i++) {
            var station = availableStations.getOrDefault(i, null);
            if (station == null) continue;
            if (station.working) continue;


            if (!BuiltInRegistries.BLOCK.getKey(station.type).equals(augmentData.requiredStation())) continue;

            station.selectedResearch = augment;
            station.working = true;
            station.researchStartedAt = currentLevel.getGameTime();
            station.workTime = creative ? 5 : augmentData.time();

            break;

        }
        this.setChanged();
    }

    public void installAugmentToPlayer(Identifier augment, boolean creative, Player player) {

        if (PlayerAugments.getAugment(Objects.requireNonNull(level).registryAccess(), augment) == null) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }

        if (!researchedAugments.contains(augment)) {
            Oritech.LOGGER.warn("Player tried to install augment with id" + augment + " without researching it.");
            return;
        }

        var augmentData = getAugmentData(augment);
        if (augmentData == null) return;

        try (var transaction = Transaction.openRoot()) {
            if (!consumeIngredients(augmentData.applyCost(), player, transaction) && !creative) return;
            transaction.commit();
        }

        var augmentInstance = PlayerAugments.getAugment(player.registryAccess(), augment);
        if (augmentInstance == null) return;
        augmentInstance.installToPlayer(player);
        this.setChanged();

        player.level().playSound(null, player.blockPosition(), SoundContent.SHORT_SERVO.value(), SoundSource.BLOCKS);
    }

    public void removeAugmentFromPlayer(Identifier augment, Player player) {

        if (PlayerAugments.getAugment(Objects.requireNonNull(level).registryAccess(), augment) == null) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }

        var augmentInstance = PlayerAugments.getAugment(player.registryAccess(), augment);
        if (augmentInstance == null) return;
        augmentInstance.removeFromPlayer(player);
        this.setChanged();
    }

    public static void toggleAugmentForPlayer(Identifier augment, Player player) {

        if (PlayerAugments.getAugment(player.registryAccess(), augment) == null) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }

        var augmentInstance = PlayerAugments.getAugment(player.registryAccess(), augment);
        if (augmentInstance == null) return;

        if (!augmentInstance.isInstalled(player)) {
            Oritech.LOGGER.error("Tried toggling not-installed augment id: " + augment + ". This should never happen");
            return;
        }

        augmentInstance.toggle(player);
    }

    public boolean hasPlayerAugment(Identifier augment, Player player) {

        if (PlayerAugments.getAugment(player.registryAccess(), augment) == null) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return false;
        }

        var augmentInstance = PlayerAugments.getAugment(player.registryAccess(), augment);
        if (augmentInstance == null) return false;
        return augmentInstance.isInstalled(player);

    }

    public void loadResearchesFromPlayer(Player player) {

        for (var entry : PlayerAugments.getAllAugments(player.registryAccess()).entrySet()) {
            var augmentId = entry.getKey();
            var augment = entry.getValue();
            var isInstalled = augment.isInstalled(player);
            var isResearched = researchedAugments.contains(augmentId);

            if (isInstalled && !isResearched) {
                researchedAugments.add(augmentId);
            }
        }
    }

    public void loadAvailableStations(Player player) {
        var facing = this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        var currentLevel = Objects.requireNonNull(level);

        var targetPositions = List.of(
                new BlockPos(0, 0, -2),
                new BlockPos(1, 0, 2),
                new BlockPos(2, 0, -1)
        );

        for (int i = 0; i < targetPositions.size(); i++) {
            var candidatePosOffset = targetPositions.get(i);
            var candidatePos = new BlockPos(Geometry.offsetToWorldPosition(facing, candidatePosOffset, worldPosition));

            var candidateState = currentLevel.getBlockState(candidatePos);
            if (!(candidateState.getBlock() instanceof AugmentResearchStationBlock) || !candidateState.getValue(MultiblockMachine.ASSEMBLED)) {
                availableStations.remove(i);
                continue;
            }

            if (availableStations.containsKey(i) && availableStations.get(i) != null && availableStations.get(i).type.equals(candidateState.getBlock()))
                continue;

            var newState = new ResearchState(candidateState.getBlock(), false, Identifier.parse(""), -1, -1);

            availableStations.put(i, newState);
        }

    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
                new Vec3i(0, 0, 1),
                new Vec3i(0, 0, -1),
                new Vec3i(-1, 0, 0),
                new Vec3i(-1, 0, 1),
                new Vec3i(-1, 0, -1),
                new Vec3i(0, 1, 1),
                new Vec3i(0, 1, -1),
                new Vec3i(-1, 1, 0),
                new Vec3i(-1, 1, 1),
                new Vec3i(-1, 1, -1)
        );
    }

    @Override
    public Direction getFacingForMultiblock() {
        var state = getBlockState();
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
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
        return inventory;
    }

    @Override
    public DynamicEnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return energyStorage;
    }

    @Override
    public void triggerSetupAnimation() {
        triggerAnim("machine", "deploy");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("machine", 0, state -> {
            if (state.isCurrentAnimation(MachineBlockEntity.SETUP)) {
                if (state.controller().hasAnimationFinished()) {
                    state.setAndContinue(MachineBlockEntity.IDLE);
                } else {
                    return state.setAndContinue(MachineBlockEntity.SETUP);
                }
            }

            return state.setAndContinue(getBlockState().getValue(MultiblockMachine.ASSEMBLED)
                    ? MachineBlockEntity.IDLE
                    : MachineBlockEntity.PACKAGED);
        })
                .triggerableAnim("deploy", MachineBlockEntity.SETUP)
                .setSoundKeyframeHandler(new MachineSoundHandler<>()));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        this.sendUpdate(SyncType.GUI_OPEN);
        var dist = player.distanceToSqr(this.worldPosition.getBottomCenter());
        if (dist > 1 || screenInvOverride)
            return new OritechScreenHandler(syncId, playerInventory, this);

        return new PlayerModifierScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 30, 30),
                new GuiSlot(1, 50, 30),
                new GuiSlot(2, 70, 30),
                new GuiSlot(3, 90, 30),
                new GuiSlot(4, 110, 30)
        );
    }

    @Override
    public float getDisplayedEnergyUsage() {
        return 0;
    }

    @Override
    public boolean showEnergyUsage() {
        return false;
    }

    @Override
    public boolean showEnergyTransfer() {
        return false;
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
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.AUGMENTER_INV_SCREEN.get();
    }

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return energyStorage;
    }

    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        return inventory;
    }

    @Override
    public StacksResourceHandler<ItemStack, ItemResource> getDisplayedInventory() {
        return inventory;
    }

    private @Nullable AugmentData getAugmentData(Identifier augment) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        var augmentData = PlayerAugments.getAugmentData(serverLevel.registryAccess(), augment);
        if (augmentData == null) {
            Oritech.LOGGER.warn("Player augment definition with id {} not found", augment);
            return null;
        }

        return augmentData;
    }

    private boolean consumeIngredients(List<SizedIngredient> costs, Player player, Transaction transaction) {

        for (var wantedInput : costs) {
            var missingCount = wantedInput.count();
            var ingredient = wantedInput.ingredient();

            var stacks = inventory.getStacks();
            for (var i = 0; i < stacks.size(); i++) {
                var stack = stacks.get(i);
                if (ingredient.test(stack)) { // using size-ignoring test to allow ingredient to be consumed from multiple slots
                    var taken = inventory.extract(i, ItemResource.of(stack), missingCount, transaction);
                    missingCount -= taken;
                    if (missingCount <= 0) break;
                }
            }

            if (missingCount > 0) {
                var playerInv = PlayerInventoryWrapper.of(player).getMainSlots();
                for (var i = 0; i < playerInv.size(); i++) {
                    var kind = playerInv.getResource(i);
                    if (ingredient.test(kind.toStack())) {
                        var taken = playerInv.extract(i, kind, missingCount, transaction);
                        missingCount -= taken;
                        if (missingCount <= 0) break;
                    }
                }
            }

            if (missingCount != 0) return false;
        }

        return true;
    }

    public static class ResearchState {

        public Block type;
        public boolean working;
        public Identifier selectedResearch;
        public int workTime;
        public long researchStartedAt;

        public static StreamCodec<RegistryFriendlyByteBuf, ResearchState> PACKET_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC.map(id -> BuiltInRegistries.BLOCK.get(id).orElseThrow().value(), BuiltInRegistries.BLOCK::getKey), ResearchState::getType,
                ByteBufCodecs.BOOL, ResearchState::getWorking,
                Identifier.STREAM_CODEC, ResearchState::getSelectedResearch,
                ByteBufCodecs.INT, ResearchState::getWorkTime,
                ByteBufCodecs.VAR_LONG, ResearchState::getResearchStartedAt,
                ResearchState::new
        );

        public Block getType() {
            return type;
        }

        public int getWorkTime() {
            return workTime;
        }

        public Identifier getSelectedResearch() {
            return selectedResearch;
        }

        public long getResearchStartedAt() {
            return researchStartedAt;
        }

        public boolean getWorking() {
            return working;
        }

        public ResearchState(Block type, boolean working, Identifier selectedResearch, int workTime, long researchStartedAt) {
            this.type = type;
            this.working = working;
            this.selectedResearch = selectedResearch;
            this.workTime = workTime;
            this.researchStartedAt = researchStartedAt;
        }

        @Override
        public String toString() {
            return "ResearchState{" +
                    "type=" + type +
                    ", working=" + working +
                    ", selectedResearch=" + selectedResearch +
                    ", workTime=" + workTime +
                    ", researchStartedAt=" + researchStartedAt +
                    '}';
        }
    }

}
