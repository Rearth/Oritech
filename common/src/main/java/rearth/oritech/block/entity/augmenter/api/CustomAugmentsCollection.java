package rearth.oritech.block.entity.augmenter.api;

import com.mojang.serialization.Codec;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
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
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.init.EntitiesContent;
import rearth.oritech.init.TagContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CustomAugmentsCollection {
    
    // for other modders: If you want to use a custom augment from a recipe, you need to add it to this map before world load!
    public static final Map<Identifier, Augment> CUSTOM_AUGMENTS = new HashMap<>();
    
    public static final Attachment<GlobalPos> PORTAL_TARGET_TYPE = new Attachment<>() {
        @Override
        public Identifier identifier() {
            return Oritech.id("portal_target");
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
    
    public static Augment getById(Identifier id) {
        return CUSTOM_AUGMENTS.get(id);
    }
    
    public static final Augment flight = new Augment(Oritech.id("augment/flight"), true) {
        @Override
        public void activate(PlayerEntity player) {
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();
        }
        
        @Override
        public void deactivate(PlayerEntity player) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
        }
        
        @Override
        public void refreshServer(PlayerEntity player) {
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();
        }
        
        @Override
        public int refreshInterval() {
            return 80;
        }
    };
    
    public static final Augment feeder = new Augment(Oritech.id("augment/autofeeder"), true) {
        @Override
        public void activate(PlayerEntity player) {
        }
        
        @Override
        public void deactivate(PlayerEntity player) {
        }
        
        @Override
        public void refreshServer(PlayerEntity player) {
            
            var playerHungerCapacity = 20 - player.getHungerManager().getFoodLevel();
            if (playerHungerCapacity < 2) return;
            
            var foodStackStream = player.getInventory().main.stream()
                                    .filter(item -> item.contains(DataComponentTypes.FOOD) && !item.isIn(TagContent.FEEDER_BLACKLIST));
            var selectedFood = foodStackStream
                                 .reduce((a, b) -> Math.abs(a.get(DataComponentTypes.FOOD).nutrition() - playerHungerCapacity) <= Math.abs(b.get(DataComponentTypes.FOOD).nutrition() - playerHungerCapacity) ? a : b);
            selectedFood.ifPresent(food -> food.finishUsing(player.getWorld(), player));
            
        }
        
        @Override
        public int refreshInterval() {
            return 10;
        }
    };
    
    public static final Augment magnet = new Augment(Oritech.id("augment/magnet"), true) {
        @Override
        public void activate(PlayerEntity player) {
        }
        
        @Override
        public void deactivate(PlayerEntity player) {
        }
        
        @Override
        public void refreshServer(PlayerEntity player) {
            var world = player.getWorld();
            var target = player.getEyePos();
            
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
        public int refreshInterval() {
            return 4;
        }
    };
    
    public static final Augment oreFinder = new Augment(Oritech.id("augment/orefinder"), true) {
        @Override
        public void activate(PlayerEntity player) {
        }
        
        @Override
        public void deactivate(PlayerEntity player) {
        }
        
        @Override
        public void refreshServer(PlayerEntity player) {
        
        }
        
        @Override
        public void refreshClient(PlayerEntity player) {
            var world = player.getWorld();
            var target = BlockPos.ofFloored(player.getEyePos());
            
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
        public int refreshInterval() {
            return 6;
        }
    };
    
    public static final Augment portal = new Augment(Oritech.id("augment/portal"), true) {
        
        @Override
        public void toggle(PlayerEntity player) {
            var world = player.getWorld();
            if (world.isClient) return;
            
            var hitResult = player.raycast(6, 0, false);
            var spawnPos = hitResult.getPos();
            var spawnToPlayer = spawnPos.subtract(player.getPos()).normalize().multiply(0.3);
            spawnPos = spawnPos.subtract(spawnToPlayer);
            
            var targetPos = AttachmentApi.getAttachmentValue(player, PORTAL_TARGET_TYPE);
            if (targetPos == null) return;
            
            var portalEntity = EntitiesContent.PORTAL_ENTITY.create((ServerWorld) world, spawner -> {},
              BlockPos.ofFloored(spawnPos),
              SpawnReason.EVENT,
              false,
              false);
            
            if (portalEntity != null) {
                portalEntity.setPosition(spawnPos);
                portalEntity.setYaw(-player.getYaw() + 90);
                world.spawnEntity(portalEntity);
                portalEntity.target = targetPos;
                world.playSound(null, BlockPos.ofFloored(spawnPos), SoundEvents.AMBIENT_CAVE.value(), SoundCategory.BLOCKS, 2, 1.2f);
                
            }
        }
        
        @Override
        public void activate(PlayerEntity player) {
            AttachmentApi.setAttachment(player, PORTAL_TARGET_TYPE, GlobalPos.create(
              player.getWorld().getRegistryKey(),
              player.getBlockPos()
            ));
        }
        
        @Override
        public void deactivate(PlayerEntity player) {
            AttachmentApi.removeAttachment(player, PORTAL_TARGET_TYPE);
        }
        
        @Override
        public void refreshServer(PlayerEntity player) {
        
        }
        
        @Override
        public int refreshInterval() {
            return Integer.MAX_VALUE;
        }
    };
    
    static {
        CUSTOM_AUGMENTS.put(flight.id, flight);
        CUSTOM_AUGMENTS.put(feeder.id, feeder);
        CUSTOM_AUGMENTS.put(magnet.id, magnet);
        CUSTOM_AUGMENTS.put(oreFinder.id, oreFinder);
        CUSTOM_AUGMENTS.put(portal.id, portal);
    }
    
}
