package rearth.oritech.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import oshi.util.tuples.Pair;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.accelerator.*;
import rearth.oritech.block.blocks.addons.*;
import rearth.oritech.block.blocks.addons.MachineAddonBlock.AddonSettings;
import rearth.oritech.block.blocks.arcane.*;
import rearth.oritech.block.blocks.augmenter.AugmentApplicationBlock;
import rearth.oritech.block.blocks.augmenter.AugmentResearchStationBlock;
import rearth.oritech.block.blocks.decorative.*;
import rearth.oritech.block.blocks.generators.*;
import rearth.oritech.block.blocks.interaction.*;
import rearth.oritech.block.blocks.pipes.energy.*;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeConnectionBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeDuctBlock;
import rearth.oritech.block.blocks.pipes.item.ItemFilterBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeConnectionBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeDuctBlock;
import rearth.oritech.block.blocks.processing.*;
import rearth.oritech.block.blocks.reactor.*;
import rearth.oritech.block.blocks.storage.*;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.init.ItemContent.Compostable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("NullableProblems")
public class BlockContent {
    
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Oritech.MOD_ID);
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(Oritech.MOD_ID);
    
    // hints for item groups
    public static final List<Pair<DeferredItem<BlockItem>, ItemContent.Groups>> BLOCK_GROUPS = new ArrayList<>();
    
    public static final DeferredBlock<Block> SPAWNER_CAGE_BLOCK = BLOCKS.registerBlock("spawner_cage_block", SpawnerCageBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()); // sample 1
    
    public static final DeferredBlock<Block> MACHINE_FRAME_BLOCK = BLOCKS.registerBlock("machine_frame_block", MachineFrameBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS)); // sample 2 new
    
    public static final DeferredBlock<Block> FLUID_PIPE = BLOCKS.registerBlock("fluid_pipe", FluidPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> FRAMED_FLUID_PIPE = BLOCKS.registerBlock("framed_fluid_pipe", FluidPipeBlock.FramedFluidPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> FLUID_PIPE_DUCT_BLOCK = BLOCKS.registerBlock("fluid_pipe_duct_block", FluidPipeDuctBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> ENERGY_PIPE = BLOCKS.registerBlock("energy_pipe", EnergyPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> FRAMED_ENERGY_PIPE = BLOCKS.registerBlock("framed_energy_pipe", EnergyPipeBlock.FramedEnergyPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> ENERGY_PIPE_DUCT_BLOCK = BLOCKS.registerBlock("energy_pipe_duct_block", EnergyPipeDuctBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> SUPERCONDUCTOR = BLOCKS.registerBlock("superconductor", SuperConductorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> FRAMED_SUPERCONDUCTOR = BLOCKS.registerBlock("framed_superconductor", SuperConductorBlock.FramedSuperConductorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> SUPERCONDUCTOR_DUCT_BLOCK = BLOCKS.registerBlock("superconductor_duct_block", SuperConductorDuctBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> ITEM_PIPE = BLOCKS.registerBlock("item_pipe", ItemPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> TRANSPARENT_ITEM_PIPE = BLOCKS.registerBlock("transparent_item_pipe", ItemPipeBlock.TransparentItemPipe::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> FRAMED_ITEM_PIPE = BLOCKS.registerBlock("framed_item_pipe", ItemPipeBlock.FramedItemPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> ITEM_PIPE_DUCT_BLOCK = BLOCKS.registerBlock("item_pipe_duct_block", ItemPipeDuctBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> ITEM_FILTER_BLOCK = BLOCKS.registerBlock("item_filter_block", ItemFilterBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    
    @NoBlockItem
    public static final DeferredBlock<Block> FLUID_PIPE_CONNECTION = BLOCKS.registerBlock("fluid_pipe_connection", FluidPipeConnectionBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final DeferredBlock<Block> FRAMED_FLUID_PIPE_CONNECTION = BLOCKS.registerBlock("framed_fluid_pipe_connection", FluidPipeConnectionBlock.FramedFluidPipeConnectionBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final DeferredBlock<Block> ENERGY_PIPE_CONNECTION = BLOCKS.registerBlock("energy_pipe_connection", EnergyPipeConnectionBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final DeferredBlock<Block> FRAMED_ENERGY_PIPE_CONNECTION = BLOCKS.registerBlock("framed_energy_pipe_connection", EnergyPipeConnectionBlock.FramedEnergyPipeConnectionBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final DeferredBlock<Block> SUPERCONDUCTOR_CONNECTION = BLOCKS.registerBlock("superconductor_connection", SuperConductorConnectionBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final DeferredBlock<Block> FRAMED_SUPERCONDUCTOR_CONNECTION = BLOCKS.registerBlock("framed_superconductor_connection", SuperConductorConnectionBlock.FramedSuperConductorConnectionBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final DeferredBlock<Block> ITEM_PIPE_CONNECTION = BLOCKS.registerBlock("item_pipe_connection", ItemPipeConnectionBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final DeferredBlock<Block> FRAMED_ITEM_PIPE_CONNECTION = BLOCKS.registerBlock("framed_item_pipe_connection", ItemPipeConnectionBlock.FramedItemPipeConnectionBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final DeferredBlock<Block> TRANSPARENT_ITEM_PIPE_CONNECTION = BLOCKS.registerBlock("transparent_item_pipe_connection", ItemPipeConnectionBlock.TransparentItemPipeConnectionBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    
    public static final DeferredBlock<Block> POWER_POLE_BLOCK = BLOCKS.registerBlock("power_pole_block", PowerPoleBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    @NoBlockItem
    public static final DeferredBlock<Block> FRAME_GANTRY_ARM = BLOCKS.registerSimpleBlock("frame_gantry_arm", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> BLOCK_DESTROYER_HEAD = BLOCKS.registerSimpleBlock("block_destroyer_head", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> BLOCK_PLACER_HEAD = BLOCKS.registerSimpleBlock("block_placer_head", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> BLOCK_FERTILIZER_HEAD = BLOCKS.registerSimpleBlock("block_fertilizer_head", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> PUMP_TRUNK_BLOCK = BLOCKS.registerSimpleBlock("pump_trunk_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> QUARRY_BEAM_RING = BLOCKS.registerSimpleBlock("quarry_beam_ring", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion().lightLevel(item -> 5));
    @NoBlockItem
    public static final DeferredBlock<Block> BLACK_HOLE_INNER = BLOCKS.registerSimpleBlock("black_hole_inner", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion().lightLevel(item -> 5));
    @NoBlockItem
    public static final DeferredBlock<Block> BLACK_HOLE_MIDDLE = BLOCKS.registerSimpleBlock("black_hole_middle", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion().lightLevel(item -> 5));
    @NoBlockItem
    public static final DeferredBlock<Block> BLACK_HOLE_OUTER = BLOCKS.registerSimpleBlock("black_hole_outer", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion().lightLevel(item -> 5));
    
    @NoBlockItem
    public static final DeferredBlock<Block> ADDON_INDICATOR_BLOCK = BLOCKS.registerSimpleBlock("addon_indicator_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_COLD_INDICATOR_BLOCK = BLOCKS.registerSimpleBlock("reactor_cold_indicator_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_MEDIUM_INDICATOR_BLOCK = BLOCKS.registerSimpleBlock("reactor_medium_indicator_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_HOT_INDICATOR_BLOCK = BLOCKS.registerSimpleBlock("reactor_hot_indicator_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> PULVERIZER_BLOCK = BLOCKS.registerBlock("pulverizer_block", PulverizerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> FRAGMENT_FORGE_BLOCK = BLOCKS.registerBlock("fragment_forge_block", FragmentForge::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> ASSEMBLER_BLOCK = BLOCKS.registerBlock("assembler_block", AssemblerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> FOUNDRY_BLOCK = BLOCKS.registerBlock("foundry_block", FoundryBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> COOLER_BLOCK = BLOCKS.registerBlock("cooler_block", CoolerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> CENTRIFUGE_BLOCK = BLOCKS.registerBlock("centrifuge_block", CentrifugeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    @ItemRarity(Rarity.RARE)
    public static final DeferredBlock<Block> ATOMIC_FORGE_BLOCK = BLOCKS.registerBlock("atomic_forge_block", AtomicForgeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    public static final DeferredBlock<Block> REFINERY_BLOCK = BLOCKS.registerBlock("refinery_block", RefineryBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    @NoAutoDrop
    public static final DeferredBlock<Block> TAINTED_REFINERY_BLOCK = BLOCKS.registerBlock("tainted_refinery_block", TaintedRefineryBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(7f, 2000f).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    public static final DeferredBlock<Block> REFINERY_MODULE_BLOCK = BLOCKS.registerBlock("refinery_module_block", RefineryModuleBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> BIO_GENERATOR_BLOCK = BLOCKS.registerBlock("bio_generator_block", BioGeneratorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> LAVA_GENERATOR_BLOCK = BLOCKS.registerBlock("lava_generator_block", LavaGeneratorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    public static final DeferredBlock<Block> FUEL_GENERATOR_BLOCK = BLOCKS.registerBlock("fuel_generator_block", FuelGeneratorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> BASIC_GENERATOR_BLOCK = BLOCKS.registerBlock("basic_generator_block", BasicGeneratorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> STEAM_ENGINE_BLOCK = BLOCKS.registerBlock("steam_engine_block", SteamEngineBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> BIG_SOLAR_PANEL_BLOCK = BLOCKS.registerBlock("big_solar_panel_block", BigSolarPanelBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> POWERED_FURNACE_BLOCK = BLOCKS.registerBlock("powered_furnace_block", PoweredFurnaceBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0));
    @UseGeoBlockItem(scale = 0.5f)
    public static final DeferredBlock<Block> LASER_ARM_BLOCK = BLOCKS.registerBlock("laser_arm_block", LaserArmBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.25f)
    public static final DeferredBlock<Block> DEEP_DRILL_BLOCK = BLOCKS.registerBlock("deep_drill_block", DeepDrillBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    public static final DeferredBlock<Block> DRONE_PORT_BLOCK = BLOCKS.registerBlock("drone_port_block", DronePortBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> SHRINKER_BLOCK = BLOCKS.registerBlock("shrinker_block", ShrinkerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    @NoAutoDrop
    @DispenserPlace
    public static final DeferredBlock<Block> SMALL_STORAGE_BLOCK = BLOCKS.registerBlock("small_storage_block", SmallStorageBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> LARGE_STORAGE_BLOCK = BLOCKS.registerBlock("large_storage_block", LargeStorageBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @DispenserPlace
    public static final DeferredBlock<Block> CREATIVE_STORAGE_BLOCK = BLOCKS.registerBlock("creative_storage_block", CreativeStorageBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK).destroyTime(-1.0F));
    
    @NoAutoDrop
    @DispenserPlace
    @NoBlockItem
    public static final DeferredBlock<Block> SMALL_TANK_BLOCK = BLOCKS.registerBlock("small_tank_block", SmallFluidTank::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0));
    
    @NoAutoDrop
    @DispenserPlace
    @NoBlockItem
    public static final DeferredBlock<Block> CREATIVE_TANK_BLOCK = BLOCKS.registerBlock("creative_tank_block", CreativeFluidTank::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0).destroyTime(-1.0F));
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> AUGMENT_APPLICATION_BLOCK = BLOCKS.registerBlock("augment_application_block", AugmentApplicationBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> SIMPLE_AUGMENT_STATION = BLOCKS.registerBlock("simple_augment_station", AugmentResearchStationBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2));
    public static final DeferredBlock<Block> ADVANCED_AUGMENT_STATION = BLOCKS.registerBlock("advanced_augment_station", AugmentResearchStationBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2));
    public static final DeferredBlock<Block> ARCANE_AUGMENT_STATION = BLOCKS.registerBlock("arcane_augment_station", AugmentResearchStationBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2));
    
    public static final DeferredBlock<Block> PLACER_BLOCK = BLOCKS.registerBlock("placer_block", PlacerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> DESTROYER_BLOCK = BLOCKS.registerBlock("destroyer_block", DestroyerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> FERTILIZER_BLOCK = BLOCKS.registerBlock("fertilizer_block", FertilizerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> TREEFELLER_BLOCK = BLOCKS.registerBlock("treefeller_block", TreefellerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> PIPE_BOOSTER_BLOCK = BLOCKS.registerBlock("pipe_booster_block", PipeBoosterBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    @UseGeoBlockItem(scale = 0.7f)
    @ItemRarity(Rarity.RARE)
    public static final DeferredBlock<Block> ENCHANTMENT_CATALYST_BLOCK = BLOCKS.registerBlock("enchantment_catalyst_block", EnchantmentCatalystBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    @ItemRarity(Rarity.RARE)
    public static final DeferredBlock<Block> ENCHANTER_BLOCK = BLOCKS.registerBlock("enchanter_block", EnchanterBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @ItemRarity(Rarity.RARE)
    public static final DeferredBlock<Block> SPAWNER_CONTROLLER_BLOCK = BLOCKS.registerBlock("spawner_controller_block", SpawnerControllerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @NoAutoDrop
    public static final DeferredBlock<Block> WITHER_CROP_BLOCK = BLOCKS.registerBlock("wither_crop_block", WitheredCropBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    
    @NoBlockItem
    public static final DeferredBlock<Block> UNSTABLE_CONTAINER = BLOCKS.registerBlock("unstable_container", UnstableContainerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(80, 1900f).noOcclusion().forceSolidOn());
    
    public static final DeferredBlock<Block> ACCELERATOR_RING = BLOCKS.registerBlock("accelerator_ring", AcceleratorRingBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> ACCELERATOR_MOTOR = BLOCKS.registerBlock("accelerator_motor", AcceleratorMotorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 5));
    public static final DeferredBlock<Block> ACCELERATOR_CONTROLLER = BLOCKS.registerBlock("accelerator_controller", AcceleratorControllerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> ACCELERATOR_SENSOR = BLOCKS.registerBlock("accelerator_sensor", AcceleratorSensorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @ItemRarity(Rarity.EPIC)
    public static final DeferredBlock<Block> BLACK_HOLE_BLOCK = BLOCKS.registerBlock("black_hole_block", BlackHoleBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_PORTAL).lightLevel(item -> 12).noOcclusion().forceSolidOn());
    
    public static final DeferredBlock<Block> PARTICLE_COLLECTOR_BLOCK = BLOCKS.registerBlock("particle_collector_block", ParticleCollectorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion());
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> PUMP_BLOCK = BLOCKS.registerBlock("pump_block", PumpBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> CHARGER_BLOCK = BLOCKS.registerBlock("charger_block", ChargerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    public static final DeferredBlock<Block> MACHINE_CORE_1 = BLOCKS.registerBlock("machine_core_1", props -> new MachineCoreBlock(props, 1), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_2 = BLOCKS.registerBlock("machine_core_2", props -> new MachineCoreBlock(props, 2), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_3 = BLOCKS.registerBlock("machine_core_3", props -> new MachineCoreBlock(props, 3), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_4 = BLOCKS.registerBlock("machine_core_4", props -> new MachineCoreBlock(props, 4), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_5 = BLOCKS.registerBlock("machine_core_5", props -> new MachineCoreBlock(props, 5), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_6 = BLOCKS.registerBlock("machine_core_6", props -> new MachineCoreBlock(props, 6), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_7 = BLOCKS.registerBlock("machine_core_7", props -> new MachineCoreBlock(props, 7), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> MACHINE_CORE_HIDDEN = BLOCKS.registerBlock("machine_core_hidden", props -> new MachineCoreBlock(props, 1), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(80, 1900f).noOcclusion().forceSolidOn());
    
    public static final DeferredBlock<Block> MACHINE_SPEED_ADDON = BLOCKS.registerBlock("machine_speed_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withSpeedMultiplier(OritechStartupConfig.speedAddonSpeed.get().floatValue()).withEfficiencyMultiplier(OritechStartupConfig.speedAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.MACHINE_SPEED_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_EFFICIENCY_ADDON = BLOCKS.registerBlock("machine_efficiency_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withEfficiencyMultiplier(OritechStartupConfig.efficiencyAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.MACHINE_EFFICIENCY_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_ULTIMATE_ADDON = BLOCKS.registerBlock("machine_ultimate_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withSpeedMultiplier(OritechStartupConfig.ultimateAddonSpeed.get().floatValue()).withEfficiencyMultiplier(OritechStartupConfig.ultimateAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.MACHINE_ULTIMATE_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> QUARRY_ADDON = BLOCKS.registerBlock("quarry_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.QUARRY_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_PROCESSING_ADDON = BLOCKS.registerBlock("machine_processing_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withEfficiencyMultiplier(OritechStartupConfig.chamberAddonEfficiency.get().floatValue()).withChambers(1).withBoundingShape(MachineAddonBlock.MACHINE_PROCESSING_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_FLUID_ADDON = BLOCKS.registerBlock("machine_fluid_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_FLUID_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_YIELD_ADDON = BLOCKS.registerBlock("machine_yield_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_YIELD_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> CROP_FILTER_ADDON = BLOCKS.registerBlock("crop_filter_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.CROP_FILTER_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_HUNTER_ADDON = BLOCKS.registerBlock("machine_hunter_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_HUNTER_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CAPACITOR_ADDON = BLOCKS.registerBlock("machine_capacitor_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withAddedCapacity(2_000_000).withAddedInsert(2_000).withBoundingShape(MachineAddonBlock.MACHINE_CAPACITOR_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_ACCEPTOR_ADDON = BLOCKS.registerBlock("machine_acceptor_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withAddedCapacity(500_000).withAddedInsert(5_000).withAcceptEnergy(true).withBoundingShape(MachineAddonBlock.MACHINE_ACCEPTOR_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_INVENTORY_PROXY_ADDON = BLOCKS.registerBlock("machine_inventory_proxy_addon", props -> new InventoryProxyAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_INVENTORY_PROXY_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_EXTENDER = BLOCKS.registerBlock("machine_extender", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> CAPACITOR_ADDON_EXTENDER = BLOCKS.registerBlock("capacitor_addon_extender", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false).withAddedCapacity(2_500_000).withAddedInsert(1_000)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> STEAM_BOILER_ADDON = BLOCKS.registerBlock("steam_boiler_addon", props -> new SteamBoilerAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.STEAM_BOILER_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_REDSTONE_ADDON = BLOCKS.registerBlock("machine_redstone_addon", props -> new RedstoneAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_REDSTONE_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_SILK_TOUCH_ADDON = BLOCKS.registerBlock("machine_silk_touch_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_SILK_TOUCH_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_BURST_ADDON = BLOCKS.registerBlock("machine_burst_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBurstTicks(OritechStartupConfig.burstAddonTicks.get()).withBoundingShape(MachineAddonBlock.MACHINE_BURST_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()); // todo config settings
    
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.NONE)
    @ItemRarity(Rarity.EPIC)
    public static final DeferredBlock<Block> MACHINE_COMBI_ADDON = BLOCKS.registerBlock("machine_combi_addon", props -> new CombiAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_COMBI_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    //region reactor
    @ItemRarity(Rarity.UNCOMMON)
    public static final DeferredBlock<Block> REACTOR_CONTROLLER = BLOCKS.registerBlock("reactor_controller", ReactorControllerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 5));
    public static final DeferredBlock<Block> REACTOR_WALL = BLOCKS.registerBlock("reactor_wall", ReactorWallBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(10, 1800));
    public static final DeferredBlock<Block> REACTOR_ROD = BLOCKS.registerBlock("reactor_rod", props -> new ReactorRodBlock(props, 1, 1), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3));
    public static final DeferredBlock<Block> REACTOR_DOUBLE_ROD = BLOCKS.registerBlock("reactor_double_rod", props -> new ReactorRodBlock(props, 2, 4), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3));
    public static final DeferredBlock<Block> REACTOR_QUAD_ROD = BLOCKS.registerBlock("reactor_quad_rod", props -> new ReactorRodBlock(props, 4, 12), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3));
    public static final DeferredBlock<Block> REACTOR_VENT = BLOCKS.registerBlock("reactor_vent", ReactorHeatVentBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> REACTOR_REFLECTOR = BLOCKS.registerBlock("reactor_reflector", ReactorReflectorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> 15));
    public static final DeferredBlock<Block> REACTOR_HEAT_PIPE = BLOCKS.registerBlock("reactor_heat_pipe", ReactorHeatPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> REACTOR_CONDENSER = BLOCKS.registerBlock("reactor_condenser", ReactorAbsorberBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> REACTOR_FUEL_PORT = BLOCKS.registerBlock("reactor_fuel_port", ReactorFuelPortBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> REACTOR_ABSORBER_PORT = BLOCKS.registerBlock("reactor_absorber_port", ReactorAbsorberPortBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> REACTOR_ENERGY_PORT = BLOCKS.registerBlock("reactor_energy_port", ReactorEnergyPortBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> REACTOR_REDSTONE_PORT = BLOCKS.registerBlock("reactor_redstone_port", ReactorRedstonePortBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> URANIUM_CRYSTAL = BLOCKS.registerBlock("uranium_crystal", props -> new AmethystClusterBlock(7, 3, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel(state -> 5));
    
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_EXPLOSION_SMALL = BLOCKS.registerBlock("reactor_explosion_small", props -> new NuclearExplosionBlock(props, 9), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_EXPLOSION_MEDIUM = BLOCKS.registerBlock("reactor_explosion_medium", props -> new NuclearExplosionBlock(props, 14), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_EXPLOSION_LARGE = BLOCKS.registerBlock("reactor_explosion_large", props -> new NuclearExplosionBlock(props, 20), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> LOW_YIELD_NUKE = BLOCKS.registerBlock("low_yield_nuke", props -> new NukeBlock(props, true), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> NUKE = BLOCKS.registerBlock("nuke", props -> new NukeBlock(props, false), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    
    // cooling cell, early game re-fillable component
    
    // lategame, second stage components:
    // plasma conduit, advanced heat transfer system
    // entropy dampener, reduce degradation rate of nearby components
    // quantum stabilizer, massively increase heat capacity of reactor
    //endregion
    
    //region metals
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> NICKEL_ORE = BLOCKS.registerSimpleBlock("nickel_ore", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> DEEPSLATE_NICKEL_ORE = BLOCKS.registerSimpleBlock("deepslate_nickel_ore", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> ENDSTONE_PLATINUM_ORE = BLOCKS.registerSimpleBlock("endstone_platinum_ore", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> DEEPSLATE_PLATINUM_ORE = BLOCKS.registerSimpleBlock("deepslate_platinum_ore", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> DEEPSLATE_URANIUM_ORE = BLOCKS.registerSimpleBlock("deepslate_uranium_ore", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE));
    //endregion
    
    //region resource nodes
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_REDSTONE = BLOCKS.registerSimpleBlock("resource_node_redstone", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_LAPIS = BLOCKS.registerSimpleBlock("resource_node_lapis", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_IRON = BLOCKS.registerSimpleBlock("resource_node_iron", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_COAL = BLOCKS.registerSimpleBlock("resource_node_coal", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_GOLD = BLOCKS.registerSimpleBlock("resource_node_gold", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_EMERALD = BLOCKS.registerSimpleBlock("resource_node_emerald", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_DIAMOND = BLOCKS.registerSimpleBlock("resource_node_diamond", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_COPPER = BLOCKS.registerSimpleBlock("resource_node_copper", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_NICKEL = BLOCKS.registerSimpleBlock("resource_node_nickel", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_PLATINUM = BLOCKS.registerSimpleBlock("resource_node_platinum", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_URANIUM = BLOCKS.registerSimpleBlock("resource_node_uranium", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    
    // region decorative
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> CEILING_LIGHT = BLOCKS.registerBlock("ceiling_light", props -> new WallMountedLight(props, 2), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE).noOcclusion());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> CEILING_LIGHT_HANGING = BLOCKS.registerBlock("ceiling_light_hanging", props -> new WallMountedLight(props, 12), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE).noOcclusion());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> TECH_BUTTON = BLOCKS.registerBlock("tech_button", props -> new TechRedstoneButton(BlockSetType.IRON, 80, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> TECH_LEVER = BLOCKS.registerBlock("tech_lever", TechLever::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> MACHINE_PLATING_BLOCK = BLOCKS.registerSimpleBlock("machine_plating_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @NoAutoDrop
    public static final DeferredBlock<Block> MACHINE_PLATING_SLAB = BLOCKS.registerBlock("machine_plating_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> MACHINE_PLATING_STAIRS = BLOCKS.registerBlock("machine_plating_stairs", props -> new StairBlock(MACHINE_PLATING_BLOCK.value().defaultBlockState(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> MACHINE_PLATING_PRESSURE_PLATE = BLOCKS.registerBlock("machine_plating_pressure_plate", props -> new PressurePlateBlock(BlockSetType.IRON, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> IRON_PLATING_BLOCK = BLOCKS.registerSimpleBlock("iron_plating_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @NoAutoDrop
    public static final DeferredBlock<Block> IRON_PLATING_SLAB = BLOCKS.registerBlock("iron_plating_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> IRON_PLATING_STAIRS = BLOCKS.registerBlock("iron_plating_stairs", props -> new StairBlock(IRON_PLATING_BLOCK.value().defaultBlockState(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> IRON_PLATING_PRESSURE_PLATE = BLOCKS.registerBlock("iron_plating_pressure_plate", props -> new PressurePlateBlock(BlockSetType.IRON, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> CARBON_PLATING_BLOCK = BLOCKS.registerSimpleBlock("carbon_plating_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @NoAutoDrop
    public static final DeferredBlock<Block> CARBON_PLATING_SLAB = BLOCKS.registerBlock("carbon_plating_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> CARBON_PLATING_STAIRS = BLOCKS.registerBlock("carbon_plating_stairs", props -> new StairBlock(CARBON_PLATING_BLOCK.value().defaultBlockState(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> CARBON_PLATING_PRESSURE_PLATE = BLOCKS.registerBlock("carbon_plating_pressure_plate", props -> new PressurePlateBlock(BlockSetType.IRON, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> NICKEL_PLATING_BLOCK = BLOCKS.registerSimpleBlock("nickel_plating_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @NoAutoDrop
    public static final DeferredBlock<Block> NICKEL_PLATING_SLAB = BLOCKS.registerBlock("nickel_plating_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> NICKEL_PLATING_STAIRS = BLOCKS.registerBlock("nickel_plating_stairs", props -> new StairBlock(NICKEL_PLATING_BLOCK.value().defaultBlockState(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> NICKEL_PLATING_PRESSURE_PLATE = BLOCKS.registerBlock("nickel_plating_pressure_plate", props -> new PressurePlateBlock(BlockSetType.IRON, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> METAL_BEAM_BLOCK = BLOCKS.registerBlock("metal_beam_block", MetalBeamBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().forceSolidOn());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE) // todo recipe
    public static final DeferredBlock<Block> METAL_GIRDER_BLOCK = BLOCKS.registerBlock("metal_girder_block", MetalGirderBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().forceSolidOn());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> INDUSTRIAL_GLASS_BLOCK = BLOCKS.registerSimpleBlock("industrial_glass_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).requiresCorrectToolForDrops().strength(7.0F, 8.0F).noOcclusion());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @UseGeoBlockItem(scale = 0.5f)
    public static final DeferredBlock<Block> TECH_DOOR = BLOCKS.registerBlock("tech_door", TechDoorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(8f).forceSolidOn());
    @NoBlockItem
    @NoAutoDrop
    public static final DeferredBlock<Block> TECH_DOOR_HINGE = BLOCKS.registerBlock("tech_door_hinge", TechDoorBlockHinge::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(8f).forceSolidOn());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @UseGeoBlockItem(scale = 0.37f)
    public static final DeferredBlock<Block> HANGAR_DOOR = BLOCKS.registerBlock("hangar_door", HangarDoorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(8f).forceSolidOn());
    @NoBlockItem
    @NoAutoDrop
    public static final DeferredBlock<Block> HANGAR_DOOR_HELPER = BLOCKS.registerBlock("hangar_door_helper", HangarDoorHelperBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(8f).forceSolidOn());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> STEEL_BLOCK = BLOCKS.registerSimpleBlock("steel_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> ENERGITE_BLOCK = BLOCKS.registerSimpleBlock("energite_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 6));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> NICKEL_BLOCK = BLOCKS.registerSimpleBlock("nickel_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> BIOSTEEL_BLOCK = BLOCKS.registerSimpleBlock("biosteel_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> PLATINUM_BLOCK = BLOCKS.registerSimpleBlock("platinum_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> ADAMANT_BLOCK = BLOCKS.registerSimpleBlock("adamant_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> ELECTRUM_BLOCK = BLOCKS.registerSimpleBlock("electrum_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> DURATIUM_BLOCK = BLOCKS.registerSimpleBlock("duratium_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @Compostable(1.0f)
    public static final DeferredBlock<Block> BIOMASS_BLOCK = BLOCKS.registerSimpleBlock("biomass_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.MOSS));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> PLASTIC_BLOCK = BLOCKS.registerSimpleBlock("plastic_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.SHROOMLIGHT));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> FLUXITE_BLOCK = BLOCKS.registerSimpleBlock("fluxite_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> SILICON_BLOCK = BLOCKS.registerBlock("silicon_block", SlimeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SLIME_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RAW_NICKEL_BLOCK = BLOCKS.registerSimpleBlock("raw_nickel_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RAW_PLATINUM_BLOCK = BLOCKS.registerSimpleBlock("raw_platinum_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RAW_URANIUM_BLOCK = BLOCKS.registerSimpleBlock("raw_uranium_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> URANIUM_DUST_BLOCK = BLOCKS.registerSimpleBlock("uranium_dust_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 2));
    //endregion
    
    @SuppressWarnings("unchecked")
    public static void AddBlockItems() {
        
        for (var field : BlockContent.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!Modifier.isPublic(field.getModifiers())) continue;
            if (!DeferredBlock.class.isAssignableFrom(field.getType())) continue;
            
            try {
                field.setAccessible(true);
                var value = (DeferredBlock<Block>) field.get(null);
                var identifier = field.getName().toLowerCase(java.util.Locale.ROOT);
                
                if (field.isAnnotationPresent(BlockContent.NoBlockItem.class)) continue;
                
                var fieldGroup = ItemContent.Groups.MACHINES;
                
                if (field.isAnnotationPresent(ItemContent.ItemGroupTarget.class)) {
                    fieldGroup = field.getAnnotation(ItemContent.ItemGroupTarget.class).value();
                }
                
                var blockItem = BLOCK_ITEMS.registerSimpleBlockItem(value);
                BLOCK_GROUPS.add(new Pair<>(blockItem, fieldGroup));
                
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + field.getName(), e);
            }
        }
        
    }
    
    // todo figure this out
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface UseGeoBlockItem {
        float scale(); // scale
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface ItemRarity {
        Rarity value();
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface NoAutoDrop {
    }
    
    // todo
    //  DispenserBlock.registerBehavior(value, new ShulkerBoxDispenseBehavior());
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface DispenserPlace {
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface NoBlockItem {
    }
    
}

