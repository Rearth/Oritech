package rearth.oritech.fabricgen.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.ToolsContent;

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
          .add(ItemContent.RAW_URANIUM)
          .add(ItemContent.RAW_PLATINUM);
        
        // clumps - added for Create and Mekanism compat support
        // Adding Create "crushed" ores as clumps, because they essentially are
        getOrCreateTagBuilder(TagContent.CLUMPS)
          .add(ItemContent.COPPER_CLUMP).addOptional(Identifier.of("create", "crushed_raw_copper"))
          .add(ItemContent.IRON_CLUMP).addOptional(Identifier.of("create", "crushed_raw_iron"))
          .add(ItemContent.GOLD_CLUMP).addOptional(Identifier.of("create", "crushed_raw_gold"))
          .add(ItemContent.NICKEL_CLUMP).addOptional(Identifier.of("create", "crushed_raw_nickel"))
          .add(ItemContent.PLATINUM_CLUMP).addOptional(Identifier.of("create", "crushed_raw_platinum"));
        
        getOrCreateTagBuilder(getClumpTag("copper")).add(ItemContent.COPPER_CLUMP).addOptional(Identifier.of("create", "crushed_raw_copper"));
        getOrCreateTagBuilder(getClumpTag("iron")).add(ItemContent.IRON_CLUMP).addOptional(Identifier.of("create", "crushed_raw_iron"));
        getOrCreateTagBuilder(getClumpTag("gold")).add(ItemContent.GOLD_CLUMP).addOptional(Identifier.of("create", "crushed_raw_gold"));
        getOrCreateTagBuilder(getClumpTag("nickel")).add(ItemContent.NICKEL_CLUMP).addOptional(Identifier.of("create", "crushed_raw_nickel"));
        getOrCreateTagBuilder(getClumpTag("platinum")).add(ItemContent.PLATINUM_CLUMP).addOptional(Identifier.of("create", "crushed_raw_platinum"));
        // for compat
        getOrCreateTagBuilder(getClumpTag("uranium")).addOptional(Identifier.of("create", "crushed_raw_uranium"));
        getOrCreateTagBuilder(getClumpTag("osmium")).addOptional(Identifier.of("create", "crushed_raw_osmium"));
        
        // dusts
        getOrCreateTagBuilder(ConventionalItemTags.DUSTS)
          .add(ItemContent.NICKEL_DUST)
          .add(ItemContent.PLATINUM_DUST)
          .add(ItemContent.BIOSTEEL_DUST)
          .add(ItemContent.DURATIUM_DUST)
          .add(ItemContent.ELECTRUM_DUST)
          .add(ItemContent.ADAMANT_DUST)
          .add(ItemContent.ENERGITE_DUST)
          .add(ItemContent.URANIUM_DUST)
          .add(ItemContent.PLUTONIUM_DUST)
          .add(ItemContent.STEEL_DUST);
        
        getOrCreateTagBuilder(getDustTag("nickel")).add(ItemContent.NICKEL_DUST);
        getOrCreateTagBuilder(getDustTag("platinum")).add(ItemContent.PLATINUM_DUST);
        getOrCreateTagBuilder(getDustTag("biosteel")).add(ItemContent.BIOSTEEL_DUST);
        getOrCreateTagBuilder(getDustTag("duratium")).add(ItemContent.DURATIUM_DUST);
        getOrCreateTagBuilder(getDustTag("electrum")).add(ItemContent.ELECTRUM_DUST);
        getOrCreateTagBuilder(getDustTag("adamant")).add(ItemContent.ADAMANT_DUST);
        getOrCreateTagBuilder(getDustTag("energite")).add(ItemContent.ENERGITE_DUST);
        getOrCreateTagBuilder(getDustTag("steel")).add(ItemContent.STEEL_DUST);
        getOrCreateTagBuilder(getDustTag("uranium")).add(ItemContent.URANIUM_DUST);
        getOrCreateTagBuilder(getDustTag("plutonium")).add(ItemContent.PLUTONIUM_DUST);
        
        
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
        
        getOrCreateTagBuilder(getIngotTag("nickel")).add(ItemContent.NICKEL_INGOT);
        getOrCreateTagBuilder(getIngotTag("platinum")).add(ItemContent.PLATINUM_INGOT);
        getOrCreateTagBuilder(getIngotTag("biosteel")).add(ItemContent.BIOSTEEL_INGOT);
        getOrCreateTagBuilder(getIngotTag("prometheum")).add(ItemContent.PROMETHEUM_INGOT);
        getOrCreateTagBuilder(getIngotTag("duratium")).add(ItemContent.DURATIUM_INGOT);
        getOrCreateTagBuilder(getIngotTag("electrum")).add(ItemContent.ELECTRUM_INGOT);
        getOrCreateTagBuilder(getIngotTag("adamant")).add(ItemContent.ADAMANT_INGOT);
        getOrCreateTagBuilder(getIngotTag("energite")).add(ItemContent.ENERGITE_INGOT);
        getOrCreateTagBuilder(getIngotTag("steel")).add(ItemContent.STEEL_INGOT);
        
        
        // gems
        getOrCreateTagBuilder(ConventionalItemTags.GEMS)
          .add(ItemContent.FLUXITE);
        
        getOrCreateTagBuilder(TagKey.of(RegistryKeys.ITEM, Identifier.of(TagUtil.C_TAG_NAMESPACE, "gems/fluxite")))
          .add(ItemContent.FLUXITE);
        
        getOrCreateTagBuilder(TagContent.NICKEL_ORES).add(BlockContent.NICKEL_ORE.asItem(), BlockContent.DEEPSLATE_NICKEL_ORE.asItem());
        getOrCreateTagBuilder(TagContent.PLATINUM_ORES).add(BlockContent.DEEPSLATE_PLATINUM_ORE.asItem(), BlockContent.ENDSTONE_PLATINUM_ORE.asItem());
        getOrCreateTagBuilder(TagContent.URANIUM_ORES).add(BlockContent.DEEPSLATE_URANIUM_ORE.asItem())
          .addOptional(Identifier.of("immersiveengineering", "ore_uranium"))
          .addOptional(Identifier.of("immersiveengineering", "deepslate_ore_uranium"));
        
        getOrCreateTagBuilder(TagContent.STEEL_INGOTS).add(ItemContent.STEEL_INGOT).add(ItemContent.BIOSTEEL_INGOT);
        getOrCreateTagBuilder(TagContent.QUARTZ_DUSTS).add(ItemContent.QUARTZ_DUST);
        getOrCreateTagBuilder(TagContent.COAL_DUSTS).add(ItemContent.COAL_DUST).addOptional(Identifier.of("immersiveengineering", "dust_hop_graphite"));
        
        // vanilla variants
        getOrCreateTagBuilder(TagContent.COPPER_DUSTS).add(ItemContent.COPPER_DUST);
        getOrCreateTagBuilder(TagContent.COPPER_NUGGETS).add(ItemContent.COPPER_NUGGET);
        getOrCreateTagBuilder(TagContent.IRON_DUSTS).add(ItemContent.IRON_DUST);
        getOrCreateTagBuilder(TagContent.GOLD_DUSTS).add(ItemContent.GOLD_DUST);
        
        // custom ores
        getOrCreateTagBuilder(TagContent.NICKEL_RAW_MATERIALS).add(ItemContent.RAW_NICKEL);
        getOrCreateTagBuilder(TagContent.NICKEL_DUSTS).add(ItemContent.NICKEL_DUST);
        getOrCreateTagBuilder(TagContent.NICKEL_NUGGETS).add(ItemContent.NICKEL_NUGGET);
        getOrCreateTagBuilder(TagContent.NICKEL_INGOTS).add(ItemContent.NICKEL_INGOT);
        
        getOrCreateTagBuilder(TagContent.PLATINUM_RAW_MATERIALS).add(ItemContent.RAW_PLATINUM);
        getOrCreateTagBuilder(TagContent.PLATINUM_DUSTS).add(ItemContent.PLATINUM_DUST);
        getOrCreateTagBuilder(TagContent.PLATINUM_NUGGETS).add(ItemContent.PLATINUM_NUGGET);
        getOrCreateTagBuilder(TagContent.PLATINUM_INGOTS).add(ItemContent.PLATINUM_INGOT);
        
        getOrCreateTagBuilder(TagContent.URANIUM_RAW_MATERIALS).add(ItemContent.RAW_URANIUM);
        
        getOrCreateTagBuilder(TagContent.FEEDER_BLACKLIST)
          .addOptional(Identifier.of("relics", "infinity_ham"));
        
        getOrCreateTagBuilder(getItemTag("bananas")).add(ItemContent.BANANA);
        getOrCreateTagBuilder(getItemTag("foods/fruit")).add(ItemContent.BANANA);
        
        // biomass
        getOrCreateTagBuilder(TagContent.BIOMASS)
          .addOptionalTag(ItemTags.VILLAGER_PLANTABLE_SEEDS)
          .addOptionalTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "seeds")))
          .addOptionalTag(ItemTags.SAPLINGS)
          .addOptionalTag(ConventionalItemTags.FOODS)
          .addOptionalTag(ConventionalItemTags.CROPS)
          .addOptionalTag(Identifier.of("farmersdelight", "wild_crops"))
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
        
        getOrCreateTagBuilder(TagContent.BIOFUEL)
          .add(ItemContent.BIOMASS);
        getOrCreateTagBuilder(TagContent.BIOFUEL_BLOCK)
          .add(BlockContent.BIOMASS_BLOCK.asItem());
        
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
        
        // plastic
        getOrCreateTagBuilder(TagContent.PLASTIC_PLATES)
          .add(ItemContent.PLASTIC_SHEET);
        getOrCreateTagBuilder(getItemTag("plastics"))
          .add(ItemContent.PLASTIC_SHEET);
        
        // carbon fibre
        getOrCreateTagBuilder(TagContent.CARBON_FIBRE)
          .add(ItemContent.CARBON_FIBRE_STRANDS);
        
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
          .add(ToolsContent.JETPACK)
          .add(ToolsContent.JETPACK_ELYTRA)
          .add(ToolsContent.JETPACK_EXO_ELYTRA)
          .add(ToolsContent.EXO_JETPACK)
          .add(ToolsContent.EXO_CHESTPLATE);
        getOrCreateTagBuilder(ItemTags.LEG_ARMOR_ENCHANTABLE)
          .add(ToolsContent.EXO_LEGGINGS);
        getOrCreateTagBuilder(ItemTags.FOOT_ARMOR_ENCHANTABLE)
          .add(ToolsContent.EXO_BOOTS);
        
        getOrCreateTagBuilder(ItemTags.HEAD_ARMOR)
          .add(ToolsContent.EXO_HELMET);
        getOrCreateTagBuilder(ItemTags.CHEST_ARMOR)
          .add(ToolsContent.JETPACK)
          .add(ToolsContent.JETPACK_ELYTRA)
          .add(ToolsContent.JETPACK_EXO_ELYTRA)
          .add(ToolsContent.EXO_JETPACK)
          .add(ToolsContent.EXO_CHESTPLATE);
        getOrCreateTagBuilder(ItemTags.LEG_ARMOR)
          .add(ToolsContent.EXO_LEGGINGS);
        getOrCreateTagBuilder(ItemTags.FOOT_ARMOR)
          .add(ToolsContent.EXO_BOOTS);
        
        getOrCreateTagBuilder(ItemTags.DURABILITY_ENCHANTABLE)
          .add(ItemContent.WRENCH);
        
        getOrCreateTagBuilder(TagContent.REACTOR_COOLANT)
          .add(Items.ICE, Items.BLUE_ICE, Items.PACKED_ICE);
        
        // storage blocks
        getOrCreateTagBuilder(ConventionalItemTags.STORAGE_BLOCKS)
          .add(BlockContent.STEEL_BLOCK.asItem())
          .add(BlockContent.ENERGITE_BLOCK.asItem())
          .add(BlockContent.NICKEL_BLOCK.asItem())
          .add(BlockContent.BIOSTEEL_BLOCK.asItem())
          .add(BlockContent.PLATINUM_BLOCK.asItem())
          .add(BlockContent.ADAMANT_BLOCK.asItem())
          .add(BlockContent.ELECTRUM_BLOCK.asItem())
          .add(BlockContent.DURATIUM_BLOCK.asItem())
          .add(BlockContent.BIOMASS_BLOCK.asItem())
          .add(BlockContent.PLASTIC_BLOCK.asItem())
          .add(BlockContent.FLUXITE_BLOCK.asItem())
          .add(BlockContent.SILICON_BLOCK.asItem())
          .add(BlockContent.RAW_NICKEL_BLOCK.asItem())
          .add(BlockContent.RAW_PLATINUM_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("steel"))
          .add(BlockContent.STEEL_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("steel"))
          .add(BlockContent.STEEL_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("energite"))
          .add(BlockContent.ENERGITE_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("nickel"))
          .add(BlockContent.NICKEL_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("biosteel"))
          .add(BlockContent.BIOSTEEL_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("platinum"))
          .add(BlockContent.PLATINUM_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("adamant"))
          .add(BlockContent.ADAMANT_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("electrum"))
          .add(BlockContent.ELECTRUM_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("duratium"))
          .add(BlockContent.DURATIUM_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("biomass"))
          .add(BlockContent.BIOMASS_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("plastic"))
          .add(BlockContent.PLASTIC_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("fluxite"))
          .add(BlockContent.FLUXITE_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("silicon"))
          .add(BlockContent.SILICON_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("raw_nickel"))
          .add(BlockContent.RAW_NICKEL_BLOCK.asItem());
        
        getOrCreateTagBuilder(getStorageBlockTag("raw_platinum"))
          .add(BlockContent.RAW_PLATINUM_BLOCK.asItem());
        
        getOrCreateTagBuilder(TagKey.of(RegistryKeys.ITEM, Identifier.of("industrialforegoing", "bioreactor")))
          .addTag(TagContent.BIOFUEL);
    }
    
    public static TagKey<Item> getStorageBlockTag(String path) {
        return getItemTag("storage_blocks/" + path);
    }
    
    public static TagKey<Item> getIngotTag(String path) {
        return getItemTag("ingots/" + path);
    }

    public static TagKey<Item> getClumpTag(String path) {
        return getItemTag("clumps/" + path);
    }
    
    public static TagKey<Item> getDustTag(String path) {
        return getItemTag("dusts/" + path);
    }

    public static TagKey<Item> getItemTag(String path) {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of(TagUtil.C_TAG_NAMESPACE, path));
    }
}
