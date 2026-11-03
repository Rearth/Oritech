package rearth.oritech.spaceage.block.hologram;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Bounded connected-panel scan, followed by the largest solid rectangle within it. */
public record HoloSurface(BlockPos origin, int width, int depth) {

    public static final HoloSurface EMPTY = new HoloSurface(BlockPos.ZERO, 0, 0);
    public static final Codec<HoloSurface> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("origin").forGetter(HoloSurface::origin),
            Codec.INT.fieldOf("width").forGetter(HoloSurface::width),
            Codec.INT.fieldOf("depth").forGetter(HoloSurface::depth)
    ).apply(i, HoloSurface::new));

    public static HoloSurface scan(Level level, BlockPos controller) {

        var pending = new ArrayDeque<BlockPos>();
        for (var direction : Direction.Plane.HORIZONTAL) pending.add(controller.relative(direction));
        var visited = new HashSet<BlockPos>();
        var panels = new HashSet<BlockPos>();
        while (!pending.isEmpty() && visited.size() < 4_096) {
            var pos = pending.removeFirst();
            if (!visited.add(pos) || Math.abs(pos.getX() - controller.getX()) > 32
                    || Math.abs(pos.getZ() - controller.getZ()) > 32 || !level.hasChunkAt(pos)
                    || !level.getBlockState(pos).is(SpaceAgeBlocks.HOLO_SCREEN)) continue;
            panels.add(pos);
            for (var direction : Direction.Plane.HORIZONTAL) pending.add(pos.relative(direction));
        }
        return largestRectangle(panels);
    }

    public static HoloSurface largestRectangle(Set<BlockPos> panels) {

        if (panels.isEmpty()) return EMPTY;
        var minX = panels.stream().mapToInt(BlockPos::getX).min().orElseThrow();
        var maxX = panels.stream().mapToInt(BlockPos::getX).max().orElseThrow();
        var minZ = panels.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
        var maxZ = panels.stream().mapToInt(BlockPos::getZ).max().orElseThrow();
        var y = panels.iterator().next().getY();
        var heights = new int[maxX - minX + 1];
        var best = EMPTY;
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                var column = x - minX;
                heights[column] = panels.contains(new BlockPos(x, y, z)) ? heights[column] + 1 : 0;
                var height = heights[column];
                for (int left = column; left >= 0 && height > 0; left--) {
                    height = Math.min(height, heights[left]);
                    var width = column - left + 1;
                    if (width * height > best.width * best.depth)
                        best = new HoloSurface(new BlockPos(minX + left, y, z - height + 1), width, height);
                }
            }
        }
        return best;
    }
}
