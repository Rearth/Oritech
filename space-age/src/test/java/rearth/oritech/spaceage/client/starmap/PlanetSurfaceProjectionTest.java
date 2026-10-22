package rearth.oritech.spaceage.client.starmap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import rearth.oritech.api.screen.widgets.BlockWidget;
import rearth.oritech.spaceage.client.starmap.RocketMapPaths.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetSurfaceProjectionTest {

    @Test
    void outlineAndTopBottomAttachmentsMatchTheRenderedModelCorners() {

        var widget = new BlockWidget(0, 0, 128, null).withScale(.5f);
        var outline = PlanetSurfaceProjection.outline(widget);
        assertEquals(6, outline.size());
        var pose = new PoseStack();
        pose.scale(1, -1, -1);
        pose.mulPose(Axis.XP.rotationDegrees(30));
        pose.mulPose(Axis.YP.rotationDegrees(225));
        pose.translate(-.5, -.5, -.5);
        double top = Double.POSITIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (var x : new float[]{-.5f, 1.5f})
            for (var y : new float[]{-.5f, 1.5f})
                for (var z : new float[]{-.5f, 1.5f}) {
                    var rendered = pose.last().pose().transformPosition(x, y, z, new Vector3f()).mul(.625f * .5f);
                    var projected = widget.projectModelPoint(x, y, z);
                    assertEquals(rendered.x, projected.x, 1e-6);
                    assertEquals(rendered.y, projected.y, 1e-6);
                    assertTrue(PlanetSurfaceProjection.contains(outline, new Point(projected.x, projected.y)));
                    top = Math.min(top, rendered.y);
                    bottom = Math.max(bottom, rendered.y);
                }
        var topSurface = PlanetSurfaceProjection.closestEdge(new Point(0, -100), 0, 0, 100, outline);
        var bottomSurface = PlanetSurfaceProjection.closestEdge(new Point(0, 100), 0, 0, 100, outline);
        assertEquals(top * 100, topSurface.y() * StarMapCamera.PLANE_TILT, 1e-5);
        assertEquals(bottom * 100, bottomSurface.y() * StarMapCamera.PLANE_TILT, 1e-5);
        assertFalse(PlanetSurfaceProjection.contains(outline, new Point(.4, -.45)),
                "The empty upper corner of the rectangular widget is not planet surface");
    }

    @Test
    void attachmentsRemainOnTheOutlineAtDifferentPositionsAndScales() {

        var outline = PlanetSurfaceProjection.outline(new BlockWidget(0, 0, 128, null).withScale(.5f));
        for (int angle = 0; angle < 360; angle += 5) {
            var point = new Point(100 * Math.cos(Math.toRadians(angle)), 100 * Math.sin(Math.toRadians(angle)));
            var edge = PlanetSurfaceProjection.closestEdge(point, 0, 0, 100, outline);
            var scaled = PlanetSurfaceProjection.closestEdge(new Point(point.x() * 2 + 500, point.y() * 2 - 200),
                    500, -200, 200, outline);
            assertEquals(edge.x() * 2 + 500, scaled.x(), 1e-8);
            assertEquals(edge.y() * 2 - 200, scaled.y(), 1e-8);
            var projected = new Point(edge.x() / 100, edge.y() * StarMapCamera.PLANE_TILT / 100);
            assertTrue(PlanetSurfaceProjection.contains(outline, projected));
            assertFalse(PlanetSurfaceProjection.contains(outline, new Point(projected.x() * 1.01, projected.y() * 1.01)));
        }
    }
}
