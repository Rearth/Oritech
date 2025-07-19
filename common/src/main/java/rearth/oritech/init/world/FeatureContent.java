package rearth.oritech.init.world;

import dev.architectury.registry.level.biome.BiomeModifications;
import io.wispforest.owo.serialization.CodecUtils;
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

// this currently only works on fabric (see https://github.com/Rearth/Oritech/pull/359 & https://github.com/architectury/architectury-api/issues/480)
// when adding/changing features, make sure to update the neo json files aswell
public class FeatureContent implements ArchitecturyRegistryContainer<Feature<?>> {
    
    public static final Feature<OilSpringFeatureConfig> OIL_SPRING = new OilSpringFeature(CodecUtils.toCodec(OilSpringFeatureConfig.OIL_FEATURE_ENDEC));
    public static final Feature<ResourceNodeFeatureConfig> RESOURCE_NODE = new ResourceNodeFeature(CodecUtils.toCodec(ResourceNodeFeatureConfig.NODE_FEATURE_ENDEC));
    public static final Feature<UraniumPatchFeatureConfig> URANIUM_PATCH = new UraniumPatchFeature(CodecUtils.toCodec(UraniumPatchFeatureConfig.URANIUM_FEATURE_ENDEC));
    
    public static void initialize() {
        
        BiomeModifications.addProperties((context, mutable) -> {
            if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                mutable.getGenerationProperties().addFeature(GenerationStep.Feature.LAKES, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("oil_spring")));
            }
        });
        
        BiomeModifications.addProperties((context, mutable) -> {
            if (context.hasTag(BiomeTags.VILLAGE_DESERT_HAS_STRUCTURE)) {
                mutable.getGenerationProperties().addFeature(GenerationStep.Feature.LAKES, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("oil_spring_desert")));
            }
        });
        
        BiomeModifications.addProperties((context, mutable) -> {
            if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                mutable.getGenerationProperties().addFeature(GenerationStep.Feature.TOP_LAYER_MODIFICATION, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("resource_node_common")));
            }
        });
        
        BiomeModifications.addProperties((context, mutable) -> {
            if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                mutable.getGenerationProperties().addFeature(GenerationStep.Feature.UNDERGROUND_DECORATION, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("resource_node_rare")));
            }
        });
        
        BiomeModifications.addProperties((context, mutable) -> {
            if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                mutable.getGenerationProperties().addFeature(GenerationStep.Feature.UNDERGROUND_DECORATION, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("resource_node_other")));
            }
        });
        
        // ores
        if (Oritech.CONFIG.generateOresFabricOnly()) {
            BiomeModifications.addProperties((context, mutable) -> {
                if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                    mutable.getGenerationProperties().addFeature(GenerationStep.Feature.UNDERGROUND_ORES, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("ore_nickel")));
                }
            });
            
            BiomeModifications.addProperties((context, mutable) -> {
                if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                    mutable.getGenerationProperties().addFeature(GenerationStep.Feature.UNDERGROUND_ORES, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("uranium_patch")));
                }
            });
            
            BiomeModifications.addProperties((context, mutable) -> {
                if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                    mutable.getGenerationProperties().addFeature(GenerationStep.Feature.UNDERGROUND_ORES, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("ore_platinum")));
                }
            });
            
            BiomeModifications.addProperties((context, mutable) -> {
                if (context.hasTag(BiomeTags.IS_END)) {
                    mutable.getGenerationProperties().addFeature(GenerationStep.Feature.UNDERGROUND_ORES, RegistryKey.of(RegistryKeys.PLACED_FEATURE, Oritech.id("ore_platinum_end")));
                }
            });
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
