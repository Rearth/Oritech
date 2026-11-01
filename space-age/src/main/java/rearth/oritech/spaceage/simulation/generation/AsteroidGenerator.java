package rearth.oritech.spaceage.simulation.generation;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.joml.Vector2f;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.AsteroidShape;
import rearth.oritech.spaceage.simulation.DenseOres;
import rearth.oritech.spaceage.simulation.SpaceObjects.Asteroid;
import rearth.oritech.spaceage.simulation.SpaceObjects.DetectionState;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.SpaceObjects.SimulatedObject;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.Field;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.LayoutType;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.Type;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.TypeSelection;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.WeightedType;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/** Generates each player's finite asteroid system once, then persists the resolved objects. */
public final class AsteroidGenerator {

    public static final ResourceKey<Registry<Type>> TYPES = ResourceKey.createRegistryKey(OritechSpaceAge.id("asteroid_types"));
    public static final ResourceKey<Registry<Field>> FIELDS = ResourceKey.createRegistryKey(OritechSpaceAge.id("asteroid_fields"));

    private AsteroidGenerator() {
    }

    public static void register(DataPackRegistryEvent.NewRegistry event) {

        event.dataPackRegistry(TYPES, Type.CODEC, Type.CODEC);
        event.dataPackRegistry(FIELDS, Field.CODEC, Field.CODEC);
    }

    public static Definitions definitions(RegistryAccess registries) {

        var types = new HashMap<Identifier, Type>();
        var fields = new HashMap<Identifier, Field>();
        registries.lookupOrThrow(TYPES).listElements().forEach(holder -> types.put(holder.key().identifier(), holder.value()));
        registries.lookupOrThrow(FIELDS).listElements().forEach(holder -> fields.put(holder.key().identifier(), holder.value()));
        return new Definitions(types, fields);
    }

    /** Bundled data also backs standalone simulation tests; server generation uses the actual datapack registries. */
    public static Definitions defaults() {

        return Defaults.DATA;
    }

    private static final class Defaults {

        private static final Definitions DATA = readDefaults();
    }

    private static Definitions readDefaults() {

        var types = new HashMap<Identifier, Type>();
        var fields = new HashMap<Identifier, Field>();
        var manifest = read("/data/oritech_space_age/asteroid_defaults.json", Codec.PASSTHROUGH)
                .convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
        for (var element : manifest.getAsJsonArray("types")) {
            var id = Identifier.parse(element.getAsString());
            types.put(id, read(path(TYPES.identifier(), id), Type.CODEC));
        }
        for (var element : manifest.getAsJsonArray("fields")) {
            var id = Identifier.parse(element.getAsString());
            fields.put(id, read(path(FIELDS.identifier(), id), Field.CODEC));
        }
        return new Definitions(types, fields);
    }

    private static String path(Identifier registry, Identifier entry) {

        return "/data/" + entry.getNamespace() + "/" + registry.getNamespace() + "/" + registry.getPath() + "/" + entry.getPath() + ".json";
    }

    private static <T> T read(String path, Codec<T> codec) {

        var stream = AsteroidGenerator.class.getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Missing bundled asteroid definition: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }

    public record Definitions(Map<Identifier, Type> types, Map<Identifier, Field> fields) {

        public Definitions {

            types = Map.copyOf(types);
            fields = Map.copyOf(fields);
            for (var entry : fields.entrySet()) {
                for (var choice : entry.getValue().asteroidTypes()) {
                    if (!types.containsKey(choice.type()))
                        throw new IllegalArgumentException("Asteroid field " + entry.getKey() + " references missing type " + choice.type());
                }
            }
        }

        public void validateBlocks() {

            for (var entry : types.entrySet()) {
                for (var material : entry.getValue().materials()) {
                    if (!BuiltInRegistries.BLOCK.containsKey(material.block()) || BuiltInRegistries.BLOCK.getValue(material.block()).defaultBlockState().isAir())
                        throw new IllegalArgumentException("Asteroid type " + entry.getKey() + " references unknown/air block " + material.block());
                }
            }
        }
    }

    public static long seed(long worldSeed, UUID owner) {

        return worldSeed ^ owner.getMostSignificantBits() ^ Long.rotateLeft(owner.getLeastSignificantBits(), 17);
    }

    private static UUID id(long seed, Identifier field, int region, int asteroid) {

        return UUID.nameUUIDFromBytes(("asteroid:" + seed + ":" + field + ":" + region + ":" + asteroid).getBytes(StandardCharsets.UTF_8));
    }

