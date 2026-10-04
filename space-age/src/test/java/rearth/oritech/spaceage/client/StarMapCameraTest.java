package rearth.oritech.spaceage.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Keep pan, zoom and rebuilt-screen placement unchanged after extracting the camera. */
class StarMapCameraTest {

    @Test
    void projectionAndFitMatchTheOriginalViewport() {
        var camera = camera();
        var center = camera.project(1_000, 3_000, 15, 42, 590, 373);
        assertEquals(310, center.x(), 1e-9);
        assertEquals(228.5, center.y(), 1e-9);
        var expectedZoom = Math.min(0.024,
                Math.min(570 / (8_000 * 1.12), 360 / (6_000 * 0.58 * 1.12)) * 0.7 * 1.35);
        assertEquals(expectedZoom, camera.zoom(), 1e-12);
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
        var systemMinimum = Math.min(570 / (8_000 * 1.12), 360 / (6_000 * 0.58 * 1.12)) * 0.7;
        var targetZoom = Math.max(systemMinimum * 0.5, originalZoom / 1.2);
        assertEquals(originalZoom + (targetZoom - originalZoom) * 0.22, firstStep, 1e-12);
        camera.advanceZoom();
        assertEquals(firstStep + (targetZoom - firstStep) * 0.22, camera.zoom(), 1e-12);
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
        assertEquals(before.x() + 16.5, firstGlide.x(), 1e-8);
        assertEquals(before.y() - 6.6, firstGlide.y(), 1e-8);

        camera.advancePan();
        var secondGlide = camera.project(500, 700, 15, 42, 590, 373);
        assertEquals(5.33, secondGlide.x() - firstGlide.x(), 1e-8);
        assertEquals(-2.132, secondGlide.y() - firstGlide.y(), 1e-8);
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
        assertEquals(0.024, camera.zoom(), 1e-12);

        camera.zoomAt(310, 228.5, -100, 15, 42, 590, 373);
        var systemZoom = Math.min(570 / (8_000 * 1.12), 360 / (6_000 * 0.58 * 1.12)) * 0.7;
        for (int index = 0; index < 100; index++) camera.advanceZoom();
        assertEquals(systemZoom * 0.5, camera.zoom(), 1e-12);
    }

    @Test
    void viewChangesInvalidateProjectedGeometryButIdleInputDoesNot() {
        var camera = camera();
        long revision = camera.revision();
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

    private static StarMapCamera camera() {
        var camera = new StarMapCamera();
        camera.fit(List.of(new StarMapCamera.Point(-3_000, 0), new StarMapCamera.Point(5_000, 6_000)),
                15, 42, 590, 373);
        return camera;
    }
}
