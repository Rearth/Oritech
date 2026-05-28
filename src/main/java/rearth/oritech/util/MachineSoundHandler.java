package rearth.oritech.util;

import com.geckolib.GeckoLibConstants;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.keyframeevent.SoundKeyframeData;
import com.geckolib.constant.DataTickets;
import com.geckolib.util.ClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.config.OritechConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MachineSoundHandler<A extends GeoAnimatable> implements AnimationController.KeyframeEventHandler<A, SoundKeyframeData> {
    
    private final Supplier<Float> speedSupplier;
    private final Map<Identifier, Long> lastPlayedAt = new HashMap<>();
    
    public MachineSoundHandler(Supplier<Float> speedSupplier) {
        this.speedSupplier = speedSupplier;
    }
    
    public MachineSoundHandler() {
        this(() -> 1f);
    }
    
    @Override
    public void handle(KeyFrameEvent<A, SoundKeyframeData> event) {
        var level = ClientUtil.getLevel();
        
        if (level == null)
            return;
        
        var segments = event.keyframeData().getSound().split("\\|");
        var id = Identifier.read(segments[0]).getOrThrow();
        
        var time = level.getGameTime();
        var age = time - lastPlayedAt.getOrDefault(id, 0L);
        if (age < 30) return;  // don't play sounds if we just played it
        
        BuiltInRegistries.SOUND_EVENT.get(id).ifPresent(sound -> {
            
            var position = event.renderState().getOrDefaultGeckolibData(DataTickets.POSITION, event.renderState() instanceof EntityRenderState entityState ?
                                                                                                new Vec3(entityState.x, entityState.y, entityState.z) : null);
            
            var animatableClass = event.renderState().getOrDefaultGeckolibData(DataTickets.ANIMATABLE_CLASS, GeoAnimatable.class);
            
            if (position != null) {
                
                var distance = Math.sqrt(Minecraft.getInstance().gameRenderer.getMainCamera().position().distanceToSqr(position));
                var volumeFalloff = Math.min(1f, 1f / (distance / 4f));
                if (distance > 35) return;
                
                var random = level.getRandom();
                
                var speed = speedSupplier.get();
                speed = Math.min(Math.max(speed, 0.125f), 8f);
                
                var volume = segments.length > 1 ? Float.parseFloat(segments[1]) : 1f;
                volume *= (float) (OritechConfig.machineVolumeMultiplier.get() * getPitchRandomMultiplier(random));
                var pitch = segments.length > 2 ? Float.parseFloat(segments[2]) : 1f;
                pitch *= speed * getPitchRandomMultiplier(random);
                
                var source = SoundSource.BLOCKS;
                
                level.playLocalSound(position.x, position.y, position.z, sound.value(), source, volume, pitch, false);
            } else {
                GeckoLibConstants.LOGGER.warn("Found sound keyframe handler, but AnimationState had no position data for animatable: {}", animatableClass.getName());
            }
        });
    }
    
    private float getPitchRandomMultiplier(RandomSource random) {
        return random.nextFloat() * 0.35f + 1;
    }
}
