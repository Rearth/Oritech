package rearth.oritech.util;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import rearth.oritech.Oritech;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.keyframe.event.SoundKeyframeEvent;

import java.util.function.Supplier;

// basically of fabric version of this: https://github.com/bernie-g/geckolib/blob/main/common/src/main/java/software/bernie/geckolib/animation/keyframe/event/builtin/AutoPlayingSoundKeyframeHandler.java
public class AutoPlayingSoundKeyframeHandler<A extends GeoAnimatable> implements AnimationController.SoundKeyframeHandler<A> {
    
    private final Supplier<Float> speedSupplier;
    
    public AutoPlayingSoundKeyframeHandler(Supplier<Float> speedSupplier) {
        this.speedSupplier = speedSupplier;
    }
    
    public AutoPlayingSoundKeyframeHandler() {
        this.speedSupplier = AutoPlayingSoundKeyframeHandler::getDefaultSpeed;
    }
    
    private static float getDefaultSpeed() {
        return 1f;
    }
    
    @Override
    public void handle(SoundKeyframeEvent<A> event) {
        var segments = event.getKeyframeData().getSound().split("\\|");
        var sound = Registries.SOUND_EVENT.get(Identifier.of(segments[0]));
        
        if (sound != null) {
            var entity = (BlockEntity) event.getAnimatable();
            var pos = entity.getPos().toCenterPos();
            var distance = Math.sqrt(MinecraftClient.getInstance().gameRenderer.getCamera().getPos().squaredDistanceTo(pos));
            var volumeFalloff = Math.min(1f, 1f / (distance / 4f));
            if (distance > 25) return;
            var speed = speedSupplier.get();
            speed = Math.min(Math.max(speed, 0.125f), 8f);
            
            var volume = segments.length > 1 ? Float.parseFloat(segments[1]) : 1f;
            volume *= (float) (Oritech.CONFIG.machineVolumeMultiplier() * getPitchRandomMultiplier(entity.getWorld().random) * volumeFalloff * 0.5f);
            var pitch = segments.length > 2 ? Float.parseFloat(segments[2]) : 1f;
            pitch *= speed * getPitchRandomMultiplier(entity.getWorld().random);
            var source = SoundCategory.BLOCKS;
            
            MinecraftClient.getInstance().player.clientWorld.playSoundAtBlockCenter(entity.getPos(), sound, source, volume, pitch, true);
        }
    }
    
    private float getPitchRandomMultiplier(Random random) {
        return random.nextFloat() * 0.15f + 1;
    }
    
}
