package rearth.oritech.init.world.features.uranium;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record UraniumPatchFeatureConfig(int number, ResourceLocation blockId, ResourceLocation crystalId) implements FeatureConfiguration {
    
    public static final Endec<UraniumPatchFeatureConfig> URANIUM_FEATURE_ENDEC = StructEndecBuilder.of(
      Endec.INT.fieldOf("number", UraniumPatchFeatureConfig::number),
      MinecraftEndecs.IDENTIFIER.fieldOf("blockId", UraniumPatchFeatureConfig::blockId),
      MinecraftEndecs.IDENTIFIER.fieldOf("crystalId", UraniumPatchFeatureConfig::crystalId),
      UraniumPatchFeatureConfig::new
    );
    
}
