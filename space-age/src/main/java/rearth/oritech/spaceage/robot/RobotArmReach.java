package rearth.oritech.spaceage.robot;

/** Shared model dimensions in pixels; keep the recording area inside the usable reach. */
public final class RobotArmReach {

    public static final float LENGTH = 121 - 19;
    public static final float MAX_PISTON_EXTENSION = 4 * 16;
    public static final float MAX_EXTENSION = 2 * MAX_PISTON_EXTENSION;
    // Centre is (0, 24, 0), while the swivel is at (8, 19, 8). Reserve room for curved joints.
    public static final int RADIUS = (int) ((LENGTH + MAX_EXTENSION - Math.sqrt(8 * 8 + 5 * 5 + 8 * 8) - 32) / 16);

    private RobotArmReach() {}
}
