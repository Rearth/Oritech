package rearth.oritech.block.entity.arcane;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import io.wispforest.owo.util.VectorRandomUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.InOutInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.EnchanterScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;

import rearth.oritech.util.AutoPlayingSoundKeyframeHandler;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class EnchanterBlockEntity extends NetworkedBlockEntity
  implements ItemApi.BlockProvider, EnergyApi.BlockProvider, GeoBlockEntity, ScreenProvider, ExtendedMenuProvider {
    
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation UNPOWERED = RawAnimation.begin().thenPlayAndHold("unpowered");
    public static final RawAnimation WORKING = RawAnimation.begin().thenPlay("working");
    
    public record EnchanterStatistics(int requiredCatalysts, int availableCatalysts){
        public static EnchanterStatistics EMPTY = new EnchanterStatistics(-1, -1);
    }
    
    @SyncField({SyncType.GUI_OPEN, SyncType.TICK})
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(50000, 1000, 0, this::setChanged);
    
    public final InOutInventoryStorage inventory = new InOutInventoryStorage(2, this::setChanged, new InventorySlotAssignment(0, 1, 1, 1));
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    public static final ResourceLocation NONE_SELECTED = ResourceLocation.parse("o:empty");
    
    @SyncField({SyncType.GUI_OPEN, SyncType.TICK})
    @NotNull
    public ResourceLocation selectedEnchantment = NONE_SELECTED;
    @SyncField({SyncType.GUI_OPEN, SyncType.TICK})
    public int progress;
    @SyncField({SyncType.GUI_OPEN, SyncType.TICK})
    public int maxProgress = 10;
    @SyncField({SyncType.GUI_OPEN, SyncType.TICK})
    public EnchanterStatistics statistics = EnchanterStatistics.EMPTY; // used for client display
    
    private final List<EnchantmentCatalystBlockEntity> cachedCatalysts = new ArrayList<>();
    private String activeAnimation = "idle";
    
    public EnchanterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENCHANTER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        activeAnimation = "idle";
        
        if (world.getGameTime() % 80 == 0)
            triggerAnim("machine", activeAnimation);
        
        // return early if there is no work to do
        statistics = EnchanterStatistics.EMPTY;
        var content = inventory.heldStacks.get(0);
        if (content.isEmpty()
              || !inventory.getItem(1).isEmpty()
              || !content.getItem().isEnchantable(content)
              || selectedEnchantment.equals(NONE_SELECTED)
              || !getSelectedEnchantment().isBound()
              || !getSelectedEnchantment().value().canEnchant(content)) {
            progress = 0;
            return;
        }
        
        var existingLevel = content.getEnchantments().getLevel(getSelectedEnchantment());
        var maxLevel = getSelectedEnchantment().value().getMaxLevel();
        
        if (existingLevel >= maxLevel) return;
        
        maxProgress = getEnchantmentCost(getSelectedEnchantment().value(), existingLevel + 1);
        
        if (canProgress(existingLevel + 1)) {
            this.setChanged();
            energyStorage.amount -= (long) getDisplayedEnergyUsage();
            progress++;
            activeAnimation = "working";
            
            var center = pos.getCenter();
            var offset = VectorRandomUtils.getRandomOffset(world, center, 4f);
            ParticleContent.WEED_KILLER.spawn(world, center, new ParticleContent.LineData(center, offset));
            
            if (progress >= maxProgress) {
                progress = 0;
                finishEnchanting();
                ParticleContent.ASSEMBLER_WORKING.spawn(world, pos.getCenter(), maxProgress + 10);
                activeAnimation = "idle";
            }
        }
        
    }
    
    @Override
    public void sendUpdate(SyncType type) {
        super.sendUpdate(type);
        triggerAnim("machine", activeAnimation);
    }
    
    public Holder<Enchantment> getSelectedEnchantment() {
        if (selectedEnchantment.equals(NONE_SELECTED)) return null;
        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        return registry.wrapAsHolder(registry.get(selectedEnchantment));
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putLong("energy", energyStorage.amount);
        nbt.putString("selected", selectedEnchantment.toString());
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        energyStorage.amount = nbt.getLong("energy");
        
        if (nbt.contains("selected")) {
            selectedEnchantment = ResourceLocation.parse(nbt.getString("selected"));
        }
    }
    
    private void finishEnchanting() {
        var content = inventory.heldStacks.get(0);
        var existingLevel = content.getEnchantments().getLevel(getSelectedEnchantment());
        content.enchant(getSelectedEnchantment(), existingLevel + 1);
        
        inventory.heldStacks.set(0, ItemStack.EMPTY);
        inventory.heldStacks.set(1, content);
        statistics = new EnchanterStatistics(0, cachedCatalysts.size());
    }
    
    private int getRequiredCatalystCount(int targetLevel) {
        return getSelectedEnchantment().value().getAnvilCost() + targetLevel;
    }
    
    private boolean canProgress(int targetLevel) {
        
        if (energyStorage.amount <= getDisplayedEnergyUsage()) {
            activeAnimation = "unpowered";
            return false;
        }
        
        if (level.getGameTime() % 15 == 0) updateNearbyCatalysts();
        var requiredCatalysts = getRequiredCatalystCount(targetLevel);
        
        statistics = new EnchanterStatistics(requiredCatalysts, cachedCatalysts.size());
        
        for (var catalyst : cachedCatalysts) {
            ParticleContent.CATALYST_CONNECTION.spawn(level, worldPosition.getCenter(), new ParticleContent.LineData(catalyst.getBlockPos().getCenter(), worldPosition.above().getCenter()));
        }
        
        if (cachedCatalysts.size() < requiredCatalysts) return false;
        
        // get a random entry where souls > 0
        Collections.shuffle(cachedCatalysts);
        var usedOne = cachedCatalysts.stream().filter(elem -> elem.collectedSouls > 0).findFirst();
        if (usedOne.isEmpty()) return false;
        
        usedOne.get().collectedSouls--;
        setChanged();
        
        return true;
    }
    
    private int getEnchantmentCost(Enchantment enchantment, int targetLevel) {
        return enchantment.getAnvilCost() * targetLevel * Oritech.CONFIG.enchanterCostMultiplier() + 1;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine", 4, state -> PlayState.CONTINUE)
                          .triggerableAnim("working", WORKING)
                          .triggerableAnim("idle", IDLE)
                          .triggerableAnim("unpowered", UNPOWERED)
                          .setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    private void updateNearbyCatalysts() {
        var chunkRadius = 1;
        
        var startX = (worldPosition.getX() >> 4) - chunkRadius;
        var startZ = (worldPosition.getZ() >> 4) - chunkRadius;
        var endX = (worldPosition.getX() >> 4) + chunkRadius;
        var endZ = (worldPosition.getZ() >> 4) + chunkRadius;
        
        cachedCatalysts.clear();
        
        for (int chunkX = startX; chunkX <= endX; chunkX++) {
            for (int chunkZ = startZ; chunkZ <= endZ; chunkZ++) {
                var chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                
                var entities = chunk.blockEntities;
                // select all non-empty catalysts within range (16)
                var catalysts = entities.values()
                                  .stream()
                                  .filter(elem -> elem instanceof EnchantmentCatalystBlockEntity catalyst && catalyst.collectedSouls > 0 && elem.getBlockPos().distManhattan(worldPosition) < 16)
                                  .map(elem -> (EnchantmentCatalystBlockEntity) elem)
                                  .toList();
                cachedCatalysts.addAll(catalysts);
            }
        }
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        this.sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new EnchanterScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 52, 58),
          new GuiSlot(1, 108, 58, true));
    }
    
    @Override
    public ArrowConfiguration getIndicatorConfiguration() {
        return new ArrowConfiguration(
          Oritech.id("textures/gui/modular/arrow_empty.png"),
          Oritech.id("textures/gui/modular/arrow_full.png"),
          73, 58, 29, 16, true);
    }
    
    @Override
    public BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(7, 7, 18, 71);
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return 512; // todo config parameter
    }
    
    @Override
    public float getProgress() {
        return (float) progress / maxProgress;
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
        return ModScreens.ENCHANTER_SCREEN;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    public static void receiveEnchantmentSelection(SelectEnchantingPacket packet, Player player, RegistryAccess dynamicRegistryManager) {
        var blockEntity = player.level().getBlockEntity(packet.self);
        if (blockEntity instanceof EnchanterBlockEntity enchanterBlock) {
            enchanterBlock.selectedEnchantment = packet.enchantmentId;
        }
    }
    
    public record SelectEnchantingPacket(BlockPos self, ResourceLocation enchantmentId) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<SelectEnchantingPacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("selected_enchant"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
}
