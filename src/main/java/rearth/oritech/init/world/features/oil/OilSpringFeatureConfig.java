package rearth.oritech.init.world.features.oil;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.FeatureConfig;

public record OilSpringFeatureConfig(int number, Identifier blockId) implements FeatureConfig {
    
    public static final Endec<OilSpringFeatureConfig> OIL_FEATURE_ENDEC = StructEndecBuilder.of(
      Endec.INT.fieldOf("number", OilSpringFeatureConfig::number),
      MinecraftEndecs.IDENTIFIER.fieldOf("blockId", OilSpringFeatureConfig::blockId),
      OilSpringFeatureConfig::new
    );
    
}
