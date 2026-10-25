package rearth.oritech.api.splicer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import rearth.oritech.block.entity.interaction.AddonSplicerBlockEntity;

/**
 * Information about the splicer that is currently looking for something to compact.
 *
 * @param level     the level the splicer is in
 * @param splicerPos the position of the splicer controller block
 * @param facing    the horizontal facing of the splicer
 * @param targetPos the position directly in front of the splicer's work slot. Behaviors are free to look elsewhere.
 * @param splicer   the splicer block entity itself
 */
public record SplicerContext(ServerLevel level, BlockPos splicerPos, Direction facing, BlockPos targetPos,
                             AddonSplicerBlockEntity splicer) {
}
