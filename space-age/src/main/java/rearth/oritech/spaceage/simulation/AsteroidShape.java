package rearth.oritech.spaceage.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A few thousand columns describe even a large asteroid; no voxel list or spherical scan is needed.
 */
public final class AsteroidShape {

    private AsteroidShape() {
    }

    public static List<Column> columns(int volume, long seed) {

        return columns(volume, seed, Integer.MAX_VALUE);
    }

    public static List<Column> columns(int volume, long seed, int heightLimit) {

        return columns(volume, seed, Math.max(1, heightLimit), 1);
    }

    private static List<Column> columns(int volume, long seed, int heightLimit, double footprintScale) {

        if (volume <= 0) return List.of();
        var radius = Math.max(1, Math.cbrt(volume * 3 / (4 * Math.PI)));
        var rx = radius * 1.4 * footprintScale;
        var rz = radius * .85 * footprintScale;
        var mixed = (seed ^ seed >>> 30) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94d049bb133111ebL;
        var phase = ((mixed ^ mixed >>> 31) & 65535) / 65535.0 * Math.PI * 2;
        var weights = new ArrayList<Weight>();
        double sum = 0;
        for (var z = -(int) rz; z <= (int) rz; z++) {
            for (var x = -(int) rx; x <= (int) rx; x++) {
                var nx = x / rx;
                var nz = z / rz;
                var edge = 1 - nx * nx - nz * nz;
                if (edge <= 0) continue;
                var broad = 1 + .22 * Math.sin(nx * 5 + phase) * Math.cos(nz * 4 - phase)
                        + .12 * Math.cos(nx * 9 + nz * 5 + phase);
                var weight = Math.sqrt(edge) * broad;
                weights.add(new Weight(x, z, weight));
                sum += weight;
            }
        }
        // Largest remainders preserve the exact block budget without flattening one end of the shape.
        var heights = new int[weights.size()];
        var order = new ArrayList<Integer>();
        var remainders = new double[weights.size()];
        var assigned = 0;
        for (var i = 0; i < weights.size(); i++) {
            var exact = volume * weights.get(i).value / sum;
            heights[i] = (int) exact;
            assigned += heights[i];
            remainders[i] = exact - heights[i];
            order.add(i);
        }
        order.sort(Comparator.<Integer>comparingDouble(i -> remainders[i]).reversed());
        for (var i = 0; i < volume - assigned; i++) heights[order.get(i)]++;
        var tallest = 0;
        for (var height : heights) tallest = Math.max(tallest, height);
        // High-altitude deliveries spread sideways instead of losing blocks at the build ceiling.
        if (tallest > heightLimit)
            return columns(volume, seed, heightLimit, footprintScale * Math.sqrt((double) tallest / heightLimit) * 1.05);
        var result = new ArrayList<Column>();
        var bottom = Integer.MAX_VALUE;
        for (var i = 0; i < weights.size(); i++) {
            if (heights[i] == 0) continue;
            var weight = weights.get(i);
            var bend = (int) Math.round(Math.min(radius, heightLimit / 4.0) * .12 * Math.sin(weight.x / rx * 4 + phase));
            var y = bend - heights[i] / 2;
            result.add(new Column(weight.x, weight.z, y, heights[i]));
            bottom = Math.min(bottom, y);
        }
        var offset = bottom;
        var normalized = result.stream().map(column -> new Column(column.x, column.z, column.bottom - offset, column.height)).toList();
        var top = normalized.stream().mapToInt(column -> column.bottom + column.height).max().orElse(0);
        if (top > heightLimit)
            return columns(volume, seed, heightLimit, footprintScale * Math.sqrt((double) top / heightLimit) * 1.05);
        return normalized;
    }

    public record Column(int x, int z, int bottom, int height) {
    }

    private record Weight(int x, int z, double value) {
    }
}
