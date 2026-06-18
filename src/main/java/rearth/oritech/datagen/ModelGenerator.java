package rearth.oritech.datagen;

import com.mojang.math.Quadrant;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.registries.DeferredItem;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.util.RegistryReflectionUtil;

import java.util.Set;
import java.util.stream.Stream;

import static net.minecraft.client.data.models.BlockModelGenerators.*;

public class ModelGenerator extends ModelProvider {

    // Blocks whose blockstate + model JSONs are hand-authored under src/main/resources
    // (BlockBench-exported custom geometry) and therefore excluded from datagen validation.
    private static final Set<Block> HAND_AUTHORED_BLOCKS = Set.of(
            BlockContent.FRAME_GANTRY_ARM.get(),
            BlockContent.MACHINE_SPEED_ADDON.get(),
            BlockContent.MACHINE_EFFICIENCY_ADDON.get(),
            BlockContent.MACHINE_ULTIMATE_ADDON.get(),
            BlockContent.QUARRY_ADDON.get(),
            BlockContent.MACHINE_PROCESSING_ADDON.get(),
            BlockContent.MACHINE_FLUID_ADDON.get(),
            BlockContent.MACHINE_YIELD_ADDON.get(),
            BlockContent.CROP_FILTER_ADDON.get(),
            BlockContent.MACHINE_HUNTER_ADDON.get(),
            BlockContent.MACHINE_CAPACITOR_ADDON.get(),
            BlockContent.MACHINE_ACCEPTOR_ADDON.get(),
            BlockContent.MACHINE_INVENTORY_PROXY_ADDON.get(),
            BlockContent.STEAM_BOILER_ADDON.get(),
            BlockContent.MACHINE_REDSTONE_ADDON.get(),
            BlockContent.MACHINE_SILK_TOUCH_ADDON.get(),
            BlockContent.MACHINE_BURST_ADDON.get(),
            BlockContent.MACHINE_COMBI_ADDON.get(),
            BlockContent.MACHINE_PLATING_BLOCK.get(),
            BlockContent.IRON_PLATING_BLOCK.get(),
            BlockContent.CARBON_PLATING_BLOCK.get(),
            BlockContent.NICKEL_PLATING_BLOCK.get()
    );


