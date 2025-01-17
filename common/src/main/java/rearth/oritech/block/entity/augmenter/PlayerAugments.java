package rearth.oritech.block.entity.augmenter;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Vector2i;
import rearth.oritech.Oritech;
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.EntitiesContent;

import java.util.*;

public class PlayerAugments {
    public static final Map<Identifier, PlayerAugment> allAugments = new HashMap<>();
    public static final Map<Identifier, AugmentExtraData> augmentAssets = new HashMap<>();
    
    private static final PlayerAugment hpBoost = new PlayerStatEnhancingAugment(Oritech.id("hpboost"), EntityAttributes.GENERIC_MAX_HEALTH, 6, EntityAttributeModifier.Operation.ADD_VALUE);
    private static final PlayerAugment hpBoostMore = new PlayerStatEnhancingAugment(Oritech.id("hpboostmore"), EntityAttributes.GENERIC_MAX_HEALTH, 4, EntityAttributeModifier.Operation.ADD_VALUE);
    private static final PlayerAugment speedBoost = new PlayerStatEnhancingAugment(Oritech.id("speedboost"), EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5f, EntityAttributeModifier.Operation.ADD_VALUE, true);
    private static final PlayerAugment dwarf = new PlayerStatEnhancingAugment(Oritech.id("dwarf"), EntityAttributes.GENERIC_SCALE, -0.5f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
    private static final PlayerAugment giant = new PlayerStatEnhancingAugment(Oritech.id("giant"), EntityAttributes.GENERIC_SCALE, 2f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
    private static final PlayerAugment armor = new PlayerStatEnhancingAugment(Oritech.id("armor"), EntityAttributes.GENERIC_ARMOR, 4f, EntityAttributeModifier.Operation.ADD_VALUE);
    
    private static final PlayerAugment flight = new PlayerCustomAugment(Oritech.id("flight")) {
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
    
    private static final PlayerAugment cloak = new PlayerCustomAugment(Oritech.id("cloak"), true) {
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
    
    public static final PlayerAugment portal = new PlayerPortalAugment(Oritech.id("portal"), true);
    
    public static final PlayerAugment nightVision = new PlayerCustomAugment(Oritech.id("nightvision"), true) {
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
    
    public static final PlayerAugment waterBreathing = new PlayerCustomAugment(Oritech.id("waterbreath")) {
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
    public static final PlayerAugment autoFeeder = new PlayerTickingAugment(Oritech.id("autofeeder"), true) {
        
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
    
    public static final PlayerAugment magnet = new PlayerTickingAugment(Oritech.id("magnet"), true) {
        
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
    
    public static final PlayerAugment oreFinder = new PlayerTickingAugment(Oritech.id("orefinder"), true) {
        
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
    
    
    
    static {
        addAugmentAsset(hpBoost, 0, 50, List.of(), BlockContent.AUGMENTER_RESEARCH_STATION_SIMPLE_BLOCK);
        addAugmentAsset(speedBoost, 0, 90, List.of(), BlockContent.AUGMENTER_RESEARCH_STATION_SIMPLE_BLOCK);
        addAugmentAsset(hpBoostMore, 30, 50, List.of(Oritech.id("hpboost")), BlockContent.AUGMENTER_RESEARCH_STATION_ADVANCED_BLOCK);
        addAugmentAsset(dwarf, 40, 20, List.of(Oritech.id("hpboost")), BlockContent.AUGMENTER_RESEARCH_STATION_SIMPLE_BLOCK);
        addAugmentAsset(giant, 40, 60, List.of(Oritech.id("hpboost")), BlockContent.AUGMENTER_RESEARCH_STATION_ADVANCED_BLOCK);
        addAugmentAsset(autoFeeder, 50, 10, List.of(Oritech.id("hpboost")), BlockContent.AUGMENTER_RESEARCH_STATION_ADVANCED_BLOCK);
        addAugmentAsset(armor, 30, 90, List.of(), BlockContent.AUGMENTER_RESEARCH_STATION_SIMPLE_BLOCK);
        addAugmentAsset(flight, 70, 30, List.of(Oritech.id("hpboostmore")), BlockContent.AUGMENTER_RESEARCH_STATION_EXPERT_BLOCK);
        addAugmentAsset(cloak, 70, 70, List.of(Oritech.id("hpboostmore")), BlockContent.AUGMENTER_RESEARCH_STATION_EXPERT_BLOCK);
        addAugmentAsset(portal, 90, 90, List.of(Oritech.id("flight")), BlockContent.AUGMENTER_RESEARCH_STATION_SIMPLE_BLOCK);
        addAugmentAsset(nightVision, 90, 60, List.of(), BlockContent.AUGMENTER_RESEARCH_STATION_ADVANCED_BLOCK);
        addAugmentAsset(waterBreathing, 120, 70, List.of(), BlockContent.AUGMENTER_RESEARCH_STATION_ADVANCED_BLOCK);
        addAugmentAsset(magnet, 150, 40, List.of(), BlockContent.AUGMENTER_RESEARCH_STATION_SIMPLE_BLOCK);
        addAugmentAsset(oreFinder, 150, 70, List.of(Oritech.id("nightvision"), Oritech.id("magnet")), BlockContent.AUGMENTER_RESEARCH_STATION_EXPERT_BLOCK);
    }
    
    private static void addAugmentAsset(PlayerAugment augment, int x, int y, List<Identifier> requirements, Block requiredStation) {
        allAugments.put(augment.id, augment);
        augmentAssets.put(augment.id, new AugmentExtraData(augment.id, requirements, Registries.BLOCK.getId(requiredStation), new Vector2i(x, y)));
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
    
    public interface TickingAugment {
        void serverTick(PlayerEntity player);
        
        default void clientTick(PlayerEntity player) {}
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
    
    public record AugmentExtraData(Identifier id, List<Identifier> requirements, Identifier requiredStation, Vector2i position) {}
}
