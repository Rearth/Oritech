package rearth.oritech.block.entity.arcane;

import io.wispforest.owo.util.VectorRandomUtils;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.EnchanterScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnchanterBlockEntity extends BlockEntity
  implements InventoryProvider, EnergyProvider, GeoBlockEntity, ScreenProvider, BlockEntityTicker<EnchanterBlockEntity>, ExtendedScreenHandlerFactory<ModScreens.BasicData> {
    
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation UNPOWERED = RawAnimation.begin().thenPlayAndHold("unpowered");
    public static final RawAnimation WORKING = RawAnimation.begin().thenPlay("working");
    
    public record EnchanterStatistics(int requiredCatalysts, int availableCatalysts){
        public static EnchanterStatistics EMPTY = new EnchanterStatistics(-1, -1);
    }
    
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(50000, 1000, 0) {
        @Override
        public void onFinalCommit() {
            super.onFinalCommit();
            EnchanterBlockEntity.this.markDirty();
        }
    };
    
    public final SimpleInventory inventory = new SimpleSidedInventory(2, new InventorySlotAssignment(0, 1, 1, 1)) {
        @Override
        public void markDirty() {
            EnchanterBlockEntity.this.markDirty();
        }
    };
    
    protected final InventoryStorage inventoryStorage = InventoryStorage.of(inventory, null);
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    public RegistryEntry<Enchantment> selectedEnchantment;
    public int progress;
    public int maxProgress = 10;
    private final List<EnchantmentCatalystBlockEntity> cachedCatalysts = new ArrayList<>();
    public EnchanterStatistics statistics = EnchanterStatistics.EMPTY; // used for client display
    private boolean networkDirty = false;
    private Identifier nbtLoadedSelection;
    private String activeAnimation = "idle";
    
    public EnchanterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENCHANTER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, EnchanterBlockEntity blockEntity) {
        
        if (world.isClient) return;
        
        if (networkDirty)
            updateNetwork();
        
        activeAnimation = "idle";
        // load data from nbt, as the registry entry is not available during the readNbt method
        if (nbtLoadedSelection != null && selectedEnchantment == null) {
            var registry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
            var selected = registry.getEntry(registry.get(nbtLoadedSelection));
            if (selected != null)
                selectedEnchantment = selected;
            nbtLoadedSelection = null;
        }
        
        // return early if there is no work to do
        statistics = EnchanterStatistics.EMPTY;
        var content = inventory.heldStacks.get(0);
        if (content.isEmpty()
              || !inventory.getStack(1).isEmpty()
              || !content.getItem().isEnchantable(content)
              || selectedEnchantment == null
              || !content.canBeEnchantedWith(selectedEnchantment, EnchantingContext.ACCEPTABLE)) {
            progress = 0;
            return;
        }
        
        var existingLevel = content.getEnchantments().getLevel(selectedEnchantment);
        var maxLevel = selectedEnchantment.value().getMaxLevel();
        
        if (existingLevel >= maxLevel) return;
        
        maxProgress = getEnchantmentCost(selectedEnchantment.value(), existingLevel + 1);
        
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
        
        if (networkDirty) {
            updateNetwork();
            updateAnimation();
        }
        
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putLong("energy", energyStorage.amount);
        if (selectedEnchantment != null) {
            nbt.putString("selected", selectedEnchantment.getIdAsString());
        } else {
            nbt.remove("selected");
        }
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        energyStorage.amount = nbt.getLong("energy");
        
        if (nbt.contains("selected")) {
            nbtLoadedSelection = Identifier.of(nbt.getString("selected"));
        }
    }
    
    private void finishEnchanting() {
        var content = inventory.heldStacks.get(0);
        var existingLevel = content.getEnchantments().getLevel(selectedEnchantment);
        content.addEnchantment(selectedEnchantment, existingLevel + 1);
        
        inventory.heldStacks.set(0, ItemStack.EMPTY);
        inventory.heldStacks.set(1, content);
        statistics = new EnchanterStatistics(0, cachedCatalysts.size());
    }
    
    private int getRequiredCatalystCount(int targetLevel) {
        return selectedEnchantment.value().getAnvilCost() + targetLevel;
    }
    
    private boolean canProgress(int targetLevel) {
        networkDirty = true;
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
        
        return true;
    }
    
    private int getEnchantmentCost(Enchantment enchantment, int targetLevel) {
        return enchantment.getAnvilCost() * targetLevel * 5 + 1;    // todo config parameter multiplicator
    }
    
    public void handleEnchantmentSelection(NetworkContent.EnchanterSelectionPacket packet) {
        System.out.println("got: " + packet.enchantment());
        
        if (packet.enchantment().isEmpty()) {
            selectedEnchantment = null;
            return;
        }
        
        var registry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var selected = registry.getEntry(registry.get(Identifier.of(packet.enchantment())));
        if (selected != null)
            selectedEnchantment = selected;
    }
    
    public void handleSyncPacket(NetworkContent.EnchanterSyncPacket message) {
        
        this.progress = message.progress();
        this.maxProgress = message.maxProgress();
        this.energyStorage.amount = message.energy();
        this.statistics = new EnchanterStatistics(message.requiredCatalysts(), message.availableCatalysts());
        
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
    
    private void updateAnimation() {
        triggerAnim("machine", activeAnimation);
    }
    
    private void updateNetwork() {
        networkDirty = false;
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.EnchanterSyncPacket(pos, energyStorage.amount, progress, maxProgress, statistics.requiredCatalysts, statistics.availableCatalysts));
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        networkDirty = true;
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
    public ModScreens.BasicData getScreenOpeningData(ServerPlayerEntity player) {
        networkDirty = true;
        return new ModScreens.BasicData(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.literal("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        sendSelectionToClient();
        networkDirty = true;
        return new EnchanterScreenHandler(syncId, playerInventory, this);
    }
    
    private void sendSelectionToClient() {
        if (selectedEnchantment == null) return;
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.EnchanterSelectionPacket(pos, selectedEnchantment.getIdAsString()));
    }
    
    @Override
    public EnergyStorage getStorage(Direction direction) {
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
    public InventoryStorage getInventory(Direction direction) {
        return inventoryStorage;
    }
}
