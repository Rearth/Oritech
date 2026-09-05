package rearth.oritech.spaceage.simulation;

import org.joml.Vector2f;

import java.util.UUID;

public class SpaceObjects {

    public static final UUID EARTH_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public enum ObjectType {
        EARTH, SUN, MARS, ASTEROID
    }

    public enum DetectionState {
        HIDDEN, HINTED, ROUGH, PRECISE
    }

    public static class SimulatedObject {

        public final UUID id;
        public final ObjectType type;
        public Vector2f currentPosition;
        public float radius;
        public float surfaceGravity;
        public DetectionState currentState = DetectionState.HIDDEN;

        public SimulatedObject(ObjectType type) {
            this(UUID.randomUUID(), type);
        }

        public SimulatedObject(UUID id, ObjectType type) {
            this.id = id;
            this.type = type;
        }

    }

    // Things like asteroids. Movement itsn't applied per tick, instead events / future positions are calculated during each interaction / event
    public static class MovableSimulatedObject extends SimulatedObject {
        public float weight;

        public MovableSimulatedObject(ObjectType type) {
            super(type);
        }
    }

    public static class Asteroid extends MovableSimulatedObject {
        public Asteroid() {
            super(ObjectType.ASTEROID);
        }

        // loot table data here, etc. in the future
    }
}
