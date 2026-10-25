package rearth.oritech.spaceage.simulation.rocket;

import com.google.common.hash.Hashing;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A rocket compacted by the addon splicer. Cube-sats only carry hardware and resources; the flight plan always
 * belongs to whatever launches them (the host rocket's plan or a particle launch ramp).
 *
 * @param fingerprint hash of the block layout. Launch ramp plans apply to every cube with the same layout.
 */
public record CubeSatData(String name, long fingerprint, List<Part> parts) {

    // Payload segments get plan anchors far outside any rocket, so references of host and cube never collide.
    private static final int PAYLOAD_ANCHOR_OFFSET = 1_000_000;
    private static final int PAYLOAD_ANCHOR_SPACING = 16;

    public static final Codec<CubeSatData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(CubeSatData::name),
            Codec.LONG.fieldOf("fingerprint").forGetter(CubeSatData::fingerprint),
            Part.CODEC.listOf().fieldOf("parts").forGetter(CubeSatData::parts)
    ).apply(i, CubeSatData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CubeSatData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public CubeSatData {

        parts = List.copyOf(parts);
    }

    public static CubeSatData of(ActiveRocketData rocket) {

        var parts = rocket.getOriginalSegments().entrySet().stream()
                .map(entry -> Part.of(entry.getValue(), rocket.getDynamicSegments().get(entry.getKey()))).toList();
        return new CubeSatData("", fingerprint(parts), parts);
    }

    /**
     * Fuel, RF and names don't change the layout, so a plan made for one cube works for every identical cube.
     */
    private static long fingerprint(List<Part> parts) {

        var cells = new TreeSet<String>();
        for (var part : parts) {
            part.structure().blocks().forEach(block -> cells.add(block.relativePos().toShortString() + "=" + block.state()));
            part.structure().originalCouplings().values().forEach(couplings -> couplings.forEach(coupling ->
                    cells.add(coupling.relativePos().toShortString() + "=" + coupling.state())));
        }
        return Hashing.sha256().hashString(String.join(";", cells), StandardCharsets.UTF_8).asLong();
    }

    public static BlockPos payloadAnchor(BlockPos payload, BlockPos anchor) {

        return anchor.offset(payload.multiply(PAYLOAD_ANCHOR_SPACING)).offset(PAYLOAD_ANCHOR_OFFSET, 0, 0);
    }

    public CubeSatData withName(String newName) {

        return new CubeSatData(newName, fingerprint, parts);
    }

    /**
     * Creates the rocket stored in this cube, with its captured resources.
     *
     * @param payload the cube's cell in a host rocket. Its segments then use plan anchors that cannot collide with the host.
     */
    public ActiveRocketData unfold(UUID rocketId, @Nullable BlockPos payload) {

        var ids = new HashMap<UUID, UUID>();
        parts.forEach(part -> ids.put(part.structure().segmentId(), UUID.nameUUIDFromBytes(
                (rocketId + "/" + part.structure().segmentId()).getBytes(StandardCharsets.UTF_8))));
        var structures = new HashMap<UUID, StaticRocketSegment>();
        var resources = new HashMap<UUID, DynamicRocketSegment>();
        for (var part : parts) {
            var structure = part.structure();
            var id = ids.get(structure.segmentId());
            var couplings = new HashMap<UUID, Set<StaticRocketSegment.CouplingData>>();
            structure.originalCouplings().forEach((other, cells) -> couplings.put(ids.get(other), cells));
            var anchor = payload == null ? structure.planAnchor() : payloadAnchor(payload, structure.planAnchor());
            structures.put(id, new StaticRocketSegment(id, structure.blocks(), couplings, structure.staticWeight(), structure.engineCount(),
                    structure.initialRF(), structure.initialFuel(), structure.payloadEnergyJoules(), anchor));
            var crafters = new HashMap<BlockPos, RocketControllerState>();
            structure.blocks().stream().filter(cell -> cell.state().is(SpaceAgeBlocks.VACUUM_CRAFTER) || cell.state().is(SpaceAgeBlocks.CARGO))
                    .forEach(cell -> crafters.put(cell.relativePos(), new RocketControllerState("", Optional.empty())));
            part.names().forEach(named -> crafters.put(named.pos(), new RocketControllerState(named.name(), Optional.empty())));
            resources.put(id, new DynamicRocketSegment(part.fuel(), part.rf(), part.fuelWeight(), couplings.keySet(), crafters,
                    part.fuelCapacity(), part.rfCapacity()));
        }
        return new ActiveRocketData(rocketId, structures, resources, null);
    }

    public int blockCount() {

        return positions().size();
    }

    /** Bounding box of all blocks, used for the splicer size limit and the miniature. */
    public Vec3i size() {

        var positions = positions();
        if (positions.isEmpty()) return Vec3i.ZERO;
        return new Vec3i(span(positions, 0), span(positions, 1), span(positions, 2));
    }

    // Both sides of a connection list the coupling between them.
    private Set<BlockPos> positions() {

        var positions = parts.stream().flatMap(part -> part.structure().blocks().stream()).map(StaticRocketSegment.BlockData::relativePos)
                .collect(Collectors.toCollection(HashSet::new));
        parts.forEach(part -> part.structure().originalCouplings().values().forEach(couplings ->
                couplings.forEach(coupling -> positions.add(coupling.relativePos()))));
        return positions;
    }

    private static int span(Set<BlockPos> positions, int axis) {

        var values = positions.stream().mapToInt(pos -> axis == 0 ? pos.getX() : axis == 1 ? pos.getY() : pos.getZ()).summaryStatistics();
        return values.getMax() - values.getMin() + 1;
    }

    /**
     * One captured segment with the resources it had when it was shrunk.
     */
    public record Part(StaticRocketSegment structure, long fuel, long rf, double fuelWeight, long fuelCapacity, long rfCapacity,
                       List<NamedCell> names) {

        public static final Codec<Part> CODEC = RecordCodecBuilder.create(i -> i.group(
                StaticRocketSegment.CODEC.fieldOf("structure").forGetter(Part::structure),
                Codec.LONG.fieldOf("fuel").forGetter(Part::fuel),
                Codec.LONG.fieldOf("rf").forGetter(Part::rf),
                Codec.DOUBLE.fieldOf("fuel_weight").forGetter(Part::fuelWeight),
                Codec.LONG.fieldOf("fuel_capacity").forGetter(Part::fuelCapacity),
                Codec.LONG.fieldOf("rf_capacity").forGetter(Part::rfCapacity),
                NamedCell.CODEC.listOf().optionalFieldOf("names", List.of()).forGetter(Part::names)
        ).apply(i, Part::new));

        public Part {

            names = List.copyOf(names);
        }

        static Part of(StaticRocketSegment structure, DynamicRocketSegment resources) {

            var names = resources.crafters.entrySet().stream().filter(entry -> !entry.getValue().name.isBlank())
                    .map(entry -> new NamedCell(entry.getKey(), entry.getValue().name)).toList();
            return new Part(structure, resources.availableFuelBurnTimeTicks, resources.availableRF, resources.currentFuelWeight,
                    resources.fuelCapacity, resources.rfCapacity, names);
        }
    }

    /** Controller names are explicit configuration and survive shrinking, like on launch. */
    public record NamedCell(BlockPos pos, String name) {

        public static final Codec<NamedCell> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("position").forGetter(NamedCell::pos),
                Codec.STRING.fieldOf("name").forGetter(NamedCell::name)
        ).apply(i, NamedCell::new));
    }
}
