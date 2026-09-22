package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringRepresentable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns one game-scaled solar system and the flight plans created inside it.
 * Positions use one shared two-dimensional orbital plane with the sun at the origin.
 */
public class SpaceSimulation {

    public static final UUID SUN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID MARS_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    public static final Codec<SpaceSimulation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(simulation -> simulation.simulationId),
            SpaceObjects.SimulatedObject.CODEC.listOf().fieldOf("objects")
                    .forGetter(simulation -> simulation.nonCelestialObjects.stream()
                            .sorted(Comparator.comparing(object -> object.id)).toList()),
            StoredFlightPlan.CODEC.listOf().optionalFieldOf("flight_plans", List.of())
                    .forGetter(simulation -> simulation.flightPlans.entrySet().stream()
                            .map(entry -> new StoredFlightPlan(entry.getKey(), entry.getValue())).toList()),
            SurveyKnowledge.CODEC.optionalFieldOf("earth_knowledge").forGetter(simulation -> java.util.Optional.of(simulation.earthKnowledge))
    ).apply(instance, SpaceSimulation::new));

    private static final Set<SpaceObjects.SimulatedObject> CELESTIAL_OBJECTS = new HashSet<>();

    private final Set<SpaceObjects.SimulatedObject> nonCelestialObjects = new HashSet<>();
    private final UUID simulationId;
    public final SurveyKnowledge earthKnowledge;
    public UUID id() { return simulationId; }
    private final Map<GlobalPos, FlightPlan> flightPlans = new HashMap<>();

    private SpaceSimulation(UUID loadedSimulationId, List<SpaceObjects.SimulatedObject> loadedObjects,
                            List<StoredFlightPlan> loadedPlans, java.util.Optional<SurveyKnowledge> knowledge) {
        earthKnowledge = knowledge.orElseGet(SurveyKnowledge::new);
        simulationId = loadedSimulationId;
        nonCelestialObjects.addAll(loadedObjects);
        assignMissingAsteroidNames();
        loadedPlans.forEach(entry -> flightPlans.put(entry.assembler(), entry.plan()));
    }

    public SpaceSimulation() {
        simulationId = UUID.randomUUID();
        earthKnowledge = new SurveyKnowledge();
        generateRandomObjects();
    }

    private void generateRandomObjects() {
        addCluster(-3_160_000, 0, 55_000, 12);
        for (int index = 0; index < 72; index++) {
            var angle = index * Math.PI * 2 / 72;
            addCluster(Math.cos(angle) * 6_500_000, Math.sin(angle) * 6_500_000, 310_000, 4);
        }
        for (int index = 0; index < 10; index++) {
            var angle = Math.random() * Math.PI * 2;
            var radius = 3_500_000 + Math.random() * 6_000_000;
            addCluster(Math.cos(angle) * radius, Math.sin(angle) * radius, 150_000, 6);
        }
        assignMissingAsteroidNames();
    }

    private void assignMissingAsteroidNames() {
        var asteroids = nonCelestialObjects.stream()
                .filter(object -> object.type == SpaceObjects.ObjectType.ASTEROID)
                .map(object -> (SpaceObjects.Asteroid) object)
                .sorted(Comparator.comparing(object -> object.id)).toList();
        for (int index = 0; index < asteroids.size(); index++) {
            var asteroid = asteroids.get(index);
            if (asteroid.name.isBlank()) asteroid.name = "Asteroid " + String.format(Locale.ROOT, "%03d", index + 1);
            if (asteroid.weight <= 0) asteroid.weight = Math.max(1, (asteroid.radius - 1_500) / 35);
            if (asteroid.materials.isEmpty()) asteroid.materials = sampleMaterials(asteroid.weight, asteroid.id.hashCode());

        }
    }

    private void addCluster(double x, double y, double radius, int count) {
        var region = new SpaceObjects.SimulatedObject(UUID.randomUUID(), SpaceObjects.ObjectType.SURVEY_REGION);
        region.currentPosition = new Vector2f((float) x, (float) y);
        region.radius = (float) radius;
        region.name = "Survey region";
        region.currentState = SpaceObjects.DetectionState.HINTED;
        nonCelestialObjects.add(region);
        for (int index = 0; index < count; index++) {
            var angle = Math.random() * Math.PI * 2;
            var offset = Math.sqrt(Math.random()) * radius;
            var asteroid = new SpaceObjects.Asteroid();
            asteroid.currentPosition = new Vector2f((float) (x + Math.cos(angle) * offset), (float) (y + Math.sin(angle) * offset));
            asteroid.currentState = SpaceObjects.DetectionState.HIDDEN;
            asteroid.weight = (float) (1 + Math.random() * 80);
            asteroid.radius = 1_500 + asteroid.weight * 35;
            asteroid.surfaceGravity = asteroid.weight * 0.0002f;
            asteroid.materials = sampleMaterials(asteroid.weight, index);
            nonCelestialObjects.add(asteroid);
        }
    }

    private static List<SpaceObjects.AsteroidMaterial> sampleMaterials(float mass, int variant) {
        int blocks = Math.max(8, Math.round(mass * 12));
        var stone = variant % 2 == 0 ? "minecraft:stone" : "minecraft:deepslate";
        var ore = switch (Math.floorMod(variant, 4)) {
            case 0 -> "minecraft:iron_ore";
            case 1 -> "minecraft:copper_ore";
            case 2 -> "minecraft:gold_ore";
            default -> "minecraft:redstone_ore";
        };
        return List.of(
                new SpaceObjects.AsteroidMaterial(Identifier.parse(stone), blocks * 4 / 5),
                new SpaceObjects.AsteroidMaterial(Identifier.parse(ore), Math.max(1, blocks / 5)));
    }

    static {
        var sun = new SpaceObjects.SimulatedObject(SUN_ID, SpaceObjects.ObjectType.SUN);
        sun.currentPosition = new Vector2f(0, 0);
        sun.radius = 250_000;
        sun.surfaceGravity = 274;
        sun.currentState = SpaceObjects.DetectionState.PRECISE;
        CELESTIAL_OBJECTS.add(sun);

        var earth = new SpaceObjects.SimulatedObject(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH);
        earth.currentPosition = new Vector2f(-3_000_000, 0);
        earth.radius = 60_000;
        earth.surfaceGravity = 9.81f;
        earth.currentState = SpaceObjects.DetectionState.PRECISE;
        CELESTIAL_OBJECTS.add(earth);

        var mars = new SpaceObjects.SimulatedObject(MARS_ID, SpaceObjects.ObjectType.MARS);
        mars.currentPosition = new Vector2f(5_000_000, 1_500_000);
        mars.radius = 45_000;
        mars.surfaceGravity = 3.71f;
        mars.currentState = SpaceObjects.DetectionState.PRECISE;
        CELESTIAL_OBJECTS.add(mars);
    }

    public FlightPlannerSnapshot createFlightPlannerSnapshot(GlobalPos assemblerPosition, UUID rocketId) {
        var objects = createObjectData();
        return new FlightPlannerSnapshot(simulationId, rocketId, objects,
                flightPlans.getOrDefault(assemblerPosition, FlightPlan.empty()));
    }

    public List<SpaceObjectData> createObjectData() { return knownObjects(earthKnowledge); }

    public List<SpaceObjectData> knownObjects(SurveyKnowledge knowledge) {
        var truth = truth();
        var visible = new ArrayList<>(truth.stream().filter(object -> {
            if (object.type() == SpaceObjects.ObjectType.ASTEROID) return false;
            if (object.type() != SpaceObjects.ObjectType.SURVEY_REGION) return true;
            return truth.stream().anyMatch(candidate -> candidate.type() == SpaceObjects.ObjectType.ASTEROID
                    && Math.hypot(candidate.x() - object.x(), candidate.y() - object.y()) <= object.radius()
                    && knowledge.contact(candidate.id()) == null);
        }).toList());
        visible.addAll(knowledge.contacts());
        visible.sort(Comparator.comparing(SpaceObjectData::type).thenComparing(SpaceObjectData::id));
        return List.copyOf(visible);
    }

    public List<SpaceObjectData> truth() {
        var objects = new ArrayList<SpaceObjectData>();
        CELESTIAL_OBJECTS.stream().map(SpaceSimulation::toData).forEach(objects::add);
        nonCelestialObjects.stream().map(SpaceSimulation::toData).forEach(objects::add);
        objects.sort(Comparator.comparing(SpaceObjectData::type).thenComparing(SpaceObjectData::id));
        return objects;
    }

    public void tickAsteroids(net.minecraft.server.level.ServerLevel level) {
        var removed = new ArrayList<SpaceObjects.SimulatedObject>();
        for (var object : nonCelestialObjects) {
            if (!(object instanceof SpaceObjects.Asteroid asteroid) || asteroid.velocity.lengthSquared() == 0) continue;
            asteroid.currentPosition.add(asteroid.velocity.x / 20, asteroid.velocity.y / 20);
            if (Math.hypot(asteroid.currentPosition.x + 3_000_000, asteroid.currentPosition.y) > 60_000) continue;
            var action = asteroid.landing == null ? FlightPlanAction.create(ActionType.NAVIGATE_TO) : asteroid.landing;
            var pos = new BlockPos(action.landingX() + action.landingOffsetX(), 0, action.landingZ() + action.landingOffsetZ());
            if (!level.hasChunkAt(pos)) { asteroid.currentPosition.sub(asteroid.velocity.x / 20, asteroid.velocity.y / 20); continue; }
            var earth = toData(CELESTIAL_OBJECTS.stream().filter(o -> o.id.equals(SpaceObjects.EARTH_ID)).findFirst().orElseThrow());
            var impact = AsteroidImpactRules.predictArrival(0, earth, asteroid.velocity.length(), toData(asteroid), action);
            deliverAsteroid(level, impact); removed.add(asteroid);
        }
        nonCelestialObjects.removeAll(removed);
    }

    public void moveAttached(UUID id, double x, double y) {
        nonCelestialObjects.stream().filter(o -> o.id.equals(id)).findFirst().ifPresent(o -> o.currentPosition.set((float) x, (float) y));
    }

    public void releaseAsteroid(UUID id, double x, double y, double vx, double vy, FlightPlanAction landing) {
        nonCelestialObjects.stream().filter(o -> o.id.equals(id)).findFirst().ifPresent(o -> {
            o.currentPosition.set((float) x, (float) y);
            if (o instanceof SpaceObjects.Asteroid asteroid) {
                asteroid.velocity.set((float) vx, (float) vy); asteroid.landing = landing;
            }
        });
    }

    public void applyAsteroidImpact(UUID targetId, AsteroidImpactRules.ImpactPrediction impact) {
        var target = nonCelestialObjects.stream().filter(o -> o.id.equals(targetId)).findFirst().orElse(null);
        if (!(target instanceof SpaceObjects.Asteroid asteroid) || impact.fragments().isEmpty()) return;
        for (int index = 0; index < impact.fragments().size(); index++) {
            var fragment = impact.fragments().get(index);
            var child = new SpaceObjects.Asteroid();
            var angle = index * Math.PI * 2 / impact.fragments().size();
            child.currentPosition = new Vector2f(asteroid.currentPosition).add((float) Math.cos(angle) * asteroid.radius, (float) Math.sin(angle) * asteroid.radius);
            child.weight = fragment.mass(); child.radius = fragment.radius(); child.materials = fragment.materials();
            child.name = asteroid.name + " fragment " + (index + 1); nonCelestialObjects.add(child);
        }
        if (impact.remainingTargetMass() <= 0) nonCelestialObjects.remove(asteroid);
        else {
            double fraction = impact.remainingTargetMass() / asteroid.weight;
            asteroid.weight = impact.remainingTargetMass(); asteroid.radius *= (float) Math.cbrt(fraction);
            asteroid.materials = AsteroidImpactRules.scaleMaterials(asteroid.materials, fraction);
        }
    }

    public void recoverAsteroid(net.minecraft.server.level.ServerLevel level, UUID id, AsteroidImpactRules.ImpactPrediction impact) {
        if (nonCelestialObjects.removeIf(o -> o.id.equals(id))) deliverAsteroid(level, impact);
    }

    private static void deliverAsteroid(net.minecraft.server.level.ServerLevel level, AsteroidImpactRules.ImpactPrediction impact) {
        var pos = new BlockPos(impact.landingX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                impact.landingX(), impact.landingZ()), impact.landingZ());
        if (impact.craterRadiusBlocks() > 0) level.explode(null, pos.getX(), pos.getY(), pos.getZ(),
                Math.min(12, impact.craterRadiusBlocks()), net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        for (var material : impact.recoverableMaterials()) {
            var item = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(material.block()).asItem();
            int remaining = material.amount();
            while (remaining > 0) {
                int count = Math.min(64, remaining); remaining -= count;
                level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5,
                        new net.minecraft.world.item.ItemStack(item, count)));
            }
        }
    }

    public boolean updateFlightPlan(GlobalPos assemblerPosition, FlightPlan plan, ActiveRocketData rocket) {
        var validated = RocketFlightPlanRules.validate(plan, rocket, createObjectData());
        if (validated == null) return false;
        return !validated.equals(flightPlans.put(assemblerPosition, validated));
    }

    private static SpaceObjectData toData(SpaceObjects.SimulatedObject object) {
        var velocity = object instanceof SpaceObjects.MovableSimulatedObject movable
                ? movable.velocity : new Vector2f();
        float mass = object instanceof SpaceObjects.MovableSimulatedObject movable ? movable.weight : 0;
        var materials = object instanceof SpaceObjects.Asteroid asteroid ? asteroid.materials : List.<SpaceObjects.AsteroidMaterial>of();
        return new SpaceObjectData(object.id, object.type, object.currentPosition.x,
                object.currentPosition.y, velocity.x, velocity.y, object.radius, object.surfaceGravity,
                mass, object.currentState, object.name, materials);
    }

    public record SpaceObjectData(UUID id, SpaceObjects.ObjectType type, float x, float y,
                                  float velocityX, float velocityY, float radius,
                                  float surfaceGravity, float mass,
                                  SpaceObjects.DetectionState detectionState, String name,
                                  List<SpaceObjects.AsteroidMaterial> materials, float uncertainty, float compositionConfidence) {
        public static final Codec<SpaceObjectData> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(SpaceObjectData::id),
                SpaceObjects.ObjectType.CODEC.fieldOf("type").forGetter(SpaceObjectData::type),
                Codec.FLOAT.fieldOf("x").forGetter(SpaceObjectData::x), Codec.FLOAT.fieldOf("y").forGetter(SpaceObjectData::y),
                Codec.FLOAT.fieldOf("vx").forGetter(SpaceObjectData::velocityX), Codec.FLOAT.fieldOf("vy").forGetter(SpaceObjectData::velocityY),
                Codec.FLOAT.fieldOf("radius").forGetter(SpaceObjectData::radius), Codec.FLOAT.fieldOf("gravity").forGetter(SpaceObjectData::surfaceGravity),
                Codec.FLOAT.fieldOf("mass").forGetter(SpaceObjectData::mass),
                SpaceObjects.DetectionState.CODEC.fieldOf("detection").forGetter(SpaceObjectData::detectionState),
                Codec.STRING.fieldOf("name").forGetter(SpaceObjectData::name),
                SpaceObjects.AsteroidMaterial.CODEC.listOf().fieldOf("materials").forGetter(SpaceObjectData::materials),
                Codec.FLOAT.fieldOf("uncertainty").forGetter(SpaceObjectData::uncertainty),
                Codec.FLOAT.fieldOf("composition").forGetter(SpaceObjectData::compositionConfidence)
        ).apply(i, SpaceObjectData::new));
        public SpaceObjectData(UUID id, SpaceObjects.ObjectType type, float x, float y, float vx, float vy,
                               float radius, float gravity, float mass, SpaceObjects.DetectionState state, String name,
                               List<SpaceObjects.AsteroidMaterial> materials) {
            this(id, type, x, y, vx, vy, radius, gravity, mass, state, name, materials, 0, 1);
        }
        public SpaceObjectData(UUID id, SpaceObjects.ObjectType type, float x, float y, float radius,
                               float surfaceGravity, SpaceObjects.DetectionState detectionState) {
            this(id, type, x, y, 0, 0, radius, surfaceGravity, 0, detectionState, "", List.of());
        }

        public SpaceObjectData {
            materials = List.copyOf(materials);
        }

        public double xAt(double seconds) {
            return x + velocityX * seconds;
        }

        public double yAt(double seconds) {
            return y + velocityY * seconds;
        }
    }

    public record FlightPlannerSnapshot(UUID simulationId, UUID rocketId,
                                        List<SpaceObjectData> objects, FlightPlan plan) {
    }

    private record StoredFlightPlan(GlobalPos assembler, FlightPlan plan) {
        private static final Codec<StoredFlightPlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("assembler").forGetter(StoredFlightPlan::assembler),
                FlightPlan.CODEC.fieldOf("plan").forGetter(StoredFlightPlan::plan)
        ).apply(instance, StoredFlightPlan::new));
    }

    /** Stable segment identity for plans applied to another rocket built at the same relative positions. */
    public record SegmentRef(BlockPos anchor) {
        public static final Codec<SegmentRef> CODEC = BlockPos.CODEC.xmap(SegmentRef::new, SegmentRef::anchor);

        public static SegmentRef of(StaticRocketSegment segment) {
            var anchor = segment.blocks().stream().map(StaticRocketSegment.BlockData::relativePos)
                    .min(Comparator.comparingInt((BlockPos pos) -> pos.getY())
                            .thenComparingInt(pos -> pos.getX())
                            .thenComparingInt(pos -> pos.getZ()))
                    .orElse(BlockPos.ZERO);
            return new SegmentRef(anchor);
        }
    }

    /** Configuration follows the segment's relative anchor so reusable plans still match an identical rocket. */
    public record SegmentConfiguration(SegmentRef segment, String name, boolean booster,
                                       List<Integer> engineStages) {

        public static final Codec<SegmentConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SegmentRef.CODEC.fieldOf("segment").forGetter(SegmentConfiguration::segment),
                Codec.STRING.optionalFieldOf("name", "").forGetter(SegmentConfiguration::name),
                Codec.BOOL.optionalFieldOf("booster", false).forGetter(SegmentConfiguration::booster),
                Codec.INT.listOf().fieldOf("engine_stages").forGetter(SegmentConfiguration::engineStages)
        ).apply(instance, SegmentConfiguration::new));

        public SegmentConfiguration {
            engineStages = List.copyOf(engineStages);
        }

        public boolean usesEnginesDuring(int stage) {
            return engineStages.contains(stage);
        }

        public int lastEngineStage() {
            return engineStages.stream().mapToInt(Integer::intValue).max().orElse(0);
        }

        public SegmentConfiguration withName(String newName) {
            return new SegmentConfiguration(segment, newName, booster, engineStages);
        }

        public SegmentConfiguration withBooster(boolean newBooster) {
            return new SegmentConfiguration(segment, name, newBooster, engineStages);
        }

        public SegmentConfiguration withEngineStages(List<Integer> newEngineStages) {
            return new SegmentConfiguration(segment, name, booster, newEngineStages);
        }
    }

    public record FlightPlan(List<FlightPlanBranch> branches, List<SegmentConfiguration> segmentConfigurations,
                             String name) {

        public static final Codec<FlightPlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FlightPlanBranch.CODEC.listOf().fieldOf("branches").forGetter(FlightPlan::branches),
                SegmentConfiguration.CODEC.listOf().optionalFieldOf("segments", List.of())
                        .forGetter(FlightPlan::segmentConfigurations),
                Codec.STRING.optionalFieldOf("name", "").forGetter(FlightPlan::name)
        ).apply(instance, FlightPlan::new));

        public FlightPlan {
            branches = List.copyOf(branches);
            segmentConfigurations = List.copyOf(segmentConfigurations);
            name = name == null ? "" : name;
        }

        public FlightPlan(List<FlightPlanBranch> branches, List<SegmentConfiguration> segmentConfigurations) {
            this(branches, segmentConfigurations, "");
        }

        public static FlightPlan empty() {
            return new FlightPlan(List.of(FlightPlanBranch.root()), List.of(), "");
        }

        public FlightPlanBranch root() {
            return branches.stream().filter(FlightPlanBranch::isRoot).findFirst()
                    .orElseGet(FlightPlanBranch::root);
        }

        public FlightPlan withBranches(List<FlightPlanBranch> newBranches) {
            return new FlightPlan(newBranches, segmentConfigurations, name);
        }

        public FlightPlan withSegmentConfigurations(List<SegmentConfiguration> newConfigurations) {
            return new FlightPlan(branches, newConfigurations, name);
        }

        public FlightPlan withName(String newName) {
            return new FlightPlan(branches, segmentConfigurations, newName);
        }

        public SegmentConfiguration configurationFor(SegmentRef segment) {
            return segmentConfigurations.stream().filter(configuration -> configuration.segment().equals(segment))
                    .findFirst().orElse(new SegmentConfiguration(segment, "", false, List.of(1)));
        }
    }

    /** Branches stay flat because this makes timeline editing and network serialization easy to follow. */
    public record FlightPlanBranch(UUID id, UUID parentSeparationAction, List<FlightPlanAction> actions) {
        public static final UUID NO_PARENT = new UUID(0, 1);

        public static final Codec<FlightPlanBranch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(FlightPlanBranch::id),
                UUIDUtil.STRING_CODEC.fieldOf("parent_action").forGetter(FlightPlanBranch::parentSeparationAction),
                FlightPlanAction.CODEC.listOf().fieldOf("actions").forGetter(FlightPlanBranch::actions)
        ).apply(instance, FlightPlanBranch::new));

        public FlightPlanBranch {
            actions = List.copyOf(actions);
        }

        public static FlightPlanBranch root() {
            return new FlightPlanBranch(UUID.randomUUID(), NO_PARENT, List.of());
        }

        public boolean isRoot() {
            return parentSeparationAction.equals(NO_PARENT);
        }

        public FlightPlanBranch withActions(List<FlightPlanAction> newActions) {
            return new FlightPlanBranch(id, parentSeparationAction, newActions);
        }
    }

    /** A navigation action blocks its branch until the automatically calculated transfer is complete. */
    public record FlightPlanAction(UUID id, ActionType type, List<SegmentRef> segments,
                                   UUID targetId, OrbitBand orbit,
                                   ArrivalVelocityMode velocityMode, int targetVelocity, int maxSpeed,
                                   int landingX, int landingZ, int landingOffsetX, int landingOffsetZ,
                                   List<ActionAddon> addons, ServiceSettings service) {
        public FlightPlanAction(UUID id, ActionType type, List<SegmentRef> segments, UUID targetId, OrbitBand orbit,
                ArrivalVelocityMode mode, int velocity, int speed, int x, int z, int ox, int oz, List<ActionAddon> addons) {
            this(id, type, segments, targetId, orbit, mode, velocity, speed, x, z, ox, oz, addons, ServiceSettings.DEFAULT);
        }
        public static final UUID NO_TARGET = new UUID(0, 0);

        public static final Codec<FlightPlanAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(FlightPlanAction::id),
                ActionType.CODEC.fieldOf("type").forGetter(FlightPlanAction::type),
                SegmentRef.CODEC.listOf().fieldOf("segments").forGetter(FlightPlanAction::segments),
                UUIDUtil.STRING_CODEC.fieldOf("target").forGetter(FlightPlanAction::targetId),
                OrbitBand.CODEC.fieldOf("orbit").forGetter(FlightPlanAction::orbit),
                ArrivalVelocityMode.CODEC.fieldOf("arrival_mode").forGetter(FlightPlanAction::velocityMode),
                Codec.INT.fieldOf("arrival_speed").forGetter(FlightPlanAction::targetVelocity),
                Codec.INT.optionalFieldOf("max_speed", 0).forGetter(FlightPlanAction::maxSpeed),
                Codec.INT.optionalFieldOf("landing_x", 0).forGetter(FlightPlanAction::landingX),
                Codec.INT.optionalFieldOf("landing_z", 0).forGetter(FlightPlanAction::landingZ),
                Codec.INT.optionalFieldOf("landing_offset_x", 0).forGetter(FlightPlanAction::landingOffsetX),
                Codec.INT.optionalFieldOf("landing_offset_z", 0).forGetter(FlightPlanAction::landingOffsetZ),
                ActionAddon.CODEC.listOf().optionalFieldOf("addons", List.of()).forGetter(FlightPlanAction::addons),
                ServiceSettings.CODEC.optionalFieldOf("service", ServiceSettings.DEFAULT).forGetter(FlightPlanAction::service)
        ).apply(instance, FlightPlanAction::new));

        public FlightPlanAction {
            segments = List.copyOf(segments);
            addons = List.copyOf(addons);
        }

        public static FlightPlanAction create(ActionType type) {
            return new FlightPlanAction(UUID.randomUUID(), type, List.of(), NO_TARGET, OrbitBand.LOW,
                    ArrivalVelocityMode.ZERO, 0, 0, 0, 0, 0, 0, List.of());
        }

        public static FlightPlanAction disconnectBooster(UUID id, SegmentRef segment, UUID navigationAction) {
            return new FlightPlanAction(id, ActionType.DISCONNECT_BOOSTER, List.of(segment),
                    navigationAction, OrbitBand.LOW, ArrivalVelocityMode.ZERO, 0, 0, 0, 0, 0, 0, List.of());
        }

        public FlightPlanAction withType(ActionType newType) {
            return new FlightPlanAction(id, newType, List.of(), NO_TARGET, OrbitBand.LOW,
                    ArrivalVelocityMode.ZERO, 0, 0, 0, 0, 0, 0, List.of());
        }

        public FlightPlanAction withTarget(UUID target) {
            return new FlightPlanAction(id, type, segments, target, orbit, velocityMode, targetVelocity, maxSpeed,
                    landingX, landingZ, landingOffsetX, landingOffsetZ, addons, service);
        }

        public FlightPlanAction withOrbit(OrbitBand newOrbit) {
            return new FlightPlanAction(id, type, segments, targetId, newOrbit, velocityMode, targetVelocity, maxSpeed,
                    landingX, landingZ, landingOffsetX, landingOffsetZ, addons, service);
        }

        public FlightPlanAction withVelocity(ArrivalVelocityMode newMode, int newVelocity) {
            return new FlightPlanAction(id, type, segments, targetId, orbit, newMode, newVelocity, maxSpeed,
                    landingX, landingZ, landingOffsetX, landingOffsetZ, addons, service);
        }

        public FlightPlanAction withSegments(List<SegmentRef> newSegments) {
            return new FlightPlanAction(id, type, newSegments, targetId, orbit, velocityMode, targetVelocity, maxSpeed,
                    landingX, landingZ, landingOffsetX, landingOffsetZ, addons, service);
        }

        /** Zero means unrestricted cruise speed. Arrival velocity remains a separate constraint. */
        public FlightPlanAction withMaxSpeed(int speed) {
            return new FlightPlanAction(id, type, segments, targetId, orbit, velocityMode, targetVelocity, speed,
                    landingX, landingZ, landingOffsetX, landingOffsetZ, addons, service);
        }

        public FlightPlanAction withLanding(int x, int z, int offsetX, int offsetZ) {
            return new FlightPlanAction(id, type, segments, targetId, orbit, velocityMode, targetVelocity, maxSpeed,
                    x, z, offsetX, offsetZ, addons, service);
        }

        public FlightPlanAction withAddons(List<ActionAddon> newAddons) {
            return new FlightPlanAction(id, type, segments, targetId, orbit, velocityMode, targetVelocity, maxSpeed,
                    landingX, landingZ, landingOffsetX, landingOffsetZ, newAddons, service);
        }

        public FlightPlanAction withService(ServiceSettings settings) {
            return new FlightPlanAction(id, type, segments, targetId, orbit, velocityMode, targetVelocity, maxSpeed,
                    landingX, landingZ, landingOffsetX, landingOffsetZ, addons, settings);
        }

        public boolean isGenerated() {
            return type == ActionType.DISCONNECT_BOOSTER;
        }
    }

    public record ServiceSettings(int durationTicks, boolean untilPrecise, int timeoutTicks, int slot) {
        public static final ServiceSettings DEFAULT = new ServiceSettings(SpaceBalance.DAY, false, 0, -1);
        public static final Codec<ServiceSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 24_000_000).fieldOf("duration").forGetter(ServiceSettings::durationTicks),
                Codec.BOOL.fieldOf("until_precise").forGetter(ServiceSettings::untilPrecise),
                Codec.intRange(0, 24_000_000).fieldOf("timeout").forGetter(ServiceSettings::timeoutTicks),
                Codec.intRange(-1, 35).fieldOf("slot").forGetter(ServiceSettings::slot)
        ).apply(i, ServiceSettings::new));
    }

    public record ActionAddon(UUID id, ActionAddonType type, int value) {
        public static final Codec<ActionAddon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(ActionAddon::id),
                ActionAddonType.CODEC.fieldOf("type").forGetter(ActionAddon::type),
                Codec.INT.fieldOf("value").forGetter(ActionAddon::value)
        ).apply(instance, ActionAddon::new));

        public static ActionAddon create(ActionAddonType type) {
            return new ActionAddon(UUID.randomUUID(), type, type.defaultValue());
        }

        public ActionAddon withType(ActionAddonType newType) {
            return new ActionAddon(id, newType, newType.defaultValue());
        }

        public ActionAddon withValue(int newValue) {
            return new ActionAddon(id, type, newValue);
        }
    }

    public enum ActionAddonType implements StringRepresentable {
        DISTANCE_FROM_TARGET(50_000),
        TIME_BEFORE_ARRIVAL(120),
        DESIRED_UNCERTAINTY(256),
        LOW_RF(10),
        LOW_FUEL(10);

        public static final Codec<ActionAddonType> CODEC = StringRepresentable.fromEnum(ActionAddonType::values);
        private final int defaultValue;

        ActionAddonType(int defaultValue) {
            this.defaultValue = defaultValue;
        }

        public int defaultValue() {
            return defaultValue;
        }

        public boolean isNavigationCondition() {
            return this == DISTANCE_FROM_TARGET || this == TIME_BEFORE_ARRIVAL || this == DESIRED_UNCERTAINTY;
        }

        public boolean isMaintainPositionCondition() {
            return !isNavigationCondition();
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum ActionType implements StringRepresentable {
        SCAN,
        TRANSMIT_INFORMATION,
        RELAY,
        NAVIGATE_TO,
        CONNECT_ASTEROID,
        DECOUPLE,
        DISCONNECT_BOOSTER,
        MAINTAIN_POSITION,
        DISCARD_CRAFT;

        public static final Codec<ActionType> CODEC = StringRepresentable.fromEnum(ActionType::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum OrbitBand implements StringRepresentable {
        SURFACE(0),
        TIGHT(1_000),
        LOW(10_000),
        MEDIUM(30_000),
        HIGH(60_000);

        public static final Codec<OrbitBand> CODEC = StringRepresentable.fromEnum(OrbitBand::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        private final double altitude;

        OrbitBand(double altitude) {
            this.altitude = altitude;
        }

        public double altitude() {
            return altitude;
        }
    }

    public enum ArrivalVelocityMode implements StringRepresentable {
        ZERO,
        MAXIMUM,
        CUSTOM;

        public static final Codec<ArrivalVelocityMode> CODEC = StringRepresentable.fromEnum(ArrivalVelocityMode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
