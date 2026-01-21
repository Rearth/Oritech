package rearth.oritech.client.cablesurfer;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class ZiplineSoundInstance extends AbstractTickableSoundInstance {
    
    private final Player player;
    
    public ZiplineSoundInstance(Player player) {
        super(SoundEvents.MINECART_RIDING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.1F;
        this.pitch = 0.5F;
        this.relative = false;  // basically means position is always at player
    }
    
    @Override
    public void tick() {
        // Stop if zipline stopped or player dead
        if (!ClientZiplineHandler.isZiplining(player) || player.isRemoved()) {
            this.stop();
            return;
        }
        
        this.x = player.getX();
        this.y = player.getY() + 2;
        this.z = player.getZ();
        
        
        float speed = (float) player.getDeltaMovement().length();
        
        float targetPitch = (float) Mth.clamp(speed / 1.5f, 0.5, 1.8);
        float targetVolume = (float) Mth.clamp(speed * 0.6, 0.0, 0.8);
        
        this.pitch = Mth.lerp(0.35F, this.pitch, targetPitch);
        this.volume = Mth.lerp(0.35F, this.volume, targetVolume);
        
    }
}