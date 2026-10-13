package rearth.oritech.util;

import com.google.gson.Gson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.world.phys.AABB;
import rearth.oritech.block.blocks.decorative.HangarDoorBlock;
import rearth.oritech.block.blocks.decorative.HangarDoorHelperBlock;
import rearth.oritech.block.blocks.decorative.IndustrialDoorBlock;
import rearth.oritech.block.blocks.decorative.IndustrialDoorBlockHinge;
import rearth.oritech.block.blocks.interaction.EndericLaserBlock;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.MachineCoreEntity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CollisionShapes {

    private static final Gson GSON = new Gson();
    // Cache missing files too. Client and integrated server can request shapes on different threads.
    private static final Map<Identifier, Map<Direction, Map<BlockPos, VoxelShape>>> SHAPES = new ConcurrentHashMap<>();
    private static final Map<BlockState, VoxelShape> OUTLINES = new ConcurrentHashMap<>();
    private static final Map<BlockState, Map<BlockPos, VoxelShape>> CUSTOM_ROTATIONS = new ConcurrentHashMap<>();

    // Null means no authored shape: keep the block's normal behavior.
    @Nullable
    public static VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos) {
        // Helpers share their anchor's asset and facing, but return only their own local section.
        var controllerPos = getControllerPos(state, level, pos);
        var offset = pos.subtract(controllerPos);
        if (!controllerPos.equals(pos)) {
            state = level.getBlockState(controllerPos);
        }

        var segments = getSegments(state);
        if (segments == null) return null;

        // A present asset with no boxes in this cell means empty space, not a full-block fallback.
        return segments.getOrDefault(offset, Shapes.empty());
    }

    // Shared with the highlight renderer so every helper selects the same complete outline.
    public static BlockPos getControllerPos(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getBlock() instanceof MachineCoreBlock && state.getValue(MachineCoreBlock.USED)
                && level.getBlockEntity(pos) instanceof MachineCoreEntity core)
            return core.getControllerPos();

        if (state.getBlock() instanceof IndustrialDoorBlockHinge) return pos.below();
        if (state.getBlock() instanceof HangarDoorHelperBlock) return HangarDoorBlock.getAnchorPos(pos, state);
        return pos;
    }

    // Only for the visual highlight. Ray tracing and collision must keep using the local segments.
    @Nullable
    public static VoxelShape getOutlineShape(BlockState controllerState) {
        var segments = getSegments(controllerState);
        if (segments == null) return null;

        return OUTLINES.computeIfAbsent(controllerState, state -> {
            var outline = Shapes.empty();
            for (var entry : segments.entrySet()) {
                outline = Shapes.or(outline, entry.getValue().move(entry.getKey()));
            }

            // Joining the sections removes the artificial edges at core block boundaries.
            return outline.optimize();
        });
    }

    @Nullable
    private static Map<BlockPos, VoxelShape> getSegments(BlockState state) {
        // The authored boxes represent the deployed machine, not its packaged form.
        if (state.hasProperty(MultiblockMachine.ASSEMBLED) && !state.getValue(MultiblockMachine.ASSEMBLED)) return null;

        // One authored file describes the closed door. Open doors retain their existing support/empty shapes.
        // Use each block's registered property instance, not vanilla OPEN with the same name.
        if (state.getBlock() instanceof IndustrialDoorBlock && state.getValue(IndustrialDoorBlock.OPENED)) return null;
        if (state.getBlock() instanceof HangarDoorBlock && state.getValue(HangarDoorBlock.OPENED)) return null;

        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        var shapes = SHAPES.computeIfAbsent(id, CollisionShapes::load);
        if (shapes.isEmpty()) return null;

        // These models are authored upright and use custom renderer transforms, not horizontal facing.
        if (state.getBlock() instanceof EndericLaserBlock || state.getBlock() instanceof HangarDoorBlock)
            return CUSTOM_ROTATIONS.computeIfAbsent(state, key -> rotateCustomModel(shapes.get(Direction.NORTH), key));

        var facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;

        return shapes.get(facing);
    }

    private static Map<BlockPos, VoxelShape> rotateCustomModel(Map<BlockPos, VoxelShape> source, BlockState state) {
        var rotation = AnimatedBlockRotation.getModelRotation(state.getValue(BlockStateProperties.FACING),
                state.getBlock() instanceof HangarDoorBlock, state.hasProperty(HangarDoorBlock.ROTATED) && state.getValue(HangarDoorBlock.ROTATED));

        // Exported coordinates include the renderer's half-block X/Z translation. Remove it before rotation.
        var transform = new Matrix4f().translation(0.5f, 0, 0.5f).mul(rotation).translate(-0.5f, 0, -0.5f);
        var result = new HashMap<BlockPos, VoxelShape>();
        for (var entry : source.entrySet()) {
            for (var box : entry.getValue().move(entry.getKey()).toAabbs()) {
                var min = new Vector3f(Float.POSITIVE_INFINITY);
                var max = new Vector3f(Float.NEGATIVE_INFINITY);
                for (int corner = 0; corner < 8; corner++) {
                    var point = transform.transformPosition(new Vector3f(
                            (float) ((corner & 1) == 0 ? box.minX : box.maxX),
                            (float) ((corner & 2) == 0 ? box.minY : box.maxY),
                            (float) ((corner & 4) == 0 ? box.minZ : box.maxZ)));
                    min.min(point);
                    max.max(point);
                }

                // Quarter turns can leave floating-point dust across a block boundary; snap it away.
                splitBox(new AABB(snap(min.x), snap(min.y), snap(min.z), snap(max.x), snap(max.y), snap(max.z)), result);
            }
        }
        return result;
    }

    private static double snap(float value) {
        return Math.round(value * 1_000_000d) / 1_000_000d;
    }

    private static void splitBox(AABB bounds, Map<BlockPos, VoxelShape> segments) {
        // Visit each intersected cell, including negative offsets. Ceil excludes zero-width sections.
        for (int x = (int) Math.floor(bounds.minX); x < Math.ceil(bounds.maxX); x++) {
            for (int y = (int) Math.floor(bounds.minY); y < Math.ceil(bounds.maxY); y++) {
                for (int z = (int) Math.floor(bounds.minZ); z < Math.ceil(bounds.maxZ); z++) {
                    // Clip to the cell and translate to its local 0..1 coordinates.
                    var segment = Shapes.box(
                            Math.max(0, bounds.minX - x), Math.max(0, bounds.minY - y), Math.max(0, bounds.minZ - z),
                            Math.min(1, bounds.maxX - x), Math.min(1, bounds.maxY - y), Math.min(1, bounds.maxZ - z));
                    segments.merge(new BlockPos(x, y, z), segment, Shapes::or);
                }
            }
        }
    }

    private static Map<Direction, Map<BlockPos, VoxelShape>> load(Identifier id) {
        var path = "/assets/" + id.getNamespace() + "/collision/block/models/" + id.getPath() + ".collision.json";

        // A client resource manager cannot supply server collision. Read the bundled asset on both sides.
        try (var stream = CollisionShapes.class.getResourceAsStream(path)) {
            if (stream == null) return Map.of();

            var file = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), CollisionFile.class);
            var result = new EnumMap<Direction, Map<BlockPos, VoxelShape>>(Direction.class);

            // Rotate and split once per facing rather than doing geometry work on every collision query.
            for (var facing : Direction.Plane.HORIZONTAL) {
                var segments = new HashMap<BlockPos, VoxelShape>();

                for (var box : file.boxes()) {
                    var shape = Shapes.box(box.min()[0], box.min()[1], box.min()[2], box.max()[0], box.max()[1], box.max()[2]);

                    // Rotate individual boxes: rotating a combined bounding box would fill in the gaps.
                    var bounds = Geometry.rotateVoxelShape(shape, facing, AttachFace.FLOOR).bounds();

                    splitBox(bounds, segments);
                }

                result.put(facing, segments);
            }

            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load collision shape " + path, e);
        }
    }

    private record CollisionFile(List<CollisionBox> boxes) {
    }

    private record CollisionBox(double[] min, double[] max) {
    }
}
