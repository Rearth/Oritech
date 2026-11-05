package rearth.oritech.client.ui.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.client.renderers.util.RenderHelpers;

/** A pair of emissive crossed sprites, usable in world and block-preview coordinates. */
public record PreviewEffect(Identifier texture, Vec3 origin, Vec3 direction, float width, float length, int frame, int frames) {

    public void submit(PoseStack stack, SubmitNodeCollector collector) {

        collector.submitCustomGeometry(stack, RenderTypes.eyes(texture), (pose, consumer) -> {
            var axis = direction.normalize();
            var right = axis.cross(Math.abs(axis.y) > .9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
            face(pose, consumer, right.scale(width / 2), axis);
            face(pose, consumer, axis.cross(right).scale(width / 2), axis);
        });
    }

    private void face(PoseStack.Pose pose, VertexConsumer consumer, Vec3 right, Vec3 axis) {

        var end = origin.add(axis.scale(length));
        var top = frame / (float) frames;
        var bottom = (frame + 1f) / frames;
        vertex(pose, consumer, origin.subtract(right), 0, top);
        vertex(pose, consumer, end.subtract(right), 0, bottom);
        vertex(pose, consumer, end.add(right), 1, bottom);
        vertex(pose, consumer, origin.add(right), 1, top);
        // The eyes pass is unlit but culls back faces; emit the reverse winding too.
        vertex(pose, consumer, origin.add(right), 1, top);
        vertex(pose, consumer, end.add(right), 1, bottom);
        vertex(pose, consumer, end.subtract(right), 0, bottom);
        vertex(pose, consumer, origin.subtract(right), 0, top);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 point, float u, float v) {

        consumer.addVertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(RenderHelpers.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }
}
