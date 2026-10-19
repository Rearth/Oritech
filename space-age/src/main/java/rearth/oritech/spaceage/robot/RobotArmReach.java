package rearth.oritech.spaceage.robot;

/** Shared model dimensions in pixels; keep the recording area inside the usable reach. */
public final class RobotArmReach {

    public static final float LENGTH = 131 - 29;
    public static final float MAX_EXTENSION = 112;
    // Centre is (0, 24, 0), while the swivel is at (0, 29, 8). Reserve room for curved joints.
    public static final int RADIUS = (int) ((LENGTH + MAX_EXTENSION - Math.sqrt(5 * 5 + 8 * 8) - 32) / 16);

    private RobotArmReach() {}
}