    public static List<SimulatedObject> generate(long seed, Definitions definitions) {

        var result = new ArrayList<SimulatedObject>();
        for (var entry : definitions.fields.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            var key = entry.getKey();
            var field = entry.getValue();
            var random = new Random(id(seed, key, -1, -1).getMostSignificantBits());
            var layout = field.layout();
            var count = switch (layout.type()) {
                case CLOUD -> 1;
                case BELT -> layout.surveySegments();
                case SCATTERED_CLOUDS -> layout.regionCount();
            };
            for (var index = 0; index < count; index++) {
                var region = new SimulatedObject(id(seed, key, index, -1), ObjectType.SURVEY_REGION);
                region.sourceField = key;
                region.currentPosition = new Vector2f((float) layout.center().x(), (float) layout.center().y());
                region.radius = layout.radius();
                region.name = "Survey region";
                region.currentState = DetectionState.PRECISE;
                if (layout.type() == LayoutType.BELT) {
                    var angle = index * Math.PI * 2 / count;
                    region.currentPosition.set((float) (Math.cos(angle) * layout.radius()), (float) (Math.sin(angle) * layout.radius()));
                    region.radius = layout.halfWidth();
                    region.surveyHalfAngle = (float) (Math.PI / count);
                } else if (layout.type() == LayoutType.SCATTERED_CLOUDS) {
                    var angle = random.nextDouble() * Math.PI * 2;
                    var distance = layout.distance().min() + random.nextDouble() * (layout.distance().max() - layout.distance().min());
                    region.currentPosition.add((float) (Math.cos(angle) * distance), (float) (Math.sin(angle) * distance));
                }
                result.add(region);
                populate(result, seed, key, index, region, field, definitions, random);
            }
        }
        return List.copyOf(result);
    }

    private static void populate(List<SimulatedObject> result, long seed, Identifier key, int index, SimulatedObject region,
                                 Field field, Definitions definitions, Random random) {

        var data = new SpaceObjectData(region.id, region.type, region.currentPosition.x, region.currentPosition.y,
                0, 0, region.radius, 0, 0, region.currentState, region.name, List.of(), region.surveyHalfAngle);
        var amount = field.asteroidsPerRegion().sample(random);
        var grouping = field.grouping().orElse(null);
        var groups = grouping == null ? 1 : grouping.groupsPerRegion().sample(random);
        var centers = new ArrayList<SurveyRules.Point>();
        var groupTypes = new ArrayList<Type>();
        for (var group = 0; group < groups; group++) {
            centers.add(sample(data, random));
            groupTypes.add(pick(field, definitions, random));
        }
        for (var asteroidIndex = 0; asteroidIndex < amount; asteroidIndex++) {
            var group = asteroidIndex % groups;
            var type = grouping != null && grouping.typeSelection() == TypeSelection.PER_GROUP
                    ? groupTypes.get(group) : pick(field, definitions, random);
            var position = sample(data, random);
            if (grouping != null) {
                var angle = random.nextDouble() * Math.PI * 2;
                var distance = Math.sqrt(random.nextDouble()) * grouping.spread();
                position = SurveyRules.clampToRegion(data, centers.get(group).x() + Math.cos(angle) * distance,
                        centers.get(group).y() + Math.sin(angle) * distance);
            }
            var asteroid = new Asteroid(id(seed, key, index, asteroidIndex));
            asteroid.sourceField = key;
            asteroid.currentPosition = new Vector2f((float) position.x(), (float) position.y());
            asteroid.currentState = DetectionState.HIDDEN;
            asteroid.materials = type.composition(type.volume().sample(random));
            asteroid.weight = DenseOres.mass(asteroid.materials);
            asteroid.radius = AsteroidShape.size(asteroid.materials).radius();
            asteroid.surfaceGravity = type.surfaceGravity();
            result.add(asteroid);
        }
    }

    private static Type pick(Field field, Definitions definitions, Random random) {

        var total = field.asteroidTypes().stream().mapToInt(WeightedType::weight).sum();
        var choice = random.nextInt(total);
        for (var entry : field.asteroidTypes()) {
            choice -= entry.weight();
            if (choice < 0) return definitions.types.get(entry.type());
        }
        throw new IllegalStateException("Empty asteroid type pool");
    }

    private static SurveyRules.Point sample(SpaceObjectData region, Random random) {

        var angle = random.nextDouble() * Math.PI * 2;
        if (SurveyRules.beltSegment(region)) {
            var radius = Math.hypot(region.x(), region.y());
            var inner = radius - region.radius();
            var outer = radius + region.radius();
            var distance = Math.sqrt(inner * inner + random.nextDouble() * (outer * outer - inner * inner));
            return SurveyRules.beltPoint(region, (random.nextDouble() * 2 - 1) * region.surveyHalfAngle(), distance - radius);
        }
        var distance = Math.sqrt(random.nextDouble()) * region.radius();
        return new SurveyRules.Point(region.x() + Math.cos(angle) * distance, region.y() + Math.sin(angle) * distance);
    }
}
