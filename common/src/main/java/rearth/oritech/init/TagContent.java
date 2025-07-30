package rearth.oritech.init;

import rearth.oritech.Oritech;

import static rearth.oritech.util.TagUtils.*;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class TagContent {
    
    // only add tags for common stuff, such as dusts and nuggets. Gems are oritech-only
    // items
    // vanilla variants
    public static final TagKey<Item> CLUMPS = cItemTag("clumps");
    public static final TagKey<Item> DUSTS = cItemTag("dusts");

    public static final TagKey<Item> COPPER_CLUMPS = cItemTag("clumps/copper");
    public static final TagKey<Item> COPPER_DUSTS = cItemTag("dusts/copper");
    public static final TagKey<Item> COPPER_NUGGETS = cItemTag("nuggets/copper");
    
    public static final TagKey<Item> IRON_CLUMPS = cItemTag("clumps/iron");
    public static final TagKey<Item> IRON_DUSTS = cItemTag("dusts/iron");

    public static final TagKey<Item> GOLD_CLUMPS = cItemTag("clumps/gold");
    public static final TagKey<Item> GOLD_DUSTS = cItemTag("dusts/gold");
    
    public static final TagKey<Item> QUARTZ_DUSTS = cItemTag("dusts/quartz");
    public static final TagKey<Item> COAL_DUSTS = cItemTag("dusts/coal");

    // custom ores -----
    // nickel
    public static final TagKey<Block> NICKEL_ORE_BLOCKS = cBlockTag("ores/nickel");
    public static final TagKey<Item> NICKEL_ORES = cItemTag("ores/nickel");
    public static final TagKey<Item> NICKEL_RAW_MATERIALS = cItemTag("raw_materials/nickel");
    public static final TagKey<Item> NICKEL_CLUMPS = cItemTag("clumps/nickel");
    public static final TagKey<Item> NICKEL_DUSTS = cItemTag("dusts/nickel");
    public static final TagKey<Item> NICKEL_INGOTS = cItemTag("ingots/nickel");
    public static final TagKey<Item> NICKEL_NUGGETS = cItemTag("nuggets/nickel");
    
    // platinum
    public static final TagKey<Block> PLATINUM_ORE_BLOCKS = cBlockTag("ores/platinum");
    public static final TagKey<Item> PLATINUM_ORES = cItemTag("ores/platinum");
    public static final TagKey<Item> PLATINUM_RAW_MATERIALS = cItemTag("raw_materials/platinum");
    public static final TagKey<Item> PLATINUM_CLUMPS = cItemTag("clumps/platinum");
    public static final TagKey<Item> PLATINUM_DUSTS = cItemTag("dusts/platinum");
    public static final TagKey<Item> PLATINUM_INGOTS = cItemTag("ingots/platinum");
    public static final TagKey<Item> PLATINUM_NUGGETS = cItemTag("nuggets/platinum");
    
    // uranium
    public static final TagKey<Block> URANIUM_ORE_BLOCKS = cBlockTag("ores/uranium");
    public static final TagKey<Item> URANIUM_ORES = cItemTag("ores/uranium");
    public static final TagKey<Item> URANIUM_RAW_MATERIALS = cItemTag("raw_materials/uranium");
    public static final TagKey<Item> URANIUM_CLUMPS = cItemTag("clumps/uranium");
    public static final TagKey<Item> URANIUM_DUSTS = cItemTag("dusts/uranium");
    
    // plutonium
    public static final TagKey<Item> PLUTONIUM_DUSTS = cItemTag("dusts/plutonium");

    // alloys
    public static final TagKey<Item> ELECTRUM_INGOTS = cItemTag("ingots/electrum");
    public static final TagKey<Item> ELECTRUM_DUSTS = cItemTag("dusts/electrum");
    public static final TagKey<Item> STEEL_INGOTS = cItemTag("ingots/steel");

    // plating
    public static final TagKey<Item> MACHINE_PLATING = oritechItemTag("plating");

    // plastic
    public static final TagKey<Item> PLASTIC_PLATES = cItemTag("plates/plastic");
    
    // biomass
    // BIOMATTER is any plantlike item that can be pulverized into biomass
    public static final TagKey<Item> BIOMATTER = oritechItemTag("biomatter");
    // BIOMASS tag is for the biomass item and other similar items (Mekanism's bio fuel, Tech Reborn's plantball, etc.)
    public static final TagKey<Item> BIOMASS = cItemTag("fuels/bio");
    public static final TagKey<Item> BIOMASS_BLOCK = cItemTag("fuels/block/bio");

    // frame support
    public static final TagKey<Block> MACHINE_FRAME_SUPPORT = TagKey.create(Registries.BLOCK, Oritech.id("frame_support"));

    // silicon
    public static final TagKey<Item> SILICON = cItemTag("silicon");

    // carbon fibre
    public static final TagKey<Item> CARBON_FIBRE = cItemTag("carbon_fibre");
    
    // blocks
    public static final TagKey<Block> DRILL_MINEABLE = oritechBlockTag("mineable/drill");
    public static final TagKey<Block> RESOURCE_NODES = oritechBlockTag("resource_nodes");
    public static final TagKey<Block> LASER_PASSTHROUGH = oritechBlockTag("laser_passthrough");
    public static final TagKey<Block> LASER_ACCELERATED = oritechBlockTag("laser_accelerated");

    // trees
    public static final TagKey<Block> CUTTER_LOGS_MINEABLE = oritechBlockTag("mineable/cutter_logs");
    public static final TagKey<Block> CUTTER_LEAVES_MINEABLE = oritechBlockTag("mineable/cutter_leaves");
    
    // c stuff
    public static final TagKey<Block> CONVENTIONAL_ORES = cBlockTag("ores");
    public static final TagKey<Item> CONVENTIONAL_FERTILIZER = cItemTag("fertilizers");
    public static final TagKey<Biome> CONVENTIONAL_COLD = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_cold"));
    
    // reactor
    public static final TagKey<Block> REACTOR_WALL_BLOCKS = oritechBlockTag("reactor_wall_blocks");
    public static final TagKey<Item> REACTOR_COOLANT = oritechItemTag("reactor_coolant_items");
    
    // feeder blacklist
    public static final TagKey<Item> FEEDER_BLACKLIST = oritechItemTag("feeder_blacklist");

    // dyes
    public static final TagKey<Item> RAW_WHITE_DYE = cItemTag("raw_materials/dyes/white");
    public static final TagKey<Item> RAW_LIGHT_GRAY_DYE = cItemTag("raw_materials/dyes/light_gray");
    public static final TagKey<Item> RAW_BLACK_DYE = cItemTag("raw_materials/dyes/black");
    public static final TagKey<Item> RAW_RED_DYE = cItemTag("raw_materials/dyes/red");
    public static final TagKey<Item> RAW_ORANGE_DYE = cItemTag("raw_materials/dyes/orange");
    public static final TagKey<Item> RAW_YELLOW_DYE = cItemTag("raw_materials/dyes/yellow");
    public static final TagKey<Item> RAW_CYAN_DYE = cItemTag("raw_materials/dyes/cyan");
    public static final TagKey<Item> RAW_BLUE_DYE = cItemTag("raw_materials/dyes/blue");
    public static final TagKey<Item> RAW_MAGENTA_DYE = cItemTag("raw_materials/dyes/magenta");
    public static final TagKey<Item> RAW_PINK_DYE = cItemTag("raw_materials/dyes/pink");
    
    // spawner blacklist
    public static final TagKey<EntityType<?>> SPAWNER_BLACKLIST = TagKey.create(Registries.ENTITY_TYPE, Oritech.id("spawner_blacklist"));
    
    public static final TagKey<Block> BLACK_HOLE_BLACKLIST = TagKey.create(Registries.BLOCK, Oritech.id("blackhole_blacklist"));
    
    // unstable container contents
    public static final TagKey<Block> UNSTABLE_CONTAINER_SOURCES_LOW = oritechBlockTag("unstable_container/low");
    public static final TagKey<Block> UNSTABLE_CONTAINER_SOURCES_MEDIUM = oritechBlockTag("unstable_container/medium");
    public static final TagKey<Block> UNSTABLE_CONTAINER_SOURCES_HIGH = oritechBlockTag("unstable_container/high");

    // recyclables
    public static final TagKey<Item> RECYCLES_TO_NETHERITE_SCRAP = oritechItemTag("recyclable/netherite_scrap");
    public static final TagKey<Item> RECYCLES_TO_DIAMOND = oritechItemTag("recyclable/diamond");
    public static final TagKey<Item> RECYCLES_TO_IRON_DUST = oritechItemTag("recyclable/iron_dust");
    public static final TagKey<Item> RECYCLES_TO_SMALL_IRON_DUST = oritechItemTag("recyclable/small_iron_dust");
    public static final TagKey<Item> RECYCLES_TO_GOLD_DUST = oritechItemTag("recyclable/gold_dust");
    public static final TagKey<Item> RECYCLES_TO_SMALL_GOLD_DUST = oritechItemTag("recyclable/small_gold_dust");
    public static final TagKey<Item> RECYCLES_TO_COPPER_DUST = oritechItemTag("recyclable/copper_dust");
    public static final TagKey<Item> RECYCLES_TO_SMALL_COPPER_DUST = oritechItemTag("recyclable/small_copper_dust");
    public static final TagKey<Item> RECYCLES_TO_2_QUARTZ_DUST = oritechItemTag("recyclable/quartz_dust_2");
    public static final TagKey<Item> RECYCLES_TO_4_QUARTZ_DUST = oritechItemTag("recyclable/quartz_dust_4");
    public static final TagKey<Item> RECYCLES_TO_REDSTONE_DUST = oritechItemTag("recyclable/redstone_dust");
    public static final TagKey<Item> RECYCLES_TO_GRAVEL = oritechItemTag("recyclable/gravel");
    public static final TagKey<Item> RECYCLES_TO_SAND = oritechItemTag("recyclable/sand");
    public static final TagKey<Item> RECYCLES_TO_RED_SAND = oritechItemTag("recyclable/red_sand");
    public static final TagKey<Item> RECYCLES_TO_STRING = oritechItemTag("recyclable/string");
    public static final TagKey<Item> RECYCLES_TO_BIOMASS = oritechItemTag("recyclable/biomass");

    // fuels
    public static final TagKey<Fluid> OIL = cFluidTag("oil");
    public static final TagKey<Fluid> BIOFUEL = oritechFluidTag("biofuel");
    public static final TagKey<Fluid> TURBOFUEL = oritechFluidTag("turbofuel");

}
