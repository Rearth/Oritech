package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A finite activation survives reload and never admits its own products.
 */
public record CraftPass(UUID action, List<RocketControllerState.Pair> pending, int total) {

    public static final Codec<CraftPass> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.STRING_CODEC.fieldOf("action").forGetter(p -> p.action),
            RocketControllerState.Pair.CODEC.listOf().fieldOf("pending").forGetter(p -> p.pending),
            Codec.INT.fieldOf("total").forGetter(p -> p.total)
    ).apply(i, CraftPass::new));

    public CraftPass(UUID action, List<RocketControllerState.Pair> pending, int total) {

        this.action = action;
        this.pending = new ArrayList<>(pending);
        this.total = total;
    }

    public CraftPass copy() {

        return new CraftPass(action, pending, total);
    }
}
