package rearth.oritech.spaceage.robot.client;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rearth.oritech.spaceage.robot.RobotArmReach;

import java.util.Arrays;

/** Curved-chain IK in GeckoLib model pixels; pivots come from robot_arm.geo.json. */
public final class RobotArmIk {

    public static final String[] BONES = {"rotY", "rotZ2", "rotZ3", "rotZ4", "rotZ5", "rotZ6", "rotZ8", "rotZ9", "rotX3", "rotX4"};
    public static final char[] AXES = {'y', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 'x', 'x'};
    private static final float[] HEIGHTS = {19, 24.5f, 38.5f, 46.6f, 61.5f, 69.5f, 88.5f, 94.5f, 97.5f, 105.5f};
    private static final float ROTATION_SPEED = 6;
    private final float[] angles = new float[BONES.length];
    private final Vector3f[] positions = new Vector3f[BONES.length];
    private final Vector3f tip = new Vector3f();
    public static final float MAX_EXTENSION = RobotArmReach.MAX_EXTENSION;
    private float extension;
    private boolean constrained;

    public RobotArmIk() {

        for (int i = 0; i < BONES.length; i++) {
            positions[i] = new Vector3f();
        }
        forward();
    }

    public Pose solve(Vector3f target, Quaternionf rotation) {

        constrained = rotation != null;
        if (constrained) target = new Vector3f(target).sub(rotation.transform(new Vector3f(0, 15.5f, 0)));
        var distance = target.distance(8, HEIGHTS[1], 8);
        var length = (constrained ? HEIGHTS[HEIGHTS.length - 1] : 121) - HEIGHTS[1];
        // Leave a little bend, but use the ordinary links before extending all three pistons.
        extension = Math.clamp(distance - length + 8, 0, MAX_EXTENSION);
        Arrays.fill(angles, 0);
        var heading = (float) Math.atan2(8 - target.z, target.x - 8);
        angles[0] = heading;
        angles[1] = -.35f;
        angles[2] = -.35f;
        angles[3] = -1.2f;
        angles[4] = -.25f;
        angles[5] = -.25f;
        angles[6] = -.65f;
        angles[7] = -.35f;
        refine(target);
        forward();
        var wrist = new Vector3f();
        if (constrained) {
            shareWristBend(target, rotation, heading);
            var parent = orientation(new Pose(angles, extension, tip, null));
            wrist.set(wristAngles(new Quaternionf(parent).invert().mul(rotation).normalize()));
            tip.add(rotation.transform(new Vector3f(0, 15.5f, 0)));
        }
        return new Pose(angles.clone(), extension, new Vector3f(tip), constrained ? wrist : null);
    }

    private void shareWristBend(Vector3f target, Quaternionf rotation, float heading) {

        var parent = orientation(new Pose(angles, extension, tip, null));
        var relative = new Quaternionf(parent).invert().mul(rotation).normalize();
        // Share the required X twist without choosing an angle branch at +/-180.
        // This bounded preference goes smoothly to zero when the twist is ambiguous.
        var radial = (float) Math.hypot(target.x - 8, target.z - 8);
        var wristLength = HEIGHTS[9] - HEIGHTS[8];
        // Close to the mount, a small sideways wrist offset needs a large base
        // turn. Fade sharing there so picking up cargo does not swing the base.
        var sharing = radial * radial / (radial * radial + 32 * 32);
        angles[8] = .7f * 2 * relative.x * relative.w / (.1f + relative.x * relative.x + relative.w * relative.w)
                * sharing;
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
        var rendered = next.angles().clone();
        for (int i = 0; i < rendered.length - 1; i++) {
            var delta = rendered[i] - previous.angles()[i];
            // The shoulder is a hinge: crossing zero lifts it over the mount.
            // Wrapping to the shorter angle can instead swing the piston through the floor.
            var step = (i == 1 ? delta : (float) Math.atan2(Math.sin(delta), Math.cos(delta))) * blend;
            rendered[i] = previous.angles()[i] + Math.clamp(step, -ROTATION_SPEED * seconds, ROTATION_SPEED * seconds);
        }
        var oldWrist = wristRotation(previous);
        var newWrist = wristRotation(next);
        var wristAngle = 2 * (float) Math.acos(Math.clamp(Math.abs(oldWrist.dot(newWrist)), 0, 1));
        var wristBlend = Math.min(blend, ROTATION_SPEED * seconds / Math.max(wristAngle, .0001f));
        var wrist = wristAngles(oldWrist.slerp(newWrist, wristBlend).normalize());
        rendered[rendered.length - 1] = 0;
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

        return pose.wrist() == null ? new Quaternionf().rotationX(pose.angles()[BONES.length - 1])
                : new Quaternionf().rotationZYX(pose.wrist().z, pose.wrist().y, pose.wrist().x);
    }

