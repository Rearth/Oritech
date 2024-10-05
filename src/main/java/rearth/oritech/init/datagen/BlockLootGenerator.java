package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.data.server.loottable.BlockLootTableGenerator;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.TableBonusLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.datagen.loot.NbtBlockLootFunction;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class BlockLootGenerator extends FabricBlockLootTableProvider {
    
    public static Set<Block> autoRegisteredDrops = new HashSet<>();
    public static final RegistryKey<LootTable> JUNGLE_LEAVES_LOOT = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.ofVanilla("jungle_leaves"));
    
    public BlockLootGenerator(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }
    
    @Override
    public void generate() {
        addOreDrop(BlockContent.NICKEL_ORE, ItemContent.RAW_NICKEL);
        addOreDrop(BlockContent.DEEPSLATE_NICKEL_ORE, ItemContent.RAW_NICKEL);
        addOreDrop(BlockContent.DEEPSLATE_PLATINUM_ORE, ItemContent.RAW_PLATINUM);
        addOreDrop(BlockContent.ENDSTONE_PLATINUM_ORE, ItemContent.RAW_PLATINUM);
        
        for (var block : autoRegisteredDrops) {
            addDrop(block);
        }
        
        addDrop(BlockContent.ITEM_PIPE_CONNECTION, BlockContent.ITEM_PIPE);
        addDrop(BlockContent.FLUID_PIPE_CONNECTION, BlockContent.FLUID_PIPE);
        addDrop(BlockContent.ENERGY_PIPE_CONNECTION, BlockContent.ENERGY_PIPE);

        addCustomDataDrop(BlockContent.SMALL_TANK_BLOCK);
        addCustomDataDrop(BlockContent.CREATIVE_TANK_BLOCK);
        addCustomDataDrop(BlockContent.SMALL_STORAGE_BLOCK);
        addCustomDataDrop(BlockContent.CREATIVE_STORAGE_BLOCK);
        
        LootCondition.Builder cropDropBuilder = BlockStatePropertyLootCondition.builder(BlockContent.WITHER_CROP_BLOCK)
                                           .properties(StatePredicate.Builder.create().exactMatch(CropBlock.AGE, 7));
        addDrop(BlockContent.WITHER_CROP_BLOCK, cropDrops(BlockContent.WITHER_CROP_BLOCK, Blocks.TWISTING_VINES_PLANT.asItem(), BlockContent.WITHER_CROP_BLOCK.asItem(), cropDropBuilder));
        
        RegistryWrapper.Impl<Enchantment> impl = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
        // similar to BlockLootTableGenerator.oakLeavesDrops()
        addDrop(Blocks.JUNGLE_LEAVES, this.leavesDrops(Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING, BlockLootTableGenerator.SAPLING_DROP_CHANCE).pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1.0F)).conditionally(this.createWithoutShearsOrSilkTouchCondition()).with(((LeafEntry.Builder)this.addSurvivesExplosionCondition(Blocks.JUNGLE_LEAVES, ItemEntry.builder(ItemContent.BANANA))).conditionally(TableBonusLootCondition.builder(impl.getOrThrow(Enchantments.FORTUNE), new float[]{0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F})))));
    }
    
    private void addOreDrop(Block block, Item item) {
        addDrop(block, oreDrops(block, item));
    }

    private void addCustomDataDrop(Block block) {
        // similar to shulkerBoxDrops
        addDrop(block, LootTable.builder().pool(
            (LootPool.Builder)this.addSurvivesExplosionCondition(block, LootPool.builder()
                                  .rolls(ConstantLootNumberProvider.create(1.0F))
                                  .with(ItemEntry.builder(block).apply(NbtBlockLootFunction.builder())))));
    }
}
