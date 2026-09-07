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
        var expectedZoom = Math.min(570 / (8_000 * 1.12), 360 / (6_000 * 0.58 * 1.12)) * 0.7 * 1.35;
        assertEquals(expectedZoom, camera.zoom(), 1e-12);
        var projected = camera.project(-1_234, 5_678, 15, 42, 590, 373);
        var restored = camera.unproject(projected.x(), projected.y(), 15, 42, 590, 373);
        assertEquals(-1_234, restored.x(), 1e-8);
        assertEquals(5_678, restored.y(), 1e-8);
    }

    @Test
    void zoomKeepsThePointUnderThePointerFixed() {
        var camera = camera();
        var before = camera.unproject(123, 234, 15, 42, 590, 373);
        camera.zoomAt(123, 234, 1, 15, 42, 590, 373);
        var after = camera.project(before.x(), before.y(), 15, 42, 590, 373);
        assertEquals(123, after.x(), 1e-8);
        assertEquals(234, after.y(), 1e-8);
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
