package rearth.oritech.client.init;

import io.wispforest.owo.particles.ClientParticles;
import io.wispforest.owo.particles.systems.ParticleSystem;
import io.wispforest.owo.particles.systems.ParticleSystemController;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import rearth.oritech.Oritech;

public class ParticleContent {
    
    public static final ParticleSystemController PARTICLE_CONTROLLER = new ParticleSystemController(Oritech.id("particles"));
    
    public static final ParticleSystem<Void> HIGHLIGHT_BLOCK = PARTICLE_CONTROLLER.register(Void.class, (world, pos, data) -> {
        spawnCubeOutline(ParticleTypes.ELECTRIC_SPARK, pos, 1, 120, 6);
        ClientParticles.reset();
    });
    
    public static final ParticleSystem<LineData> WEED_KILLER = PARTICLE_CONTROLLER.register(LineData.class, (world, pos, data) -> {
        var dist = (int) data.end.distanceTo(data.start);
        ClientParticles.setParticleCount(dist * 4 + world.random.nextInt(3));
        ClientParticles.spawnLine(ParticleTypes.DRAGON_BREATH, world, data.start, data.end, 0.2f);
    });
    
    public static final ParticleSystem<Void> DEBUG_BLOCK = PARTICLE_CONTROLLER.register(Void.class, (world, pos, data) -> {
        spawnCubeOutline(ParticleTypes.ELECTRIC_SPARK, pos, 1, 120, 2);
        ClientParticles.reset();
    });
    
    public static final ParticleSystem<SoulParticleData> WANDERING_SOUL = PARTICLE_CONTROLLER.register(SoulParticleData.class, (world, pos, data) -> {
        ClientParticles.setVelocity(data.offset.multiply((1f / data.duration) * 1.5f));
        ClientParticles.spawnWithMaxAge(ParticleTypes.SCULK_SOUL, pos, data.duration);
    });
    
    public static final ParticleSystem<LineData> CATALYST_CONNECTION = PARTICLE_CONTROLLER.register(LineData.class, (world, pos, data) -> {
        ClientParticles.spawnEnchantParticles(world, data.start, data.end, 0.7f);
    });
    
    public static final ParticleSystem<Integer> FERTILIZER_EFFECT = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawnWithinBlock(ParticleTypes.HAPPY_VILLAGER, world, new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
    }));
    
    public static final ParticleSystem<Integer> BLOCK_DESTROY_EFFECT = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawnCenteredOnBlock(ParticleTypes.SOUL_FIRE_FLAME, world, new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), 0.6);
    }));
    
    public static final ParticleSystem<Integer> QUARRY_DESTROY_EFFECT = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawn(ParticleTypes.SOUL_FIRE_FLAME, world, pos, 0.4);
    }));
    
    public static final ParticleSystem<Integer> WATERING_EFFECT = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawnCenteredOnBlock(ParticleTypes.FALLING_WATER, world, new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), 0.6);
    }));
    
    public static final ParticleSystem<Integer> FURNACE_BURNING = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawnCenteredOnBlock(ParticleTypes.LAVA, world, new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), 0.6);
    }));
    
    public static final ParticleSystem<Integer> PULVERIZER_WORKING = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawnCenteredOnBlock(ParticleTypes.DUST_PLUME, world, new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), 0.6);
    }));
    
    public static final ParticleSystem<Integer> SOUL_USED = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawn(ParticleTypes.HAPPY_VILLAGER, world, pos, 1.2);
    }));
    
    public static final ParticleSystem<Integer> MELTDOWN_IMMINENT = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawn(ParticleTypes.LAVA, world, pos, 1);
    }));
    
    public static final ParticleSystem<Integer> GRINDER_WORKING = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawn(ParticleTypes.DUST_PLUME, world, pos, 0.8);
    }));
    
    public static final ParticleSystem<Integer> ASSEMBLER_WORKING = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawn(ParticleTypes.ENCHANTED_HIT, world, pos, 0.6);
    }));
    
    public static final ParticleSystem<Integer> STEAM_ENGINE_WORKING = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawn(ParticleTypes.CLOUD, world, pos, 0.6);
    }));
    
    public static final ParticleSystem<Integer> CHARGING = PARTICLE_CONTROLLER.register(Integer.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(data);
        ClientParticles.spawnCenteredOnBlock(ParticleTypes.SONIC_BOOM, world, new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), 0.6);
    }));
    
    public static final ParticleSystem<Void> BIG_HIT = PARTICLE_CONTROLLER.register(Void.class, ((world, pos, data) -> {
        ClientParticles.spawn(ParticleTypes.SONIC_BOOM, world, pos, 0.3);
    }));
    
    public static final ParticleSystem<Void> PARTICLE_COLLIDE = PARTICLE_CONTROLLER.register(Void.class, ((world, pos, data) -> {
        ClientParticles.spawn(ParticleTypes.GUST, world, pos, 0);
    }));
    
    public static final ParticleSystem<Vec3d> JETPACK_EXHAUST = PARTICLE_CONTROLLER.register(Vec3d.class, ((world, pos, data) -> {
        ClientParticles.setVelocity(data);
        ClientParticles.spawn(ParticleTypes.SMOKE, world, pos, 0.1);
    }));
    
    public static final ParticleSystem<Void> ACCELERATING = PARTICLE_CONTROLLER.register(Void.class, (world, pos, data) -> {
        spawnCubeOutline(ParticleTypes.SCULK_CHARGE_POP, pos, 1, 5, 3);
        ClientParticles.reset();
    });
    
    public static final ParticleSystem<Void> LASER_BEAM_EFFECT = PARTICLE_CONTROLLER.register(Void.class, ((world, pos, data) -> {
        ClientParticles.setParticleCount(1);
        ClientParticles.spawnPrecise(ParticleTypes.SMALL_FLAME, world, pos, 0.4, 0.3, 0.4);
    }));
    
    public static final ParticleSystem<Void> PARTICLE_MOVING = PARTICLE_CONTROLLER.register(Void.class, ((world, pos, data) -> {
        ClientParticles.spawnPrecise(ParticleTypes.REVERSE_PORTAL, world, pos, 0.2, 0.3, 0.2);
    }));
    
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
        Vec3d increment = end.subtract(start).multiply(1f / particleCount);
        
        for (int i = 0; i < particleCount; i++) {
            ClientParticles.spawnWithMaxAge(particle, start, duration);
            start = start.add(increment);
        }
    }
    
    public static void registerParticles() {
        Oritech.LOGGER.debug("Registering Oritech particles");
    }
    
    public record LineData(Vec3d start, Vec3d end) {}
    
    public record SoulParticleData(Vec3d offset, int duration) {}
    
}
