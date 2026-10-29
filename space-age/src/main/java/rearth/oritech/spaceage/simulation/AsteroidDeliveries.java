package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.SpaceObjects.AsteroidMaterial;

import java.util.ArrayList;
import java.util.List;

/**
 * One overworld queue, advanced only on physical ticks (never on mission time warp).
 */
public final class AsteroidDeliveries extends SavedData {

    public static final int BLOCKS_PER_TICK = 512;
    public static final Codec<AsteroidDeliveries> CODEC = Job.CODEC.listOf().xmap(AsteroidDeliveries::new, data -> data.jobs);
    private static final SavedDataType<AsteroidDeliveries> TYPE = new SavedDataType<>(
            OritechSpaceAge.id("asteroid_deliveries"), AsteroidDeliveries::new, CODEC, null);
    final List<Job> jobs = new ArrayList<>();
    private int nextJob;

    public AsteroidDeliveries() {
    }

    private AsteroidDeliveries(List<Job> jobs) {

        this.jobs.addAll(jobs);
    }

    public static AsteroidDeliveries get(ServerLevel level) {

        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    static int landingBottom(int surface, Integer rocketTop) {

        return rocketTop == null ? surface : Math.max(surface, rocketTop + 3);
    }

    public void enqueue(ServerLevel level, AsteroidImpactRules.ImpactPrediction impact, boolean explode, Integer rocketTop) {

        var x = impact.landingX();
        var z = impact.landingZ();
        var y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (explode && impact.craterRadiusBlocks() > 0)
            level.explode(null, x + .5, y, z + .5, Math.min(12, impact.craterRadiusBlocks()), Level.ExplosionInteraction.BLOCK);
        // This query is deliberately after the explosion. Controlled deliveries instead clear the restored rocket.
        y = landingBottom(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), rocketTop);
        if (impact.recoverableMaterials().isEmpty()) return;
        jobs.add(new Job(new BlockPos(x, y, z), level.getRandom().nextLong(), impact.recoverableMaterials(),
                impact.recoverableMaterials(), 0, 0, 0, Math.max(1, level.getMaxY() - y + 1)));
        setDirty();
    }

    public void tick(ServerLevel level) {

        if (jobs.isEmpty()) return;
        var deadline = System.nanoTime() + 4_000_000;
        var stalled = 0;
        for (var work = 0; work < BLOCKS_PER_TICK && !jobs.isEmpty() && System.nanoTime() < deadline; work++) {
            nextJob = Math.floorMod(nextJob, jobs.size());
            var job = jobs.get(nextJob);
            if (job.step(level)) {
                stalled = 0;
                setDirty();
                if (job.finished()) jobs.remove(nextJob);
                else nextJob++;
            } else {
                nextJob++;
                if (++stalled >= jobs.size()) break;
            }
        }
    }

    public static final class Job {

        public static final Codec<Job> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("origin").forGetter(job -> job.origin),
                Codec.LONG.fieldOf("seed").forGetter(job -> job.seed),
                AsteroidMaterial.CODEC.listOf().fieldOf("materials").forGetter(job -> job.materials),
                AsteroidMaterial.CODEC.listOf().fieldOf("remaining").forGetter(Job::remaining),
                Codec.INT.fieldOf("column").forGetter(job -> job.column),
                Codec.INT.fieldOf("height").forGetter(job -> job.height),
                Codec.INT.fieldOf("skipped").forGetter(job -> job.skipped),
                Codec.INT.fieldOf("height_limit").forGetter(job -> job.heightLimit)
        ).apply(i, Job::new));
        final BlockPos origin;
        final long seed;
        final List<AsteroidMaterial> materials;
        final int[] amounts;
        private final int volume;
        private final int heightLimit;
        private List<AsteroidShape.Column> columns;
        private int column;
        private int height;
        private int skipped;

        Job(BlockPos origin, long seed, List<AsteroidMaterial> materials, List<AsteroidMaterial> remaining,
            int column, int height, int skipped) {

            this(origin, seed, materials, remaining, column, height, skipped, Integer.MAX_VALUE);
        }

        Job(BlockPos origin, long seed, List<AsteroidMaterial> materials, List<AsteroidMaterial> remaining,
            int column, int height, int skipped, int heightLimit) {

            this.origin = origin;
            this.seed = seed;
            this.materials = List.copyOf(materials);
            volume = materials.stream().mapToInt(AsteroidMaterial::amount).sum();
            amounts = remaining.stream().mapToInt(AsteroidMaterial::amount).toArray();
            this.column = column;
            this.height = height;
            this.skipped = skipped;
            this.heightLimit = heightLimit;
        }

        List<AsteroidMaterial> remaining() {

            var result = new ArrayList<AsteroidMaterial>();
            for (var i = 0; i < materials.size(); i++)
                result.add(new AsteroidMaterial(materials.get(i).block(), amounts[i]));
            return result;
        }

        List<AsteroidShape.Column> columns() {

            if (columns == null) columns = AsteroidShape.columns(volume, seed, heightLimit);
            return columns;
        }

        boolean finished() {

            return column >= columns().size();
        }

        BlockPos position() {

            var c = columns().get(column);
            return origin.offset(c.x(), c.bottom() + height + skipped, c.z());
        }

        /**
         * Spatially coherent pockets use the original composition; exhaustion falls back to another remaining material.
         */
        int materialAt(BlockPos pos) {

            var hash = seed ^ Math.floorDiv(pos.getX(), 5) * 73856093L ^ Math.floorDiv(pos.getY(), 5) * 19349663L
                    ^ Math.floorDiv(pos.getZ(), 5) * 83492791L;
            hash = (hash ^ hash >>> 33) * 0xff51afd7ed558ccdL;
            hash = (hash ^ hash >>> 33) * 0xc4ceb9fe1a85ec53L;
            var choice = Math.floorMod(hash ^ hash >>> 33, volume);
            var selected = 0;
            while (selected < materials.size() - 1 && choice >= materials.get(selected).amount())
                choice -= materials.get(selected++).amount();
            while (amounts[selected] == 0) selected = (selected + 1) % amounts.length;
            return selected;
        }

        void placed(int material) {

            amounts[material]--;
            height++;
            if (height == columns().get(column).height()) {
                height = 0;
                skipped = 0;
                column++;
            }
        }

        boolean step(ServerLevel level) {

            if (finished()) return true;
            var pos = position();
            // Chunk queries never synchronously load or generate terrain. Resume when a player loads this column.
            if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos))
                return false;
            var existing = level.getBlockState(pos);
            if (existing.getDestroySpeed(level, pos) < 0 || existing.hasBlockEntity()) {
                skipped++;
                return true;
            }
            var material = materialAt(pos);
            var state = BuiltInRegistries.BLOCK.getValue(materials.get(material).block()).defaultBlockState();
            if (state.isAir()) return false;
            if (!existing.equals(state) && !level.setBlock(pos, state, Block.UPDATE_CLIENTS)) return false;
            placed(material);
            return true;
        }
    }
}
