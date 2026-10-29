package rearth.oritech.spaceage.client.robot_arm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.block.robot_arm.RobotArmReach;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotArmIkTest {

    private static Vector3f modelPivot(JsonObject bone) {

        var pivot = bone.getAsJsonArray("pivot");
        return new Vector3f(-pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat());
    }

    @Test
    void handHeadingWindingCannotChangeThePoseForTheSameWorkTarget() {

        var solver = new RobotArmIk();
        var hand = new Vector3f(48, 64, 8);
        var first = solver.solve(hand, null);
        for (int degrees = 0; degrees <= 1440; degrees += 5) {
            var yaw = Math.toRadians(degrees);
            solver.solve(new Vector3f(8 + 40 * (float) Math.cos(yaw), 64, 8 - 40 * (float) Math.sin(yaw)), null);
        }
        var last = solver.solve(hand, null);
        assertTrue(last.tip().distance(hand) < .1f);
        for (int joint = 0; joint < RobotArmIk.BONES.length; joint++) {
            var delta = last.angles()[joint] - first.angles()[joint];
            assertTrue(Math.abs(Math.atan2(Math.sin(delta), Math.cos(delta))) < .001,
                    "Heading winding changed joint " + joint + " from " + first.angles()[joint] + " to " + last.angles()[joint]);
        }
    }

    @Test
    void threePistonsMatchTheUpdatedModelAndShareExtension() throws Exception {

        var resource = "/assets/oritech_space_age/geckolib/models/robot_arm.geo.json";
        var bones = new HashMap<String, JsonObject>();
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8)) {
            var geometry = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            for (var element : geometry.getAsJsonArray("bones")) {
                var bone = element.getAsJsonObject();
                bones.put(bone.get("name").getAsString(), bone);
            }
        }
        for (var target : new Vector3f[]{new Vector3f(8, 22, -5), new Vector3f(48, 48, 16), new Vector3f(176, 24, 0), new Vector3f(-152, 64, 8)}) {
            for (var rotation : new Quaternionf[]{null, new Quaternionf().rotationX((float) Math.PI)}) {
                var pose = new RobotArmIk().solve(target, rotation);
                var extension = pose.extension() / 3;
                assertTrue(extension >= 0 && extension <= 64, "Each piston is limited to four blocks");
                var position = new Vector3f();
                var previous = new Vector3f();
                var orientation = new Quaternionf();
                for (int i = 0; i < RobotArmIk.BONES.length; i++) {
                    var pivot = modelPivot(bones.get(RobotArmIk.BONES[i]));
                    var offset = new Vector3f(pivot).sub(previous);
                    if (i == 2 || i == 4 || i == 6) offset.y += extension;
                    position.add(orientation.transform(offset));
                    previous.set(pivot);
                    if (i == RobotArmIk.BONES.length - 1 && pose.wrist() != null) {
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
                assertTrue(position.distance(pose.tip()) < .01f, "Solver and model disagree");
            }
        }
    }

    @Test
    void reachesWarpAndWorkAreaWithoutDiscontinuities() {

        var solver = new RobotArmIk();
        var warp = new Vector3f(8, 47, -5);
        for (int frame = 0; frame < 240; frame++) solver.solve(warp, null);
        for (var target : new Vector3f[]{new Vector3f(160, 24, 0), new Vector3f(-160, 24, 0),
                new Vector3f(0, 24, 160), new Vector3f(0, 24, -160), new Vector3f(0, 184, 0), new Vector3f(0, -136, 0), new Vector3f(-16, 8, 0), new Vector3f(48, 8, 16), warp}) {
            var start = solver.solve(warp, null).tip();
            var previous = start;
            var previousWanted = start;
            for (int frame = 0; frame <= 240; frame++) {
                var wanted = RobotArmIk.travel(start, target, frame / 240f);
                var pose = solver.solve(wanted, null);
                assertTrue(pose.tip().isFinite());
                assertTrue(pose.tip().distance(previous) < wanted.distance(previousWanted) + 2, "Tip jumped " + pose.tip().distance(previous) + " target " + target + " frame " + frame + " tip " + pose.tip());
                previous = pose.tip();
                previousWanted = wanted;

            }
            for (int frame = 0; frame < 60; frame++) previous = solver.solve(target, null).tip();
            assertTrue(previous.distance(target) < 1, "Unreachable target " + target + ": " + previous);
            for (int frame = 0; frame <= 240; frame++) {
                var returning = solver.solve(RobotArmIk.travel(target, warp, frame / 240f), null);
                assertTrue(returning.tip().isFinite());
            }
            for (int frame = 0; frame < 60; frame++) solver.solve(warp, null);
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
                for (int frame = 0; frame < 240; frame++) pose = solver.solve(target, null);
                assertTrue(pose.tip().distance(target) < 1, "Boundary unreachable: " + target + " tip " + pose.tip());
                // Extreme below-base targets may clip; reach is the priority there.
            }
        }
    }

    @Test
    void middleHingesBendAndNearGroundGraspsKeepThePistonRetracted() {

        var solver = new RobotArmIk();
        var rotation = new Quaternionf().rotationX((float) Math.PI);
        var near = solver.solve(new Vector3f(48, 8, 16), rotation);
        assertTrue(near.extension() < 1, "Ground-level grasp should use the normal links");
        for (int frame = 0; frame <= 60; frame++) {
            var point = RobotArmIk.travel(new Vector3f(8, 22, -5), new Vector3f(48, 8, 16), frame / 60f);
            assertTrue(solver.solve(point, rotation).extension() < 16, "Short transfers should not stretch the piston");
        }
        var turned = solver.solve(new Vector3f(48, 48, -48), rotation);
        assertTrue(Math.abs(turned.angles()[3]) + Math.abs(turned.angles()[4]) + Math.abs(turned.angles()[5]) > .1f, "Middle hinges should contribute to the bend");
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
            var pose = solver.solve(target, null);
            assertTrue(pose.tip().distance(target) < .1f, "Missed " + target + " tip " + pose.tip() + " angles " + Arrays.toString(pose.angles()));
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
    void wristSolvesPositionAndRotationTogetherWithoutPistonCreep() {

        for (int facing = 0; facing < 4; facing++) {
            var rotation = new Quaternionf().rotationY(facing * (float) Math.PI / 2).rotateX((float) Math.PI);
            for (var target : new Vector3f[]{new Vector3f(8, 22, -5), new Vector3f(8, 47, -5),
                    new Vector3f(48, 8, 16), new Vector3f(160, 24, 0), new Vector3f(0, -136, 0)}) {
                var solver = new RobotArmIk();
                var first = solver.solve(target, rotation);
                var pose = first;
                for (int frame = 0; frame < 120; frame++) pose = solver.solve(target, rotation);
                assertTrue(Math.abs(first.extension() - pose.extension()) < .0001f, "Piston changed after arrival");
                assertTrue(pose.tip().distance(target) < 1, "Wrist missed " + target + ": " + pose.tip());
                var actual = RobotArmIk.orientation(pose);
                assertTrue(actual.transform(new Vector3f(1, 0, 0)).distance(rotation.transform(new Vector3f(1, 0, 0))) < .001f);
                assertTrue(actual.transform(new Vector3f(0, 1, 0)).distance(rotation.transform(new Vector3f(0, 1, 0))) < .001f, "Rotation mismatch at " + target + " facing " + facing + " actual " + actual + " wrist " + pose.wrist());
            }
        }
    }

    @Test
    void middleHingesStayContinuousAcrossFullCircles() {

        for (var radius : new float[]{24, 80, 160, 224}) {
            var solver = new RobotArmIk();
            RobotArmIk.Pose previous = null;
            for (int degrees = 0; degrees <= 720; degrees++) {
                var heading = Math.toRadians(degrees);
                var target = new Vector3f(8 + radius * (float) Math.cos(heading), 64, 8 - radius * (float) Math.sin(heading));
                var pose = solver.solve(target, null);
                assertTrue(pose.tip().distance(target) < .1f, "Missed " + target);
                if (previous != null) {
                    for (int joint = 0; joint < RobotArmIk.BONES.length; joint++) {
                        var delta = pose.angles()[joint] - previous.angles()[joint];
                        assertTrue(Math.abs(Math.atan2(Math.sin(delta), Math.cos(delta))) < .15,
                                "Jump at " + degrees + " radius " + radius + " joint " + joint);
                    }
                }
                previous = pose;
            }
        }
    }

    @Test
    void newChainRespectsRenderSpeedLimits() {

        var solver = new RobotArmIk();
        var start = solver.solve(new Vector3f(8, 47, -5), null);
        var end = solver.solve(new Vector3f(-160, 8, 64), new Quaternionf().rotationX((float) Math.PI));
        for (int rate : new int[]{30, 60, 144}) {
            var pose = start;
            for (int frame = 0; frame < rate * 2; frame++) {
                var next = RobotArmIk.interpolate(pose, end, 1f / rate);
                for (int joint = 0; joint < RobotArmIk.BONES.length - 1; joint++) {
                    assertTrue(Math.abs(next.angles()[joint] - pose.angles()[joint]) <= 6f / rate + .00001f);
                }
                pose = next;
            }
            assertTrue(pose.tip().distance(end.tip()) < .01f);
        }
    }

    @Test
    void firstWristJointSharesTheGraspWithoutMovingTheHeldPivot() {

        var target = new Vector3f(64, 48, -32);
        var rotation = new Quaternionf().rotationY(.6f).rotateX(1.1f);
        var pose = new RobotArmIk().solve(target, rotation);
        assertTrue(Math.abs(pose.angles()[8]) > .1f, "rotX3 should share a useful wrist bend");
        assertTrue(Math.abs(pose.angles()[8]) < .7f, "Wrist sharing should stay modest");
        assertTrue(pose.tip().distance(target) < .1f);
        assertTrue(Math.abs(RobotArmIk.orientation(pose).dot(rotation)) > .99999f);
        var parentAngles = pose.angles().clone();
        parentAngles[8] = 0;
        var unshared = RobotArmIk.orientation(new RobotArmIk.Pose(parentAngles, pose.extension(), pose.tip(), null)).invert().mul(rotation);
        var shared = new Quaternionf().rotationZYX(pose.wrist().z, pose.wrist().y, pose.wrist().x);
        assertTrue(Math.abs(shared.w) > Math.abs(unshared.w), "Sharing should reduce the final wrist rotation");
    }

    @Test
    void wristSharingStaysContinuousAcrossTwistHalfTurns() {

        var target = new Vector3f(64, 48, -32);
        var solver = new RobotArmIk();
        RobotArmIk.Pose previous = null;
        for (int degrees = -360; degrees <= 360; degrees++) {
            var rotation = new Quaternionf().rotationX((float) Math.toRadians(degrees));
            var pose = solver.solve(target, rotation);
            assertTrue(pose.tip().distance(target) < .1f);
            assertTrue(Math.abs(RobotArmIk.orientation(pose).dot(rotation)) > .99999f);
            if (previous != null)
                assertTrue(Math.abs(pose.angles()[8] - previous.angles()[8]) < .05f, "rotX3 snapped across a twist seam");
            previous = pose;
        }
    }

    @Test
    void placementTransfersAvoidLargeBaseDetours() {

        var warp = new Vector3f(8, 22, -5);
        var rotation = new Quaternionf().rotationX((float) Math.PI);
        for (var work : new Vector3f[]{new Vector3f(-96, 8, 8), new Vector3f(112, 8, 8),
                new Vector3f(-16, 8, -16), new Vector3f(32, 8, -16)}) {
            for (var returning : new boolean[]{false, true}) {
                var start = returning ? work : warp;
                var end = returning ? warp : work;
                var solver = new RobotArmIk();
                var previous = solver.solve(start, rotation).angles()[0];
                var endYaw = solver.solve(end, rotation).angles()[0];
                var direction = Math.signum((float) Math.atan2(Math.sin(endYaw - previous), Math.cos(endYaw - previous)));
                var reverseTravel = 0f;
                for (int frame = 1; frame <= 120; frame++) {
                    var hand = RobotArmIk.travel(start, end, frame / 120f);
                    var pose = solver.solve(hand, rotation);
                    var yaw = pose.angles()[0];
                    var step = (float) Math.atan2(Math.sin(yaw - previous), Math.cos(yaw - previous));
                    reverseTravel += Math.max(0, -step * direction);
                    assertTrue(pose.tip().distance(hand) < .1f);
                    previous = yaw;
                }
                // Allow small wrist-alignment corrections, not a quarter-turn detour.
                assertTrue(reverseTravel < Math.toRadians(10), "Base took a detour of " + Math.toDegrees(reverseTravel) + " degrees on the way to " + end);
            }
        }
    }
}
