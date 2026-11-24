package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.HashMap;
import java.util.HashSet;

public class PipeCoreBlockEntity extends BlockEntity {

    // stores the different subparts of the block. They each store their connection state
    // position = down means we're talking about the part at the bottom of the current block
    private final HashMap<PartPosition, PipePart> parts = new HashMap<>();

    public PipeCoreBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntitiesContent.PIPE_CORE.get(), worldPosition, blockState);
    }

    private boolean isEmpty() {
        return parts.isEmpty();
    }

    public void setStartPart(PartPosition position) {
        parts.put(position, new PipePart());
    }

    public void updateConnections() {

        System.out.println("Updating Connections at " + getBlockPos().toShortString());

        for (var pair : parts.entrySet()) {
            updateConnection(pair.getKey(), pair.getValue());
        }

        PipeConnectionDebugRenderer.draw(level, worldPosition, parts);
    }

    private void updateConnection(PartPosition partPosition, PipePart part) {

        // the surface direction points toward the supporting block, or is null for a center part (e.g. down for a part at the bottom)
        var surface = PartPosition.ToDirection(partPosition);

        for (var direction : Direction.values()) {
            var state = DirectionState.NONE;

            // a surface part reaches the center in the direction away from its support
            var internalCandidatePos = surface != null && direction == surface.getOpposite()
                ? PartPosition.CENTER
                : PartPosition.FromDirection(direction);

            // each direction stores one route, so prefer an internal connection when available. Technically those should be exclusive of each other.
            if (parts.containsKey(internalCandidatePos) && canConnectToInternalPart(partPosition, internalCandidatePos)) {
                state = surface == null || internalCandidatePos == PartPosition.CENTER
                    ? DirectionState.CONNECTED_INSIDE
                    : DirectionState.CONNECTED_CORNER;
            } else if (surface == null || direction.getAxis() != surface.getAxis()) {   // e.g. either be a center (surface null)  or we're checking a local horizontal dir
                var neighbor = worldPosition.relative(direction);
                var neighborState = level.getBlockState(neighbor);

                // go straight to a matching part before trying to wrap around the edge
                if (neighborState.is(BlockContent.PIPE_CORE)
                    && level.getBlockEntity(neighbor) instanceof PipeCoreBlockEntity neighborPipe
                    && neighborPipe.parts.containsKey(partPosition)
                    && canConnectToNeighborPart(worldPosition, neighbor, partPosition, partPosition)) {
                    state = DirectionState.CONNECTED;
                } else if (surface != null && !neighborState.isCollisionShapeFullBlock(level, neighbor)) {
                    // the part around the edge faces back toward the shared support
                    var edgePos = neighbor.relative(surface);
                    var edgePart = PartPosition.FromDirection(direction.getOpposite());
                    if (level.getBlockState(edgePos).is(BlockContent.PIPE_CORE)
                        && level.getBlockEntity(edgePos) instanceof PipeCoreBlockEntity edgePipe
                        && edgePipe.parts.containsKey(edgePart)
                        && canConnectToNeighborPart(worldPosition, edgePos, partPosition, edgePart)) {
                        state = DirectionState.CONNECTED_EDGE;
                    }
                }
            }

            part.set(direction, state);
        }
    }

    // connection rules (part-> part) (for each part, based on the parts local position):
    // inside the same blockspace:
    //   routes can go across inside corners in the same blockspace to connect to another part inside the same blockspace (e.g. from down to east)
    //   routes cannot connect from the floor to ceiling / the opposite surface
    //   routes can connect from the surface to the center
    // to neighboring blocks across back edges for surface parts:
    //   routes can connect across corners if the surface if the same support block is used by a pipe part there
    //   routes can only connect across corners if the corner isn't occupied by a solid block (already checked before reaching this method)
    // to neighboring blocks for surface parts:
    //   routes can only connect if the neighboring block has a part on the same part position (e.g. if they share a surface)
    // for center parts:
    //   routes can only be created to neighboring center parts, or any part within the same block space as the current center

    // this method applied those rules regarding neighboring blocks (e.g. not inside the same blockspace).
    private boolean canConnectToNeighborPart(BlockPos source, BlockPos target, PartPosition sourcePart, PartPosition targetPart) {
        // center candidates only come from the direct neighbor check
        if (sourcePart == PartPosition.CENTER || targetPart == PartPosition.CENTER) {
            return sourcePart == targetPart;    // center only connects to other centers across blocks. If only one is a center, this returns false
        }

        var sourceSurface = PartPosition.ToDirection(sourcePart);
        var targetSurface = PartPosition.ToDirection(targetPart);

        // same-surface candidates only come from the direct check along the surface
        if (sourceSurface == targetSurface) {
            return true;
        }

        // outside corners join perpendicular faces of the same support block
        return source.relative(sourceSurface).equals(target.relative(targetSurface));
    }

    // this implements those rules regarding inside connections (e.g. inside the same blockspace)
    private boolean canConnectToInternalPart(PartPosition sourcePart, PartPosition targetPart) {
        if (sourcePart == targetPart) return false;
        if (sourcePart == PartPosition.CENTER || targetPart == PartPosition.CENTER) return true;

        // inside corners join perpendicular surfaces within the same block
        return PartPosition.ToDirection(sourcePart).getAxis() != PartPosition.ToDirection(targetPart).getAxis();
    }

    // can be either on a surface (e.g. all 6 cardinal directions) or the center.
    public static class PipePart {

        // those are all world space cardinal positions and reflect the computed auto-connection states. Overrides are applied afterwards
        DirectionState NORTH = DirectionState.NONE;
        DirectionState EAST = DirectionState.NONE;
        DirectionState SOUTH = DirectionState.NONE;
        DirectionState WEST = DirectionState.NONE;
        DirectionState UP = DirectionState.NONE;
        DirectionState DOWN = DirectionState.NONE;

        // all included directions are disabled. This could be turned into a single int in the future as bit map / flag
        HashSet<Direction> disabledDirections = new HashSet<>();

        DirectionState get(Direction direction) {
            return switch (direction) {
                case NORTH -> NORTH;
                case EAST -> EAST;
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                case UP -> UP;
                case DOWN -> DOWN;
            };
        }

        void set(Direction direction, DirectionState state) {
            switch (direction) {
                case NORTH -> NORTH = state;
                case EAST -> EAST = state;
                case SOUTH -> SOUTH = state;
                case WEST -> WEST = state;
                case UP -> UP = state;
                case DOWN -> DOWN = state;
            }
        }

        boolean isDisabled(Direction direction) {
            return disabledDirections.contains(direction);
        }

        void setDisabled(Direction direction, boolean disabled) {
            if (disabled) {
                disabledDirections.add(direction);
            } else {
                disabledDirections.remove(direction);
            }
        }

    }

    public enum DirectionState {
        NONE, // none is just no state / it not being set
        CONNECTED,  // connected to pipe / machine on neighboring block on same surface or part position (for centers)
        CONNECTED_EDGE, // connected to a pipe that is "back" by one block relative to the current surface of the part (not possible for center) (outside corner)
        CONNECTED_INSIDE, // connected from surface to center
        CONNECTED_CORNER // for inside connections in the same block space between 2 surfaces (e.g. inside 90 degree angle)
    }

    // basically direction but with center
    public enum PartPosition {
        NORTH, EAST, SOUTH, WEST, UP, DOWN, CENTER;

        // center  = null
        static PartPosition FromDirection(@Nullable Direction direction) {
            return switch (direction) {
                case NORTH -> NORTH;
                case EAST -> EAST;
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                case UP -> UP;
                case DOWN -> DOWN;
                case null -> CENTER;
            };
        }

        @Nullable
        static Direction ToDirection(PartPosition position) {
            return switch (position) {
                case NORTH -> Direction.NORTH;
                case EAST -> Direction.EAST;
                case SOUTH -> Direction.SOUTH;
                case WEST -> Direction.WEST;
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
                case CENTER -> null;
            };
        }

    }

}
