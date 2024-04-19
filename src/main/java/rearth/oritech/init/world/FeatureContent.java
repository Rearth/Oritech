package rearth.oritech.init.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import rearth.oritech.Oritech;
import rearth.oritech.init.world.features.oil.OilSpringFeature;
import rearth.oritech.init.world.features.oil.OilSpringFeatureConfig;
import rearth.oritech.init.world.features.resourcenode.ResourceNodeFeature;
import rearth.oritech.init.world.features.resourcenode.ResourceNodeFeatureConfig;

public class FeatureContent {
    
    public static final OilSpringFeature OIL_SPRING_FEATURE = new OilSpringFeature(OilSpringFeatureConfig.OIL_FEATURE_ENDEC.codec());
    public static final ResourceNodeFeature RESOURCE_NODE_FEATURE = new ResourceNodeFeature(ResourceNodeFeatureConfig.NODE_FEATURE_ENDEC.codec());
    
    public static void initialize() {
        Registry.register(Registries.FEATURE, new Identifier(Oritech.MOD_ID, "oil_spring"), OIL_SPRING_FEATURE);
        Registry.register(Registries.FEATURE, new Identifier(Oritech.MOD_ID, "resource_node"), RESOURCE_NODE_FEATURE);
        
        BiomeModifications.addFeature(
          BiomeSelectors.foundInOverworld(),
          GenerationStep.Feature.LAKES,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(Oritech.MOD_ID, "oil_spring")));
        
        BiomeModifications.addFeature(
          BiomeSelectors.tag(BiomeTags.VILLAGE_DESERT_HAS_STRUCTURE),
          GenerationStep.Feature.LAKES,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(Oritech.MOD_ID, "oil_spring_desert")));
        
        BiomeModifications.addFeature(
          BiomeSelectors.foundInOverworld(),
          GenerationStep.Feature.TOP_LAYER_MODIFICATION,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(Oritech.MOD_ID, "resource_node_common")));
        
        BiomeModifications.addFeature(
          BiomeSelectors.foundInOverworld(),
          GenerationStep.Feature.TOP_LAYER_MODIFICATION,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(Oritech.MOD_ID, "resource_node_rare")));
        BiomeModifications.addFeature(
          BiomeSelectors.foundInOverworld(),
          GenerationStep.Feature.TOP_LAYER_MODIFICATION,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(Oritech.MOD_ID, "resource_node_other")));
    }
    
}
