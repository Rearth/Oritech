package rearth.oritech.init.world.features.oil;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record OilSpringFeatureConfig(int number, ResourceLocation blockId) implements FeatureConfiguration {
    
    public static final Endec<OilSpringFeatureConfig> OIL_FEATURE_ENDEC = StructEndecBuilder.of(
      Endec.INT.fieldOf("number", OilSpringFeatureConfig::number),
      MinecraftEndecs.IDENTIFIER.fieldOf("blockId", OilSpringFeatureConfig::blockId),
      OilSpringFeatureConfig::new
    );
    
}
