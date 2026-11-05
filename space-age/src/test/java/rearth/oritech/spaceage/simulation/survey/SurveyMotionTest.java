package rearth.oritech.spaceage.simulation.survey;

import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceObjects.AsteroidMaterial;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SurveyMotionTest {

    private static SpaceObjectData asteroid() {

        return new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID, 1_000, 2_000, 0, 0,
                10, 0, 1, SpaceObjects.DetectionState.PRECISE, "Test", List.of());
    }

    @Test
    void discoveredContactsAlwaysUseTheCurrentPositionVelocityAndTug() {

        var knowledge = new SurveyKnowledge();
        var body = asteroid();
        assertNull(knowledge.contact(body));
        knowledge.scan(List.of(body), 100, body.x(), body.y(), 10);
        var current = body.withMotion(4_000, 5_000, 24, 2).withTug(UUID.randomUUID());
        assertEquals(current, knowledge.contact(current));
        var encoded = SurveyKnowledge.CODEC.encodeStart(JsonOps.INSTANCE, knowledge).getOrThrow();
        var restored = SurveyKnowledge.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(current, restored.contact(current));
    }

    @Test
    void currentMotionDoesNotRevealNewCompositionWithoutAScan() {

        var id = UUID.randomUUID();
        var measured = new SpaceObjectData(id, SpaceObjects.ObjectType.ASTEROID, 1_000, 2_000, 0, 0, 10, 0, 1,
                SpaceObjects.DetectionState.PRECISE, "Test", List.of(new AsteroidMaterial(Identifier.withDefaultNamespace("iron_ore"), 500)));
        var current = new SpaceObjectData(id, SpaceObjects.ObjectType.ASTEROID, 4_000, 5_000, 10, 2, 10, 0, 1,
                SpaceObjects.DetectionState.PRECISE, "Test", List.of(new AsteroidMaterial(Identifier.withDefaultNamespace("copper_ore"), 1_000)));
        var knowledge = new SurveyKnowledge();
        knowledge.scan(List.of(measured), 100, measured.x(), measured.y(), 10);
        var contact = knowledge.contact(current);
        assertEquals(current.x(), contact.x());
        assertEquals(current.velocityX(), contact.velocityX());
        assertEquals(measured.materials(), contact.materials());
    }

    @Test
    void discoveryStillNeedsTransmissionButMotionStaysCurrentAfterTransmission() {

        var onboard = new SurveyKnowledge();
        var body = asteroid();
        onboard.scan(List.of(body), 100, body.x(), body.y(), 10);
        var earth = new SurveyKnowledge();
        assertFalse(earth.precise(body.id()));
        earth.merge(onboard);
        var released = body.withMotion(6_000, 7_000, 15, 0);
        assertEquals(released, earth.contact(released));
        assertEquals(body, earth.contacts().getFirst(), "Saved scan data stays independent of live motion");
    }
}
