package rearth.oritech.block.entity.augmenter.api;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
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

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class CustomAugmentsCollection {
    
    // for other modders: If you want to use a custom augment from a recipe, you need to add it to this map before world load!
    public static final Map<ResourceLocation, Augment> CUSTOM_AUGMENTS = new HashMap<>();
    
    public static final Attachment<GlobalPos> PORTAL_TARGET_TYPE = new Attachment<>() {
        @Override
        public ResourceLocation identifier() {
            return Oritech.id("portal_target");
        }
        
        @Override
        public Codec<GlobalPos> persistenceCodec() {
            return GlobalPos.CODEC;
        }
        
        @Override
        public StreamCodec<ByteBuf, GlobalPos> networkCodec() {
            return GlobalPos.STREAM_CODEC;
        }
        
        @Override
        public Supplier<GlobalPos> initializer() {
            return () -> GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        }
    };
    
    public static Augment getById(ResourceLocation id) {
        return CUSTOM_AUGMENTS.get(id);
    }
    
    public static final Augment flight = new Augment(Oritech.id("augment/flight"), true) {
        @Override
        public void activate(Player player) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        
        @Override
        public void deactivate(Player player) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        
        @Override
        public void refreshServer(Player player) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        
        @Override
        public int refreshInterval() {
            return 80;
        }
    };
    
    public static final Augment feeder = new Augment(Oritech.id("augment/autofeeder"), true) {
        @Override
        public void activate(Player player) {
        }
        
        @Override
        public void deactivate(Player player) {
        }
        
        @Override
        public void refreshServer(Player player) {
            
            var playerHungerCapacity = 20 - player.getFoodData().getFoodLevel();
            if (playerHungerCapacity < 2) return;
            
            var foodStackStream = player.getInventory().items.stream()
                                    .filter(item -> item.has(DataComponents.FOOD) && !item.is(TagContent.FEEDER_BLACKLIST));
            var selectedFood = foodStackStream
                                 .reduce((a, b) -> Math.abs(a.get(DataComponents.FOOD).nutrition() - playerHungerCapacity) <= Math.abs(b.get(DataComponents.FOOD).nutrition() - playerHungerCapacity) ? a : b);
            selectedFood.ifPresent(food -> food.finishUsingItem(player.level(), player));
            
        }
        
        @Override
        public int refreshInterval() {
            return 10;
        }
    };
    
    public static final Augment magnet = new Augment(Oritech.id("augment/magnet"), true) {
        @Override
        public void activate(Player player) {
        }
        
        @Override
        public void deactivate(Player player) {
        }
        
        @Override
        public void refreshServer(Player player) {
            var world = player.level();
            var target = player.getEyePosition();
            
            var range = 8;
            var speed = 0.3;
            
            var box = new AABB(target.x - range, target.y - range, target.z - range, target.x + range, target.y + range, target.z + range);
            var items = world.getEntitiesOfClass(ItemEntity.class, box, itemEntity -> !itemEntity.hasPickUpDelay());
            
            for (var item : items) {
                var direction = target.subtract(item.position()).normalize().scale(speed);
                item.push(direction);
            }
        }
        
        @Override
        public int refreshInterval() {
            return 4;
        }
    };
    
    public static final Augment oreFinder = new Augment(Oritech.id("augment/orefinder"), true) {
        @Override
        public void activate(Player player) {
        }
        
        @Override
        public void deactivate(Player player) {
        }
        
        @Override
        public void refreshServer(Player player) {
        
        }
        
        @Override
        public void refreshClient(Player player) {
            var world = player.level();
            var target = BlockPos.containing(player.getEyePosition());
            
            var range = 16;
            
            var highlightPositions = new ArrayList<BlockPos>();
            BlockPos.betweenClosed(target.getX() - range, target.getY() - range, target.getZ() - range, target.getX() + range, target.getY() + range, target.getZ() + range)
              .forEach(pos -> {
                  var state = world.getBlockState(pos);
                  var isOre = state.is(TagContent.CONVENTIONAL_ORES);
                  if (isOre) highlightPositions.add(pos.immutable());
              });
            
            if (!highlightPositions.isEmpty()) {
                OreFinderRenderer.receivedAt = player.level().getGameTime();
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
        public void toggle(Player player) {
            var world = player.level();
            if (world.isClientSide) return;
            
            var hitResult = player.pick(6, 0, false);
            var spawnPos = hitResult.getLocation();
            var spawnToPlayer = spawnPos.subtract(player.position()).normalize().scale(0.3);
            spawnPos = spawnPos.subtract(spawnToPlayer);
            
            var targetPos = AttachmentApi.getAttachmentValue(player, PORTAL_TARGET_TYPE);
            if (targetPos == null) return;
            
            var portalEntity = EntitiesContent.PORTAL_ENTITY.create((ServerLevel) world, spawner -> {},
              BlockPos.containing(spawnPos),
              MobSpawnType.EVENT,
              false,
              false);
            
            if (portalEntity != null) {
                portalEntity.setPos(spawnPos);
                portalEntity.setYRot(-player.getYRot() + 90);
                world.addFreshEntity(portalEntity);
                portalEntity.target = targetPos;
                world.playSound(null, BlockPos.containing(spawnPos), SoundEvents.AMBIENT_CAVE.value(), SoundSource.BLOCKS, 2, 1.2f);
                
            }
        }
        
        @Override
        public void activate(Player player) {
            AttachmentApi.setAttachment(player, PORTAL_TARGET_TYPE, GlobalPos.of(
              player.level().dimension(),
              player.blockPosition()
            ));
        }
        
        @Override
        public void deactivate(Player player) {
            AttachmentApi.removeAttachment(player, PORTAL_TARGET_TYPE);
        }
        
        @Override
        public void refreshServer(Player player) {
        
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
