package rearth.oritech.init.world;

import dev.architectury.registry.level.biome.BiomeModifications;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
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
                mutable.getGenerationProperties().addFeature(GenerationStep.Decoration.LAKES, ResourceKey.create(Registries.PLACED_FEATURE, Oritech.id("oil_spring")));
            }
        });
        
        BiomeModifications.addProperties((context, mutable) -> {
            if (context.hasTag(BiomeTags.HAS_VILLAGE_DESERT)) {
                mutable.getGenerationProperties().addFeature(GenerationStep.Decoration.LAKES, ResourceKey.create(Registries.PLACED_FEATURE, Oritech.id("oil_spring_desert")));
            }
        });
        
        BiomeModifications.addProperties((context, mutable) -> {
            if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                mutable.getGenerationProperties().addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, ResourceKey.create(Registries.PLACED_FEATURE, Oritech.id("resource_node_common")));
            }
        });
        
        BiomeModifications.addProperties((context, mutable) -> {
            if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                mutable.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, Oritech.id("resource_node_rare")));
            }
        });
        
        BiomeModifications.addProperties((context, mutable) -> {
            if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                mutable.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, Oritech.id("resource_node_other")));
            }
        });
        
        // ores
        if (Oritech.CONFIG.generateOresFabricOnly()) {
            BiomeModifications.addProperties((context, mutable) -> {
                if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                    mutable.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ResourceKey.create(Registries.PLACED_FEATURE, Oritech.id("ore_nickel")));
                }
            });
            
            BiomeModifications.addProperties((context, mutable) -> {
                if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                    mutable.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ResourceKey.create(Registries.PLACED_FEATURE, Oritech.id("uranium_patch")));
                }
            });
            
            BiomeModifications.addProperties((context, mutable) -> {
                if (context.hasTag(BiomeTags.IS_OVERWORLD)) {
                    mutable.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ResourceKey.create(Registries.PLACED_FEATURE, Oritech.id("ore_platinum")));
                }
            });
            
            BiomeModifications.addProperties((context, mutable) -> {
                if (context.hasTag(BiomeTags.IS_END)) {
                    mutable.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ResourceKey.create(Registries.PLACED_FEATURE, Oritech.id("ore_platinum_end")));
                }
            });
        }
    }
    
    @Override
    public ResourceKey<Registry<Feature<?>>> getRegistryType() {
        return Registries.FEATURE;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<Feature<?>> getTargetFieldType() {
        return (Class<Feature<?>>) (Object) Feature.class;
    }
}
