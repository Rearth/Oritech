package rearth.oritech.fabricgen.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.TagContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static rearth.oritech.util.TagUtils.cBlockTag;
import static rearth.oritech.util.TagUtils.getStorageBlockyTag;

public class BlockTagGenerator extends FabricTagProvider.BlockTagProvider {
    
    public BlockTagGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }
    
    @Override
    protected void addTags(HolderLookup.Provider arg) {
        
        var pickaxeBuilder = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        
        // sort auto registered drops before writing to pickaxe.json to keep pickaxe.json
        // from being changed every time datagen is run.
        var blockDrops = new ArrayList<Block>(BlockContent.autoRegisteredDrops);
        Collections.sort(blockDrops, (b1, b2) -> b1.toString().compareTo(b2.toString()));
        for (var block : blockDrops) {
            pickaxeBuilder.add(block);
        }
        pickaxeBuilder.add(BlockContent.ENERGY_PIPE_CONNECTION);
        pickaxeBuilder.add(BlockContent.SUPERCONDUCTOR_CONNECTION);
        pickaxeBuilder.add(BlockContent.FLUID_PIPE_CONNECTION);
        pickaxeBuilder.add(BlockContent.ITEM_PIPE_CONNECTION);
        pickaxeBuilder.add(BlockContent.FRAMED_ENERGY_PIPE_CONNECTION);
        pickaxeBuilder.add(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION);
        pickaxeBuilder.add(BlockContent.FRAMED_FLUID_PIPE_CONNECTION);
        pickaxeBuilder.add(BlockContent.FRAMED_ITEM_PIPE_CONNECTION);
        pickaxeBuilder.add(BlockContent.TRANSPARENT_ITEM_PIPE);
        pickaxeBuilder.add(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION);
        pickaxeBuilder.add(BlockContent.SMALL_TANK_BLOCK);
        pickaxeBuilder.add(BlockContent.SMALL_STORAGE_BLOCK);
        pickaxeBuilder.add(BlockContent.PUMP_TRUNK_BLOCK);
        pickaxeBuilder.add(BlockContent.MACHINE_CORE_HIDDEN);
        
        pickaxeBuilder
          .add(BlockContent.NICKEL_ORE)
          .add(BlockContent.DEEPSLATE_NICKEL_ORE)
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE)
          .add(BlockContent.DEEPSLATE_URANIUM_ORE)
          .add(BlockContent.URANIUM_CRYSTAL)
          .add(BlockContent.ENDSTONE_PLATINUM_ORE);
        
        tag(BlockTags.MINEABLE_WITH_AXE)
          .add(BlockContent.ITEM_PIPE)
          .add(BlockContent.TRANSPARENT_ITEM_PIPE)
          .add(BlockContent.ITEM_PIPE_CONNECTION)
          .add(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION);
        
        tag(ConventionalBlockTags.ORES)
          .add(BlockContent.NICKEL_ORE)
          .add(BlockContent.DEEPSLATE_NICKEL_ORE)
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE)
          .add(BlockContent.DEEPSLATE_URANIUM_ORE)
          .add(BlockContent.ENDSTONE_PLATINUM_ORE);
        
        tag(cBlockTag("ores_in_ground/stone"))
          .add(BlockContent.NICKEL_ORE);

        tag(cBlockTag("ores_in_ground/deepslate"))
          .add(BlockContent.DEEPSLATE_NICKEL_ORE)
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE)
          .add(BlockContent.DEEPSLATE_URANIUM_ORE);
        
        tag(cBlockTag("ores_in_ground/end_stone"))
          .add(BlockContent.ENDSTONE_PLATINUM_ORE);
        
        tag(BlockTags.NEEDS_STONE_TOOL)
          .add(BlockContent.NICKEL_ORE)
          .add(BlockContent.DEEPSLATE_NICKEL_ORE);
        
        tag(BlockTags.NEEDS_IRON_TOOL)
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE)
          .add(BlockContent.DEEPSLATE_URANIUM_ORE)
          .add(BlockContent.ENDSTONE_PLATINUM_ORE);
        
        tag(TagContent.NICKEL_ORE_BLOCKS)
          .add(BlockContent.NICKEL_ORE, BlockContent.DEEPSLATE_NICKEL_ORE);
        tag(TagContent.PLATINUM_ORE_BLOCKS)
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE, BlockContent.ENDSTONE_PLATINUM_ORE);
        tag(TagContent.URANIUM_ORE_BLOCKS)
          .add(BlockContent.DEEPSLATE_URANIUM_ORE);
        
        tag(TagContent.DRILL_MINEABLE)
          .addOptionalTag(BlockTags.MINEABLE_WITH_PICKAXE)
          .addOptionalTag(BlockTags.MINEABLE_WITH_SHOVEL);
        
        tag(TagContent.RESOURCE_NODES)
          .add(BlockContent.RESOURCE_NODE_COPPER)
          .add(BlockContent.RESOURCE_NODE_IRON)
          .add(BlockContent.RESOURCE_NODE_NICKEL)
          .add(BlockContent.RESOURCE_NODE_GOLD)
          .add(BlockContent.RESOURCE_NODE_REDSTONE)
          .add(BlockContent.RESOURCE_NODE_LAPIS)
          .add(BlockContent.RESOURCE_NODE_EMERALD)
          .add(BlockContent.RESOURCE_NODE_DIAMOND)
          .add(BlockContent.RESOURCE_NODE_COAL)
          .add(BlockContent.RESOURCE_NODE_URANIUM)
          .add(BlockContent.RESOURCE_NODE_PLATINUM);
        
        tag(TagContent.LASER_PASSTHROUGH)
          .forceAddTag(ConventionalBlockTags.GLASS_BLOCKS)
          .forceAddTag(ConventionalBlockTags.GLASS_PANES)
          .forceAddTag(ConventionalBlockTags.BUDS);
        tag(ConventionalBlockTags.BUDS)
          .addOptional(ResourceLocation.fromNamespaceAndPath("clutter", "small_onyx_bud"))
          .addOptional(ResourceLocation.fromNamespaceAndPath("clutter", "medium_onyx_bud"))
          .addOptional(ResourceLocation.fromNamespaceAndPath("clutter", "large_onyx_bud"));
        
        tag(TagContent.LASER_ACCELERATED)
          .forceAddTag(ConventionalBlockTags.BUDDING_BLOCKS);
        tag(ConventionalBlockTags.BUDDING_BLOCKS)
          .addOptional(ResourceLocation.fromNamespaceAndPath("clutter", "budding_onyx"));

        tag(TagContent.CUTTER_LOGS_MINEABLE)
        // using forceAddTag because the datagen wasn't recognizing the vanilla LOGS, LEAVES, and WART_BLOCKS tags
        // even though they should absolutely be there
          .forceAddTag(BlockTags.LOGS)
          .add(Blocks.MANGROVE_ROOTS)
          .add(Blocks.MUSHROOM_STEM);
        
        tag(TagContent.CUTTER_LEAVES_MINEABLE)
          .forceAddTag(BlockTags.LEAVES)
          .forceAddTag(BlockTags.WART_BLOCKS)
          .add(Blocks.SHROOMLIGHT)
          .add(Blocks.RED_MUSHROOM_BLOCK)
          .add(Blocks.BROWN_MUSHROOM_BLOCK);
        
        tag(TagContent.REACTOR_WALL_BLOCKS)
          .add(BlockContent.REACTOR_WALL)
          .add(BlockContent.REACTOR_ABSORBER_PORT)
          .add(BlockContent.REACTOR_ENERGY_PORT)
          .add(BlockContent.REACTOR_FUEL_PORT)
          .add(BlockContent.REACTOR_REDSTONE_PORT)
          .add(BlockContent.REACTOR_CONTROLLER);
        
        tag(TagContent.UNSTABLE_CONTAINER_SOURCES_LOW)
          .add(Blocks.REDSTONE_BLOCK)
          .add(Blocks.TNT)
          .add(BlockContent.FLUXITE_BLOCK);
        
        tag(TagContent.UNSTABLE_CONTAINER_SOURCES_MEDIUM)
          .add(Blocks.DRAGON_EGG)
          .add(BlockContent.LOW_YIELD_NUKE)
          .add(BlockContent.NUKE);
        
        tag(TagContent.UNSTABLE_CONTAINER_SOURCES_HIGH)
          .add(BlockContent.BLACK_HOLE_BLOCK);

        tag(TagContent.MACHINE_FRAME_SUPPORT)
          .add(BlockContent.METAL_BEAM_BLOCK);

        tag(TagContent.BLACK_HOLE_BLACKLIST)
          .add(BlockContent.BLOCK_PLACER_HEAD); // just a dummy so packdevs can find the tag easier
        
        // storage block tags
        tag(getStorageBlockyTag("steel"))
          .add(BlockContent.STEEL_BLOCK);
        
        tag(getStorageBlockyTag("energite"))
          .add(BlockContent.ENERGITE_BLOCK);
        
        tag(getStorageBlockyTag("nickel"))
          .add(BlockContent.NICKEL_BLOCK);
        
        tag(getStorageBlockyTag("biosteel"))
          .add(BlockContent.BIOSTEEL_BLOCK);
        
        tag(getStorageBlockyTag("platinum"))
          .add(BlockContent.PLATINUM_BLOCK);
        
        tag(getStorageBlockyTag("adamant"))
          .add(BlockContent.ADAMANT_BLOCK);
        
        tag(getStorageBlockyTag("electrum"))
          .add(BlockContent.ELECTRUM_BLOCK);
        
        tag(getStorageBlockyTag("duratium"))
          .add(BlockContent.DURATIUM_BLOCK);
        
        tag(getStorageBlockyTag("biomass"))
          .add(BlockContent.BIOMASS_BLOCK);
        
        tag(getStorageBlockyTag("plastic"))
          .add(BlockContent.PLASTIC_BLOCK);
        
        tag(getStorageBlockyTag("fluxite"))
          .add(BlockContent.FLUXITE_BLOCK);
        
        tag(getStorageBlockyTag("silicon"))
          .add(BlockContent.SILICON_BLOCK);
        
        tag(getStorageBlockyTag("raw_nickel"))
          .add(BlockContent.RAW_NICKEL_BLOCK);
        
        tag(getStorageBlockyTag("raw_platinum"))
          .add(BlockContent.RAW_PLATINUM_BLOCK);
    }
}
