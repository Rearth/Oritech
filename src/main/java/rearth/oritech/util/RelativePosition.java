package rearth.oritech.util;

import net.minecraft.core.BlockPos;

public final class RelativePosition {

    private static final int MAX_EXPECTED_OFFSET = 10;

    private RelativePosition() {
    }

    public static BlockPos toOffset(BlockPos worldPosition, BlockPos origin) {
        return worldPosition.subtract(origin);
    }

    public static BlockPos normalizeOffset(BlockPos storedPosition, BlockPos origin) {
        // Position lists and controller links used world positions before relative storage was introduced.
        return looksLikeWorldPosition(storedPosition) ? storedPosition.subtract(origin) : storedPosition;
    }

    public static BlockPos toWorldPosition(BlockPos storedPosition, BlockPos origin) {
        return origin.offset(normalizeOffset(storedPosition, origin));
    }

    // this is just needed for migration / updates from old versions. Since the position was in world space, we treat anything larger than
    // 10 as old. Yes this will not work for machines that are exactly
    private static boolean looksLikeWorldPosition(BlockPos position) {
        return position.getX() < -MAX_EXPECTED_OFFSET || position.getX() > MAX_EXPECTED_OFFSET
                || position.getY() < -MAX_EXPECTED_OFFSET || position.getY() > MAX_EXPECTED_OFFSET
                || position.getZ() < -MAX_EXPECTED_OFFSET || position.getZ() > MAX_EXPECTED_OFFSET;
    }
}
