package rearth.oritech.api.splicer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Client-side view of the current splicer operation, synced to the splicer screen.
 */
public record SplicerPreview(boolean hasOperation, List<Component> tooltip, ItemStack result, long energyCost,
                             Optional<Component> blockingReason) {

    public static final SplicerPreview EMPTY = new SplicerPreview(false, List.of(), ItemStack.EMPTY, 0, Optional.empty());

    public static final StreamCodec<RegistryFriendlyByteBuf, SplicerPreview> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SplicerPreview::hasOperation,
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()), SplicerPreview::tooltip,
            ItemStack.OPTIONAL_STREAM_CODEC, SplicerPreview::result,
            ByteBufCodecs.VAR_LONG, SplicerPreview::energyCost,
            ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), SplicerPreview::blockingReason,
            SplicerPreview::new
    );

    public static SplicerPreview of(SplicerOperation operation, long energyCost) {
        return new SplicerPreview(true, List.copyOf(operation.getTooltip()), operation.getPreviewResult().copy(), energyCost, operation.getBlockingReason());
    }

    // records compare ItemStacks by reference, so this is used to detect actual changes
    public boolean matches(SplicerPreview other) {
        return hasOperation == other.hasOperation
                && energyCost == other.energyCost
                && tooltip.equals(other.tooltip)
                && blockingReason.equals(other.blockingReason)
                && ItemStack.matches(result, other.result);
    }
}
