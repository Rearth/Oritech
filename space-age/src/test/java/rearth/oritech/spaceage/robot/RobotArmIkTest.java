package rearth.oritech.spaceage.robot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.robot.client.RobotArmIk;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotArmIkTest {

    @Test
    void bothPistonsMatchTheUpdatedModelAndShareExtension() throws Exception {

        var resource = "/assets/oritech_space_age/geckolib/models/robot_arm.geo.json";
        var bones = new HashMap<String, JsonObject>();
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8)) {
            var geometry = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            for (var element : geometry.getAsJsonArray("bones")) {
                var bone = element.getAsJsonObject();
                bones.put(bone.get("name").getAsString(), bone);
            }
        }
        for (var target : new Vector3f[]{new Vector3f(16, 23, 8), new Vector3f(48, 48, 16), new Vector3f(176, 24, 0)}) {
            for (var rotation : new Quaternionf[]{null, new Quaternionf().rotationX((float) Math.PI)}) {
                var pose = new RobotArmIk().solve(target, rotation, 1);
                var extension = pose.extension() / 2;
                assertTrue(extension >= 0 && extension <= 64, "Each piston is limited to four blocks");
                var position = new Vector3f();
                var previous = new Vector3f();
                var orientation = new Quaternionf();
                for (int i = 0; i < RobotArmIk.BONES.length; i++) {
                    var pivot = modelPivot(bones.get(RobotArmIk.BONES[i]));
                    var offset = new Vector3f(pivot).sub(previous);
                    if (i == 2 || i == 7) offset.y += extension;
                    position.add(orientation.transform(offset));
                    previous.set(pivot);
                    if (i == 10 && pose.wrist() != null) {
                        orientation.rotateZYX(pose.wrist().z, pose.wrist().y, pose.wrist().x);
                    } else {
                        switch (RobotArmIk.AXES[i]) {
                            case 'x' -> orientation.rotateX(pose.angles()[i]);
                            case 'y' -> orientation.rotateY(pose.angles()[i]);
                            default -> orientation.rotateZ(pose.angles()[i]);
                        }
                    }
                }
                position.add(orientation.transform(modelPivot(bones.get("heldBlockPivot")).sub(previous)));
                assertTrue(position.distance(target) < .1f, "Rendered model misses target: " + position + " wanted " + target);
                assertTrue(position.distance(pose.tip()) < .001f, "Solver and model disagree");
            }
        }
    }

    private static Vector3f modelPivot(JsonObject bone) {

        var pivot = bone.getAsJsonArray("pivot");
        return new Vector3f(-pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat());
    }

    @Test
    void reachesWarpAndWorkAreaWithoutDiscontinuities() {

        var solver = new RobotArmIk();
        var warp = new Vector3f(16, 48, 8);
        for (int frame = 0; frame < 240; frame++) solver.solve(warp, 1 / 60f);
        for (var target : new Vector3f[]{new Vector3f(160, 24, 0), new Vector3f(-160, 24, 0),
                new Vector3f(0, 24, 160), new Vector3f(0, 24, -160), new Vector3f(0, 184, 0), new Vector3f(0, -136, 0), new Vector3f(-16, 8, 0), new Vector3f(48, 8, 16), warp}) {
            var start = solver.solve(warp, 1 / 60f).tip();
            var previous = start;
            var previousWanted = start;
            for (int frame = 0; frame <= 240; frame++) {
                var wanted = RobotArmIk.travel(start, target, frame / 240f);
                var pose = solver.solve(wanted, 1 / 60f);
                assertTrue(pose.tip().isFinite());
                assertTrue(pose.tip().distance(previous) < wanted.distance(previousWanted) + 2, "Tip jumped " + pose.tip().distance(previous) + " target " + target + " frame " + frame + " tip " + pose.tip());
                previous = pose.tip();
                previousWanted = wanted;
                assertTrue(wanted.y < -64 || solver.clearsBase(), "Arm intersects housing at " + wanted + " destination " + target + " frame " + frame + " angles " + Arrays.toString(pose.angles()));
            }
            for (int frame = 0; frame < 60; frame++) previous = solver.solve(target, 1 / 60f).tip();
            assertTrue(previous.distance(target) < 1, "Unreachable target " + target + ": " + previous);
            for (int frame = 0; frame <= 240; frame++) {
                var returning = solver.solve(RobotArmIk.travel(target, warp, frame / 240f), 1 / 60f);
                assertTrue(returning.tip().y < -64 || solver.clearsBase(), "Return path intersects housing from " + target + " at frame " + frame);
            }
            for (int frame = 0; frame < 60; frame++) solver.solve(warp, 1 / 60f);
        }
    }

    @Test
    void reachesTheBoundaryAcrossTheLoweredSphere() {

        // The equal telescopes cannot clear the housing at the extreme lower cap.
        var radius = RobotArmReach.RADIUS * 16f;
        for (int pitch = -90; pitch <= 90; pitch += 30) {
            for (int yaw = 0; yaw < 360; yaw += 30) {
                var latitude = Math.toRadians(pitch);
                var longitude = Math.toRadians(yaw);
                var target = new Vector3f((float) (radius * Math.cos(latitude) * Math.cos(longitude)),
                        24 + (float) (radius * Math.sin(latitude)), (float) (radius * Math.cos(latitude) * Math.sin(longitude)));
                var solver = new RobotArmIk();
                RobotArmIk.Pose pose = null;
                for (int frame = 0; frame < 240; frame++) pose = solver.solve(target, 1 / 60f);
                assertTrue(pose.tip().distance(target) < 1, "Boundary unreachable: " + target + " tip " + pose.tip());
                assertTrue(target.y < -64 || solver.clearsBase(), "Boundary intersects housing: " + target);
            }
        }
    }

    @Test
    void nearWorkUsesCurvedJointsBeforeExtendingThePiston() {

        var solver = new RobotArmIk();
        RobotArmIk.Pose pose = null;
        for (int frame = 0; frame < 240; frame++) pose = solver.solve(new Vector3f(48, 48, 16), 1 / 60f);
        assertTrue(pose.extension() < 1, "Nearby work should not need the piston");
        for (int joint : new int[]{2, 3, 5, 7, 8, 9, 10}) {
            assertTrue(Math.abs(pose.angles()[joint]) > .05f, "Unused intermediate joint " + joint);
        }
        for (int frame = 0; frame < 240; frame++) pose = solver.solve(new Vector3f(160, 24, 0), 1 / 60f);
        assertTrue(pose.extension() > 48, "Distant work should extend the piston");
        assertTrue(pose.extension() <= RobotArmIk.MAX_EXTENSION);
    }

    @Test
    void shoulderReachesForwardBeforeThePistonExtends() {

        var solver = new RobotArmIk();
        for (var rotation : new Quaternionf[]{null, new Quaternionf().rotationX((float) Math.PI)}) {
            // The oriented grasp puts the wrist 15.5 pixels above the held block.
            var target = new Vector3f(104, rotation == null ? 32 : 16.5f, 8);
            var pose = solver.solve(target, rotation, 1);
            assertTrue(pose.tip().distance(target) < .1f, "Forward reach missed the target");
            var minimumExtension = Math.max(0, new Vector3f(104, 32, 8).distance(8, 24.5f, 8) - (rotation == null ? 96.5f : 81));
            assertTrue(pose.extension() < minimumExtension + 5, "Forward reach should use nearly the full ordinary chain");
            assertTrue(Math.abs(pose.angles()[1]) > .9f, "Shoulder should lean toward the target");
            assertTrue(solver.clearsBase(), "Forward reach intersects the housing");
        }
    }

    @Test
    void middleSwivelTurnsAndNearGroundGraspsKeepThePistonRetracted() {

        var solver = new RobotArmIk();
        var rotation = new Quaternionf().rotationX((float) Math.PI);
        var near = solver.solve(new Vector3f(48, 8, 16), rotation, 1);
        assertTrue(near.extension() < 1, "Ground-level grasp should use the normal links");
        for (int frame = 0; frame <= 60; frame++) {
            var point = RobotArmIk.travel(new Vector3f(16, 23, 8), new Vector3f(48, 8, 16), frame / 60f);
            assertTrue(solver.solve(point, rotation, 1 / 60f).extension() < 16, "Short transfers should not stretch the piston");
        }
        var turned = solver.solve(new Vector3f(48, 48, -48), rotation, 1);
        assertTrue(Math.abs(turned.angles()[4]) > .2f, "Middle swivel should contribute to the turn");
        for (int rate : new int[]{30, 60, 144}) {
            var rendered = near;
            for (int frame = 0; frame < rate; frame++) rendered = RobotArmIk.interpolate(rendered, turned, 1f / rate);
            assertTrue(RobotArmIk.orientation(rendered).transform(new Vector3f(0, 1, 0))
                    .distance(RobotArmIk.orientation(turned).transform(new Vector3f(0, 1, 0))) < .001f);
            assertTrue(Math.abs(rendered.extension() - turned.extension()) < .001f);
        }
    }

    @Test
    void turnsStayContinuousAcrossTheHeadingSeam() {

        var solver = new RobotArmIk();
        RobotArmIk.Pose previous = null;
        for (int degrees = 150; degrees <= 210; degrees++) {
            var angle = Math.toRadians(degrees);
            var target = new Vector3f(40 * (float) Math.cos(angle), 64, 8 - 40 * (float) Math.sin(angle));
            var pose = solver.solve(target, 1);
            assertTrue(pose.tip().distance(target) < .1f);
            if (previous != null) {
                for (int joint : new int[]{0, 1, 4, 6}) {
                    var delta = pose.angles()[joint] - previous.angles()[joint];
                    assertTrue(Math.abs(Math.atan2(Math.sin(delta), Math.cos(delta))) < .12,
                            "Swivel branch jumped at " + degrees + " degrees, joint " + joint);
                }
            }
            previous = pose;
        }
    }

    @Test
    void swivelStaysUprightAndBranchChangesRespectRenderSpeedLimits() {

        var solver = new RobotArmIk();
        RobotArmIk.Pose start = null;
        RobotArmIk.Pose end = null;
        for (int degrees = 0; degrees <= 360; degrees += 5) {
            var angle = Math.toRadians(degrees);
            var target = new Vector3f(40 * (float) Math.cos(angle), 64, 8 - 40 * (float) Math.sin(angle));
            var pose = solver.solve(target, 1);
            assertTrue(pose.tip().distance(target) < .1f);
            assertTrue(Math.abs(pose.angles()[4]) < Math.PI / 2, "Middle swivel must not invert the upper arm");
            if (degrees == 0) start = pose;
            if (degrees == 180) end = pose;
        }
        for (int rate : new int[]{30, 60, 144}) {
            var pose = start;
            for (int frame = 0; frame < rate * 2; frame++) {
                var next = RobotArmIk.interpolate(pose, end, 1f / rate);
                for (int joint = 0; joint < RobotArmIk.BONES.length - 1; joint++) {
                    var delta = next.angles()[joint] - pose.angles()[joint];
                    var speed = 6f;
                    assertTrue(Math.abs(Math.atan2(Math.sin(delta), Math.cos(delta))) <= speed / rate + .00001,
                            "Joint snapped faster than its render speed limit");
                }
                var oldWrist = pose.wrist() == null ? new Quaternionf().rotationX(pose.angles()[10])
                        : new Quaternionf().rotationZYX(pose.wrist().z, pose.wrist().y, pose.wrist().x);
                var newWrist = new Quaternionf().rotationZYX(next.wrist().z, next.wrist().y, next.wrist().x);
                var wristStep = 2 * Math.acos(Math.clamp(Math.abs(oldWrist.dot(newWrist)), 0, 1));
                assertTrue(wristStep <= 6.0 / rate + .0002, "Wrist snapped faster than its render speed limit");
                pose = next;
            }
        }
    }

    @Test
    void wristSolvesPositionAndRotationTogetherWithoutPistonCreep() {

        for (int facing = 0; facing < 4; facing++) {
            var rotation = new Quaternionf().rotationY(facing * (float) Math.PI / 2).rotateX((float) Math.PI);
            for (var target : new Vector3f[]{new Vector3f(16, 23, 8), new Vector3f(16, 48, 8),
                    new Vector3f(48, 8, 16), new Vector3f(160, 24, 0), new Vector3f(0, -136, 0)}) {
                var solver = new RobotArmIk();
                var first = solver.solve(target, rotation, 1);
                var pose = first;
                for (int frame = 0; frame < 120; frame++) pose = solver.solve(target, rotation, 1 / 60f);
                assertTrue(Math.abs(first.extension() - pose.extension()) < .0001f, "Piston changed after arrival");
                assertTrue(pose.tip().distance(target) < 1, "Wrist missed " + target + ": " + pose.tip());
                var actual = RobotArmIk.orientation(pose);
                assertTrue(actual.transform(new Vector3f(1, 0, 0)).distance(rotation.transform(new Vector3f(1, 0, 0))) < .0002f);
                assertTrue(actual.transform(new Vector3f(0, 1, 0)).distance(rotation.transform(new Vector3f(0, 1, 0))) < .0002f, "Rotation mismatch at " + target + " facing " + facing + " actual " + actual + " wrist " + pose.wrist());
            }
        }
    }

}
