package rearth.oritech.block.entity.arcane;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.CatalystScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.AutoPlayingSoundKeyframeHandler;
import rearth.oritech.util.EnergyProvider;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.InventoryProvider;
import rearth.oritech.util.ScreenProvider;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;

public class EnchantmentCatalystBlockEntity extends BaseSoulCollectionEntity
  implements InventoryProvider, EnergyProvider, ScreenProvider, GeoBlockEntity, BlockEntityTicker<EnchantmentCatalystBlockEntity>, ExtendedScreenHandlerFactory<ModScreens.BasicData> {
    
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation STABILIZED = RawAnimation.begin().thenLoop("stabilized");
    public static final RawAnimation UNSTABLE = RawAnimation.begin().thenLoop("unstable");
    
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
    private String lastAnimation = "idle";
    private int lastComparatorOutput;
    
    public final SimpleInventory inventory = new SimpleInventory(2) {
        @Override
        public void markDirty() {
            EnchantmentCatalystBlockEntity.this.markDirty();
        }
    };
    
    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(50000, 10000, 0) {
        @Override
        protected void onFinalCommit() {
            EnchantmentCatalystBlockEntity.this.markDirty();
        }
    };
    
    protected final InventoryStorage inventoryStorage = InventoryStorage.of(inventory, null);
    
    public EnchantmentCatalystBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENCHANTMENT_CATALYST_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, EnchantmentCatalystBlockEntity blockEntity) {
        
        if (world.isClient) return;
        
        // check if powered, and adjust soul capacity
        if (energyStorage.amount > 0) {
            var gainedSoulCapacity = energyStorage.amount / Oritech.CONFIG.catalystRFPerSoul();
            energyStorage.amount = 0;
            var newMax = baseSoulCapacity + gainedSoulCapacity;
            adjustMaxSouls(newMax);
        } else if (maxSouls > baseSoulCapacity) {
            adjustMaxSouls(baseSoulCapacity);
        }
        
        // explode if unstable
        if (collectedSouls > maxSouls) {
            unstableTicks++;
            
            ParticleContent.MELTDOWN_IMMINENT.spawn(world, pos.toCenterPos(), unstableTicks / 4);
            
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
            
            ParticleContent.SOUL_USED.spawn(world, pos.toCenterPos().add(0, 0.3, 0), isHyperEnchanting ? 15 : 3);
            
            if (progress >= maxProgress) {
                enchantInput();
                ParticleContent.ASSEMBLER_WORKING.spawn(world, pos.toCenterPos(), maxProgress + 10);
                
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
                world.updateComparators(pos, state.getBlock());
            }
            
        }
        
        // periodically re-trigger animation updates
        if (world.getTime() % 60 == 0)
            lastAnimation = "idle";
        
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putInt("souls", collectedSouls);
        nbt.putInt("maxSouls", maxSouls);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        collectedSouls = nbt.getInt("souls");
        maxSouls = nbt.getInt("maxSouls");
    }
    
    private void doExplosion() {
        
        var center = pos.toCenterPos();
        var strength = Math.sqrt(collectedSouls - baseSoulCapacity);
        
        world.createExplosion(null, center.x, center.y, center.z, (int) strength, true, World.ExplosionSourceType.BLOCK);
        world.removeBlock(pos, false);
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
        
        var bookCandidate = inventory.getStack(0);
        if (!bookCandidate.getItem().equals(Items.ENCHANTED_BOOK) || !bookCandidate.contains(DataComponentTypes.STORED_ENCHANTMENTS))
            return;
        
        var enchantment = bookCandidate.get(DataComponentTypes.STORED_ENCHANTMENTS).getEnchantments().stream().findFirst().get();
        
        var inputStack = inventory.getStack(1);
        var toolLevel = inputStack.getEnchantments().getLevel(enchantment);
        inputStack.addEnchantment(enchantment, toolLevel + 1);
        
        collectedSouls -= getEnchantmentCost(enchantment.value(), toolLevel + 1, isHyperEnchanting);
        
        if (isHyperEnchanting)
            inventory.setStack(0, ItemStack.EMPTY);
        
    }
    
    private boolean hasEnoughSouls(Enchantment enchantment, int targetLevel) {
        var resultingCost = getEnchantmentCost(enchantment, targetLevel, isHyperEnchanting);
        return collectedSouls >= resultingCost;
    }
    
    private int getEnchantmentCost(Enchantment enchantment, int targetLevel, boolean hyper) {
        var baseCost = enchantment.getAnvilCost();
        var resultingCost = baseCost * targetLevel * Oritech.CONFIG.catalystCostMultiplier();
        if (hyper) resultingCost = resultingCost * Oritech.CONFIG.catalystHyperMultiplier() + Oritech.CONFIG.catalystBaseSouls();
        return resultingCost;
    }
    
    // for UI
    public int getDisplayedCost() {
        if (inventory.getStack(0).isEmpty() || inventory.getStack(1).isEmpty()) return 0;
        var bookCandidate = inventory.getStack(0);
        
        if (bookCandidate.getItem().equals(Items.ENCHANTED_BOOK) && bookCandidate.contains(DataComponentTypes.STORED_ENCHANTMENTS)) {
            
            var enchantment = bookCandidate.get(DataComponentTypes.STORED_ENCHANTMENTS).getEnchantments().stream().findFirst().get();
            var maxLevel = enchantment.value().getMaxLevel();
            var bookLevel = bookCandidate.get(DataComponentTypes.STORED_ENCHANTMENTS).getLevel(enchantment);
            
            if (bookLevel != maxLevel) return 0;
            
            var inputStack = inventory.getStack(1);
            var toolLevel = inputStack.getEnchantments().getLevel(enchantment);
            var isHyper = toolLevel >= maxLevel;
            
            return getEnchantmentCost(enchantment.value(), toolLevel + 1, isHyper);
        }
        
        return 0;
    }
    
    private boolean canProceed() {
        
        if (inventory.getStack(0).isEmpty() || inventory.getStack(1).isEmpty()) return false;
        
        var bookCandidate = inventory.getStack(0);
        if (bookCandidate.getItem().equals(Items.ENCHANTED_BOOK) && bookCandidate.contains(DataComponentTypes.STORED_ENCHANTMENTS)) {
            
            var enchantment = bookCandidate.get(DataComponentTypes.STORED_ENCHANTMENTS).getEnchantments().stream().findFirst().get();
            var maxLevel = enchantment.value().getMaxLevel();
            var level = bookCandidate.get(DataComponentTypes.STORED_ENCHANTMENTS).getLevel(enchantment);
            
            // yes this does not check if the item can be enchanted with this enchantment. This is intentional, allowing you to skip the normal limitations
            var inputStack = inventory.getStack(1);
            var toolLevel = inputStack.getEnchantments().getLevel(enchantment);
            this.isHyperEnchanting = toolLevel >= maxLevel;
            
            return level == maxLevel && hasEnoughSouls(enchantment.value(), toolLevel + 1);
        }
        
        return false;
    }
    
    @Override
    public void onSoulIncoming(Vec3d source) {
        var distance = (float) source.distanceTo(pos.toCenterPos());
        collectedSouls++;
        networkDirty = true;
        
        var soulPath = pos.toCenterPos().subtract(source);
        var animData = new ParticleContent.SoulParticleData(soulPath, (int) getSoulTravelDuration(distance));
        
        ParticleContent.WANDERING_SOUL.spawn(world, source.add(0, 0.7f, 0), animData);
    }
    
    @Override
    public boolean canAcceptSoul() {
        return collectedSouls < maxSouls;
    }
    
    public int getComparatorOutput() {
        return calculateComparatorLevel();
    }
    
    private int calculateComparatorLevel() {
        return (int) ((float) collectedSouls / maxSouls * 16);
    }
    
    private void updateNetwork() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.CatalystSyncPacket(pos, collectedSouls, progress, isHyperEnchanting, maxSouls));
    }
    
    public void handleNetworkPacket(NetworkContent.CatalystSyncPacket packet) {
        this.isHyperEnchanting = packet.isHyperEnchanting();
        this.progress = packet.progress();
        this.collectedSouls = packet.storedSouls();
        this.maxSouls = packet.maxSouls();
    }
    
    @Override
    public ModScreens.BasicData getScreenOpeningData(ServerPlayerEntity player) {
        return new ModScreens.BasicData(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.literal("");
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        updateNetwork();
        return new CatalystScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public InventoryStorage getInventory(Direction direction) {
        return inventoryStorage;
    }

    @Override
    public EnergyStorage getStorage(Direction direction) {
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
                return state.setAndContinue(IDLE);
            return PlayState.CONTINUE;
        })
                          .triggerableAnim("stabilized", STABILIZED)
                          .triggerableAnim("idle", IDLE)
                          .triggerableAnim("unstable", UNSTABLE)
                          .setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
    }
    
    private void updateAnimation() {
        
        var targetAnim = "idle";
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
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
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
}
