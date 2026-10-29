package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.AsteroidDeliveries.Job;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules.ArrivalOutcome;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules.FragmentationMode;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules.ImpactPrediction;
import rearth.oritech.spaceage.simulation.DenseOres.Family;
import rearth.oritech.spaceage.simulation.SpaceObjects.AsteroidMaterial;

import java.util.List;
import java.util.Arrays;

/**
 * Real loot and chunk placement checks, including the same bounded queue used by live deliveries.
 */
public final class AsteroidGameTests {

    private AsteroidGameTests() {
    }

    public static void denseMining(GameTestHelper test) {

        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(1, 2, 1));
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var normal = new ItemStack(Items.DIAMOND_PICKAXE);
        var fortune = normal.copy();
        fortune.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), 3);
        var silk = normal.copy();
        silk.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        for (var family : Family.values()) {
            var state = family.block().defaultBlockState();
            test.assertTrue(normal.isCorrectToolForDrops(state), "Diamond pickaxe mines " + family);
            test.assertTrue(!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state), "Wood cannot harvest dense ores");
            test.assertTrue(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(state) != family.diamondTool,
                    "Each family has the intended tool tier: " + family);
            var plainTotal = 0;
            var fortuneTotal = 0;
            for (var i = 0; i < 48; i++) {
                var drops = Block.getDrops(state, level, pos, null, null, normal);
                test.assertTrue(drops.stream().allMatch(stack -> stack.is(family.output())), "Explicit mining output: " + family);
                var amount = drops.stream().mapToInt(ItemStack::getCount).sum();
                test.assertTrue(amount >= 30 && amount <= 60, "Base dense yield is 30–60");
                plainTotal += amount;
                fortuneTotal += Block.getDrops(state, level, pos, null, null, fortune).stream().mapToInt(ItemStack::getCount).sum();
            }
            test.assertTrue(fortuneTotal > plainTotal * 1.5, "Fortune increases dense yield: " + family);
            var drops = Block.getDrops(state, level, pos, null, null, silk);
            test.assertTrue(drops.size() == 1 && drops.getFirst().is(family.block().asItem()) && drops.getFirst().getCount() == 1,
                    "Silk Touch preserves one dense block");
        }
        test.succeed();
    }

    public static void landingAndProtection(GameTestHelper test) {

        var level = test.getLevel();
        var centre = test.absolutePos(new BlockPos(128, 2, 128));
        level.getChunk(centre.getX() >> 4, centre.getZ() >> 4);
        var top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre.getX(), centre.getZ());
        var materials = List.of(new AsteroidMaterial(Family.IRON.id(), 60));
        var impact = new ImpactPrediction(ArrivalOutcome.HEAVY_IMPACT, 300, 1e10, 5,
                FragmentationMode.NONE, List.of(), 0, materials, centre.getX(), centre.getZ());
        var deliveries = new AsteroidDeliveries();
        deliveries.enqueue(level, impact, true, null);
        var job = deliveries.jobs.getFirst();
        test.assertTrue(job.origin.getY() == level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre.getX(), centre.getZ()),
                "Crash placement starts on the post-explosion crater floor");
        test.assertTrue(job.origin.getY() < top, "The impact excavates the centre before placing its deposit");
        var controlled = new AsteroidDeliveries();
        controlled.enqueue(level, impact, false, top + 20);
        test.assertTrue(controlled.jobs.getFirst().origin.getY() >= top + 23, "Controlled placement clears rocket top");

        var blocked = job.position();
        level.getChunk(blocked.getX() >> 4, blocked.getZ() >> 4);
        level.setBlock(blocked, Blocks.BEDROCK.defaultBlockState(), 2);
        var chest = blocked.above();
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        test.assertTrue(job.step(level), "Protected block advances the position without consuming material");
        test.assertTrue(job.remaining().getFirst().amount() == 60, "Bedrock does not consume composition");
        test.assertTrue(job.step(level), "Block entity is protected too");
        test.assertTrue(level.getBlockState(chest).is(Blocks.CHEST), "Delivery preserves block entities");
        test.assertTrue(job.remaining().getFirst().amount() == 60, "Protected cells do not destroy deposited resources");
        var missing = new Job(new BlockPos(100_000, 100, 100_000), 4, materials, materials, 0, 0, 0);
        test.assertTrue(!level.hasChunkAt(missing.position()) && !missing.step(level), "Unloaded chunk pauses the job");
        test.assertTrue(!level.hasChunkAt(missing.position()), "Placement does not generate the missing chunk");
        test.succeed();
    }

    public static void largeDelivery(GameTestHelper test) {

        var level = test.getLevel();
        var centre = test.absolutePos(new BlockPos(512, 0, 512));
        var origin = new BlockPos(centre.getX(), 100, centre.getZ());
        var materials = List.of(new AsteroidMaterial(Identifier.parse("minecraft:stone"), 200_000),
                new AsteroidMaterial(Family.IRON.id(), 30_000),
                new AsteroidMaterial(Identifier.parse("minecraft:prismarine"), 20_000));
        var job = new Job(origin, 98, materials, materials, 0, 0, 0);
        for (var column : job.columns()) {
            var cx = (origin.getX() + column.x()) >> 4;
            var cz = (origin.getZ() + column.z()) >> 4;
            level.setChunkForced(cx, cz, true);
            level.getChunk(cx, cz);
        }
        var queue = new AsteroidDeliveries[]{new AsteroidDeliveries()};
        queue[0].jobs.add(job);
        var reloaded = new boolean[1];
        var ticks = new int[1];
        var durations = new long[6000];
        test.onEachTick(() -> {
            if (queue[0].jobs.isEmpty()) return;
            var before = queue[0].jobs.getFirst().remaining().stream().mapToInt(AsteroidMaterial::amount).sum();
            var start = System.nanoTime();
            queue[0].tick(level);
            durations[ticks[0]] = System.nanoTime() - start;
            ticks[0]++;
            var after = queue[0].jobs.isEmpty() ? 0
                    : queue[0].jobs.getFirst().remaining().stream().mapToInt(AsteroidMaterial::amount).sum();
            test.assertTrue(before - after <= AsteroidDeliveries.BLOCKS_PER_TICK, "Every tick obeys the global block budget");
            if (after < 125_000 && !reloaded[0]) {
                queue[0] = AsteroidDeliveries.CODEC.parse(NbtOps.INSTANCE,
                        AsteroidDeliveries.CODEC.encodeStart(NbtOps.INSTANCE, queue[0]).getOrThrow()).getOrThrow();
                reloaded[0] = true;
            }
            if (after != 0) return;
            var counts = new int[materials.size()];
            for (var column : job.columns()) {
                for (var y = 0; y < column.height(); y++) {
                    var state = level.getBlockState(origin.offset(column.x(), column.bottom() + y, column.z()));
                    for (var i = 0; i < materials.size(); i++) {
                        if (state.is(BuiltInRegistries.BLOCK.getValue(materials.get(i).block()))) counts[i]++;
                    }
                }
            }
            for (var i = 0; i < counts.length; i++)
                test.assertTrue(counts[i] == materials.get(i).amount(), "World composition matches after reload");
            Arrays.sort(durations, 0, ticks[0]);
            OritechSpaceAge.LOGGER.info("Asteroid delivery: 250000 blocks, {} ticks, p95 {} ms, slowest {} ms",
                    ticks[0], durations[(int) (ticks[0] * .95)] / 1e6, durations[ticks[0] - 1] / 1e6);
            for (var column : job.columns())
                level.setChunkForced((origin.getX() + column.x()) >> 4, (origin.getZ() + column.z()) >> 4, false);
            test.succeed();
        });
    }
}
