package rearth.oritech.spaceage.client.starmap;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keep pan, zoom and rebuilt-screen placement unchanged after extracting the camera.
 */
class StarMapCameraTest {

    private static StarMapCamera camera() {

        var camera = new StarMapCamera();
        camera.fit(List.of(new StarMapCamera.Point(-3_000, 0), new StarMapCamera.Point(5_000, 6_000)),
                15, 42, 590, 373);
        return camera;
    }

    @Test
    void fitCentersTheViewAndProjectionRoundTrips() {

        var camera = camera();
        var center = camera.project(1_000, 3_000, 15, 42, 590, 373);
        assertEquals(310, center.x(), 1e-9);
        assertEquals(228.5, center.y(), 1e-9);
        assertTrue(camera.zoom() > 0);
        var projected = camera.project(-1_234, 5_678, 15, 42, 590, 373);
        var restored = camera.unproject(projected.x(), projected.y(), 15, 42, 590, 373);
        assertEquals(-1_234, restored.x(), 1e-8);
        assertEquals(5_678, restored.y(), 1e-8);
    }

    @Test
    void zoomKeepsThePointUnderThePointerFixed() {

        var camera = camera();
        var originalZoom = camera.zoom();
        var before = camera.unproject(123, 234, 15, 42, 590, 373);
        camera.zoomAt(123, 234, -1, 15, 42, 590, 373);
        camera.advanceZoom();
        var after = camera.project(before.x(), before.y(), 15, 42, 590, 373);
        assertEquals(123, after.x(), 1e-8);
        assertEquals(234, after.y(), 1e-8);
        var firstStep = camera.zoom();
        assertTrue(firstStep > 0 && firstStep < originalZoom);
        camera.advanceZoom();
        assertTrue(camera.zoom() > 0 && camera.zoom() < firstStep);
    }

    @Test
    void draggingMovesTheMapByThePointerDelta() {

        var camera = camera();
        var before = camera.project(500, 700, 15, 42, 590, 373);
        camera.pan(31, -17);
        var after = camera.project(500, 700, 15, 42, 590, 373);
        assertEquals(before.x() + 31, after.x(), 1e-8);
        assertEquals(before.y() - 17, after.y(), 1e-8);
    }

    @Test
    void releasedDragContinuesWithDecayingInertia() {

        var camera = camera();
        var before = camera.project(500, 700, 15, 42, 590, 373);
        camera.beginDrag();
        camera.dragBy(10, -4);
        camera.endDrag();
        camera.advancePan();
        var firstGlide = camera.project(500, 700, 15, 42, 590, 373);
        var firstX = firstGlide.x() - before.x() - 10;
        var firstY = firstGlide.y() - before.y() + 4;
        assertTrue(firstX > 0 && firstY < 0);

        camera.advancePan();
        var secondGlide = camera.project(500, 700, 15, 42, 590, 373);
        assertTrue(secondGlide.x() > firstGlide.x() && secondGlide.x() - firstGlide.x() < firstX);
        assertTrue(secondGlide.y() < firstGlide.y() && firstGlide.y() - secondGlide.y() < -firstY);
    }

    @Test
    void rebuildingTheScreenPreservesItsCamera() {

        var previous = camera();
        previous.zoomAt(123, 234, -2, 15, 42, 590, 373);
        previous.pan(31, -17);
        var rebuilt = camera();
        rebuilt.copyFrom(previous);
        assertEquals(previous.project(500, 700, 15, 42, 590, 373),
                rebuilt.project(500, 700, 15, 42, 590, 373));
    }

    @Test
    void focusFramesLocalPointsWithoutChangingSystemZoomLimit() {

        var camera = camera();
        camera.focus(List.of(new StarMapCamera.Point(900, 2_900), new StarMapCamera.Point(1_100, 3_100)),
                15, 42, 590, 373);
        var center = camera.project(1_000, 3_000, 15, 42, 590, 373);
        assertEquals(310, center.x(), 1e-9);
        assertEquals(228.5, center.y(), 1e-9);
        var localZoom = camera.zoom();

        camera.zoomAt(310, 228.5, -100, 15, 42, 590, 373);
        for (int index = 0; index < 100; index++) camera.advanceZoom();
        assertTrue(camera.zoom() > 0 && camera.zoom() < localZoom);
    }

    @Test
    void viewChangesInvalidateProjectedGeometryButIdleInputDoesNot() {

        var camera = camera();
        var revision = camera.revision();
        camera.pan(0, 0);
        camera.zoomAt(123, 234, 0, 15, 42, 590, 373);
        assertEquals(revision, camera.revision());
        camera.pan(1, 0);
        assertEquals(++revision, camera.revision());
        camera.zoomAt(123, 234, 1, 15, 42, 590, 373);
        assertEquals(++revision, camera.revision());
        camera.copyFrom(camera());
        assertEquals(++revision, camera.revision());
        camera.fit(List.of(), 15, 42, 590, 373);
        assertEquals(++revision, camera.revision());
    }
}
