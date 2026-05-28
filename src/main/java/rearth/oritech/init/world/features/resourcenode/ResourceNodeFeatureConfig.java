package rearth.oritech.init.world.features.resourcenode;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

public record ResourceNodeFeatureConfig(int nodeSize, int boulderRadius, List<Identifier> nodeOres, float nodeOreChance,
                                        List<Identifier> boulderOres, Identifier overlayBlock,
                                        int overlayHeight) implements FeatureConfiguration {
    
    public static final Codec<ResourceNodeFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("nodeSize").forGetter(ResourceNodeFeatureConfig::nodeSize),
      Codec.INT.fieldOf("boulderRadius").forGetter(ResourceNodeFeatureConfig::boulderRadius),
      Identifier.CODEC.listOf().fieldOf("nodeOres").forGetter(ResourceNodeFeatureConfig::nodeOres),
      Codec.FLOAT.fieldOf("nodeOreChance").forGetter(ResourceNodeFeatureConfig::nodeOreChance),
      Identifier.CODEC.listOf().fieldOf("boulderOres").forGetter(ResourceNodeFeatureConfig::boulderOres),
      Identifier.CODEC.fieldOf("overlayBlock").forGetter(ResourceNodeFeatureConfig::overlayBlock),
      Codec.INT.fieldOf("overlayHeight").forGetter(ResourceNodeFeatureConfig::overlayHeight)
    ).apply(instance, ResourceNodeFeatureConfig::new));
}
