package rearth.oritech.block.entity.arcane;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import io.wispforest.owo.util.VectorRandomUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
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
import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;
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

public class EnchanterBlockEntity extends NetworkedBlockEntity
  implements ItemApi.BlockProvider, EnergyApi.BlockProvider, GeoBlockEntity, ScreenProvider, ExtendedMenuProvider {
    
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation UNPOWERED = RawAnimation.begin().thenPlayAndHold("unpowered");
    public static final RawAnimation WORKING = RawAnimation.begin().thenPlay("working");
    
    public record EnchanterStatistics(int requiredCatalysts, int availableCatalysts){
        public static EnchanterStatistics EMPTY = new EnchanterStatistics(-1, -1);
    }
    
    @SyncField({SyncType.GUI_OPEN, SyncType.TICK})
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(50000, 1000, 0, this::markDirty);
    
    public final InOutInventoryStorage inventory = new InOutInventoryStorage(2, this::markDirty, new InventorySlotAssignment(0, 1, 1, 1));
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    public static final Identifier NONE_SELECTED = Identifier.of("o:empty");
    
    @SyncField({SyncType.GUI_OPEN, SyncType.TICK})
    @NotNull
    public Identifier selectedEnchantment = NONE_SELECTED;
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
    public void serverTick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        activeAnimation = "idle";
        
        if (world.getTime() % 80 == 0)
            triggerAnim("machine", activeAnimation);
        
        // return early if there is no work to do
        statistics = EnchanterStatistics.EMPTY;
        var content = inventory.heldStacks.get(0);
        if (content.isEmpty()
              || !inventory.getStack(1).isEmpty()
              || !content.getItem().isEnchantable(content)
              || selectedEnchantment.equals(NONE_SELECTED)
              || !getSelectedEnchantment().hasKeyAndValue()
              || !getSelectedEnchantment().value().isAcceptableItem(content)) {
            progress = 0;
            return;
        }
        
        var existingLevel = content.getEnchantments().getLevel(getSelectedEnchantment());
        var maxLevel = getSelectedEnchantment().value().getMaxLevel();
        
        if (existingLevel >= maxLevel) return;
        
        maxProgress = getEnchantmentCost(getSelectedEnchantment().value(), existingLevel + 1);
        
        if (canProgress(existingLevel + 1)) {
            this.markDirty();
            energyStorage.amount -= (long) getDisplayedEnergyUsage();
            progress++;
            activeAnimation = "working";
            
            var center = pos.toCenterPos();
            var offset = VectorRandomUtils.getRandomOffset(world, center, 4f);
            ParticleContent.WEED_KILLER.spawn(world, center, new ParticleContent.LineData(center, offset));
            
            if (progress >= maxProgress) {
                progress = 0;
                finishEnchanting();
                ParticleContent.ASSEMBLER_WORKING.spawn(world, pos.toCenterPos(), maxProgress + 10);
                activeAnimation = "idle";
            }
        }
        
    }
    
    @Override
    public void sendUpdate(SyncType type) {
        super.sendUpdate(type);
        triggerAnim("machine", activeAnimation);
    }
    
    public RegistryEntry<Enchantment> getSelectedEnchantment() {
        if (selectedEnchantment.equals(NONE_SELECTED)) return null;
        var registry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        return registry.getEntry(registry.get(selectedEnchantment));
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putLong("energy", energyStorage.amount);
        nbt.putString("selected", selectedEnchantment.toString());
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        energyStorage.amount = nbt.getLong("energy");
        
        if (nbt.contains("selected")) {
            selectedEnchantment = Identifier.of(nbt.getString("selected"));
        }
    }
    
    private void finishEnchanting() {
        var content = inventory.heldStacks.get(0);
        var existingLevel = content.getEnchantments().getLevel(getSelectedEnchantment());
        content.addEnchantment(getSelectedEnchantment(), existingLevel + 1);
        
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
        
        if (world.getTime() % 15 == 0) updateNearbyCatalysts();
        var requiredCatalysts = getRequiredCatalystCount(targetLevel);
        
        statistics = new EnchanterStatistics(requiredCatalysts, cachedCatalysts.size());
        
        for (var catalyst : cachedCatalysts) {
            ParticleContent.CATALYST_CONNECTION.spawn(world, pos.toCenterPos(), new ParticleContent.LineData(catalyst.getPos().toCenterPos(), pos.up().toCenterPos()));
        }
        
        if (cachedCatalysts.size() < requiredCatalysts) return false;
        
        // get a random entry where souls > 0
        Collections.shuffle(cachedCatalysts);
        var usedOne = cachedCatalysts.stream().filter(elem -> elem.collectedSouls > 0).findFirst();
        if (usedOne.isEmpty()) return false;
        
        usedOne.get().collectedSouls--;
        markDirty();
        
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
        
        var startX = (pos.getX() >> 4) - chunkRadius;
        var startZ = (pos.getZ() >> 4) - chunkRadius;
        var endX = (pos.getX() >> 4) + chunkRadius;
        var endZ = (pos.getZ() >> 4) + chunkRadius;
        
        cachedCatalysts.clear();
        
        for (int chunkX = startX; chunkX <= endX; chunkX++) {
            for (int chunkZ = startZ; chunkZ <= endZ; chunkZ++) {
                var chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                
                var entities = chunk.blockEntities;
                // select all non-empty catalysts within range (16)
                var catalysts = entities.values()
                                  .stream()
                                  .filter(elem -> elem instanceof EnchantmentCatalystBlockEntity catalyst && catalyst.collectedSouls > 0 && elem.getPos().getManhattanDistance(pos) < 16)
                                  .map(elem -> (EnchantmentCatalystBlockEntity) elem)
                                  .toList();
                cachedCatalysts.addAll(catalysts);
            }
        }
    }
    
    @Override
    public void saveExtraData(PacketByteBuf buf) {
        this.sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.literal("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
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
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
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
    
    public static void receiveEnchantmentSelection(SelectEnchantingPacket packet, PlayerEntity player, DynamicRegistryManager dynamicRegistryManager) {
        var blockEntity = player.getWorld().getBlockEntity(packet.self);
        if (blockEntity instanceof EnchanterBlockEntity enchanterBlock) {
            enchanterBlock.selectedEnchantment = packet.enchantmentId;
        }
    }
    
    public record SelectEnchantingPacket(BlockPos self, Identifier enchantmentId) implements CustomPayload {
        
        public static final CustomPayload.Id<SelectEnchantingPacket> PACKET_ID = new CustomPayload.Id<>(Oritech.id("selected_enchant"));
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }
    
}
