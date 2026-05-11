package rearth.oritech.init;

import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
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
import rearth.oritech.item.OritechGeoItem;
import rearth.oritech.item.other.SmallEnergyStorageBlockItem;
import rearth.oritech.item.other.SmallFluidTankBlockItem;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("NullableProblems")
public class BlockContent {
    
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Oritech.MOD_ID);
    
    public static final DeferredBlock<Block> SPAWNER_CAGE_BLOCK = BLOCKS.registerBlock("spawner_cage_block", SpawnerCageBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()); // sample 1
    
    public static final DeferredBlock<Block> MACHINE_FRAME_BLOCK = BLOCKS.register("machine_frame_block", new MachineFrameBlock(blockProperties("machine_frame_block", Blocks.IRON_BARS)));
    
    public static final DeferredBlock<Block> FLUID_PIPE = BLOCKS.register("fluid_pipe", new FluidPipeBlock(blockProperties("fluid_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final DeferredBlock<Block> FRAMED_FLUID_PIPE = BLOCKS.register("framed_fluid_pipe", new FluidPipeBlock.FramedFluidPipeBlock(blockProperties("framed_fluid_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final DeferredBlock<Block> FLUID_PIPE_DUCT_BLOCK = BLOCKS.register("fluid_pipe_duct_block", new FluidPipeDuctBlock(blockProperties("fluid_pipe_duct_block", Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> ENERGY_PIPE = BLOCKS.register("energy_pipe", new EnergyPipeBlock(blockProperties("energy_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final DeferredBlock<Block> FRAMED_ENERGY_PIPE = BLOCKS.register("framed_energy_pipe", new EnergyPipeBlock.FramedEnergyPipeBlock(blockProperties("framed_energy_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final DeferredBlock<Block> ENERGY_PIPE_DUCT_BLOCK = BLOCKS.register("energy_pipe_duct_block", new EnergyPipeDuctBlock(blockProperties("energy_pipe_duct_block", Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> SUPERCONDUCTOR = BLOCKS.register("superconductor", new SuperConductorBlock(blockProperties("superconductor", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final DeferredBlock<Block> FRAMED_SUPERCONDUCTOR = BLOCKS.register("framed_superconductor", new SuperConductorBlock.FramedSuperConductorBlock(blockProperties("framed_superconductor", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final DeferredBlock<Block> SUPERCONDUCTOR_DUCT_BLOCK = BLOCKS.register("superconductor_duct_block", new SuperConductorDuctBlock(blockProperties("superconductor_duct_block", Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> ITEM_PIPE = BLOCKS.register("item_pipe", new ItemPipeBlock(blockProperties("item_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final DeferredBlock<Block> TRANSPARENT_ITEM_PIPE = BLOCKS.register("transparent_item_pipe", new ItemPipeBlock.TransparentItemPipe(blockProperties("transparent_item_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final DeferredBlock<Block> FRAMED_ITEM_PIPE = BLOCKS.register("framed_item_pipe", new ItemPipeBlock.FramedItemPipeBlock(blockProperties("framed_item_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final DeferredBlock<Block> ITEM_PIPE_DUCT_BLOCK = BLOCKS.register("item_pipe_duct_block", new ItemPipeDuctBlock(blockProperties("item_pipe_duct_block", Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> ITEM_FILTER_BLOCK = BLOCKS.register("item_filter_block", new ItemFilterBlock(blockProperties("item_filter_block", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    
    @NoBlockItem
    public static final DeferredBlock<Block> FLUID_PIPE_CONNECTION = BLOCKS.register("fluid_pipe_connection", new FluidPipeConnectionBlock(blockProperties("fluid_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final DeferredBlock<Block> FRAMED_FLUID_PIPE_CONNECTION = BLOCKS.register("framed_fluid_pipe_connection", new FluidPipeConnectionBlock.FramedFluidPipeConnectionBlock(blockProperties("framed_fluid_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final DeferredBlock<Block> ENERGY_PIPE_CONNECTION = BLOCKS.register("energy_pipe_connection", new EnergyPipeConnectionBlock(blockProperties("energy_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final DeferredBlock<Block> FRAMED_ENERGY_PIPE_CONNECTION = BLOCKS.register("framed_energy_pipe_connection", new EnergyPipeConnectionBlock.FramedEnergyPipeConnectionBlock(blockProperties("framed_energy_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final DeferredBlock<Block> SUPERCONDUCTOR_CONNECTION = BLOCKS.register("superconductor_connection", new SuperConductorConnectionBlock(blockProperties("superconductor_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final DeferredBlock<Block> FRAMED_SUPERCONDUCTOR_CONNECTION = BLOCKS.register("framed_superconductor_connection", new SuperConductorConnectionBlock.FramedSuperConductorConnectionBlock(blockProperties("framed_superconductor_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final DeferredBlock<Block> ITEM_PIPE_CONNECTION = BLOCKS.register("item_pipe_connection", new ItemPipeConnectionBlock(blockProperties("item_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final DeferredBlock<Block> FRAMED_ITEM_PIPE_CONNECTION = BLOCKS.register("framed_item_pipe_connection", new ItemPipeConnectionBlock.FramedItemPipeConnectionBlock(blockProperties("framed_item_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final DeferredBlock<Block> TRANSPARENT_ITEM_PIPE_CONNECTION = BLOCKS.register("transparent_item_pipe_connection", new ItemPipeConnectionBlock.TransparentItemPipeConnectionBlock(blockProperties("transparent_item_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    
    public static final DeferredBlock<Block> POWER_POLE_BLOCK = BLOCKS.register("power_pole_block", new PowerPoleBlock(blockProperties("power_pole_block", Blocks.IRON_BLOCK).noOcclusion()));
    
    @NoBlockItem
    public static final DeferredBlock<Block> FRAME_GANTRY_ARM = BLOCKS.register("frame_gantry_arm", new Block(blockProperties("frame_gantry_arm", Blocks.CHAIN).noOcclusion()));
    @NoBlockItem
    public static final DeferredBlock<Block> BLOCK_DESTROYER_HEAD = BLOCKS.register("block_destroyer_head", new Block(blockProperties("block_destroyer_head", Blocks.CHAIN).noOcclusion()));
    @NoBlockItem
    public static final DeferredBlock<Block> BLOCK_PLACER_HEAD = BLOCKS.register("block_placer_head", new Block(blockProperties("block_placer_head", Blocks.CHAIN).noOcclusion()));
    @NoBlockItem
    public static final DeferredBlock<Block> BLOCK_FERTILIZER_HEAD = BLOCKS.register("block_fertilizer_head", new Block(blockProperties("block_fertilizer_head", Blocks.CHAIN).noOcclusion()));
    @NoBlockItem
    public static final DeferredBlock<Block> PUMP_TRUNK_BLOCK = BLOCKS.register("pump_trunk_block", new Block(blockProperties("pump_trunk_block", Blocks.CHAIN).noOcclusion()));
    @NoBlockItem
    public static final DeferredBlock<Block> TANK_ITEM_MODEL = BLOCKS.register("tank_item_model", new Block(blockProperties("tank_item_model", Blocks.CHAIN).noOcclusion()));   // workaround because I don't understand how to properly get the model to load
    @NoBlockItem
    public static final DeferredBlock<Block> CREATIVE_TANK_ITEM_MODEL = BLOCKS.register("creative_tank_item_model", new Block(blockProperties("creative_tank_item_model", Blocks.CHAIN).noOcclusion()));   // workaround because I don't understand how to properly get the model to load
    @NoBlockItem
    public static final DeferredBlock<Block> QUARRY_BEAM_RING = BLOCKS.register("quarry_beam_ring", new Block(blockProperties("quarry_beam_ring", Blocks.CHAIN).noOcclusion().lightLevel(item -> 5)));
    @NoBlockItem
    public static final DeferredBlock<Block> BLACK_HOLE_INNER = BLOCKS.register("black_hole_inner", new Block(blockProperties("black_hole_inner", Blocks.CHAIN).noOcclusion().lightLevel(item -> 5)));
    @NoBlockItem
    public static final DeferredBlock<Block> BLACK_HOLE_MIDDLE = BLOCKS.register("black_hole_middle", new Block(blockProperties("black_hole_middle", Blocks.CHAIN).noOcclusion().lightLevel(item -> 5)));
    @NoBlockItem
    public static final DeferredBlock<Block> BLACK_HOLE_OUTER = BLOCKS.register("black_hole_outer", new Block(blockProperties("black_hole_outer", Blocks.CHAIN).noOcclusion().lightLevel(item -> 5)));
    
    @NoBlockItem
    public static final DeferredBlock<Block> ADDON_INDICATOR_BLOCK = BLOCKS.register("addon_indicator_block", new Block(blockProperties("addon_indicator_block", Blocks.GLASS)));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_COLD_INDICATOR_BLOCK = BLOCKS.register("reactor_cold_indicator_block", new Block(blockProperties("reactor_cold_indicator_block", Blocks.GLASS)));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_MEDIUM_INDICATOR_BLOCK = BLOCKS.register("reactor_medium_indicator_block", new Block(blockProperties("reactor_medium_indicator_block", Blocks.GLASS)));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_HOT_INDICATOR_BLOCK = BLOCKS.register("reactor_hot_indicator_block", new Block(blockProperties("reactor_hot_indicator_block", Blocks.GLASS)));
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> PULVERIZER_BLOCK = BLOCKS.register("pulverizer_block", new PulverizerBlock(blockProperties("pulverizer_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> FRAGMENT_FORGE_BLOCK = BLOCKS.register("fragment_forge_block", new FragmentForge(blockProperties("fragment_forge_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> ASSEMBLER_BLOCK = BLOCKS.register("assembler_block", new AssemblerBlock(blockProperties("assembler_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> FOUNDRY_BLOCK = BLOCKS.register("foundry_block", new FoundryBlock(blockProperties("foundry_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> COOLER_BLOCK = BLOCKS.register("cooler_block", new CoolerBlock(blockProperties("cooler_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> CENTRIFUGE_BLOCK = BLOCKS.register("centrifuge_block", new CentrifugeBlock(blockProperties("centrifuge_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    @ItemRarity(ItemRarityValue.RARE)
    public static final DeferredBlock<Block> ATOMIC_FORGE_BLOCK = BLOCKS.register("atomic_forge_block", new AtomicForgeBlock(blockProperties("atomic_forge_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    public static final DeferredBlock<Block> REFINERY_BLOCK = BLOCKS.register("refinery_block", new RefineryBlock(blockProperties("refinery_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    @NoAutoDrop
    public static final DeferredBlock<Block> TAINTED_REFINERY_BLOCK = BLOCKS.register("tainted_refinery_block", new TaintedRefineryBlock(blockProperties("tainted_refinery_block", Blocks.IRON_BLOCK).strength(7f, 2000f).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    public static final DeferredBlock<Block> REFINERY_MODULE_BLOCK = BLOCKS.register("refinery_module_block", new RefineryModuleBlock(blockProperties("refinery_module_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> BIO_GENERATOR_BLOCK = BLOCKS.register("bio_generator_block", new BioGeneratorBlock(blockProperties("bio_generator_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> LAVA_GENERATOR_BLOCK = BLOCKS.register("lava_generator_block", new LavaGeneratorBlock(blockProperties("lava_generator_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    public static final DeferredBlock<Block> FUEL_GENERATOR_BLOCK = BLOCKS.register("fuel_generator_block", new FuelGeneratorBlock(blockProperties("fuel_generator_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> BASIC_GENERATOR_BLOCK = BLOCKS.register("basic_generator_block", new BasicGeneratorBlock(blockProperties("basic_generator_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> STEAM_ENGINE_BLOCK = BLOCKS.register("steam_engine_block", new SteamEngineBlock(blockProperties("steam_engine_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> BIG_SOLAR_PANEL_BLOCK = BLOCKS.register("big_solar_panel_block", new BigSolarPanelBlock(blockProperties("big_solar_panel_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> POWERED_FURNACE_BLOCK = BLOCKS.register("powered_furnace_block", new PoweredFurnaceBlock(blockProperties("powered_furnace_block", Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0)));
    @UseGeoBlockItem(scale = 0.5f)
    public static final DeferredBlock<Block> LASER_ARM_BLOCK = BLOCKS.register("laser_arm_block", new LaserArmBlock(blockProperties("laser_arm_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.25f)
    public static final DeferredBlock<Block> DEEP_DRILL_BLOCK = BLOCKS.register("deep_drill_block", new DeepDrillBlock(blockProperties("deep_drill_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    public static final DeferredBlock<Block> DRONE_PORT_BLOCK = BLOCKS.register("drone_port_block", new DronePortBlock(blockProperties("drone_port_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> SHRINKER_BLOCK = BLOCKS.register("shrinker_block", new ShrinkerBlock(blockProperties("shrinker_block", Blocks.IRON_BLOCK).noOcclusion()));
    
    @NoAutoDrop
    @DispenserPlace
    public static final DeferredBlock<Block> SMALL_STORAGE_BLOCK = BLOCKS.register("small_storage_block", new SmallStorageBlock(blockProperties("small_storage_block", Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> LARGE_STORAGE_BLOCK = BLOCKS.register("large_storage_block", new LargeStorageBlock(blockProperties("large_storage_block", Blocks.IRON_BLOCK).noOcclusion()));
    @DispenserPlace
    public static final DeferredBlock<Block> CREATIVE_STORAGE_BLOCK = BLOCKS.register("creative_storage_block", new CreativeStorageBlock(blockProperties("creative_storage_block", Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK).destroyTime(-1.0F)));
    
    @NoAutoDrop
    @DispenserPlace
    public static final DeferredBlock<Block> SMALL_TANK_BLOCK = BLOCKS.register("small_tank_block", new SmallFluidTank(blockProperties("small_tank_block", Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0)));
    
    @NoAutoDrop
    @DispenserPlace
    public static final DeferredBlock<Block> CREATIVE_TANK_BLOCK = BLOCKS.register("creative_tank_block", new CreativeFluidTank(blockProperties("creative_tank_block", Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0).destroyTime(-1.0F)));
    
    public static final DeferredBlock<Item> SMALL_TANK_ITEM = ItemContent.registerItem("small_tank_block", new SmallFluidTankBlockItem(blockValue(SMALL_TANK_BLOCK), blockItemProperties("small_tank_block")));
    public static final DeferredBlock<Item> CREATIVE_TANK_ITEM = ItemContent.registerItem("creative_tank_block", new SmallFluidTankBlockItem(blockValue(CREATIVE_TANK_BLOCK), blockItemProperties("creative_tank_block")));
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> AUGMENT_APPLICATION_BLOCK = BLOCKS.register("augment_application_block", new AugmentApplicationBlock(blockProperties("augment_application_block", Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> SIMPLE_AUGMENT_STATION = BLOCKS.register("simple_augment_station", new AugmentResearchStationBlock(blockProperties("simple_augment_station", Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2)));
    public static final DeferredBlock<Block> ADVANCED_AUGMENT_STATION = BLOCKS.register("advanced_augment_station", new AugmentResearchStationBlock(blockProperties("advanced_augment_station", Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2)));
    public static final DeferredBlock<Block> ARCANE_AUGMENT_STATION = BLOCKS.register("arcane_augment_station", new AugmentResearchStationBlock(blockProperties("arcane_augment_station", Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2)));
    
    public static final DeferredBlock<Block> PLACER_BLOCK = BLOCKS.register("placer_block", new PlacerBlock(blockProperties("placer_block", Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> DESTROYER_BLOCK = BLOCKS.register("destroyer_block", new DestroyerBlock(blockProperties("destroyer_block", Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> FERTILIZER_BLOCK = BLOCKS.register("fertilizer_block", new FertilizerBlock(blockProperties("fertilizer_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> TREEFELLER_BLOCK = BLOCKS.register("treefeller_block", new TreefellerBlock(blockProperties("treefeller_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> PIPE_BOOSTER_BLOCK = BLOCKS.register("pipe_booster_block", new PipeBoosterBlock(blockProperties("pipe_booster_block", Blocks.IRON_BLOCK).noOcclusion()));
    
    @UseGeoBlockItem(scale = 0.7f)
    @ItemRarity(ItemRarityValue.RARE)
    public static final DeferredBlock<Block> ENCHANTMENT_CATALYST_BLOCK = BLOCKS.register("enchantment_catalyst_block", new EnchantmentCatalystBlock(blockProperties("enchantment_catalyst_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    @ItemRarity(ItemRarityValue.RARE)
    public static final DeferredBlock<Block> ENCHANTER_BLOCK = BLOCKS.register("enchanter_block", new EnchanterBlock(blockProperties("enchanter_block", Blocks.IRON_BLOCK).noOcclusion()));
    @ItemRarity(ItemRarityValue.RARE)
    public static final DeferredBlock<Block> SPAWNER_CONTROLLER_BLOCK = BLOCKS.register("spawner_controller_block", new SpawnerControllerBlock(blockProperties("spawner_controller_block", Blocks.IRON_BLOCK).noOcclusion()));
    @NoAutoDrop
    public static final DeferredBlock<Block> WITHER_CROP_BLOCK = BLOCKS.register("wither_crop_block", new WitheredCropBlock(blockProperties("wither_crop_block", Blocks.WHEAT)));
    
    @NoBlockItem
    public static final DeferredBlock<Block> UNSTABLE_CONTAINER = BLOCKS.register("unstable_container", new UnstableContainerBlock(blockProperties("unstable_container", Blocks.OBSIDIAN).strength(80, 1900f).noOcclusion().forceSolidOn()));
    
    public static final DeferredBlock<Block> ACCELERATOR_RING = BLOCKS.register("accelerator_ring", new AcceleratorRingBlock(blockProperties("accelerator_ring", Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> ACCELERATOR_MOTOR = BLOCKS.register("accelerator_motor", new AcceleratorMotorBlock(blockProperties("accelerator_motor", Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 5)));
    public static final DeferredBlock<Block> ACCELERATOR_CONTROLLER = BLOCKS.register("accelerator_controller", new AcceleratorControllerBlock(blockProperties("accelerator_controller", Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> ACCELERATOR_SENSOR = BLOCKS.register("accelerator_sensor", new AcceleratorSensorBlock(blockProperties("accelerator_sensor", Blocks.IRON_BLOCK).noOcclusion()));
    @ItemRarity(ItemRarityValue.EPIC)
    public static final DeferredBlock<Block> BLACK_HOLE_BLOCK = BLOCKS.register("black_hole_block", new BlackHoleBlock(blockProperties("black_hole_block", Blocks.END_PORTAL).lightLevel(item -> 12).noOcclusion().forceSolidOn()));
    
    public static final DeferredBlock<Block> PARTICLE_COLLECTOR_BLOCK = BLOCKS.register("particle_collector_block", new ParticleCollectorBlock(blockProperties("particle_collector_block", Blocks.GLASS).noOcclusion()));
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> PUMP_BLOCK = BLOCKS.register("pump_block", new PumpBlock(blockProperties("pump_block", Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> CHARGER_BLOCK = BLOCKS.register("charger_block", new ChargerBlock(blockProperties("charger_block", Blocks.IRON_BLOCK).noOcclusion()));
    
    public static final DeferredBlock<Block> MACHINE_CORE_1 = BLOCKS.register("machine_core_1", new MachineCoreBlock(blockProperties("machine_core_1", Blocks.IRON_BLOCK).noOcclusion(), 1));
    public static final DeferredBlock<Block> MACHINE_CORE_2 = BLOCKS.register("machine_core_2", new MachineCoreBlock(blockProperties("machine_core_2", Blocks.IRON_BLOCK).noOcclusion(), 2));
    public static final DeferredBlock<Block> MACHINE_CORE_3 = BLOCKS.register("machine_core_3", new MachineCoreBlock(blockProperties("machine_core_3", Blocks.IRON_BLOCK).noOcclusion(), 3));
    public static final DeferredBlock<Block> MACHINE_CORE_4 = BLOCKS.register("machine_core_4", new MachineCoreBlock(blockProperties("machine_core_4", Blocks.IRON_BLOCK).noOcclusion(), 4));
    public static final DeferredBlock<Block> MACHINE_CORE_5 = BLOCKS.register("machine_core_5", new MachineCoreBlock(blockProperties("machine_core_5", Blocks.IRON_BLOCK).noOcclusion(), 5));
    public static final DeferredBlock<Block> MACHINE_CORE_6 = BLOCKS.register("machine_core_6", new MachineCoreBlock(blockProperties("machine_core_6", Blocks.IRON_BLOCK).noOcclusion(), 6));
    public static final DeferredBlock<Block> MACHINE_CORE_7 = BLOCKS.register("machine_core_7", new MachineCoreBlock(blockProperties("machine_core_7", Blocks.IRON_BLOCK).noOcclusion(), 7));
    @NoBlockItem
    public static final DeferredBlock<Block> MACHINE_CORE_HIDDEN = BLOCKS.register("machine_core_hidden", new MachineCoreBlock(blockProperties("machine_core_hidden", Blocks.OBSIDIAN).strength(80, 1900f).noOcclusion().forceSolidOn(), 1));
    
    public static final DeferredBlock<Block> MACHINE_SPEED_ADDON = BLOCKS.register("machine_speed_addon", new MachineAddonBlock(blockProperties("machine_speed_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withSpeedMultiplier(OritechStartupConfig.speedAddonSpeed.get().floatValue()).withEfficiencyMultiplier(OritechStartupConfig.speedAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.MACHINE_SPEED_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_EFFICIENCY_ADDON = BLOCKS.register("machine_efficiency_addon", new MachineAddonBlock(blockProperties("machine_efficiency_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withEfficiencyMultiplier(OritechStartupConfig.efficiencyAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.MACHINE_EFFICIENCY_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_ULTIMATE_ADDON = BLOCKS.register("machine_ultimate_addon", new MachineAddonBlock(blockProperties("machine_ultimate_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withSpeedMultiplier(OritechStartupConfig.ultimateAddonSpeed.get().floatValue()).withEfficiencyMultiplier(OritechStartupConfig.ultimateAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.MACHINE_ULTIMATE_ADDON_SHAPE)));
    public static final DeferredBlock<Block> QUARRY_ADDON = BLOCKS.register("quarry_addon", new MachineAddonBlock(blockProperties("quarry_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.QUARRY_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_PROCESSING_ADDON = BLOCKS.register("machine_processing_addon", new MachineAddonBlock(blockProperties("machine_processing_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withEfficiencyMultiplier(OritechStartupConfig.chamberAddonEfficiency.get().floatValue()).withChambers(1).withBoundingShape(MachineAddonBlock.MACHINE_PROCESSING_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_FLUID_ADDON = BLOCKS.register("machine_fluid_addon", new MachineAddonBlock(blockProperties("machine_fluid_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_FLUID_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_YIELD_ADDON = BLOCKS.register("machine_yield_addon", new MachineAddonBlock(blockProperties("machine_yield_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_YIELD_ADDON_SHAPE)));
    public static final DeferredBlock<Block> CROP_FILTER_ADDON = BLOCKS.register("crop_filter_addon", new MachineAddonBlock(blockProperties("crop_filter_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.CROP_FILTER_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_HUNTER_ADDON = BLOCKS.register("machine_hunter_addon", new MachineAddonBlock(blockProperties("machine_hunter_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_HUNTER_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_CAPACITOR_ADDON = BLOCKS.register("machine_capacitor_addon", new MachineAddonBlock(blockProperties("machine_capacitor_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withAddedCapacity(2_000_000).withAddedInsert(2_000).withBoundingShape(MachineAddonBlock.MACHINE_CAPACITOR_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_ACCEPTOR_ADDON = BLOCKS.register("machine_acceptor_addon", new MachineAddonBlock(blockProperties("machine_acceptor_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withAddedCapacity(500_000).withAddedInsert(5_000).withAcceptEnergy(true).withBoundingShape(MachineAddonBlock.MACHINE_ACCEPTOR_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_INVENTORY_PROXY_ADDON = BLOCKS.register("machine_inventory_proxy_addon", new InventoryProxyAddonBlock(blockProperties("machine_inventory_proxy_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_INVENTORY_PROXY_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_EXTENDER = BLOCKS.register("machine_extender", new MachineAddonBlock(blockProperties("machine_extender", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false)));
    public static final DeferredBlock<Block> CAPACITOR_ADDON_EXTENDER = BLOCKS.register("capacitor_addon_extender", new MachineAddonBlock(blockProperties("capacitor_addon_extender", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false).withAddedCapacity(2_500_000).withAddedInsert(1_000)));
    public static final DeferredBlock<Block> STEAM_BOILER_ADDON = BLOCKS.register("steam_boiler_addon", new SteamBoilerAddonBlock(blockProperties("steam_boiler_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.STEAM_BOILER_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_REDSTONE_ADDON = BLOCKS.register("machine_redstone_addon", new RedstoneAddonBlock(blockProperties("machine_redstone_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_REDSTONE_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_SILK_TOUCH_ADDON = BLOCKS.register("machine_silk_touch_addon", new MachineAddonBlock(blockProperties("machine_silk_touch_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_SILK_TOUCH_ADDON_SHAPE)));
    public static final DeferredBlock<Block> MACHINE_BURST_ADDON = BLOCKS.register("machine_burst_addon", new MachineAddonBlock(blockProperties("machine_burst_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBurstTicks(OritechStartupConfig.burstAddonTicks.get()).withBoundingShape(MachineAddonBlock.MACHINE_BURST_ADDON_SHAPE))); // todo config settings
    
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.none)
    @ItemRarity(ItemRarityValue.EPIC)
    public static final DeferredBlock<Block> MACHINE_COMBI_ADDON = BLOCKS.register("machine_combi_addon", new CombiAddonBlock(blockProperties("machine_combi_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_COMBI_ADDON_SHAPE)));
    
    //region reactor
    @ItemRarity(ItemRarityValue.UNCOMMON)
    public static final DeferredBlock<Block> REACTOR_CONTROLLER = BLOCKS.register("reactor_controller", new ReactorControllerBlock(blockProperties("reactor_controller", Blocks.IRON_BLOCK).lightLevel(state -> 5)));
    public static final DeferredBlock<Block> REACTOR_WALL = BLOCKS.register("reactor_wall", new ReactorWallBlock(blockProperties("reactor_wall", Blocks.NETHERITE_BLOCK).strength(10, 1800)));
    public static final DeferredBlock<Block> REACTOR_ROD = BLOCKS.register("reactor_rod", new ReactorRodBlock(blockProperties("reactor_rod", Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3), 1, 1));
    public static final DeferredBlock<Block> REACTOR_DOUBLE_ROD = BLOCKS.register("reactor_double_rod", new ReactorRodBlock(blockProperties("reactor_double_rod", Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3), 2, 4));
    public static final DeferredBlock<Block> REACTOR_QUAD_ROD = BLOCKS.register("reactor_quad_rod", new ReactorRodBlock(blockProperties("reactor_quad_rod", Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3), 4, 12));
    public static final DeferredBlock<Block> REACTOR_VENT = BLOCKS.register("reactor_vent", new ReactorHeatVentBlock(blockProperties("reactor_vent", Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> REACTOR_REFLECTOR = BLOCKS.register("reactor_reflector", new ReactorReflectorBlock(blockProperties("reactor_reflector", Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> 15)));
    public static final DeferredBlock<Block> REACTOR_HEAT_PIPE = BLOCKS.register("reactor_heat_pipe", new ReactorHeatPipeBlock(blockProperties("reactor_heat_pipe", Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> REACTOR_CONDENSER = BLOCKS.register("reactor_condenser", new ReactorAbsorberBlock(blockProperties("reactor_condenser", Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> REACTOR_FUEL_PORT = BLOCKS.register("reactor_fuel_port", new ReactorFuelPortBlock(blockProperties("reactor_fuel_port", Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> REACTOR_ABSORBER_PORT = BLOCKS.register("reactor_absorber_port", new ReactorAbsorberPortBlock(blockProperties("reactor_absorber_port", Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> REACTOR_ENERGY_PORT = BLOCKS.register("reactor_energy_port", new ReactorEnergyPortBlock(blockProperties("reactor_energy_port", Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> REACTOR_REDSTONE_PORT = BLOCKS.register("reactor_redstone_port", new ReactorRedstonePortBlock(blockProperties("reactor_redstone_port", Blocks.IRON_BLOCK).noOcclusion()));
    
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> URANIUM_CRYSTAL = BLOCKS.register("uranium_crystal", new AmethystClusterBlock(7, 3, blockProperties("uranium_crystal", Blocks.AMETHYST_CLUSTER).lightLevel(state -> 5)));
    
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_EXPLOSION_SMALL = BLOCKS.register("reactor_explosion_small", new NuclearExplosionBlock(blockProperties("reactor_explosion_small", Blocks.IRON_BLOCK), 9));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_EXPLOSION_MEDIUM = BLOCKS.register("reactor_explosion_medium", new NuclearExplosionBlock(blockProperties("reactor_explosion_medium", Blocks.IRON_BLOCK), 14));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_EXPLOSION_LARGE = BLOCKS.register("reactor_explosion_large", new NuclearExplosionBlock(blockProperties("reactor_explosion_large", Blocks.IRON_BLOCK), 20));
    public static final DeferredBlock<Block> LOW_YIELD_NUKE = BLOCKS.register("low_yield_nuke", new NukeBlock(blockProperties("low_yield_nuke", Blocks.IRON_BLOCK), true));
    public static final DeferredBlock<Block> NUKE = BLOCKS.register("nuke", new NukeBlock(blockProperties("nuke", Blocks.IRON_BLOCK), false));
    
    // cooling cell, early game re-fillable component
    
    // lategame, second stage components:
    // plasma conduit, advanced heat transfer system
    // entropy dampener, reduce degradation rate of nearby components
    // quantum stabilizer, massively increase heat capacity of reactor
    //endregion
    
    //region metals
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> NICKEL_ORE = BLOCKS.register("nickel_ore", new Block(blockProperties("nickel_ore", Blocks.IRON_ORE)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> DEEPSLATE_NICKEL_ORE = BLOCKS.register("deepslate_nickel_ore", new Block(blockProperties("deepslate_nickel_ore", Blocks.DEEPSLATE_IRON_ORE)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> ENDSTONE_PLATINUM_ORE = BLOCKS.register("endstone_platinum_ore", new Block(blockProperties("endstone_platinum_ore", Blocks.DIAMOND_ORE)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> DEEPSLATE_PLATINUM_ORE = BLOCKS.register("deepslate_platinum_ore", new Block(blockProperties("deepslate_platinum_ore", Blocks.DEEPSLATE_DIAMOND_ORE)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> DEEPSLATE_URANIUM_ORE = BLOCKS.register("deepslate_uranium_ore", new Block(blockProperties("deepslate_uranium_ore", Blocks.DEEPSLATE_DIAMOND_ORE)));
    //endregion
    
    //region resource nodes
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RESOURCE_NODE_REDSTONE = BLOCKS.register("resource_node_redstone", new Block(blockProperties("resource_node_redstone", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RESOURCE_NODE_LAPIS = BLOCKS.register("resource_node_lapis", new Block(blockProperties("resource_node_lapis", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RESOURCE_NODE_IRON = BLOCKS.register("resource_node_iron", new Block(blockProperties("resource_node_iron", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RESOURCE_NODE_COAL = BLOCKS.register("resource_node_coal", new Block(blockProperties("resource_node_coal", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RESOURCE_NODE_GOLD = BLOCKS.register("resource_node_gold", new Block(blockProperties("resource_node_gold", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RESOURCE_NODE_EMERALD = BLOCKS.register("resource_node_emerald", new Block(blockProperties("resource_node_emerald", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RESOURCE_NODE_DIAMOND = BLOCKS.register("resource_node_diamond", new Block(blockProperties("resource_node_diamond", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RESOURCE_NODE_COPPER = BLOCKS.register("resource_node_copper", new Block(blockProperties("resource_node_copper", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RESOURCE_NODE_NICKEL = BLOCKS.register("resource_node_nickel", new Block(blockProperties("resource_node_nickel", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RESOURCE_NODE_PLATINUM = BLOCKS.register("resource_node_platinum", new Block(blockProperties("resource_node_platinum", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RESOURCE_NODE_URANIUM = BLOCKS.register("resource_node_uranium", new Block(blockProperties("resource_node_uranium", Blocks.BEDROCK)));
    
    // region decorative
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> CEILING_LIGHT = BLOCKS.register("ceiling_light", new WallMountedLight(blockProperties("ceiling_light", Blocks.GLOWSTONE).noOcclusion(), 2));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> CEILING_LIGHT_HANGING = BLOCKS.register("ceiling_light_hanging", new WallMountedLight(blockProperties("ceiling_light_hanging", Blocks.GLOWSTONE).noOcclusion(), 12));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> TECH_BUTTON = BLOCKS.register("tech_button", new TechRedstoneButton(BlockSetType.IRON, 80, blockProperties("tech_button", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> TECH_LEVER = BLOCKS.register("tech_lever", new TechLever(blockProperties("tech_lever", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> MACHINE_PLATING_BLOCK = BLOCKS.register("machine_plating_block", new Block(blockProperties("machine_plating_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @NoAutoDrop
    public static final DeferredBlock<Block> MACHINE_PLATING_SLAB = BLOCKS.register("machine_plating_slab", new SlabBlock(blockProperties("machine_plating_slab", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> MACHINE_PLATING_STAIRS = BLOCKS.register("machine_plating_stairs", new StairBlock(blockValue(MACHINE_PLATING_BLOCK).defaultBlockState(), blockProperties("machine_plating_stairs", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> MACHINE_PLATING_PRESSURE_PLATE = BLOCKS.register("machine_plating_pressure_plate", new PressurePlateBlock(BlockSetType.IRON, blockProperties("machine_plating_pressure_plate", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> IRON_PLATING_BLOCK = BLOCKS.register("iron_plating_block", new Block(blockProperties("iron_plating_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @NoAutoDrop
    public static final DeferredBlock<Block> IRON_PLATING_SLAB = BLOCKS.register("iron_plating_slab", new SlabBlock(blockProperties("iron_plating_slab", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> IRON_PLATING_STAIRS = BLOCKS.register("iron_plating_stairs", new StairBlock(blockValue(IRON_PLATING_BLOCK).defaultBlockState(), blockProperties("iron_plating_stairs", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> IRON_PLATING_PRESSURE_PLATE = BLOCKS.register("iron_plating_pressure_plate", new PressurePlateBlock(BlockSetType.IRON, blockProperties("iron_plating_pressure_plate", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> CARBON_PLATING_BLOCK = BLOCKS.register("carbon_plating_block", new Block(blockProperties("carbon_plating_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @NoAutoDrop
    public static final DeferredBlock<Block> CARBON_PLATING_SLAB = BLOCKS.register("carbon_plating_slab", new SlabBlock(blockProperties("carbon_plating_slab", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> CARBON_PLATING_STAIRS = BLOCKS.register("carbon_plating_stairs", new StairBlock(blockValue(CARBON_PLATING_BLOCK).defaultBlockState(), blockProperties("carbon_plating_stairs", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> CARBON_PLATING_PRESSURE_PLATE = BLOCKS.register("carbon_plating_pressure_plate", new PressurePlateBlock(BlockSetType.IRON, blockProperties("carbon_plating_pressure_plate", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> NICKEL_PLATING_BLOCK = BLOCKS.register("nickel_plating_block", new Block(blockProperties("nickel_plating_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @NoAutoDrop
    public static final DeferredBlock<Block> NICKEL_PLATING_SLAB = BLOCKS.register("nickel_plating_slab", new SlabBlock(blockProperties("nickel_plating_slab", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> NICKEL_PLATING_STAIRS = BLOCKS.register("nickel_plating_stairs", new StairBlock(blockValue(NICKEL_PLATING_BLOCK).defaultBlockState(), blockProperties("nickel_plating_stairs", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> NICKEL_PLATING_PRESSURE_PLATE = BLOCKS.register("nickel_plating_pressure_plate", new PressurePlateBlock(BlockSetType.IRON, blockProperties("nickel_plating_pressure_plate", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> METAL_BEAM_BLOCK = BLOCKS.register("metal_beam_block", new MetalBeamBlock(blockProperties("metal_beam_block", Blocks.IRON_BLOCK).noOcclusion().forceSolidOn()));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative) // todo recipe
    public static final DeferredBlock<Block> METAL_GIRDER_BLOCK = BLOCKS.register("metal_girder_block", new MetalGirderBlock(blockProperties("metal_girder_block", Blocks.IRON_BLOCK).noOcclusion().forceSolidOn()));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> INDUSTRIAL_GLASS_BLOCK = BLOCKS.register("industrial_glass_block", new Block(blockProperties("industrial_glass_block", Blocks.GLASS).requiresCorrectToolForDrops().strength(7.0F, 8.0F).noOcclusion()));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @UseGeoBlockItem(scale = 0.5f)
    public static final DeferredBlock<Block> TECH_DOOR = BLOCKS.register("tech_door", new TechDoorBlock(blockProperties("tech_door", Blocks.IRON_DOOR).strength(8f).forceSolidOn()));
    @NoBlockItem
    @NoAutoDrop
    public static final DeferredBlock<Block> TECH_DOOR_HINGE = BLOCKS.register("tech_door_hinge", new TechDoorBlockHinge(blockProperties("tech_door_hinge", Blocks.IRON_DOOR).strength(8f).forceSolidOn()));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @UseGeoBlockItem(scale = 0.37f)
    public static final DeferredBlock<Block> HANGAR_DOOR = BLOCKS.register("hangar_door", new HangarDoorBlock(blockProperties("hangar_door", Blocks.IRON_DOOR).strength(8f).forceSolidOn()));
    @NoBlockItem
    @NoAutoDrop
    public static final DeferredBlock<Block> HANGAR_DOOR_HELPER = BLOCKS.register("hangar_door_helper", new HangarDoorHelperBlock(blockProperties("hangar_door_helper", Blocks.IRON_DOOR).strength(8f).forceSolidOn()));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> STEEL_BLOCK = BLOCKS.register("steel_block", new Block(blockProperties("steel_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> ENERGITE_BLOCK = BLOCKS.register("energite_block", new Block(blockProperties("energite_block", Blocks.IRON_BLOCK).lightLevel(state -> 6)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> NICKEL_BLOCK = BLOCKS.register("nickel_block", new Block(blockProperties("nickel_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> BIOSTEEL_BLOCK = BLOCKS.register("biosteel_block", new Block(blockProperties("biosteel_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> PLATINUM_BLOCK = BLOCKS.register("platinum_block", new Block(blockProperties("platinum_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> ADAMANT_BLOCK = BLOCKS.register("adamant_block", new Block(blockProperties("adamant_block", Blocks.DIAMOND_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> ELECTRUM_BLOCK = BLOCKS.register("electrum_block", new Block(blockProperties("electrum_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> DURATIUM_BLOCK = BLOCKS.register("duratium_block", new Block(blockProperties("duratium_block", Blocks.NETHERITE_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @Compostable(1.0f)
    public static final DeferredBlock<Block> BIOMASS_BLOCK = BLOCKS.register("biomass_block", new Block(blockProperties("biomass_block", Blocks.IRON_BLOCK).sound(SoundType.MOSS)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> PLASTIC_BLOCK = BLOCKS.register("plastic_block", new Block(blockProperties("plastic_block", Blocks.IRON_BLOCK).sound(SoundType.SHROOMLIGHT)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> FLUXITE_BLOCK = BLOCKS.register("fluxite_block", new Block(blockProperties("fluxite_block", Blocks.IRON_BLOCK).sound(SoundType.AMETHYST)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> SILICON_BLOCK = BLOCKS.register("silicon_block", new SlimeBlock(blockProperties("silicon_block", Blocks.SLIME_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RAW_NICKEL_BLOCK = BLOCKS.register("raw_nickel_block", new Block(blockProperties("raw_nickel_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RAW_PLATINUM_BLOCK = BLOCKS.register("raw_platinum_block", new Block(blockProperties("raw_platinum_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> RAW_URANIUM_BLOCK = BLOCKS.register("raw_uranium_block", new Block(blockProperties("raw_uranium_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final DeferredBlock<Block> URANIUM_DUST_BLOCK = BLOCKS.register("uranium_dust_block", new Block(blockProperties("uranium_dust_block", Blocks.IRON_BLOCK).lightLevel(state -> 2)));
    //endregion
    
    private static void postProcessField(Block value, Identifier identifier, Field field, DeferredBlock<? extends Block> supplier) {
        var path = identifier.getPath();
        
        if (field.isAnnotationPresent(NoBlockItem.class)) return;
        
        var targetGroup = ItemContent.Groups.machines;
        if (field.isAnnotationPresent(ItemContent.ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemContent.ItemGroupTarget.class).value();
        }
        var rarity = getItemRarity(field);
        
        if (field.isAnnotationPresent(UseGeoBlockItem.class)) {
            var scale = field.getAnnotation(UseGeoBlockItem.class).scale();
            ItemContent.registerItem(path, getGeoBlockItem(value, path, scale, rarity));
        } else if (FluidApi.ITEM != null && (value instanceof SmallFluidTank)) {
            var item = value.equals(blockValue(SMALL_TANK_BLOCK)) ? ItemContent.itemValue(SMALL_TANK_ITEM) : ItemContent.itemValue(CREATIVE_TANK_ITEM);
            FluidApi.ITEM.registerForItem(() -> item);
        } else if (value.equals(blockValue(SMALL_STORAGE_BLOCK)) && EnergyApi.ITEM != null) {
            var item = new SmallEnergyStorageBlockItem(value, blockItemProperties(path).component(EnergyApi.ITEM.getEnergyComponent(), 0L));
            ItemContent.registerItem(path, item);
            EnergyApi.ITEM.registerForItem(() -> item);
            
            var variantStack = new ItemStack(item);
            variantStack.set(EnergyApi.ITEM.getEnergyComponent(), 1_000_000L);
            ItemGroups.add(targetGroup, variantStack);
            
        } else {
            ItemContent.registerItem(path, createBlockItem(value, rarity, path));
        }
        
        if (!field.isAnnotationPresent(NoAutoDrop.class)) {
            autoRegisteredDrops.add(value);
        }
        
        if (field.isAnnotationPresent(DispenserPlace.class)) {
            DispenserBlock.registerBehavior(value, new ShulkerBoxDispenseBehavior());
        }

        if (field.isAnnotationPresent(Compostable.class)) {
            Oritech.COMPOSTABLES_DATA.add(new Pair<>(value, field.getAnnotation(Compostable.class).value()));
        }
        
        ItemGroups.add(targetGroup, value);
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface UseGeoBlockItem {
        float scale(); // scale
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface ItemRarity {
        ItemRarityValue value();
    }
    
    // helper enum because using the minecraft-native one causes mapping issues in production builds somehow?
    public enum ItemRarityValue {
        UNCOMMON(Rarity.UNCOMMON),
        RARE(Rarity.RARE),
        EPIC(Rarity.EPIC);
        
        private final Rarity rarity;
        
        ItemRarityValue(Rarity rarity) {
            this.rarity = rarity;
        }
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface NoAutoDrop {
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface DispenserPlace {
    }
    
}

