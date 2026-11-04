package rearth.oritech.spaceage.client.hologram;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.block.hologram.HoloSettings;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot;
import rearth.oritech.spaceage.block.hologram.HoloSurface;
import rearth.oritech.spaceage.block.hologram.HoloView.Frame;
import rearth.oritech.spaceage.simulation.FleetCategories;
import rearth.oritech.spaceage.simulation.SpaceObjects.DetectionState;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoloAsteroidGroupsTest {

    @Test
    void crowdsCollapseButPinnedContactsAndZoomedInContactsRemainVisible() {

        var first = new SpaceObjectData(new UUID(0, 1), ObjectType.ASTEROID, -100, 0, 10, 0, DetectionState.PRECISE);
        var second = new SpaceObjectData(new UUID(0, 2), ObjectType.ASTEROID, 0, 0, 10, 0, DetectionState.PRECISE);
        var third = new SpaceObjectData(new UUID(0, 3), ObjectType.ASTEROID, 100, 0, 10, 0, DetectionState.PRECISE);
        var data = new HoloSnapshot(HoloSettings.DEFAULT, new HoloSurface(BlockPos.ZERO, 5, 6),
                List.of(third, first, second), List.of(), FleetCategories.Saved.EMPTY, 0, 1, 0, "ready");
        var groups = new HoloAsteroidGroups();
        assertTrue(groups.hidden(data, new Frame(0, 0, 2_000), null).isEmpty(), "Nearby but separate contacts stay visible");
        assertEquals(Set.of(second.id(), third.id()), groups.hidden(data, new Frame(0, 0, 10_000), null));
        assertFalse(groups.hidden(data, new Frame(0, 0, 10_000), third.id()).contains(third.id()));
        assertTrue(groups.hidden(data, new Frame(0, 0, 1_000), null).isEmpty());
        // Off-table contacts cannot become the representative of a visible cluster.
        assertTrue(groups.hidden(data, new Frame(5_000, 0, 1_000), null).isEmpty());
    }
}
