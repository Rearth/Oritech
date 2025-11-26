package rearth.oritech.generator;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
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
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.util.NbtBlockLootFunction;

import java.util.concurrent.CompletableFuture;

public class BlockLootGenerator extends FabricBlockLootTableProvider {
    
    public static final ResourceKey<LootTable> JUNGLE_LEAVES_LOOT = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("jungle_leaves"));
    
    public BlockLootGenerator(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }
    
    @Override
    public void generate() {
        addOreDrop(BlockContent.NICKEL_ORE, ItemContent.RAW_NICKEL);
        addOreDrop(BlockContent.DEEPSLATE_NICKEL_ORE, ItemContent.RAW_NICKEL);
        addOreDrop(BlockContent.DEEPSLATE_PLATINUM_ORE, ItemContent.RAW_PLATINUM);
        addOreDrop(BlockContent.ENDSTONE_PLATINUM_ORE, ItemContent.RAW_PLATINUM);
        addOreDrop(BlockContent.DEEPSLATE_URANIUM_ORE, ItemContent.RAW_URANIUM);
        addOreDrop(BlockContent.URANIUM_CRYSTAL, ItemContent.RAW_URANIUM);
        
        for (var block : BlockContent.autoRegisteredDrops) {
            dropSelf(block);
        }
        
        dropOther(BlockContent.ITEM_PIPE_CONNECTION, BlockContent.ITEM_PIPE);
        dropOther(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION, BlockContent.TRANSPARENT_ITEM_PIPE);
        dropOther(BlockContent.FLUID_PIPE_CONNECTION, BlockContent.FLUID_PIPE);
        dropOther(BlockContent.ENERGY_PIPE_CONNECTION, BlockContent.ENERGY_PIPE);
        dropOther(BlockContent.SUPERCONDUCTOR_CONNECTION, BlockContent.SUPERCONDUCTOR);
        dropOther(BlockContent.SUPERCONDUCTOR_CONNECTION, BlockContent.SUPERCONDUCTOR);
        
        dropOther(BlockContent.FRAMED_ITEM_PIPE_CONNECTION, BlockContent.FRAMED_ITEM_PIPE);
        dropOther(BlockContent.FRAMED_FLUID_PIPE_CONNECTION, BlockContent.FRAMED_FLUID_PIPE);
        dropOther(BlockContent.FRAMED_ENERGY_PIPE_CONNECTION, BlockContent.FRAMED_ENERGY_PIPE);
        dropOther(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION, BlockContent.FRAMED_SUPERCONDUCTOR);
        
        addCustomDataDrop(BlockContent.SMALL_TANK_BLOCK);
        addCustomDataDrop(BlockContent.CREATIVE_TANK_BLOCK);
        addCustomDataDrop(BlockContent.SMALL_STORAGE_BLOCK);
        addCustomDataDrop(BlockContent.CREATIVE_STORAGE_BLOCK);
        addCustomDataDrop(BlockContent.MACHINE_COMBI_ADDON);
        
        LootItemCondition.Builder cropDropBuilder = LootItemBlockStatePropertyCondition.hasBlockStateProperties(BlockContent.WITHER_CROP_BLOCK)
                                                  .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CropBlock.AGE, 7));
        add(BlockContent.WITHER_CROP_BLOCK, createCropDrops(BlockContent.WITHER_CROP_BLOCK, Blocks.TWISTING_VINES_PLANT.asItem(), BlockContent.WITHER_CROP_BLOCK.asItem(), cropDropBuilder));
        
        HolderLookup.RegistryLookup<Enchantment> impl = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        // similar to BlockLootTableGenerator.oakLeavesDrops()
        add(Blocks.JUNGLE_LEAVES, this.createLeavesDrops(Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING, BlockLootSubProvider.NORMAL_LEAVES_SAPLING_CHANCES).withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(this.doesNotHaveShearsOrSilkTouch()).add(((LootPoolSingletonContainer.Builder) this.applyExplosionCondition(Blocks.JUNGLE_LEAVES, LootItem.lootTableItem(ItemContent.BANANA))).when(BonusLevelTableCondition.bonusLevelFlatChance(impl.getOrThrow(Enchantments.FORTUNE), new float[]{0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F})))));
    }
    
    private void addOreDrop(Block block, Item item) {
        add(block, createOreDrop(block, item));
    }
    
    private void addCustomDataDrop(Block block) {
        // similar to shulkerBoxDrops
        add(block, LootTable.lootTable().withPool(
          (LootPool.Builder) this.applyExplosionCondition(block, LootPool.lootPool()
                                                                         .setRolls(ConstantValue.exactly(1.0F))
                                                                         .add(LootItem.lootTableItem(block).apply(NbtBlockLootFunction.builder())))));
    }
}
