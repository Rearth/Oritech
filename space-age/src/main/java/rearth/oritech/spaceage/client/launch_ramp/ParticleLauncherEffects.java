package rearth.oritech.spaceage.client.launch_ramp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampBlockEntity;

/** Client-only particles and the shared muzzle-to-sky breakup timing. */
public final class ParticleLauncherEffects {

    private static final ColorParticleOption FLASH = ColorParticleOption.create(ParticleTypes.FLASH, 0xFFE4FBFF);

    private ParticleLauncherEffects() {
    }

    public static double dissolve(double distance, double age) {

        var segment = (int) (distance / 2);
        var stagger = (segment * 37 % 17) / 4d;
        return Math.clamp((age - 4 - Math.sqrt(distance / LaunchRampBlockEntity.BEAM_LENGTH) * 44 - stagger) / 8, 0, 1);
    }

    public static void launch(LaunchRampBlockEntity launcher) {

        var level = launcher.getLevel();
        if (level == null) return;
        var muzzle = Vec3.atLowerCornerOf(launcher.getBlockPos()).add(launcher.muzzleOffset());
        var direction = launcher.launchDirection();
        ParticleContent.addParticle(level, FLASH, muzzle.x, muzzle.y, muzzle.z, 0, 0, 0);
        // Reuse the railgun's staggered sonic-boom sequence along the first stretch of the shot.
        ParticleContent.LaserBoom(level, muzzle.add(direction.scale(.5)), muzzle.add(direction.scale(64)));
        var random = level.getRandom();
        for (var i = 0; i < 20; i++) {
            var angle = i * Math.PI * 2 / 20;
            var radius = .7 + random.nextDouble() * 1.3;
            var x = launcher.getBlockPos().getX() + .5 + Math.cos(angle) * radius;
            var z = launcher.getBlockPos().getZ() + .5 + Math.sin(angle) * radius;
            var ground = BlockPos.containing(x, launcher.getBlockPos().getY() - .1, z);
            if (!level.hasChunkAt(ground)) continue;
            var surface = level.getBlockState(ground);
            if (surface.isAir() || !surface.getFluidState().isEmpty()) continue;
            ParticleContent.addParticle(level, ParticleTypes.DUST_PLUME, x, launcher.getBlockPos().getY() + .05, z,
                    Math.cos(angle) * .22, .02 + random.nextDouble() * .04, Math.sin(angle) * .22);
        }
    }
}
