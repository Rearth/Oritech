package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.SpaceAgeConfig;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.simulation.StaticRocketSegment.BlockData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** One bounded construction scan, shared by physical receivers and spacecraft. */
public final class BeamAperture {

    public static final TagKey<Block> FRAMES = TagKey.create(Registries.BLOCK, OritechSpaceAge.id("beam_aperture_frames"));
    public static final double RF_PER_GLASS = 10_000;
    private static final double METRES_PER_FOCUS = 100_000;
    private static final double ATMOSPHERIC_SPREAD = 10;

    private BeamAperture() {
    }

    public static Array scan(BlockPos centre, Direction facing, Function<BlockPos, BlockState> blocks) {

        return scan(centre, facing, blocks, state -> state.is(FRAMES), state -> {
            var quality = state.getBlock().builtInRegistryHolder().getData(SpaceAgeDataMaps.BEAM_APERTURE_MATERIALS);
            return quality == null ? 0 : quality;
        });
    }

    static Array scan(BlockPos centre, Direction facing, Function<BlockPos, BlockState> blocks,
                      Predicate<BlockState> frame, ToDoubleFunction<BlockState> opticalQuality) {

        var horizontal = facing.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        var vertical = facing.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;
        var width = arm(centre, horizontal, blocks, frame);
        var height = arm(centre, vertical, blocks, frame);
        var oppositeWidth = arm(centre, horizontal.getOpposite(), blocks, frame);
        var oppositeHeight = arm(centre, vertical.getOpposite(), blocks, frame);
        var limit = SpaceAgeConfig.apertureArmLimit();
        BlockPos defect = null;
        if (width == 0 || width < oppositeWidth) defect = centre.relative(horizontal, width + 1);
        else if (oppositeWidth == 0 || oppositeWidth < width) defect = centre.relative(horizontal.getOpposite(), oppositeWidth + 1);
        else if (height == 0 || height < oppositeHeight) defect = centre.relative(vertical, height + 1);
        else if (oppositeHeight == 0 || oppositeHeight < height) defect = centre.relative(vertical.getOpposite(), oppositeHeight + 1);
        else if (width > limit) defect = centre.relative(horizontal, limit + 1);
        else if (height > limit) defect = centre.relative(vertical, limit + 1);
        if (defect != null) return new Array(0, 0, 0, 0, Set.of(), defect);
        var cells = new HashSet<BlockPos>();
        var quality = 0d;
        for (int x = -width; x <= width; x++) {
            for (int y = -height; y <= height; y++) {
                var pos = centre.relative(horizontal, x).relative(vertical, y);
                cells.add(pos);
                if (x == 0 || y == 0) continue;
                var value = opticalQuality.applyAsDouble(blocks.apply(pos));
                if (!Double.isFinite(value) || value <= 0) return new Array(0, 0, 0, 0, Set.of(), pos);
                quality += value;
            }
        }
        return new Array(width * 2 + 1, height * 2 + 1, quality, (width * 2 + 1) * (height * 2 + 1), Set.copyOf(cells), null);
    }

    private static int arm(BlockPos centre, Direction direction, Function<BlockPos, BlockState> blocks, Predicate<BlockState> frame) {

        var length = 0;
        while (length <= SpaceAgeConfig.apertureArmLimit() && frame.test(blocks.apply(centre.relative(direction, length + 1)))) length++;
        return length;
    }

    /** Includes partial construction so the receiver screen can explain an incomplete array. */
    public static List<BlockData> preview(BlockPos centre, Direction facing, Function<BlockPos, BlockState> blocks) {

        var horizontal = facing.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        var vertical = facing.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;
        Predicate<BlockState> frame = state -> state.is(FRAMES);
        var width = Math.max(1, Math.max(arm(centre, horizontal, blocks, frame), arm(centre, horizontal.getOpposite(), blocks, frame)));
        var height = Math.max(1, Math.max(arm(centre, vertical, blocks, frame), arm(centre, vertical.getOpposite(), blocks, frame)));
        var result = new ArrayList<BlockData>();
        for (int x = -width; x <= width; x++) {
            for (int y = -height; y <= height; y++) {
                var pos = centre.relative(horizontal, x).relative(vertical, y);
                var state = blocks.apply(pos);
                if (!state.isAir()) result.add(new BlockData(pos.subtract(centre), state));
            }
        }
        return List.copyOf(result);
    }

    public static double availablePower(double stored, double capacity, double throughput, int reservePercent) {

        return Math.min(throughput, Math.max(0, stored - capacity * reservePercent / 100d));
    }

    /** Fixed reference collecting area, never dependent on the chosen receiver's installed hardware. */
    public static double idealEfficiency(double distance, double senderWidth, boolean surface) {

        var arm = SpaceAgeConfig.apertureArmLimit();
        return collectionFraction(Math.pow(arm * 2 + 1, 2), footprint(distance, senderWidth, surface));
    }

    /** Both craft and surface receivers collect their share of a uniform circular beam. */
    public static double collectionFraction(double collectingArea, double radius) {

        var fraction = Math.max(0, collectingArea) / (Math.PI * radius * radius);
        // Exact coverage must not lose a whole RF to floating-point error at surface insertion.
        return fraction >= 1 - 1e-9 ? 1 : fraction;
    }

    /** Area of one block's square face inside the circular beam, including partial edge cells. */
    public static double cellArea(double x, double z, double radius) {

        if (Math.hypot(Math.abs(x) + .5, Math.abs(z) + .5) <= radius) return 1;
        if (Math.hypot(Math.max(0, Math.abs(x) - .5), Math.max(0, Math.abs(z) - .5)) >= radius) return 0;
        return Math.clamp(quadrantArea(x + .5, z + .5, radius) - quadrantArea(x - .5, z + .5, radius)
                - quadrantArea(x + .5, z - .5, radius) + quadrantArea(x - .5, z - .5, radius), 0, 1);
    }

    /** Signed area from the circle centre to (x,z), used for rectangle inclusion/exclusion. */
    private static double quadrantArea(double x, double z, double radius) {

        var width = Math.min(radius, Math.abs(x));
        var height = Math.min(radius, Math.abs(z));
        var straight = Math.min(width, Math.sqrt(Math.max(0, radius * radius - height * height)));
        var area = straight * height + circleIntegral(width, radius) - circleIntegral(straight, radius);
        return Math.signum(x) * Math.signum(z) * area;
    }

    private static double circleIntegral(double x, double radius) {

        return .5 * (x * Math.sqrt(Math.max(0, radius * radius - x * x)) + radius * radius * Math.asin(x / radius));
    }

    public static double footprint(double distance, double width, boolean surface) {

        var spread = Math.max(0, distance) / (METRES_PER_FOCUS * Math.max(1, width));
        // Atmosphere magnifies accumulated spread; a nearby relay starts a freshly focused final leg.
        return Math.max(surface ? 4 : 1, spread * (surface ? ATMOSPHERIC_SPREAD : 1));
    }

    public record Array(int width, int height, double quality, int area, Set<BlockPos> cells, BlockPos defect) {

        public boolean valid() {

            return defect == null;
        }

        public double throughput() {

            return quality * RF_PER_GLASS;
        }

        public int focus() {

            return Math.min(width, height);
        }
    }
}
