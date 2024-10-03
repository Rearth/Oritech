package rearth.oritech.init.world.features.resourcenode;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.FeatureConfig;

import java.util.List;

public record ResourceNodeFeatureConfig(int nodeSize, int boulderRadius, List<Identifier> nodeOres, float nodeOreChance, List<Identifier> boulderOres, Identifier overlayBlock, int overlayHeight) implements FeatureConfig {
    
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
