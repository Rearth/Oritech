package rearth.oritech.spaceage.robot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import rearth.oritech.spaceage.robot.client.RobotArmIk;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotArmIkTest {

    @Test
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void repeatedTransfersDoNotWindUpTheBase() {

        var warp = new Vector3f(8, 47, -5);
        for (var rotation : new Quaternionf[]{null, new Quaternionf().rotationX((float) Math.PI)}) {
            var solver = new RobotArmIk();
            var rendered = solve(solver, warp, rotation, 1);
            for (int action = 0; action < 24; action++) {
                var yaw = Math.toRadians(action * 135);
                var work = new Vector3f(8 + 160 * (float) Math.cos(yaw), 8, 8 - 160 * (float) Math.sin(yaw));
                for (var returning : new boolean[]{false, true}) {
                    var start = returning ? work : warp;
                    var end = returning ? warp : work;
                    var travel = 0f;
                    for (int frame = 0; frame <= 120; frame++) {
                        var hand = RobotArmIk.travel(start, end, frame / 84f);
                        var next = RobotArmIk.interpolate(rendered, solve(solver, hand, rotation, 1 / 60f, work), 1 / 60f);
                        travel += Math.abs(next.angles()[0] - rendered.angles()[0]);
                        rendered = next;
                    }
                    assertTrue(travel < Math.PI, "Base took a long detour: " + Math.toDegrees(travel) + " degrees, action " + action + " returning " + returning);
                }
            }
        }
    }

    @Test
    void handHeadingWindingCannotChangeThePoseForTheSameWorkTarget() {

        var solver = new RobotArmIk();
        var work = new Vector3f(-96, 8, 8);
        var hand = new Vector3f(48, 64, 8);
        var first = solve(solver, hand, null, 1, work);
        for (int degrees = 0; degrees <= 1440; degrees += 5) {
            var yaw = Math.toRadians(degrees);
            solve(solver, new Vector3f(8 + 40 * (float) Math.cos(yaw), 64, 8 - 40 * (float) Math.sin(yaw)), null, 1, work);
        }
        var last = solve(solver, hand, null, 1, work);
        assertTrue(last.tip().distance(hand) < .1f);
        for (int joint = 0; joint < RobotArmIk.BONES.length; joint++) {
            var delta = last.angles()[joint] - first.angles()[joint];
            assertTrue(Math.abs(Math.atan2(Math.sin(delta), Math.cos(delta))) < .001,
                    "Heading winding changed joint " + joint + " from " + first.angles()[joint] + " to " + last.angles()[joint]);
        }
    }

    @Test
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void distantTurnsPreferTheMiddleSwivelWithoutLosingReach() {

        for (var radius : new float[]{40, 100, 160, 176}) {
            for (var rotation : new Quaternionf[]{null, new Quaternionf().rotationX((float) Math.PI)}) {
                for (var height : new float[]{8, 64}) {
                    var solver = new RobotArmIk();
                    for (int degrees = 30; degrees < 360; degrees += 30) {
                        var yaw = (float) Math.toRadians(degrees);
                        var target = new Vector3f(8 + radius * (float) Math.cos(yaw), height, 8 - radius * (float) Math.sin(yaw));
                        // Check fresh loads and machines that already worked on other sides.
                        for (var current : new RobotArmIk[]{new RobotArmIk(), solver}) {
                            var pose = solve(current, target, rotation, 1);
                            var base = Math.abs(Math.atan2(Math.sin(pose.angles()[0]), Math.cos(pose.angles()[0])));
                            var middle = Math.abs(Math.atan2(Math.sin(pose.angles()[4]), Math.cos(pose.angles()[4])));
                            assertTrue(pose.tip().distance(target) < .1f, "Missed " + target);
                            assertTrue(base < Math.PI / 2, "Mount should not turn the entire arm backwards");
                            assertTrue(middle > base, "Middle should supply most of the turn: radius " + radius + " heading " + degrees + " height " + height + " base " + base + " middle " + middle);
                            assertTrue(current.clearsBase(), "Swivelling intersects the housing at " + target);
                        }
                    }
                }
            }
        }
    }

    @Test
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void rearTransfersKeepTheirBendAndAvoidRepeatedShoulderReversals() {

        var warp = new Vector3f(8, 47, -5);
        var rear = new Vector3f(-96, 8, 8);
        var routes = new Vector3f[][]{{warp, rear}, {rear, warp}, {rear, new Vector3f(-96, 8, 56)}};
        for (var rotation : new Quaternionf[]{null, new Quaternionf().rotationX((float) Math.PI)}) {
            for (var route : routes) {
                for (var rate : new int[]{30, 60, 144}) {
                    var solver = new RobotArmIk();
                    var start = route[0];
                    var end = route[1];
                    var seconds = 1f / rate;
                    var rendered = solve(solver, start, rotation, seconds);
                    var hand = new Vector3f(start);
                    var bendTarget = end.x < 0 ? end : start;
                    var peak = rendered.angles()[1];
                    var direction = 0f;
                    var reversals = 0;
                    for (int frame = 0; frame <= rate * 2; frame++) {
                        var target = RobotArmIk.travel(start, end, frame / (rate * 1.4f));
                        hand.lerp(target, 1 - (float) Math.exp(-seconds * 24));
                        var pose = solve(solver, hand, rotation, seconds, bendTarget);
                        assertTrue(pose.tip().distance(hand) < .1f, "Transfer must still reach its animated hand target");
                        var next = RobotArmIk.interpolate(rendered, pose, seconds);
                        var shoulder = next.angles()[1];
                        if (direction == 0) {
                            if (Math.abs(shoulder - peak) > .05f) {
                                direction = Math.signum(shoulder - peak);
                                peak = shoulder;
                            }
                        } else if ((shoulder - peak) * direction >= 0) {
                            peak = shoulder;
                        } else if (Math.abs(shoulder - peak) > .05f) {
                            reversals++;
                            direction = -direction;
                            peak = shoulder;
                        }
                        rendered = next;
                    }
                    assertTrue(reversals <= 1, "Shoulder should lift then reach, without extra rocking: " + reversals + " from " + start + " to " + end);
                    assertTrue(rendered.tip().distance(end) < .1f, "Transfer should finish at the destination");
                }
            }
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
                var pose = solve(new RobotArmIk(), target, rotation, 1);
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

    private static Vector3f modelPivot(JsonObject bone) {

        var pivot = bone.getAsJsonArray("pivot");
        return new Vector3f(-pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat());
    }

    @Test
    void reachesWarpAndWorkAreaWithoutDiscontinuities() {

        var solver = new RobotArmIk();
        var warp = new Vector3f(8, 47, -5);
        for (int frame = 0; frame < 240; frame++) solve(solver, warp, 1 / 60f);
        for (var target : new Vector3f[]{new Vector3f(160, 24, 0), new Vector3f(-160, 24, 0),
                new Vector3f(0, 24, 160), new Vector3f(0, 24, -160), new Vector3f(0, 184, 0), new Vector3f(0, -136, 0), new Vector3f(-16, 8, 0), new Vector3f(48, 8, 16), warp}) {
            var start = solve(solver, warp, 1 / 60f).tip();
            var previous = start;
            var previousWanted = start;
            for (int frame = 0; frame <= 240; frame++) {
                var wanted = RobotArmIk.travel(start, target, frame / 240f);
                var pose = solve(solver, wanted, 1 / 60f);
                assertTrue(pose.tip().isFinite());
                assertTrue(pose.tip().distance(previous) < wanted.distance(previousWanted) + 2, "Tip jumped " + pose.tip().distance(previous) + " target " + target + " frame " + frame + " tip " + pose.tip());
                previous = pose.tip();
                previousWanted = wanted;

            }
            for (int frame = 0; frame < 60; frame++) previous = solve(solver, target, 1 / 60f).tip();
            assertTrue(previous.distance(target) < 1, "Unreachable target " + target + ": " + previous);
            for (int frame = 0; frame <= 240; frame++) {
                var returning = solve(solver, RobotArmIk.travel(target, warp, frame / 240f), 1 / 60f);
                assertTrue(returning.tip().isFinite());
            }
            for (int frame = 0; frame < 60; frame++) solve(solver, warp, 1 / 60f);
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
                for (int frame = 0; frame < 240; frame++) pose = solve(solver, target, 1 / 60f);
                assertTrue(pose.tip().distance(target) < 1, "Boundary unreachable: " + target + " tip " + pose.tip());
                // Extreme below-base targets may clip; reach is the priority there.
            }
        }
    }

    @Test
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void nearWorkUsesCurvedJointsBeforeExtendingThePiston() {

        var solver = new RobotArmIk();
        RobotArmIk.Pose pose = null;
        for (int frame = 0; frame < 240; frame++) pose = solve(solver, new Vector3f(48, 48, 16), 1 / 60f);
        assertTrue(pose.extension() < 1, "Nearby work should not need the piston");
        for (int joint : new int[]{2, 3, 5, 7, 8, 9, 10}) {
            assertTrue(Math.abs(pose.angles()[joint]) > .05f, "Unused intermediate joint " + joint);
        }
        for (int frame = 0; frame < 240; frame++) pose = solve(solver, new Vector3f(160, 24, 0), 1 / 60f);
        assertTrue(pose.extension() > 48, "Distant work should extend the piston");
        assertTrue(pose.extension() <= RobotArmIk.MAX_EXTENSION);
    }

    @Test
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void shoulderReachesForwardBeforeThePistonExtends() {

        var solver = new RobotArmIk();
        for (var rotation : new Quaternionf[]{null, new Quaternionf().rotationX((float) Math.PI)}) {
            // The oriented grasp puts the wrist 15.5 pixels above the held block.
            var target = new Vector3f(104, rotation == null ? 32 : 16.5f, 8);
            var pose = solve(solver, target, rotation, 1);
            assertTrue(pose.tip().distance(target) < .1f, "Forward reach missed the target");
            var minimumExtension = Math.max(0, new Vector3f(104, 32, 8).distance(8, 24.5f, 8) - (rotation == null ? 96.5f : 81));
            assertTrue(pose.extension() < minimumExtension + 5, "Forward reach should use nearly the full ordinary chain");
            assertTrue(Math.abs(pose.angles()[1]) > .9f, "Shoulder should lean toward the target");
            assertTrue(solver.clearsBase(), "Forward reach intersects the housing");
        }
    }

    @Test
    void middleHingesBendAndNearGroundGraspsKeepThePistonRetracted() {

        var solver = new RobotArmIk();
        var rotation = new Quaternionf().rotationX((float) Math.PI);
        var near = solve(solver, new Vector3f(48, 8, 16), rotation, 1);
        assertTrue(near.extension() < 1, "Ground-level grasp should use the normal links");
        for (int frame = 0; frame <= 60; frame++) {
            var point = RobotArmIk.travel(new Vector3f(8, 22, -5), new Vector3f(48, 8, 16), frame / 60f);
            assertTrue(solve(solver, point, rotation, 1 / 60f).extension() < 16, "Short transfers should not stretch the piston");
        }
        var turned = solve(solver, new Vector3f(48, 48, -48), rotation, 1);
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
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void nearbyTurnsShareTheirHeadingWithTheMiddleSwivel() {

        for (var rotation : new Quaternionf[]{null, new Quaternionf().rotationX((float) Math.PI)}) {
            var target = new Vector3f(24, 64, -24);
            var pose = solve(new RobotArmIk(), target, rotation, 1);
            var heading = (float) Math.atan2(8 - target.z, target.x - 8);
            assertTrue(pose.tip().distance(target) < .1f, "Missed " + target + " tip " + pose.tip() + " angles " + Arrays.toString(pose.angles()));
            assertTrue(Math.abs(pose.angles()[0]) < heading * .75f, "Mount should not supply almost the whole turn: " + Arrays.toString(pose.angles()));
            assertTrue(Math.abs(pose.angles()[4]) > .35f && Math.abs(pose.angles()[4]) < 1.3f,
                    "Middle swivel should contribute a visible share: " + Arrays.toString(pose.angles()));
        }
    }

    @Test
    void turnsStayContinuousAcrossTheHeadingSeam() {

        var solver = new RobotArmIk();
        RobotArmIk.Pose previous = null;
        for (int degrees = 150; degrees <= 210; degrees++) {
            var angle = Math.toRadians(degrees);
            var target = new Vector3f(40 * (float) Math.cos(angle), 64, 8 - 40 * (float) Math.sin(angle));
            var pose = solve(solver, target, 1);
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
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void oppositeTargetsReverseTheLowerBendInsteadOfTurningTheMount() {

        for (var rotation : new Quaternionf[]{null, new Quaternionf().rotationX((float) Math.PI)}) {
            for (var radius : new float[]{40, 80, 160}) {
                var front = solve(new RobotArmIk(), new Vector3f(8 + radius, 64, 8), rotation, 1);
                var solver = new RobotArmIk();
                var target = new Vector3f(8 - radius, 64, 8);
                var back = solve(solver, target, rotation, 1);
                assertTrue(back.tip().distance(target) < .1f);
                assertTrue(Math.abs(back.angles()[0]) < .25f, "Opposite target should barely turn the mount: " + Arrays.toString(back.angles()));
                assertTrue(Math.abs(back.angles()[4]) > 2.5f, "Middle swivel should supply the half-turn");
                assertTrue(Math.signum(front.angles()[1]) != Math.signum(back.angles()[1]), "Lower hinge should bend the other way");
                assertTrue(solver.clearsBase());
            }
        }
    }

    @Test
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void reversedBendDoesNotFlickerBetweenNearbyTargets() {

        var solver = new RobotArmIk();
        for (int degrees = 0; degrees <= 145; degrees++) {
            var heading = Math.toRadians(degrees);
            solve(solver, new Vector3f(8 + 40 * (float) Math.cos(heading), 64, 8 - 40 * (float) Math.sin(heading)), 1 / 60f);
        }
        for (int frame = 0; frame < 120; frame++) {
            var heading = Math.toRadians(frame % 2 == 0 ? 125 : 135);
            var target = new Vector3f(8 + 40 * (float) Math.cos(heading), 64, 8 - 40 * (float) Math.sin(heading));
            var pose = solve(solver, target, 1 / 60f);
            assertTrue(Math.abs(pose.angles()[4]) > 2, "Nearby targets should retain the reversed bend");
            assertTrue(pose.tip().distance(target) < .1f, "Missed " + target + " tip " + pose.tip() + " angles " + Arrays.toString(pose.angles()));
        }
    }

    @Test
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void sideTargetsChangeSmoothlyWithoutTurningTheMountBackwards() {

        for (var rotation : new Quaternionf[]{null, new Quaternionf().rotationX((float) Math.PI)}) {
            var solver = new RobotArmIk();
            RobotArmIk.Pose previous = null;
            for (int degrees = 0; degrees <= 90; degrees++) {
                var heading = Math.toRadians(degrees);
                var target = new Vector3f(8 + 40 * (float) Math.cos(heading), 64, 8 - 40 * (float) Math.sin(heading));
                var pose = solve(solver, target, rotation, 1 / 60f);
                assertTrue(Math.abs(pose.angles()[0]) < Math.PI / 2, "Side targets should not turn the mount backwards");
                assertTrue(pose.tip().distance(target) < .1f, "Missed " + target + " tip " + pose.tip() + " angles " + Arrays.toString(pose.angles()));
                if (previous != null) {
                    for (int joint = 0; joint < RobotArmIk.BONES.length; joint++) {
                        assertTrue(Math.abs(pose.angles()[joint] - previous.angles()[joint]) < .05f, "Nearby target changed the bend abruptly");
                    }
                }
                previous = pose;
            }
        }
    }

    @Test
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void preferredSwivelStaysContinuousAroundFullTurns() {

        for (var radius : new float[]{24, 40, 80, 160}) {
            for (var rotation : new Quaternionf[]{null, new Quaternionf().rotationX((float) Math.PI)}) {
                var solver = new RobotArmIk();
                RobotArmIk.Pose previous = null;
                for (int degrees = 0; degrees <= 360; degrees++) {
                    var heading = Math.toRadians(degrees);
                    var target = new Vector3f(8 + radius * (float) Math.cos(heading), 64, 8 - radius * (float) Math.sin(heading));
                    var pose = solve(solver, target, rotation, 1 / 60f);
                    assertTrue(pose.tip().distance(target) < .1f, "Missed " + target + " tip " + pose.tip() + " angles " + Arrays.toString(pose.angles()));
                    if (previous != null) {
                        for (int joint : new int[]{0, 1, 4, 6}) {
                            // The lower bend changes at the hysteresis threshold;
                            // rendered joint speed limits cover those transitions.
                            if (degrees == 98 || degrees == 278) break;
                            var delta = pose.angles()[joint] - previous.angles()[joint];
                            assertTrue(Math.abs(Math.atan2(Math.sin(delta), Math.cos(delta))) < .15,
                                    "Swivel jumped at " + degrees + " radius " + radius + " joint " + joint);
                        }
                    }
                    previous = pose;
                }
            }
        }
    }

    @Test
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void shoulderReversesOverTheMountInsteadOfWrappingThroughTheFloor() {

        for (var direction : new float[]{-1, 1}) {
            var startAngles = new float[RobotArmIk.BONES.length];
            startAngles[1] = direction * 1.8f;
            var endAngles = startAngles.clone();
            endAngles[1] = -startAngles[1];
            var end = new RobotArmIk.Pose(endAngles, 112, new Vector3f(), null);
            for (var rate : new int[]{30, 60, 144}) {
                var pose = new RobotArmIk.Pose(startAngles, 112, new Vector3f(), null);
                for (int frame = 0; frame < rate * 2; frame++) {
                    var next = RobotArmIk.interpolate(pose, end, 1f / rate);
                    var shoulder = next.angles()[1];
                    assertTrue(Math.abs(shoulder) <= 1.8f, "Shoulder must take the path through zero");
                    var pistonEndHeight = 24.5f + (14 + next.extension() / 2) * (float) Math.cos(shoulder);
                    assertTrue(pistonEndHeight > 0, "Lower piston swung through the floor");
                    assertTrue(Math.abs(shoulder - pose.angles()[1]) <= 6f / rate + .00001f);
                    pose = next;
                }
                assertTrue(Math.abs(pose.angles()[1] - endAngles[1]) < .001f, "Shoulder should finish the transition");
            }
        }
    }

    @Test
    @Disabled("Historical two-piston rotY2 posture; model now has three pistons and middle Z hinges")
    void swivelBranchChangesRespectRenderSpeedLimits() {

        var solver = new RobotArmIk();
        RobotArmIk.Pose start = null;
        RobotArmIk.Pose end = null;
        for (int degrees = 0; degrees <= 360; degrees += 5) {
            var angle = Math.toRadians(degrees);
            var target = new Vector3f(40 * (float) Math.cos(angle), 64, 8 - 40 * (float) Math.sin(angle));
            var pose = solve(solver, target, 1);
            assertTrue(pose.tip().distance(target) < .1f, "Missed " + target + " tip " + pose.tip() + " angles " + Arrays.toString(pose.angles()));
            var swivel = pose.angles()[4];
            var residual = swivel - Math.round(swivel / (float) Math.PI) * (float) Math.PI;
            assertTrue(Math.abs(residual) <= 1.3f, "Swivel should remain near the normal or reversed bend branch");
            if (degrees == 0) start = pose;
            if (degrees == 180) end = pose;
        }
        for (int rate : new int[]{30, 60, 144}) {
            var pose = start;
            for (int frame = 0; frame < rate * 2; frame++) {
                var next = RobotArmIk.interpolate(pose, end, 1f / rate);
                for (int joint = 0; joint < RobotArmIk.BONES.length - 1; joint++) {
                    var delta = next.angles()[joint] - pose.angles()[joint];
                    var speed = joint == 4 ? 5.2f : 6f;
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
            for (var target : new Vector3f[]{new Vector3f(8, 22, -5), new Vector3f(8, 47, -5),
                    new Vector3f(48, 8, 16), new Vector3f(160, 24, 0), new Vector3f(0, -136, 0)}) {
                var solver = new RobotArmIk();
                var first = solve(solver, target, rotation, 1);
                var pose = first;
                for (int frame = 0; frame < 120; frame++) pose = solve(solver, target, rotation, 1 / 60f);
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
            if (previous != null) assertTrue(Math.abs(pose.angles()[8] - previous.angles()[8]) < .05f, "rotX3 snapped across a twist seam");
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

    private static RobotArmIk.Pose solve(RobotArmIk solver, Vector3f target, float seconds) {

        return solver.solve(target, null);
    }

    private static RobotArmIk.Pose solve(RobotArmIk solver, Vector3f target, Quaternionf rotation, float seconds) {

        return solver.solve(target, rotation);
    }

    private static RobotArmIk.Pose solve(RobotArmIk solver, Vector3f target, Quaternionf rotation, float seconds, Vector3f destination) {

        return solver.solve(target, rotation);
    }

}
