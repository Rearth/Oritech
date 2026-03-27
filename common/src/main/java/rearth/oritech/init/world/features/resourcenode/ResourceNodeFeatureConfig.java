package rearth.oritech.init.world.features.resourcenode;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record ResourceNodeFeatureConfig(int nodeSize, int boulderRadius, List<ResourceLocation> nodeOres, float nodeOreChance, List<ResourceLocation> boulderOres, ResourceLocation overlayBlock, int overlayHeight) implements FeatureConfiguration {
    
    public static final Codec<ResourceNodeFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("nodeSize").forGetter(ResourceNodeFeatureConfig::nodeSize),
      Codec.INT.fieldOf("boulderRadius").forGetter(ResourceNodeFeatureConfig::boulderRadius),
      ResourceLocation.CODEC.listOf().fieldOf("nodeOres").forGetter(ResourceNodeFeatureConfig::nodeOres),
      Codec.FLOAT.fieldOf("nodeOreChance").forGetter(ResourceNodeFeatureConfig::nodeOreChance),
      ResourceLocation.CODEC.listOf().fieldOf("boulderOres").forGetter(ResourceNodeFeatureConfig::boulderOres),
      ResourceLocation.CODEC.fieldOf("overlayBlock").forGetter(ResourceNodeFeatureConfig::overlayBlock),
      Codec.INT.fieldOf("overlayHeight").forGetter(ResourceNodeFeatureConfig::overlayHeight)
    ).apply(instance, ResourceNodeFeatureConfig::new));
}
