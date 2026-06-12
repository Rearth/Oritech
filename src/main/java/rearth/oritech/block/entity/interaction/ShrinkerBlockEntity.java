package rearth.oritech.block.entity.interaction;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.UpgradableOritechScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.block.base.entity.MachineBlockEntity.*;

public class ShrinkerBlockEntity extends NetworkedBlockEntity implements ItemProvider, EnergyProvider, GeoBlockEntity, MenuProvider,
        ScreenProvider, MultiblockMachineController, MachineAddonController, ColorableMachine {

    public static final RawAnimation SHRINK = RawAnimation.begin().thenPlay("work");

    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    private final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), 0, 0, this::setChanged, false);

    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(1, this::setChanged);

    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();

    @SyncField(SyncType.GUI_OPEN)
    private float coreQuality = 1f;

    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorVariant currentColor = getDefaultColor();

    // addon data
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> openSlots = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private BaseAddonData addonData = BaseAddonData.DEFAULT_ADDON_DATA;

    @SyncField(SyncType.GUI_OPEN)
    public ShrunkAddonData currentCandidate = new ShrunkAddonData(BaseAddonData.DEFAULT_ADDON_DATA, false, 0, 0, false, false);

    private boolean wasRedstoneActive = false;

    public ShrinkerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.SHRINKER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {

        var currentRedstone = serverLevel.hasNeighborSignal(pos);

        if (currentRedstone && !wasRedstoneActive) {
            // recently enabled redstone
            doShrink();
        }

        wasRedstoneActive = currentRedstone;

    }

    public void doShrink() {

        if (energyStorage.energy < getDefaultCapacity()) return;

        initAddons();

        if (currentCandidate == null || connectedAddons.isEmpty() || !inventory.isEmpty()) return;

        energyStorage.set(energyStorage.energy - getDefaultCapacity());

        var createdStack = new ItemStack(BlockContent.MACHINE_COMBI_ADDON.asItem());
        createdStack.set(ComponentContent.ADDON_DATA.get(), currentCandidate);

        inventory.set(0, ItemResource.of(createdStack), 1);

        for (var addonPos : connectedAddons.reversed()) {

            level.setBlock(addonPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);

            if (level instanceof ServerLevel serverLevel) {
                var spawnAt = addonPos.getCenter();
                serverLevel.sendParticles(ParticleTypes.GUST, spawnAt.x, spawnAt.y, spawnAt.z, 1, 0, 0.1f, 0, 0.5f);
                serverLevel.playSound(null, worldPosition, SoundEvents.SMALL_AMETHYST_BUD_PLACE, SoundSource.BLOCKS, 2f, 0.5f);
            }
        }

        triggerAnim("machine", "work");

        initAddons();
        this.sendUpdate(SyncType.GUI_OPEN);
    }

    @Override
    public void gatherAddonStats(List<AddonBlock> addons) {
        MachineAddonController.super.gatherAddonStats(addons);

        if (addons.isEmpty()) {
            currentCandidate = new ShrunkAddonData(BaseAddonData.DEFAULT_ADDON_DATA, false, 0, 0, false, false);
            return;
        }

        // collect all data
        var data = getBaseAddonData();
        var fluid = false;
        var quarryCount = 0;
        var yieldCount = 0;
        var cropFilter = false;
        var silk = false;

        for (var addon : addons) {
            if (addon.addonBlock().equals(BlockContent.MACHINE_FLUID_ADDON.get())) fluid = true;
            if (addon.addonBlock().equals(BlockContent.QUARRY_ADDON.get())) quarryCount++;
            if (addon.addonBlock().equals(BlockContent.MACHINE_YIELD_ADDON.get())) yieldCount++;
            if (addon.addonBlock().equals(BlockContent.CROP_FILTER_ADDON.get())) cropFilter = true;
            if (addon.addonBlock().equals(BlockContent.MACHINE_SILK_TOUCH_ADDON.get())) silk = true;
        }

        currentCandidate = new ShrunkAddonData(data, fluid, quarryCount, yieldCount, cropFilter, silk);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
        serializeMultiblock(output);
        if (currentCandidate != null) output.store("shrunk_addon", ShrunkAddonData.CODEC, currentCandidate);
        serializeColor(output);
        output.putLong("energy_stored", energyStorage.energy);
        output.putBoolean("redstone", wasRedstoneActive);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
        deserializeMultiblock(input);
        currentCandidate = input.read("shrunk_addon", ShrunkAddonData.CODEC).orElse(null);
        deserializeColor(input);

        energyStorage.energy = input.getLongOr("energy_stored", 0);
        wasRedstoneActive = input.getBooleanOr("redstone", false);

        updateEnergyContainer();
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
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return energyStorage;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("machine", 0, state -> {
            if (state.isCurrentAnimation(SETUP)) {
                if (state.controller().hasAnimationFinished()) {
                    state.setAndContinue(IDLE);
                } else {
                    return state.setAndContinue(SETUP);
                }
            }

            if (isActive(getBlockState())) {
                return state.setAndContinue(IDLE);
            } else {
                return state.setAndContinue(PACKAGED);
            }
        })
                .triggerableAnim("work", SHRINK)
                .triggerableAnim("deploy", MachineBlockEntity.SETUP)
                .setSoundKeyframeHandler(new MachineSoundHandler<>()));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    private boolean isActive(BlockState state) {
        return state.getValue(ASSEMBLED);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        sendUpdate(SyncType.GUI_OPEN);
    }

    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new UpgradableOritechScreenHandler(i, inventory, this);
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
    public BlockPos getPosForAddon() {
        return getBlockPos();
    }

    @Override
    public Level getWorldForAddon() {
        return getLevel();
    }

    @Override
    public Direction getFacingForAddon() {
        return Objects.requireNonNull(level).getBlockState(getBlockPos()).getValue(BlockStateProperties.HORIZONTAL_FACING);
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
    public List<Vec3i> getAddonSlots() {
        return List.of(
                new Vec3i(1, 0, 0)
        );
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
    public long getDefaultCapacity() {
        return OritechConfig.addonConfig.addonShrinkerRF.get();
    }

    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.addonConfig.addonShrinkerRF.get() / 60;
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
                new Vec3i(1, 0, -1),
                new Vec3i(0, 0, -1),
                new Vec3i(1, 0, 1),
                new Vec3i(0, 0, 1)
        );
    }

    @Override
    public Direction getFacingForMultiblock() {
        return Objects.requireNonNull(level).getBlockState(getBlockPos()).getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockPos getPosForMultiblock() {
        return getBlockPos();
    }

    @Override
    public Level getWorldForMultiblock() {
        return getLevel();
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
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 40, 40, true));
    }

    @Override
    public float getDisplayedEnergyUsage() {
        return getDefaultCapacity();
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
        return inventory;
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.SHRINKER_SCREEN.get();
    }

    @Override
    public boolean showProgress() {
        return false;
    }

    public static void onPlayerUse(ShrinkerPlayerUsePacket packet, IPayloadContext context) {

        var level = context.player().level();
        var candidate = level.getBlockEntity(packet.pos(), BlockEntitiesContent.SHRINKER_BLOCK_ENTITY.get());
        candidate.ifPresent(ShrinkerBlockEntity::doShrink);

    }

    @Override
    public int receivedRedstoneSignal() {
        if (wasRedstoneActive) return 15;
        return level.getBestNeighborSignal(worldPosition);
    }

    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        return inventory;
    }

    @Override
    public boolean hasRedstoneControlAvailable() {
        return true;
    }

    public record ShrunkAddonData(BaseAddonData data, boolean fluid, int quarryCount, int yieldCount,
                                  boolean cropFilter, boolean silk) {

        public static final Codec<ShrunkAddonData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BaseAddonData.CODEC.fieldOf("data").forGetter(ShrunkAddonData::data),
                Codec.BOOL.fieldOf("fluid").forGetter(ShrunkAddonData::fluid),
                Codec.INT.fieldOf("quarry_count").forGetter(ShrunkAddonData::quarryCount),
                Codec.INT.fieldOf("yield_count").forGetter(ShrunkAddonData::yieldCount),
                Codec.BOOL.fieldOf("crop_filter").forGetter(ShrunkAddonData::cropFilter),
                Codec.BOOL.fieldOf("silk").forGetter(ShrunkAddonData::silk)
        ).apply(instance, ShrunkAddonData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShrunkAddonData> STREAM_CODEC = NetworkManager.getAutoCodec(ShrunkAddonData.class);

        @Override
        public @NotNull String toString() {
            return "ShrunkAddonData{" +
                    "data=" + data +
                    ", fluid=" + fluid +
                    ", quarryCount=" + quarryCount +
                    ", yieldCount=" + yieldCount +
                    ", cropFilter=" + cropFilter +
                    '}';
        }
    }

    public record ShrinkerPlayerUsePacket(BlockPos pos) implements CustomPacketPayload {

        public static final Type<ShrinkerPlayerUsePacket> PACKET_ID = new Type<>(Oritech.id("shrink"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
