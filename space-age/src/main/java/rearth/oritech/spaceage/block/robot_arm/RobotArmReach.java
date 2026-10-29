package rearth.oritech.spaceage.block.robot_arm;

import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Shared model dimensions in pixels, relative to the bottom centre of the controller and facing north.
 * Keep the recording area inside the usable reach.
 */
public final class RobotArmReach {

    public static final float BASE_HEIGHT = 19;
    public static final float TIP_HEIGHT = 121;
    public static final float LENGTH = TIP_HEIGHT - BASE_HEIGHT;
    public static final float MAX_PISTON_EXTENSION = 4 * 16;
    public static final float MAX_EXTENSION = 3 * MAX_PISTON_EXTENSION;
    // Centre is (0, 24, 0), while the swivel is at (8, 19, 8). Reserve room for curved joints.
    public static final int RADIUS = (int) ((LENGTH + MAX_EXTENSION - Math.sqrt(8 * 8 + 5 * 5 + 8 * 8) - 32) / 16);
    /**
     * The port where carried items appear and vanish.
     */
    public static final Vector3fc WARP = new Vector3f(8, 22, -5);

    private RobotArmReach() {
    }

    /**
     * Rotation from model space (facing north) to world space around the Y axis.
     */
    public static float facingYaw(Direction facing) {

        return switch (facing) {
            case SOUTH -> (float) Math.PI;
            case WEST -> (float) Math.PI / 2;
            case EAST -> (float) -Math.PI / 2;
            default -> 0;
        };
    }
}
