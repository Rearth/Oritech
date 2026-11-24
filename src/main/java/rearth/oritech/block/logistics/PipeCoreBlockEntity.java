package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.VoxelShape;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.HashMap;

public class PipeCoreBlockEntity extends BlockEntity {

    // stores the different subparts of the block. They each store their connection state
    // position = down means we're talking about the part at the bottom of the current block
    private final HashMap<PipeCore.PartPosition, PipeCore.PipePart> parts = new HashMap<>();

    private VoxelShape pipeShape = PipeCoreShape.CENTER;

    public PipeCoreBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntitiesContent.PIPE_CORE.get(), worldPosition, blockState);
    }

    private boolean isEmpty() {
        return parts.isEmpty();
    }

    public void setStartPart(PipeCore.PartPosition position) {
        parts.put(position, new PipeCore.PipePart());
        onPartsChanged();
    }

    public VoxelShape getPipeShape() {
        return pipeShape;
    }

    // mainly for shape caching
    private void onPartsChanged() {
        pipeShape = PipeCoreShape.build(parts);
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

        for (var entry : parts.entrySet()) {
            var storedPart = storedParts.child(entry.getKey().name());
            for (var direction : Direction.values()) {
                var connection = entry.getValue().get(direction);
                if (connection != PipeCore.DirectionState.NONE) {
                    storedPart.store(direction.getName(), PipeCore.DirectionState.CODEC, connection);
                }
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        var storedParts = input.childOrEmpty("parts");
        parts.clear();

        for (var position : PipeCore.PartPosition.values()) {
            storedParts.child(position.name()).ifPresent(storedPart -> {
                var part = new PipeCore.PipePart();
                for (var direction : Direction.values()) {
                    part.set(direction, storedPart.read(direction.getName(), PipeCore.DirectionState.CODEC).orElse(PipeCore.DirectionState.NONE));
                }
                parts.put(position, part);
            });
        }

        pipeShape = PipeCoreShape.build(parts);
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

        System.out.println("Updating Connections at " + getBlockPos().toShortString());

        var changed = false;
        for (var pair : parts.entrySet()) {
            changed |= updateConnection(pair.getKey(), pair.getValue());
        }

        if (changed) {
            onPartsChanged();
        }

        PipeConnectionDebugRenderer.draw(level, worldPosition, parts);
    }

    private boolean updateConnection(PipeCore.PartPosition partPosition, PipeCore.PipePart part) {
        var changed = false;
        for (var direction : Direction.values()) {
            var connection = resolveConnection(partPosition, direction);
            if (part.get(direction) != connection) {
                part.set(direction, connection);
                changed = true;
            }
        }
        return changed;
    }

    // connection rules (part -> part) based on each part's local position
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
    private PipeCore.DirectionState resolveConnection(PipeCore.PartPosition partPosition, Direction direction) {
        // the surface direction points toward the supporting block and is null for a center part
        var surface = PipeCore.PartPosition.ToDirection(partPosition);

        if (surface != null) {  // null surfaces are centers
            if (direction == surface) {
                return PipeCore.DirectionState.NONE;
            }

            // away from the support, a surface part can only reach the local center
            if (direction == surface.getOpposite()) {
                return parts.containsKey(PipeCore.PartPosition.CENTER)
                    ? PipeCore.DirectionState.CONNECTED_INSIDE
                    : PipeCore.DirectionState.NONE;
            }
        }

        // the local surface in this direction can join the center or form an inside corner
        var internalPart = PipeCore.PartPosition.FromDirection(direction);
        if (parts.containsKey(internalPart)) {
            return surface == null ? PipeCore.DirectionState.CONNECTED_INSIDE : PipeCore.DirectionState.CONNECTED_CORNER;
        }

        // go straight to a matching part before trying to wrap around the edge
        var neighbor = worldPosition.relative(direction);
        if (hasPartAt(neighbor, partPosition)) {
            return PipeCore.DirectionState.CONNECTED;
        }

        if (surface == null || level.getBlockState(neighbor).isCollisionShapeFullBlock(level, neighbor)) {
            return PipeCore.DirectionState.NONE;
        }

        // the part around the edge faces back toward the shared support
        var edgePos = neighbor.relative(surface);
        var edgePart = PipeCore.PartPosition.FromDirection(direction.getOpposite());
        return hasPartAt(edgePos, edgePart) ? PipeCore.DirectionState.CONNECTED_EDGE : PipeCore.DirectionState.NONE;
    }

    private boolean hasPartAt(BlockPos pos, PipeCore.PartPosition partPosition) {
        return level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe
            && pipe.parts.containsKey(partPosition);
    }

}
