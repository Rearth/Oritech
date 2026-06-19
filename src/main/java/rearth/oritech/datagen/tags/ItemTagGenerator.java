package rearth.oritech.datagen.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.ToolsContent;

import java.util.concurrent.CompletableFuture;

import static rearth.oritech.util.TagUtils.*;

public class ItemTagGenerator extends ItemTagsProvider {
    
    public ItemTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture, Oritech.MOD_ID);
    }
    
    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        
        // raw ores
        this.tag(Tags.Items.RAW_MATERIALS)
          .add(ItemContent.RAW_NICKEL.get())
          .add(ItemContent.RAW_URANIUM.get())
          .add(ItemContent.RAW_PLATINUM.get());
        
        // clumps - added for Create and Mekanism compat support
        // Adding Create "crushed" ores as clumps, because they essentially are
        tag(TagContent.CLUMPS)
          .add(ItemContent.COPPER_CLUMP.get()).addOptionalTag(itemTag("create", "crushed_raw_copper"))
          .add(ItemContent.IRON_CLUMP.get()).addOptionalTag(itemTag("create", "crushed_raw_iron"))
          .add(ItemContent.GOLD_CLUMP.get()).addOptionalTag(itemTag("create", "crushed_raw_gold"))
          .add(ItemContent.NICKEL_CLUMP.get()).addOptionalTag(itemTag("create", "crushed_raw_nickel"))
          .add(ItemContent.PLATINUM_CLUMP.get()).addOptionalTag(itemTag("create", "crushed_raw_platinum"))
          .addOptionalTag(itemTag("create", "crushed_raw_zinc"));
        
        tag(getClumpTag("copper")).add(ItemContent.COPPER_CLUMP.get()).addOptionalTag(itemTag("create", "crushed_raw_copper"));
        tag(getClumpTag("iron")).add(ItemContent.IRON_CLUMP.get()).addOptionalTag(itemTag("create", "crushed_raw_iron"));
        tag(getClumpTag("gold")).add(ItemContent.GOLD_CLUMP.get()).addOptionalTag(itemTag("create", "crushed_raw_gold"));
        tag(getClumpTag("nickel")).add(ItemContent.NICKEL_CLUMP.get()).addOptionalTag(itemTag("create", "crushed_raw_nickel"));
        tag(getClumpTag("platinum")).add(ItemContent.PLATINUM_CLUMP.get()).addOptionalTag(itemTag("create", "crushed_raw_platinum"));
        // for compat
        tag(getClumpTag("zinc")).addOptionalTag(itemTag("create", "crushed_raw_zinc"));
        tag(getClumpTag("uranium")).addOptionalTag(itemTag("create", "crushed_raw_uranium"));
        tag(getClumpTag("osmium")).addOptionalTag(itemTag("create", "crushed_raw_osmium"));
        
        // dusts
        tag(Tags.Items.DUSTS)
          .add(ItemContent.NICKEL_DUST.get())
          .add(ItemContent.PLATINUM_DUST.get())
          .add(ItemContent.BIOSTEEL_DUST.get())
          .add(ItemContent.DURATIUM_DUST.get())
          .add(ItemContent.ELECTRUM_DUST.get())
          .add(ItemContent.ADAMANT_DUST.get())
          .add(ItemContent.ENERGITE_DUST.get())
          .add(ItemContent.URANIUM_DUST.get())
          .add(ItemContent.PLUTONIUM_DUST.get())
          .add(ItemContent.COAL_DUST.get())
          .add(ItemContent.STEEL_DUST.get());
        
        // nuggets
        tag(Tags.Items.NUGGETS)
          .add(ItemContent.NICKEL_NUGGET.get())
          .add(ItemContent.COPPER_NUGGET.get())
          .add(ItemContent.PLATINUM_NUGGET.get());
        
        tag(getDustTag("nickel")).add(ItemContent.NICKEL_DUST.get());
        tag(getDustTag("platinum")).add(ItemContent.PLATINUM_DUST.get());
        tag(getDustTag("biosteel")).add(ItemContent.BIOSTEEL_DUST.get());
        tag(getDustTag("duratium")).add(ItemContent.DURATIUM_DUST.get());
        tag(getDustTag("electrum")).add(ItemContent.ELECTRUM_DUST.get());
        tag(getDustTag("adamant")).add(ItemContent.ADAMANT_DUST.get());
        tag(getDustTag("energite")).add(ItemContent.ENERGITE_DUST.get());
        tag(getDustTag("steel")).add(ItemContent.STEEL_DUST.get());
        tag(getDustTag("uranium")).add(ItemContent.URANIUM_DUST.get());
        tag(getDustTag("plutonium")).add(ItemContent.PLUTONIUM_DUST.get());
        tag(TagContent.COAL_DUSTS).add(ItemContent.COAL_DUST.get());
        
        
        // ingots
        tag(Tags.Items.INGOTS)
          .add(ItemContent.NICKEL_INGOT.get())
          .add(ItemContent.PLATINUM_INGOT.get())
          .add(ItemContent.BIOSTEEL_INGOT.get())
          .add(ItemContent.PROMETHEUM_INGOT.get())
          .add(ItemContent.DURATIUM_INGOT.get())
          .add(ItemContent.ELECTRUM_INGOT.get())
          .add(ItemContent.ADAMANT_INGOT.get())
          .add(ItemContent.ENERGITE_INGOT.get())
          .add(ItemContent.STEEL_INGOT.get());
        
        tag(getIngotTag("nickel")).add(ItemContent.NICKEL_INGOT.get());
        tag(getIngotTag("platinum")).add(ItemContent.PLATINUM_INGOT.get());
        tag(getIngotTag("biosteel")).add(ItemContent.BIOSTEEL_INGOT.get());
        tag(getIngotTag("prometheum")).add(ItemContent.PROMETHEUM_INGOT.get());
        tag(getIngotTag("duratium")).add(ItemContent.DURATIUM_INGOT.get());
        tag(getIngotTag("electrum")).add(ItemContent.ELECTRUM_INGOT.get());
        tag(getIngotTag("adamant")).add(ItemContent.ADAMANT_INGOT.get());
        tag(getIngotTag("energite")).add(ItemContent.ENERGITE_INGOT.get());
        tag(getIngotTag("steel")).add(ItemContent.STEEL_INGOT.get());
        
        
        // gems
        tag(Tags.Items.GEMS)
          .add(ItemContent.FLUXITE.get());
        
        tag(cItemTag("gems/fluxite"))
          .add(ItemContent.FLUXITE.get());
        
        tag(TagContent.NICKEL_ORES).add(BlockContent.NICKEL_ORE.asItem(), BlockContent.DEEPSLATE_NICKEL_ORE.asItem());
        tag(TagContent.PLATINUM_ORES).add(BlockContent.DEEPSLATE_PLATINUM_ORE.asItem(), BlockContent.ENDSTONE_PLATINUM_ORE.asItem());
        tag(TagContent.URANIUM_ORES).add(BlockContent.DEEPSLATE_URANIUM_ORE.asItem())
          .addOptionalTag(itemTag("immersiveengineering", "ore_uranium"))
          .addOptionalTag(itemTag("immersiveengineering", "deepslate_ore_uranium"));
        
        tag(TagContent.STEEL_INGOTS).add(ItemContent.STEEL_INGOT.get()).add(ItemContent.BIOSTEEL_INGOT.get());
        tag(TagContent.QUARTZ_DUSTS).add(ItemContent.QUARTZ_DUST.get());
        
        // vanilla variants
        tag(TagContent.COPPER_DUSTS).add(ItemContent.COPPER_DUST.get());
        tag(TagContent.COPPER_NUGGETS).add(ItemContent.COPPER_NUGGET.get());
        tag(TagContent.IRON_DUSTS).add(ItemContent.IRON_DUST.get());
        tag(TagContent.GOLD_DUSTS).add(ItemContent.GOLD_DUST.get());
        
        // custom ores
        tag(TagContent.NICKEL_RAW_MATERIALS).add(ItemContent.RAW_NICKEL.get());
        tag(TagContent.NICKEL_DUSTS).add(ItemContent.NICKEL_DUST.get());
        tag(TagContent.NICKEL_NUGGETS).add(ItemContent.NICKEL_NUGGET.get());
        tag(TagContent.NICKEL_INGOTS).add(ItemContent.NICKEL_INGOT.get());
        
        tag(TagContent.PLATINUM_RAW_MATERIALS).add(ItemContent.RAW_PLATINUM.get());
        tag(TagContent.PLATINUM_DUSTS).add(ItemContent.PLATINUM_DUST.get());
        tag(TagContent.PLATINUM_NUGGETS).add(ItemContent.PLATINUM_NUGGET.get());
        tag(TagContent.PLATINUM_INGOTS).add(ItemContent.PLATINUM_INGOT.get());
        
        tag(TagContent.URANIUM_RAW_MATERIALS).add(ItemContent.RAW_URANIUM.get());
        
        tag(TagContent.FEEDER_BLACKLIST)
          .addOptionalTag(itemTag("relics", "infinity_ham"));
        
        tag(cItemTag("bananas")).add(ItemContent.BANANA.get());
        tag(cItemTag("foods/fruit")).add(ItemContent.BANANA.get());
        tag(Tags.Items.CROPS).add(BlockContent.WITHER_CROP_BLOCK.asItem());
        tag(cItemTag("crops/soul_flower")).add(BlockContent.WITHER_CROP_BLOCK.asItem());
        
        // biomass
        tag(TagContent.BIOMATTER)
          .addOptionalTag(ItemTags.VILLAGER_PLANTABLE_SEEDS)
          .addOptionalTag(cItemTag("seeds"))
          .addOptionalTag(ItemTags.SAPLINGS)
          .addOptionalTag(Tags.Items.FOODS)
          .addOptionalTag(Tags.Items.CROPS)
          .addOptionalTag(itemTag("farmersdelight", "wild_crops"))
          .addOptionalTag(itemTag("createaddition", "plant_foods"))
          .addOptionalTag(itemTag("enderio", "plant_matter_green"))
          .addOptionalTag(itemTag("enderio", "plant_matter_brown"))
          .add(BlockContent.WITHER_CROP_BLOCK.asItem())
          .add(ItemContent.BANANA.get().asItem())
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
        
        tag(TagContent.BIOMASS)
          .add(ItemContent.BIOMASS.get())
          .addOptionalTag(itemTag("techreborn", "plantball"));
        tag(TagContent.BIOMASS_BLOCK)
          .add(BlockContent.BIOMASS_BLOCK.asItem());
        
        // dyes
        tag(TagContent.RAW_WHITE_DYE)
          .add(Items.BONE_MEAL);
        tag(TagContent.RAW_LIGHT_GRAY_DYE)
          .add(Items.AZURE_BLUET)
          .add(Items.OXEYE_DAISY)
          .add(Items.WHITE_TULIP);
        tag(TagContent.RAW_BLACK_DYE)
          .add(Items.INK_SAC)
          .add(Items.WITHER_ROSE);
        tag(TagContent.RAW_RED_DYE)
          .add(Items.POPPY)
          .add(Items.RED_TULIP)
          .add(Items.ROSE_BUSH);
        tag(TagContent.RAW_ORANGE_DYE)
          .add(Items.ORANGE_TULIP)
          .add(Items.TORCHFLOWER);
        tag(TagContent.RAW_YELLOW_DYE)
          .add(Items.DANDELION)
          .add(Items.SUNFLOWER);
        tag(TagContent.RAW_CYAN_DYE)
          .add(Items.PITCHER_PLANT);
        tag(TagContent.RAW_BLUE_DYE)
          .add(Items.LAPIS_LAZULI)
          .add(Items.CORNFLOWER);
        tag(TagContent.RAW_MAGENTA_DYE)
          .add(Items.ALLIUM)
          .add(Items.LILAC);
        tag(TagContent.RAW_PINK_DYE)
          .add(Items.PINK_TULIP)
          .add(Items.PEONY)
          .add(Items.PINK_PETALS);
        
        // plating variants
        tag(TagContent.MACHINE_PLATING)
          .add(BlockContent.MACHINE_PLATING_BLOCK.asItem())
          .add(BlockContent.IRON_PLATING_BLOCK.asItem())
          .add(BlockContent.CARBON_PLATING_BLOCK.asItem())
          .add(BlockContent.NICKEL_PLATING_BLOCK.asItem());
        
        // silicon
        tag(TagContent.SILICON)
          .add(ItemContent.SILICON.get());
        
        // plastic
        tag(TagContent.PLASTIC_PLATES)
          .add(ItemContent.PLASTIC_SHEET.get());
        tag(cItemTag("plastics"))
          .add(ItemContent.PLASTIC_SHEET.get());
        tag(itemTag("pneumaticcraft", "plastic_sheets"))
          .add(ItemContent.PLASTIC_SHEET.get());
        
        // carbon fibre
        tag(TagContent.CARBON_FIBRE)
          .add(ItemContent.CARBON_FIBRE_STRANDS.get());
        
        // equipment enchanting
        tag(ItemTags.SWORDS)
          .add(ToolsContent.CHAINSAW.get(), ToolsContent.PROMETHIUM_AXE.get(), ToolsContent.PORTABLE_LASER.get());
        tag(ItemTags.AXES)
          .add(ToolsContent.CHAINSAW.get(), ToolsContent.PROMETHIUM_AXE.get(), ToolsContent.PORTABLE_LASER.get());
        
        tag(ItemTags.PICKAXES)
          .add(ToolsContent.HAND_DRILL.get(), ToolsContent.PROMETHIUM_PICKAXE.get(), ToolsContent.PORTABLE_LASER.get());
        tag(ItemTags.SHOVELS)
          .add(ToolsContent.HAND_DRILL.get(), ToolsContent.PROMETHIUM_PICKAXE.get());
        
        tag(ItemTags.CLUSTER_MAX_HARVESTABLES)
          .add(ToolsContent.HAND_DRILL.get(), ToolsContent.PROMETHIUM_PICKAXE.get());
        
        tag(ItemTags.HEAD_ARMOR_ENCHANTABLE)
          .add(ToolsContent.EXO_HELMET.get());
        tag(ItemTags.CHEST_ARMOR_ENCHANTABLE)
          .add(ToolsContent.JETPACK.get())
          .add(ToolsContent.JETPACK_ELYTRA.get())
          .add(ToolsContent.JETPACK_EXO_ELYTRA.get())
          .add(ToolsContent.EXO_JETPACK.get())
          .add(ToolsContent.EXO_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR_ENCHANTABLE)
          .add(ToolsContent.EXO_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR_ENCHANTABLE)
          .add(ToolsContent.EXO_BOOTS.get());
        
        tag(cItemTag("elytras"))
          .add(ToolsContent.JETPACK_ELYTRA.get())
          .add(ToolsContent.JETPACK_EXO_ELYTRA.get());
        
        tag(ItemTags.HEAD_ARMOR)
          .add(ToolsContent.EXO_HELMET.get());
        tag(ItemTags.CHEST_ARMOR)
          .add(ToolsContent.JETPACK.get())
          .add(ToolsContent.JETPACK_ELYTRA.get())
          .add(ToolsContent.JETPACK_EXO_ELYTRA.get())
          .add(ToolsContent.EXO_JETPACK.get())
          .add(ToolsContent.EXO_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR)
          .add(ToolsContent.EXO_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR)
          .add(ToolsContent.EXO_BOOTS.get());
        
        tag(ItemTags.DURABILITY_ENCHANTABLE)
          .add(ToolsContent.ELECTRIC_MACE.get())
          .add(ItemContent.WRENCH.get());
        
        tag(TagContent.WRENCHES)
          .add(ItemContent.WRENCH.get());
        
        tag(TagContent.REACTOR_COOLANT)
          .add(Items.ICE, Items.BLUE_ICE, Items.PACKED_ICE);
        
        tag(ItemTags.MACE_ENCHANTABLE)
          .add(ToolsContent.ELECTRIC_MACE.get());
        
        // storage blocks
        tag(Tags.Items.STORAGE_BLOCKS)
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
          .add(BlockContent.RAW_URANIUM_BLOCK.asItem())
          .add(BlockContent.URANIUM_DUST_BLOCK.asItem())
          .add(BlockContent.RAW_PLATINUM_BLOCK.asItem());
        
        tag(getStorageBlockTag("steel"))
          .add(BlockContent.STEEL_BLOCK.asItem());
        
        tag(getStorageBlockTag("energite"))
          .add(BlockContent.ENERGITE_BLOCK.asItem());
        
        tag(getStorageBlockTag("nickel"))
          .add(BlockContent.NICKEL_BLOCK.asItem());
        
        tag(getStorageBlockTag("biosteel"))
          .add(BlockContent.BIOSTEEL_BLOCK.asItem());
        
        tag(getStorageBlockTag("platinum"))
          .add(BlockContent.PLATINUM_BLOCK.asItem());
        
        tag(getStorageBlockTag("adamant"))
          .add(BlockContent.ADAMANT_BLOCK.asItem());
        
        tag(getStorageBlockTag("electrum"))
          .add(BlockContent.ELECTRUM_BLOCK.asItem());
        
        tag(getStorageBlockTag("duratium"))
          .add(BlockContent.DURATIUM_BLOCK.asItem());
        
        tag(getStorageBlockTag("biomass"))
          .add(BlockContent.BIOMASS_BLOCK.asItem());
        
        tag(getStorageBlockTag("plastic"))
          .add(BlockContent.PLASTIC_BLOCK.asItem());
        
        tag(getStorageBlockTag("fluxite"))
          .add(BlockContent.FLUXITE_BLOCK.asItem());
        
        tag(getStorageBlockTag("silicon"))
          .add(BlockContent.SILICON_BLOCK.asItem());
        
        tag(getStorageBlockTag("raw_nickel"))
          .add(BlockContent.RAW_NICKEL_BLOCK.asItem());
        
        tag(getStorageBlockTag("raw_platinum"))
          .add(BlockContent.RAW_PLATINUM_BLOCK.asItem());
        
        tag(getStorageBlockTag("raw_uranium"))
          .add(BlockContent.RAW_URANIUM_BLOCK.asItem());
        
        tag(getStorageBlockTag("uranium_dust"))
          .add(BlockContent.URANIUM_DUST_BLOCK.asItem());
        
        tag(itemTag("industrialforegoing", "bioreactor"))
          .addTag(TagContent.BIOMASS);
        
        tag(TagContent.MACHINE_PAINTS)
          .add(ItemContent.DIAMOND_PAINT.get())
          .add(ItemContent.REDSTONE_PAINT.get())
          .add(ItemContent.CAMO_PAINT.get())
          .add(ItemContent.ORANGE_PAINT.get())
          .add(ItemContent.FLUXITE_PAINT.get())
          .add(ItemContent.WHITE_PAINT.get())
          .add(ItemContent.INDUSTRIAL_PAINT.get())
          .add(ItemContent.NETHERITE_PAINT.get())
          .add(ItemContent.SCULK_PAINT.get());
        
        // recycling
        tag(TagContent.RECYCLES_TO_NETHERITE_SCRAP)
          .add(Items.NETHERITE_AXE)
          .add(Items.NETHERITE_BOOTS)
          .add(Items.NETHERITE_CHESTPLATE)
          .add(Items.NETHERITE_HELMET)
          .add(Items.NETHERITE_LEGGINGS)
          .add(Items.NETHERITE_PICKAXE)
          .add(Items.NETHERITE_SHOVEL)
          .add(Items.NETHERITE_SWORD);
        tag(TagContent.RECYCLES_TO_DIAMOND)
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
        tag(TagContent.RECYCLES_TO_IRON_DUST)
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
        tag(TagContent.RECYCLES_TO_SMALL_IRON_DUST)
          .add(Items.ACTIVATOR_RAIL)
          .add(Items.IRON_CHAIN)
          .add(Items.DETECTOR_RAIL)
          .add(Items.FLINT_AND_STEEL)
          .add(Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
          .add(Items.IRON_BARS)
          .add(Items.RAIL)
          .add(Items.TRIPWIRE_HOOK);
        tag(TagContent.RECYCLES_TO_GOLD_DUST)
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
        tag(TagContent.RECYCLES_TO_SMALL_GOLD_DUST)
          .add(Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
          .add(Items.POWERED_RAIL);
        tag(TagContent.RECYCLES_TO_COPPER_DUST)
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
        tag(TagContent.RECYCLES_TO_SMALL_COPPER_DUST)
          .add(Items.LIGHTNING_ROD);
        tag(TagContent.RECYCLES_TO_2_QUARTZ_DUST)
          .add(Items.QUARTZ_SLAB).add(Items.SMOOTH_QUARTZ_SLAB);
        tag(TagContent.RECYCLES_TO_4_QUARTZ_DUST)
          .add(Items.QUARTZ_BLOCK).add(Items.CHISELED_QUARTZ_BLOCK).add(Items.SMOOTH_QUARTZ)
          .add(Items.QUARTZ_BRICKS)
          .add(Items.QUARTZ_PILLAR)
          .add(Items.QUARTZ_STAIRS).add(Items.SMOOTH_QUARTZ_STAIRS);
        tag(TagContent.RECYCLES_TO_REDSTONE_DUST)
          .add(Items.REPEATER)
          .add(Items.COMPARATOR)
          .add(Items.REDSTONE_TORCH)
          .add(Items.TARGET);
        tag(TagContent.RECYCLES_TO_GRAVEL)
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
        tag(TagContent.RECYCLES_TO_SAND)
          .add(Items.SANDSTONE_STAIRS).add(Items.SANDSTONE_WALL)
          .add(Items.GRAVEL)
          .add(Items.SMOOTH_SANDSTONE_STAIRS);
        tag(TagContent.RECYCLES_TO_RED_SAND)
          .add(Items.RED_SANDSTONE_STAIRS).add(Items.RED_SANDSTONE_WALL)
          .add(Items.SMOOTH_RED_SANDSTONE_STAIRS);
        tag(TagContent.RECYCLES_TO_STRING)
          .addOptionalTag(ItemTags.WOOL_CARPETS)
          .addOptionalTag(ItemTags.BANNERS)
          .add(Items.PAINTING);
        tag(TagContent.RECYCLES_TO_BIOMASS)
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
