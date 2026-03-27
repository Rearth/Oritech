package rearth.oritech.init.world.features.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record OilSpringFeatureConfig(int number, ResourceLocation blockId) implements FeatureConfiguration {
    
    public static final Codec<OilSpringFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("number").forGetter(OilSpringFeatureConfig::number),
      ResourceLocation.CODEC.fieldOf("blockId").forGetter(OilSpringFeatureConfig::blockId)
    ).apply(instance, OilSpringFeatureConfig::new));
    
}
