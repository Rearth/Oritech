package rearth.oritech.block.entity.augmenter;

import com.mojang.serialization.Codec;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.init.EntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.network.NetworkContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

// Todo refactor the whole handling here. Make all augments init as attachments, which can be of type stat, effect or custom.
public class PlayerAugments {
    public static final Map<Identifier, PlayerAugment> allAugments = new HashMap<>();

    private static final PlayerAugment hpBoost = new PlayerStatEnhancingAugment(Oritech.id("augment/hpboost"), EntityAttributes.GENERIC_MAX_HEALTH, 6, EntityAttributeModifier.Operation.ADD_VALUE);
    private static final PlayerAugment hpBoostMore = new PlayerStatEnhancingAugment(Oritech.id("augment/hpboostmore"), EntityAttributes.GENERIC_MAX_HEALTH, 4, EntityAttributeModifier.Operation.ADD_VALUE);
    private static final PlayerAugment hpBoostUltra = new PlayerStatEnhancingAugment(Oritech.id("augment/hpboostultra"), EntityAttributes.GENERIC_MAX_HEALTH, 10, EntityAttributeModifier.Operation.ADD_VALUE);
    private static final PlayerAugment hpBoostUltimate = new PlayerStatEnhancingAugment(Oritech.id("augment/hpboostultimate"), EntityAttributes.GENERIC_MAX_HEALTH, 10, EntityAttributeModifier.Operation.ADD_VALUE);
    private static final PlayerAugment speedBoost = new PlayerStatEnhancingAugment(Oritech.id("augment/speedboost"), EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, false);
    private static final PlayerAugment superSpeedBoost = new PlayerStatEnhancingAugment(Oritech.id("augment/superspeedboost"), EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25f, EntityAttributeModifier.Operation.ADD_VALUE, true);
    private static final PlayerAugment stepAssist = new PlayerStatEnhancingAugment(Oritech.id("augment/stepassist"), EntityAttributes.GENERIC_STEP_HEIGHT, 0.6f, EntityAttributeModifier.Operation.ADD_VALUE, true);
    private static final PlayerAugment dwarf = new PlayerStatEnhancingAugment(Oritech.id("augment/dwarf"), EntityAttributes.GENERIC_SCALE, -0.5f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
    private static final PlayerAugment giant = new PlayerStatEnhancingAugment(Oritech.id("augment/giant"), EntityAttributes.GENERIC_SCALE, 1f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
    private static final PlayerAugment armor = new PlayerStatEnhancingAugment(Oritech.id("augment/armor"), EntityAttributes.GENERIC_ARMOR, 4f, EntityAttributeModifier.Operation.ADD_VALUE);
    private static final PlayerAugment betterArmor = new PlayerStatEnhancingAugment(Oritech.id("augment/betterarmor"), EntityAttributes.GENERIC_ARMOR, 6f, EntityAttributeModifier.Operation.ADD_VALUE);
    private static final PlayerAugment ultimateArmor = new PlayerStatEnhancingAugment(Oritech.id("augment/ultimatearmor"), EntityAttributes.GENERIC_ARMOR, 8f, EntityAttributeModifier.Operation.ADD_VALUE);
    private static final PlayerAugment weaponReach = new PlayerStatEnhancingAugment(Oritech.id("augment/weaponreach"), EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, 0.3f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final PlayerAugment blockReach = new PlayerStatEnhancingAugment(Oritech.id("augment/blockreach"), EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE, 0.3f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final PlayerAugment farBlockReach = new PlayerStatEnhancingAugment(Oritech.id("augment/farblockreach"), EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE, 1f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
    private static final PlayerAugment miningSpeed = new PlayerStatEnhancingAugment(Oritech.id("augment/miningspeed"), EntityAttributes.PLAYER_BLOCK_BREAK_SPEED, 0.5f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
    private static final PlayerAugment superMiningSpeed = new PlayerStatEnhancingAugment(Oritech.id("augment/superminingspeed"), EntityAttributes.PLAYER_BLOCK_BREAK_SPEED, 3f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
    private static final PlayerAugment luck = new PlayerStatEnhancingAugment(Oritech.id("augment/luck"), EntityAttributes.GENERIC_LUCK, 500f, EntityAttributeModifier.Operation.ADD_VALUE, false);
    private static final PlayerAugment gravity = new PlayerStatEnhancingAugment(Oritech.id("augment/gravity"), EntityAttributes.GENERIC_GRAVITY, -0.5f, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, false);
    private static final PlayerAugment attackDamage = new PlayerStatEnhancingAugment(Oritech.id("augment/attackdamage"), EntityAttributes.GENERIC_ATTACK_DAMAGE, 4f, EntityAttributeModifier.Operation.ADD_VALUE, false, true);
    private static final PlayerAugment superAttackDamage = new PlayerStatEnhancingAugment(Oritech.id("augment/superattackdamage"), EntityAttributes.GENERIC_ATTACK_DAMAGE, 6f, EntityAttributeModifier.Operation.ADD_VALUE, false, true);

    private static final PlayerAugment flight = new PlayerCustomAugment(Oritech.id("augment/flight"), true) {
        @Override
        public void onInstalled(PlayerEntity player) {
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();

            if (autoSync && !player.getWorld().isClient)
                NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentOperationSyncPacket(this.id, AugmentOperation.ADD.ordinal()));
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

    private static final PlayerAugment cloak = new PlayerCustomAugment(Oritech.id("augment/cloak"), true) {
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

            if (autoSync && !player.getWorld().isClient)
                NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentOperationSyncPacket(this.id, AugmentOperation.ADD.ordinal()));
        }

        @Override
        public void toggle(PlayerEntity player) {
            super.toggle(player);
            var isInvisible = player.isInvisible();
            player.setInvisible(!isInvisible);
        }

        @Override
        public boolean isEnabled(PlayerEntity player) {
            return player.isInvisible();
        }
    };

    public static final PlayerAugment portal = new PlayerPortalAugment(Oritech.id("augment/portal"), true);

    public static final PlayerAugment nightVision = new PlayerCustomAugment(Oritech.id("augment/nightvision"), true) {
        @Override
        public void onInstalled(PlayerEntity player) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 9999999, 0, true, false, false));
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

    public static final PlayerAugment waterBreathing = new PlayerCustomAugment(Oritech.id("augment/waterbreath")) {
        @Override
        public void onInstalled(PlayerEntity player) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 9999999, 0, true, false, false));
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

    public static final PlayerAugment autoFeeder = new PlayerTickingAugment(Oritech.id("augment/autofeeder"), true) {

        @Override
        public void serverTick(PlayerEntity player) {

            // ensure that player has at least 1 food missing
            var playerHungerCapacity = 20 - player.getHungerManager().getFoodLevel();
            if (playerHungerCapacity < 2) return;
            
            var foodStackStream = player.getInventory().main.stream().filter(item -> item.contains(DataComponentTypes.FOOD) && !item.isIn(TagContent.FEEDER_BLACKLIST));
            var selectedFood = foodStackStream.reduce((a, b) -> Math.abs(a.get(DataComponentTypes.FOOD).nutrition() - playerHungerCapacity) <= Math.abs(b.get(DataComponentTypes.FOOD).nutrition() - playerHungerCapacity) ? a : b);
            selectedFood.ifPresent(food -> food.finishUsing(player.getWorld(), player));

        }

        @Override
        public void toggle(PlayerEntity player) {
            super.toggle(player);
            var value = AttachmentApi.getAttachmentValue(player, getOwnType());
            if (value == null) return;

            if (value >= 0) {
                AttachmentApi.setAttachment(player, getOwnType(), -1);
            } else {
                AttachmentApi.setAttachment(player, getOwnType(), 0);
            }

        }

        @Override
        public boolean isEnabled(PlayerEntity player) {
            var value = AttachmentApi.getAttachmentValue(player, getOwnType());
            return value != null && value >= 0;
        }
    };

    public static final PlayerAugment magnet = new PlayerTickingAugment(Oritech.id("augment/magnet"), true) {

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
            super.toggle(player);
            var value = AttachmentApi.getAttachmentValue(player, getOwnType());
            if (value == null) return;

            if (value >= 0) {
                AttachmentApi.setAttachment(player, getOwnType(), -1);
            } else {
                AttachmentApi.setAttachment(player, getOwnType(), 0);
            }

        }

        @Override
        public boolean isEnabled(PlayerEntity player) {
            var value = AttachmentApi.getAttachmentValue(player, getOwnType());
            return value != null && value >= 0;
        }
    };

    public static final PlayerAugment oreFinder = new PlayerTickingAugment(Oritech.id("augment/orefinder"), true) {

        @Override
        public void serverTick(PlayerEntity player) {
        }

        @Override
        public void clientTick(PlayerEntity player) {
            var world = player.getWorld();
            var target = BlockPos.ofFloored(player.getEyePos());

            if (world.getTime() % 5 != 0) return;

            var range = 16;

            var highlightPositions = new ArrayList<BlockPos>();
            BlockPos.iterate(target.getX() - range, target.getY() - range, target.getZ() - range, target.getX() + range, target.getY() + range, target.getZ() + range)
                    .forEach(pos -> {
                        var state = world.getBlockState(pos);
                        var isOre = state.isIn(TagContent.CONVENTIONAL_ORES);
                        if (isOre) highlightPositions.add(pos.toImmutable());
                    });

            if (!highlightPositions.isEmpty()) {
                OreFinderRenderer.receivedAt = player.getWorld().getTime();
                OreFinderRenderer.renderedBlocks = highlightPositions;
            }

        }

        @Override
        public void toggle(PlayerEntity player) {
            super.toggle(player);
            var value = AttachmentApi.getAttachmentValue(player, getOwnType());
            if (value == null) return;

            if (value >= 0) {
                AttachmentApi.setAttachment(player, getOwnType(), -1);
            } else {
                AttachmentApi.setAttachment(player, getOwnType(), 0);
            }

        }

        @Override
        public boolean isEnabled(PlayerEntity player) {
            var value = AttachmentApi.getAttachmentValue(player, getOwnType());
            return value != null && value >= 0;
        }
    };

    // layout:
    /*
    0   5       30      55      80      105          130        155     180     205     230
    10  att1    mining1 o       mining2 magnet      orevi       cloak   gravity o
    30  speed1  o       speed2  step    nightvi     portal      flight  o       o
    50  o       armor1  o       feeder  armor2      o           armor3  att2    hp4
    70  hp1     o       luck    hp2     o           weaponreach hp3     water   o
    90  o       dwarf   giant   portal  blockreach  farreach    o       o
    
     */


    public static void init() {
        
        if (EnergyApi.BLOCK == null) {
            System.out.println("APIs not defined, skipping augment init (if this is not a datagen run then something is very wrong)");
            return;
        }

        addAugmentAsset(hpBoost);
        addAugmentAsset(hpBoostMore);
        addAugmentAsset(hpBoostUltra);
        addAugmentAsset(hpBoostUltimate);
        addAugmentAsset(speedBoost);
        addAugmentAsset(superSpeedBoost);
        addAugmentAsset(stepAssist);
        addAugmentAsset(dwarf);
        addAugmentAsset(giant);
        addAugmentAsset(autoFeeder);
        addAugmentAsset(armor);
        addAugmentAsset(betterArmor);
        addAugmentAsset(ultimateArmor);
        addAugmentAsset(flight);
        addAugmentAsset(cloak);
        addAugmentAsset(portal);
        addAugmentAsset(nightVision);
        addAugmentAsset(weaponReach);
        addAugmentAsset(blockReach);
        addAugmentAsset(farBlockReach);
        addAugmentAsset(miningSpeed);
        addAugmentAsset(superMiningSpeed);
        addAugmentAsset(attackDamage);
        addAugmentAsset(superAttackDamage);
        addAugmentAsset(luck);
        addAugmentAsset(gravity);
        addAugmentAsset(waterBreathing);
        addAugmentAsset(magnet);
        addAugmentAsset(oreFinder);
    }

    private static void addAugmentAsset(PlayerAugment augment) {
        allAugments.put(augment.id, augment);
        augment.register();
    }

    // called when a client connect to a server
    public static void refreshPlayerAugments(PlayerEntity player) {
        for (var augment : allAugments.values()) {
            if (augment.isInstalled(player))
                augment.onPlayerLoad(player);
            
            // migrate old augment IDs
            // this can be removed in a few versions
            if (augment instanceof PlayerStatEnhancingAugment statEnhancingAugment) {
                
                var oldId = statEnhancingAugment.id.withPath(statEnhancingAugment.id.getPath().replace("augment/", ""));
                var instance = player.getAttributeInstance(statEnhancingAugment.targetAttribute);
                if (instance == null) continue;
                if (instance.hasModifier(oldId)) {
                    Oritech.LOGGER.info("Removing old augment id {} from player {}", oldId, player.getName().getString());
                    instance.removeModifier(oldId);
                }
            }
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

    public enum AugmentOperation {
        RESEARCH, ADD, REMOVE, NEEDS_INIT, TOGGLE, NONE
    }

    public interface TickingAugment {
        void serverTick(PlayerEntity player);

        default void clientTick(PlayerEntity player) {
        }
    }

    public static abstract class PlayerAugment {

        public final Identifier id;
        public final boolean toggleable;
        public final boolean autoSync;

        protected PlayerAugment(Identifier id, boolean toggleable, boolean autoSync) {
            this.id = id;
            this.toggleable = toggleable;
            this.autoSync = autoSync;
        }

        public abstract boolean isInstalled(PlayerEntity player);

        public abstract void installToPlayer(PlayerEntity player);

        public abstract void removeFromPlayer(PlayerEntity player);

        public void onInstalled(PlayerEntity player) {
        }

        public void onRemoved(PlayerEntity player) {
        }

        public void onPlayerLoad(PlayerEntity player) {

            if (autoSync && !player.getWorld().isClient) {
                NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentOperationSyncPacket(this.id, AugmentOperation.ADD.ordinal()));
                if (toggleable && !isEnabled(player))   // send disabled status to client aswell
                    NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentOperationSyncPacket(this.id, AugmentOperation.TOGGLE.ordinal()));
            }

        }

        public void toggle(PlayerEntity player) {
        }

        public boolean isEnabled(PlayerEntity player) {
            return true;
        }

        public void register() {
        }

    }

    @SuppressWarnings("UnstableApiUsage")
    public static class PlayerCustomAugment extends PlayerAugment {

        private Attachment<Integer> OWN_TYPE;

        protected PlayerCustomAugment(Identifier id) {
            this(id, false);
        }

        protected PlayerCustomAugment(Identifier id, boolean toggleable) {
            super(id, toggleable, true);
        }

        @Override
        public void register() {
            
            OWN_TYPE = new Attachment<>() {
                
                @Override
                public Identifier identifier() {
                    return id;
                }
                
                @Override
                public Codec<Integer> persistenceCodec() {
                    return Codec.INT;
                }
                
                @Override
                public Supplier<Integer> initializer() {
                    return () -> 1;
                }
            };
            AttachmentApi.register(OWN_TYPE);
        }

        public Attachment<Integer> getOwnType() {
            return OWN_TYPE;
        }

        @Override
        public boolean isInstalled(PlayerEntity player) {
            return AttachmentApi.hasAttachment(player, OWN_TYPE);
        }

        @Override
        public void installToPlayer(PlayerEntity player) {
            AttachmentApi.setAttachment(player, OWN_TYPE, 0);
            this.onInstalled(player);

            if (autoSync && !player.getWorld().isClient)
                NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentOperationSyncPacket(this.id, AugmentOperation.ADD.ordinal()));
        }

        @Override
        public void removeFromPlayer(PlayerEntity player) {
            AttachmentApi.removeAttachment(player, OWN_TYPE);
            this.onRemoved(player);

            if (autoSync && !player.getWorld().isClient)
                NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentOperationSyncPacket(this.id, AugmentOperation.REMOVE.ordinal()));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class PlayerPortalAugment extends PlayerAugment {
        
        private Attachment<GlobalPos> OWN_TYPE;
        protected PlayerPortalAugment(Identifier id, boolean toggleable) {
            super(id, toggleable, true);
        }

        @Override
        public void register() {
            
            OWN_TYPE = new Attachment<>() {
                @Override
                public Identifier identifier() {
                    return id;
                }
                
                @Override
                public Codec<GlobalPos> persistenceCodec() {
                    return GlobalPos.CODEC;
                }
                
                @Override
                public Supplier<GlobalPos> initializer() {
                    return () -> GlobalPos.create(World.OVERWORLD, BlockPos.ORIGIN);
                }
            };
            
            AttachmentApi.register(OWN_TYPE);
        }

        public Attachment<GlobalPos> getOwnType() {
            return OWN_TYPE;
        }

        @Override
        public boolean isInstalled(PlayerEntity player) {
            return AttachmentApi.hasAttachment(player, OWN_TYPE);
        }

        @Override
        public void installToPlayer(PlayerEntity player) {
            AttachmentApi.setAttachment(player, OWN_TYPE, GlobalPos.create(
              player.getWorld().getRegistryKey(),
              player.getBlockPos()
            ));

            this.onInstalled(player);

            if (autoSync && !player.getWorld().isClient)
                NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentOperationSyncPacket(this.id, AugmentOperation.ADD.ordinal()));
        }

        @Override
        public void removeFromPlayer(PlayerEntity player) {
            AttachmentApi.removeAttachment(player, OWN_TYPE);
            this.onRemoved(player);

            if (autoSync && !player.getWorld().isClient)
                NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentOperationSyncPacket(this.id, AugmentOperation.REMOVE.ordinal()));
        }

        @Override
        public void toggle(PlayerEntity player) {
            super.toggle(player);
            var world = player.getWorld();
            if (world.isClient) return;

            var hitResult = player.raycast(6, 0, false);
            var spawnPos = hitResult.getPos();
            var hitDist = Math.sqrt(hitResult.squaredDistanceTo(player));
            var spawnToPlayer = spawnPos.subtract(player.getPos()).normalize().multiply(0.3);
            spawnPos = spawnPos.subtract(spawnToPlayer);

            var targetPos = AttachmentApi.getAttachmentValue(player, OWN_TYPE);
            if (targetPos == null) return;

            var portalEntity = EntitiesContent.PORTAL_ENTITY.create((ServerWorld) world, spawner -> {
            }, BlockPos.ofFloored(spawnPos), SpawnReason.EVENT, false, false);
            if (portalEntity != null) {
                portalEntity.setPosition(spawnPos);
                portalEntity.setYaw(-player.getYaw() + 90);
                world.spawnEntity(portalEntity);
                portalEntity.target = targetPos;
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
            this(id, targetAttribute, amount, operation, false, false);
        }

        protected PlayerStatEnhancingAugment(Identifier id, RegistryEntry<EntityAttribute> targetAttribute, float amount, EntityAttributeModifier.Operation operation, boolean toggleable) {
            this(id, targetAttribute, amount, operation, toggleable, false);
        }

        protected PlayerStatEnhancingAugment(Identifier id, RegistryEntry<EntityAttribute> targetAttribute, float amount, EntityAttributeModifier.Operation operation, boolean toggleable, boolean autoSync) {
            super(id, toggleable, autoSync);
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
            
            if (autoSync && !player.getWorld().isClient)
                NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentOperationSyncPacket(this.id, AugmentOperation.ADD.ordinal()));
        }

        @Override
        public void removeFromPlayer(PlayerEntity player) {
            var instance = player.getAttributeInstance(targetAttribute);
            if (instance == null) return;
            instance.removeModifier(id);
            this.onRemoved(player);

            if (autoSync && !player.getWorld().isClient)
                NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentOperationSyncPacket(this.id, AugmentOperation.REMOVE.ordinal()));
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

            var modifier = instance.getModifier(id);
            if (modifier == null) return;

            var isActive = modifier.value() == amount;
            var newAmount = isActive ? 0 : amount;
            instance.overwritePersistentModifier(new EntityAttributeModifier(id, newAmount, operation));
        }
    }
}
