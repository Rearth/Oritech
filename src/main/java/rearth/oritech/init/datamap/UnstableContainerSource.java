package rearth.oritech.init.datamap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Data map value attached to blocks that can be captured by the unstable container.
 * The {@code quality} acts as a multiplier on the container's base energy capacity.
 * <p>
 * The codec accepts either a full object ({@code {"quality": 1.0}}) or a bare float for convenience.
 */
public record UnstableContainerSource(float quality) {

    public static final Codec<UnstableContainerSource> CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.FLOAT.fieldOf("quality").forGetter(UnstableContainerSource::quality)
            ).apply(instance, UnstableContainerSource::new)),
            Codec.FLOAT.xmap(UnstableContainerSource::new, UnstableContainerSource::quality)
    );
}

