package rearth.oritech.spaceage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MissionControlBlock extends SurveyModuleBlock {
    public MissionControlBlock(Properties properties) { super(properties); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof GroundStationBlockEntity station) {
            serverPlayer.openMenu(station, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
