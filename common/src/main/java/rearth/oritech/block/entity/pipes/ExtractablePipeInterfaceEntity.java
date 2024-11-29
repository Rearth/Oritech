package rearth.oritech.block.entity.pipes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public abstract class ExtractablePipeInterfaceEntity extends GenericPipeInterfaceEntity {

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
