package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringRepresentable;
import org.joml.Vector2f;
import java.util.Locale;

import java.util.UUID;

public class SpaceObjects {

    public static final UUID EARTH_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public enum ObjectType implements StringRepresentable {
        EARTH, SUN, MARS, ASTEROID;

        public static final Codec<ObjectType> CODEC = StringRepresentable.fromEnum(ObjectType::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum DetectionState implements StringRepresentable {
        HIDDEN, HINTED, ROUGH, PRECISE;

        public static final Codec<DetectionState> CODEC = StringRepresentable.fromEnum(DetectionState::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static class SimulatedObject {
        public static final Codec<SimulatedObject> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(object -> object.id),
                ObjectType.CODEC.fieldOf("type").forGetter(object -> object.type),
                Codec.FLOAT.fieldOf("x").forGetter(object -> object.currentPosition.x),
                Codec.FLOAT.fieldOf("y").forGetter(object -> object.currentPosition.y),
                Codec.FLOAT.fieldOf("radius").forGetter(object -> object.radius),
                Codec.FLOAT.fieldOf("gravity").forGetter(object -> object.surfaceGravity),
                DetectionState.CODEC.fieldOf("detection").forGetter(object -> object.currentState),
                Codec.FLOAT.optionalFieldOf("weight", 0F)
                        .forGetter(object -> object instanceof MovableSimulatedObject movable ? movable.weight : 0F)
        ).apply(instance, (id, type, x, y, radius, gravity, detection, weight) -> {
            var object = type == ObjectType.ASTEROID ? new Asteroid(id) : new SimulatedObject(id, type);
            object.currentPosition = new Vector2f(x, y);
            object.radius = radius;
            object.surfaceGravity = gravity;
            object.currentState = detection;
            if (object instanceof MovableSimulatedObject movable) movable.weight = weight;
            return object;
        }));

        public final UUID id;
        public final ObjectType type;
        public Vector2f currentPosition;
        public float radius;
        public float surfaceGravity;
        public DetectionState currentState = DetectionState.HIDDEN;

        public SimulatedObject(ObjectType type) {
            this(UUID.randomUUID(), type);
        }

        public SimulatedObject(UUID id, ObjectType type) {
            this.id = id;
            this.type = type;
        }

    }

    // Things like asteroids. Movement itsn't applied per tick, instead events / future positions are calculated during each interaction / event
    public static class MovableSimulatedObject extends SimulatedObject {
        public float weight;

        public MovableSimulatedObject(UUID id, ObjectType type) {
            super(id, type);
        }
    }

    public static class Asteroid extends MovableSimulatedObject {
        public Asteroid() {
            this(UUID.randomUUID());
        }

        private Asteroid(UUID id) {
            super(id, ObjectType.ASTEROID);
        }

        // loot table data here, etc. in the future
    }
}
