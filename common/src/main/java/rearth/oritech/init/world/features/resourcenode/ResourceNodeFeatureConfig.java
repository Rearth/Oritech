package rearth.oritech.init.world.features.resourcenode;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record ResourceNodeFeatureConfig(int nodeSize, int boulderRadius, List<ResourceLocation> nodeOres, float nodeOreChance, List<ResourceLocation> boulderOres, ResourceLocation overlayBlock, int overlayHeight) implements FeatureConfiguration {
    
    public static final Endec<ResourceNodeFeatureConfig> NODE_FEATURE_ENDEC = StructEndecBuilder.of(
      Endec.INT.fieldOf("nodeSize", ResourceNodeFeatureConfig::nodeSize),
      Endec.INT.fieldOf("boulderRadius", ResourceNodeFeatureConfig::boulderRadius),
      MinecraftEndecs.IDENTIFIER.listOf().fieldOf("nodeOres", ResourceNodeFeatureConfig::nodeOres),
      Endec.FLOAT.fieldOf("nodeOreChance", ResourceNodeFeatureConfig::nodeOreChance),
      MinecraftEndecs.IDENTIFIER.listOf().fieldOf("boulderOres", ResourceNodeFeatureConfig::boulderOres),
      MinecraftEndecs.IDENTIFIER.fieldOf("overlayBlock", ResourceNodeFeatureConfig::overlayBlock),
      Endec.INT.fieldOf("overlayHeight", ResourceNodeFeatureConfig::overlayHeight),
      ResourceNodeFeatureConfig::new
    );
}
