package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.logistics.PipeCore.PartPosition;
import rearth.oritech.block.logistics.PipeCore.PipeBlockData;
import rearth.oritech.block.logistics.PipeCore.PipeKind;
import rearth.oritech.block.logistics.PipeCore.PipePart;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class PipeCoreBlockEntity extends BlockEntity {

    public static final ModelProperty<Map<PipeKind, List<AABB>>> MODEL_LAYOUT = new ModelProperty<>();

    // position = down means we're talking about the part at the bottom of the current block
    private final PipeBlockData parts = new PipeBlockData();

    private PipeCoreShape.BlockShapes pipeShapes = PipeCoreShape.EMPTY;

    public PipeCoreBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntitiesContent.PIPE_CORE.get(), worldPosition, blockState);
    }

    boolean hasMultipleParts() {
        var count = 0;
        for (var kind : PipeKind.values()) {
            count += parts.getByKind(kind).size();
        }
        return count > 1;
    }

    @Nullable PipeCoreShape.PartHit findHitPart(Vec3 start, Vec3 end) {
        return pipeShapes.findHitPart(worldPosition, start, end);
    }

    boolean hasPart(PipeKind kind, PartPosition position) {
        return parts.getByKind(kind).containsKey(position);
    }

    boolean isRibbonTop(PipeKind kind, Direction surface, Vec3 localHit) {
        return pipeShapes.isRibbonTop(kind, surface, localHit);
    }

    void forEachPart(PipeKind kind, BiConsumer<PartPosition, PipePart> action) {
        parts.getByKind(kind).forEach(action);
    }

    List<Vec3> connectionPath(PipeKind kind, PartPosition position, Direction direction, PipeCore.DirectionState state) {
        return PipeCoreShape.connectionPath(parts, kind, position, direction, state);
    }

    boolean addPart(PipeKind kind, PartPosition position) {
        if (parts.getByKind(kind).putIfAbsent(position, new PipePart()) != null) {
            return false;
        }

        updateConnections(true);
        return true;
    }

    void removePart(PipeKind kind, PartPosition position) {
        if (parts.getByKind(kind).remove(position) != null) {
            updateConnections(true);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!(level instanceof ServerLevel serverLevel)) return;

        // the chunk installs the replacement state before removing the old block entity
        // during a chunk unload the block state itself is still the pipe core
        var chunk = serverLevel.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(worldPosition.getX()),
            SectionPos.blockToSectionCoord(worldPosition.getZ()));
        if (chunk == null || chunk.getBlockState(worldPosition).is(getBlockState().getBlock())) return;

        // piston and command changes can replace several states before the final connections are known
        var server = serverLevel.getServer();
        server.schedule(server.wrapRunnable(() -> PipeCoreBlock.refreshNearbyPipes(serverLevel, worldPosition)));
    }

    public VoxelShape getPipeShape() {
        return pipeShapes.combined();
    }

    public boolean hasPipeParts() {
        return !pipeShapes.outlines().isEmpty();
    }

    public @Nullable HitOutline findHitOutline(Vec3 start, Vec3 end) {
        var part = findHitPart(start, end);
        return part == null ? null : new HitOutline(pipeShapes.outlines().get(part.kind()), part.shape());
    }

    public record HitOutline(VoxelShape kindShape, VoxelShape partShape) {}

    // mainly for shape caching
    private void onPartsChanged() {
        pipeShapes = PipeCoreShape.build(parts);
        setChanged();

        // the client needs the same parts and connections for block targeting
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var storedParts = output.child("parts");

        for (var kind : PipeKind.values()) {
            var kindParts = parts.getByKind(kind);
            if (kindParts.isEmpty()) continue;

            var storedKind = storedParts.child(kind.name());
            for (var entry : kindParts.entrySet()) {
                var storedPart = storedKind.child(entry.getKey().name());
                for (var direction : Direction.values()) {
                    var connection = entry.getValue().get(direction);
                    if (connection != PipeCore.DirectionState.NONE) {
                        storedPart.store(direction.getName(), PipeCore.DirectionState.CODEC, connection);
                    }
                }
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        var storedParts = input.childOrEmpty("parts");

        for (var kind : PipeKind.values()) {
            var kindParts = parts.getByKind(kind);
            kindParts.clear();

            // older saves stored positions directly under parts. Those parts become item pipes.
            var storedKind = kind == PipeKind.ITEM ? storedParts.child(kind.name()).orElse(storedParts) : storedParts.childOrEmpty(kind.name());
            for (var position : PartPosition.values()) {
                storedKind.child(position.name()).ifPresent(storedPart -> {
                    var part = new PipePart();
                    for (var direction : Direction.values()) {
                        part.set(direction, storedPart.read(direction.getName(), PipeCore.DirectionState.CODEC).orElse(PipeCore.DirectionState.NONE));
                    }
                    kindParts.put(position, part);
                });
            }
        }

        pipeShapes = PipeCoreShape.build(parts);

        // A part update can arrive without changing the block state. Refresh after reading its data.
        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ModelData getModelData() {
        if (!hasPipeParts()) return ModelData.EMPTY;

        // Chunk builders use a snapshot instead of reading the live block entity.
        return ModelData.of(MODEL_LAYOUT, PipeCoreShape.buildModelLayout(parts, pipeShapes));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void updateConnections() {
        updateConnections(false);
    }

    private void updateConnections(boolean changed) {
        System.out.println("Updating Connections at " + getBlockPos().toShortString());

        for (var kind : PipeKind.values()) {
            for (var partEntry : parts.getByKind(kind).entrySet()) {
                changed |= updateConnection(kind, partEntry.getKey(), partEntry.getValue());
            }
        }

        if (changed) {
            onPartsChanged();
        }
    }

    private boolean updateConnection(PipeKind kind, PartPosition partPosition, PipePart part) {
        var changed = false;
        for (var direction : Direction.values()) {
            var connection = resolveConnection(kind, partPosition, direction);
            if (part.get(direction) != connection) {
                part.set(direction, connection);
                changed = true;
            }
        }
        return changed;
    }

    // connection rules (part -> part) based on each part's local position
    // all routes can only connect parts of the same kind
    // inside the same blockspace:
    //   routes can go across inside corners to connect two perpendicular surfaces (e.g. from down to east)
    //   routes cannot connect opposite surfaces (e.g. from the floor to the ceiling)
    //   routes can connect from a surface to the center
    // to neighboring blocks across back edges for surface parts:
    //   routes can wrap around outside corners if both parts use the same support block
    //   routes can only wrap around a corner if the block along the straight path has no full-block collision shape
    // to neighboring blocks for surface parts:
    //   straight routes can only connect if the neighboring block has a part in the same part position (e.g. if they share a surface)
    // for center parts:
    //   routes can connect to neighboring center parts or any surface part within the same blockspace
    private PipeCore.DirectionState resolveConnection(PipeKind kind, PartPosition partPosition, Direction direction) {
        // the surface direction points toward the supporting block and is null for a center part
        var surface = PartPosition.ToDirection(partPosition);

        if (surface != null) {  // null surfaces are centers
            if (direction == surface) {
                return PipeCore.DirectionState.NONE;
            }

            // away from the support, a surface part can only reach the local center
            if (direction == surface.getOpposite()) {
                return hasPart(kind, PartPosition.CENTER)
                    ? PipeCore.DirectionState.CONNECTED_INSIDE
                    : PipeCore.DirectionState.NONE;
            }
        }

        // the local surface in this direction can join the center or form an inside corner
        var internalPart = PartPosition.FromDirection(direction);
        if (hasPart(kind, internalPart)) {
            return surface == null ? PipeCore.DirectionState.CONNECTED_INSIDE : PipeCore.DirectionState.CONNECTED_CORNER;
        }

        // go straight to a matching part before trying to wrap around the edge
        var neighbor = worldPosition.relative(direction);
        if (hasPartAt(neighbor, kind, partPosition)) {
            return PipeCore.DirectionState.CONNECTED;
        }

        if (surface == null || level.getBlockState(neighbor).isCollisionShapeFullBlock(level, neighbor)) {
            return PipeCore.DirectionState.NONE;
        }

        // the part around the edge faces back toward the shared support
        var edgePos = neighbor.relative(surface);
        var edgePart = PartPosition.FromDirection(direction.getOpposite());
        return hasPartAt(edgePos, kind, edgePart) ? PipeCore.DirectionState.CONNECTED_EDGE : PipeCore.DirectionState.NONE;
    }

    private boolean hasPartAt(BlockPos pos, PipeKind kind, PartPosition partPosition) {
        return level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe && pipe.hasPart(kind, partPosition);
    }

}
