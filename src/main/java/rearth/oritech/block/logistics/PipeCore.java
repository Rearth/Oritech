package rearth.oritech.block.logistics;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;

public class PipeCore {

    // each kind has its own set of parts, even when they share a position
    public static class PipeBlockData {
        private final Map<PartPosition, PipePart> itemParts = new EnumMap<>(PartPosition.class);
        private final Map<PartPosition, PipePart> fluidParts = new EnumMap<>(PartPosition.class);
        private final Map<PartPosition, PipePart> energyParts = new EnumMap<>(PartPosition.class);

        public Map<PartPosition, PipePart> getByKind(PipeKind kind) {
            return switch (kind) {
                case ITEM -> itemParts;
                case FLUID -> fluidParts;
                case ENERGY -> energyParts;
            };
        }

    }

    public enum PipeKind {
        ITEM, FLUID, ENERGY
    }

    // a pipe entity has up to 7 parts per type (e.g. 3 types: item, fluid, energy), meaning up to 21 parts in total
    // each part has 1 direction state per cardinal direction
    // state of a single direction of a single part
    public enum DirectionState implements StringRepresentable {
        NONE, // none is just no state / it not being set
        CONNECTED,  // connected to pipe / machine on neighboring block on same surface or part position (for centers)
        CONNECTED_EDGE, // connected to a pipe that is "back" by one block relative to the current surface of the part (not possible for center) (outside corner)
        CONNECTED_INSIDE, // connected from surface to center
        CONNECTED_CORNER; // for inside connections in the same block space between 2 surfaces (e.g. inside 90 degree angle)

        public static final Codec<DirectionState> CODEC = StringRepresentable.fromEnum(DirectionState::values);

        @Override
        public String getSerializedName() {
            return name();
        }
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
}
