package rearth.oritech.datagen.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.registries.DeferredBlock;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.RegistryReflectionUtil;

import java.util.concurrent.CompletableFuture;

import static rearth.oritech.util.TagUtils.*;

public class BlockTagGenerator extends BlockTagsProvider {
    
    public BlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture, Oritech.MOD_ID);
    }
    
    @Override
    protected void addTags(HolderLookup.Provider arg) {
        
        var pickaxeBuilder = this.tag(BlockTags.MINEABLE_WITH_PICKAXE);
        
        RegistryReflectionUtil.IterateFields(BlockContent.class, DeferredBlock.class, ((field, identifier, value) -> {
            var holder = (DeferredBlock<Block>) value;
            pickaxeBuilder.add(holder.get());
        }));
        
        pickaxeBuilder.add(BlockContent.ENERGY_PIPE_CONNECTION.get());
        pickaxeBuilder.add(BlockContent.SUPERCONDUCTOR_CONNECTION.get());
        pickaxeBuilder.add(BlockContent.FLUID_PIPE_CONNECTION.get());
        pickaxeBuilder.add(BlockContent.ITEM_PIPE_CONNECTION.get());
        pickaxeBuilder.add(BlockContent.FRAMED_ENERGY_PIPE_CONNECTION.get());
        pickaxeBuilder.add(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION.get());
        pickaxeBuilder.add(BlockContent.FRAMED_FLUID_PIPE_CONNECTION.get());
        pickaxeBuilder.add(BlockContent.FRAMED_ITEM_PIPE_CONNECTION.get());
        pickaxeBuilder.add(BlockContent.TRANSPARENT_ITEM_PIPE.get());
        pickaxeBuilder.add(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.get());
        pickaxeBuilder.add(BlockContent.PORTABLE_TANK.get());
        pickaxeBuilder.add(BlockContent.PORTABLE_ENERGY_STORAGE.get());
        pickaxeBuilder.add(BlockContent.HEART_OF_THE_MACHINE_ADDON.get());
        pickaxeBuilder.add(BlockContent.PUMP_TRUNK.get());
        pickaxeBuilder.add(BlockContent.COMPLEX_PLATING.get());
        pickaxeBuilder.add(BlockContent.CARBON_PLATING_SLAB.get());
        pickaxeBuilder.add(BlockContent.IRON_PLATING_SLAB.get());
        pickaxeBuilder.add(BlockContent.COPPER_REINFORCED_PLATING_SLAB.get());
        pickaxeBuilder.add(BlockContent.NICKEL_PLATING_SLAB.get());
        pickaxeBuilder.add(BlockContent.INDUSTRIAL_DOOR_HINGE.get());
        pickaxeBuilder.add(BlockContent.HANGAR_DOOR_HELPER.get());
        pickaxeBuilder.add(BlockContent.TAINTED_REFINERY.get());
        
        pickaxeBuilder
          .add(BlockContent.NICKEL_ORE.get())
          .add(BlockContent.DEEPSLATE_NICKEL_ORE.get())
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE.get())
          .add(BlockContent.DEEPSLATE_URANIUM_ORE.get())
          .add(BlockContent.URANITE_CRYSTAL.get())
          .add(BlockContent.ENDSTONE_PLATINUM_ORE.get());
        
        this.tag(BlockTags.MINEABLE_WITH_AXE)
          .add(BlockContent.ITEM_PIPE.get())
          .add(BlockContent.TRANSPARENT_ITEM_PIPE.get())
          .add(BlockContent.ITEM_PIPE_CONNECTION.get())
          .add(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.get());

        this.tag(BlockTags.CROPS)
          .add(BlockContent.SOUL_FLOWERS.get());

        this.tag(BlockTags.MAINTAINS_FARMLAND)
          .add(BlockContent.SOUL_FLOWERS.get());
        
        this.tag(Tags.Blocks.ORES)
          .add(BlockContent.NICKEL_ORE.get())
          .add(BlockContent.DEEPSLATE_NICKEL_ORE.get())
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE.get())
          .add(BlockContent.DEEPSLATE_URANIUM_ORE.get())
          .add(BlockContent.ENDSTONE_PLATINUM_ORE.get());
        
        this.tag(cBlockTag("ores_in_ground/stone"))
          .add(BlockContent.NICKEL_ORE.get());

        this.tag(cBlockTag("ores_in_ground/deepslate"))
          .add(BlockContent.DEEPSLATE_NICKEL_ORE.get())
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE.get())
          .add(BlockContent.DEEPSLATE_URANIUM_ORE.get());
        
        this.tag(cBlockTag("ores_in_ground/end_stone"))
          .add(BlockContent.ENDSTONE_PLATINUM_ORE.get());
        
        this.tag(BlockTags.NEEDS_STONE_TOOL)
          .add(BlockContent.NICKEL_ORE.get())
          .add(BlockContent.DEEPSLATE_NICKEL_ORE.get());
        
        this.tag(BlockTags.NEEDS_IRON_TOOL)
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE.get())
          .add(BlockContent.DEEPSLATE_URANIUM_ORE.get())
          .add(BlockContent.ENDSTONE_PLATINUM_ORE.get());
        
        this.tag(TagContent.NICKEL_ORE_BLOCKS)
          .add(BlockContent.NICKEL_ORE.get(), BlockContent.DEEPSLATE_NICKEL_ORE.get());
        this.tag(TagContent.PLATINUM_ORE_BLOCKS)
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE.get(), BlockContent.ENDSTONE_PLATINUM_ORE.get());
        this.tag(TagContent.URANIUM_ORE_BLOCKS)
          .add(BlockContent.DEEPSLATE_URANIUM_ORE.get());
        
        this.tag(TagContent.DRILL_MINEABLE)
          .addOptionalTag(BlockTags.MINEABLE_WITH_PICKAXE)
          .addOptionalTag(BlockTags.MINEABLE_WITH_SHOVEL);
        
        this.tag(TagContent.RESOURCE_NODES)
          .add(BlockContent.COPPER_RESOURCE_NODE.get())
          .add(BlockContent.IRON_RESOURCE_NODE.get())
          .add(BlockContent.NICKEL_RESOURCE_NODE.get())
          .add(BlockContent.GOLD_RESOURCE_NODE.get())
          .add(BlockContent.REDSTONE_RESOURCE_NODE.get())
          .add(BlockContent.RESOURCE_NODE_LAPIS.get())
          .add(BlockContent.EMERALD_RESOURCE_NODE.get())
          .add(BlockContent.DIAMOND_RESOURCE_NODE.get())
          .add(BlockContent.COAL_RESOURCE_NODE.get())
          .add(BlockContent.URANIUM_RESOURCE_NODE.get())
          .add(BlockContent.PLATINUM_RESOURCE_NODE.get())
          .add(BlockContent.ALUMINUM_RESOURCE_NODE.get())
          .add(BlockContent.LEAD_RESOURCE_NODE.get())
          .add(BlockContent.OSMIUM_RESOURCE_NODE.get())
          .add(BlockContent.SILVER_RESOURCE_NODE.get())
          .add(BlockContent.TIN_RESOURCE_NODE.get())
          .add(BlockContent.ZINC_RESOURCE_NODE.get())
          .add(BlockContent.IRIDIUM_RESOURCE_NODE.get())
          .add(BlockContent.TUNGSTEN_RESOURCE_NODE.get())
          .add(BlockContent.TITANIUM_RESOURCE_NODE.get());
        
        this.tag(TagContent.LASER_PASSTHROUGH)
                .addTag(Tags.Blocks.GLASS_BLOCKS)
          .addTag(Tags.Blocks.GLASS_PANES)
          .addTag(Tags.Blocks.BUDS);
        this.tag(Tags.Blocks.BUDS)
          .addOptionalTag(blockTag("clutter", "small_onyx_bud"))
          .addOptionalTag(blockTag("clutter", "medium_onyx_bud"))
          .addOptionalTag(blockTag("clutter", "large_onyx_bud"));
        
        this.tag(TagContent.LASER_ACCELERATED)
          .addTag(Tags.Blocks.BUDDING_BLOCKS);
        this.tag(Tags.Blocks.BUDDING_BLOCKS)
          .addOptionalTag(blockTag("clutter", "budding_onyx"));

        this.tag(TagContent.CUTTER_LOGS_MINEABLE)
        // using addTag because the datagen wasn't recognizing the vanilla LOGS, LEAVES, and WART_BLOCKS tags
        // even though they should absolutely be there
          .addTag(BlockTags.LOGS)
          .add(Blocks.MANGROVE_ROOTS)
          .add(Blocks.MUSHROOM_STEM);
        
        this.tag(TagContent.REFINERY_ARCANE_BLOCKS)
          .add(Blocks.ENCHANTING_TABLE)
          .add(Blocks.BOOKSHELF)
          .add(Blocks.CHISELED_BOOKSHELF)
          .add(Blocks.END_ROD)
          .add(Blocks.AMETHYST_BLOCK)
          .add(Blocks.BUDDING_AMETHYST)
          .add(Blocks.SMALL_AMETHYST_BUD)
          .add(Blocks.MEDIUM_AMETHYST_BUD)
          .add(Blocks.LARGE_AMETHYST_BUD)
          .add(Blocks.AMETHYST_CLUSTER)
          .add(Blocks.SOUL_TORCH)
          .add(Blocks.SOUL_LANTERN)
          .add(Blocks.SOUL_CAMPFIRE)
          .add(Blocks.CRYING_OBSIDIAN)
          .add(Blocks.RESPAWN_ANCHOR)
          .add(BlockContent.ARCANE_AUGMENT_STATION.get())
          .add(BlockContent.STABILIZED_ENCHANTER.get())
          .add(BlockContent.ARCANE_CATALYST.get());
        
        this.tag(TagContent.REFINERY_SCULK_BLOCKS)
          .add(Blocks.SPAWNER)
          .add(Blocks.INFESTED_STONE)
          .add(Blocks.INFESTED_COBBLESTONE)
          .add(Blocks.INFESTED_STONE_BRICKS)
          .add(Blocks.INFESTED_MOSSY_STONE_BRICKS)
          .add(Blocks.INFESTED_CRACKED_STONE_BRICKS)
          .add(Blocks.INFESTED_CHISELED_STONE_BRICKS)
          .add(Blocks.INFESTED_DEEPSLATE)
          .add(Blocks.SCULK_CATALYST)
          .add(Blocks.SCULK_SENSOR)
          .add(Blocks.SCULK_SHRIEKER)
          .add(Blocks.SCULK_VEIN)
          .add(Blocks.CALIBRATED_SCULK_SENSOR)
          .add(Blocks.SCULK)
          .add(BlockContent.SPAWNER_CAGE.get())
          .add(BlockContent.SPAWNER_CONTROLLER.get());
        
        this.tag(TagContent.CUTTER_LEAVES_MINEABLE)
          .addTag(BlockTags.LEAVES)
          .addTag(BlockTags.WART_BLOCKS)
          .add(Blocks.SHROOMLIGHT)
          .add(Blocks.RED_MUSHROOM_BLOCK)
          .add(Blocks.BROWN_MUSHROOM_BLOCK);
        
        this.tag(TagContent.REACTOR_WALL_BLOCKS)
          .add(BlockContent.REACTOR_WALL.get())
          .add(BlockContent.REACTOR_COOLANT_ABSORBER_PORT.get())
          .add(BlockContent.REACTOR_ENERGY_PORT.get())
          .add(BlockContent.REACTOR_FUEL_PORT.get())
          .add(BlockContent.REACTOR_REDSTONE_PORT.get())
          .add(BlockContent.NUCLEAR_REACTOR_CONTROLLER.get());

        this.tag(TagContent.MACHINE_FRAME_SUPPORT)
          .add(BlockContent.INDUSTRIAL_SUPPORT_BEAM.get());

        this.tag(TagContent.BLACK_HOLE_BLACKLIST)
          .add(BlockContent.BLOCK_PLACER_HEAD.get()); // just a dummy so packdevs can find the tag easier
        
        // storage block tags
        this.tag(getStorageBlockyTag("steel"))
          .add(BlockContent.STEEL.get());
        
        this.tag(getStorageBlockyTag("energite"))
          .add(BlockContent.ENERGITE.get());
        
        this.tag(getStorageBlockyTag("nickel"))
          .add(BlockContent.NICKEL.get());
        
        this.tag(getStorageBlockyTag("biosteel"))
          .add(BlockContent.BIOSTEEL.get());
        
        this.tag(getStorageBlockyTag("platinum"))
          .add(BlockContent.PLATINUM.get());
        
        this.tag(getStorageBlockyTag("adamant"))
          .add(BlockContent.ADAMANT.get());
        
        this.tag(getStorageBlockyTag("electrum"))
          .add(BlockContent.ELECTRUM.get());
        
        this.tag(getStorageBlockyTag("duratium"))
          .add(BlockContent.DURATIUM.get());
        
        this.tag(getStorageBlockyTag("biomass"))
          .add(BlockContent.BIOMASS.get());
        
        this.tag(getStorageBlockyTag("plastic"))
          .add(BlockContent.PLASTIC.get());
        
        this.tag(getStorageBlockyTag("fluxite"))
          .add(BlockContent.FLUXITE.get());
        
        this.tag(getStorageBlockyTag("silicon"))
          .add(BlockContent.SILICON.get());
        
        this.tag(getStorageBlockyTag("raw_nickel"))
          .add(BlockContent.RAW_NICKEL.get());
        
        this.tag(getStorageBlockyTag("raw_platinum"))
          .add(BlockContent.RAW_PLATINUM.get());
        
        this.tag(getStorageBlockyTag("raw_uranium"))
          .add(BlockContent.RAW_URANIUM.get());
        
        this.tag(getStorageBlockyTag("uranium_dust"))
          .add(BlockContent.URANIUM.get());
    }
}
