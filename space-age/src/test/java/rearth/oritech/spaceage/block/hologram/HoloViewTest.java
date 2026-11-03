package rearth.oritech.spaceage.block.hologram;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Craft;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Point;
import rearth.oritech.spaceage.block.hologram.HoloView.Frame;
import rearth.oritech.spaceage.simulation.FleetCategories;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoloViewTest {

    private static HoloSettings settings(String mode, String framing) {

        var json = HoloSettings.CODEC.encodeStart(JsonOps.INSTANCE, HoloSettings.DEFAULT).getOrThrow().getAsJsonObject();
        json.addProperty("mode", mode);
        json.addProperty("framing", framing);
        return HoloSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private static HoloSnapshot snapshot(HoloSettings settings, List<Craft> craft) {

        return new HoloSnapshot(settings, new HoloSurface(BlockPos.ZERO, 8, 4), List.of(), craft,
                FleetCategories.Saved.EMPTY, 0, 1, 0, "ready");
    }

    @Test
    void irregularPadsUseAnExactSolidRectangleWithoutFillingHoles() {

        var panels = new HashSet<BlockPos>();
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 4; z++) panels.add(new BlockPos(x, 5, z));
        }
        panels.remove(new BlockPos(5, 5, 3));
        panels.add(new BlockPos(-1, 5, 0));
        var surface = HoloSurface.largestRectangle(panels);
        assertEquals(20, surface.width() * surface.depth());
        for (int x = 0; x < surface.width(); x++) {
            for (int z = 0; z < surface.depth(); z++) assertTrue(panels.contains(surface.origin().offset(x, 0, z)));
        }
        assertEquals(HoloSurface.EMPTY, HoloSurface.largestRectangle(Set.of()));
    }

    @Test
    void categoryFilteringAutoFitLimitsAndEmptyTrackingAreStable() {

        var one = new Craft(UUID.randomUUID(), "Tug", "uncategorized", 1_000, 2_000, 0, 0, UUID.randomUUID(), true, "ready", List.of());
        var other = new Craft(UUID.randomUUID(), "Relay", "other", 100_000_000, 0, 0, 0, UUID.randomUUID(), true, "ready", List.of());
        var s = settings("category", "fit");
        assertTrue(HoloView.selected(s, one));
        assertFalse(HoloView.selected(s, other));
        var frame = HoloView.frame(snapshot(s, List.of(one, other)), null);
        assertEquals(1_000, frame.x());
        assertEquals(2_000, frame.y());
        assertEquals(s.minimum(), frame.coverage());
        assertEquals(frame, HoloView.frame(snapshot(s, List.of()), frame));
        var shrunk = new Frame(0, 0, 1_000_000).towards(new Frame(0, 0, 20_000), .1, 1.2);
        var expanded = new Frame(0, 0, 20_000).towards(new Frame(0, 0, 1_000_000), .1, 1.2);
        assertTrue(expanded.coverage() - 20_000 > 1_000_000 - shrunk.coverage());
    }

    @Test
    void clippingKeepsCrossingPathsAndRejectsOutsideSegments() {

        var surface = new HoloSurface(BlockPos.ZERO, 8, 4);
        var clipped = HoloView.clip(new Point(-100, 2), new Point(100, 2), surface);
        assertEquals(2, clipped.size());
        assertEquals(.08, clipped.getFirst().x(), 1e-6);
        assertEquals(7.92, clipped.getLast().x(), 1e-6);
        assertTrue(HoloView.clip(new Point(-3, -2), new Point(10, -2), surface).isEmpty());
        assertFalse(HoloView.inside(new Point(-1, 2), surface));
    }

    @Test
    void savedSettingsAndInspectionSnapshotsRoundTrip() {

        var s = settings("craft", "journey");
        assertEquals(s, HoloSettings.CODEC.parse(JsonOps.INSTANCE, HoloSettings.CODEC.encodeStart(JsonOps.INSTANCE, s).getOrThrow()).getOrThrow());
        var snapshot = snapshot(s, List.of());
        assertEquals(snapshot, HoloSnapshot.CODEC.parse(JsonOps.INSTANCE, HoloSnapshot.CODEC.encodeStart(JsonOps.INSTANCE, snapshot).getOrThrow()).getOrThrow());
        var json = HoloSettings.CODEC.encodeStart(JsonOps.INSTANCE, s).getOrThrow().getAsJsonObject();
        json.addProperty("height", -200);
        json.addProperty("minimum", 500_000);
        json.addProperty("maximum", 20);
        var normalized = HoloSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().normalized();
        assertEquals(.16, normalized.height());
        assertEquals(normalized.minimum(), normalized.maximum());
        var frame = new Frame(0, 0, 1_000_000);
        assertEquals(new Point(4, 2), HoloView.project(0, 0, frame, snapshot));
    }
}
