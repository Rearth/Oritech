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

public class FeatureContent {
    
    public static final OilSpringFeature OIL_SPRING_FEATURE = new OilSpringFeature(OilSpringFeatureConfig.OIL_FEATURE_ENDEC.codec());
    
    public static void initialize() {
        Registry.register(Registries.FEATURE, new Identifier(Oritech.MOD_ID, "oil_spring"), OIL_SPRING_FEATURE);
        
        BiomeModifications.addFeature(
          BiomeSelectors.foundInOverworld(),
          GenerationStep.Feature.LAKES,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(Oritech.MOD_ID, "oil_spring")));
        
        BiomeModifications.addFeature(
          BiomeSelectors.tag(BiomeTags.VILLAGE_DESERT_HAS_STRUCTURE),
          GenerationStep.Feature.LAKES,
          RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(Oritech.MOD_ID, "oil_spring_desert")));
    }
    
}
