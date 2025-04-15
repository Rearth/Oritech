package rearth.oritech.neoforgegen.datagen.loot;

import com.enderio.base.api.EnderIO;
import com.enderio.base.common.init.EIOItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import rearth.oritech.Oritech;
import rearth.oritech.init.ItemContent;

import java.util.List;
import java.util.function.BiConsumer;

public class FireCraftingLootProvider implements LootTableSubProvider {
    private final HolderLookup.Provider registries;

    public static ResourceKey<LootTable> SCULK_CRAFTING = ResourceKey.create(Registries.LOOT_TABLE, Oritech.id("compat/enderio/firecrafting/sculk"));

    public FireCraftingLootProvider(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        LootTable.Builder sculk = LootTable
            .lootTable()
            .withPool(LootPool
                .lootPool()
                .name("enderic_compound_in_world_crafting")
                .setRolls(UniformGenerator.between(1.0f, 3.0f))
                .add(LootItem.lootTableItem(ItemContent.ENDERIC_COMPOUND).when(LootItemRandomChanceCondition.randomChance(0.4f))))
            .setParamSet(LootContextParamSet.builder().build());

        writer.accept(SCULK_CRAFTING, sculk);
    }
}