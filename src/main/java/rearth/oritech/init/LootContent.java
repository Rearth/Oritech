package rearth.oritech.init;

import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import rearth.oritech.Oritech;
import rearth.oritech.init.datagen.loot.NbtBlockLootFunction;

public class LootContent {
    public static final LootFunctionType<NbtBlockLootFunction> NBT_BLOCK_LOOT_FUNCTION = new LootFunctionType<>(NbtBlockLootFunction.CODEC);

    public static void init() {
        Registry.register(Registries.LOOT_FUNCTION_TYPE, Oritech.id("nbt_block_loot_function"), NBT_BLOCK_LOOT_FUNCTION);
    }
}
