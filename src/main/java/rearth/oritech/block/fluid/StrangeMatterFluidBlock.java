package rearth.oritech.block.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

// teleports the player on contact and applies any random potion effect for x seconds
public class StrangeMatterFluidBlock extends LiquidBlock {

    private static final String NEXT_CONTACT = "oritech:strange_matter_next_contact";

    public StrangeMatterFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);

        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player) || player.isSpectator()) return;
        if (player.getY() >= pos.getY() + state.getFluidState().getHeight(level, pos)) return;

        // A player can touch several fluid blocks per tick. Share the cooldown across all of them.
        var now = serverLevel.getServer().overworld().getGameTime();
        var data = player.getPersistentData();
        if (now < data.getLongOr(NEXT_CONTACT, 0)) return;
        data.putLong(NEXT_CONTACT, now + 20);

        teleportNearby(serverLevel, player);
        var random = player.getRandom();
        BuiltInRegistries.MOB_EFFECT.getRandom(random).ifPresent(effect -> {
            if (player.addEffect(new MobEffectInstance(effect, (5 + random.nextInt(56)) * 20))) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.3f, 1.2f);
            }
        });
    }

    private static void teleportNearby(ServerLevel level, ServerPlayer player) {
        var origin = player.position();
        var center = player.blockPosition();
        var destinations = new ArrayList<Vec3>();

        for (var pos : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
            var destination = Vec3.atBottomCenterOf(pos);
            var distance = origin.distanceToSqr(destination);
            if (distance < 4 || distance > 25 || !level.hasChunkAt(pos)) continue;
            if (level.isOutsideBuildHeight(pos) || level.isOutsideBuildHeight(pos.above())) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;

            var floorPos = pos.below();
            var floor = level.getBlockState(floorPos);
            // vanilla path classification also rejects damaging floors such as magma and cactus.
            if (!floor.entityCanStandOn(level, floorPos, player) || !floor.getFluidState().isEmpty()) continue;
            if (level.getPathTypeCache().getOrCompute(level, floorPos) != PathType.BLOCKED) continue;

            var bounds = player.getBoundingBox().move(destination.subtract(origin));
            if (!level.getWorldBorder().isWithinBounds(bounds) || !level.noCollision(player, bounds)) continue;
            if (!BlockPos.betweenClosedStream(bounds).allMatch(bodyPos -> level.getBlockState(bodyPos).isAir())) continue;
            destinations.add(destination);
        }

        if (destinations.isEmpty()) return;
        var destination = destinations.get(player.getRandom().nextInt(destinations.size()));
        player.stopRiding();
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        level.playSound(null, destination.x, destination.y, destination.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.35f, 1f);
    }
}
