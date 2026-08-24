package rearth.oritech.spaceage.simulation;

import org.joml.Vector2f;

public class SpaceObjects {

    public enum DetectionState {
        HIDDEN, HINTED, ROUGH, PRECISE
    }

    public static class SimulatedObject {

        public Vector2f currentPosition;
        public DetectionState currentState = DetectionState.HIDDEN;

    }

    // Things like asteroids. Movement itsn't applied per tick, instead events / future positions are calculated during each interaction / event
    public static class MovableSimulatedObject extends SimulatedObject {
        public float weight;
    }

    public static class Asteroid extends MovableSimulatedObject {
        // loot table data here, etc. in the future
    }
}
