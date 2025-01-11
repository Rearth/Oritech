package rearth.oritech.block.entity.interaction;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import rearth.oritech.Oritech;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.client.ui.PlayerModifierScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.EntitiesContent;
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
    public static final Map<Identifier, AugmentExtraData> augmentAssets = loadAugmentAssets();
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
    
    private static HashMap<Identifier, AugmentExtraData> loadAugmentAssets() {
        
        var augments = new HashMap<Identifier, AugmentExtraData>();
        
        addAugmentAsset(augments, "hpboost", 0, 50, List.of());
        addAugmentAsset(augments, "speedboost", 0, 90, List.of());
        addAugmentAsset(augments, "hpboostmore", 30, 50, List.of(Oritech.id("hpboost")));
        addAugmentAsset(augments, "dwarf", 40, 20, List.of(Oritech.id("hpboost")));
        addAugmentAsset(augments, "giant", 40, 60, List.of(Oritech.id("hpboost")));
        addAugmentAsset(augments, "autofeeder", 50, 10, List.of(Oritech.id("hpboost")));
        addAugmentAsset(augments, "armor", 30, 90, List.of());
        addAugmentAsset(augments, "flight", 70, 30, List.of(Oritech.id("hpboostmore")));
        addAugmentAsset(augments, "cloak", 70, 70, List.of(Oritech.id("hpboostmore")));
        addAugmentAsset(augments, "portal", 90, 90, List.of(Oritech.id("flight")));
        addAugmentAsset(augments, "nightvision", 90, 60, List.of());
        addAugmentAsset(augments, "waterbreath", 120, 70, List.of());
        addAugmentAsset(augments, "magnet", 150, 40, List.of());
        addAugmentAsset(augments, "orefinder", 150, 70, List.of(Oritech.id("nightvision"), Oritech.id("magnet")));
        
        return augments;
        
    }
    
    private static void addAugmentAsset(HashMap<Identifier, AugmentExtraData> map, String idPath, int x, int y, List<Identifier> requirements) {
        var data = new AugmentExtraData(Oritech.id(idPath), requirements, new Vector2i(x, y));
        map.put(data.id, data);
    }
    
    private static HashMap<Identifier, PlayerAugment> loadAugments() {
        
        var augments = new HashMap<Identifier, PlayerAugment>();
        
        var hpBoost = new PlayerStatEnhancingAugment(Oritech.id("hpboost"), EntityAttributes.GENERIC_MAX_HEALTH, 6, EntityAttributeModifier.Operation.ADD_VALUE);
        var hpBoostMore = new PlayerStatEnhancingAugment(Oritech.id("hpboostmore"), EntityAttributes.GENERIC_MAX_HEALTH, 4, EntityAttributeModifier.Operation.ADD_VALUE);
        var speedBoost = new PlayerStatEnhancingAugment(Oritech.id("speedboost"), EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5f, EntityAttributeModifier.Operation.ADD_VALUE, true);
        var dwarf = new PlayerStatEnhancingAugment(Oritech.id("dwarf"), EntityAttributes.GENERIC_SCALE, -0.5f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
        var giant = new PlayerStatEnhancingAugment(Oritech.id("giant"), EntityAttributes.GENERIC_SCALE, 2f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
        var armor = new PlayerStatEnhancingAugment(Oritech.id("armor"), EntityAttributes.GENERIC_ARMOR, 0.5f, EntityAttributeModifier.Operation.ADD_VALUE, true);
        
        var flight = new PlayerCustomAugment(Oritech.id("flight")) {
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
                
                if (!player.getAbilities().allowFlying && player.getAbilities().flying)
                    player.getAbilities().flying = false;
                
                player.sendAbilitiesUpdate();
            }
            
            @Override
            public boolean isEnabled(PlayerEntity player) {
                return player.getAbilities().allowFlying;
            }
        };
        
        var cloak = new PlayerCustomAugment(Oritech.id("cloak"), true) {
            @Override
            public void onInstalled(PlayerEntity player) {
                player.setInvisible(true);
            }
            
            @Override
            public void onRemoved(PlayerEntity player) {
                player.setInvisible(false);
            }
            
            @Override
            public void onPlayerLoad(PlayerEntity player) {
                this.onInstalled(player);
            }
            
            @Override
            public void toggle(PlayerEntity player) {
                var isInvisible = player.isInvisible();
                player.setInvisible(!isInvisible);
            }
            
            @Override
            public boolean isEnabled(PlayerEntity player) {
                return player.isInvisible();
            }
        };
        
        var portal = new PlayerPortalAugment(Oritech.id("portal"), true);
        
        var nightVision = new PlayerCustomAugment(Oritech.id("nightvision"), true) {
            @Override
            public void onInstalled(PlayerEntity player) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 999999, 0, true, false, false));
            }
            
            @Override
            public void onRemoved(PlayerEntity player) {
                player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
            
            @Override
            public void toggle(PlayerEntity player) {
                
                if (player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    onRemoved(player);
                } else {
                    onInstalled(player);
                }
            }
            
            @Override
            public boolean isEnabled(PlayerEntity player) {
                return player.hasStatusEffect(StatusEffects.NIGHT_VISION);
            }
        };
        
        var waterBreathing = new PlayerCustomAugment(Oritech.id("waterbreath")) {
            @Override
            public void onInstalled(PlayerEntity player) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 999999, 0, true, false, false));
            }
            
            @Override
            public void onRemoved(PlayerEntity player) {
                player.removeStatusEffect(StatusEffects.WATER_BREATHING);
            }
            
            @Override
            public void toggle(PlayerEntity player) {
                
                if (player.hasStatusEffect(StatusEffects.WATER_BREATHING)) {
                    onRemoved(player);
                } else {
                    onInstalled(player);
                }
            }
            
            @Override
            public boolean isEnabled(PlayerEntity player) {
                return player.hasStatusEffect(StatusEffects.WATER_BREATHING);
            }
        };
        
        // stored int is the number of hunger bars current buffered
        var autoFeeder = new PlayerTickingAugment(Oritech.id("autofeeder"), true) {
            
            @Override
            public void serverTick(PlayerEntity player) {
                
                // ensure that player has at least 1 food missing
                var playerHungerCapacity = 20 - player.getHungerManager().getFoodLevel();
                if (playerHungerCapacity < 2) return;
                
                System.out.println(playerHungerCapacity);
                
                var storedFood = player.getAttached(getOwnType());
                if (storedFood == null) return;
                
                // we have food consumed/stored already, use it
                if (storedFood > 0) {
                    var usedFood = Math.min(playerHungerCapacity, storedFood);
                    player.getHungerManager().add(usedFood, 1f);
                    player.setAttached(getOwnType(), storedFood - usedFood);
                    System.out.println("fed amount: " + usedFood);
                } else {
                    var foodCandidate = player.getInventory().main.stream().filter(item -> item.contains(DataComponentTypes.FOOD)).findFirst();
                    if (foodCandidate.isPresent()) {
                        var foodSourceStack = foodCandidate.get();
                        var gainedFood = Objects.requireNonNull(foodSourceStack.get(DataComponentTypes.FOOD)).nutrition();
                        foodSourceStack.decrement(1);
                        player.setAttached(getOwnType(), gainedFood);
                        System.out.println("consumed food: " + gainedFood);
                    }
                }
                
            }
            
            @Override
            public void toggle(PlayerEntity player) {
                var value = player.getAttached(getOwnType());
                if (value == null) return;
                
                if (value >= 0) {
                    player.setAttached(getOwnType(), -1);
                } else {
                    player.setAttached(getOwnType(), 0);
                }
                
            }
            
            @Override
            public boolean isEnabled(PlayerEntity player) {
                var value = player.getAttached(getOwnType());
                return value != null && value >= 0;
            }
        };
        
        var magnet = new PlayerTickingAugment(Oritech.id("magnet"), true) {
            
            @Override
            public void serverTick(PlayerEntity player) {
                var world = player.getWorld();
                var target = player.getEyePos();
                
                if (world.getTime() % 2 == 0) return;
                
                var range = 8;
                var speed = 0.3;
                
                var box = new Box(target.x - range, target.y - range, target.z - range, target.x + range, target.y + range, target.z + range);
                var items = world.getEntitiesByClass(ItemEntity.class, box, itemEntity -> !itemEntity.cannotPickup());
                
                for (var item : items) {
                    var direction = target.subtract(item.getPos()).normalize().multiply(speed);
                    item.addVelocity(direction);
                }
            }
            
            @Override
            public void toggle(PlayerEntity player) {
                var value = player.getAttached(getOwnType());
                if (value == null) return;
                
                if (value >= 0) {
                    player.setAttached(getOwnType(), -1);
                } else {
                    player.setAttached(getOwnType(), 0);
                }
                
            }
            
            @Override
            public boolean isEnabled(PlayerEntity player) {
                var value = player.getAttached(getOwnType());
                return value != null && value >= 0;
            }
        };
        
        var oreFinder = new PlayerTickingAugment(Oritech.id("orefinder"), true) {
            
            @Override
            public void serverTick(PlayerEntity player) { }
            
            @Override
            public void clientTick(PlayerEntity player) {
                var world = player.getWorld();
                var target = BlockPos.ofFloored(player.getEyePos());
                
                if (world.getTime() % 20 != 0) return;
                
                var range = 16;
                
                var highlightPositions = new ArrayList<BlockPos>();
                BlockPos.iterate(target.getX() - range, target.getY() - range, target.getZ() - range, target.getX() + range, target.getY() + range, target.getZ() + range)
                  .forEach(pos -> {
                      var state = world.getBlockState(pos);
                      var isOre = state.isIn(ConventionalBlockTags.ORES);
                      if (isOre) highlightPositions.add(pos.toImmutable());
                  });
                
                if (!highlightPositions.isEmpty()) {
                    OreFinderRenderer.receivedAt = player.getWorld().getTime();
                    OreFinderRenderer.renderedBlocks = highlightPositions;
                }
                
            }
            
            @Override
            public void toggle(PlayerEntity player) {
                var value = player.getAttached(getOwnType());
                if (value == null) return;
                
                if (value >= 0) {
                    player.setAttached(getOwnType(), -1);
                } else {
                    player.setAttached(getOwnType(), 0);
                }
                
            }
            
            @Override
            public boolean isEnabled(PlayerEntity player) {
                var value = player.getAttached(getOwnType());
                return value != null && value >= 0;
            }
        };
        
        // augment ideas:
        
        // done: auto feeder (consumes first found food, stores the remaining amount and keep player at full food). Toggleable
        // done: magnet (attracts items). Toggleable
        // done: creative flight
        // done: night vision
        // done: water breathing
        // done: ore finder. Toggleable
        // done: giant (increases scale attribute).
        // done: dwarf (smaller scale)
        // energy provider (multiple levels, using overcharged crystals. Doesn't actually do anything).
        // done: invisibility (using player.setInvisible)
        
        // done: temporary portal generator (with set home point)
        
        // done: bonus hp (multiple levels)
        // done: bonus armor (multiple levels)
        // bonus armor toughness
        // bonus knockback resistance
        // bonus mining speed (multiple levels). Toggleable
        // bonus reach. Toggleable
        // bonus walk/flight speed. Toggleable
        // bonus swim speed. Toggleable
        // auto stepper (generic step height attribute)
        // bonus luck
        
        augments.put(hpBoost.id, hpBoost);
        augments.put(hpBoostMore.id, hpBoostMore);
        augments.put(speedBoost.id, speedBoost);
        augments.put(dwarf.id, dwarf);
        augments.put(giant.id, giant);
        augments.put(armor.id, armor);
        augments.put(flight.id, flight);
        augments.put(nightVision.id, nightVision);
        augments.put(autoFeeder.id, autoFeeder);
        augments.put(magnet.id, magnet);
        augments.put(waterBreathing.id, waterBreathing);
        augments.put(oreFinder.id, oreFinder);
        augments.put(cloak.id, cloak);
        augments.put(portal.id, portal);
        
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
    
    public static void toggleAugmentForPlayer(Identifier augment, PlayerEntity player) {
        System.out.println("toggling augment: " + augment);
        
        if (!allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        var augmentInstance = allAugments.get(augment);
        
        if (!augmentInstance.isInstalled(player)) {
            Oritech.LOGGER.error("Tried toggling not-installed augment id: " + augment + ". This should never happen");
            return;
        }
        
        augmentInstance.toggle(player);
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
    
    public static void serverTickAugments(PlayerEntity player) {
        for (var augment : allAugments.values()) {
            if (augment instanceof TickingAugment tickingAugment && augment.isInstalled(player) && augment.isEnabled(player))
                tickingAugment.serverTick(player);
        }
    }
    
    public static void clientTickAugments(PlayerEntity player) {
        for (var augment : allAugments.values()) {
            if (augment instanceof TickingAugment tickingAugment && augment.isInstalled(player) && augment.isEnabled(player))
                tickingAugment.clientTick(player);
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
    
    public static class PlayerCustomAugment extends PlayerAugment {
        
        private final AttachmentType<Integer> OWN_TYPE = AttachmentRegistry.<Integer>builder()
                                                                     .copyOnDeath()
                                                                     .persistent(Codec.INT)
                                                                     .initializer(() -> 0)
                                                                     .syncWith(PacketCodecs.VAR_INT.cast(), AttachmentSyncPredicate.targetOnly())   // todo either wait for FFAPI update or manually replace this
                                                                     .buildAndRegister(this.id);
        
        
        protected PlayerCustomAugment(Identifier id) {
            this(id, false);
        }
        protected PlayerCustomAugment(Identifier id, boolean toggleable) {
            super(id, toggleable);
        }
        
        public AttachmentType<Integer> getOwnType() {
            return OWN_TYPE;
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
    
    public static class PlayerPortalAugment extends PlayerAugment {
        
        private final AttachmentType<BlockPos> OWN_TYPE = AttachmentRegistry.<BlockPos>builder()
                                                           .copyOnDeath()
                                                           .persistent(BlockPos.CODEC)
                                                           .initializer(() -> BlockPos.ORIGIN)
                                                           .syncWith(BlockPos.PACKET_CODEC.cast(), AttachmentSyncPredicate.targetOnly())   // todo either wait for FFAPI update or manually replace this
                                                           .buildAndRegister(this.id);
        
        
        protected PlayerPortalAugment(Identifier id) {
            this(id, false);
        }
        protected PlayerPortalAugment(Identifier id, boolean toggleable) {
            super(id, toggleable);
        }
        
        public AttachmentType<BlockPos> getOwnType() {
            return OWN_TYPE;
        }
        
        @Override
        public boolean isInstalled(PlayerEntity player) {
            return player.hasAttached(OWN_TYPE);
        }
        
        @Override
        public void installToPlayer(PlayerEntity player) {
            player.setAttached(OWN_TYPE, player.getBlockPos());
            this.onInstalled(player);
        }
        
        @Override
        public void removeFromPlayer(PlayerEntity player) {
            player.removeAttached(OWN_TYPE);
            this.onRemoved(player);
        }
        
        @Override
        public void toggle(PlayerEntity player) {
            var world = player.getWorld();
            
            var hitResult = player.raycast(6, 0, false);
            var spawnPos = hitResult.getPos();
            var hitDist = Math.sqrt(hitResult.squaredDistanceTo(player));
            var spawnToPlayer = spawnPos.subtract(player.getPos()).normalize().multiply(0.3);
            spawnPos = spawnPos.subtract(spawnToPlayer);
            
            System.out.println(hitDist);
            
            var targetPos = player.getAttached(OWN_TYPE);
            if (targetPos == null) return;
            
            var portalEntity = EntitiesContent.PORTAL_ENTITY.create((ServerWorld) world, spawner -> {}, BlockPos.ofFloored(spawnPos), SpawnReason.EVENT, false, false);
            if (portalEntity != null) {
                portalEntity.setPosition(spawnPos);
                portalEntity.setYaw(-player.getYaw() + 90);
                world.spawnEntity(portalEntity);
                portalEntity.target = targetPos.toCenterPos();
                
                world.playSound(null, BlockPos.ofFloored(spawnPos), SoundEvents.AMBIENT_CAVE.value(), SoundCategory.BLOCKS, 2, 1.2f);
                
            }
        }
    }
    public abstract static class PlayerTickingAugment extends PlayerCustomAugment implements TickingAugment {
        
        protected PlayerTickingAugment(Identifier id) {
            super(id);
        }
        
        protected PlayerTickingAugment(Identifier id, boolean toggleable) {
            super(id, toggleable);
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
    
    public interface TickingAugment {
        void serverTick(PlayerEntity player);
        
        default void clientTick(PlayerEntity player) {}
    }
    
    public record AugmentExtraData(Identifier id, List<Identifier> requirements, Vector2i position) {}
    
}
