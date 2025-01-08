package rearth.oritech.block.entity.interaction;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.PlayerModifierScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.AutoPlayingSoundKeyframeHandler;
import rearth.oritech.util.InventoryProvider;
import rearth.oritech.util.MultiblockMachineController;
import rearth.oritech.util.energy.EnergyApi;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class PlayerModifierTestEntity extends BlockEntity implements BlockEntityTicker<PlayerModifierTestEntity>, MultiblockMachineController, GeoBlockEntity, ExtendedScreenHandlerFactory {
    
    public static final Map<Identifier, PlayerAugment> allAugments = loadAugments();
    public final Set<Identifier> researchedAugments = new HashSet<>();
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    // working state
    private boolean networkDirty = false;
    
    public PlayerModifierTestEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PLAYER_MODIFIER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, PlayerModifierTestEntity blockEntity) {
        
        if (allAugments.isEmpty()) {
            loadAugments(); // this is loaded on both client and server to allow the UI to read the list without having to sync it
        }
        
        if (world.isClient) return;
        
        if (networkDirty) {
            updateNetwork();
        }
    }
    
    private static HashMap<Identifier, PlayerAugment> loadAugments() {
        
        var augments = new HashMap<Identifier, PlayerAugment>();
        
        var hpBoost = new PlayerStatEnhancingAugment(Oritech.id("hpboost"), EntityAttributes.GENERIC_MAX_HEALTH, 6, EntityAttributeModifier.Operation.ADD_VALUE);
        var hpBoostMore = new PlayerStatEnhancingAugment(Oritech.id("hpboostmore"), EntityAttributes.GENERIC_MAX_HEALTH, 4, EntityAttributeModifier.Operation.ADD_VALUE);
        var speedBoost = new PlayerStatEnhancingAugment(Oritech.id("speedboost"), EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5f, EntityAttributeModifier.Operation.ADD_VALUE, true);
        var dwarf = new PlayerStatEnhancingAugment(Oritech.id("dwarf"), EntityAttributes.GENERIC_SCALE, -0.5f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
        
        var energyCounter = new PlayerCountingAugment(Oritech.id("rf"));
        var otherCounter = new PlayerCountingAugment(Oritech.id("other"));
        var flight = new PlayerCountingAugment(Oritech.id("flight"), true) {
            @Override
            public void onInstalled(PlayerEntity player) {
                player.getAbilities().allowFlying = true;
                player.sendAbilitiesUpdate();
            }
            
            @Override
            public void onRemoved(PlayerEntity player) {
                player.getAbilities().allowFlying = false;
                player.getAbilities().flying = false;
                player.sendAbilitiesUpdate();
            }
            
            @Override
            public void onPlayerLoad(PlayerEntity player) {
                this.onInstalled(player);
            }
            
            @Override
            public void toggle(PlayerEntity player) {
                player.getAbilities().allowFlying = !player.getAbilities().allowFlying;
                player.sendAbilitiesUpdate();
            }
            
            @Override
            public boolean isEnabled(PlayerEntity player) {
                return player.getAbilities().allowFlying;
            }
        };
        
        // augment ideas:
        
        // auto feeder (consumes first found food, stores the remaining amount and keep player at full food). Toggleable
        // magnet (attracts items). Toggleable
        // creative flight
        // night vision
        // water breathing
        // ore finder. Toggleable
        // auto stepper (generic step height attribute)
        // giant (increases scale attribute).
        // dwarf (smaller scale)
        // energy provider (multiple levels, using overcharged crystals. Doesn't actually do anything).
        
        // temporary portal generator (with set home point)
        
        // bonus hp (multiple levels)
        // bonus armor (multiple levels)
        // bonus armor toughness
        // bonus knockback resistance
        // bonus mining speed (multiple levels). Toggleable
        // bonus reach. Toggleable
        // bonus walk/flight speed. Toggleable
        // bonus swim speed. Toggleable
        
        augments.put(hpBoost.id, hpBoost);
        augments.put(hpBoostMore.id, hpBoostMore);
        augments.put(speedBoost.id, speedBoost);
        augments.put(dwarf.id, dwarf);
        augments.put(energyCounter.id, energyCounter);
        augments.put(otherCounter.id, otherCounter);
        augments.put(flight.id, flight);
        
        return augments;
        
    }
    
    public void researchAugment(Identifier augment) {
        System.out.println("researching augment: " + augment);
        
        if (!allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        if (researchedAugments.contains(augment)) {
            Oritech.LOGGER.warn("Player tried to research already researched augment " + augment);
            return;
        }
        
        researchedAugments.add(augment);
        this.markNetDirty();
    }
    
    public void installAugmentToPlayer(Identifier augment, PlayerEntity player) {
        System.out.println("adding augment: " + augment);
        
        if (!allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        if (!researchedAugments.contains(augment)) {
            Oritech.LOGGER.warn("Player tried to install augment with id" + augment + " without researching it.");
            return;
        }
        
        var augmentInstance = allAugments.get(augment);
        augmentInstance.installToPlayer(player);
        this.markNetDirty();
    }
    
    public void removeAugmentFromPlayer(Identifier augment, PlayerEntity player) {
        System.out.println("removing augment: " + augment);
        
        if (!allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        var augmentInstance = allAugments.get(augment);
        augmentInstance.removeFromPlayer(player);
        this.markNetDirty();
    }
    
    public boolean hasPlayerAugment(Identifier augment, PlayerEntity player) {
        
        if (!allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return false;
        }
        
        var augmentInstance = allAugments.get(augment);
        return augmentInstance.isInstalled(player);
        
    }
    
    public void onPlayerLocked(PlayerEntity player) {
        
        for (var augmentId : allAugments.keySet()) {
            var augment = allAugments.get(augmentId);
            var isInstalled = augment.isInstalled(player);
            System.out.println(augmentId + ": " + isInstalled);
        }
        
    }
    
    public void onUse(PlayerEntity player) {
        onPlayerLocked(player);
        
        if (player.isSneaking()) {
            System.out.println("resetting all augments!");
            
            for (var augmentId : allAugments.keySet()) {
                var isInstalled = hasPlayerAugment(augmentId, player);
                if (isInstalled)
                    removeAugmentFromPlayer(augmentId, player);
            }
            
        }
        
    }
    
    private void markNetDirty() {
        this.networkDirty = true;
    }
    
    private void updateNetwork() {
        this.networkDirty = false;
        
        // collect researched augments, send them to client
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.AugmentResearchList(pos, researchedAugments.stream().toList()));
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
        var state = getCachedState();
        return state.get(Properties.HORIZONTAL_FACING).getOpposite();
    }
    
    @Override
    public BlockPos getMachinePos() {
        return pos;
    }
    
    @Override
    public World getMachineWorld() {
        return world;
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
    public InventoryProvider getInventoryForLink() {
        return null;
    }
    
    @Override
    public EnergyApi.EnergyContainer getEnergyStorageForLink() {
        return null;
    }
    
    @Override
    public void playSetupAnimation() {
    
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine", 5, state -> PlayState.CONTINUE)
                          .setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        return new ModScreens.BasicData(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.empty();
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        updateNetwork();
        return new PlayerModifierScreenHandler(syncId, playerInventory, this);
    }
    
    // called when a client connect to a server
    public static void refreshPlayerAugments(PlayerEntity player) {
        for (var augment : allAugments.values()) {
            if (augment.isInstalled(player))
                augment.onPlayerLoad(player);
        }
    }
    
    public static abstract class PlayerAugment {
        
        public final Identifier id;
        public final boolean toggleable;
        
        protected PlayerAugment(Identifier id, boolean toggleable) {
            this.id = id;
            this.toggleable = toggleable;
        }
        
        public abstract boolean isInstalled(PlayerEntity player);
        
        public abstract void installToPlayer(PlayerEntity player);
        
        public abstract void removeFromPlayer(PlayerEntity player);
        
        public void onInstalled(PlayerEntity player) {}
        public void onRemoved(PlayerEntity player) {}
        
        public void onPlayerLoad(PlayerEntity player) {}
        
        public void toggle(PlayerEntity player) {}
        
        public boolean isEnabled (PlayerEntity player) {return true;}
        
    }
    
    public static class PlayerCountingAugment extends PlayerAugment {
        
        private final AttachmentType<Integer> OWN_TYPE = AttachmentRegistry.<Integer>builder()
                                                                     .copyOnDeath()
                                                                     .persistent(Codec.INT)
                                                                     .initializer(() -> 0)
                                                                     .syncWith(PacketCodecs.VAR_INT.cast(), AttachmentSyncPredicate.targetOnly())   // todo either wait for FFAPI update or manually replace this
                                                                     .buildAndRegister(this.id);
        
        
        protected PlayerCountingAugment(Identifier id) {
            this(id, false);
        }
        protected PlayerCountingAugment(Identifier id, boolean toggleable) {
            super(id, toggleable);
        }
        
        @Override
        public boolean isInstalled(PlayerEntity player) {
            return player.hasAttached(OWN_TYPE);
        }
        
        @Override
        public void installToPlayer(PlayerEntity player) {
            player.setAttached(OWN_TYPE, 0);
            this.onInstalled(player);
        }
        
        @Override
        public void removeFromPlayer(PlayerEntity player) {
            player.removeAttached(OWN_TYPE);
            this.onRemoved(player);
        }
    }
    
    public static class PlayerStatEnhancingAugment extends PlayerAugment {
        
        private final RegistryEntry<EntityAttribute> targetAttribute;
        private final float amount;
        private final EntityAttributeModifier.Operation operation;
        
        protected PlayerStatEnhancingAugment(Identifier id, RegistryEntry<EntityAttribute> targetAttribute, float amount, EntityAttributeModifier.Operation operation) {
            this(id, targetAttribute, amount, operation, false);
        }
        
        protected PlayerStatEnhancingAugment(Identifier id, RegistryEntry<EntityAttribute> targetAttribute, float amount, EntityAttributeModifier.Operation operation, boolean toggleable) {
            super(id, toggleable);
            this.targetAttribute = targetAttribute;
            this.amount = amount;
            this.operation = operation;
        }
        
        @Override
        public boolean isInstalled(PlayerEntity player) {
            var instance = player.getAttributeInstance(targetAttribute);
            if (instance == null) return false;
            return instance.hasModifier(id);
        }
        
        @Override
        public void installToPlayer(PlayerEntity player) {
            var instance = player.getAttributeInstance(targetAttribute);
            if (instance == null) return;
            instance.overwritePersistentModifier(new EntityAttributeModifier(id, amount, operation));
            this.onInstalled(player);
        }
        
        @Override
        public void removeFromPlayer(PlayerEntity player) {
            var instance = player.getAttributeInstance(targetAttribute);
            if (instance == null) return;
            instance.removeModifier(id);
            this.onRemoved(player);
        }
        
        @Override
        public boolean isEnabled(PlayerEntity player) {
            if (!this.toggleable) return true;
            var instance = player.getAttributeInstance(targetAttribute);
            if (instance == null) return false;
            
            var modifier = instance.getModifier(id);
            if (modifier == null) return false;
            
            return modifier.value() == amount;
        }
        
        @Override
        public void toggle(PlayerEntity player) {
            var instance = player.getAttributeInstance(targetAttribute);
            if (instance == null) return;
            
            System.out.println("toggling: " + this.id.getPath());
            
            var modifier = instance.getModifier(id);
            if (modifier == null) return;
            
            var isActive = modifier.value() == amount;
            var newAmount = isActive ? 0 : amount;
            instance.overwritePersistentModifier(new EntityAttributeModifier(id, newAmount, operation));
        }
    }
    
}
