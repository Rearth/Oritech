package rearth.oritech.datagen;

import com.google.gson.JsonObject;
import com.mojang.math.Quadrant;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
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
            BlockContent.SYNERGY_MATRIX_ADDON.get(),
            BlockContent.QUARRY_ADDON.get(),
            BlockContent.AUXILIARY_PROCESSING_CHAMBER_ADDON.get(),
            BlockContent.MACHINE_FLUID_ADDON.get(),
            BlockContent.MACHINE_YIELD_ADDON.get(),
            BlockContent.CROP_FILTER_ADDON.get(),
            BlockContent.MACHINE_HUNTER_ADDON.get(),
            BlockContent.MACHINE_CAPACITOR_ADDON.get(),
            BlockContent.MACHINE_ACCEPTOR_ADDON.get(),
            BlockContent.MACHINE_INVENTORY_PROXY_ADDON.get(),
            BlockContent.STEAM_BOILER_ADDON.get(),
            BlockContent.CONTROL_UNIT_ADDON.get(),
            BlockContent.MACHINE_SILK_TOUCH_ADDON.get(),
            BlockContent.MACHINE_BURST_ADDON.get(),
            BlockContent.HEART_OF_THE_MACHINE_ADDON.get(),
            BlockContent.COPPER_REINFORCED_PLATING.get(),
            BlockContent.IRON_PLATING.get(),
            BlockContent.CARBON_PLATING.get(),
            BlockContent.NICKEL_PLATING.get(),
            BlockContent.SMART_SPLITTER.get()
    );

    private static final Set<Block> HAND_AUTHORED_ITEM_MODEL_BLOCKS = Set.of(
            BlockContent.MACHINE_SPEED_ADDON.get(),
            BlockContent.MACHINE_EFFICIENCY_ADDON.get(),
            BlockContent.SYNERGY_MATRIX_ADDON.get(),
            BlockContent.QUARRY_ADDON.get(),
            BlockContent.AUXILIARY_PROCESSING_CHAMBER_ADDON.get(),
            BlockContent.MACHINE_FLUID_ADDON.get(),
            BlockContent.MACHINE_YIELD_ADDON.get(),
            BlockContent.CROP_FILTER_ADDON.get(),
            BlockContent.MACHINE_HUNTER_ADDON.get(),
            BlockContent.MACHINE_CAPACITOR_ADDON.get(),
            BlockContent.MACHINE_ACCEPTOR_ADDON.get(),
            BlockContent.MACHINE_INVENTORY_PROXY_ADDON.get(),
            BlockContent.STEAM_BOILER_ADDON.get(),
            BlockContent.CONTROL_UNIT_ADDON.get(),
            BlockContent.MACHINE_SILK_TOUCH_ADDON.get(),
            BlockContent.MACHINE_BURST_ADDON.get(),
            BlockContent.HEART_OF_THE_MACHINE_ADDON.get()
    );

    private static final Set<Item> GECKOLIB_ITEM_MODELS = Set.of(
            ToolsContent.ENDERIC_RAILGUN.get(),
            ToolsContent.PROMETHIUM_AXE.get(),
            ToolsContent.PROMETHIUM_PICKAXE.get(),
            ItemContent.SCHRODINGERS_SAFE.get()
    );

    private static final Set<Item> HAND_AUTHORED_ITEM_DEFINITIONS = Set.of(
            ItemContent.PORTABLE_ENERGY_STORAGE_ITEM.get(),
            ItemContent.PORTABLE_TANK_ITEM.get(),
            ItemContent.CREATIVE_TANK_ITEM.get(),
            BlockContent.SMART_SPLITTER.asItem()
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

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return super.getKnownItems()
                .filter(h -> !GECKOLIB_ITEM_MODELS.contains(h.value()) && !HAND_AUTHORED_ITEM_DEFINITIONS.contains(h.value()));
    }

    private void generateBlockStateModels(BlockModelGenerators generator) {

        createNonTemplateModelBlock(BlockContent.MACHINE_FRAME.get(), generator);
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
        createModelAlias(BlockContent.FRAMED_FLUID_PIPE.get(), Oritech.id("block/framed_pipe"), generator);
        createModelAlias(BlockContent.FRAMED_ENERGY_PIPE.get(), Oritech.id("block/framed_pipe"), generator);
        createModelAlias(BlockContent.FRAMED_SUPERCONDUCTOR.get(), Oritech.id("block/framed_pipe"), generator);
        createModelAlias(BlockContent.FRAMED_ITEM_PIPE.get(), Oritech.id("block/framed_pipe"), generator);
        createModelAlias(BlockContent.FRAMED_FLUID_PIPE_CONNECTION.get(), Oritech.id("block/framed_pipe_skinny"), generator);
        createModelAlias(BlockContent.FRAMED_ENERGY_PIPE_CONNECTION.get(), Oritech.id("block/framed_pipe_skinny"), generator);
        createModelAlias(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION.get(), Oritech.id("block/framed_pipe_skinny"), generator);
        createModelAlias(BlockContent.FRAMED_ITEM_PIPE_CONNECTION.get(), Oritech.id("block/framed_pipe_skinny"), generator);
        generator.createTrivialCube(BlockContent.FLUID_PIPE_DUCT.get());
        generator.createTrivialCube(BlockContent.ENERGY_PIPE_DUCT.get());
        generator.createTrivialCube(BlockContent.SUPERCONDUCTOR_DUCT.get());
        generator.createTrivialCube(BlockContent.ITEM_PIPE_DUCT.get());
        createNonTemplateModelBlock(BlockContent.ITEM_FILTER.get(), generator);
        createNonTemplateModelBlock(BlockContent.CYBERNETIC_RESEARCH_STATION.get(), generator);
        createNonTemplateModelBlock(BlockContent.QUANTUM_RESEARCH_STATION.get(), generator);
        createNonTemplateModelBlock(BlockContent.ARCANE_AUGMENT_STATION.get(), generator);

        createNonTemplateModelBlock(BlockContent.PORTABLE_ENERGY_STORAGE.get(), generator);
        createNonTemplateModelBlock(BlockContent.LARGE_STORAGE.get(), generator);
        createNonTemplateModelBlock(BlockContent.CREATIVE_STORAGE.get(), generator);
        createNonTemplateModelBlock(BlockContent.PORTABLE_TANK.get(), generator);
        createNonTemplateModelBlock(BlockContent.CREATIVE_TANK.get(), generator);

        createNonTemplateHorizontalBlock(BlockContent.PLACER.get(), generator);
        createNonTemplateHorizontalBlock(BlockContent.DESTROYER.get(), generator);
        createNonTemplateHorizontalBlock(BlockContent.FERTILIZER.get(), generator);

        createNonTemplateHorizontalBlock(BlockContent.ENERGY_TRANSMISSION_POLE.get(), generator);

        createNonTemplateModelBlock(BlockContent.PUMP.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.EQUIPMENT_CHARGER.get(), generator);

        generator.createTrivialCube(BlockContent.ADDON_INDICATOR.get());
        generator.createTrivialCube(BlockContent.BLOCK_DESTROYER_HEAD.get());
        generator.createTrivialCube(BlockContent.BLOCK_PLACER_HEAD.get());
        generator.createTrivialCube(BlockContent.BLOCK_FERTILIZER_HEAD.get());
        generator.createTrivialCube(BlockContent.PUMP_TRUNK.get());
        createNonTemplateModelBlock(BlockContent.QUARRY_BEAM_RING.get(), generator);

        // reactor section
        createNonTemplateHorizontalBlock(BlockContent.NUCLEAR_REACTOR_CONTROLLER.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_ENERGY_PORT.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_REDSTONE_PORT.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_FUEL_PORT.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_COOLANT_ABSORBER_PORT.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_ROD.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_DOUBLE_ROD.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_QUAD_ROD.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_WALL.get(), Blocks.BRICKS, generator); // this is overridden by athena
        createNonTemplateModelBlock(BlockContent.REACTOR_HEAT_VENT.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_NEUTRON_REFLECTOR.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_HEAT_PIPE.get(), generator);
        generator.createTrivialCube(BlockContent.REACTOR_HEAT_ABSORBER.get());

        generator.createAmethystCluster(BlockContent.URANITE_CRYSTAL.get());

        generator.createTrivialCube(BlockContent.LOW_YIELD_NUCLEAR_EXPLOSION_DEVICE.get());
        generator.createTrivialCube(BlockContent.MANHATTAN_MODULE.get());

        generator.createTrivialCube(BlockContent.REACTOR_COLD_INDICATOR.get());
        generator.createTrivialCube(BlockContent.REACTOR_MEDIUM_INDICATOR.get());
        generator.createTrivialCube(BlockContent.REACTOR_HOT_INDICATOR.get());

        // these blocks all use geckolib to render/display, so the only thing this really adds are block particles (e.g. when breaking)
        createNonTemplateModelBlock(BlockContent.PULVERIZER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.FRAGMENT_FORGE.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.ASSEMBLER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.FOUNDRY.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.REFINERY.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.TAINTED_REFINERY.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.REFINERY_CHAMBER_MODULE.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.INDUSTRIAL_CHILLER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.CENTRIFUGE.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.ATOMIC_FORGE.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.POWERED_FURNACE.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.CYBERNETIC_AUGMENTATION_CENTER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.ENDERIC_LASER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.BIO_GENERATOR.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.FUEL_GENERATOR.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.BASIC_GENERATOR.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.LAVA_GENERATOR.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.BIG_SOLAR_PANEL.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.BEDROCK_EXTRACTOR.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.DRONE_PORT.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.ADDON_SPLICER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.INDUSTRIAL_DOOR.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.INDUSTRIAL_DOOR_HINGE.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.HANGAR_DOOR.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.HANGAR_DOOR_HELPER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.TREE_CUTTER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.STEAM_ENGINE.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.PIPE_BOOSTER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_EXPLOSION_SMALL.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_EXPLOSION_MEDIUM.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.REACTOR_EXPLOSION_LARGE.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);

        for (var fluid : FluidContent.FLUID_BLOCKS.getEntries()) {
            createNonTemplateModelBlock(fluid.get(), Blocks.WATER, generator);
        }

        generator.createTrivialCube(BlockContent.MACHINE_CORE_1.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_2.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_3.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_4.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_5.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_6.get());
        generator.createTrivialCube(BlockContent.MACHINE_CORE_7.get());
        createNonTemplateModelBlock(BlockContent.COMPLEX_PLATING.get(), Blocks.AIR, generator);   // never visible

        generator.createTrivialCube(BlockContent.MACHINE_EXTENDER.get());

        //arcane
        createNonTemplateModelBlock(BlockContent.SPAWNER_CAGE.get(), generator);
        createNonTemplateModelBlock(BlockContent.SPAWNER_CONTROLLER.get(), generator);
        generator.createCropBlock(BlockContent.SOUL_FLOWERS.get(), CropBlock.AGE, 0, 1, 1, 2, 3, 3, 4, 5);
        createNonTemplateModelBlock(BlockContent.STABILIZED_ENCHANTER.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.SCHRODINGERS_SAFE.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);
        createNonTemplateModelBlock(BlockContent.ARCANE_CATALYST.get(), BlockContent.MACHINE_SPEED_ADDON.get(), generator);

        // particle accelerator
        createNonTemplateModelBlock(BlockContent.ACCELERATOR_RING.get(), generator);
        createNonTemplateHorizontalBlock(BlockContent.ACCELERATOR_MOTOR.get(), generator);
        createNonTemplateHorizontalBlock(BlockContent.PARTICLE_ACCELERATOR.get(), generator);
        createNonTemplateHorizontalBlock(BlockContent.ACCELERATOR_SENSOR.get(), generator);
        createNonTemplateModelBlock(BlockContent.BLACK_HOLE.get(), generator);
        createNonTemplateModelBlock(BlockContent.BLACK_HOLE_INNER.get(), generator);
        createNonTemplateModelBlock(BlockContent.BLACK_HOLE_MIDDLE.get(), generator);
        createNonTemplateModelBlock(BlockContent.BLACK_HOLE_OUTER.get(), generator);
        createNonTemplateModelBlock(BlockContent.TACHYON_ABSORBER.get(), generator);

        // metals
        generator.createTrivialCube(BlockContent.NICKEL_ORE.get());
        generator.createTrivialCube(BlockContent.DEEPSLATE_NICKEL_ORE.get());
        generator.createTrivialCube(BlockContent.ENDSTONE_PLATINUM_ORE.get());
        generator.createTrivialCube(BlockContent.DEEPSLATE_PLATINUM_ORE.get());
        generator.createTrivialCube(BlockContent.DEEPSLATE_URANIUM_ORE.get());

        // NODES
        generator.createTrivialCube(BlockContent.REDSTONE_RESOURCE_NODE.get());
        generator.createTrivialCube(BlockContent.RESOURCE_NODE_LAPIS.get());
        generator.createTrivialCube(BlockContent.IRON_RESOURCE_NODE.get());
        generator.createTrivialCube(BlockContent.COAL_RESOURCE_NODE.get());
        generator.createTrivialCube(BlockContent.GOLD_RESOURCE_NODE.get());
        generator.createTrivialCube(BlockContent.EMERALD_RESOURCE_NODE.get());
        generator.createTrivialCube(BlockContent.DIAMOND_RESOURCE_NODE.get());
        generator.createTrivialCube(BlockContent.COPPER_RESOURCE_NODE.get());
        generator.createTrivialCube(BlockContent.NICKEL_RESOURCE_NODE.get());
        generator.createTrivialCube(BlockContent.PLATINUM_RESOURCE_NODE.get());
        generator.createTrivialCube(BlockContent.URANIUM_RESOURCE_NODE.get());

        //decorative
        generator.createTrivialCube(BlockContent.INDUSTRIAL_GLASS.get());
        generator.createTrivialCube(BlockContent.POWER_BANK_ADDON_EXTENDER.get());
        createNonTemplateModelBlock(BlockContent.INDUSTRIAL_SUPPORT_BEAM.get(), generator);
        createNonTemplateModelBlock(BlockContent.INDUSTRIAL_SUPPORT_GIRDER.get(), generator);

        generator.blockStateOutput.accept(createWallMountedState(BlockContent.INDUSTRIAL_LIGHT.get()));
        generator.blockStateOutput.accept(createWallMountedState(BlockContent.INDUSTRIAL_LIGHT_HANGING.get()));
        registerLever(BlockContent.INDUSTRIAL_LEVER.get(), generator);
        registerButton(BlockContent.INDUSTRIAL_BUTTON.get(), TextureMapping.cube(BlockContent.COPPER_REINFORCED_PLATING.get()), generator);


        generator.createTrivialCube(BlockContent.STEEL.get());
        generator.createTrivialCube(BlockContent.ENERGITE.get());
        generator.createTrivialCube(BlockContent.NICKEL.get());
        generator.createTrivialCube(BlockContent.BIOSTEEL.get());
        generator.createTrivialCube(BlockContent.PLATINUM.get());
        generator.createTrivialCube(BlockContent.ADAMANT.get());
        generator.createTrivialCube(BlockContent.ELECTRUM.get());
        generator.createTrivialCube(BlockContent.DURATIUM.get());
        generator.createTrivialCube(BlockContent.BIOMASS.get());
        generator.createTrivialCube(BlockContent.PLASTIC.get());
        generator.createTrivialCube(BlockContent.FLUXITE.get());
        generator.createTrivialCube(BlockContent.SILICON.get());
        generator.createTrivialCube(BlockContent.RAW_NICKEL.get());
        generator.createTrivialCube(BlockContent.RAW_PLATINUM.get());
        generator.createTrivialCube(BlockContent.RAW_URANIUM.get());
        generator.createTrivialCube(BlockContent.URANIUM.get());

        var copperReinforcedPlatingPool = generator.familyWithExistingFullBlock(BlockContent.COPPER_REINFORCED_PLATING.get());
        copperReinforcedPlatingPool.stairs(BlockContent.COPPER_REINFORCED_PLATING_STAIRS.get());
        copperReinforcedPlatingPool.slab(BlockContent.COPPER_REINFORCED_PLATING_SLAB.get());
        copperReinforcedPlatingPool.pressurePlate(BlockContent.COPPER_REINFORCED_PLATING_PRESSURE_PLATE.get());

        var ironPlatingPool = generator.familyWithExistingFullBlock(BlockContent.IRON_PLATING.get());
        ironPlatingPool.stairs(BlockContent.IRON_PLATING_STAIRS.get());
        ironPlatingPool.slab(BlockContent.IRON_PLATING_SLAB.get());
        ironPlatingPool.pressurePlate(BlockContent.IRON_PLATING_PRESSURE_PLATE.get());

        var nickelPlatingPool = generator.familyWithExistingFullBlock(BlockContent.NICKEL_PLATING.get());
        nickelPlatingPool.stairs(BlockContent.NICKEL_PLATING_STAIRS.get());
        nickelPlatingPool.slab(BlockContent.NICKEL_PLATING_SLAB.get());
        nickelPlatingPool.pressurePlate(BlockContent.NICKEL_PLATING_PRESSURE_PLATE.get());

        var carbonPlatingPool = generator.familyWithExistingFullBlock(BlockContent.CARBON_PLATING.get());
        carbonPlatingPool.stairs(BlockContent.CARBON_PLATING_STAIRS.get());
        carbonPlatingPool.slab(BlockContent.CARBON_PLATING_SLAB.get());
        carbonPlatingPool.pressurePlate(BlockContent.CARBON_PLATING_PRESSURE_PLATE.get());
    }

    private void generateItemModels(ItemModelGenerators itemModelGenerator) {
        HAND_AUTHORED_ITEM_MODEL_BLOCKS.forEach(block -> itemModelGenerator.itemModelOutput.accept(
                block.asItem(),
                ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(block.asItem()))
        ));

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

        itemModelGenerator.itemModelOutput.accept(
                ItemContent.PORTABLE_ENERGY_STORAGE_ITEM.get(),
                ItemModelUtils.plainModel(getBlockModelLocation(ItemContent.PORTABLE_ENERGY_STORAGE_ITEM.asItem()))
        );

        RegistryReflectionUtil.IterateFields(ItemContent.class, DeferredItem.class, (field, id, item) -> {
            if (HAND_AUTHORED_ITEM_DEFINITIONS.contains(item.asItem()) || GECKOLIB_ITEM_MODELS.contains(item.asItem()))
                return;

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

    private void createModelAlias(Block block, Identifier parent, BlockModelGenerators generator) {
        var model = new JsonObject();
        model.addProperty("parent", parent.toString());
        generator.modelOutput.accept(ModelLocationUtils.getModelLocation(block), () -> model);
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

    public static Identifier getBlockModelLocation(Item item) {
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        return key.withPrefix("block/");
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
