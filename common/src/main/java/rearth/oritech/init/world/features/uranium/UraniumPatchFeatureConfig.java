package rearth.oritech.init.world.features.uranium;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record UraniumPatchFeatureConfig(int number, ResourceLocation blockId, ResourceLocation crystalId) implements FeatureConfiguration {
    
    public static final Codec<UraniumPatchFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("number").forGetter(UraniumPatchFeatureConfig::number),
      ResourceLocation.CODEC.fieldOf("blockId").forGetter(UraniumPatchFeatureConfig::blockId),
      ResourceLocation.CODEC.fieldOf("crystalId").forGetter(UraniumPatchFeatureConfig::crystalId)
    ).apply(instance, UraniumPatchFeatureConfig::new));
    
}
