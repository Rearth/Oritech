package rearth.oritech.init;

import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import rearth.oritech.util.ArchitecturyRegistryContainer;
import rearth.oritech.util.NbtBlockLootFunction;

public class LootContent implements ArchitecturyRegistryContainer<LootFunctionType<?>> {
    
    public static final LootFunctionType<NbtBlockLootFunction> NBT_BLOCK_LOOT_FUNCTION = new LootFunctionType<>(NbtBlockLootFunction.CODEC);
    
    @Override
    public RegistryKey<Registry<LootFunctionType<?>>> getRegistryType() {
        return RegistryKeys.LOOT_FUNCTION_TYPE;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<LootFunctionType<?>> getTargetFieldType() {
        return (Class<LootFunctionType<?>>) (Object) LootFunctionType.class;
    }
}
