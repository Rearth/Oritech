package rearth.oritech.spaceage.client.robot_arm;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import rearth.oritech.spaceage.block.robot_arm.RobotArmReach;

/**
 * Curved-chain IK in GeckoLib model pixels; pivots come from robot_arm.geo.json.
 * The solver is stateless between calls; its fields are scratch buffers reused by every solve.
 */
public final class RobotArmIk {

    public static final String[] BONES = {"rotY", "rotZ2", "rotZ3", "rotZ4", "rotZ5", "rotZ6", "rotZ8", "rotZ9", "rotX3", "rotX4"};
    public static final char[] AXES = {'y', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 'x', 'x'};
    private static final float[] HEIGHTS = {RobotArmReach.BASE_HEIGHT, 24.5f, 38.5f, 46.6f, 61.5f, 69.5f, 88.5f, 94.5f, 97.5f, 105.5f};
    // Starting bend for every solve, so the result only depends on the target.
    private static final float[] REST_ANGLES = {0, -.35f, -.35f, -1.2f, -.25f, -.25f, -.65f, -.35f, 0, 0};
    private static final int WRIST = BONES.length - 1;
    private static final int LAST_HINGE = 7;
    // The swivel sits at x = z = 8; the held pivot hangs 15.5 pixels beyond the last wrist joint.
    private static final float PIVOT = 8;
    private static final float GRIP = 15.5f;
    private static final float ROTATION_SPEED = 6;

    private final float[] angles = new float[BONES.length];
    private final Vector3f[] positions = new Vector3f[BONES.length];
    private final Vector3f tip = new Vector3f();
    private final Quaternionf frame = new Quaternionf();
    private final Vector3f cursor = new Vector3f();
    private final Vector3f link = new Vector3f();
    private final Vector3f error = new Vector3f();
    private final float[] axisX = new float[LAST_HINGE + 1];
    private final float[] axisY = new float[LAST_HINGE + 1];
    private float extension;
    private boolean constrained;

    public RobotArmIk() {

        for (int i = 0; i < BONES.length; i++) positions[i] = new Vector3f();
    }

    public Pose solve(Vector3f target, Quaternionf rotation) {

        constrained = rotation != null;
        if (constrained) target = new Vector3f(target).sub(rotation.transform(new Vector3f(0, GRIP, 0)));
        var distance = target.distance(PIVOT, HEIGHTS[1], PIVOT);
        var length = (constrained ? HEIGHTS[WRIST] : RobotArmReach.TIP_HEIGHT) - HEIGHTS[1];
        // Leave a little bend, but use the ordinary links before extending all three pistons.
        extension = Math.clamp(distance - length + 8, 0, RobotArmReach.MAX_EXTENSION);
        var heading = (float) Math.atan2(PIVOT - target.z, target.x - PIVOT);
        System.arraycopy(REST_ANGLES, 0, angles, 0, angles.length);
        angles[0] = heading;
        refine(target);
        forward();
        Vector3f wrist = null;
        if (constrained) {
            shareWristBend(target, rotation, heading);
            var parent = orientation(new Pose(angles, extension, tip, null));
            wrist = wristAngles(parent.invert().mul(rotation).normalize());
            tip.add(rotation.transform(new Vector3f(0, GRIP, 0)));
        }
        return new Pose(angles.clone(), extension, new Vector3f(tip), wrist);
    }

    private void shareWristBend(Vector3f target, Quaternionf rotation, float heading) {

        var parent = orientation(new Pose(angles, extension, tip, null));
        var relative = parent.invert().mul(rotation).normalize();
        // Share the required X twist without choosing an angle branch at +/-180.
        // This bounded preference goes smoothly to zero when the twist is ambiguous.
        var radial = (float) Math.hypot(target.x - PIVOT, target.z - PIVOT);
        var wristLength = HEIGHTS[9] - HEIGHTS[8];
        // Close to the mount, a small sideways wrist offset needs a large base
        // turn. Fade sharing there so picking up cargo does not swing the base.
        var sharing = radial * radial / (radial * radial + 32 * 32);
        angles[8] = .7f * 2 * relative.x * relative.w / (.1f + relative.x * relative.x + relative.w * relative.w) * sharing;
        // rotX3 moves the wrist sideways by eight pixels times sin(bend). Turn
        // the arm plane slightly to compensate, then solve its remaining X/Y reach.
        var sideways = wristLength * (float) Math.sin(angles[8]);
        angles[0] = heading + (float) Math.asin(Math.clamp(sideways / Math.max(radial, .001f), -1, 1));
        refine(target);
        forward();
    }

