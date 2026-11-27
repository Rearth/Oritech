package rearth.oritech.block.logistics;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
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

    public enum PipeKind implements StringRepresentable {
        ITEM(4), FLUID(12), ENERGY(8);

        static final Codec<PipeKind> CODEC = StringRepresentable.fromEnum(PipeKind::values);

        // lane coordinate in model units (0 to 16)
        final int laneCoordinate;

        PipeKind(int laneCoordinate) {
            this.laneCoordinate = laneCoordinate;
        }

        boolean supportsExtraction() {
            return this == ITEM || this == FLUID;
        }

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    // a pipe entity has up to 7 parts per type (e.g. 3 types: item, fluid, energy), meaning up to 21 parts in total
    // each part has 1 direction state per cardinal direction
    // state of a single direction of a single part
    public enum DirectionState implements StringRepresentable {
        NONE, // none is just no state / it not being set
        CONNECTED,  // connected to a matching pipe part in the neighboring block
        CONNECTED_EDGE, // connected to a pipe that is "back" by one block relative to the current surface of the part (not possible for center) (outside corner)
        CONNECTED_INSIDE, // connected from surface to center
        CONNECTED_CORNER, // for inside connections in the same block space between 2 surfaces (e.g. inside 90 degree angle)
        CONNECTED_MACHINE; // ends at the neighboring block's capability on the face toward the pipe

        public static final Codec<DirectionState> CODEC = StringRepresentable.fromEnum(DirectionState::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }

    // basically direction but with center
    public enum PartPosition implements StringRepresentable {
        NORTH, EAST, SOUTH, WEST, UP, DOWN, CENTER;

        static final Codec<PartPosition> CODEC = StringRepresentable.fromEnum(PartPosition::values);

        @Override
        public String getSerializedName() {
            return name();
        }

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

        // These world-space directions contain the connection states after applying wrench settings.
        DirectionState NORTH = DirectionState.NONE;
        DirectionState EAST = DirectionState.NONE;
        DirectionState SOUTH = DirectionState.NONE;
        DirectionState WEST = DirectionState.NONE;
        DirectionState UP = DirectionState.NONE;
        DirectionState DOWN = DirectionState.NONE;

        static final Codec<Map<Direction, DirectionState>> CLOSED_PORTS_CODEC = Codec.unboundedMap(Direction.CODEC, DirectionState.CODEC);

        // Remember which route a closed port had so it can be reopened from either endpoint.
        Map<Direction, DirectionState> closedPorts = Map.of();

        // Keep the setting when a wrench closes the port so reopening restores its mode.
        final EnumSet<Direction> extractionPorts = EnumSet.noneOf(Direction.class);

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
            return closedPorts.containsKey(direction);
        }

        boolean isExtracting(Direction direction) {
            return get(direction) == DirectionState.CONNECTED_MACHINE && extractionPorts.contains(direction);
        }

        static Map<Direction, DirectionState> readLegacyClosedPorts(int mask) {
            if (mask == 0) return Map.of();
            var ports = new EnumMap<Direction, DirectionState>(Direction.class);
            for (var direction : Direction.values()) {
                if ((mask & (1 << direction.get3DDataValue())) != 0) ports.put(direction, DirectionState.NONE);
            }
            return Map.copyOf(ports);
        }
    }
}
