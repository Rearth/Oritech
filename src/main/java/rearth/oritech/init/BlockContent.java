package rearth.oritech.init;

import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
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
import rearth.oritech.block.blocks.augmenter.CyberneticAugmentationCenterBlock;
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
import rearth.oritech.util.ColorableMachine;
import rearth.oritech.util.RegistryReflectionUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("NullableProblems")
public class BlockContent {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Oritech.MOD_ID);
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(Oritech.MOD_ID);

    // hints for item groups
    public static final List<Pair<DeferredItem<BlockItem>, ItemContent.Groups>> BLOCK_GROUPS = new ArrayList<>();

    public static final DeferredBlock<Block> SPAWNER_CAGE = BLOCKS.registerBlock("spawner_cage", SpawnerCageBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()); // sample 1

    public static final DeferredBlock<Block> MACHINE_FRAME = BLOCKS.registerBlock("machine_frame", MachineFrameBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS)); // sample 2 new

    public static final DeferredBlock<Block> FLUID_PIPE = BLOCKS.registerBlock("fluid_pipe", FluidPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> FRAMED_FLUID_PIPE = BLOCKS.registerBlock("framed_fluid_pipe", FluidPipeBlock.FramedFluidPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> FLUID_PIPE_DUCT = BLOCKS.registerBlock("fluid_pipe_duct", FluidPipeDuctBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> ENERGY_PIPE = BLOCKS.registerBlock("energy_pipe", EnergyPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> FRAMED_ENERGY_PIPE = BLOCKS.registerBlock("framed_energy_pipe", EnergyPipeBlock.FramedEnergyPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> ENERGY_PIPE_DUCT = BLOCKS.registerBlock("energy_pipe_duct", EnergyPipeDuctBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> SUPERCONDUCTOR = BLOCKS.registerBlock("superconductor", SuperConductorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> FRAMED_SUPERCONDUCTOR = BLOCKS.registerBlock("framed_superconductor", SuperConductorBlock.FramedSuperConductorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> SUPERCONDUCTOR_DUCT = BLOCKS.registerBlock("superconductor_duct", SuperConductorDuctBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> ITEM_PIPE = BLOCKS.registerBlock("item_pipe", ItemPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> TRANSPARENT_ITEM_PIPE = BLOCKS.registerBlock("transparent_item_pipe", ItemPipeBlock.TransparentItemPipe::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> FRAMED_ITEM_PIPE = BLOCKS.registerBlock("framed_item_pipe", ItemPipeBlock.FramedItemPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final DeferredBlock<Block> ITEM_PIPE_DUCT = BLOCKS.registerBlock("item_pipe_duct", ItemPipeDuctBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> ITEM_FILTER = BLOCKS.registerBlock("item_filter", ItemFilterBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));

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

    public static final DeferredBlock<Block> ENERGY_TRANSMISSION_POLE = BLOCKS.registerBlock("energy_transmission_pole", EnergyTransmissionPoleBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());

    @NoBlockItem
    public static final DeferredBlock<Block> FRAME_GANTRY_ARM = BLOCKS.registerSimpleBlock("frame_gantry_arm", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> BLOCK_DESTROYER_HEAD = BLOCKS.registerSimpleBlock("block_destroyer_head", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> BLOCK_PLACER_HEAD = BLOCKS.registerSimpleBlock("block_placer_head", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> BLOCK_FERTILIZER_HEAD = BLOCKS.registerSimpleBlock("block_fertilizer_head", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> PUMP_TRUNK = BLOCKS.registerSimpleBlock("pump_trunk", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> QUARRY_BEAM_RING = BLOCKS.registerSimpleBlock("quarry_beam_ring", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion().lightLevel(item -> 5));
    @NoBlockItem
    public static final DeferredBlock<Block> BLACK_HOLE_INNER = BLOCKS.registerSimpleBlock("black_hole_inner", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion().lightLevel(item -> 5));
    @NoBlockItem
    public static final DeferredBlock<Block> BLACK_HOLE_MIDDLE = BLOCKS.registerSimpleBlock("black_hole_middle", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion().lightLevel(item -> 5));
    @NoBlockItem
    public static final DeferredBlock<Block> BLACK_HOLE_OUTER = BLOCKS.registerSimpleBlock("black_hole_outer", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).noOcclusion().lightLevel(item -> 5));

    @NoBlockItem
    public static final DeferredBlock<Block> ADDON_INDICATOR = BLOCKS.registerSimpleBlock("addon_indicator", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_COLD_INDICATOR = BLOCKS.registerSimpleBlock("reactor_cold_indicator", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_MEDIUM_INDICATOR = BLOCKS.registerSimpleBlock("reactor_medium_indicator", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_HOT_INDICATOR = BLOCKS.registerSimpleBlock("reactor_hot_indicator", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));

    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> PULVERIZER = BLOCKS.registerBlock("pulverizer", PulverizerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> FRAGMENT_FORGE = BLOCKS.registerBlock("fragment_forge", FragmentForge::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> ASSEMBLER = BLOCKS.registerBlock("assembler", AssemblerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> FOUNDRY = BLOCKS.registerBlock("foundry", FoundryBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> INDUSTRIAL_CHILLER = BLOCKS.registerBlock("industrial_chiller", IndustrialChillerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> CENTRIFUGE = BLOCKS.registerBlock("centrifuge", CentrifugeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f, defaultColor = ColorableMachine.ColorVariant.INDUSTRIAL)
    @ItemRarity(Rarity.RARE)
    public static final DeferredBlock<Block> ATOMIC_FORGE = BLOCKS.registerBlock("atomic_forge", AtomicForgeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f, defaultColor = ColorableMachine.ColorVariant.FLUXITE)
    public static final DeferredBlock<Block> REFINERY = BLOCKS.registerBlock("refinery", RefineryBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    @NoAutoDrop
    public static final DeferredBlock<Block> TAINTED_REFINERY = BLOCKS.registerBlock("tainted_refinery", TaintedRefineryBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(7f, 2000f).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f, defaultColor = ColorableMachine.ColorVariant.FLUXITE)
    public static final DeferredBlock<Block> REFINERY_CHAMBER_MODULE = BLOCKS.registerBlock("refinery_chamber_module", RefineryChamberModuleBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> BIO_GENERATOR = BLOCKS.registerBlock("bio_generator", BioGeneratorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> LAVA_GENERATOR = BLOCKS.registerBlock("lava_generator", LavaGeneratorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    public static final DeferredBlock<Block> FUEL_GENERATOR = BLOCKS.registerBlock("fuel_generator", FuelGeneratorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> BASIC_GENERATOR = BLOCKS.registerBlock("basic_generator", BasicGeneratorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> STEAM_ENGINE = BLOCKS.registerBlock("steam_engine", SteamEngineBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> BIG_SOLAR_PANEL = BLOCKS.registerBlock("big_solar_panel", BigSolarPanelBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> POWERED_FURNACE = BLOCKS.registerBlock("powered_furnace", PoweredFurnaceBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0));
    @UseGeoBlockItem(scale = 0.5f)
    public static final DeferredBlock<Block> ENDERIC_LASER = BLOCKS.registerBlock("enderic_laser", EndericLaserBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.25f)
    public static final DeferredBlock<Block> BEDROCK_EXTRACTOR = BLOCKS.registerBlock("bedrock_extractor", BedrockExtractorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    public static final DeferredBlock<Block> DRONE_PORT = BLOCKS.registerBlock("drone_port", DronePortBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> ADDON_SPLICER = BLOCKS.registerBlock("addon_splicer", AddonSplicerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());

    @NoAutoDrop
    @DispenserPlace
    @NoBlockItem
    public static final DeferredBlock<Block> PORTABLE_ENERGY_STORAGE = BLOCKS.registerBlock("portable_energy_storage", PortableEnergyStorageBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> LARGE_STORAGE = BLOCKS.registerBlock("large_storage", LargeStorageBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @DispenserPlace
    public static final DeferredBlock<Block> CREATIVE_STORAGE = BLOCKS.registerBlock("creative_storage", CreativeStorageBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK).destroyTime(-1.0F));

    @NoAutoDrop
    @DispenserPlace
    @NoBlockItem
    public static final DeferredBlock<Block> PORTABLE_TANK = BLOCKS.registerBlock("portable_tank", SmallFluidTank::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0));

    @NoAutoDrop
    @DispenserPlace
    @NoBlockItem
    public static final DeferredBlock<Block> CREATIVE_TANK = BLOCKS.registerBlock("creative_tank", CreativeFluidTank::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0).destroyTime(-1.0F));

    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> CYBERNETIC_AUGMENTATION_CENTER = BLOCKS.registerBlock("cybernetic_augmentation_center", CyberneticAugmentationCenterBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> CYBERNETIC_RESEARCH_STATION = BLOCKS.registerBlock("cybernetic_research_station", AugmentResearchStationBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2));
    public static final DeferredBlock<Block> QUANTUM_RESEARCH_STATION = BLOCKS.registerBlock("quantum_research_station", AugmentResearchStationBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2));
    public static final DeferredBlock<Block> ARCANE_AUGMENT_STATION = BLOCKS.registerBlock("arcane_augment_station", AugmentResearchStationBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2));

    public static final DeferredBlock<Block> PLACER = BLOCKS.registerBlock("placer", PlacerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> DESTROYER = BLOCKS.registerBlock("destroyer", DestroyerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> FERTILIZER = BLOCKS.registerBlock("fertilizer", FertilizerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> TREE_CUTTER = BLOCKS.registerBlock("tree_cutter", TreeCutterBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> PIPE_BOOSTER = BLOCKS.registerBlock("pipe_booster", PipeBoosterBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());

    @UseGeoBlockItem(scale = 0.7f)
    @ItemRarity(Rarity.RARE)
    public static final DeferredBlock<Block> ARCANE_CATALYST = BLOCKS.registerBlock("arcane_catalyst", ArcaneCatalystBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    @ItemRarity(Rarity.RARE)
    public static final DeferredBlock<Block> STABILIZED_ENCHANTER = BLOCKS.registerBlock("stabilized_enchanter", StabilizedEnchanterBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @ItemRarity(Rarity.RARE)
    public static final DeferredBlock<Block> SPAWNER_CONTROLLER = BLOCKS.registerBlock("spawner_controller", SpawnerControllerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @NoAutoDrop
    public static final DeferredBlock<Block> SOUL_FLOWERS = BLOCKS.registerBlock("soul_flowers", SoulFlowersBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));

    @NoBlockItem
    public static final DeferredBlock<Block> SCHRODINGERS_SAFE = BLOCKS.registerBlock("schrodingers_safe", SchrodingersSafeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(80, 1900f).noOcclusion().forceSolidOn());

    public static final DeferredBlock<Block> ACCELERATOR_RING = BLOCKS.registerBlock("accelerator_ring", AcceleratorRingBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> ACCELERATOR_MOTOR = BLOCKS.registerBlock("accelerator_motor", AcceleratorMotorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 5));
    public static final DeferredBlock<Block> PARTICLE_ACCELERATOR = BLOCKS.registerBlock("particle_accelerator", ParticleAcceleratorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> ACCELERATOR_SENSOR = BLOCKS.registerBlock("accelerator_sensor", AcceleratorSensorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @ItemRarity(Rarity.EPIC)
    @NoAutoDrop
    public static final DeferredBlock<Block> BLACK_HOLE = BLOCKS.registerBlock("black_hole", BlackHoleBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_PORTAL).lightLevel(item -> 12).noOcclusion().forceSolidOn());

    public static final DeferredBlock<Block> TACHYON_ABSORBER = BLOCKS.registerBlock("tachyon_absorber", TachyonAbsorberBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion());

    @UseGeoBlockItem(scale = 0.7f)
    public static final DeferredBlock<Block> PUMP = BLOCKS.registerBlock("pump", PumpBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> EQUIPMENT_CHARGER = BLOCKS.registerBlock("equipment_charger", EquipmentChargerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());

    public static final DeferredBlock<Block> MACHINE_CORE_1 = BLOCKS.registerBlock("machine_core_1", props -> new MachineCoreBlock(props, 1), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_2 = BLOCKS.registerBlock("machine_core_2", props -> new MachineCoreBlock(props, 2), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_3 = BLOCKS.registerBlock("machine_core_3", props -> new MachineCoreBlock(props, 3), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_4 = BLOCKS.registerBlock("machine_core_4", props -> new MachineCoreBlock(props, 4), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_5 = BLOCKS.registerBlock("machine_core_5", props -> new MachineCoreBlock(props, 5), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_6 = BLOCKS.registerBlock("machine_core_6", props -> new MachineCoreBlock(props, 6), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CORE_7 = BLOCKS.registerBlock("machine_core_7", props -> new MachineCoreBlock(props, 7), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @NoBlockItem
    public static final DeferredBlock<Block> COMPLEX_PLATING = BLOCKS.registerBlock("complex_plating", props -> new MachineCoreBlock(props, 1), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(80, 1900f).noOcclusion().forceSolidOn());

    public static final DeferredBlock<Block> MACHINE_SPEED_ADDON = BLOCKS.registerBlock("machine_speed_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withSpeedMultiplier(OritechStartupConfig.speedAddonSpeed.get().floatValue()).withEfficiencyMultiplier(OritechStartupConfig.speedAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.MACHINE_SPEED_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_EFFICIENCY_ADDON = BLOCKS.registerBlock("machine_efficiency_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withEfficiencyMultiplier(OritechStartupConfig.efficiencyAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.MACHINE_EFFICIENCY_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> SYNERGY_MATRIX_ADDON = BLOCKS.registerBlock("synergy_matrix_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withSpeedMultiplier(OritechStartupConfig.ultimateAddonSpeed.get().floatValue()).withEfficiencyMultiplier(OritechStartupConfig.ultimateAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.SYNERGY_MATRIX_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> QUARRY_ADDON = BLOCKS.registerBlock("quarry_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.QUARRY_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> AUXILIARY_PROCESSING_CHAMBER_ADDON = BLOCKS.registerBlock("auxiliary_processing_chamber_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withEfficiencyMultiplier(OritechStartupConfig.chamberAddonEfficiency.get().floatValue()).withChambers(1).withBoundingShape(MachineAddonBlock.AUXILIARY_PROCESSING_CHAMBER_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_FLUID_ADDON = BLOCKS.registerBlock("machine_fluid_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_FLUID_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_YIELD_ADDON = BLOCKS.registerBlock("machine_yield_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_YIELD_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> CROP_FILTER_ADDON = BLOCKS.registerBlock("crop_filter_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.CROP_FILTER_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_HUNTER_ADDON = BLOCKS.registerBlock("machine_hunter_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_HUNTER_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_CAPACITOR_ADDON = BLOCKS.registerBlock("machine_capacitor_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withAddedCapacity(2_000_000).withAddedInsert(2_000).withBoundingShape(MachineAddonBlock.MACHINE_CAPACITOR_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_ACCEPTOR_ADDON = BLOCKS.registerBlock("machine_acceptor_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withAddedCapacity(500_000).withAddedInsert(5_000).withAcceptEnergy(true).withBoundingShape(MachineAddonBlock.MACHINE_ACCEPTOR_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_INVENTORY_PROXY_ADDON = BLOCKS.registerBlock("machine_inventory_proxy_addon", props -> new InventoryProxyAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_INVENTORY_PROXY_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_EXTENDER = BLOCKS.registerBlock("machine_extender", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> POWER_BANK_ADDON_EXTENDER = BLOCKS.registerBlock("power_bank_addon_extender", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false).withAddedCapacity(2_500_000).withAddedInsert(1_000)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> STEAM_BOILER_ADDON = BLOCKS.registerBlock("steam_boiler_addon", props -> new SteamBoilerAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.STEAM_BOILER_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> CONTROL_UNIT_ADDON = BLOCKS.registerBlock("control_unit_addon", props -> new RedstoneAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.CONTROL_UNIT_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_SILK_TOUCH_ADDON = BLOCKS.registerBlock("machine_silk_touch_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_SILK_TOUCH_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> MACHINE_BURST_ADDON = BLOCKS.registerBlock("machine_burst_addon", props -> new MachineAddonBlock(props, AddonSettings.getDefaultSettings().withBurstTicks(OritechStartupConfig.burstAddonTicks.get()).withBoundingShape(MachineAddonBlock.MACHINE_BURST_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()); // todo config settings

    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.NONE)
    @ItemRarity(Rarity.EPIC)
    public static final DeferredBlock<Block> HEART_OF_THE_MACHINE_ADDON = BLOCKS.registerBlock("heart_of_the_machine_addon", props -> new HeartOfTheMachineAddonBlock(props, AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.HEART_OF_THE_MACHINE_ADDON_SHAPE)), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());

    //region reactor
    @ItemRarity(Rarity.UNCOMMON)
    public static final DeferredBlock<Block> NUCLEAR_REACTOR_CONTROLLER = BLOCKS.registerBlock("nuclear_reactor_controller", NuclearReactorControllerBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 5));
    public static final DeferredBlock<Block> REACTOR_WALL = BLOCKS.registerBlock("reactor_wall", ReactorWallBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(10, 1800));
    public static final DeferredBlock<Block> REACTOR_ROD = BLOCKS.registerBlock("reactor_rod", props -> new ReactorRodBlock(props, 1, 1), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3));
    public static final DeferredBlock<Block> REACTOR_DOUBLE_ROD = BLOCKS.registerBlock("reactor_double_rod", props -> new ReactorRodBlock(props, 2, 4), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3));
    public static final DeferredBlock<Block> REACTOR_QUAD_ROD = BLOCKS.registerBlock("reactor_quad_rod", props -> new ReactorRodBlock(props, 4, 12), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3));
    public static final DeferredBlock<Block> REACTOR_HEAT_VENT = BLOCKS.registerBlock("reactor_heat_vent", ReactorHeatVentBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> REACTOR_NEUTRON_REFLECTOR = BLOCKS.registerBlock("reactor_neutron_reflector", ReactorNeutronReflectorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> 15));
    public static final DeferredBlock<Block> REACTOR_HEAT_PIPE = BLOCKS.registerBlock("reactor_heat_pipe", ReactorHeatPipeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> REACTOR_HEAT_ABSORBER = BLOCKS.registerBlock("reactor_heat_absorber", ReactorAbsorberBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> REACTOR_FUEL_PORT = BLOCKS.registerBlock("reactor_fuel_port", ReactorFuelPortBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> REACTOR_COOLANT_ABSORBER_PORT = BLOCKS.registerBlock("reactor_coolant_absorber_port", ReactorCoolantAbsorberPortBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> REACTOR_ENERGY_PORT = BLOCKS.registerBlock("reactor_energy_port", ReactorEnergyPortBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> REACTOR_REDSTONE_PORT = BLOCKS.registerBlock("reactor_redstone_port", ReactorRedstonePortBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());

    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> URANITE_CRYSTAL = BLOCKS.registerBlock("uranite_crystal", props -> new AmethystClusterBlock(11, 6, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel(state -> 5));

    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_EXPLOSION_SMALL = BLOCKS.registerBlock("reactor_explosion_small", props -> new NuclearExplosionBlock(props, 9), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_EXPLOSION_MEDIUM = BLOCKS.registerBlock("reactor_explosion_medium", props -> new NuclearExplosionBlock(props, 14), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @NoBlockItem
    public static final DeferredBlock<Block> REACTOR_EXPLOSION_LARGE = BLOCKS.registerBlock("reactor_explosion_large", props -> new NuclearExplosionBlock(props, 20), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> LOW_YIELD_NUCLEAR_EXPLOSION_DEVICE = BLOCKS.registerBlock("low_yield_nuclear_explosion_device", props -> new ManhattanModuleBlock(props, true), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> MANHATTAN_MODULE = BLOCKS.registerBlock("manhattan_module", props -> new ManhattanModuleBlock(props, false), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

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
    public static final DeferredBlock<Block> REDSTONE_RESOURCE_NODE = BLOCKS.registerSimpleBlock("redstone_resource_node", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RESOURCE_NODE_LAPIS = BLOCKS.registerSimpleBlock("resource_node_lapis", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> IRON_RESOURCE_NODE = BLOCKS.registerSimpleBlock("iron_resource_node", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> COAL_RESOURCE_NODE = BLOCKS.registerSimpleBlock("coal_resource_node", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> GOLD_RESOURCE_NODE = BLOCKS.registerSimpleBlock("gold_resource_node", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> EMERALD_RESOURCE_NODE = BLOCKS.registerSimpleBlock("emerald_resource_node", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> DIAMOND_RESOURCE_NODE = BLOCKS.registerSimpleBlock("diamond_resource_node", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> COPPER_RESOURCE_NODE = BLOCKS.registerSimpleBlock("copper_resource_node", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> NICKEL_RESOURCE_NODE = BLOCKS.registerSimpleBlock("nickel_resource_node", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> PLATINUM_RESOURCE_NODE = BLOCKS.registerSimpleBlock("platinum_resource_node", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> URANIUM_RESOURCE_NODE = BLOCKS.registerSimpleBlock("uranium_resource_node", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));

    // region decorative
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> INDUSTRIAL_LIGHT = BLOCKS.registerBlock("industrial_light", props -> new WallMountedLight(props, 2), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE).noOcclusion());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> INDUSTRIAL_LIGHT_HANGING = BLOCKS.registerBlock("industrial_light_hanging", props -> new WallMountedLight(props, 12), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE).noOcclusion());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> INDUSTRIAL_BUTTON = BLOCKS.registerBlock("industrial_button", props -> new IndustrialRedstoneButton(BlockSetType.IRON, 80, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> INDUSTRIAL_LEVER = BLOCKS.registerBlock("industrial_lever", IndustrialLever::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> COPPER_REINFORCED_PLATING = BLOCKS.registerSimpleBlock("copper_reinforced_plating", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @NoAutoDrop
    public static final DeferredBlock<Block> COPPER_REINFORCED_PLATING_SLAB = BLOCKS.registerBlock("copper_reinforced_plating_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> COPPER_REINFORCED_PLATING_STAIRS = BLOCKS.registerBlock("copper_reinforced_plating_stairs", props -> new StairBlock(COPPER_REINFORCED_PLATING.value().defaultBlockState(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> COPPER_REINFORCED_PLATING_PRESSURE_PLATE = BLOCKS.registerBlock("copper_reinforced_plating_pressure_plate", props -> new PressurePlateBlock(BlockSetType.IRON, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> IRON_PLATING = BLOCKS.registerSimpleBlock("iron_plating", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @NoAutoDrop
    public static final DeferredBlock<Block> IRON_PLATING_SLAB = BLOCKS.registerBlock("iron_plating_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> IRON_PLATING_STAIRS = BLOCKS.registerBlock("iron_plating_stairs", props -> new StairBlock(IRON_PLATING.value().defaultBlockState(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> IRON_PLATING_PRESSURE_PLATE = BLOCKS.registerBlock("iron_plating_pressure_plate", props -> new PressurePlateBlock(BlockSetType.IRON, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> CARBON_PLATING = BLOCKS.registerSimpleBlock("carbon_plating", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @NoAutoDrop
    public static final DeferredBlock<Block> CARBON_PLATING_SLAB = BLOCKS.registerBlock("carbon_plating_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> CARBON_PLATING_STAIRS = BLOCKS.registerBlock("carbon_plating_stairs", props -> new StairBlock(CARBON_PLATING.value().defaultBlockState(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> CARBON_PLATING_PRESSURE_PLATE = BLOCKS.registerBlock("carbon_plating_pressure_plate", props -> new PressurePlateBlock(BlockSetType.IRON, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> NICKEL_PLATING = BLOCKS.registerSimpleBlock("nickel_plating", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @NoAutoDrop
    public static final DeferredBlock<Block> NICKEL_PLATING_SLAB = BLOCKS.registerBlock("nickel_plating_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> NICKEL_PLATING_STAIRS = BLOCKS.registerBlock("nickel_plating_stairs", props -> new StairBlock(NICKEL_PLATING.value().defaultBlockState(), props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> NICKEL_PLATING_PRESSURE_PLATE = BLOCKS.registerBlock("nickel_plating_pressure_plate", props -> new PressurePlateBlock(BlockSetType.IRON, props), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> INDUSTRIAL_SUPPORT_BEAM = BLOCKS.registerBlock("industrial_support_beam", IndustrialSupportBeamBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().forceSolidOn());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> INDUSTRIAL_SUPPORT_GIRDER = BLOCKS.registerBlock("industrial_support_girder", IndustrialSupportGirderBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().forceSolidOn());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> INDUSTRIAL_GLASS = BLOCKS.registerSimpleBlock("industrial_glass", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).requiresCorrectToolForDrops().strength(7.0F, 8.0F).noOcclusion());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @UseGeoBlockItem(scale = 0.5f)
    public static final DeferredBlock<Block> INDUSTRIAL_DOOR = BLOCKS.registerBlock("industrial_door", IndustrialDoorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(8f).forceSolidOn());
    @NoBlockItem
    @NoAutoDrop
    public static final DeferredBlock<Block> INDUSTRIAL_DOOR_HINGE = BLOCKS.registerBlock("industrial_door_hinge", IndustrialDoorBlockHinge::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(8f).forceSolidOn());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @UseGeoBlockItem(scale = 0.37f)
    public static final DeferredBlock<Block> HANGAR_DOOR = BLOCKS.registerBlock("hangar_door", HangarDoorBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(8f).forceSolidOn());
    @NoBlockItem
    @NoAutoDrop
    public static final DeferredBlock<Block> HANGAR_DOOR_HELPER = BLOCKS.registerBlock("hangar_door_helper", HangarDoorHelperBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(8f).forceSolidOn());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> STEEL = BLOCKS.registerSimpleBlock("steel", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> ENERGITE = BLOCKS.registerSimpleBlock("energite", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 6));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> NICKEL = BLOCKS.registerSimpleBlock("nickel", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> BIOSTEEL = BLOCKS.registerSimpleBlock("biosteel", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> PLATINUM = BLOCKS.registerSimpleBlock("platinum", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> ADAMANT = BLOCKS.registerSimpleBlock("adamant", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> ELECTRUM = BLOCKS.registerSimpleBlock("electrum", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> DURATIUM = BLOCKS.registerSimpleBlock("duratium", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    @Compostable(1.0f)
    public static final DeferredBlock<Block> BIOMASS = BLOCKS.registerSimpleBlock("biomass_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.MOSS));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> PLASTIC = BLOCKS.registerSimpleBlock("plastic", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.SHROOMLIGHT));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> FLUXITE = BLOCKS.registerSimpleBlock("fluxite_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> SILICON = BLOCKS.registerBlock("silicon_block", SlimeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SLIME_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RAW_NICKEL = BLOCKS.registerSimpleBlock("raw_nickel_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RAW_PLATINUM = BLOCKS.registerSimpleBlock("raw_platinum_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> RAW_URANIUM = BLOCKS.registerSimpleBlock("raw_uranium_block", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.DECORATIVE)
    public static final DeferredBlock<Block> URANIUM = BLOCKS.registerSimpleBlock("uranium", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 2));
    //endregion

    @SuppressWarnings("unchecked")
    public static void AddBlockItems() {

        RegistryReflectionUtil.IterateFields(
                BlockContent.class,
                DeferredBlock.class,
                (field, identifier, value) -> {

                    // Cast the raw generic to DeferredBlock<Block>
                    var deferredBlock = (DeferredBlock<Block>) value;

                    // Use return instead of continue inside a lambda
                    if (field.isAnnotationPresent(BlockContent.NoBlockItem.class)) {
                        return;
                    }

                    var fieldGroup = ItemContent.Groups.MACHINES;

                    if (field.isAnnotationPresent(ItemContent.ItemGroupTarget.class)) {
                        fieldGroup = field.getAnnotation(ItemContent.ItemGroupTarget.class).value();
                    }

                    var itemRarity = field.isAnnotationPresent(BlockContent.ItemRarity.class)
                            ? field.getAnnotation(BlockContent.ItemRarity.class).value()
                            : null;

                    DeferredItem<BlockItem> blockItem = BLOCK_ITEMS.registerItem(
                            deferredBlock.unwrapKey().orElseThrow().identifier().getPath(),
                            props -> {
                                if (field.isAnnotationPresent(BlockContent.UseGeoBlockItem.class)) {
                                    var geoItem = field.getAnnotation(BlockContent.UseGeoBlockItem.class);
                                    return new OritechGeoItem(deferredBlock.value(), props, geoItem.scale(), deferredBlock.getId().getPath(), geoItem.defaultColor());
                                }

                                return new BlockItem(deferredBlock.value(), props) {
                                    @Override
                                    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
                                        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

                                        if (deferredBlock.get() instanceof TooltipProvider tooltipProvider) {
                                            tooltipProvider.addToTooltip(context, builder, tooltipFlag, itemStack);
                                        }
                                    }
                                };
                            },
                            () -> {
                                var itemProperties = new BlockItem.Properties().useBlockDescriptionPrefix();

                                if (itemRarity != null) {
                                    itemProperties = itemProperties.rarity(itemRarity);
                                }

                                return itemProperties;
                            }
                    );
                    BLOCK_GROUPS.add(new Pair<>(blockItem, fieldGroup));
                }
        );
    }

    @SuppressWarnings("unchecked")
    public static void registerDispenserBehaviors() {
        RegistryReflectionUtil.IterateFields(
                BlockContent.class,
                DeferredBlock.class,
                (field, identifier, value) -> {
                    if (field.isAnnotationPresent(DispenserPlace.class)) {
                        DispenserBlock.registerBehavior(
                                ((DeferredBlock<Block>) value).value(),
                                new ShulkerBoxDispenseBehavior()
                        );
                    }
                }
        );
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface UseGeoBlockItem {
        float scale();

        ColorableMachine.ColorVariant defaultColor() default ColorableMachine.ColorVariant.ORANGE;
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

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface DispenserPlace {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface NoBlockItem {
    }

}