    private void refine(Vector3f target) {

        // All arm hinges now share one plane. Solve only its two coordinates;
        // the base supplies heading and the wrist supplies the final grasp rotation.
        var derivatives = new Vector3f[8];
        var shoulderLimit = Math.max(1.45f, (float) Math.atan2(Math.hypot(target.x - 8, target.z - 8), target.y - HEIGHTS[1]));
        for (int pass = 0; pass < 160; pass++) {
            forward();
            var error = new Vector3f(target).sub(tip).rotateY(-angles[0]);
            if (error.lengthSquared() < .0001f) break;
            var xx = 4f;
            var xy = 0f;
            var yy = 4f;
            for (int joint = 1; joint <= 7; joint++) {
                var offset = new Vector3f(tip).sub(positions[joint]).rotateY(-angles[0]);
                var axis = derivatives[joint] = new Vector3f(-offset.y, offset.x, 0);
                xx += axis.x * axis.x;
                xy += axis.x * axis.y;
                yy += axis.y * axis.y;
            }
            // Damped least squares keeps nearly straight chains well behaved.
            var determinant = xx * yy - xy * xy;
            var correctionX = (yy * error.x - xy * error.y) / determinant;
            var correctionY = (xx * error.y - xy * error.x) / determinant;
            for (int joint = 1; joint <= 7; joint++) {
                var axis = derivatives[joint];
                angles[joint] += Math.clamp(axis.x * correctionX + axis.y * correctionY, -.12f, .12f);
            }
            // Keep the shoulder up for ordinary work, but allow reaching below the machine.
            angles[1] = Math.clamp(angles[1], -shoulderLimit, shoulderLimit);
        }
    }

    private float clearancePenalty() {

        var penalty = 0f;
        // The mount is intentionally inside the housing. Check every moving link above it.
        for (int i = 2; i <= positions.length; i++) {
            var a = positions[i - 1];
            var b = i == positions.length ? tip : positions[i];
            for (int sample = 0; sample <= 8; sample++) {
                var t = sample / 8f;
                var x = a.x + (b.x - a.x) * t;
                var y = a.y + (b.y - a.y) * t;
                var z = a.z + (b.z - a.z) * t;
                var depth = Math.min(17 - y, y - 2);
                depth = Math.min(depth, Math.min(x + 6, 22 - x));
                depth = Math.min(depth, Math.min(z + 5, 21 - z));
                if (depth > 0) penalty += depth * depth;
            }
        }
        return penalty;
    }

    public boolean clearsBase() {

        return clearancePenalty() < .01f;
    }

    private void forward() {

        var orientation = new Quaternionf();
        var position = new Vector3f(8, HEIGHTS[0], 8);
        for (int i = 0; i < BONES.length; i++) {
            if (i > 0) position.add(orientation.transform(new Vector3f(0, HEIGHTS[i] - HEIGHTS[i - 1] + (i == 2 || i == 4 || i == 6 ? extension / 3 : 0), 0)));
            positions[i].set(position);
            switch (AXES[i]) {
                case 'x' -> orientation.rotateX(angles[i]);
                case 'y' -> orientation.rotateY(angles[i]);
                default -> orientation.rotateZ(angles[i]);
            }
        }
        tip.set(position).add(orientation.transform(new Vector3f(0, constrained ? 0 : 121 - HEIGHTS[HEIGHTS.length - 1], 0)));
    }

    public static Quaternionf orientation(Pose pose) {

        var rotation = new Quaternionf();
        for (int i = 0; i < BONES.length; i++) {
            if (i == BONES.length - 1 && pose.wrist() != null) {
                rotation.rotateZYX(pose.wrist().z, pose.wrist().y, pose.wrist().x);
                continue;
            }
            switch (AXES[i]) {
                case 'x' -> rotation.rotateX(pose.angles()[i]);
                case 'y' -> rotation.rotateY(pose.angles()[i]);
                default -> rotation.rotateZ(pose.angles()[i]);
            }
        }
        return rotation;
    }

    /** Lift over the housing before traversing, then lower at the destination. */
    public static Vector3f travel(Vector3f start, Vector3f end, float progress) {

        var t = Math.clamp(progress, 0, 1);
        var clearance = 64 + Math.min(32, start.distance(end) * .25f);
        var height = Math.max(clearance, Math.max(start.y, end.y) + 16);
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

    private static Vector3f outsideBase(Vector3f point) {

        var result = new Vector3f(point);
        if (point.y < 30 && point.x > -40 && point.x < 48 && point.z > -40 && point.z < 48) {
            // Clear the housing along the same heading. Choosing a fixed X side
            // makes the base turn away from the destination before turning back.
            var x = point.x - 8;
            var z = point.z - 8;
            if (x == 0 && z == 0) {
                result.x = 48;
            } else {
                var scaleX = x == 0 ? Float.POSITIVE_INFINITY : (x < 0 ? -48 : 40) / x;
                var scaleZ = z == 0 ? Float.POSITIVE_INFINITY : (z < 0 ? -48 : 40) / z;
                var scale = Math.min(scaleX, scaleZ);
                result.set(8 + x * scale, point.y, 8 + z * scale);
            }
        }
        return result;
    }

    public static float ease(float value) {

        var t = Math.clamp(value, 0, 1);
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /** Extension is the combined travel of three equally extended pistons, in pixels. */
    public record Pose(float[] angles, float extension, Vector3f tip, Vector3f wrist) {}
}
