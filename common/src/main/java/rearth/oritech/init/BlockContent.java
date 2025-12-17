package rearth.oritech.init;

import dev.architectury.registry.registries.RegistrySupplier;
import io.wispforest.owo.registration.reflect.BlockRegistryContainer.NoBlockItem;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
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
import rearth.oritech.block.blocks.interaction.ShrinkerBlock;
import rearth.oritech.block.blocks.storage.*;
import rearth.oritech.init.ItemContent.Compostable;
import rearth.oritech.item.OritechGeoItem;
import rearth.oritech.item.other.SmallEnergyStorageBlockItem;
import rearth.oritech.item.other.SmallFluidTankBlockItem;
import rearth.oritech.util.registry.ArchitecturyBlockRegistryContainer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class BlockContent implements ArchitecturyBlockRegistryContainer {
    
    public static Set<Block> autoRegisteredDrops = new HashSet<>();
    
    public static final Block SPAWNER_CAGE_BLOCK = new SpawnerCageBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    public static final Block MACHINE_FRAME_BLOCK = new MachineFrameBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS));
    
    public static final Block FLUID_PIPE = new FluidPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block FRAMED_FLUID_PIPE = new FluidPipeBlock.FramedFluidPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block FLUID_PIPE_DUCT_BLOCK = new FluidPipeDuctBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Block ENERGY_PIPE = new EnergyPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block FRAMED_ENERGY_PIPE = new EnergyPipeBlock.FramedEnergyPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block ENERGY_PIPE_DUCT_BLOCK = new EnergyPipeDuctBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Block SUPERCONDUCTOR = new SuperConductorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block FRAMED_SUPERCONDUCTOR = new SuperConductorBlock.FramedSuperConductorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block SUPERCONDUCTOR_DUCT_BLOCK = new SuperConductorDuctBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Block ITEM_PIPE = new ItemPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block TRANSPARENT_ITEM_PIPE = new ItemPipeBlock.TransparentItemPipe(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block FRAMED_ITEM_PIPE = new ItemPipeBlock.FramedItemPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block ITEM_PIPE_DUCT_BLOCK = new ItemPipeDuctBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Block ITEM_FILTER_BLOCK = new ItemFilterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    
    @NoBlockItem
    public static final Block FLUID_PIPE_CONNECTION = new FluidPipeConnectionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block FRAMED_FLUID_PIPE_CONNECTION = new FluidPipeConnectionBlock.FramedFluidPipeConnectionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block ENERGY_PIPE_CONNECTION = new EnergyPipeConnectionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block FRAMED_ENERGY_PIPE_CONNECTION = new EnergyPipeConnectionBlock.FramedEnergyPipeConnectionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block SUPERCONDUCTOR_CONNECTION = new SuperConductorConnectionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block FRAMED_SUPERCONDUCTOR_CONNECTION = new SuperConductorConnectionBlock.FramedSuperConductorConnectionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block ITEM_PIPE_CONNECTION = new ItemPipeConnectionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block FRAMED_ITEM_PIPE_CONNECTION = new ItemPipeConnectionBlock.FramedItemPipeConnectionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block TRANSPARENT_ITEM_PIPE_CONNECTION = new ItemPipeConnectionBlock.TransparentItemPipeConnectionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    
    public static final Block POWER_POLE_BLOCK = new PowerPoleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    @NoBlockItem
    public static final Block FRAME_GANTRY_ARM = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion());
    @NoBlockItem
    public static final Block BLOCK_DESTROYER_HEAD = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion());
    @NoBlockItem
    public static final Block BLOCK_PLACER_HEAD = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion());
    @NoBlockItem
    public static final Block BLOCK_FERTILIZER_HEAD = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion());
    @NoBlockItem
    public static final Block PUMP_TRUNK_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion());
    @NoBlockItem
    public static final Block TANK_ITEM_MODEL = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion());   // workaround because I don't understand how to properly get the model to load
    @NoBlockItem
    public static final Block CREATIVE_TANK_ITEM_MODEL = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion());   // workaround because I don't understand how to properly get the model to load
    @NoBlockItem
    public static final Block QUARRY_BEAM_INNER = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion().lightLevel(item -> 5));
    @NoBlockItem
    public static final Block QUARRY_BEAM_RING = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion().lightLevel(item -> 5));
    @NoBlockItem
    public static final Block QUARRY_BEAM_TARGET = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion());
    @NoBlockItem
    public static final Block BLACK_HOLE_INNER = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion().lightLevel(item -> 5));
    @NoBlockItem
    public static final Block BLACK_HOLE_MIDDLE = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion().lightLevel(item -> 5));
    @NoBlockItem
    public static final Block BLACK_HOLE_OUTER = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion().lightLevel(item -> 5));
    
    @NoBlockItem
    public static final Block ADDON_INDICATOR_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    @NoBlockItem
    public static final Block REACTOR_COLD_INDICATOR_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    @NoBlockItem
    public static final Block REACTOR_MEDIUM_INDICATOR_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    @NoBlockItem
    public static final Block REACTOR_HOT_INDICATOR_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block PULVERIZER_BLOCK = new PulverizerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block FRAGMENT_FORGE_BLOCK = new FragmentForge(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block ASSEMBLER_BLOCK = new AssemblerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block FOUNDRY_BLOCK = new FoundryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block COOLER_BLOCK = new CoolerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block CENTRIFUGE_BLOCK = new CentrifugeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    public static final Block ATOMIC_FORGE_BLOCK = new AtomicForgeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    public static final Block REFINERY_BLOCK = new RefineryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    public static final Block REFINERY_MODULE_BLOCK = new RefineryModuleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block BIO_GENERATOR_BLOCK = new BioGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block LAVA_GENERATOR_BLOCK = new LavaGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    public static final Block FUEL_GENERATOR_BLOCK = new FuelGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block BASIC_GENERATOR_BLOCK = new BasicGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block STEAM_ENGINE_BLOCK = new SteamEngineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block BIG_SOLAR_PANEL_BLOCK = new BigSolarPanelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), Oritech.CONFIG.generators.solarGeneratorData.energyPerTick());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block POWERED_FURNACE_BLOCK = new PoweredFurnaceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0));
    @UseGeoBlockItem(scale = 0.5f)
    public static final Block LASER_ARM_BLOCK = new LaserArmBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.25f)
    public static final Block DEEP_DRILL_BLOCK = new DeepDrillBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.3f)
    public static final Block DRONE_PORT_BLOCK = new DronePortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block SHRINKER_BLOCK = new ShrinkerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    @NoAutoDrop
    @DispenserPlace
    public static final Block SMALL_STORAGE_BLOCK = new SmallStorageBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final Block LARGE_STORAGE_BLOCK = new LargeStorageBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @DispenserPlace
    public static final Block CREATIVE_STORAGE_BLOCK = new CreativeStorageBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK).destroyTime(-1.0F));
    
    @NoAutoDrop
    @DispenserPlace
    public static final Block SMALL_TANK_BLOCK = new SmallFluidTank(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0));
    
    @NoAutoDrop
    @DispenserPlace
    public static final Block CREATIVE_TANK_BLOCK = new CreativeFluidTank(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0).destroyTime(-1.0F));
    
    public static final Item SMALL_TANK_ITEM = new SmallFluidTankBlockItem(SMALL_TANK_BLOCK, new Item.Properties());
    public static final Item CREATIVE_TANK_ITEM = new SmallFluidTankBlockItem(CREATIVE_TANK_BLOCK, new Item.Properties());
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block AUGMENT_APPLICATION_BLOCK = new AugmentApplicationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final Block SIMPLE_AUGMENT_STATION = new AugmentResearchStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2));
    public static final Block ADVANCED_AUGMENT_STATION = new AugmentResearchStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2));
    public static final Block ARCANE_AUGMENT_STATION = new AugmentResearchStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2));
    
    public static final Block PLACER_BLOCK = new PlacerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final Block DESTROYER_BLOCK = new DestroyerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final Block FERTILIZER_BLOCK = new FertilizerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block TREEFELLER_BLOCK = new TreefellerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block PIPE_BOOSTER_BLOCK = new PipeBoosterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block ENCHANTMENT_CATALYST_BLOCK = new EnchantmentCatalystBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block ENCHANTER_BLOCK = new EnchanterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final Block SPAWNER_CONTROLLER_BLOCK = new SpawnerControllerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    @NoAutoDrop
    public static final Block WITHER_CROP_BLOCK = new WitheredCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    
    @NoBlockItem
    public static final Block UNSTABLE_CONTAINER = new UnstableContainerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(80, 1900f).noOcclusion().forceSolidOn());
    
    public static final Block ACCELERATOR_RING = new AcceleratorRingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final Block ACCELERATOR_MOTOR = new AcceleratorMotorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 5));
    public static final Block ACCELERATOR_CONTROLLER = new AcceleratorControllerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final Block ACCELERATOR_SENSOR = new AcceleratorSensorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final Block BLACK_HOLE_BLOCK = new BlackHoleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.END_PORTAL).lightLevel(item -> 12).noOcclusion().forceSolidOn());
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block PARTICLE_COLLECTOR_BLOCK = new ParticleCollectorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion());
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block PUMP_BLOCK = new PumpBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final Block CHARGER_BLOCK = new ChargerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    public static final Block MACHINE_CORE_1 = new MachineCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), 1);
    public static final Block MACHINE_CORE_2 = new MachineCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), 2);
    public static final Block MACHINE_CORE_3 = new MachineCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), 3);
    public static final Block MACHINE_CORE_4 = new MachineCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), 4);
    public static final Block MACHINE_CORE_5 = new MachineCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), 5);
    public static final Block MACHINE_CORE_6 = new MachineCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), 6);
    public static final Block MACHINE_CORE_7 = new MachineCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), 7);
    @NoBlockItem
    public static final Block MACHINE_CORE_HIDDEN = new MachineCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(80, 1900f).noOcclusion().forceSolidOn(), 1);
    
    public static final Block MACHINE_SPEED_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withSpeedMultiplier(Oritech.CONFIG.addonConfig.speedAddonSpeed()).withEfficiencyMultiplier(Oritech.CONFIG.addonConfig.speedAddonEfficiency()).withBoundingShape(MachineAddonBlock.MACHINE_SPEED_ADDON_SHAPE));
    public static final Block MACHINE_EFFICIENCY_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withEfficiencyMultiplier(Oritech.CONFIG.addonConfig.efficiencyAddonEfficiency()).withBoundingShape(MachineAddonBlock.MACHINE_EFFICIENCY_ADDON_SHAPE));
    public static final Block MACHINE_ULTIMATE_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withSpeedMultiplier(Oritech.CONFIG.addonConfig.ultimateAddonSpeed()).withEfficiencyMultiplier(Oritech.CONFIG.addonConfig.ultimateAddonEfficiency()).withBoundingShape(MachineAddonBlock.MACHINE_ULTIMATE_ADDON_SHAPE));
    public static final Block QUARRY_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.QUARRY_ADDON_SHAPE));
    public static final Block MACHINE_PROCESSING_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withEfficiencyMultiplier(Oritech.CONFIG.addonConfig.chamberAddonEfficiency()).withChambers(1).withBoundingShape(MachineAddonBlock.MACHINE_PROCESSING_ADDON_SHAPE));
    public static final Block MACHINE_FLUID_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_FLUID_ADDON_SHAPE));
    public static final Block MACHINE_YIELD_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_YIELD_ADDON_SHAPE));
    public static final Block CROP_FILTER_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.CROP_FILTER_ADDON_SHAPE));
    public static final Block MACHINE_HUNTER_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_HUNTER_ADDON_SHAPE));
    public static final Block MACHINE_CAPACITOR_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withAddedCapacity(2_000_000).withAddedInsert(2_000).withBoundingShape(MachineAddonBlock.MACHINE_CAPACITOR_ADDON_SHAPE));
    public static final Block MACHINE_ACCEPTOR_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withAddedCapacity(500_000).withAddedInsert(5_000).withAcceptEnergy(true).withBoundingShape(MachineAddonBlock.MACHINE_ACCEPTOR_ADDON_SHAPE));
    public static final Block MACHINE_INVENTORY_PROXY_ADDON = new InventoryProxyAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_INVENTORY_PROXY_ADDON_SHAPE));
    public static final Block MACHINE_EXTENDER = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false));
    public static final Block CAPACITOR_ADDON_EXTENDER = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false).withAddedCapacity(2_500_000).withAddedInsert(1_000));
    public static final Block STEAM_BOILER_ADDON = new SteamBoilerAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.STEAM_BOILER_ADDON_SHAPE));
    public static final Block MACHINE_REDSTONE_ADDON = new RedstoneAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_REDSTONE_ADDON_SHAPE));
    public static final Block MACHINE_SILK_TOUCH_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_SILK_TOUCH_ADDON_SHAPE));
    public static final Block MACHINE_BURST_ADDON = new MachineAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBurstTicks(Oritech.CONFIG.addonConfig.burstAddonTicks()).withBoundingShape(MachineAddonBlock.MACHINE_BURST_ADDON_SHAPE)); // todo config settings
    
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.none)
    public static final Block MACHINE_COMBI_ADDON = new CombiAddonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_COMBI_ADDON_SHAPE));
    
    //region reactor
    public static final Block REACTOR_CONTROLLER = new ReactorControllerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 5));
    public static final Block REACTOR_WALL = new ReactorWallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).strength(10, 1800));
    public static final Block REACTOR_ROD = new ReactorRodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3), 1, 1);
    public static final Block REACTOR_DOUBLE_ROD = new ReactorRodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3), 2, 4);
    public static final Block REACTOR_QUAD_ROD = new ReactorRodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3), 4, 12);
    public static final Block REACTOR_VENT = new ReactorHeatVentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final Block REACTOR_REFLECTOR = new ReactorReflectorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> 15));
    public static final Block REACTOR_HEAT_PIPE = new ReactorHeatPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final Block REACTOR_CONDENSER = new ReactorAbsorberBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Block REACTOR_FUEL_PORT = new ReactorFuelPortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Block REACTOR_ABSORBER_PORT = new ReactorAbsorberPortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Block REACTOR_ENERGY_PORT = new ReactorEnergyPortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Block REACTOR_REDSTONE_PORT = new ReactorRedstonePortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block URANIUM_CRYSTAL = new AmethystClusterBlock(7, 3, BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel(state -> 5));
    
    @NoBlockItem
    public static final Block REACTOR_EXPLOSION_SMALL = new NuclearExplosionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), 9);
    @NoBlockItem
    public static final Block REACTOR_EXPLOSION_MEDIUM = new NuclearExplosionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), 14);
    @NoBlockItem
    public static final Block REACTOR_EXPLOSION_LARGE = new NuclearExplosionBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), 20);
    public static final Block LOW_YIELD_NUKE = new NukeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), true);
    public static final Block NUKE = new NukeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), false);
    
    // cooling cell, early game re-fillable component
    
    // lategame, second stage components:
    // plasma conduit, advanced heat transfer system
    // entropy dampener, reduce degradation rate of nearby components
    // quantum stabilizer, massively increase heat capacity of reactor
    //endregion
    
    //region metals
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_ORE = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block DEEPSLATE_NICKEL_ORE = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block ENDSTONE_PLATINUM_ORE = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block DEEPSLATE_PLATINUM_ORE = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block DEEPSLATE_URANIUM_ORE = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE));
    //endregion
    
    //region resource nodes
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_REDSTONE = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_LAPIS = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_IRON = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_COAL = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_GOLD = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_EMERALD = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_DIAMOND = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_COPPER = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_NICKEL = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_PLATINUM = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_URANIUM = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK));
    
    // region decorative
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block CEILING_LIGHT = new WallMountedLight(BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE).noOcclusion(), 2);
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block CEILING_LIGHT_HANGING = new WallMountedLight(BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE).noOcclusion(), 12);
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block TECH_BUTTON = new TechRedstoneButton(BlockSetType.IRON, 80, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block TECH_LEVER = new TechLever(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block MACHINE_PLATING_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block MACHINE_PLATING_SLAB = new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block MACHINE_PLATING_STAIRS = new StairBlock(MACHINE_PLATING_BLOCK.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block MACHINE_PLATING_PRESSURE_PLATE = new PressurePlateBlock(BlockSetType.IRON, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block IRON_PLATING_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block IRON_PLATING_SLAB = new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block IRON_PLATING_STAIRS = new StairBlock(IRON_PLATING_BLOCK.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block IRON_PLATING_PRESSURE_PLATE = new PressurePlateBlock(BlockSetType.IRON, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block CARBON_PLATING_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block CARBON_PLATING_SLAB = new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block CARBON_PLATING_STAIRS = new StairBlock(CARBON_PLATING_BLOCK.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block CARBON_PLATING_PRESSURE_PLATE = new PressurePlateBlock(BlockSetType.IRON, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_PLATING_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_PLATING_SLAB = new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_PLATING_STAIRS = new StairBlock(NICKEL_PLATING_BLOCK.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_PLATING_PRESSURE_PLATE = new PressurePlateBlock(BlockSetType.IRON, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block METAL_BEAM_BLOCK = new MetalBeamBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().forceSolidOn());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative) // todo recipe
    public static final Block METAL_GIRDER_BLOCK = new MetalGirderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().forceSolidOn());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block INDUSTRIAL_GLASS_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).requiresCorrectToolForDrops().strength(7.0F, 8.0F).noOcclusion());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @UseGeoBlockItem(scale = 0.5f)
    public static final Block TECH_DOOR = new TechDoorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(8f).forceSolidOn());
    @NoBlockItem
    public static final Block TECH_DOOR_HINGE = new TechDoorBlockHinge(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(8f).forceSolidOn());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block STEEL_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block ENERGITE_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 6));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block BIOSTEEL_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block PLATINUM_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block ADAMANT_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block ELECTRUM_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block DURATIUM_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @Compostable(1.0f)
    public static final Block BIOMASS_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.MOSS));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block PLASTIC_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.SHROOMLIGHT));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block FLUXITE_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block SILICON_BLOCK = new SlimeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SLIME_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RAW_NICKEL_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RAW_PLATINUM_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RAW_URANIUM_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block URANIUM_DUST_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 2));
    //endregion
    
    @Override
    public void postProcessField(String namespace, Block value, String identifier, Field field, RegistrySupplier<Block> supplier) {
        
        if (field.isAnnotationPresent(NoBlockItem.class)) return;
        
        var targetGroup = ItemContent.Groups.machines;
        if (field.isAnnotationPresent(ItemContent.ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemContent.ItemGroupTarget.class).value();
        }
        
        if (field.isAnnotationPresent(UseGeoBlockItem.class)) {
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, identifier), getGeoBlockItem(value, identifier, field.getAnnotation(UseGeoBlockItem.class).scale()));
        } else if (FluidApi.ITEM != null && (value instanceof SmallFluidTank)) {
            var item = value.equals(BlockContent.SMALL_TANK_BLOCK) ? SMALL_TANK_ITEM : CREATIVE_TANK_ITEM;
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, identifier), item);
            FluidApi.ITEM.registerForItem(() -> item);
        } else if (value.equals(BlockContent.SMALL_STORAGE_BLOCK) && EnergyApi.ITEM != null) {
            var item = new SmallEnergyStorageBlockItem(value, new Item.Properties().component(EnergyApi.ITEM.getEnergyComponent(), 0L));
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, identifier), item);
            EnergyApi.ITEM.registerForItem(() -> item);
            
            var variantStack = new ItemStack(item);
            variantStack.set(EnergyApi.ITEM.getEnergyComponent(), Oritech.CONFIG.smallEnergyStorage.energyCapacity());
            ItemGroups.add(targetGroup, variantStack);
            
        } else {
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, identifier), createBlockItem(value, identifier));
        }
        
        if (!field.isAnnotationPresent(NoAutoDrop.class)) {
            autoRegisteredDrops.add(value);
        }
        
        if (field.isAnnotationPresent(DispenserPlace.class)) {
            DispenserBlock.registerBehavior(value, new ShulkerBoxDispenseBehavior());
        }

        if (field.isAnnotationPresent(Compostable.class)) {
            ComposterBlock.add(field.getAnnotation(Compostable.class).value(), value.asItem());
        }
        
        ItemGroups.add(targetGroup, value);
    }
    
    private BlockItem getGeoBlockItem(Block block, String identifier, float scale) {
        return new OritechGeoItem(block, new Item.Properties(), scale, identifier);
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface UseGeoBlockItem {
        float scale(); // scale
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
