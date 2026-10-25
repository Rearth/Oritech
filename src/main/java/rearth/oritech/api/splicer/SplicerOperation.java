package rearth.oritech.api.splicer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.config.OritechConfig;

import java.util.List;
import java.util.Optional;

/**
 * A single compaction that a splicer can perform. Created by a {@link SplicerBehavior}.
 * Operations are short-lived: the splicer resolves a fresh one before each use, so they can safely hold a snapshot of the world.
 */
public interface SplicerOperation {

    /**
     * Lines shown in the tooltip of the splicer's compact button.
     */
    List<Component> getTooltip();

    /**
     * The item that will be created, shown as a faded preview in the empty output slot.
     * Used for display only, the actual result is returned by {@link #execute(ServerLevel)}.
     */
    ItemStack getPreviewResult();

    /**
     * RF consumed by this operation. Will be clamped to the energy capacity of the splicer.
     */
    default long getEnergyCost() {
        return OritechConfig.addonConfig.addonAddonSplicerRF.get();
    }

    /**
     * If present, the operation is currently not possible, and the reason is shown in the splicer UI.
     */
    default Optional<Component> getBlockingReason() {
        return Optional.empty();
    }

    /**
     * Positions used for the particles and sounds when compacting.
     */
    default List<BlockPos> getAffectedPositions() {
        return List.of();
    }

    /**
     * Performs the compaction (e.g. removing the blocks / entities from the world).
     * Only called when the splicer has enough energy, an empty output slot, and no blocking reason exists.
     *
     * @return the resulting item that is placed in the splicer output slot
     */
    ItemStack execute(ServerLevel level);

}
