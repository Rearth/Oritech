package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.BeamApertureBlockEntity;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Physical delivery and heating run once per world tick, never once per creative simulation step. */
public final class SurfaceBeamData extends SavedData {

    public static final Codec<SurfaceBeamData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).xmap(SurfaceBeamData::new,
            data -> {
                var result = new HashMap<String, Double>();
                data.heat.forEach((chunk, value) -> result.put(Long.toString(chunk), value));
                return result;
            });
    private static final SavedDataType<SurfaceBeamData> TYPE = new SavedDataType<>(OritechSpaceAge.id("surface_beam_heat"),
            () -> new SurfaceBeamData(Map.of()), CODEC, null);
    public final Map<BlockPos, BeamApertureBlockEntity> receivers = new HashMap<>();
    private final Map<Long, Double> heat = new HashMap<>();
    private final Map<BlockPos, Double> receivedThisTick = new HashMap<>();

    private SurfaceBeamData(Map<String, Double> saved) {

        saved.forEach((key, value) -> heat.put(Long.parseLong(key), value));
    }

    public static SurfaceBeamData get(ServerLevel level) {

        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public double temperature(BlockPos pos) {

        return heat.getOrDefault(ChunkPos.pack(pos), 0d);
    }

    public static boolean loaded(ServerLevel level, int x, int z, double radius) {

        if (radius > 512) return false;
        for (int cx = (int) Math.floor((x - radius) / 16); cx <= (int) Math.floor((x + radius) / 16); cx++)
            for (int cz = (int) Math.floor((z - radius) / 16); cz <= (int) Math.floor((z + radius) / 16); cz++)
                if (!level.hasChunk(cx, cz)) return false;
        return true;
    }

    public BeamReception deliver(ServerLevel level, int x, int z, double radius, double incident) {

        if (!loaded(level, x, z, radius)) return BeamReception.EMPTY;
        var footprint = Math.PI * radius * radius;
        if (incident > 0 && level.getGameTime() % 20 == 0) {
            for (int part = 0; part < 16; part++) {
                var angle = part * Math.PI * 2 / 16;
                var px = (int) Math.floor(x + Math.cos(angle) * radius);
                var pz = (int) Math.floor(z + Math.sin(angle) * radius);
                var surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(px, 0, pz));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px + .5, surface.getY() + .2, pz + .5, 1, 0, 0, 0, 0);
            }
        }
        var claimed = new HashSet<BlockPos>();
        var columns = new HashSet<Long>();
        var reception = BeamReception.EMPTY;
        for (var pos : receivers.keySet().stream().sorted().toList()) {
            var receiver = receivers.get(pos);
            if (receiver.isRemoved() || !level.hasChunkAt(pos)
                    || receiver.getBlockState().getValue(BlockStateProperties.FACING) != Direction.UP) continue;
            var array = receiver.array();
            if (!array.valid() || array.cells().stream().anyMatch(claimed::contains)) continue;
            claimed.addAll(array.cells());
            var area = array.cells().stream().filter(cell -> columns.add(BlockPos.asLong(cell.getX(), 0, cell.getZ())))
                    .mapToDouble(cell -> BeamAperture.cellArea(cell.getX() - x, cell.getZ() - z, radius)).sum();
            var intercepted = Math.min(Math.max(0, incident - reception.collected()), incident * BeamAperture.collectionFraction(area, radius));
            var available = Math.max(0, array.throughput() - receivedThisTick.getOrDefault(pos, 0d));
            var stored = receiver.receive(Math.min(intercepted, available));
            receivedThisTick.merge(pos, stored, Double::sum);
            var detail = BeamReception.limited(incident, intercepted, available, stored, radius);
            if (area > 0) receiver.recordBeam(detail);
            // Incident is shared by all arrays: count the footprint once, not once per receiver.
            reception = reception.plus(new BeamReception(0, detail.collected(), detail.capacityLoss(), detail.storageLoss(), stored, radius));
        }
        var overflow = Math.max(0, incident - reception.accepted());
        var chunks = new HashSet<Long>();
        for (int cx = (int) Math.floor((x - radius) / 16); cx <= (int) Math.floor((x + radius) / 16); cx++)
            for (int cz = (int) Math.floor((z - radius) / 16); cz <= (int) Math.floor((z + radius) / 16); cz++)
                chunks.add(ChunkPos.pack(cx, cz));
        for (var chunk : chunks) heat.merge(chunk, overflow / Math.max(1, footprint) / 100_000, Double::sum);
        setDirty();
        return new BeamReception(incident, reception.collected(), reception.capacityLoss(), reception.storageLoss(), reception.accepted(), radius);
    }

    public void tick(ServerLevel level) {

        receivedThisTick.clear();
        receivers.entrySet().removeIf(entry -> entry.getValue().isRemoved() || !level.hasChunkAt(entry.getKey()));
        var effects = 0;
        var keys = heat.keySet().stream().sorted().toList();
        var offset = keys.isEmpty() ? 0 : (int) (level.getGameTime() / 20 * 8 % keys.size());
        for (int index = 0; index < keys.size(); index++) {
            var key = keys.get((index + offset) % keys.size());
            var chunk = ChunkPos.unpack(key);
            var temperature = Math.max(0, heat.get(key) - .002);
            if (temperature == 0) heat.remove(key);
            else heat.put(key, temperature);
            if (!level.hasChunk(chunk.x(), chunk.z()) || temperature < 1 || effects >= 8 || level.getGameTime() % 20 != 0) continue;
            effects++;
            var x = chunk.getMinBlockX() + level.getRandom().nextInt(16);
            var z = chunk.getMinBlockZ() + level.getRandom().nextInt(16);
            var pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).below();
            level.sendParticles(ParticleTypes.SMOKE, x + .5, pos.getY() + 1.1, z + .5, 2, .2, .1, .2, .01);
            if (temperature < 5) continue;
            for (var entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(chunk.getMinBlockX(), pos.getY(), chunk.getMinBlockZ(),
                    chunk.getMaxBlockX() + 1, level.getMaxY(), chunk.getMaxBlockZ() + 1)))
                if (level.canSeeSky(entity.blockPosition())) entity.hurtServer(level, level.damageSources().onFire(), 2);
            var state = level.getBlockState(pos);
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            else if (temperature >= 10 || state.is(Blocks.GRASS_BLOCK)) {
                var result = state.getBlock().builtInRegistryHolder().getData(SpaceAgeDataMaps.BEAM_SCORCHING);
                if (result != null) level.setBlockAndUpdate(pos, result);
            }
        }
        if (!heat.isEmpty()) setDirty();
    }
}
