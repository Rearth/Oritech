package rearth.oritech.spaceage.simulation.generation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import rearth.oritech.spaceage.simulation.DenseOres;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules;
import rearth.oritech.spaceage.simulation.SpaceObjects.AsteroidMaterial;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Small datapack schemas; resolved asteroids are saved independently of these definitions. */
public final class AsteroidDefinitions {

    private AsteroidDefinitions() {
    }

    public record IntRange(int min, int max) {

        public static final Codec<IntRange> CODEC = RecordCodecBuilder.<IntRange>create(i -> i.group(
                Codec.INT.fieldOf("min").forGetter(IntRange::min),
                Codec.INT.fieldOf("max").forGetter(IntRange::max)
        ).apply(i, IntRange::new)).validate(value -> value.min >= 0 && value.max >= value.min
                ? DataResult.success(value) : DataResult.error(() -> "Expected 0 <= min <= max"));

        public int sample(Random random) {

            return min + random.nextInt(max - min + 1);
        }
    }

    public record Distance(double min, double max) {

        public static final Codec<Distance> CODEC = RecordCodecBuilder.<Distance>create(i -> i.group(
                Codec.DOUBLE.fieldOf("min").forGetter(Distance::min),
                Codec.DOUBLE.fieldOf("max").forGetter(Distance::max)
        ).apply(i, Distance::new)).validate(value -> value.min >= 0 && value.max >= value.min
                ? DataResult.success(value) : DataResult.error(() -> "Expected 0 <= min <= max distance"));
    }

    public record Point(double x, double y) {

        public static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Point::x),
                Codec.DOUBLE.fieldOf("y").forGetter(Point::y)
        ).apply(i, Point::new));
    }

    public record Material(Identifier block, int weight) {

        public static final Codec<Material> CODEC = RecordCodecBuilder.<Material>create(i -> i.group(
                Identifier.CODEC.fieldOf("block").forGetter(Material::block),
                Codec.INT.fieldOf("weight").forGetter(Material::weight)
        ).apply(i, Material::new)).validate(value -> value.weight > 0 ? DataResult.success(value)
                : DataResult.error(() -> "Material weights must be positive"));

        public Material {

            block = DenseOres.denseVariant(block);
        }
    }

    public record Type(IntRange volume, List<Material> materials, float surfaceGravity) {

        public static final Codec<Type> CODEC = RecordCodecBuilder.<Type>create(i -> i.group(
                IntRange.CODEC.fieldOf("volume").forGetter(Type::volume),
                Material.CODEC.listOf().fieldOf("materials").forGetter(Type::materials),
                Codec.FLOAT.optionalFieldOf("surface_gravity", 0F).forGetter(Type::surfaceGravity)
        ).apply(i, Type::new)).validate(value -> value.volume.min > 0 && !value.materials.isEmpty()
                && value.surfaceGravity >= 0 && value.materials.stream().map(Material::block).distinct().count() == value.materials.size()
                ? DataResult.success(value) : DataResult.error(() -> "Types need positive volume, unique materials and nonnegative gravity"));

        public Type {

            materials = List.copyOf(materials);
        }

        public List<AsteroidMaterial> composition(int blocks) {

            var weights = materials.stream().mapToDouble(Material::weight).toArray();
            var amounts = AsteroidImpactRules.partition(blocks, weights);
            var result = new ArrayList<AsteroidMaterial>();
            for (var index = 0; index < amounts.length; index++) {
                if (amounts[index] > 0) result.add(new AsteroidMaterial(materials.get(index).block, amounts[index]));
            }
            return List.copyOf(result);
        }
    }

    public enum LayoutType implements StringRepresentable {
        CLOUD, BELT, SCATTERED_CLOUDS;

        public static final Codec<LayoutType> CODEC = StringRepresentable.fromEnum(LayoutType::values);

        @Override
        public String getSerializedName() {

            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Layout(LayoutType type, Point center, float radius, float halfWidth,
                         int surveySegments, int regionCount, Distance distance) {

        public static final Codec<Layout> CODEC = RecordCodecBuilder.<Layout>create(i -> i.group(
                LayoutType.CODEC.fieldOf("type").forGetter(Layout::type),
                Point.CODEC.fieldOf("center").forGetter(Layout::center),
                Codec.FLOAT.fieldOf("radius").forGetter(Layout::radius),
                Codec.FLOAT.optionalFieldOf("half_width", 0F).forGetter(Layout::halfWidth),
                Codec.INT.optionalFieldOf("survey_segments", 1).forGetter(Layout::surveySegments),
                Codec.INT.optionalFieldOf("region_count", 1).forGetter(Layout::regionCount),
                Distance.CODEC.optionalFieldOf("distance", new Distance(0, 0)).forGetter(Layout::distance)
        ).apply(i, Layout::new)).validate(value -> value.radius > 0 && value.regionCount > 0 && value.surveySegments > 0
                && (value.type != LayoutType.BELT || value.halfWidth > 0 && value.radius > value.halfWidth
                        && value.center.x == 0 && value.center.y == 0)
                ? DataResult.success(value) : DataResult.error(() -> "Invalid field dimensions; belts must be centred on the Sun"));
    }

    public enum TypeSelection implements StringRepresentable {
        PER_GROUP, PER_ASTEROID;

        public static final Codec<TypeSelection> CODEC = StringRepresentable.fromEnum(TypeSelection::values);

        @Override
        public String getSerializedName() {

            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Grouping(IntRange groupsPerRegion, float spread, TypeSelection typeSelection) {

        public static final Codec<Grouping> CODEC = RecordCodecBuilder.<Grouping>create(i -> i.group(
                IntRange.CODEC.fieldOf("groups_per_region").forGetter(Grouping::groupsPerRegion),
                Codec.FLOAT.fieldOf("spread").forGetter(Grouping::spread),
                TypeSelection.CODEC.optionalFieldOf("type_selection", TypeSelection.PER_ASTEROID).forGetter(Grouping::typeSelection)
        ).apply(i, Grouping::new)).validate(value -> value.groupsPerRegion.min > 0 && value.spread >= 0
                ? DataResult.success(value) : DataResult.error(() -> "Groups need a positive count and nonnegative spread"));
    }

    public record WeightedType(Identifier type, int weight) {

        public static final Codec<WeightedType> CODEC = RecordCodecBuilder.<WeightedType>create(i -> i.group(
                Identifier.CODEC.fieldOf("type").forGetter(WeightedType::type),
                Codec.INT.fieldOf("weight").forGetter(WeightedType::weight)
        ).apply(i, WeightedType::new)).validate(value -> value.weight > 0 ? DataResult.success(value)
                : DataResult.error(() -> "Asteroid type weights must be positive"));
    }

    public record Field(Layout layout, IntRange asteroidsPerRegion, List<WeightedType> asteroidTypes, Optional<Grouping> grouping) {

        public static final Codec<Field> CODEC = RecordCodecBuilder.<Field>create(i -> i.group(
                Layout.CODEC.fieldOf("layout").forGetter(Field::layout),
                IntRange.CODEC.fieldOf("asteroids_per_region").forGetter(Field::asteroidsPerRegion),
                WeightedType.CODEC.listOf().fieldOf("asteroid_types").forGetter(Field::asteroidTypes),
                Grouping.CODEC.optionalFieldOf("grouping").forGetter(Field::grouping)
        ).apply(i, Field::new)).validate(value -> !value.asteroidTypes.isEmpty() ? DataResult.success(value)
                : DataResult.error(() -> "Fields need at least one weighted asteroid type"));

        public Field {

            asteroidTypes = List.copyOf(asteroidTypes);
        }
    }
}
