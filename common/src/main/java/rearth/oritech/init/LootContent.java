package rearth.oritech.init;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import rearth.oritech.util.NbtBlockLootFunction;
import rearth.oritech.util.registry.OritechDeferredRegistry;

public class LootContent {

    public static final OritechDeferredRegistry<LootItemFunctionType<?>> LOOT_FUNCTIONS = OritechDeferredRegistry.create(Registries.LOOT_FUNCTION_TYPE);
    
    public static final RegistrySupplier<LootItemFunctionType<?>> NBT_BLOCK_LOOT_FUNCTION = LOOT_FUNCTIONS.register("nbt_block_loot_function", () -> new LootItemFunctionType<>(NbtBlockLootFunction.CODEC));

    public static void register() {
        LOOT_FUNCTIONS.register();
    }
}
