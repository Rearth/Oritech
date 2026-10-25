package rearth.oritech.spaceage.block.launch_ramp;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Placed where accelerator particles leave the ring. Its menu holds the pattern cube-sat the plan is made for.
 */
public class LaunchRampBlock extends Block implements EntityBlock {

    public LaunchRampBlock(Properties properties) {

        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return new LaunchRampBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        // The menu sends the pattern's rocket and plan itself, also whenever the pattern changes.
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof LaunchRampBlockEntity ramp)
            serverPlayer.openMenu(ramp, pos);
        return InteractionResult.SUCCESS;
    }
}
