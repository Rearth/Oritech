package rearth.oritech.init.world;

import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.Feature;
import rearth.oritech.Oritech;
import rearth.oritech.init.world.features.oil.OilSpringFeature;
import rearth.oritech.init.world.features.oil.OilSpringFeatureConfig;
import rearth.oritech.init.world.features.resourcenode.ResourceNodeFeature;
import rearth.oritech.init.world.features.resourcenode.ResourceNodeFeatureConfig;
import rearth.oritech.init.world.features.uranium.UraniumPatchFeature;
import rearth.oritech.init.world.features.uranium.UraniumPatchFeatureConfig;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

public class FeatureContent implements ArchitecturyRegistryContainer<Feature<?>> {
    
    public static final Feature<OilSpringFeatureConfig> OIL_SPRING = new OilSpringFeature(CodecUtils.toCodec(OilSpringFeatureConfig.OIL_FEATURE_ENDEC));
    public static final Feature<ResourceNodeFeatureConfig> RESOURCE_NODE = new ResourceNodeFeature(CodecUtils.toCodec(ResourceNodeFeatureConfig.NODE_FEATURE_ENDEC));
    public static final Feature<UraniumPatchFeatureConfig> URANIUM_PATCH = new UraniumPatchFeature(CodecUtils.toCodec(UraniumPatchFeatureConfig.URANIUM_FEATURE_ENDEC));
    
    public static void initialize() {
        
        BiomeModifications.addFeature(
          BiomeSelectors.foundInOverworld(),
          GenerationStep.Feature.LAKES,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("oil_spring")));
        
        BiomeModifications.addFeature(
          BiomeSelectors.tag(BiomeTags.VILLAGE_DESERT_HAS_STRUCTURE),
          GenerationStep.Feature.LAKES,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("oil_spring_desert")));
        
        BiomeModifications.addFeature(
          BiomeSelectors.foundInOverworld(),
          GenerationStep.Feature.TOP_LAYER_MODIFICATION,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("resource_node_common")));
        
        BiomeModifications.addFeature(
          BiomeSelectors.foundInOverworld(),
          GenerationStep.Feature.UNDERGROUND_DECORATION,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("resource_node_rare")));
        
        BiomeModifications.addFeature(
          BiomeSelectors.foundInOverworld(),
          GenerationStep.Feature.UNDERGROUND_DECORATION,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("resource_node_other")));
        
        // ores
        if (Oritech.CONFIG.generateOres()) {
            BiomeModifications.addFeature(
              BiomeSelectors.foundInOverworld(),
              GenerationStep.Feature.UNDERGROUND_ORES,
              RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("ore_nickel")));
            
            BiomeModifications.addFeature(
              BiomeSelectors.foundInOverworld(),
              GenerationStep.Feature.UNDERGROUND_ORES,
              RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("uranium_patch")));
            
            BiomeModifications.addFeature(
              BiomeSelectors.foundInOverworld(),
              GenerationStep.Feature.UNDERGROUND_ORES,
              RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("ore_platinum")));
            
            BiomeModifications.addFeature(
              BiomeSelectors.foundInTheEnd(),
              GenerationStep.Feature.UNDERGROUND_ORES,
              RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("ore_platinum_end")));
        }
    }
    
    @Override
    public RegistryKey<Registry<Feature<?>>> getRegistryType() {
        return RegistryKeys.FEATURE;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<Feature<?>> getTargetFieldType() {
        return (Class<Feature<?>>) (Object) Feature.class;
    }
}