    public static Pose interpolate(Pose previous, Pose next, float seconds) {

        if (previous == null) return next;
        var blend = 1 - (float) Math.exp(-seconds * 32);
        var maxStep = ROTATION_SPEED * seconds;
        var rendered = next.angles().clone();
        for (int i = 0; i < WRIST; i++) {
            var delta = rendered[i] - previous.angles()[i];
            // The shoulder is a hinge: crossing zero lifts it over the mount.
            // Wrapping to the shorter angle can instead swing the piston through the floor.
            var step = (i == 1 ? delta : (float) Math.atan2(Math.sin(delta), Math.cos(delta))) * blend;
            rendered[i] = previous.angles()[i] + Math.clamp(step, -maxStep, maxStep);
        }
        var oldWrist = wristRotation(previous);
        var newWrist = wristRotation(next);
        var wristAngle = 2 * (float) Math.acos(Math.clamp(Math.abs(oldWrist.dot(newWrist)), 0, 1));
        var wristBlend = Math.min(blend, maxStep / Math.max(wristAngle, .0001f));
        var wrist = wristAngles(oldWrist.slerp(newWrist, wristBlend).normalize());
        rendered[WRIST] = 0;
        return new Pose(rendered, previous.extension() + (next.extension() - previous.extension()) * blend,
                new Vector3f(previous.tip()).lerp(next.tip(), blend), wrist);
    }

    private static Vector3f wristAngles(Quaternionf rotation) {

        var matrix = new Matrix3f().rotation(rotation);
        // At +/-90-degree pitch, roll and yaw describe the same axis. Choose zero
        // roll instead of extracting two undefined angles independently.
        if (Math.abs(matrix.m02()) > .999999f) {
            return new Vector3f(0, (float) Math.copySign(Math.PI / 2, -matrix.m02()),
                    (float) Math.atan2(-matrix.m10(), matrix.m11()));
        }
        return rotation.getEulerAnglesZYX(new Vector3f());
    }

    private static Quaternionf wristRotation(Pose pose) {

        return pose.wrist() == null ? new Quaternionf().rotationX(pose.angles()[WRIST])
                : new Quaternionf().rotationZYX(pose.wrist().z, pose.wrist().y, pose.wrist().x);
    }

    private void refine(Vector3f target) {

        // All arm hinges share one plane. Solve only its two coordinates;
        // the base supplies heading and the wrist supplies the final grasp rotation.
        var shoulderLimit = Math.max(1.45f, (float) Math.atan2(Math.hypot(target.x - PIVOT, target.z - PIVOT), target.y - HEIGHTS[1]));
        for (int pass = 0; pass < 160; pass++) {
            forward();
            error.set(target).sub(tip).rotateY(-angles[0]);
            if (error.lengthSquared() < .0001f) break;
            var xx = 4f;
            var xy = 0f;
            var yy = 4f;
            for (int joint = 1; joint <= LAST_HINGE; joint++) {
                link.set(tip).sub(positions[joint]).rotateY(-angles[0]);
                axisX[joint] = -link.y;
                axisY[joint] = link.x;
                xx += axisX[joint] * axisX[joint];
                xy += axisX[joint] * axisY[joint];
                yy += axisY[joint] * axisY[joint];
            }
            // Damped least squares keeps nearly straight chains well behaved.
            var determinant = xx * yy - xy * xy;
            var correctionX = (yy * error.x - xy * error.y) / determinant;
            var correctionY = (xx * error.y - xy * error.x) / determinant;
            for (int joint = 1; joint <= LAST_HINGE; joint++)
                angles[joint] += Math.clamp(axisX[joint] * correctionX + axisY[joint] * correctionY, -.12f, .12f);
            // Keep the shoulder up for ordinary work, but allow reaching below the machine.
            angles[1] = Math.clamp(angles[1], -shoulderLimit, shoulderLimit);
        }
    }

