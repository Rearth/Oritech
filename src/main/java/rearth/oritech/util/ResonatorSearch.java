package rearth.oritech.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.ComponentContent;

import java.util.ArrayDeque;
import java.util.function.Predicate;

/**
 * Loaded-chunk searches, advanced only on the server thread with a shared per-tick budget.
 */
public final class ResonatorSearch {
    public static final int RANGE = 128;
    private static final ArrayDeque<Search> SEARCHES = new ArrayDeque<>();

    public static boolean start(ServerPlayer player, ItemStack stack, TagKey<Block> targets, String translationKey, int particleColor) {
        if (SEARCHES.stream().anyMatch(search -> search.player == player)) return false;
        SEARCHES.addLast(new Search(player, stack, targets, translationKey, particleColor));
        player.sendSystemMessage(Component.translatable("message.oritech.resonator.searching"), true);
        return true;
    }

    public static void pointTo(ServerPlayer player, GlobalPos result, String translationKey, int particleColor) {
        if (!result.dimension().equals(player.level().dimension())) {
            player.sendSystemMessage(Component.translatable("message.oritech.resonator.other_dimension",
                    result.dimension().identifier().toString()), true);
            return;
        }
        // Cached guidance only needs coordinates; it never loads or checks the target chunk.
        var target = result.pos().getCenter();
        var origin = player.getEyePosition().add(0, -0.3, 0);
        var direction = target.subtract(origin);
        // Extend behind the target's bearing without raising the trail for underground targets.
        var horizontalDirection = new Vec3(direction.x, 0, direction.z).normalize();
        var start = origin.subtract(horizontalDirection.scale(3));
        var end = origin.add(direction.normalize().scale(Math.min(8, direction.length())));
        PacketDistributor.sendToPlayer(player, new ParticleContent.Payload(
                ParticleContent.EffectType.RESONATOR_DIRECTION.ordinal(), start, end, Vec3.ZERO, particleColor));
        int distance = (int) Math.round(player.position().distanceTo(target));
        player.sendSystemMessage(Component.translatable("message.oritech." + translationKey + ".found",
                distance, result.pos().getY()), true);
    }

    public static void tick() {
        var budget = new Budget();
        int pending = SEARCHES.size();
        while (pending-- > 0 && budget.sections > 0 && budget.blocks > 0) {
            var search = SEARCHES.removeFirst();
            if (search.player.isRemoved() || search.player.hasDisconnected() || search.player.level() != search.level || !search.player.isAlive())
                continue;
            // The result belongs to the initiating stack, even if the player switches held items.
            if (!search.player.getInventory().contains(stack -> stack == search.stack)) continue;
            if (search.advance(budget)) search.finish();
            else SEARCHES.addLast(search);
        }
    }

    public static void clear() {
        SEARCHES.clear();
    }

    private static final class Budget {
        int sections = 256;
        int blocks = 16_384;
    }

    private static final class Search {
        final ServerPlayer player;
        final ItemStack stack;
        final ServerLevel level;
        final Vec3 origin;
        final Predicate<BlockState> targetPredicate;
        final String translationKey;
        final int particleColor;
        final int minChunkX;
        final int minChunkZ;
        final int width;
        final int depth;
        int chunkIndex;
        int sectionIndex;
        int blockIndex = -1;
        BlockPos closest;
        double closestDistance = Double.POSITIVE_INFINITY;

        Search(ServerPlayer player, ItemStack stack, TagKey<Block> targets, String translationKey, int particleColor) {
            this.player = player;
            this.stack = stack;
            targetPredicate = state -> state.is(targets);
            this.translationKey = translationKey;
            this.particleColor = particleColor;
            level = player.level();
            origin = player.position();
            var pos = player.blockPosition();
            minChunkX = (pos.getX() - RANGE) >> 4;
            minChunkZ = (pos.getZ() - RANGE) >> 4;
            width = ((pos.getX() + RANGE) >> 4) - minChunkX + 1;
            depth = ((pos.getZ() + RANGE) >> 4) - minChunkZ + 1;
        }

        boolean advance(Budget budget) {
            while (chunkIndex < width * depth && budget.sections > 0 && budget.blocks > 0) {
                int chunkX = minChunkX + chunkIndex % width;
                int chunkZ = minChunkZ + chunkIndex / width;
                // Never load or generate chunks, and don't retain chunk references between ticks.
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null || sectionIndex >= chunk.getSectionsCount()) {
                    chunkIndex++;
                    sectionIndex = 0;
                    blockIndex = -1;
                    budget.sections--;
                    continue;
                }
                var section = chunk.getSection(sectionIndex);
                if (blockIndex == -1) {
                    budget.sections--;
                    if (!section.maybeHas(targetPredicate)) {
                        sectionIndex++;
                        continue;
                    }
                    blockIndex = 0;
                }
                int baseY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
                while (blockIndex < 4096 && budget.blocks > 0) {
                    int index = blockIndex++;
                    budget.blocks--;
                    int x = index & 15;
                    int z = (index >> 4) & 15;
                    int y = index >> 8;
                    if (!targetPredicate.test(section.getBlockState(x, y, z))) continue;
                    double worldX = (chunkX << 4) + x + 0.5;
                    double worldZ = (chunkZ << 4) + z + 0.5;
                    double dx = worldX - origin.x;
                    double dz = worldZ - origin.z;
                    if (dx * dx + dz * dz > RANGE * RANGE) continue;
                    double distance = origin.distanceToSqr(worldX, baseY + y + 0.5, worldZ);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closest = new BlockPos((chunkX << 4) + x, baseY + y, (chunkZ << 4) + z);
                    }
                }
                if (blockIndex == 4096) {
                    blockIndex = -1;
                    sectionIndex++;
                }
            }
            return chunkIndex == width * depth;
        }

        void finish() {
            if (closest == null) {
                stack.remove(ComponentContent.TARGET_GLOBAL_POSITION.get());
                player.getInventory().setChanged();
                player.sendSystemMessage(Component.translatable("message.oritech." + translationKey + ".empty"), true);
                return;
            }
            var result = GlobalPos.of(level.dimension(), closest);
            stack.set(ComponentContent.TARGET_GLOBAL_POSITION.get(), result);
            player.getInventory().setChanged();
            pointTo(player, result, translationKey, particleColor);
        }
    }
}
