package rearth.oritech.block.entity.arcane;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.CatalystScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.ComparatorOutputProvider;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.MachineSoundHandler;
import rearth.oritech.util.ScreenProvider;

import java.util.List;

public class ArcaneCatalystBlockEntity extends BaseSoulCollectionEntity
        implements ItemProvider, EnergyProvider, ScreenProvider, ComparatorOutputProvider, GeoBlockEntity, BlockEntityTicker<ArcaneCatalystBlockEntity>, MenuProvider {

    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation STABILIZED = RawAnimation.begin().thenLoop("stabilized");
    public static final RawAnimation UNSTABLE = RawAnimation.begin().thenLoop("unstable");
    public static final RawAnimation EMPTY = RawAnimation.begin().thenLoop("empty");

    public final int baseSoulCapacity = OritechConfig.catalystBaseSouls.get();
    public final int maxProgress = 20;
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    // working data
    public int collectedSouls;
    public int maxSouls = OritechConfig.catalystBaseSouls.get();
    private int unstableTicks;
    private int progress;
    private boolean isHyperEnchanting;
    private boolean networkDirty;
    private String lastAnimation = "invalid";
    private int lastComparatorOutput;

    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(2, this::setChanged) {
        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index == 0 && !resource.is(Items.ENCHANTED_BOOK)) return 0; // only allow stabilized_enchanter books in slot 0
            return super.insert(index, resource, amount, transaction);
        }
    };

    public final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(10_000_000, 10_000_000, 0, 0, this::setChanged, false);

    public ArcaneCatalystBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ARCANE_CATALYST_BLOCK.get(), pos, state);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, ArcaneCatalystBlockEntity blockEntity) {

        if (level.isClientSide()) return;

        // check if powered, and adjust soul capacity
        if (energyStorage.energy > 0) {
            var gainedSoulCapacity = energyStorage.energy / OritechConfig.catalystRFPerSoul.get();
            energyStorage.set(0);
            var newMax = baseSoulCapacity + gainedSoulCapacity;
            adjustMaxSouls(newMax);
            this.setChanged();
        } else if (maxSouls > baseSoulCapacity) {
            adjustMaxSouls(baseSoulCapacity);
        }

        // explode if unstable
        if (collectedSouls > maxSouls) {
            unstableTicks++;

            if (level instanceof ServerLevel sl) {
                var c = pos.getCenter();
                sl.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, unstableTicks / 4, 1, 1, 1, 0);
            }

            if (unstableTicks > 60)
                doExplosion();
            return;
        }

        unstableTicks = 0;

        // check if output is empty
        // check if a book is in slot 0
        // check if an item is in slot 1
        if (canProceed()) {
            networkDirty = true;
            progress++;

            if (level instanceof ServerLevel sl) {
                var c = pos.getCenter().add(0, 0.3, 0);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y, c.z, isHyperEnchanting ? 15 : 3, 1.2, 1.2, 1.2, 0);
            }

            if (progress >= maxProgress) {
                enchantInput();
                if (level instanceof ServerLevel sl) {
                    var c = pos.getCenter();
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, c.x, c.y, c.z, maxProgress + 10, 0.6, 0.6, 0.6, 0);
                }

                progress = 0;
                isHyperEnchanting = false;
            }
        } else {
            progress = 0;
        }

        if (networkDirty) {
            networkDirty = false;
            updateNetwork();
            DeathListener.resetEvents();
            updateAnimation();

            var comparatorLevel = calculateComparatorLevel();
            if (comparatorLevel != lastComparatorOutput) {
                lastComparatorOutput = comparatorLevel;
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }

        }

        // periodically re-trigger animation updates
        if (level.getGameTime() % 60 == 0) {
            lastAnimation = "invalid";
            updateAnimation();
        }

    }

    private boolean isEmpty() {
        return collectedSouls <= 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
        output.putInt("souls", collectedSouls);
        output.putInt("maxSouls", maxSouls);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
        collectedSouls = input.getIntOr("souls", 0);
        maxSouls = input.getIntOr("maxSouls", 0);
    }

    public void doExplosion() {

        var center = worldPosition.getCenter();
        var strength = Math.sqrt(collectedSouls - baseSoulCapacity);

        level.explode(null, center.x, center.y, center.z, (int) strength, true, Level.ExplosionInteraction.BLOCK);
        level.removeBlock(worldPosition, false);
    }

    private void adjustMaxSouls(long target) {
        if (maxSouls > target) {
            maxSouls--;
        } else if (maxSouls < target) {
            maxSouls++;
        }

        this.networkDirty = true;
        this.setChanged();
    }

    private void enchantInput() {

        var bookCandidate = inventory.getStacks().getFirst();
        if (!bookCandidate.getItem().equals(Items.ENCHANTED_BOOK) || !bookCandidate.has(DataComponents.STORED_ENCHANTMENTS))
            return;

        var enchantmentData = bookCandidate.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantmentData == null || enchantmentData.isEmpty()) return;

        var enchantment = enchantmentData.keySet().stream().findFirst().orElseThrow();

        var inputStack = inventory.getStacks().get(1);
        var toolLevel = inputStack.getTagEnchantments().getLevel(enchantment);
        inputStack.enchant(enchantment, toolLevel + 1);

        collectedSouls -= getEnchantmentCost(enchantment.value(), toolLevel + 1, isHyperEnchanting);

        if (isHyperEnchanting)
            inventory.getStacks().set(0, ItemStack.EMPTY);

    }

    private boolean hasEnoughSouls(Enchantment enchantment, int targetLevel) {
        var resultingCost = getEnchantmentCost(enchantment, targetLevel, isHyperEnchanting);
        return collectedSouls >= resultingCost;
    }

    private int getEnchantmentCost(Enchantment enchantment, int targetLevel, boolean hyper) {
        var baseCost = enchantment.getAnvilCost();
        var resultingCost = baseCost * targetLevel * OritechConfig.catalystCostMultiplier.get();
        if (hyper)
            resultingCost = (int) (Math.pow(resultingCost * OritechConfig.catalystHyperMultiplier.get(), OritechConfig.catalystHyperExpFactor.get()) + OritechConfig.catalystBaseSouls.get());
        return resultingCost;
    }

    // for UI
    public int getDisplayedCost() {
        if (inventory.getItem(0).isEmpty() || inventory.getItem(1).isEmpty()) return 0;
        var bookCandidate = inventory.getItem(0);

        if (bookCandidate.getItem().equals(Items.ENCHANTED_BOOK) && bookCandidate.has(DataComponents.STORED_ENCHANTMENTS)) {

            var enchantmentData = bookCandidate.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantmentData == null || enchantmentData.isEmpty()) return 0;

            var enchantment = enchantmentData.keySet().stream().findFirst().orElseThrow();
            var maxLevel = enchantment.value().getMaxLevel();
            var bookLevel = enchantmentData.getLevel(enchantment);

            if (bookLevel != maxLevel) return 0;

            var inputStack = inventory.getItem(1);
            var toolLevel = inputStack.getTagEnchantments().getLevel(enchantment);
            var isHyper = toolLevel >= maxLevel;

            return getEnchantmentCost(enchantment.value(), toolLevel + 1, isHyper);
        }

        return 0;
    }

    private boolean canProceed() {

        if (inventory.getItem(0).isEmpty() || inventory.getItem(1).isEmpty()) return false;

        var bookCandidate = inventory.getItem(0);
        if (bookCandidate.getItem().equals(Items.ENCHANTED_BOOK) && bookCandidate.has(DataComponents.STORED_ENCHANTMENTS)) {

            var enchantmentData = bookCandidate.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantmentData == null || enchantmentData.isEmpty()) return false;

            var enchantment = enchantmentData.keySet().stream().findFirst().orElseThrow();
            var maxLevel = enchantment.value().getMaxLevel();
            var level = enchantmentData.getLevel(enchantment);

            if (enchantment.is(TagContent.CATALYST_ENCHANTMENT_BLACKLIST)) return false;

            // yes this does not check if the item can be enchanted with this enchantment. This is intentional, allowing you to skip the normal limitations
            var inputStack = inventory.getItem(1);
            var toolLevel = inputStack.getTagEnchantments().getLevel(enchantment);
            this.isHyperEnchanting = toolLevel >= maxLevel;

            return level == maxLevel && hasEnoughSouls(enchantment.value(), toolLevel + 1);
        }

        return false;
    }

    @Override
    public void onSoulIncoming(Vec3 source) {
        var distance = (float) source.distanceTo(worldPosition.getCenter());
        collectedSouls++;
        networkDirty = true;
        this.setChanged();

        var soulPath = worldPosition.getCenter().subtract(source);
        ParticleContent.WanderingSoul(level, source.add(0, 0.7f, 0), soulPath, (int) getSoulTravelDuration(distance));
    }

    @Override
    public boolean canAcceptSoul() {
        return collectedSouls < maxSouls;
    }

    @Override
    public int getComparatorOutput() {
        return calculateComparatorLevel();
    }

    private int calculateComparatorLevel() {
        return (int) ((float) collectedSouls / maxSouls * 16);
    }

    private void updateNetwork() {
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, ChunkPos.containing(worldPosition), new CatalystSyncPacket(worldPosition, collectedSouls, progress, isHyperEnchanting, maxSouls));
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }

    @Override
    public boolean showProgress() {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        updateNetwork();
        return new CatalystScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        return inventory;
    }

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return energyStorage;
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 56, 35),
                new GuiSlot(1, 75, 35));
    }

    @Override
    public BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(8, 7, 18, 71);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("machine", 4, state -> {
            if (state.controller().getPlayState().equals(PlayState.STOP))
                return state.setAndContinue(EMPTY);
            return PlayState.CONTINUE;
        })
                .triggerableAnim("stabilized", STABILIZED)
                .triggerableAnim("idle", IDLE)
                .triggerableAnim("unstable", UNSTABLE)
                .triggerableAnim("empty", EMPTY)
                .setSoundKeyframeHandler(new MachineSoundHandler<>(() -> 1f)));
    }

    private void updateAnimation() {

        var targetAnim = isEmpty() ? "empty" : "idle";
        if (maxSouls > baseSoulCapacity)
            targetAnim = "stabilized";

        if (unstableTicks > 0)
            targetAnim = "unstable";

        if (!targetAnim.equals(lastAnimation)) {
            triggerAnim("machine", targetAnim);
            lastAnimation = targetAnim;
        }

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    @Override
    public float getDisplayedEnergyUsage() {
        return 0;
    }

    @Override
    public float getProgress() {
        return progress / (float) maxProgress;
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
        return ModScreens.CATALYST_SCREEN.get();
    }

    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }

    // this is used as soul display instead
    @Override
    public boolean showEnergy() {
        return true;
    }

    public static void receiveUpdatePacket(CatalystSyncPacket packet, IPayloadContext context) {
        var level = context.player().level();
        if (level.getBlockEntity(packet.position) instanceof ArcaneCatalystBlockEntity catalystBlock) {
            catalystBlock.isHyperEnchanting = packet.isHyperEnchanting();
            catalystBlock.progress = packet.progress();
            catalystBlock.collectedSouls = packet.storedSouls();
            catalystBlock.maxSouls = packet.maxSouls();
        }
    }

    public record CatalystSyncPacket(BlockPos position, int storedSouls, int progress, boolean isHyperEnchanting,
                                     int maxSouls) implements CustomPacketPayload {

        public static final Type<CatalystSyncPacket> PACKET_ID = new Type<>(Oritech.id("catalyst"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
