package rearth.oritech.init.world.features.oil;

import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.FeatureConfig;

public record OilSpringFeatureConfig(int number, Identifier blockId) implements FeatureConfig {
    
    public static final Endec<OilSpringFeatureConfig> OIL_FEATURE_ENDEC = StructEndecBuilder.of(
      Endec.INT.fieldOf("number", OilSpringFeatureConfig::number),
      BuiltInEndecs.IDENTIFIER.fieldOf("blockId", OilSpringFeatureConfig::blockId),
      OilSpringFeatureConfig::new
    );
    
}
