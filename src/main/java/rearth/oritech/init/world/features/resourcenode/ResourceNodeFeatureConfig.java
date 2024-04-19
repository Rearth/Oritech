package rearth.oritech.init.world.features.resourcenode;

import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.FeatureConfig;

import java.util.List;

public record ResourceNodeFeatureConfig(int nodeSize, int boulderRadius, List<Identifier> nodeOres, List<Identifier> boulderOres, Identifier overlayBlock, int overlayHeight) implements FeatureConfig {
    
    public static final Endec<ResourceNodeFeatureConfig> NODE_FEATURE_ENDEC = StructEndecBuilder.of(
      Endec.INT.fieldOf("nodeSize", ResourceNodeFeatureConfig::nodeSize),
      Endec.INT.fieldOf("boulderRadius", ResourceNodeFeatureConfig::boulderRadius),
      BuiltInEndecs.IDENTIFIER.listOf().fieldOf("nodeOres", ResourceNodeFeatureConfig::nodeOres),
      BuiltInEndecs.IDENTIFIER.listOf().fieldOf("boulderOres", ResourceNodeFeatureConfig::boulderOres),
      BuiltInEndecs.IDENTIFIER.fieldOf("overlayBlock", ResourceNodeFeatureConfig::overlayBlock),
      Endec.INT.fieldOf("overlayHeight", ResourceNodeFeatureConfig::overlayHeight),
      ResourceNodeFeatureConfig::new
    );
}
