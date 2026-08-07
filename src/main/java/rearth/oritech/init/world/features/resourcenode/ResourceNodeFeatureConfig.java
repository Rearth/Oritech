package rearth.oritech.init.world.features.resourcenode;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

public record ResourceNodeFeatureConfig(int nodeSize, int boulderRadius, List<WeightedBlock> nodeOres, float nodeOreChance,
                                        List<WeightedBlock> boulderOres, Identifier overlayBlock,
                                        int overlayHeight) implements FeatureConfiguration {

    public static final Codec<ResourceNodeFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("nodeSize").forGetter(ResourceNodeFeatureConfig::nodeSize),
            Codec.INT.fieldOf("boulderRadius").forGetter(ResourceNodeFeatureConfig::boulderRadius),
            WeightedBlock.CODEC.listOf().fieldOf("nodeOres").forGetter(ResourceNodeFeatureConfig::nodeOres),
            Codec.FLOAT.fieldOf("nodeOreChance").forGetter(ResourceNodeFeatureConfig::nodeOreChance),
            WeightedBlock.CODEC.listOf().fieldOf("boulderOres").forGetter(ResourceNodeFeatureConfig::boulderOres),
            Identifier.CODEC.fieldOf("overlayBlock").forGetter(ResourceNodeFeatureConfig::overlayBlock),
            Codec.INT.fieldOf("overlayHeight").forGetter(ResourceNodeFeatureConfig::overlayHeight)
    ).apply(instance, ResourceNodeFeatureConfig::new));
}
