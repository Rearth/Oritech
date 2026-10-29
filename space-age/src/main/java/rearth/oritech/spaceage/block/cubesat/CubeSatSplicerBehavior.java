package rearth.oritech.spaceage.block.cubesat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.api.splicer.SplicerBehavior;
import rearth.oritech.api.splicer.SplicerBehaviors;
import rearth.oritech.api.splicer.SplicerContext;
import rearth.oritech.api.splicer.SplicerOperation;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.SpaceAgeConfig;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.rocket.CubeSatData;
import rearth.oritech.spaceage.simulation.rocket.RocketStructureScan;

import java.util.List;
import java.util.Optional;

/**
 * Compacts the rocket standing on the pad in front of a splicer into a cube-sat. Rockets may be at most as long on
 * each axis as the splicer's core quality, so cube-sats stay a late-game feature.
 */
public class CubeSatSplicerBehavior implements SplicerBehavior {

    public static void register() {

        SplicerBehaviors.register(OritechSpaceAge.id("cube_sat"), 10, new CubeSatSplicerBehavior());
    }

    static SplicerOperation operation(ServerLevel level, BlockPos pad, int maxSize) {

        var scan = RocketStructureScan.scan(level, pad, true);
        var data = scan.rocket() == null ? null : CubeSatData.of(scan.rocket());
        return new ShrinkOperation(scan, data, maxSize);
    }

    @Override
    public Optional<SplicerOperation> findOperation(SplicerContext context) {

        if (!context.level().getBlockState(context.targetPos()).is(SpaceAgeBlocks.ROCKET_PAD)) return Optional.empty();
        return Optional.of(operation(context.level(), context.targetPos(), (int) Math.floor(context.splicer().getCoreQuality())));
    }

    private record ShrinkOperation(RocketStructureScan.Result scan, CubeSatData data,
                                   int maxSize) implements SplicerOperation {

        @Override
        public List<Component> getTooltip() {

            if (data == null)
                return List.of(Component.translatable("tooltip.oritech_space_age.cube_sat.splicer_action"));
            var size = data.size();
            return List.of(Component.translatable("tooltip.oritech_space_age.cube_sat.splicer_action"),
                    Component.translatable("tooltip.oritech_space_age.cube_sat.size", size.getX(), size.getY(), size.getZ(), data.blockCount()));
        }

        @Override
        public ItemStack getPreviewResult() {

            return data == null ? new ItemStack(SpaceAgeBlocks.CUBE_SAT.get()) : CubeSatBlock.createStack(data);
        }

        @Override
        public long getEnergyCost() {

            return data == null ? 0 : data.blockCount() * SpaceAgeConfig.cubeSatRfPerBlock();
        }

        @Override
        public Optional<Component> getBlockingReason() {

            if (data == null) return Optional.of(Component.translatable(scan.issue().isEmpty()
                    ? "status.oritech_space_age.cube_sat_no_rocket" : scan.issue()));
            var size = data.size();
            var longest = Math.max(size.getX(), Math.max(size.getY(), size.getZ()));
            if (longest > maxSize)
                return Optional.of(Component.translatable("status.oritech_space_age.cube_sat_too_large", longest, maxSize));
            return Optional.empty();
        }

        @Override
        public List<BlockPos> getAffectedPositions() {

            return List.copyOf(scan.positions());
        }

        @Override
        public ItemStack execute(ServerLevel level) {

            RocketStructureScan.removeBlocks(level, scan);
            return CubeSatBlock.createStack(data);
        }
    }
}
