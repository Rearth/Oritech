package rearth.oritech.spaceage.block.launch_ramp;

import rearth.oritech.api.accelerator.ParticleImpactBehavior;
import rearth.oritech.api.accelerator.ParticleImpactBehaviors;
import rearth.oritech.api.accelerator.ParticleImpactContext;
import rearth.oritech.api.accelerator.ParticleImpactResult;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.cubesat.CubeSatBlock;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.Optional;

/**
 * Launch ramps stop every particle hitting them. Cube-sats are launched with the remaining speed, other items are lost.
 */
public class LaunchRampImpactBehavior implements ParticleImpactBehavior {

    public static void register() {

        ParticleImpactBehaviors.register(OritechSpaceAge.id("launch_ramp"), 10, new LaunchRampImpactBehavior());
    }

    @Override
    public Optional<ParticleImpactResult> onImpact(ParticleImpactContext context) {

        if (!context.hitState().is(SpaceAgeBlocks.LAUNCH_RAMP)) return Optional.empty();
        var cube = CubeSatBlock.data(context.particleItem());
        if (context.level().getBlockEntity(context.hitPos()) instanceof LaunchRampBlockEntity ramp) {
            if (cube != null) ramp.launch(cube, context.speed(), context.direction());
            else ramp.absorb(context.speed());
        }
        return Optional.of(ParticleImpactResult.absorbed(context, true));
    }
}
