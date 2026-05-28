package rearth.oritech.init;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;
import rearth.oritech.util.NbtBlockLootFunction;

import java.util.function.Supplier;

public class LootContent {
    
    public static final DeferredRegister<MapCodec<? extends LootItemFunction>> LOOT_FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Oritech.MOD_ID);
    
    public static final Supplier<MapCodec<NbtBlockLootFunction>> NBT_BLOCK_LOOT_FUNCTION = LOOT_FUNCTIONS.register(NbtBlockLootFunction.NAME, () -> NbtBlockLootFunction.CODEC);
}
