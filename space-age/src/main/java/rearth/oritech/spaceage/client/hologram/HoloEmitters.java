package rearth.oritech.spaceage.client.hologram;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

/** Faint emitter rays with a soft pulse travelling from the table to each object. */
final class HoloEmitters {

    private HoloEmitters() {
    }

    static void render(Pose pose, VertexConsumer consumer, Beam beam, double seconds) {

        var top = Math.max(.08, beam.position.y - beam.radius);
        var radius = Math.clamp(beam.radius * .65, .025, .12);
        var phase = (seconds * .65 + Math.floorMod(beam.seed, 1000) / 1000d) % 1;
        for (int side = 0; side < 3; side++) {
            var angle = side * Math.PI * 2 / 3;
            var direction = new Vec3(Math.cos(angle) * (radius - .012), top - .015,
                    Math.sin(angle) * (radius - .012)).normalize();
            // Subdivide only to give the travelling highlight soft edges.
            for (int row = 0; row < 16; row++) {
                var low = row / 16d;
                var high = (row + 1) / 16d;
                vertex(pose, consumer, beam, top, radius, low, angle, direction, alpha(low, phase, beam.alpha));
                vertex(pose, consumer, beam, top, radius, high, angle, direction, alpha(high, phase, beam.alpha));
            }
        }
    }

    private static int alpha(double height, double phase, double opacity) {

        var distance = Math.abs(height - phase);
        var pulse = Math.max(0, 1 - distance / .17);
        var fade = Math.min(1, Math.min(height, 1 - height) * 12);
        return (int) ((9 + 65 * pulse * pulse) * fade * opacity);
    }

    private static void vertex(Pose pose, VertexConsumer consumer, Beam beam, double top, double radius,
                               double height, double angle, Vec3 direction, int alpha) {

        var width = .012 + (radius - .012) * height;
        consumer.addVertex(pose, (float) (beam.position.x + Math.cos(angle) * width),
                        (float) (.015 + (top - .015) * height), (float) (beam.position.z + Math.sin(angle) * width))
                .setColor((alpha << 24) | 0x65DFFF)
                .setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z).setLineWidth(1.25f);
    }

    record Beam(Vec3 position, double radius, double alpha, int seed) {
    }
}
