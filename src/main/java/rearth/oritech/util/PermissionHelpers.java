package rearth.oritech.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public class PermissionHelpers {
    // Permission checks post another break event;
    // to avoid recusion on neoforge we add this flag
    public static final ThreadLocal<Boolean> CHECKING_OFFSET_BREAK_PERMISSION = ThreadLocal.withInitial(() -> false);

    public static boolean CanPlayerBreakBlock(Level level, BlockPos blockPos, BlockState blockState, ServerPlayer player) {
        var wasCheckingOffsetBreakPermission = CHECKING_OFFSET_BREAK_PERMISSION.get();
        CHECKING_OFFSET_BREAK_PERMISSION.set(true);
        try {
            var event = new BreakBlockEvent(level, blockPos, blockState, player);
            NeoForge.EVENT_BUS.post(event);
            return !event.isCanceled();
        } finally {
            CHECKING_OFFSET_BREAK_PERMISSION.set(wasCheckingOffsetBreakPermission);
        }
    }

    public static boolean CanPlayerAttackEntity(LivingEntity target, Level level, float amount, DamageSource damageSource) {

        if (!(level instanceof ServerLevel serverLevel))
            return true;

        var event = new LivingIncomingDamageEvent(target, new DamageContainer(damageSource, amount));
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

}
