package rearth.oritech.init.level.features.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record OilSpringFeatureConfig(int number, Identifier blockId) implements FeatureConfiguration {
    
    public static final Codec<OilSpringFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("number").forGetter(OilSpringFeatureConfig::number),
      Identifier.CODEC.fieldOf("blockId").forGetter(OilSpringFeatureConfig::blockId)
    ).apply(instance, OilSpringFeatureConfig::new));
    
}
