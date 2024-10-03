package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.init.datagen.data.TagContent;
import techreborn.TechReborn;
import techreborn.init.TRContent;

import java.util.concurrent.CompletableFuture;

public class ItemTagGenerator extends FabricTagProvider.ItemTagProvider {
    
    public ItemTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }
    
    @Override
    protected void configure(RegistryWrapper.WrapperLookup lookup) {
    
        // raw ores
        getOrCreateTagBuilder(ConventionalItemTags.RAW_MATERIALS)
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
          .addOptionalTag(ConventionalItemTags.CROPS)
          .addOptional(Identifier.of(TechReborn.MOD_ID, TRContent.Parts.PLANTBALL.name))
          .add(BlockContent.WITHER_CROP_BLOCK.asItem())
          .add(ItemContent.BANANA.asItem())
          .add(Items.WHEAT)
          .add(Items.DRIED_KELP)
          .add(Items.SHORT_GRASS)
          .add(Items.KELP)
          .add(Items.SEAGRASS)
          .add(Items.MOSS_CARPET)
          .add(Items.SMALL_DRIPLEAF)
          .add(Items.HANGING_ROOTS)
          .add(Items.MANGROVE_ROOTS)
          .add(Items.PITCHER_POD)
          .add(Items.TALL_GRASS)
          .add(Items.VINE)
          .add(Items.NETHER_SPROUTS)
          .add(Items.WEEPING_VINES)
          .add(Items.TWISTING_VINES)
          .add(Items.GLOW_LICHEN)
          .add(Items.SEA_PICKLE)
          .add(Items.LILY_PAD)
          .add(Items.BROWN_MUSHROOM)
          .add(Items.RED_MUSHROOM)
          .add(Items.MUSHROOM_STEM)
          .add(Items.CRIMSON_FUNGUS)
          .add(Items.WARPED_FUNGUS)
          .add(Items.NETHER_WART)
          .add(Items.CRIMSON_ROOTS)
          .add(Items.WARPED_ROOTS)
          .add(Items.SHROOMLIGHT)
          .add(Items.FERN)
          .add(Items.LARGE_FERN)
          .add(Items.MOSS_BLOCK)
          .add(Items.BIG_DRIPLEAF)
          .add(Items.BROWN_MUSHROOM_BLOCK)
          .add(Items.RED_MUSHROOM_BLOCK)
          .add(Items.NETHER_WART_BLOCK)
          .add(Items.WARPED_WART_BLOCK);
        
        // dyes
        getOrCreateTagBuilder(TagContent.RAW_WHITE_DYE)
          .add(Items.BONE_MEAL);
        getOrCreateTagBuilder(TagContent.RAW_LIGHT_GRAY_DYE)
          .add(Items.AZURE_BLUET)
          .add(Items.OXEYE_DAISY)
          .add(Items.WHITE_TULIP);
        getOrCreateTagBuilder(TagContent.RAW_BLACK_DYE)
          .add(Items.INK_SAC)
          .add(Items.WITHER_ROSE);
        getOrCreateTagBuilder(TagContent.RAW_RED_DYE)
          .add(Items.POPPY)
          .add(Items.RED_TULIP)
          .add(Items.ROSE_BUSH);
        getOrCreateTagBuilder(TagContent.RAW_ORANGE_DYE)
          .add(Items.ORANGE_TULIP)
          .add(Items.TORCHFLOWER);
        getOrCreateTagBuilder(TagContent.RAW_YELLOW_DYE)
          .add(Items.DANDELION)
          .add(Items.SUNFLOWER);
        getOrCreateTagBuilder(TagContent.RAW_CYAN_DYE)
          .add(Items.PITCHER_PLANT);
        getOrCreateTagBuilder(TagContent.RAW_BLUE_DYE)
          .add(Items.LAPIS_LAZULI)
          .add(Items.CORNFLOWER);
        getOrCreateTagBuilder(TagContent.RAW_MAGENTA_DYE)
          .add(Items.ALLIUM)
          .add(Items.LILAC);
        getOrCreateTagBuilder(TagContent.RAW_PINK_DYE)
          .add(Items.PINK_TULIP)
          .add(Items.PEONY)
          .add(Items.PINK_PETALS);
        
        // plating variants
        getOrCreateTagBuilder(TagContent.MACHINE_PLATING)
          .add(BlockContent.MACHINE_PLATING_BLOCK.asItem())
          .add(BlockContent.IRON_PLATING_BLOCK.asItem())
          .add(BlockContent.NICKEL_PLATING_BLOCK.asItem());
        
        // silicon
        getOrCreateTagBuilder(TagContent.SILICON)
          .add(ItemContent.SILICON);
        
        // carbon fibre
        getOrCreateTagBuilder(TagContent.CARBON_FIBRE)
          .add(ItemContent.CARBON_FIBRE_STRANDS)
          .addOptional(Identifier.of(TechReborn.MOD_ID, TRContent.Parts.CARBON_FIBER.name));
        
        // wires
        getOrCreateTagBuilder(TagContent.WIRES)
          .add(ItemContent.INSULATED_WIRE);
        
        // equipment enchanting
        getOrCreateTagBuilder(ItemTags.SWORDS)
          .add(ToolsContent.CHAINSAW, ToolsContent.PROMETHIUM_AXE);
        getOrCreateTagBuilder(ItemTags.AXES)
          .add(ToolsContent.CHAINSAW, ToolsContent.PROMETHIUM_AXE);
        
        getOrCreateTagBuilder(ItemTags.PICKAXES)
          .add(ToolsContent.HAND_DRILL, ToolsContent.PROMETHIUM_PICKAXE);
        getOrCreateTagBuilder(ItemTags.SHOVELS)
          .add(ToolsContent.HAND_DRILL, ToolsContent.PROMETHIUM_PICKAXE);
        
        getOrCreateTagBuilder(ItemTags.CLUSTER_MAX_HARVESTABLES)
          .add(ToolsContent.HAND_DRILL, ToolsContent.PROMETHIUM_PICKAXE);
        
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