    private void forward() {

        frame.identity();
        cursor.set(PIVOT, HEIGHTS[0], PIVOT);
        for (int i = 0; i < BONES.length; i++) {
            if (i > 0) {
                var piston = i == 2 || i == 4 || i == 6 ? extension / 3 : 0;
                cursor.add(frame.transform(link.set(0, HEIGHTS[i] - HEIGHTS[i - 1] + piston, 0)));
            }
            positions[i].set(cursor);
            rotate(frame, AXES[i], angles[i]);
        }
        tip.set(cursor).add(frame.transform(link.set(0, constrained ? 0 : RobotArmReach.TIP_HEIGHT - HEIGHTS[WRIST], 0)));
    }

    private static void rotate(Quaternionf rotation, char axis, float angle) {

        switch (axis) {
            case 'x' -> rotation.rotateX(angle);
            case 'y' -> rotation.rotateY(angle);
            default -> rotation.rotateZ(angle);
        }
    }

    public static Quaternionf orientation(Pose pose) {

        var rotation = new Quaternionf();
        for (int i = 0; i < WRIST; i++) rotate(rotation, AXES[i], pose.angles()[i]);
        if (pose.wrist() != null) rotation.rotateZYX(pose.wrist().z, pose.wrist().y, pose.wrist().x);
        else rotate(rotation, AXES[WRIST], pose.angles()[WRIST]);
        return rotation;
    }

    /** Lift over the housing before traversing, then lower at the destination. */
    public static Vector3f travel(Vector3fc start, Vector3fc end, float progress) {

        var t = Math.clamp(progress, 0, 1);
        var clearance = 64 + Math.min(32, start.distance(end) * .25f);
        var height = Math.max(clearance, Math.max(start.y(), end.y()) + 16);
        var outerStart = outsideBase(start);
        var outerEnd = outsideBase(end);
        var raisedStart = new Vector3f(outerStart.x, height, outerStart.z);
        var raisedEnd = new Vector3f(outerEnd.x, height, outerEnd.z);
        if (t < .1f) return new Vector3f(start).lerp(outerStart, ease(t * 10));
        if (t > .9f) return outerEnd.lerp(end, ease((t - .9f) * 10));
        // A cubic arc avoids the stop-and-turn corners of separate lift/traverse legs.
        var u = ease((t - .1f) / .8f);
        var v = 1 - u;
        return outerStart.mul(v * v * v).add(raisedStart.mul(3 * v * v * u))
                .add(raisedEnd.mul(3 * v * u * u)).add(outerEnd.mul(u * u * u));
    }

    private static Vector3f outsideBase(Vector3fc point) {

        var result = new Vector3f(point);
        if (point.y() >= 30 || point.x() <= -40 || point.x() >= 48 || point.z() <= -40 || point.z() >= 48) return result;
        // Clear the housing along the same heading. Choosing a fixed X side
        // makes the base turn away from the destination before turning back.
        var x = point.x() - PIVOT;
        var z = point.z() - PIVOT;
        if (x == 0 && z == 0) {
            result.x = 48;
            return result;
        }
        var scaleX = x == 0 ? Float.POSITIVE_INFINITY : (x < 0 ? -48 : 40) / x;
        var scaleZ = z == 0 ? Float.POSITIVE_INFINITY : (z < 0 ? -48 : 40) / z;
        var scale = Math.min(scaleX, scaleZ);
        return result.set(PIVOT + x * scale, point.y(), PIVOT + z * scale);
    }

    public static float ease(float value) {

        var t = Math.clamp(value, 0, 1);
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /** Extension is the combined travel of three equally extended pistons, in pixels. */
    public record Pose(float[] angles, float extension, Vector3f tip, Vector3f wrist) {}
}
