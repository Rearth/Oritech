package rearth.oritech.generator;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.ToolsContent;

import java.util.concurrent.CompletableFuture;

import static rearth.oritech.util.TagUtils.*;

public class ItemTagGenerator extends FabricTagProvider.ItemTagProvider {
    
    public ItemTagGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }
    
    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        
        // raw ores
        getOrCreateTagBuilder(ConventionalItemTags.RAW_MATERIALS)
          .add(ItemContent.RAW_NICKEL)
          .add(ItemContent.RAW_URANIUM)
          .add(ItemContent.RAW_PLATINUM);
        
        // clumps - added for Create and Mekanism compat support
        // Adding Create "crushed" ores as clumps, because they essentially are
        getOrCreateTagBuilder(TagContent.CLUMPS)
          .add(ItemContent.COPPER_CLUMP).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_copper"))
          .add(ItemContent.IRON_CLUMP).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_iron"))
          .add(ItemContent.GOLD_CLUMP).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_gold"))
          .add(ItemContent.NICKEL_CLUMP).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_nickel"))
          .add(ItemContent.PLATINUM_CLUMP).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_platinum"))
          .addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_zinc"));
        
        getOrCreateTagBuilder(getClumpTag("copper")).add(ItemContent.COPPER_CLUMP).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_copper"));
        getOrCreateTagBuilder(getClumpTag("iron")).add(ItemContent.IRON_CLUMP).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_iron"));
        getOrCreateTagBuilder(getClumpTag("gold")).add(ItemContent.GOLD_CLUMP).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_gold"));
        getOrCreateTagBuilder(getClumpTag("nickel")).add(ItemContent.NICKEL_CLUMP).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_nickel"));
        getOrCreateTagBuilder(getClumpTag("platinum")).add(ItemContent.PLATINUM_CLUMP).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_platinum"));
        // for compat
        getOrCreateTagBuilder(getClumpTag("zinc")).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_zinc"));
        getOrCreateTagBuilder(getClumpTag("uranium")).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_uranium"));
        getOrCreateTagBuilder(getClumpTag("osmium")).addOptional(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_osmium"));
        
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
          .add(ItemContent.COAL_DUST)
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
        getOrCreateTagBuilder(TagContent.COAL_DUSTS).add(ItemContent.COAL_DUST);
        
        
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
        
        getOrCreateTagBuilder(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TagUtil.C_TAG_NAMESPACE, "gems/fluxite")))
          .add(ItemContent.FLUXITE);
        
        getOrCreateTagBuilder(TagContent.NICKEL_ORES).add(BlockContent.NICKEL_ORE.asItem(), BlockContent.DEEPSLATE_NICKEL_ORE.asItem());
        getOrCreateTagBuilder(TagContent.PLATINUM_ORES).add(BlockContent.DEEPSLATE_PLATINUM_ORE.asItem(), BlockContent.ENDSTONE_PLATINUM_ORE.asItem());
        getOrCreateTagBuilder(TagContent.URANIUM_ORES).add(BlockContent.DEEPSLATE_URANIUM_ORE.asItem())
          .addOptional(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "ore_uranium"))
          .addOptional(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "deepslate_ore_uranium"));
        
        getOrCreateTagBuilder(TagContent.STEEL_INGOTS).add(ItemContent.STEEL_INGOT).add(ItemContent.BIOSTEEL_INGOT);
        getOrCreateTagBuilder(TagContent.QUARTZ_DUSTS).add(ItemContent.QUARTZ_DUST);
        
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
          .addOptional(ResourceLocation.fromNamespaceAndPath("relics", "infinity_ham"));
        
        getOrCreateTagBuilder(cItemTag("bananas")).add(ItemContent.BANANA);
        getOrCreateTagBuilder(cItemTag("foods/fruit")).add(ItemContent.BANANA);
        
        // biomass
        getOrCreateTagBuilder(TagContent.BIOMATTER)
          .addOptionalTag(ItemTags.VILLAGER_PLANTABLE_SEEDS)
          .addOptionalTag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "seeds")))
          .addOptionalTag(ItemTags.SAPLINGS)
          .addOptionalTag(ConventionalItemTags.FOODS)
          .addOptionalTag(ConventionalItemTags.CROPS)
          .addOptionalTag(ResourceLocation.fromNamespaceAndPath("farmersdelight", "wild_crops"))
          .addOptionalTag(ResourceLocation.fromNamespaceAndPath("createaddition", "plant_foods"))
          .addOptional(ResourceLocation.fromNamespaceAndPath("enderio", "plant_matter_green"))
          .addOptional(ResourceLocation.fromNamespaceAndPath("enderio", "plant_matter_brown"))
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
        
        getOrCreateTagBuilder(TagContent.BIOMASS)
          .add(ItemContent.BIOMASS)
          .addOptional(ResourceLocation.fromNamespaceAndPath("techreborn", "plantball"));
        getOrCreateTagBuilder(TagContent.BIOMASS_BLOCK)
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
          .add(BlockContent.CARBON_PLATING_BLOCK.asItem())
          .add(BlockContent.NICKEL_PLATING_BLOCK.asItem());
        
        // silicon
        getOrCreateTagBuilder(TagContent.SILICON)
          .add(ItemContent.SILICON);
        
        // plastic
        getOrCreateTagBuilder(TagContent.PLASTIC_PLATES)
          .add(ItemContent.PLASTIC_SHEET);
        getOrCreateTagBuilder(cItemTag("plastics"))
          .add(ItemContent.PLASTIC_SHEET);
        getOrCreateTagBuilder(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("pneumaticcraft", "plastic_sheets")))
          .add(ItemContent.PLASTIC_SHEET);
        
        // carbon fibre
        getOrCreateTagBuilder(TagContent.CARBON_FIBRE)
          .add(ItemContent.CARBON_FIBRE_STRANDS);
        
        // equipment enchanting
        getOrCreateTagBuilder(ItemTags.SWORDS)
          .add(ToolsContent.CHAINSAW, ToolsContent.PROMETHIUM_AXE, ToolsContent.PORTABLE_LASER);
        getOrCreateTagBuilder(ItemTags.AXES)
          .add(ToolsContent.CHAINSAW, ToolsContent.PROMETHIUM_AXE, ToolsContent.PORTABLE_LASER);
        
        getOrCreateTagBuilder(ItemTags.PICKAXES)
          .add(ToolsContent.HAND_DRILL, ToolsContent.PROMETHIUM_PICKAXE, ToolsContent.PORTABLE_LASER);
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
          .add(ToolsContent.ELECTRIC_MACE)
          .add(ItemContent.WRENCH);
        
        getOrCreateTagBuilder(cItemTag("tools/wrench"))
          .add(ItemContent.WRENCH);
        
        getOrCreateTagBuilder(TagContent.REACTOR_COOLANT)
          .add(Items.ICE, Items.BLUE_ICE, Items.PACKED_ICE);
        
        getOrCreateTagBuilder(ItemTags.MACE_ENCHANTABLE)
          .add(ToolsContent.ELECTRIC_MACE);
        
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
        
        getOrCreateTagBuilder(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("industrialforegoing", "bioreactor")))
          .addTag(TagContent.BIOMASS);

        // recycling
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_NETHERITE_SCRAP)
          .add(Items.NETHERITE_AXE)
          .add(Items.NETHERITE_BOOTS)
          .add(Items.NETHERITE_CHESTPLATE)
          .add(Items.NETHERITE_HELMET)
          .add(Items.NETHERITE_LEGGINGS)
          .add(Items.NETHERITE_PICKAXE)
          .add(Items.NETHERITE_SHOVEL)
          .add(Items.NETHERITE_SWORD);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_DIAMOND)
          .add(Items.DIAMOND_AXE)
          .add(Items.DIAMOND_BOOTS)
          .add(Items.DIAMOND_CHESTPLATE)
          .add(Items.DIAMOND_HELMET)
          .add(Items.DIAMOND_HOE)
          .add(Items.DIAMOND_HORSE_ARMOR)
          .add(Items.DIAMOND_LEGGINGS)
          .add(Items.DIAMOND_PICKAXE)
          .add(Items.DIAMOND_SHOVEL)
          .add(Items.DIAMOND_SWORD);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_IRON_DUST)
          .add(Items.BUCKET)  
          .add(Items.CAULDRON)
          .add(Items.COMPASS)
          .add(Items.HOPPER)
          .add(Items.IRON_AXE)
          .add(Items.IRON_BOOTS)
          .add(Items.IRON_CHESTPLATE)
          .add(Items.IRON_HELMET)
          .add(Items.IRON_DOOR)
          .add(Items.IRON_HOE)
          .add(Items.IRON_HORSE_ARMOR)
          .add(Items.IRON_LEGGINGS)
          .add(Items.IRON_PICKAXE)
          .add(Items.IRON_SHOVEL)
          .add(Items.IRON_SWORD)
          .add(Items.IRON_TRAPDOOR)
          .add(Items.MINECART)
          .add(Items.SHEARS);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_SMALL_IRON_DUST)
          .add(Items.ACTIVATOR_RAIL)  
          .add(Items.CHAIN)
          .add(Items.DETECTOR_RAIL)
          .add(Items.FLINT_AND_STEEL)
          .add(Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
          .add(Items.IRON_BARS)
          .add(Items.RAIL)
          .add(Items.TRIPWIRE_HOOK);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_GOLD_DUST)
          .add(Items.BELL)
          .add(Items.CLOCK)
          .add(Items.GOLDEN_AXE)
          .add(Items.GOLDEN_BOOTS)
          .add(Items.GOLDEN_CHESTPLATE)
          .add(Items.GOLDEN_HELMET)
          .add(Items.GOLDEN_HOE)
          .add(Items.GOLDEN_HORSE_ARMOR)
          .add(Items.GOLDEN_LEGGINGS)
          .add(Items.GOLDEN_PICKAXE)
          .add(Items.GOLDEN_SHOVEL)
          .add(Items.GOLDEN_SWORD);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_SMALL_GOLD_DUST)
          .add(Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
          .add(Items.POWERED_RAIL);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_COPPER_DUST)
          .add(Items.CHISELED_COPPER).add(Items.EXPOSED_CHISELED_COPPER).add(Items.WEATHERED_CHISELED_COPPER).add(Items.OXIDIZED_CHISELED_COPPER)
          .add(Items.WAXED_CHISELED_COPPER).add(Items.WAXED_EXPOSED_CHISELED_COPPER).add(Items.WAXED_WEATHERED_CHISELED_COPPER).add(Items.WAXED_OXIDIZED_CHISELED_COPPER)
          .add(Items.COPPER_GRATE).add(Items.EXPOSED_COPPER_GRATE).add(Items.WEATHERED_COPPER_GRATE).add(Items.OXIDIZED_COPPER_GRATE)
          .add(Items.WAXED_COPPER_GRATE).add(Items.WAXED_EXPOSED_COPPER_GRATE).add(Items.WAXED_WEATHERED_COPPER_GRATE).add(Items.WAXED_OXIDIZED_COPPER_GRATE)
          .add(Items.CUT_COPPER).add(Items.EXPOSED_CUT_COPPER).add(Items.WEATHERED_CUT_COPPER).add(Items.OXIDIZED_CUT_COPPER)
          .add(Items.WAXED_CUT_COPPER).add(Items.WAXED_EXPOSED_CUT_COPPER).add(Items.WAXED_WEATHERED_CUT_COPPER).add(Items.WAXED_OXIDIZED_CUT_COPPER)
          .add(Items.CUT_COPPER_SLAB).add(Items.EXPOSED_CUT_COPPER_SLAB).add(Items.WEATHERED_CUT_COPPER_SLAB).add(Items.OXIDIZED_CUT_COPPER_SLAB)
          .add(Items.WAXED_CUT_COPPER_SLAB).add(Items.WAXED_EXPOSED_CUT_COPPER_SLAB).add(Items.WAXED_WEATHERED_CUT_COPPER_SLAB).add(Items.WAXED_OXIDIZED_CUT_COPPER_SLAB)
          .add(Items.CUT_COPPER_STAIRS).add(Items.EXPOSED_CUT_COPPER_STAIRS).add(Items.WEATHERED_CUT_COPPER_STAIRS).add(Items.OXIDIZED_CUT_COPPER_STAIRS)
          .add(Items.WAXED_CUT_COPPER_STAIRS).add(Items.WAXED_EXPOSED_CUT_COPPER_STAIRS).add(Items.WAXED_WEATHERED_CUT_COPPER_STAIRS).add(Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS)
          .add(Items.COPPER_DOOR).add(Items.EXPOSED_COPPER_DOOR).add(Items.WEATHERED_COPPER_DOOR).add(Items.OXIDIZED_COPPER_DOOR)
          .add(Items.WAXED_COPPER_DOOR).add(Items.WAXED_EXPOSED_COPPER_DOOR).add(Items.WAXED_WEATHERED_COPPER_DOOR).add(Items.WAXED_OXIDIZED_COPPER_DOOR)
          .add(Items.COPPER_TRAPDOOR).add(Items.EXPOSED_COPPER_TRAPDOOR).add(Items.WEATHERED_COPPER_TRAPDOOR).add(Items.OXIDIZED_COPPER_TRAPDOOR)
          .add(Items.WAXED_COPPER_TRAPDOOR).add(Items.WAXED_EXPOSED_COPPER_TRAPDOOR).add(Items.WAXED_WEATHERED_COPPER_TRAPDOOR).add(Items.WAXED_OXIDIZED_COPPER_TRAPDOOR)
          .add(Items.COPPER_BULB).add(Items.EXPOSED_COPPER_BULB).add(Items.WEATHERED_COPPER_BULB).add(Items.OXIDIZED_COPPER_BULB)
          .add(Items.WAXED_COPPER_BULB).add(Items.WAXED_EXPOSED_COPPER_BULB).add(Items.WAXED_WEATHERED_COPPER_BULB).add(Items.WAXED_OXIDIZED_COPPER_BULB);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_SMALL_COPPER_DUST)
          .add(Items.LIGHTNING_ROD);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_2_QUARTZ_DUST)
          .add(Items.QUARTZ_SLAB).add(Items.SMOOTH_QUARTZ_SLAB);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_4_QUARTZ_DUST)
          .add(Items.QUARTZ_BLOCK).add(Items.CHISELED_QUARTZ_BLOCK).add(Items.SMOOTH_QUARTZ)
          .add(Items.QUARTZ_BRICKS)
          .add(Items.QUARTZ_PILLAR)
          .add(Items.QUARTZ_STAIRS).add(Items.SMOOTH_QUARTZ_STAIRS);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_REDSTONE_DUST)
          .add(Items.REPEATER)
          .add(Items.COMPARATOR)
          .add(Items.REDSTONE_TORCH)
          .add(Items.TARGET);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_GRAVEL)
          .add(Items.STONE_AXE)
          .add(Items.STONE_BUTTON)
          .add(Items.STONE_HOE)
          .add(Items.STONE_PICKAXE)
          .add(Items.STONE_PRESSURE_PLATE)
          .add(Items.STONE_SHOVEL)
          .add(Items.STONE_SWORD)
          .add(Items.FURNACE)
          .add(Items.SMOKER)
          .add(Items.BLAST_FURNACE)
          .add(Items.DISPENSER)
          .add(Items.DROPPER)
          .add(Items.OBSERVER)
          .add(Items.CHISELED_STONE_BRICKS)
          .add(Items.COBBLESTONE_STAIRS).add(Items.COBBLESTONE_WALL)
          .add(Items.CRACKED_STONE_BRICKS)
          .add(Items.MOSSY_COBBLESTONE_SLAB).add(Items.MOSSY_COBBLESTONE_STAIRS).add(Items.MOSSY_COBBLESTONE_WALL)
          .add(Items.MOSSY_STONE_BRICKS).add(Items.MOSSY_STONE_BRICK_STAIRS).add(Items.MOSSY_STONE_BRICK_WALL)
          .add(Items.SMOOTH_STONE)
          .add(Items.STONE).add(Items.STONE_STAIRS)
          .add(Items.STONE_BRICKS).add(Items.STONE_BRICK_STAIRS).add(Items.STONE_BRICK_WALL);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_SAND)
          .add(Items.SANDSTONE_STAIRS).add(Items.SANDSTONE_WALL)
          .add(Items.GRAVEL)
          .add(Items.SMOOTH_SANDSTONE_STAIRS);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_RED_SAND)
          .add(Items.RED_SANDSTONE_STAIRS).add(Items.RED_SANDSTONE_WALL)
          .add(Items.SMOOTH_RED_SANDSTONE_STAIRS);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_STRING)
          .addOptionalTag(ItemTags.WOOL_CARPETS)
          .addOptionalTag(ItemTags.BANNERS)
          .add(Items.PAINTING);
        getOrCreateTagBuilder(TagContent.RECYCLES_TO_BIOMASS)
          .add(Items.SADDLE)
          .add(Items.LEATHER)
          .add(Items.LEATHER_BOOTS)
          .add(Items.LEATHER_CHESTPLATE)
          .add(Items.LEATHER_HELMET)
          .add(Items.LEATHER_HORSE_ARMOR)
          .add(Items.LEATHER_LEGGINGS)
          .add(Items.RABBIT_FOOT)
          .add(Items.RABBIT_HIDE);
    }
}
