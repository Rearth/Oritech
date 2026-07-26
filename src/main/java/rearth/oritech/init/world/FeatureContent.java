package rearth.oritech.init.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;
import rearth.oritech.init.world.features.oil.OilSpringFeature;
import rearth.oritech.init.world.features.oil.OilSpringFeatureConfig;
import rearth.oritech.init.world.features.resourcenode.ResourceNodeFeature;
import rearth.oritech.init.world.features.resourcenode.ResourceNodeFeatureConfig;
import rearth.oritech.init.world.features.uranium.UraniumPatchFeature;
import rearth.oritech.init.world.features.uranium.UraniumPatchFeatureConfig;

import java.util.function.Supplier;

public class FeatureContent {

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Oritech.MOD_ID);

    public static final Supplier<Feature<?>> OIL_SPRING = FEATURES.register("oil_spring", () -> new OilSpringFeature(OilSpringFeatureConfig.CODEC));
    public static final Supplier<Feature<?>> RESOURCE_NODE = FEATURES.register("resource_node", () -> new ResourceNodeFeature(ResourceNodeFeatureConfig.CODEC));
    public static final Supplier<Feature<?>> URANIUM_PATCH = FEATURES.register("uranium_patch", () -> new UraniumPatchFeature(UraniumPatchFeatureConfig.CODEC));

}
