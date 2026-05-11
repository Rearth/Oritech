package rearth.oritech.client.cablesurfer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZiplineFxHandler {
    
    private static final Map<UUID, ZiplineSoundInstance> ACTIVE_SOUNDS = new HashMap<>();
    
    public static void tick() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            ACTIVE_SOUNDS.clear();
            return;
        }
        
        for (var player : level.players()) {
            handlePlayerFX(player);
        }
        
        // cleanup sounds
        ACTIVE_SOUNDS.entrySet().removeIf(entry -> {
            var sound = entry.getValue();
            return sound.isStopped();
        });
    }
    
    private static void handlePlayerFX(Player player) {
        
        if (!ClientZiplineHandler.isZiplining(player)) {
            return;
        }
        
        // add sound instance
        if (!ACTIVE_SOUNDS.containsKey(player.getUUID())) {
            var sound = new ZiplineSoundInstance(player);
            Minecraft.getInstance().getSoundManager().play(sound);
            ACTIVE_SOUNDS.put(player.getUUID(), sound);
            
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
              SoundEvents.IRON_TRAPDOOR_OPEN, player.getSoundSource(), 0.5f, 0.5f, false);
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
              SoundEvents.CHAIN_PLACE, player.getSoundSource(), 0.2f, 1f, false);
            
        }
        
        spawnParticles(player);
    }
    
    private static void spawnParticles(Player player) {
        var random = player.level().random;
        var speed = player.getDeltaMovement().length();
        
        if (speed < 0.1) return;
        
        // scale with speed
        if (random.nextFloat() < (speed / 2.0f)) {
            
            double wireY = player.getY() + ClientZiplineHandler.HANG_OFFSET + 0.38;
            
            var vel = player.getDeltaMovement().scale(2.0);
            
            player.level().addParticle(
              ParticleTypes.ELECTRIC_SPARK,
              player.getX(), wireY, player.getZ(),
              vel.x + (random.nextFloat() - 0.5) * 0.2,
              vel.y + random.nextFloat() * 0.2,
              vel.z + (random.nextFloat() - 0.5) * 0.2
            );
            
            // scrape sound sometimes
            if (random.nextFloat() > 0.85) {
                player.level().playLocalSound(player.getX(), wireY, player.getZ(),
                  SoundEvents.CHAIN_HIT, player.getSoundSource(), 0.3f, 2.0f, false);
            }
        }
    }
    
}
