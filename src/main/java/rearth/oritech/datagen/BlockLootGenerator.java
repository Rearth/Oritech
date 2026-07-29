package rearth.oritech.datagen;

import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.registries.DeferredBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.util.NbtBlockLootFunction;
import rearth.oritech.util.RegistryReflectionUtil;

import java.util.ArrayList;
import java.util.Set;

public class BlockLootGenerator extends BlockLootSubProvider {

    public BlockLootGenerator(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, provider);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {

        var blackList = Set.of(
                BlockContent.BLACK_HOLE_OUTER.get(),
                BlockContent.BLACK_HOLE_MIDDLE.get(),
                BlockContent.BLACK_HOLE_INNER.get(),
                BlockContent.PUMP_TRUNK.get(),
                BlockContent.QUARRY_BEAM_RING.get(),
                BlockContent.ADDON_INDICATOR.get(),
                BlockContent.REACTOR_COLD_INDICATOR.get(),
                BlockContent.REACTOR_MEDIUM_INDICATOR.get(),
                BlockContent.REACTOR_HOT_INDICATOR.get(),
                BlockContent.REACTOR_EXPLOSION_SMALL.get(),
                BlockContent.REACTOR_EXPLOSION_MEDIUM.get(),
                BlockContent.REACTOR_EXPLOSION_LARGE.get(),
                BlockContent.COMPLEX_PLATING.get(),
                BlockContent.SCHRODINGERS_SAFE.get(),
                BlockContent.BLOCK_DESTROYER_HEAD.get(),
                BlockContent.BLOCK_FERTILIZER_HEAD.get(),
                BlockContent.BLOCK_PLACER_HEAD.get(),
                BlockContent.INDUSTRIAL_DOOR_HINGE.get(),
                BlockContent.HANGAR_DOOR_HELPER.get(),
                BlockContent.FRAME_GANTRY_ARM.get()
        );

        var result = new ArrayList<Block>();
        result.add(Blocks.JUNGLE_LEAVES);

        RegistryReflectionUtil.IterateFields(BlockContent.class, DeferredBlock.class, ((field, identifier, value) -> {
            var holder = (DeferredBlock<Block>) value;
            if (blackList.contains(holder.get())) return;

            result.add(holder.get());

        }));

        return result;
    }

    @Override
    public void generate() {
        addOreDrop(BlockContent.NICKEL_ORE.get(), ItemContent.RAW_NICKEL.get());
        addOreDrop(BlockContent.DEEPSLATE_NICKEL_ORE.get(), ItemContent.RAW_NICKEL.get());
        addOreDrop(BlockContent.DEEPSLATE_PLATINUM_ORE.get(), ItemContent.RAW_PLATINUM.get());
        addOreDrop(BlockContent.ENDSTONE_PLATINUM_ORE.get(), ItemContent.RAW_PLATINUM.get());
        addOreDrop(BlockContent.DEEPSLATE_URANIUM_ORE.get(), ItemContent.RAW_URANIUM.get());
        addOreDrop(BlockContent.URANITE_CRYSTAL.get(), ItemContent.RAW_URANIUM.get());

        RegistryReflectionUtil.IterateFields(BlockContent.class, DeferredBlock.class, ((field, identifier, value) -> {
            if (field.isAnnotationPresent(BlockContent.NoAutoDrop.class) || field.isAnnotationPresent(BlockContent.NoBlockItem.class))
                return;

            var holder = (DeferredBlock<Block>) value;

            dropSelf(holder.get());

        }));

        dropOther(BlockContent.ITEM_PIPE_CONNECTION.get(), BlockContent.ITEM_PIPE);
        dropOther(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.get(), BlockContent.TRANSPARENT_ITEM_PIPE);
        dropOther(BlockContent.FLUID_PIPE_CONNECTION.get(), BlockContent.FLUID_PIPE);
        dropOther(BlockContent.ENERGY_PIPE_CONNECTION.get(), BlockContent.ENERGY_PIPE);
        dropOther(BlockContent.SUPERCONDUCTOR_CONNECTION.get(), BlockContent.SUPERCONDUCTOR);
        dropOther(BlockContent.SUPERCONDUCTOR_CONNECTION.get(), BlockContent.SUPERCONDUCTOR);

        dropOther(BlockContent.TAINTED_REFINERY.get(), BlockContent.REFINERY);

        dropOther(BlockContent.FRAMED_ITEM_PIPE_CONNECTION.get(), BlockContent.FRAMED_ITEM_PIPE);
        dropOther(BlockContent.FRAMED_FLUID_PIPE_CONNECTION.get(), BlockContent.FRAMED_FLUID_PIPE);
        dropOther(BlockContent.FRAMED_ENERGY_PIPE_CONNECTION.get(), BlockContent.FRAMED_ENERGY_PIPE);
        dropOther(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION.get(), BlockContent.FRAMED_SUPERCONDUCTOR);

        addCustomDataDrop(BlockContent.PORTABLE_TANK.get());
        addCustomDataDrop(BlockContent.CREATIVE_TANK.get());
        addCustomDataDrop(BlockContent.PORTABLE_ENERGY_STORAGE.get());
        addCustomDataDrop(BlockContent.CREATIVE_STORAGE.get());
        addCustomDataDrop(BlockContent.HEART_OF_THE_MACHINE_ADDON.get());

        add(BlockContent.CARBON_PLATING_SLAB.get(), createSlabItemTable(BlockContent.CARBON_PLATING_SLAB.get()));
        add(BlockContent.NICKEL_PLATING_SLAB.get(), createSlabItemTable(BlockContent.NICKEL_PLATING_SLAB.get()));
        add(BlockContent.COPPER_REINFORCED_PLATING_SLAB.get(), createSlabItemTable(BlockContent.COPPER_REINFORCED_PLATING_SLAB.get()));
        add(BlockContent.IRON_PLATING_SLAB.get(), createSlabItemTable(BlockContent.IRON_PLATING_SLAB.get()));

        LootItemCondition.Builder cropDropBuilder = LootItemBlockStatePropertyCondition.hasBlockStateProperties(BlockContent.SOUL_FLOWERS.get()).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CropBlock.AGE, 7));
        add(BlockContent.SOUL_FLOWERS.get(), createCropDrops(BlockContent.SOUL_FLOWERS.get(), BlockContent.SOUL_FLOWERS.asItem(), BlockContent.SOUL_FLOWERS.asItem(), cropDropBuilder));
//
        var impl = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        // similar to BlockLootTableGenerator.oakLeavesDrops()
        add(Blocks.JUNGLE_LEAVES,
                this.createLeavesDrops(Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING, BlockLootSubProvider.NORMAL_LEAVES_SAPLING_CHANCES)
                        .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                                .when(this.doesNotHaveShearsOrSilkTouch()).add(((LootPoolSingletonContainer.Builder) this.applyExplosionCondition(Blocks.JUNGLE_LEAVES, LootItem.lootTableItem(ItemContent.BANANA))).when(BonusLevelTableCondition.bonusLevelFlatChance(impl.getOrThrow(Enchantments.FORTUNE), new float[]{0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F})))));
    }

    private void addOreDrop(Block block, Item item) {
        add(block, createOreDrop(block, item));
    }

    private void addCustomDataDrop(Block block) {
        // similar to shulkerBoxDrops
        add(block, LootTable.lootTable().withPool((LootPool.Builder) this.applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(block).apply(NbtBlockLootFunction.builder())))));
    }
}
