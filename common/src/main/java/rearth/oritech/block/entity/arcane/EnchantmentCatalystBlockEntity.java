package rearth.oritech.block.entity.arcane;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.init.ParticleContent.SoulParticleData;
import rearth.oritech.client.ui.CatalystScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;

import rearth.oritech.util.AutoPlayingSoundKeyframeHandler;
import rearth.oritech.util.ComparatorOutputProvider;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.ScreenProvider;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EnchantmentCatalystBlockEntity extends BaseSoulCollectionEntity
  implements ItemApi.BlockProvider, EnergyApi.BlockProvider, ScreenProvider, ComparatorOutputProvider, GeoBlockEntity, BlockEntityTicker<EnchantmentCatalystBlockEntity>, ExtendedMenuProvider {
    
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation STABILIZED = RawAnimation.begin().thenLoop("stabilized");
    public static final RawAnimation UNSTABLE = RawAnimation.begin().thenLoop("unstable");
    public static final RawAnimation EMPTY = RawAnimation.begin().thenLoop("empty");
    
    public final int baseSoulCapacity = Oritech.CONFIG.catalystBaseSouls();
    public final int maxProgress = 20;
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    // working data
    public int collectedSouls;
    public int maxSouls = Oritech.CONFIG.catalystBaseSouls();
    private int unstableTicks;
    private int progress;
    private boolean isHyperEnchanting;
    private boolean networkDirty;
    private String lastAnimation = "invalid";
    private int lastComparatorOutput;
    
    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(2, this::setChanged) {
        @Override
        public int insertToSlot(ItemStack addedStack, int slot, boolean simulate) {
            if (slot == 0 && !addedStack.isEmpty() && !addedStack.getItem().equals(Items.ENCHANTED_BOOK)) return 0; // only allow enchanter books in slot 0
            return super.insertToSlot(addedStack, slot, simulate);
        }
    };
    
    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(10_000, 0, 50_000);
    
    public EnchantmentCatalystBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENCHANTMENT_CATALYST_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, EnchantmentCatalystBlockEntity blockEntity) {
        
        if (world.isClientSide) return;
        
        // check if powered, and adjust soul capacity
        if (energyStorage.getAmount() > 0) {
            var gainedSoulCapacity = energyStorage.getAmount() / Oritech.CONFIG.catalystRFPerSoul();
            energyStorage.setAmount(0);
            var newMax = baseSoulCapacity + gainedSoulCapacity;
            adjustMaxSouls(newMax);
            this.setChanged();
        } else if (maxSouls > baseSoulCapacity) {
            adjustMaxSouls(baseSoulCapacity);
        }
        
        // explode if unstable
        if (collectedSouls > maxSouls) {
            unstableTicks++;
            
            ParticleContent.MELTDOWN_IMMINENT.spawn(world, pos.getCenter(), unstableTicks / 4);
            
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
            
            ParticleContent.SOUL_USED.spawn(world, pos.getCenter().add(0, 0.3, 0), isHyperEnchanting ? 15 : 3);
            
            if (progress >= maxProgress) {
                enchantInput();
                ParticleContent.ASSEMBLER_WORKING.spawn(world, pos.getCenter(), maxProgress + 10);
                
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
            
            var level = calculateComparatorLevel();
            if (level != lastComparatorOutput) {
                lastComparatorOutput = level;
                world.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
            
        }
        
        // periodically re-trigger animation updates
        if (world.getGameTime() % 60 == 0) {
            lastAnimation = "invalid";
            updateAnimation();
        }
        
    }
    
    private boolean isEmpty() {
        return collectedSouls <= 0;
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putInt("souls", collectedSouls);
        nbt.putInt("maxSouls", maxSouls);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        collectedSouls = nbt.getInt("souls");
        maxSouls = nbt.getInt("maxSouls");
    }
    
    private void doExplosion() {
        
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
    }
    
    private void enchantInput() {
        
        var bookCandidate = inventory.getItem(0);
        if (!bookCandidate.getItem().equals(Items.ENCHANTED_BOOK) || !bookCandidate.has(DataComponents.STORED_ENCHANTMENTS))
            return;
        
        var enchantment = bookCandidate.get(DataComponents.STORED_ENCHANTMENTS).keySet().stream().findFirst().get();
        
        var inputStack = inventory.getItem(1);
        var toolLevel = inputStack.getEnchantments().getLevel(enchantment);
        inputStack.enchant(enchantment, toolLevel + 1);
        
        collectedSouls -= getEnchantmentCost(enchantment.value(), toolLevel + 1, isHyperEnchanting);
        
        if (isHyperEnchanting)
            inventory.setItem(0, ItemStack.EMPTY);
        
    }
    
    private boolean hasEnoughSouls(Enchantment enchantment, int targetLevel) {
        var resultingCost = getEnchantmentCost(enchantment, targetLevel, isHyperEnchanting);
        return collectedSouls >= resultingCost;
    }
    
    private int getEnchantmentCost(Enchantment enchantment, int targetLevel, boolean hyper) {
        var baseCost = enchantment.getAnvilCost();
        var resultingCost = baseCost * targetLevel * Oritech.CONFIG.catalystCostMultiplier();
        if (hyper) resultingCost = (int) (Math.pow(resultingCost * Oritech.CONFIG.catalystHyperMultiplier(), Oritech.CONFIG.catalystHyperExpFactor()) + Oritech.CONFIG.catalystBaseSouls());
        return resultingCost;
    }
    
    // for UI
    public int getDisplayedCost() {
        if (inventory.getItem(0).isEmpty() || inventory.getItem(1).isEmpty()) return 0;
        var bookCandidate = inventory.getItem(0);
        
        if (bookCandidate.getItem().equals(Items.ENCHANTED_BOOK) && bookCandidate.has(DataComponents.STORED_ENCHANTMENTS)) {
            
            var enchantment = bookCandidate.get(DataComponents.STORED_ENCHANTMENTS).keySet().stream().findFirst().get();
            var maxLevel = enchantment.value().getMaxLevel();
            var bookLevel = bookCandidate.get(DataComponents.STORED_ENCHANTMENTS).getLevel(enchantment);
            
            if (bookLevel != maxLevel) return 0;
            
            var inputStack = inventory.getItem(1);
            var toolLevel = inputStack.getEnchantments().getLevel(enchantment);
            var isHyper = toolLevel >= maxLevel;
            
            return getEnchantmentCost(enchantment.value(), toolLevel + 1, isHyper);
        }
        
        return 0;
    }
    
    private boolean canProceed() {
        
        if (inventory.getItem(0).isEmpty() || inventory.getItem(1).isEmpty()) return false;
        
        var bookCandidate = inventory.getItem(0);
        if (bookCandidate.getItem().equals(Items.ENCHANTED_BOOK) && bookCandidate.has(DataComponents.STORED_ENCHANTMENTS)) {
            
            var enchantment = bookCandidate.get(DataComponents.STORED_ENCHANTMENTS).keySet().stream().findFirst().get();
            var maxLevel = enchantment.value().getMaxLevel();
            var level = bookCandidate.get(DataComponents.STORED_ENCHANTMENTS).getLevel(enchantment);
            
            // yes this does not check if the item can be enchanted with this enchantment. This is intentional, allowing you to skip the normal limitations
            var inputStack = inventory.getItem(1);
            var toolLevel = inputStack.getEnchantments().getLevel(enchantment);
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
        
        var soulPath = worldPosition.getCenter().subtract(source);
        var animData = new ParticleContent.SoulParticleData(soulPath, (int) getSoulTravelDuration(distance));
        
        ParticleContent.WANDERING_SOUL.spawn(level, source.add(0, 0.7f, 0), animData);
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
        NetworkManager.sendBlockHandle(this, new CatalystSyncPacket(worldPosition, collectedSouls, progress, isHyperEnchanting, maxSouls));
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
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
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
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
        return new BarConfiguration(7, 7, 18, 71);
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine", 4, state -> {
            if (state.getController().getAnimationState().equals(AnimationController.State.STOPPED))
                return state.setAndContinue(EMPTY);
            return PlayState.CONTINUE;
        })
                          .triggerableAnim("stabilized", STABILIZED)
                          .triggerableAnim("idle", IDLE)
                          .triggerableAnim("unstable", UNSTABLE)
                          .triggerableAnim("empty", EMPTY)
                          .setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
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
    public Container getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.CATALYST_SCREEN;
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
    
    public static void receiveUpdatePacket(CatalystSyncPacket packet, Level world, RegistryAccess dynamicRegistryManager) {
        if (world.getBlockEntity(packet.position) instanceof EnchantmentCatalystBlockEntity catalystBlock) {
            catalystBlock.isHyperEnchanting = packet.isHyperEnchanting();
            catalystBlock.progress = packet.progress();
            catalystBlock.collectedSouls = packet.storedSouls();
            catalystBlock.maxSouls = packet.maxSouls();
        }
    }
    
    public record CatalystSyncPacket(BlockPos position, int storedSouls, int progress, boolean isHyperEnchanting, int maxSouls) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<CatalystSyncPacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("catalyst"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
