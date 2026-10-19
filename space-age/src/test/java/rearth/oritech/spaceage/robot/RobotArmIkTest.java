package rearth.oritech.spaceage.robot;

import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.robot.client.RobotArmIk;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotArmIkTest {

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
                assertTrue(solver.clearsBase(), "Arm intersects housing at " + wanted + " destination " + target + " frame " + frame + " angles " + Arrays.toString(pose.angles()));
            }
            for (int frame = 0; frame < 60; frame++) previous = solver.solve(target, 1 / 60f).tip();
            assertTrue(previous.distance(target) < 1, "Unreachable target " + target + ": " + previous);
            for (int frame = 0; frame <= 240; frame++) {
                solver.solve(RobotArmIk.travel(target, warp, frame / 240f), 1 / 60f);
                assertTrue(solver.clearsBase(), "Return path intersects housing from " + target + " at frame " + frame);
            }
            for (int frame = 0; frame < 60; frame++) solver.solve(warp, 1 / 60f);
        }
    }

    @Test
    void reachesTheBoundaryAcrossTheLoweredSphere() {

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
                assertTrue(solver.clearsBase(), "Boundary intersects housing: " + target);
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
