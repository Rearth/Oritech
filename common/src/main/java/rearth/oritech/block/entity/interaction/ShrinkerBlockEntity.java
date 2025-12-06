package rearth.oritech.block.entity.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.block.base.entity.MachineBlockEntity.*;

public class ShrinkerBlockEntity extends NetworkedBlockEntity implements ItemApi.BlockProvider, EnergyApi.BlockProvider, GeoBlockEntity, ExtendedMenuProvider,
                                                                           ScreenProvider, MultiblockMachineController, MachineAddonController, ColorableMachine {
    
    public static final RawAnimation SHRINK = RawAnimation.begin().thenPlay("work");
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    private final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), 0, this::setChanged);
    
    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(1, this::setChanged);
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    @SyncField(SyncType.GUI_OPEN)
    private float coreQuality = 1f;
    
    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorableMachine.ColorVariant currentColor = getDefaultColor();
    
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
        super(BlockEntitiesContent.SHRINKER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
    
        var currentRedstone = world.hasNeighborSignal(pos);
        
        if (currentRedstone && !wasRedstoneActive) {
            // recently enabled redstone
            System.out.println("triggered redstone shrink");
            doShrink();
        }
        
        wasRedstoneActive = currentRedstone;
        
    }
    
    public void doShrink() {
        
        if (energyStorage.getAmount() < getDefaultCapacity()) return;
        
        initAddons();
        
        if (currentCandidate == null || connectedAddons.isEmpty() || !inventory.isEmpty()) return;
        
        energyStorage.setAmount(energyStorage.getAmount() - getDefaultCapacity());
        energyStorage.update();
        
        var createdStack = new ItemStack(BlockContent.MACHINE_COMBI_ADDON.asItem());
        createdStack.set(ComponentContent.ADDON_DATA.get(), currentCandidate);
        
        inventory.setStackInSlot(0, createdStack);
        
        for (var addonPos : connectedAddons.reversed()) {
            
            level.setBlock(addonPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            
            if (level instanceof ServerLevel serverWorld) {
                var spawnAt = addonPos.getCenter();
                serverWorld.sendParticles(ParticleTypes.GUST, spawnAt.x, spawnAt.y, spawnAt.z, 1, 0, 0.1f, 0, 0.5f);
                serverWorld.playSound(null, worldPosition, SoundEvents.SMALL_AMETHYST_BUD_PLACE, SoundSource.BLOCKS, 2f, 0.5f);
            }
        }
        triggerAnim("machine", "work");
        
    }
    
    @Override
    public void gatherAddonStats(List<AddonBlock> addons) {
        MachineAddonController.super.gatherAddonStats(addons);
        
        if (addons.isEmpty()) {
            currentCandidate = new ShrunkAddonData(BaseAddonData.DEFAULT_ADDON_DATA, false, 0, 0, false, false);;
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
            if (addon.addonBlock().equals(BlockContent.MACHINE_FLUID_ADDON)) fluid = true;
            if (addon.addonBlock().equals(BlockContent.QUARRY_ADDON)) quarryCount++;
            if (addon.addonBlock().equals(BlockContent.MACHINE_YIELD_ADDON)) yieldCount++;
            if (addon.addonBlock().equals(BlockContent.CROP_FILTER_ADDON)) cropFilter = true;
            if (addon.addonBlock().equals(BlockContent.MACHINE_SILK_TOUCH_ADDON)) silk = true;
        }
        
        currentCandidate = new ShrunkAddonData(data, fluid, quarryCount, yieldCount, cropFilter, silk);
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        addMultiblockToNbt(nbt);
        writeAddonToNbt(nbt);
        addColorToNbt(nbt);
        nbt.putLong("energy_stored", energyStorage.amount);
        nbt.putBoolean("redstone", wasRedstoneActive);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        loadMultiblockNbtData(nbt);
        loadAddonNbtData(nbt);
        loadColorFromNbt(nbt);
        
        energyStorage.amount = nbt.getLong("energy_stored");
        wasRedstoneActive = nbt.getBoolean("redstone");
        
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
            this.markDirty(false);
            this.sendUpdate(SyncType.SPARSE_TICK);
        }
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine", 0, state -> {
            if (state.isCurrentAnimation(SETUP)) {
                if (state.getController().hasAnimationFinished()) {
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
                          .setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    private boolean isActive(BlockState state) {
        return state.getValue(ASSEMBLED);
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }
    
    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new UpgradableMachineScreenHandler(i, inventory, this);
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
    public ItemApi.InventoryStorage getInventoryForAddon() {
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
        return Oritech.CONFIG.addonConfig.addonShrinkerRF();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.addonConfig.addonShrinkerRF() / 60;
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
    public ItemApi.InventoryStorage getInventoryForMultiblock() {
        return inventory;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorageForMultiblock(Direction direction) {
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
    public Container getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.SHRINKER_SCREEN;
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
    
    public static void onPlayerUse(ShrinkerPlayerUsePacket packet, Player player, RegistryAccess registryAccess) {
        
        var world = player.level();
        var candidate = world.getBlockEntity(packet.pos(), BlockEntitiesContent.SHRINKER_BLOCK_ENTITY);
        candidate.ifPresent(ShrinkerBlockEntity::doShrink);
        
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
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
        
        public static final CustomPacketPayload.Type<ShrinkerPlayerUsePacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("shrink"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
