package rearth.oritech.spaceage.block.trophy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;

public record TrophyInscription(String player, OrbitBand orbit) {

    public static final Codec<TrophyInscription> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("player").forGetter(TrophyInscription::player),
            OrbitBand.CODEC.fieldOf("orbit").forGetter(TrophyInscription::orbit)
    ).apply(i, TrophyInscription::new));
}
