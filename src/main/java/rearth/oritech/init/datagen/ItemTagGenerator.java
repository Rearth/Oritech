package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.init.datagen.data.TagContent;

import java.util.concurrent.CompletableFuture;

public class ItemTagGenerator extends FabricTagProvider.ItemTagProvider {
    
    public ItemTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }
    
    @Override
    protected void configure(RegistryWrapper.WrapperLookup lookup) {
    
        // raw ores
        getOrCreateTagBuilder(ConventionalItemTags.RAW_ORES)
          .add(ItemContent.RAW_NICKEL)
          .add(ItemContent.RAW_PLATINUM);
        
        // dusts
        getOrCreateTagBuilder(ConventionalItemTags.DUSTS)
          .add(ItemContent.NICKEL_DUST)
          .add(ItemContent.PLATINUM_DUST)
          .add(ItemContent.BIOSTEEL_DUST)
          .add(ItemContent.DURATIUM_DUST)
          .add(ItemContent.ELECTRUM_DUST)
          .add(ItemContent.ADAMANT_DUST)
          .add(ItemContent.ENERGITE_DUST)
          .add(ItemContent.STEEL_DUST);
        
        // ingots
        getOrCreateTagBuilder(ConventionalItemTags.INGOTS)
          .add(ItemContent.NICKEL_INGOT)
          .add(ItemContent.PLATINUM_INGOT)
          .add(ItemContent.BIOSTEEL_INGOT)
          .add(ItemContent.PROMETHEUM_INGOT)
          .add(ItemContent.DURATIUM_INGOT)
          .add(ItemContent.ELECTRUM_INGOT)
          .add(ItemContent.ADAMANT_INGOT)
          .add(ItemContent.ENERGITE_INGOT)
          .add(ItemContent.STEEL_INGOT);
        
        getOrCreateTagBuilder(TagContent.NICKEL_ORES).add(BlockContent.NICKEL_ORE.asItem(), BlockContent.DEEPSLATE_NICKEL_ORE.asItem());
        getOrCreateTagBuilder(TagContent.PLATINUM_ORES).add(BlockContent.DEEPSLATE_PLATINUM_ORE.asItem(), BlockContent.ENDSTONE_PLATINUM_ORE.asItem());
        
        getOrCreateTagBuilder(TagContent.STEEL_INGOTS).add(ItemContent.STEEL_INGOT).add(ItemContent.BIOSTEEL_INGOT);
        getOrCreateTagBuilder(TagContent.QUARTZ_DUSTS).add(ItemContent.QUARTZ_DUST);
        getOrCreateTagBuilder(TagContent.COAL_DUSTS).add(ItemContent.COAL_DUST);
        
        // vanilla variants
        getOrCreateTagBuilder(TagContent.COPPER_DUSTS).add(ItemContent.COPPER_DUST);
        getOrCreateTagBuilder(TagContent.COPPER_NUGGETS).add(ItemContent.COPPER_NUGGET);
        getOrCreateTagBuilder(TagContent.IRON_DUSTS).add(ItemContent.IRON_DUST);
        getOrCreateTagBuilder(TagContent.GOLD_DUSTS).add(ItemContent.GOLD_DUST);
        
        // custom ores
        getOrCreateTagBuilder(TagContent.NICKEL_RAW_ORES).add(ItemContent.RAW_NICKEL);
        getOrCreateTagBuilder(TagContent.NICKEL_DUSTS).add(ItemContent.NICKEL_DUST);
        getOrCreateTagBuilder(TagContent.NICKEL_NUGGETS).add(ItemContent.NICKEL_NUGGET);
        getOrCreateTagBuilder(TagContent.NICKEL_INGOTS).add(ItemContent.NICKEL_INGOT);
        
        getOrCreateTagBuilder(TagContent.PLATINUM_RAW_ORES).add(ItemContent.RAW_PLATINUM);
        getOrCreateTagBuilder(TagContent.PLATINUM_DUSTS).add(ItemContent.PLATINUM_DUST);
        getOrCreateTagBuilder(TagContent.PLATINUM_NUGGETS).add(ItemContent.PLATINUM_NUGGET);
        getOrCreateTagBuilder(TagContent.PLATINUM_INGOTS).add(ItemContent.PLATINUM_INGOT);
        
        // biomass
        getOrCreateTagBuilder(TagContent.BIOMASS)
          .addOptionalTag(ItemTags.VILLAGER_PLANTABLE_SEEDS)
          .addOptionalTag(ItemTags.SAPLINGS)
          .addOptionalTag(ConventionalItemTags.FOODS)
          .add(BlockContent.WITHER_CROP_BLOCK.asItem())
          .add(Items.WHEAT);
        
        // plating variants
        getOrCreateTagBuilder(TagContent.MACHINE_PLATING)
          .add(BlockContent.MACHINE_PLATING_BLOCK.asItem())
          .add(BlockContent.IRON_PLATING_BLOCK.asItem())
          .add(BlockContent.NICKEL_PLATING_BLOCK.asItem());
        
        // silicon
        getOrCreateTagBuilder(TagContent.SILICON)
          .add(ItemContent.SILICON);
        
        // equipment enchanting
        getOrCreateTagBuilder(ItemTags.MINING_ENCHANTABLE)
          .add(ToolsContent.HAND_DRILL, ToolsContent.PROMETHIUM_PICKAXE);
        
        getOrCreateTagBuilder(ItemTags.CLUSTER_MAX_HARVESTABLES)
          .add(ToolsContent.HAND_DRILL, ToolsContent.PROMETHIUM_PICKAXE);
        
        getOrCreateTagBuilder(ItemTags.MINING_LOOT_ENCHANTABLE)
          .add(ToolsContent.HAND_DRILL, ToolsContent.PROMETHIUM_PICKAXE);
        
        getOrCreateTagBuilder(ItemTags.SWORD_ENCHANTABLE)
          .add(ToolsContent.PROMETHIUM_AXE, ToolsContent.CHAINSAW);
        getOrCreateTagBuilder(ItemTags.WEAPON_ENCHANTABLE)
          .add(ToolsContent.PROMETHIUM_AXE, ToolsContent.CHAINSAW);
        
        getOrCreateTagBuilder(ItemTags.DURABILITY_ENCHANTABLE)
          .add(ToolsContent.CHAINSAW, ToolsContent.HAND_DRILL);
        
        getOrCreateTagBuilder(ItemTags.HEAD_ARMOR_ENCHANTABLE)
          .add(ToolsContent.EXO_HELMET);
        getOrCreateTagBuilder(ItemTags.CHEST_ARMOR_ENCHANTABLE)
          .add(ToolsContent.EXO_CHESTPLATE);
        getOrCreateTagBuilder(ItemTags.LEG_ARMOR_ENCHANTABLE)
          .add(ToolsContent.EXO_LEGGINGS);
        getOrCreateTagBuilder(ItemTags.FOOT_ARMOR_ENCHANTABLE)
          .add(ToolsContent.EXO_BOOTS);
        
    }
}
