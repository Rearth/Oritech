package rearth.oritech.util;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;

import java.util.function.Supplier;

// basically of fabric version of this: https://github.com/bernie-g/geckolib/blob/main/common/src/main/java/software/bernie/geckolib/animation/keyframe/event/builtin/AutoPlayingSoundKeyframeHandler.java
public class AutoPlayingSoundKeyframeHandler<A extends GeoAnimatable> implements AnimationController.SoundKeyframeHandler<A> {
    
    private final Supplier<Float> speedSupplier;
    
    public AutoPlayingSoundKeyframeHandler(Supplier<Float> speedSupplier) {
        this.speedSupplier = speedSupplier;
    }
    
    @Override
    public void handle(SoundKeyframeEvent<A> event) {
        var segments = event.getKeyframeData().getSound().split("\\|");
        var sound = Registries.SOUND_EVENT.get(new Identifier(segments[0]));
        
        if (sound != null) {
            var entity = (BlockEntity) event.getAnimatable();
            var pos = entity.getPos().toCenterPos();
            var speed = speedSupplier.get();
            speed = Math.min(Math.max(speed, 0.25f), 4f);
            
            System.out.println("playing sound: " + sound.getId());
            
            var volume = segments.length > 1 ? Float.parseFloat(segments[1]) : 1f;  // TODO config volume multiplier
            var pitch = segments.length > 2 ? Float.parseFloat(segments[2]) : 1f;
            pitch *= speed;
            var source = SoundCategory.BLOCKS;
            
            MinecraftClient.getInstance().player.clientWorld.playSound(MinecraftClient.getInstance().player, pos.x, pos.y, pos.z, sound, source, volume, pitch);
        }
    }
}
