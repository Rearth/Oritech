package rearth.oritech.init.datapack;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import rearth.oritech.Oritech;

// this is a datapack driven registry, so no augments are registered directly via code
public class AugmentContent {

    public static final ResourceKey<Registry<AugmentData>> AUGMENT_REGISTRY_KEY = ResourceKey.createRegistryKey(Oritech.id("augments"));

    public static ResourceKey<AugmentData> key(Identifier id) {
        return ResourceKey.create(AUGMENT_REGISTRY_KEY, id);
    }

    public static void registerDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(AUGMENT_REGISTRY_KEY, AugmentData.CODEC, AugmentData.CODEC);
    }
}