    public ModelGenerator(PackOutput output) {
        super(output, Oritech.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockStateModelGenerator, ItemModelGenerators itemModelGenerator) {
        generateBlockStateModels(blockStateModelGenerator);
        generateItemModels(itemModelGenerator);
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return super.getKnownBlocks().filter(h -> !HAND_AUTHORED_BLOCKS.contains(h.value()));
    }

    private void generateBlockStateModels(BlockModelGenerators generator) {

        createNonTemplateModelBlock(BlockContent.MACHINE_FRAME_BLOCK.get(), generator);
        createNonTemplateModelBlock(BlockContent.FLUID_PIPE.get(), generator);
        createNonTemplateModelBlock(BlockContent.ENERGY_PIPE.get(), generator);
        createNonTemplateModelBlock(BlockContent.SUPERCONDUCTOR.get(), generator);
        createNonTemplateModelBlock(BlockContent.ITEM_PIPE.get(), generator);
        createNonTemplateModelBlock(BlockContent.TRANSPARENT_ITEM_PIPE.get(), generator);
        createNonTemplateModelBlock(BlockContent.FLUID_PIPE_CONNECTION.get(), generator);
        createNonTemplateModelBlock(BlockContent.ENERGY_PIPE_CONNECTION.get(), generator);
        createNonTemplateModelBlock(BlockContent.SUPERCONDUCTOR_CONNECTION.get(), generator);
        createNonTemplateModelBlock(BlockContent.ITEM_PIPE_CONNECTION.get(), generator);
        createNonTemplateModelBlock(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.get(), generator);
        createNonTemplateModelBlock(BlockContent.FRAMED_FLUID_PIPE.get(), generator);
        createNonTemplateModelBlock(BlockContent.FRAMED_ENERGY_PIPE.get(), generator);
        createNonTemplateModelBlock(BlockContent.FRAMED_SUPERCONDUCTOR.get(), generator);
        createNonTemplateModelBlock(BlockContent.FRAMED_ITEM_PIPE.get(), generator);
        createNonTemplateModelBlock(BlockContent.FRAMED_FLUID_PIPE_CONNECTION.get(), generator);
        createNonTemplateModelBlock(BlockContent.FRAMED_ENERGY_PIPE_CONNECTION.get(), generator);
        createNonTemplateModelBlock(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION.get(), generator);
        createNonTemplateModelBlock(BlockContent.FRAMED_ITEM_PIPE_CONNECTION.get(), generator);
        generator.createTrivialCube(BlockContent.FLUID_PIPE_DUCT_BLOCK.get());
        generator.createTrivialCube(BlockContent.ENERGY_PIPE_DUCT_BLOCK.get());
        generator.createTrivialCube(BlockContent.SUPERCONDUCTOR_DUCT_BLOCK.get());
        generator.createTrivialCube(BlockContent.ITEM_PIPE_DUCT_BLOCK.get());
        createNonTemplateModelBlock(BlockContent.ITEM_FILTER_BLOCK.get(), generator);
        createNonTemplateModelBlock(BlockContent.SIMPLE_AUGMENT_STATION.get(), generator);
        createNonTemplateModelBlock(BlockContent.ADVANCED_AUGMENT_STATION.get(), generator);
        createNonTemplateModelBlock(BlockContent.ARCANE_AUGMENT_STATION.get(), generator);

        createNonTemplateModelBlock(BlockContent.SMALL_STORAGE_BLOCK.get(), generator);
        createNonTemplateModelBlock(BlockContent.LARGE_STORAGE_BLOCK.get(), generator);
        createNonTemplateModelBlock(BlockContent.CREATIVE_STORAGE_BLOCK.get(), generator);
        createNonTemplateModelBlock(BlockContent.SMALL_TANK_BLOCK.get(), generator);
        createNonTemplateModelBlock(BlockContent.CREATIVE_TANK_BLOCK.get(), generator);

        createNonTemplateHorizontalBlock(BlockContent.PLACER_BLOCK.get(), generator);
        createNonTemplateHorizontalBlock(BlockContent.DESTROYER_BLOCK.get(), generator);
        createNonTemplateHorizontalBlock(BlockContent.FERTILIZER_BLOCK.get(), generator);

        createNonTemplateHorizontalBlock(BlockContent.POWER_POLE_BLOCK.get(), generator);

        createNonTemplateModelBlock(BlockContent.PUMP_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.CHARGER_BLOCK.get(), generator);

        generator.createTrivialCube(BlockContent.ADDON_INDICATOR_BLOCK.get());
        generator.createTrivialCube(BlockContent.BLOCK_DESTROYER_HEAD.get());
        generator.createTrivialCube(BlockContent.BLOCK_PLACER_HEAD.get());
        generator.createTrivialCube(BlockContent.BLOCK_FERTILIZER_HEAD.get());
        generator.createTrivialCube(BlockContent.PUMP_TRUNK_BLOCK.get());
        createNonTemplateModelBlock(BlockContent.QUARRY_BEAM_RING.get(), generator);

        // reactor section
        createNonTemplateHorizontalBlock(BlockContent.REACTOR_CONTROLLER.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_ENERGY_PORT.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_REDSTONE_PORT.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_FUEL_PORT.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_ABSORBER_PORT.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_ROD.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_DOUBLE_ROD.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_QUAD_ROD.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_WALL.get(), Blocks.BRICKS, generator); // this is overridden by athena
        createNonTemplateModelBlock(BlockContent.REACTOR_VENT.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_REFLECTOR.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_HEAT_PIPE.get(), generator);
        generator.createTrivialCube(BlockContent.REACTOR_CONDENSER.get());

        generator.createAmethystCluster(BlockContent.URANIUM_CRYSTAL.get());

        generator.createTrivialCube(BlockContent.LOW_YIELD_NUKE.get());
        generator.createTrivialCube(BlockContent.NUKE.get());

        generator.createTrivialCube(BlockContent.REACTOR_COLD_INDICATOR_BLOCK.get());
        generator.createTrivialCube(BlockContent.REACTOR_MEDIUM_INDICATOR_BLOCK.get());
        generator.createTrivialCube(BlockContent.REACTOR_HOT_INDICATOR_BLOCK.get());

        // these blocks all use geckolib to render/display, so the only thing this really adds are block particles (e.g. when breaking)
        createNonTemplateModelBlock(BlockContent.PULVERIZER_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.FRAGMENT_FORGE_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.ASSEMBLER_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.FOUNDRY_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.REFINERY_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.TAINTED_REFINERY_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.REFINERY_MODULE_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.COOLER_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.CENTRIFUGE_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.ATOMIC_FORGE_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.POWERED_FURNACE_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.AUGMENT_APPLICATION_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.LASER_ARM_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.BIO_GENERATOR_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.FUEL_GENERATOR_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.BASIC_GENERATOR_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.LAVA_GENERATOR_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.BIG_SOLAR_PANEL_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.DEEP_DRILL_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.DRONE_PORT_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.SHRINKER_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.TECH_DOOR.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.TECH_DOOR_HINGE.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.HANGAR_DOOR.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.HANGAR_DOOR_HELPER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.TREEFELLER_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.STEAM_ENGINE_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.PIPE_BOOSTER_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_EXPLOSION_SMALL.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_EXPLOSION_MEDIUM.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_EXPLOSION_LARGE.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);

        // todo
//        for (var fluid : FluidContent.FLUID_ATTRIBUTES)
//            createNonTemplateModelBlock(fluid.getBlock(), Blocks.WATER, generator);

        generator.createTrivialCube(BlockContent.MACHINE_CORE_1.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_2.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_3.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_4.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_5.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_6.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_7.get());
        createNonTemplateModelBlock(BlockContent.MACHINE_CORE_HIDDEN.get(), Blocks.AIR, generator);   // never visible

        generator.createTrivialCube(BlockContent.MACHINE_EXTENDER.get());

        //arcane
        createNonTemplateModelBlock(BlockContent.SPAWNER_CAGE_BLOCK.get(), generator);
        createNonTemplateModelBlock(BlockContent.SPAWNER_CONTROLLER_BLOCK.get(), generator);
        generator.createCropBlock(BlockContent.WITHER_CROP_BLOCK.get(), CropBlock.AGE, 0, 1, 1, 2, 3, 3, 4, 5);
        createNonTemplateModelBlock(BlockContent.ENCHANTER_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.UNSTABLE_CONTAINER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.ENCHANTMENT_CATALYST_BLOCK.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);

        // particle accelerator
        createNonTemplateModelBlock(BlockContent.ACCELERATOR_RING.get(), generator);
        createNonTemplateHorizontalBlock(BlockContent.ACCELERATOR_MOTOR.get(), generator);
        createNonTemplateHorizontalBlock(BlockContent.ACCELERATOR_CONTROLLER.get(), generator);
        createNonTemplateHorizontalBlock(BlockContent.ACCELERATOR_SENSOR.get(), generator);
        createNonTemplateModelBlock(BlockContent.BLACK_HOLE_BLOCK.get(), generator);
        createNonTemplateModelBlock(BlockContent.BLACK_HOLE_INNER.get(), generator);
        createNonTemplateModelBlock(BlockContent.BLACK_HOLE_MIDDLE.get(), generator);
        createNonTemplateModelBlock(BlockContent.BLACK_HOLE_OUTER.get(), generator);
        createNonTemplateModelBlock(BlockContent.PARTICLE_COLLECTOR_BLOCK.get(), generator);

        // metals
        generator.createTrivialCube(BlockContent.NICKEL_ORE.get());
        generator.createTrivialCube(BlockContent.DEEPSLATE_NICKEL_ORE.get());
        generator.createTrivialCube(BlockContent.ENDSTONE_PLATINUM_ORE.get());
        generator.createTrivialCube(BlockContent.DEEPSLATE_PLATINUM_ORE.get());
        generator.createTrivialCube(BlockContent.DEEPSLATE_URANIUM_ORE.get());

        // NODES
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_REDSTONE.get());
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_LAPIS.get());
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_IRON.get());
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_COAL.get());
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_GOLD.get());
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_EMERALD.get());
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_DIAMOND.get());
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_COPPER.get());
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_NICKEL.get());
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_PLATINUM.get());
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_URANIUM.get());

        //decorative
        generator.createTrivialCube(BlockContent.INDUSTRIAL_GLASS_BLOCK.get());
        generator.createTrivialCube(BlockContent.CAPACITOR_ADDON_EXTENDER.get());
        createNonTemplateModelBlock(BlockContent.METAL_BEAM_BLOCK.get(), generator);
        createNonTemplateModelBlock(BlockContent.METAL_GIRDER_BLOCK.get(), generator);

        generator.blockStateOutput.accept(createWallMountedState(BlockContent.CEILING_LIGHT.get()));
        generator.blockStateOutput.accept(createWallMountedState(BlockContent.CEILING_LIGHT_HANGING.get()));
        registerLever(BlockContent.TECH_LEVER.get(), generator);
        registerButton(BlockContent.TECH_BUTTON.get(), TextureMapping.cube(BlockContent.MACHINE_PLATING_BLOCK.get()), generator);


        generator.createTrivialCube(BlockContent.STEEL_BLOCK.get());
        generator.createTrivialCube(BlockContent.ENERGITE_BLOCK.get());
        generator.createTrivialCube(BlockContent.NICKEL_BLOCK.get());
        generator.createTrivialCube(BlockContent.BIOSTEEL_BLOCK.get());
        generator.createTrivialCube(BlockContent.PLATINUM_BLOCK.get());
        generator.createTrivialCube(BlockContent.ADAMANT_BLOCK.get());
        generator.createTrivialCube(BlockContent.ELECTRUM_BLOCK.get());
        generator.createTrivialCube(BlockContent.DURATIUM_BLOCK.get());
        generator.createTrivialCube(BlockContent.BIOMASS_BLOCK.get());
        generator.createTrivialCube(BlockContent.PLASTIC_BLOCK.get());
        generator.createTrivialCube(BlockContent.FLUXITE_BLOCK.get());
        generator.createTrivialCube(BlockContent.SILICON_BLOCK.get());
        generator.createTrivialCube(BlockContent.RAW_NICKEL_BLOCK.get());
        generator.createTrivialCube(BlockContent.RAW_PLATINUM_BLOCK.get());
        generator.createTrivialCube(BlockContent.RAW_URANIUM_BLOCK.get());
        generator.createTrivialCube(BlockContent.URANIUM_DUST_BLOCK.get());

        var machinePlatingPool = generator.familyWithExistingFullBlock(BlockContent.MACHINE_PLATING_BLOCK.get());
        machinePlatingPool.stairs(BlockContent.MACHINE_PLATING_STAIRS.get());
        machinePlatingPool.slab(BlockContent.MACHINE_PLATING_SLAB.get());
        machinePlatingPool.pressurePlate(BlockContent.MACHINE_PLATING_PRESSURE_PLATE.get());

        var ironPlatingPool = generator.familyWithExistingFullBlock(BlockContent.IRON_PLATING_BLOCK.get());
        ironPlatingPool.stairs(BlockContent.IRON_PLATING_STAIRS.get());
        ironPlatingPool.slab(BlockContent.IRON_PLATING_SLAB.get());
        ironPlatingPool.pressurePlate(BlockContent.IRON_PLATING_PRESSURE_PLATE.get());

        var nickelPlatingPool = generator.familyWithExistingFullBlock(BlockContent.NICKEL_PLATING_BLOCK.get());
        nickelPlatingPool.stairs(BlockContent.NICKEL_PLATING_STAIRS.get());
        nickelPlatingPool.slab(BlockContent.NICKEL_PLATING_SLAB.get());
        nickelPlatingPool.pressurePlate(BlockContent.NICKEL_PLATING_PRESSURE_PLATE.get());

        var carbonPlatingPool = generator.familyWithExistingFullBlock(BlockContent.CARBON_PLATING_BLOCK.get());
        carbonPlatingPool.stairs(BlockContent.CARBON_PLATING_STAIRS.get());
        carbonPlatingPool.slab(BlockContent.CARBON_PLATING_SLAB.get());
        carbonPlatingPool.pressurePlate(BlockContent.CARBON_PLATING_PRESSURE_PLATE.get());
    }

    private void generateItemModels(ItemModelGenerators itemModelGenerator) {
        itemModelGenerator.generateFlatItem(ToolsContent.EXO_HELMET.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.EXO_CHESTPLATE.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.EXO_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.EXO_BOOTS.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.CHAINSAW.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.HAND_DRILL.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.ELECTRIC_MACE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.JETPACK.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.EXO_JETPACK.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.JETPACK_ELYTRA.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.JETPACK_EXO_ELYTRA.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.PORTABLE_LASER.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.PROMETHIUM_AXE.get(), ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.PROMETHIUM_PICKAXE.get(), ModelTemplates.FLAT_ITEM);

        RegistryReflectionUtil.ForEachPublicStaticField(ItemContent.class, DeferredItem.class, (field, id, item) -> {
            itemModelGenerator.generateFlatItem(item.asItem(), ModelTemplates.FLAT_ITEM);
        });

        for (var bucketItem : FluidContent.BUCKET_ITEMS.getEntries()) {
            itemModelGenerator.generateFlatItem(bucketItem.get(), ModelTemplates.FLAT_ITEM);
        }

    }

    // Custom Helpers replacing Fabric's BlockModelGenerators extensions
    private void createNonTemplateModelBlock(Block block, BlockModelGenerators generator) {
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.variant(new Variant(ModelLocationUtils.getModelLocation(block)))));
    }

    private void createNonTemplateModelBlock(Block block, Block baseModelBlock, BlockModelGenerators generator) {
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.variant(new Variant(ModelLocationUtils.getModelLocation(baseModelBlock)))));
    }

    private void createNonTemplateHorizontalBlock(Block block, BlockModelGenerators generator) {
        var variant = new Variant(ModelLocationUtils.getModelLocation(block));
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.variant(variant))
                .with(PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
                        .select(Direction.EAST, BlockModelGenerators.Y_ROT_90)
                        .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                        .select(Direction.WEST, Y_ROT_270)
                        .select(Direction.NORTH, BlockModelGenerators.NOP)));
    }

    // no idea what this is. Just tried to update it somehow
    public static MultiVariantGenerator createWallMountedState(Block block) {
        var loc = ModelLocationUtils.getModelLocation(block);
        return MultiVariantGenerator.dispatch(block, BlockModelGenerators.variant(new Variant(loc)))
                .with(PropertyDispatch.modify(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)

                        .select(AttachFace.FLOOR, Direction.EAST, VariantMutator.Y_ROT.withValue(Quadrant.R90))
                        .select(AttachFace.FLOOR, Direction.WEST, VariantMutator.Y_ROT.withValue(Quadrant.R270))
                        .select(AttachFace.FLOOR, Direction.SOUTH, VariantMutator.Y_ROT.withValue(Quadrant.R180))
                        .select(AttachFace.FLOOR, Direction.NORTH, VariantMutator.X_ROT.withValue(Quadrant.R0)) // not sure how to do this better
                        .select(AttachFace.WALL, Direction.EAST, VariantMutator.Y_ROT.withValue(Quadrant.R90).then(VariantMutator.X_ROT.withValue(Quadrant.R90)))
                        .select(AttachFace.WALL, Direction.WEST, VariantMutator.Y_ROT.withValue(Quadrant.R270).then(VariantMutator.X_ROT.withValue(Quadrant.R90)))
                        .select(AttachFace.WALL, Direction.SOUTH, VariantMutator.Y_ROT.withValue(Quadrant.R180).then(VariantMutator.X_ROT.withValue(Quadrant.R90)))
                        .select(AttachFace.WALL, Direction.NORTH, VariantMutator.X_ROT.withValue(Quadrant.R90))
                        .select(AttachFace.CEILING, Direction.EAST, VariantMutator.Y_ROT.withValue(Quadrant.R270).then(VariantMutator.X_ROT.withValue(Quadrant.R180)))
                        .select(AttachFace.CEILING, Direction.WEST, VariantMutator.Y_ROT.withValue(Quadrant.R90).then(VariantMutator.X_ROT.withValue(Quadrant.R180)))
                        .select(AttachFace.CEILING, Direction.SOUTH, VariantMutator.X_ROT.withValue(Quadrant.R180))
                        .select(AttachFace.CEILING, Direction.NORTH, VariantMutator.Y_ROT.withValue(Quadrant.R180).then(VariantMutator.X_ROT.withValue(Quadrant.R180))));
    }

    public static void registerButton(Block button, TextureMapping mapping, BlockModelGenerators generator) {

        var unpressed = plainVariant(ModelTemplates.BUTTON.create(button, mapping, generator.modelOutput));
        var pressed = plainVariant(ModelTemplates.BUTTON_PRESSED.create(button, mapping, generator.modelOutput));
        generator.blockStateOutput.accept(BlockModelGenerators.createButton(button, unpressed, pressed));

        var inventory = ModelTemplates.BUTTON_INVENTORY.create(button, mapping, generator.modelOutput);
        generator.registerSimpleItemModel(button, inventory);
    }

    // copy of net.minecraft.client.data.models.BlockModelGenerators.createLever
    public static void registerLever(Block block, BlockModelGenerators generator) {
        var off = plainVariant(ModelLocationUtils.getModelLocation(block));
        var on = plainVariant(ModelLocationUtils.getModelLocation(block, "_on"));
        generator.registerSimpleFlatItemModel(block);
        generator.blockStateOutput
                .accept(
                        MultiVariantGenerator.dispatch(block)
                                .with(createBooleanModelDispatch(BlockStateProperties.POWERED, off, on))
                                .with(
                                        PropertyDispatch.modify(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                                                .select(AttachFace.CEILING, Direction.NORTH, X_ROT_180.then(Y_ROT_180))
                                                .select(AttachFace.CEILING, Direction.EAST, X_ROT_180.then(Y_ROT_270))
                                                .select(AttachFace.CEILING, Direction.SOUTH, X_ROT_180)
                                                .select(AttachFace.CEILING, Direction.WEST, X_ROT_180.then(Y_ROT_90))
                                                .select(AttachFace.FLOOR, Direction.NORTH, NOP)
                                                .select(AttachFace.FLOOR, Direction.EAST, Y_ROT_90)
                                                .select(AttachFace.FLOOR, Direction.SOUTH, Y_ROT_180)
                                                .select(AttachFace.FLOOR, Direction.WEST, Y_ROT_270)
                                                .select(AttachFace.WALL, Direction.NORTH, X_ROT_90)
                                                .select(AttachFace.WALL, Direction.EAST, X_ROT_90.then(Y_ROT_90))
                                                .select(AttachFace.WALL, Direction.SOUTH, X_ROT_90.then(Y_ROT_180))
                                                .select(AttachFace.WALL, Direction.WEST, X_ROT_90.then(Y_ROT_270))
                                )
                );
    }
}