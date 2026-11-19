package rearth.oritech.spaceage.simulation.survey;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record EarthScan(int x, int z, int radius, Target target) {

    public static final Identifier MAP_MODEL = OritechSpaceAge.id("earth_survey_map");
    private static final int MIN_RADIUS = 64;
    private static final int MAX_RADIUS = 2048;
    public static final int PREVIEW_SIZE = 16;
    private static final int MAX_STRUCTURE_CHECKS = 32;
    private static final int MAX_CHUNK_CHECKS = 8;
    public static final Codec<EarthScan> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("x").forGetter(EarthScan::x), Codec.INT.fieldOf("z").forGetter(EarthScan::z),
            Codec.INT.optionalFieldOf("radius", 0).forGetter(EarthScan::radius), Target.CODEC.fieldOf("target").forGetter(EarthScan::target)
    ).apply(i, EarthScan::new));

    public EarthScan {

        x = Math.clamp(x, -Level.MAX_LEVEL_SIZE, Level.MAX_LEVEL_SIZE);
        z = Math.clamp(z, -Level.MAX_LEVEL_SIZE, Level.MAX_LEVEL_SIZE);
    }

    public EarthScan(int x, int z, Target target) {

        this(x, z, 0, target);
    }

    public Optional<EarthScan> atPosition(double craftX, double craftY) {

        // Waypoints and interrupted transfers can leave the craft within range of Earth too.
        var altitude = SpaceObjects.earthAltitude(craftX, craftY);
        if (!(altitude >= 1 && altitude <= SpaceBalance.SCAN_RANGE)) return Optional.empty();
        var blocks = Math.round(altitude / SpaceBalance.EARTH_SCAN_METRES_PER_BLOCK);
        return Optional.of(new EarthScan(x, z, (int) Math.clamp(blocks, MIN_RADIUS, MAX_RADIUS), target));
    }

    public long energyCost() {

        return radius * SpaceBalance.EARTH_SCAN_RF_PER_BLOCK;
    }

    public static List<Identifier> biomeIds(ServerLevel level) {

        // Use the Overworld generator so the picker only offers biomes it can actually find.
        return level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().stream()
                .flatMap(biome -> biome.unwrapKey().stream()).map(ResourceKey::identifier).sorted().toList();
    }

    public Report run(ServerLevel level, long tick) {

        var result = target.biome ? new Search(findBiome(level), false) : findStructure(level);
        return new Report(UUID.randomUUID(), this, Optional.ofNullable(result.position), result.limited, tick, preview(level));
    }

    private BlockPos findBiome(ServerLevel level) {

        var key = ResourceKey.create(Registries.BIOME, target.id());
        var generator = level.getChunkSource().getGenerator();
        if (generator.getBiomeSource().possibleBiomes().stream().noneMatch(biome -> biome.is(key))) return null;
        // A coarser grid on large surveys keeps the number of samples roughly constant.
        var step = Math.max(16, (radius + 31) / 32);
        var verticalStep = Math.max(32, level.getHeight() / 16);
        var heights = Mth.outFromOrigin(level.getSeaLevel(), level.getMinY(), level.getMaxY(), verticalStep).toArray();
        var limit = (long) radius * radius;
        var columns = new ArrayList<BlockPos>();
        for (var dx = -radius / step * step; dx <= radius; dx += step) {
            for (var dz = -radius / step * step; dz <= radius; dz += step) {
                var distance = (long) dx * dx + (long) dz * dz;
                if (distance <= limit && level.getWorldBorder().isWithinBounds(x + dx, z + dz))
                    columns.add(new BlockPos(x + dx, 0, z + dz));
            }
        }
        columns.sort(Comparator.comparingLong(this::distanceSquared));
        for (var column : columns) {
            for (var y : heights) {
                if (level.getUncachedNoiseBiome(QuartPos.fromBlock(column.getX()), QuartPos.fromBlock(y),
                        QuartPos.fromBlock(column.getZ())).is(key)) return column.atY(y);
            }
        }
        return null;
    }

    private Search findStructure(ServerLevel level) {

        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var structures = target.equals(Target.VILLAGE) ? registry.get(StructureTags.VILLAGE).stream().flatMap(HolderSet::stream).toList()
                : registry.get(ResourceKey.create(Registries.STRUCTURE, target.id())).stream().toList();
        var state = level.getChunkSource().getGeneratorState();
        var candidates = new ArrayList<Candidate>();
        var minX = SectionPos.blockToSectionCoord(x - radius);
        var maxX = SectionPos.blockToSectionCoord(x + radius);
        var minZ = SectionPos.blockToSectionCoord(z - radius);
        var maxZ = SectionPos.blockToSectionCoord(z + radius);
        // Check Minecraft's candidate locations instead of searching or generating every chunk in the area.
        for (var structure : structures) {
            for (var placement : state.getPlacementsForStructure(structure)) {
                if (placement instanceof RandomSpreadStructurePlacement spread) {
                    var spacing = spread.spacing();
                    for (var cx = Math.floorDiv(minX, spacing) * spacing; cx <= maxX; cx += spacing) {
                        for (var cz = Math.floorDiv(minZ, spacing) * spacing; cz <= maxZ; cz += spacing) {
                            var chunk = spread.getPotentialStructureChunk(state.getLevelSeed(), cx, cz);
                            addCandidate(level, candidates, structure, spread, chunk);
                        }
                    }
                } else if (placement instanceof ConcentricRingsStructurePlacement rings) {
                    var chunks = state.getRingPositionsFor(rings);
                    if (chunks != null) for (var chunk : chunks) addCandidate(level, candidates, structure, rings, chunk);
                }
            }
        }
        candidates.sort(Comparator.comparingLong(candidate -> distanceSquared(candidate.position)));
        var manager = level.structureManager();
        var structureChecks = 0;
        var chunkChecks = 0;
        for (var candidate : candidates) {
            if (structureChecks++ == MAX_STRUCTURE_CHECKS) return new Search(null, true);
            var structure = candidate.structure.value();
            var chunk = candidate.chunk;
            var check = manager.checkStructurePresence(chunk, structure, candidate.placement, false);
            if (check == StructureCheckResult.START_PRESENT) return new Search(candidate.position, false);
            if (check != StructureCheckResult.CHUNK_LOAD_NEEDED) continue;
            // Some starts need a chunk lookup. Stop before a rare target can monopolize the server tick.
            if (chunkChecks++ == MAX_CHUNK_CHECKS) return new Search(null, true);
            var access = level.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS);
            var start = manager.getStartForStructure(SectionPos.bottomOf(access), structure, access);
            if (start != null && start.isValid()) return new Search(candidate.position, false);
        }
        return new Search(null, false);
    }

    private void addCandidate(ServerLevel level, List<Candidate> candidates, Holder<Structure> structure,
                              StructurePlacement placement, ChunkPos chunk) {

        var pos = placement.getLocatePos(chunk);
        if (distanceSquared(pos) <= (long) radius * radius && level.getWorldBorder().isWithinBounds(pos))
            candidates.add(new Candidate(structure, placement, chunk, pos));
    }

    private long distanceSquared(BlockPos pos) {

        var dx = (long) pos.getX() - x;
        var dz = (long) pos.getZ() - z;
        return dx * dx + dz * dz;
    }

    private String preview(ServerLevel level) {

        // Save a tiny biome preview with the report so viewing it never runs another world scan.
        var pixels = new StringBuilder(PREVIEW_SIZE * PREVIEW_SIZE);
        for (var row = 0; row < PREVIEW_SIZE; row++) {
            for (var column = 0; column < PREVIEW_SIZE; column++) {
                var sampleX = x - radius + (2 * column + 1) * radius / PREVIEW_SIZE;
                var sampleZ = z - radius + (2 * row + 1) * radius / PREVIEW_SIZE;
                var biome = level.getUncachedNoiseBiome(QuartPos.fromBlock(sampleX), QuartPos.fromBlock(level.getSeaLevel()),
                        QuartPos.fromBlock(sampleZ));
                pixels.append(biome.is(BiomeTags.WATER_ON_MAP_OUTLINES) ? 'w' : biome.is(BiomeTags.IS_MOUNTAIN) ? 'm' : 'l');
            }
        }
        return pixels.toString();
    }

    public record Target(Identifier id, boolean biome) {

        public static final Target VILLAGE = structure("village");
        public static final Target PLAINS = new Target(Identifier.withDefaultNamespace("plains"), true);
        public static final List<Target> STRUCTURES = List.of(VILLAGE, structure("monument"), structure("mansion"),
                structure("desert_pyramid"), structure("shipwreck"), structure("trial_chambers"), structure("ancient_city"));
        private static final Codec<Target> IDENTIFIED = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("id").forGetter(Target::id), Codec.BOOL.fieldOf("biome").forGetter(Target::biome)
        ).apply(i, Target::new));
        // Old cards and reports used bare preset names. Read them without losing existing surveys.
        public static final Codec<Target> CODEC = Codec.withAlternative(IDENTIFIED, Identifier.CODEC, id ->
                STRUCTURES.stream().filter(target -> target.id.equals(id)).findFirst().orElseGet(() -> new Target(id, true)));

        private static Target structure(String path) {

            return new Target(Identifier.withDefaultNamespace(path), false);
        }

        public Component label() {

            if (!biome) return Component.translatable("screen.oritech_space_age.earth_survey.target." + id.getPath());
            return Component.translatableWithFallback(id.toLanguageKey("biome"), id.getPath().replace('_', ' '));
        }
    }

    public record Report(UUID id, EarthScan scan, Optional<BlockPos> position, boolean limited, long tick, String preview) {

        public static final Codec<Report> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Report::id), EarthScan.CODEC.fieldOf("scan").forGetter(Report::scan),
                BlockPos.CODEC.optionalFieldOf("position").forGetter(Report::position), Codec.BOOL.fieldOf("limited").forGetter(Report::limited),
                Codec.LONG.fieldOf("tick").forGetter(Report::tick), Codec.STRING.fieldOf("preview").forGetter(Report::preview)
        ).apply(i, Report::new));

        public char previewAt(int column, int row) {

            if (column < 0 || column >= PREVIEW_SIZE || row < 0 || row >= PREVIEW_SIZE) return ' ';
            var index = row * PREVIEW_SIZE + column;
            return index < preview.length() ? preview.charAt(index) : ' ';
        }

        public ItemStack createMap(ServerLevel level) {

            var pos = position.orElseThrow();
            byte scale = 0;
            while (scale < 4 && (64 << scale) < scan.radius) scale++;
            var stack = MapItem.create(level, pos.getX(), pos.getZ(), scale, true, true);
            var map = MapItem.getSavedData(stack, level);
            if (map != null) {
                // Fill the map from the saved preview so exporting never scans the world again.
                var pixelSize = 1 << scale;
                for (var row = 0; row < 128; row++) {
                    var z = map.centerZ + (row - 64) * pixelSize;
                    var previewRow = previewCoordinate(z - scan.z);
                    for (var column = 0; column < 128; column++) {
                        var x = map.centerX + (column - 64) * pixelSize;
                        var color = switch (previewAt(previewCoordinate(x - scan.x), previewRow)) {
                            case 'w' -> MapColor.TERRACOTTA_BLUE;
                            case 'm' -> MapColor.COLOR_LIGHT_BLUE;
                            case 'l' -> MapColor.COLOR_CYAN;
                            default -> MapColor.NONE;
                        };
                        if (color != MapColor.NONE) map.setColor(column, row, color.getPackedId(MapColor.Brightness.NORMAL));
                    }
                }
            }
            MapItemSavedData.addTargetDecoration(stack, pos, "survey", MapDecorationTypes.RED_X);
            // Keep the vanilla map behavior while giving this stack a survey icon.
            stack.set(DataComponents.ITEM_MODEL, MAP_MODEL);
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.oritech_space_age.earth_survey_map",
                    scan.target.label()));
            return stack;
        }

        private int previewCoordinate(int offset) {

            return Math.floorDiv((offset + scan.radius) * PREVIEW_SIZE, scan.radius * 2);
        }
    }

    private record Candidate(Holder<Structure> structure, StructurePlacement placement, ChunkPos chunk, BlockPos position) {}

    private record Search(BlockPos position, boolean limited) {}
}
