package rearth.oritech.block.entity.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.lookup.BlockLookupCache;

public abstract class ExtractablePipeInterfaceEntity extends GenericPipeInterfaceEntity {
	protected record CachedTarget<T>(BlockPos pos, Direction side, BlockLookupCache<T> lookup) {}

	protected int filteredTargetsNetHash;

	public ExtractablePipeInterfaceEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * Invalidates the target cache of this block entity
	 * This is used to force the block entity to recalculate the targets
	 * Used when extraction is toggled
	 */
	public void invalidateTargetCache() {
		filteredTargetsNetHash = 0;
	}
}
