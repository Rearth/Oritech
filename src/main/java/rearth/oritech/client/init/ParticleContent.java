package rearth.oritech.client.init;

import io.wispforest.owo.particles.ClientParticles;
import io.wispforest.owo.particles.systems.ParticleSystem;
import io.wispforest.owo.particles.systems.ParticleSystemController;
import io.wispforest.owo.util.VectorRandomUtils;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import rearth.oritech.Oritech;

public class ParticleContent {
    
    public static final ParticleSystemController PARTICLE_CONTROLLER = new ParticleSystemController(new Identifier(Oritech.MOD_ID, "particles"));
    public static final ParticleSystem<Void> HIGHLIGHT_BLOCK = PARTICLE_CONTROLLER.register(Void.class, (world, pos, data) -> {
        spawnCubeOutline(ParticleTypes.ELECTRIC_SPARK, pos, 1, 120, 6);
        ClientParticles.reset();
    });
    
    private static void spawnCubeOutline(ParticleEffect particle, Vec3d origin, float size, int duration, int segments) {
        
        spawnLineInner(particle, origin, origin.add(size, 0, 0), segments, duration);
        spawnLineInner(particle, origin.add(size, 0, 0), origin.add(size, 0, size), segments, duration);
        
        spawnLineInner(particle, origin, origin.add(0, 0, size), segments, duration);
        spawnLineInner(particle, origin.add(0, 0, size), origin.add(size, 0, size), segments, duration);
        
        origin = origin.add(0, size, 0);
        
        spawnLineInner(particle, origin, origin.add(size, 0, 0), segments, duration);
        spawnLineInner(particle, origin.add(size, 0, 0), origin.add(size, 0, size), segments, duration);
        
        spawnLineInner(particle, origin, origin.add(0, 0, size), segments, duration);
        spawnLineInner(particle, origin.add(0, 0, size), origin.add(size, 0, size), segments, duration);
        
        spawnLineInner(particle, origin, origin.add(0, -size, 0), segments, duration);
        spawnLineInner(particle, origin.add(size, 0, 0), origin.add(size, -size, 0), segments, duration);
        spawnLineInner(particle, origin.add(0, 0, size), origin.add(0, -size, size), segments, duration);
        spawnLineInner(particle, origin.add(size, 0, size), origin.add(size, -size, size), segments, duration);
    }
    
    private static void spawnLineInner(ParticleEffect particle, Vec3d start, Vec3d end, float particleCount, int duration) {
        Vec3d increment = end.subtract(start).multiply(1f / (float) particleCount);
        
        for (int i = 0; i < particleCount; i++) {
            ClientParticles.spawnWithMaxAge(particle, start, duration);
            start = start.add(increment);
        }
    }
    
    public static void registerParticles() {
        Oritech.LOGGER.info("Registering Oritech particles");
    }
    
}
