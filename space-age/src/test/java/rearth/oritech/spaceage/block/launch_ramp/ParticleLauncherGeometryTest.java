package rearth.oritech.spaceage.block.launch_ramp;

import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.util.Geometry;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticleLauncherGeometryTest {

    @Test
    void coresAndBeamFollowAllHorizontalFacings() {

        for (var facing : Direction.Plane.HORIZONTAL) {
            var state = SpaceAgeBlocks.LAUNCH_RAMP.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            var launcher = new LaunchRampBlockEntity(BlockPos.ZERO, state);
            var cores = launcher.getCorePositions().stream().map(pos -> Geometry.rotatePosition(pos, facing)).toList();
            assertEquals(2, cores.size());
            assertEquals(BlockPos.ZERO.above(), cores.get(0));
            assertEquals(BlockPos.ZERO.above().relative(facing), cores.get(1));
            var direction = launcher.launchDirection();
            assertEquals(1, direction.length(), 1e-10);
            assertEquals(40, Math.toDegrees(Math.asin(direction.y)), 1e-10);
            assertEquals(facing.getStepX() * Math.cos(Math.toRadians(40)), direction.x, 1e-10);
            assertEquals(facing.getStepZ() * Math.cos(Math.toRadians(40)), direction.z, 1e-10);
        }
    }

    @Test
    void muzzleFollowsAuthoredBarrelPivotAndRotation() throws Exception {

        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/oritech_space_age/geckolib/models/particle_launcher.geo.json"), StandardCharsets.UTF_8)) {
            var bones = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry")
                    .get(0).getAsJsonObject().getAsJsonArray("bones");
            var muzzleEnd = 0d;
            var boreHeight = 0d;
            for (var element : bones) {
                var bone = element.getAsJsonObject();
                var name = bone.get("name").getAsString();
                if (name.equals("end")) {
                    var cube = bone.getAsJsonArray("cubes").get(0).getAsJsonObject();
                    muzzleEnd = cube.getAsJsonArray("origin").get(2).getAsDouble() + cube.getAsJsonArray("size").get(2).getAsDouble();
                }
                if (name.equals("bone23")) {
                    var cube = bone.getAsJsonArray("cubes").get(0).getAsJsonObject();
                    boreHeight = cube.getAsJsonArray("origin").get(1).getAsDouble() + cube.getAsJsonArray("size").get(1).getAsDouble() / 2;
                }
            }
            for (var element : bones) {
                var bone = element.getAsJsonObject();
                if (!bone.get("name").getAsString().equals("acceleratorLauncher")) continue;
                var pivot = bone.getAsJsonArray("pivot");
                var tilt = Math.toRadians(bone.getAsJsonArray("rotation").get(0).getAsDouble());
                var origin = new Vector3d(pivot.get(0).getAsDouble(), pivot.get(1).getAsDouble(), pivot.get(2).getAsDouble());
                var muzzle = new Vector3d(0, boreHeight, muzzleEnd).sub(origin).rotateX(-tilt).add(origin).div(16).add(.5, 0, .5);
                var launcher = new LaunchRampBlockEntity(BlockPos.ZERO, SpaceAgeBlocks.LAUNCH_RAMP.get().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
                assertEquals(muzzle.x, launcher.muzzleOffset().x, 1e-10);
                assertEquals(muzzle.y, launcher.muzzleOffset().y, 1e-10);
                assertEquals(muzzle.z, launcher.muzzleOffset().z, 1e-10);
                return;
            }
            throw new AssertionError("Missing acceleratorLauncher bone");
        }
    }
}
