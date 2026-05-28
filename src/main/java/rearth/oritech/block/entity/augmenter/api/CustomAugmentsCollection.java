package rearth.oritech.block.entity.augmenter.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import rearth.oritech.Oritech;
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.init.AttachmentContent;
import rearth.oritech.init.EntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.PortalEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CustomAugmentsCollection {
    
    // for other modders: If you want to use a custom augment from a recipe, you need to add it to this map before level load!
    public static final Map<Identifier, Augment> CUSTOM_AUGMENTS = new HashMap<>();
    
    public static Augment getById(Identifier id) {
        return CUSTOM_AUGMENTS.get(id);
    }
    
    // todo add fly as normal modifier attribute (see net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT)
    
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
            
            var foodStackStream = player.getInventory().getNonEquipmentItems().stream()
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
            var level = player.level();
            var target = player.getEyePosition();
            
            var range = 8;
            var speed = 0.3;
            
            var box = new AABB(target.x - range, target.y - range, target.z - range, target.x + range, target.y + range, target.z + range);
            var items = level.getEntitiesOfClass(ItemEntity.class, box, itemEntity -> !itemEntity.hasPickUpDelay());
            
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
            var level = player.level();
            var target = BlockPos.containing(player.getEyePosition());
            
            var range = 16;
            
            var highlightPositions = new ArrayList<BlockPos>();
            BlockPos.betweenClosed(target.getX() - range, target.getY() - range, target.getZ() - range, target.getX() + range, target.getY() + range, target.getZ() + range)
              .forEach(pos -> {
                  var state = level.getBlockState(pos);
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
            var level = player.level();
            if (level.isClientSide()) return;
            
            var hitResult = player.pick(6, 0, false);
            var spawnPos = hitResult.getLocation();
            var spawnToPlayer = spawnPos.subtract(player.position()).normalize().scale(0.3);
            spawnPos = spawnPos.subtract(spawnToPlayer);
            
            var targetPos = player.getData(AttachmentContent.PORTAL_TARGET);
            if (targetPos.equals(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO))) return;
            
            var spawnedEntity = EntitiesContent.PORTAL_ENTITY.get().create((ServerLevel) level, spawner -> {
              },
              BlockPos.containing(spawnPos),
              EntitySpawnReason.MOB_SUMMONED,
              false,
              false);
            
            if (spawnedEntity instanceof PortalEntity portalEntity) {
                spawnedEntity.setPos(spawnPos);
                spawnedEntity.setYRot(-player.getYRot() + 90);
                level.addFreshEntity(spawnedEntity);
                portalEntity.target = targetPos;
                level.playSound(null, BlockPos.containing(spawnPos), SoundEvents.AMBIENT_CAVE.value(), SoundSource.BLOCKS, 2, 1.2f);
            }
        }
        
        @Override
        public void activate(Player player) {
            player.setData(AttachmentContent.PORTAL_TARGET, GlobalPos.of(
              player.level().dimension(),
              player.blockPosition()
            ));
        }
        
        @Override
        public void deactivate(Player player) {
            player.removeData(AttachmentContent.PORTAL_TARGET);
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
        CUSTOM_AUGMENTS.put(feeder.id, feeder);
        CUSTOM_AUGMENTS.put(magnet.id, magnet);
        CUSTOM_AUGMENTS.put(oreFinder.id, oreFinder);
        CUSTOM_AUGMENTS.put(portal.id, portal);
    }
    
}
