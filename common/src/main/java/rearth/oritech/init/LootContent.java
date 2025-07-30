package rearth.oritech.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import rearth.oritech.util.NbtBlockLootFunction;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

public class LootContent implements ArchitecturyRegistryContainer<LootItemFunctionType<?>> {
    
    public static final LootItemFunctionType<NbtBlockLootFunction> NBT_BLOCK_LOOT_FUNCTION = new LootItemFunctionType<>(NbtBlockLootFunction.CODEC);
    
    @Override
    public ResourceKey<Registry<LootItemFunctionType<?>>> getRegistryType() {
        return Registries.LOOT_FUNCTION_TYPE;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<LootItemFunctionType<?>> getTargetFieldType() {
        return (Class<LootItemFunctionType<?>>) (Object) LootItemFunctionType.class;
    }
}
