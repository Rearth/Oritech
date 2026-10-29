package rearth.oritech.spaceage.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import rearth.oritech.spaceage.simulation.DenseOres.Family;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.util.RegistryReflectionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SpaceAgeBlockLootProvider extends BlockLootSubProvider {

    public SpaceAgeBlockLootProvider(HolderLookup.Provider provider) {

        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, provider);
    }

    @SuppressWarnings("unchecked")
    private static List<Block> getAutoDropBlocks() {

        var blocks = new ArrayList<Block>();

        RegistryReflectionUtil.IterateFields(SpaceAgeBlocks.class, DeferredBlock.class, (field, identifier, value) -> {
            if (field.isAnnotationPresent(BlockContent.NoBlockItem.class)
                    || field.isAnnotationPresent(BlockContent.NoAutoDrop.class)) return;

            blocks.add(((DeferredBlock<? extends Block>) value).get());
        });

        return blocks;
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {

        var blocks = getAutoDropBlocks();
        blocks.add(SpaceAgeBlocks.CUBE_SAT.get());
        for (var family : Family.values()) blocks.add(family.block());
        return blocks;
    }

    @Override
    protected void generate() {

        getAutoDropBlocks().forEach(this::dropSelf);
        var fortune = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        for (var family : Family.values()) {
            var block = family.block();
            add(block, createSilkTouchDispatchTable(block, applyExplosionDecay(block, LootItem.lootTableItem(family.output())
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(30, 60)))
                    .apply(ApplyBonusCount.addOreBonusCount(fortune)))));
        }
        var cube = SpaceAgeBlocks.CUBE_SAT.get();
        add(cube, LootTable.lootTable().withPool(applyExplosionCondition(cube, LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(cube).apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(SpaceAgeComponents.CUBE_SAT.get()).include(DataComponents.CUSTOM_NAME))))));
    }
}
