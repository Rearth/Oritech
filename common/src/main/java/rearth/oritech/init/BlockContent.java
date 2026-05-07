package rearth.oritech.init;

import dev.architectury.registry.registries.RegistrySupplier;
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
import oshi.util.tuples.Pair;
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
import rearth.oritech.block.blocks.storage.*;
import rearth.oritech.init.ItemContent.Compostable;
import rearth.oritech.item.OritechGeoItem;
import rearth.oritech.item.other.SmallEnergyStorageBlockItem;
import rearth.oritech.item.other.SmallFluidTankBlockItem;
import rearth.oritech.util.registry.NoBlockItem;
import rearth.oritech.util.registry.OritechBlockRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class BlockContent {
    
    public static final OritechBlockRegistry BLOCKS = new OritechBlockRegistry();
    private static final Map<RegistrySupplier<? extends Block>, Block> BLOCK_VALUES = new IdentityHashMap<>();
    private static boolean loaded;
    
    public static Set<Block> autoRegisteredDrops = new HashSet<>();

    public static void load() {
        if (loaded) return;

        loaded = true;
        for (var field : BlockContent.class.getDeclaredFields()) {
            if (!RegistrySupplier.class.isAssignableFrom(field.getType())) continue;

            try {
                field.setAccessible(true);
                var supplier = (RegistrySupplier<? extends Block>) field.get(null);
                var block = BLOCK_VALUES.get(supplier);
                if (block == null) continue;

                postProcessField(block, supplier.getId(), field, supplier);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access block field: " + field.getName(), e);
            }
        }
    }

    public static void registerBlocks() {
        BLOCKS.register();
        load();
    }

    public static <T extends Block> RegistrySupplier<T> registerBlock(String path, T block) {
        var supplier = BLOCKS.register(path, () -> block);
        BLOCK_VALUES.put(supplier, block);
        return supplier;
    }

    public static Block value(RegistrySupplier<? extends Block> supplier) {
        return BLOCK_VALUES.get(supplier);
    }
    
    public static final RegistrySupplier<Block> SPAWNER_CAGE_BLOCK = registerBlock("spawner_cage_block", new SpawnerCageBlock(blockProperties("spawner_cage_block", Blocks.IRON_BLOCK).noOcclusion()));
    
    public static final RegistrySupplier<Block> MACHINE_FRAME_BLOCK = registerBlock("machine_frame_block", new MachineFrameBlock(blockProperties("machine_frame_block", Blocks.IRON_BARS)));
    
    public static final RegistrySupplier<Block> FLUID_PIPE = registerBlock("fluid_pipe", new FluidPipeBlock(blockProperties("fluid_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final RegistrySupplier<Block> FRAMED_FLUID_PIPE = registerBlock("framed_fluid_pipe", new FluidPipeBlock.FramedFluidPipeBlock(blockProperties("framed_fluid_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final RegistrySupplier<Block> FLUID_PIPE_DUCT_BLOCK = registerBlock("fluid_pipe_duct_block", new FluidPipeDuctBlock(blockProperties("fluid_pipe_duct_block", Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> ENERGY_PIPE = registerBlock("energy_pipe", new EnergyPipeBlock(blockProperties("energy_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final RegistrySupplier<Block> FRAMED_ENERGY_PIPE = registerBlock("framed_energy_pipe", new EnergyPipeBlock.FramedEnergyPipeBlock(blockProperties("framed_energy_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final RegistrySupplier<Block> ENERGY_PIPE_DUCT_BLOCK = registerBlock("energy_pipe_duct_block", new EnergyPipeDuctBlock(blockProperties("energy_pipe_duct_block", Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> SUPERCONDUCTOR = registerBlock("superconductor", new SuperConductorBlock(blockProperties("superconductor", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final RegistrySupplier<Block> FRAMED_SUPERCONDUCTOR = registerBlock("framed_superconductor", new SuperConductorBlock.FramedSuperConductorBlock(blockProperties("framed_superconductor", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final RegistrySupplier<Block> SUPERCONDUCTOR_DUCT_BLOCK = registerBlock("superconductor_duct_block", new SuperConductorDuctBlock(blockProperties("superconductor_duct_block", Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> ITEM_PIPE = registerBlock("item_pipe", new ItemPipeBlock(blockProperties("item_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final RegistrySupplier<Block> TRANSPARENT_ITEM_PIPE = registerBlock("transparent_item_pipe", new ItemPipeBlock.TransparentItemPipe(blockProperties("transparent_item_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final RegistrySupplier<Block> FRAMED_ITEM_PIPE = registerBlock("framed_item_pipe", new ItemPipeBlock.FramedItemPipeBlock(blockProperties("framed_item_pipe", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    public static final RegistrySupplier<Block> ITEM_PIPE_DUCT_BLOCK = registerBlock("item_pipe_duct_block", new ItemPipeDuctBlock(blockProperties("item_pipe_duct_block", Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> ITEM_FILTER_BLOCK = registerBlock("item_filter_block", new ItemFilterBlock(blockProperties("item_filter_block", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    
    @NoBlockItem
    public static final RegistrySupplier<Block> FLUID_PIPE_CONNECTION = registerBlock("fluid_pipe_connection", new FluidPipeConnectionBlock(blockProperties("fluid_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final RegistrySupplier<Block> FRAMED_FLUID_PIPE_CONNECTION = registerBlock("framed_fluid_pipe_connection", new FluidPipeConnectionBlock.FramedFluidPipeConnectionBlock(blockProperties("framed_fluid_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final RegistrySupplier<Block> ENERGY_PIPE_CONNECTION = registerBlock("energy_pipe_connection", new EnergyPipeConnectionBlock(blockProperties("energy_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final RegistrySupplier<Block> FRAMED_ENERGY_PIPE_CONNECTION = registerBlock("framed_energy_pipe_connection", new EnergyPipeConnectionBlock.FramedEnergyPipeConnectionBlock(blockProperties("framed_energy_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final RegistrySupplier<Block> SUPERCONDUCTOR_CONNECTION = registerBlock("superconductor_connection", new SuperConductorConnectionBlock(blockProperties("superconductor_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final RegistrySupplier<Block> FRAMED_SUPERCONDUCTOR_CONNECTION = registerBlock("framed_superconductor_connection", new SuperConductorConnectionBlock.FramedSuperConductorConnectionBlock(blockProperties("framed_superconductor_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final RegistrySupplier<Block> ITEM_PIPE_CONNECTION = registerBlock("item_pipe_connection", new ItemPipeConnectionBlock(blockProperties("item_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final RegistrySupplier<Block> FRAMED_ITEM_PIPE_CONNECTION = registerBlock("framed_item_pipe_connection", new ItemPipeConnectionBlock.FramedItemPipeConnectionBlock(blockProperties("framed_item_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    @NoBlockItem
    public static final RegistrySupplier<Block> TRANSPARENT_ITEM_PIPE_CONNECTION = registerBlock("transparent_item_pipe_connection", new ItemPipeConnectionBlock.TransparentItemPipeConnectionBlock(blockProperties("transparent_item_pipe_connection", Blocks.IRON_BARS).strength(1.0f, 2.0f)));
    
    public static final RegistrySupplier<Block> POWER_POLE_BLOCK = registerBlock("power_pole_block", new PowerPoleBlock(blockProperties("power_pole_block", Blocks.IRON_BLOCK).noOcclusion()));
    
    @NoBlockItem
    public static final RegistrySupplier<Block> FRAME_GANTRY_ARM = registerBlock("frame_gantry_arm", new Block(blockProperties("frame_gantry_arm", Blocks.CHAIN).noOcclusion()));
    @NoBlockItem
    public static final RegistrySupplier<Block> BLOCK_DESTROYER_HEAD = registerBlock("block_destroyer_head", new Block(blockProperties("block_destroyer_head", Blocks.CHAIN).noOcclusion()));
    @NoBlockItem
    public static final RegistrySupplier<Block> BLOCK_PLACER_HEAD = registerBlock("block_placer_head", new Block(blockProperties("block_placer_head", Blocks.CHAIN).noOcclusion()));
    @NoBlockItem
    public static final RegistrySupplier<Block> BLOCK_FERTILIZER_HEAD = registerBlock("block_fertilizer_head", new Block(blockProperties("block_fertilizer_head", Blocks.CHAIN).noOcclusion()));
    @NoBlockItem
    public static final RegistrySupplier<Block> PUMP_TRUNK_BLOCK = registerBlock("pump_trunk_block", new Block(blockProperties("pump_trunk_block", Blocks.CHAIN).noOcclusion()));
    @NoBlockItem
    public static final RegistrySupplier<Block> TANK_ITEM_MODEL = registerBlock("tank_item_model", new Block(blockProperties("tank_item_model", Blocks.CHAIN).noOcclusion()));   // workaround because I don't understand how to properly get the model to load
    @NoBlockItem
    public static final RegistrySupplier<Block> CREATIVE_TANK_ITEM_MODEL = registerBlock("creative_tank_item_model", new Block(blockProperties("creative_tank_item_model", Blocks.CHAIN).noOcclusion()));   // workaround because I don't understand how to properly get the model to load
    @NoBlockItem
    public static final RegistrySupplier<Block> QUARRY_BEAM_RING = registerBlock("quarry_beam_ring", new Block(blockProperties("quarry_beam_ring", Blocks.CHAIN).noOcclusion().lightLevel(item -> 5)));
    @NoBlockItem
    public static final RegistrySupplier<Block> BLACK_HOLE_INNER = registerBlock("black_hole_inner", new Block(blockProperties("black_hole_inner", Blocks.CHAIN).noOcclusion().lightLevel(item -> 5)));
    @NoBlockItem
    public static final RegistrySupplier<Block> BLACK_HOLE_MIDDLE = registerBlock("black_hole_middle", new Block(blockProperties("black_hole_middle", Blocks.CHAIN).noOcclusion().lightLevel(item -> 5)));
    @NoBlockItem
    public static final RegistrySupplier<Block> BLACK_HOLE_OUTER = registerBlock("black_hole_outer", new Block(blockProperties("black_hole_outer", Blocks.CHAIN).noOcclusion().lightLevel(item -> 5)));
    
    @NoBlockItem
    public static final RegistrySupplier<Block> ADDON_INDICATOR_BLOCK = registerBlock("addon_indicator_block", new Block(blockProperties("addon_indicator_block", Blocks.GLASS)));
    @NoBlockItem
    public static final RegistrySupplier<Block> REACTOR_COLD_INDICATOR_BLOCK = registerBlock("reactor_cold_indicator_block", new Block(blockProperties("reactor_cold_indicator_block", Blocks.GLASS)));
    @NoBlockItem
    public static final RegistrySupplier<Block> REACTOR_MEDIUM_INDICATOR_BLOCK = registerBlock("reactor_medium_indicator_block", new Block(blockProperties("reactor_medium_indicator_block", Blocks.GLASS)));
    @NoBlockItem
    public static final RegistrySupplier<Block> REACTOR_HOT_INDICATOR_BLOCK = registerBlock("reactor_hot_indicator_block", new Block(blockProperties("reactor_hot_indicator_block", Blocks.GLASS)));
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> PULVERIZER_BLOCK = registerBlock("pulverizer_block", new PulverizerBlock(blockProperties("pulverizer_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> FRAGMENT_FORGE_BLOCK = registerBlock("fragment_forge_block", new FragmentForge(blockProperties("fragment_forge_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> ASSEMBLER_BLOCK = registerBlock("assembler_block", new AssemblerBlock(blockProperties("assembler_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> FOUNDRY_BLOCK = registerBlock("foundry_block", new FoundryBlock(blockProperties("foundry_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> COOLER_BLOCK = registerBlock("cooler_block", new CoolerBlock(blockProperties("cooler_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> CENTRIFUGE_BLOCK = registerBlock("centrifuge_block", new CentrifugeBlock(blockProperties("centrifuge_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    @ItemRarity(ItemRarityValue.RARE)
    public static final RegistrySupplier<Block> ATOMIC_FORGE_BLOCK = registerBlock("atomic_forge_block", new AtomicForgeBlock(blockProperties("atomic_forge_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    public static final RegistrySupplier<Block> REFINERY_BLOCK = registerBlock("refinery_block", new RefineryBlock(blockProperties("refinery_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    @NoAutoDrop
    public static final RegistrySupplier<Block> TAINTED_REFINERY_BLOCK = registerBlock("tainted_refinery_block", new TaintedRefineryBlock(blockProperties("tainted_refinery_block", Blocks.IRON_BLOCK).strength(7f, 2000f).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    public static final RegistrySupplier<Block> REFINERY_MODULE_BLOCK = registerBlock("refinery_module_block", new RefineryModuleBlock(blockProperties("refinery_module_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> BIO_GENERATOR_BLOCK = registerBlock("bio_generator_block", new BioGeneratorBlock(blockProperties("bio_generator_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> LAVA_GENERATOR_BLOCK = registerBlock("lava_generator_block", new LavaGeneratorBlock(blockProperties("lava_generator_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    public static final RegistrySupplier<Block> FUEL_GENERATOR_BLOCK = registerBlock("fuel_generator_block", new FuelGeneratorBlock(blockProperties("fuel_generator_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> BASIC_GENERATOR_BLOCK = registerBlock("basic_generator_block", new BasicGeneratorBlock(blockProperties("basic_generator_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> STEAM_ENGINE_BLOCK = registerBlock("steam_engine_block", new SteamEngineBlock(blockProperties("steam_engine_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> BIG_SOLAR_PANEL_BLOCK = registerBlock("big_solar_panel_block", new BigSolarPanelBlock(blockProperties("big_solar_panel_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> POWERED_FURNACE_BLOCK = registerBlock("powered_furnace_block", new PoweredFurnaceBlock(blockProperties("powered_furnace_block", Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0)));
    @UseGeoBlockItem(scale = 0.5f)
    public static final RegistrySupplier<Block> LASER_ARM_BLOCK = registerBlock("laser_arm_block", new LaserArmBlock(blockProperties("laser_arm_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.25f)
    public static final RegistrySupplier<Block> DEEP_DRILL_BLOCK = registerBlock("deep_drill_block", new DeepDrillBlock(blockProperties("deep_drill_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.3f)
    public static final RegistrySupplier<Block> DRONE_PORT_BLOCK = registerBlock("drone_port_block", new DronePortBlock(blockProperties("drone_port_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> SHRINKER_BLOCK = registerBlock("shrinker_block", new ShrinkerBlock(blockProperties("shrinker_block", Blocks.IRON_BLOCK).noOcclusion()));
    
    @NoAutoDrop
    @DispenserPlace
    public static final RegistrySupplier<Block> SMALL_STORAGE_BLOCK = registerBlock("small_storage_block", new SmallStorageBlock(blockProperties("small_storage_block", Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final RegistrySupplier<Block> LARGE_STORAGE_BLOCK = registerBlock("large_storage_block", new LargeStorageBlock(blockProperties("large_storage_block", Blocks.IRON_BLOCK).noOcclusion()));
    @DispenserPlace
    public static final RegistrySupplier<Block> CREATIVE_STORAGE_BLOCK = registerBlock("creative_storage_block", new CreativeStorageBlock(blockProperties("creative_storage_block", Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK).destroyTime(-1.0F)));
    
    @NoAutoDrop
    @DispenserPlace
    public static final RegistrySupplier<Block> SMALL_TANK_BLOCK = registerBlock("small_tank_block", new SmallFluidTank(blockProperties("small_tank_block", Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0)));
    
    @NoAutoDrop
    @DispenserPlace
    public static final RegistrySupplier<Block> CREATIVE_TANK_BLOCK = registerBlock("creative_tank_block", new CreativeFluidTank(blockProperties("creative_tank_block", Blocks.IRON_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0).destroyTime(-1.0F)));
    
    public static final RegistrySupplier<Item> SMALL_TANK_ITEM = ItemContent.registerItem("small_tank_block", new SmallFluidTankBlockItem(blockValue(SMALL_TANK_BLOCK), blockItemProperties("small_tank_block")));
    public static final RegistrySupplier<Item> CREATIVE_TANK_ITEM = ItemContent.registerItem("creative_tank_block", new SmallFluidTankBlockItem(blockValue(CREATIVE_TANK_BLOCK), blockItemProperties("creative_tank_block")));
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> AUGMENT_APPLICATION_BLOCK = registerBlock("augment_application_block", new AugmentApplicationBlock(blockProperties("augment_application_block", Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> SIMPLE_AUGMENT_STATION = registerBlock("simple_augment_station", new AugmentResearchStationBlock(blockProperties("simple_augment_station", Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2)));
    public static final RegistrySupplier<Block> ADVANCED_AUGMENT_STATION = registerBlock("advanced_augment_station", new AugmentResearchStationBlock(blockProperties("advanced_augment_station", Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2)));
    public static final RegistrySupplier<Block> ARCANE_AUGMENT_STATION = registerBlock("arcane_augment_station", new AugmentResearchStationBlock(blockProperties("arcane_augment_station", Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 2)));
    
    public static final RegistrySupplier<Block> PLACER_BLOCK = registerBlock("placer_block", new PlacerBlock(blockProperties("placer_block", Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> DESTROYER_BLOCK = registerBlock("destroyer_block", new DestroyerBlock(blockProperties("destroyer_block", Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> FERTILIZER_BLOCK = registerBlock("fertilizer_block", new FertilizerBlock(blockProperties("fertilizer_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> TREEFELLER_BLOCK = registerBlock("treefeller_block", new TreefellerBlock(blockProperties("treefeller_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> PIPE_BOOSTER_BLOCK = registerBlock("pipe_booster_block", new PipeBoosterBlock(blockProperties("pipe_booster_block", Blocks.IRON_BLOCK).noOcclusion()));
    
    @UseGeoBlockItem(scale = 0.7f)
    @ItemRarity(ItemRarityValue.RARE)
    public static final RegistrySupplier<Block> ENCHANTMENT_CATALYST_BLOCK = registerBlock("enchantment_catalyst_block", new EnchantmentCatalystBlock(blockProperties("enchantment_catalyst_block", Blocks.IRON_BLOCK).noOcclusion()));
    @UseGeoBlockItem(scale = 0.7f)
    @ItemRarity(ItemRarityValue.RARE)
    public static final RegistrySupplier<Block> ENCHANTER_BLOCK = registerBlock("enchanter_block", new EnchanterBlock(blockProperties("enchanter_block", Blocks.IRON_BLOCK).noOcclusion()));
    @ItemRarity(ItemRarityValue.RARE)
    public static final RegistrySupplier<Block> SPAWNER_CONTROLLER_BLOCK = registerBlock("spawner_controller_block", new SpawnerControllerBlock(blockProperties("spawner_controller_block", Blocks.IRON_BLOCK).noOcclusion()));
    @NoAutoDrop
    public static final RegistrySupplier<Block> WITHER_CROP_BLOCK = registerBlock("wither_crop_block", new WitheredCropBlock(blockProperties("wither_crop_block", Blocks.WHEAT)));
    
    @NoBlockItem
    public static final RegistrySupplier<Block> UNSTABLE_CONTAINER = registerBlock("unstable_container", new UnstableContainerBlock(blockProperties("unstable_container", Blocks.OBSIDIAN).strength(80, 1900f).noOcclusion().forceSolidOn()));
    
    public static final RegistrySupplier<Block> ACCELERATOR_RING = registerBlock("accelerator_ring", new AcceleratorRingBlock(blockProperties("accelerator_ring", Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> ACCELERATOR_MOTOR = registerBlock("accelerator_motor", new AcceleratorMotorBlock(blockProperties("accelerator_motor", Blocks.IRON_BLOCK).noOcclusion().lightLevel(item -> 5)));
    public static final RegistrySupplier<Block> ACCELERATOR_CONTROLLER = registerBlock("accelerator_controller", new AcceleratorControllerBlock(blockProperties("accelerator_controller", Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> ACCELERATOR_SENSOR = registerBlock("accelerator_sensor", new AcceleratorSensorBlock(blockProperties("accelerator_sensor", Blocks.IRON_BLOCK).noOcclusion()));
    @ItemRarity(ItemRarityValue.EPIC)
    public static final RegistrySupplier<Block> BLACK_HOLE_BLOCK = registerBlock("black_hole_block", new BlackHoleBlock(blockProperties("black_hole_block", Blocks.END_PORTAL).lightLevel(item -> 12).noOcclusion().forceSolidOn()));
    
    public static final RegistrySupplier<Block> PARTICLE_COLLECTOR_BLOCK = registerBlock("particle_collector_block", new ParticleCollectorBlock(blockProperties("particle_collector_block", Blocks.GLASS).noOcclusion()));
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final RegistrySupplier<Block> PUMP_BLOCK = registerBlock("pump_block", new PumpBlock(blockProperties("pump_block", Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> CHARGER_BLOCK = registerBlock("charger_block", new ChargerBlock(blockProperties("charger_block", Blocks.IRON_BLOCK).noOcclusion()));
    
    public static final RegistrySupplier<Block> MACHINE_CORE_1 = registerBlock("machine_core_1", new MachineCoreBlock(blockProperties("machine_core_1", Blocks.IRON_BLOCK).noOcclusion(), 1));
    public static final RegistrySupplier<Block> MACHINE_CORE_2 = registerBlock("machine_core_2", new MachineCoreBlock(blockProperties("machine_core_2", Blocks.IRON_BLOCK).noOcclusion(), 2));
    public static final RegistrySupplier<Block> MACHINE_CORE_3 = registerBlock("machine_core_3", new MachineCoreBlock(blockProperties("machine_core_3", Blocks.IRON_BLOCK).noOcclusion(), 3));
    public static final RegistrySupplier<Block> MACHINE_CORE_4 = registerBlock("machine_core_4", new MachineCoreBlock(blockProperties("machine_core_4", Blocks.IRON_BLOCK).noOcclusion(), 4));
    public static final RegistrySupplier<Block> MACHINE_CORE_5 = registerBlock("machine_core_5", new MachineCoreBlock(blockProperties("machine_core_5", Blocks.IRON_BLOCK).noOcclusion(), 5));
    public static final RegistrySupplier<Block> MACHINE_CORE_6 = registerBlock("machine_core_6", new MachineCoreBlock(blockProperties("machine_core_6", Blocks.IRON_BLOCK).noOcclusion(), 6));
    public static final RegistrySupplier<Block> MACHINE_CORE_7 = registerBlock("machine_core_7", new MachineCoreBlock(blockProperties("machine_core_7", Blocks.IRON_BLOCK).noOcclusion(), 7));
    @NoBlockItem
    public static final RegistrySupplier<Block> MACHINE_CORE_HIDDEN = registerBlock("machine_core_hidden", new MachineCoreBlock(blockProperties("machine_core_hidden", Blocks.OBSIDIAN).strength(80, 1900f).noOcclusion().forceSolidOn(), 1));
    
    public static final RegistrySupplier<Block> MACHINE_SPEED_ADDON = registerBlock("machine_speed_addon", new MachineAddonBlock(blockProperties("machine_speed_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withSpeedMultiplier(OritechStartupConfig.speedAddonSpeed.get().floatValue()).withEfficiencyMultiplier(OritechStartupConfig.speedAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.MACHINE_SPEED_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_EFFICIENCY_ADDON = registerBlock("machine_efficiency_addon", new MachineAddonBlock(blockProperties("machine_efficiency_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withEfficiencyMultiplier(OritechStartupConfig.efficiencyAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.MACHINE_EFFICIENCY_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_ULTIMATE_ADDON = registerBlock("machine_ultimate_addon", new MachineAddonBlock(blockProperties("machine_ultimate_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withSpeedMultiplier(OritechStartupConfig.ultimateAddonSpeed.get().floatValue()).withEfficiencyMultiplier(OritechStartupConfig.ultimateAddonEfficiency.get().floatValue()).withBoundingShape(MachineAddonBlock.MACHINE_ULTIMATE_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> QUARRY_ADDON = registerBlock("quarry_addon", new MachineAddonBlock(blockProperties("quarry_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.QUARRY_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_PROCESSING_ADDON = registerBlock("machine_processing_addon", new MachineAddonBlock(blockProperties("machine_processing_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withEfficiencyMultiplier(OritechStartupConfig.chamberAddonEfficiency.get().floatValue()).withChambers(1).withBoundingShape(MachineAddonBlock.MACHINE_PROCESSING_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_FLUID_ADDON = registerBlock("machine_fluid_addon", new MachineAddonBlock(blockProperties("machine_fluid_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_FLUID_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_YIELD_ADDON = registerBlock("machine_yield_addon", new MachineAddonBlock(blockProperties("machine_yield_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_YIELD_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> CROP_FILTER_ADDON = registerBlock("crop_filter_addon", new MachineAddonBlock(blockProperties("crop_filter_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.CROP_FILTER_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_HUNTER_ADDON = registerBlock("machine_hunter_addon", new MachineAddonBlock(blockProperties("machine_hunter_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_HUNTER_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_CAPACITOR_ADDON = registerBlock("machine_capacitor_addon", new MachineAddonBlock(blockProperties("machine_capacitor_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withAddedCapacity(2_000_000).withAddedInsert(2_000).withBoundingShape(MachineAddonBlock.MACHINE_CAPACITOR_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_ACCEPTOR_ADDON = registerBlock("machine_acceptor_addon", new MachineAddonBlock(blockProperties("machine_acceptor_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withAddedCapacity(500_000).withAddedInsert(5_000).withAcceptEnergy(true).withBoundingShape(MachineAddonBlock.MACHINE_ACCEPTOR_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_INVENTORY_PROXY_ADDON = registerBlock("machine_inventory_proxy_addon", new InventoryProxyAddonBlock(blockProperties("machine_inventory_proxy_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_INVENTORY_PROXY_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_EXTENDER = registerBlock("machine_extender", new MachineAddonBlock(blockProperties("machine_extender", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false)));
    public static final RegistrySupplier<Block> CAPACITOR_ADDON_EXTENDER = registerBlock("capacitor_addon_extender", new MachineAddonBlock(blockProperties("capacitor_addon_extender", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false).withAddedCapacity(2_500_000).withAddedInsert(1_000)));
    public static final RegistrySupplier<Block> STEAM_BOILER_ADDON = registerBlock("steam_boiler_addon", new SteamBoilerAddonBlock(blockProperties("steam_boiler_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.STEAM_BOILER_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_REDSTONE_ADDON = registerBlock("machine_redstone_addon", new RedstoneAddonBlock(blockProperties("machine_redstone_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_REDSTONE_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_SILK_TOUCH_ADDON = registerBlock("machine_silk_touch_addon", new MachineAddonBlock(blockProperties("machine_silk_touch_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_SILK_TOUCH_ADDON_SHAPE)));
    public static final RegistrySupplier<Block> MACHINE_BURST_ADDON = registerBlock("machine_burst_addon", new MachineAddonBlock(blockProperties("machine_burst_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBurstTicks(OritechStartupConfig.burstAddonTicks.get()).withBoundingShape(MachineAddonBlock.MACHINE_BURST_ADDON_SHAPE))); // todo config settings
    
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.none)
    @ItemRarity(ItemRarityValue.EPIC)
    public static final RegistrySupplier<Block> MACHINE_COMBI_ADDON = registerBlock("machine_combi_addon", new CombiAddonBlock(blockProperties("machine_combi_addon", Blocks.IRON_BLOCK).noOcclusion(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_COMBI_ADDON_SHAPE)));
    
    //region reactor
    @ItemRarity(ItemRarityValue.UNCOMMON)
    public static final RegistrySupplier<Block> REACTOR_CONTROLLER = registerBlock("reactor_controller", new ReactorControllerBlock(blockProperties("reactor_controller", Blocks.IRON_BLOCK).lightLevel(state -> 5)));
    public static final RegistrySupplier<Block> REACTOR_WALL = registerBlock("reactor_wall", new ReactorWallBlock(blockProperties("reactor_wall", Blocks.NETHERITE_BLOCK).strength(10, 1800)));
    public static final RegistrySupplier<Block> REACTOR_ROD = registerBlock("reactor_rod", new ReactorRodBlock(blockProperties("reactor_rod", Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3), 1, 1));
    public static final RegistrySupplier<Block> REACTOR_DOUBLE_ROD = registerBlock("reactor_double_rod", new ReactorRodBlock(blockProperties("reactor_double_rod", Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3), 2, 4));
    public static final RegistrySupplier<Block> REACTOR_QUAD_ROD = registerBlock("reactor_quad_rod", new ReactorRodBlock(blockProperties("reactor_quad_rod", Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 3), 4, 12));
    public static final RegistrySupplier<Block> REACTOR_VENT = registerBlock("reactor_vent", new ReactorHeatVentBlock(blockProperties("reactor_vent", Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> REACTOR_REFLECTOR = registerBlock("reactor_reflector", new ReactorReflectorBlock(blockProperties("reactor_reflector", Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> 15)));
    public static final RegistrySupplier<Block> REACTOR_HEAT_PIPE = registerBlock("reactor_heat_pipe", new ReactorHeatPipeBlock(blockProperties("reactor_heat_pipe", Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> REACTOR_CONDENSER = registerBlock("reactor_condenser", new ReactorAbsorberBlock(blockProperties("reactor_condenser", Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> REACTOR_FUEL_PORT = registerBlock("reactor_fuel_port", new ReactorFuelPortBlock(blockProperties("reactor_fuel_port", Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> REACTOR_ABSORBER_PORT = registerBlock("reactor_absorber_port", new ReactorAbsorberPortBlock(blockProperties("reactor_absorber_port", Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> REACTOR_ENERGY_PORT = registerBlock("reactor_energy_port", new ReactorEnergyPortBlock(blockProperties("reactor_energy_port", Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> REACTOR_REDSTONE_PORT = registerBlock("reactor_redstone_port", new ReactorRedstonePortBlock(blockProperties("reactor_redstone_port", Blocks.IRON_BLOCK).noOcclusion()));
    
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> URANIUM_CRYSTAL = registerBlock("uranium_crystal", new AmethystClusterBlock(7, 3, blockProperties("uranium_crystal", Blocks.AMETHYST_CLUSTER).lightLevel(state -> 5)));
    
    @NoBlockItem
    public static final RegistrySupplier<Block> REACTOR_EXPLOSION_SMALL = registerBlock("reactor_explosion_small", new NuclearExplosionBlock(blockProperties("reactor_explosion_small", Blocks.IRON_BLOCK), 9));
    @NoBlockItem
    public static final RegistrySupplier<Block> REACTOR_EXPLOSION_MEDIUM = registerBlock("reactor_explosion_medium", new NuclearExplosionBlock(blockProperties("reactor_explosion_medium", Blocks.IRON_BLOCK), 14));
    @NoBlockItem
    public static final RegistrySupplier<Block> REACTOR_EXPLOSION_LARGE = registerBlock("reactor_explosion_large", new NuclearExplosionBlock(blockProperties("reactor_explosion_large", Blocks.IRON_BLOCK), 20));
    public static final RegistrySupplier<Block> LOW_YIELD_NUKE = registerBlock("low_yield_nuke", new NukeBlock(blockProperties("low_yield_nuke", Blocks.IRON_BLOCK), true));
    public static final RegistrySupplier<Block> NUKE = registerBlock("nuke", new NukeBlock(blockProperties("nuke", Blocks.IRON_BLOCK), false));
    
    // cooling cell, early game re-fillable component
    
    // lategame, second stage components:
    // plasma conduit, advanced heat transfer system
    // entropy dampener, reduce degradation rate of nearby components
    // quantum stabilizer, massively increase heat capacity of reactor
    //endregion
    
    //region metals
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> NICKEL_ORE = registerBlock("nickel_ore", new Block(blockProperties("nickel_ore", Blocks.IRON_ORE)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> DEEPSLATE_NICKEL_ORE = registerBlock("deepslate_nickel_ore", new Block(blockProperties("deepslate_nickel_ore", Blocks.DEEPSLATE_IRON_ORE)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> ENDSTONE_PLATINUM_ORE = registerBlock("endstone_platinum_ore", new Block(blockProperties("endstone_platinum_ore", Blocks.DIAMOND_ORE)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> DEEPSLATE_PLATINUM_ORE = registerBlock("deepslate_platinum_ore", new Block(blockProperties("deepslate_platinum_ore", Blocks.DEEPSLATE_DIAMOND_ORE)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> DEEPSLATE_URANIUM_ORE = registerBlock("deepslate_uranium_ore", new Block(blockProperties("deepslate_uranium_ore", Blocks.DEEPSLATE_DIAMOND_ORE)));
    //endregion
    
    //region resource nodes
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RESOURCE_NODE_REDSTONE = registerBlock("resource_node_redstone", new Block(blockProperties("resource_node_redstone", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RESOURCE_NODE_LAPIS = registerBlock("resource_node_lapis", new Block(blockProperties("resource_node_lapis", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RESOURCE_NODE_IRON = registerBlock("resource_node_iron", new Block(blockProperties("resource_node_iron", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RESOURCE_NODE_COAL = registerBlock("resource_node_coal", new Block(blockProperties("resource_node_coal", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RESOURCE_NODE_GOLD = registerBlock("resource_node_gold", new Block(blockProperties("resource_node_gold", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RESOURCE_NODE_EMERALD = registerBlock("resource_node_emerald", new Block(blockProperties("resource_node_emerald", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RESOURCE_NODE_DIAMOND = registerBlock("resource_node_diamond", new Block(blockProperties("resource_node_diamond", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RESOURCE_NODE_COPPER = registerBlock("resource_node_copper", new Block(blockProperties("resource_node_copper", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RESOURCE_NODE_NICKEL = registerBlock("resource_node_nickel", new Block(blockProperties("resource_node_nickel", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RESOURCE_NODE_PLATINUM = registerBlock("resource_node_platinum", new Block(blockProperties("resource_node_platinum", Blocks.BEDROCK)));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RESOURCE_NODE_URANIUM = registerBlock("resource_node_uranium", new Block(blockProperties("resource_node_uranium", Blocks.BEDROCK)));
    
    // region decorative
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> CEILING_LIGHT = registerBlock("ceiling_light", new WallMountedLight(blockProperties("ceiling_light", Blocks.GLOWSTONE).noOcclusion(), 2));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> CEILING_LIGHT_HANGING = registerBlock("ceiling_light_hanging", new WallMountedLight(blockProperties("ceiling_light_hanging", Blocks.GLOWSTONE).noOcclusion(), 12));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> TECH_BUTTON = registerBlock("tech_button", new TechRedstoneButton(BlockSetType.IRON, 80, blockProperties("tech_button", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> TECH_LEVER = registerBlock("tech_lever", new TechLever(blockProperties("tech_lever", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> MACHINE_PLATING_BLOCK = registerBlock("machine_plating_block", new Block(blockProperties("machine_plating_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @NoAutoDrop
    public static final RegistrySupplier<Block> MACHINE_PLATING_SLAB = registerBlock("machine_plating_slab", new SlabBlock(blockProperties("machine_plating_slab", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> MACHINE_PLATING_STAIRS = registerBlock("machine_plating_stairs", new StairBlock(blockValue(MACHINE_PLATING_BLOCK).defaultBlockState(), blockProperties("machine_plating_stairs", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> MACHINE_PLATING_PRESSURE_PLATE = registerBlock("machine_plating_pressure_plate", new PressurePlateBlock(BlockSetType.IRON, blockProperties("machine_plating_pressure_plate", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> IRON_PLATING_BLOCK = registerBlock("iron_plating_block", new Block(blockProperties("iron_plating_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @NoAutoDrop
    public static final RegistrySupplier<Block> IRON_PLATING_SLAB = registerBlock("iron_plating_slab", new SlabBlock(blockProperties("iron_plating_slab", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> IRON_PLATING_STAIRS = registerBlock("iron_plating_stairs", new StairBlock(blockValue(IRON_PLATING_BLOCK).defaultBlockState(), blockProperties("iron_plating_stairs", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> IRON_PLATING_PRESSURE_PLATE = registerBlock("iron_plating_pressure_plate", new PressurePlateBlock(BlockSetType.IRON, blockProperties("iron_plating_pressure_plate", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> CARBON_PLATING_BLOCK = registerBlock("carbon_plating_block", new Block(blockProperties("carbon_plating_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @NoAutoDrop
    public static final RegistrySupplier<Block> CARBON_PLATING_SLAB = registerBlock("carbon_plating_slab", new SlabBlock(blockProperties("carbon_plating_slab", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> CARBON_PLATING_STAIRS = registerBlock("carbon_plating_stairs", new StairBlock(blockValue(CARBON_PLATING_BLOCK).defaultBlockState(), blockProperties("carbon_plating_stairs", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> CARBON_PLATING_PRESSURE_PLATE = registerBlock("carbon_plating_pressure_plate", new PressurePlateBlock(BlockSetType.IRON, blockProperties("carbon_plating_pressure_plate", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> NICKEL_PLATING_BLOCK = registerBlock("nickel_plating_block", new Block(blockProperties("nickel_plating_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @NoAutoDrop
    public static final RegistrySupplier<Block> NICKEL_PLATING_SLAB = registerBlock("nickel_plating_slab", new SlabBlock(blockProperties("nickel_plating_slab", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> NICKEL_PLATING_STAIRS = registerBlock("nickel_plating_stairs", new StairBlock(blockValue(NICKEL_PLATING_BLOCK).defaultBlockState(), blockProperties("nickel_plating_stairs", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> NICKEL_PLATING_PRESSURE_PLATE = registerBlock("nickel_plating_pressure_plate", new PressurePlateBlock(BlockSetType.IRON, blockProperties("nickel_plating_pressure_plate", Blocks.STONE_BUTTON)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> METAL_BEAM_BLOCK = registerBlock("metal_beam_block", new MetalBeamBlock(blockProperties("metal_beam_block", Blocks.IRON_BLOCK).noOcclusion().forceSolidOn()));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative) // todo recipe
    public static final RegistrySupplier<Block> METAL_GIRDER_BLOCK = registerBlock("metal_girder_block", new MetalGirderBlock(blockProperties("metal_girder_block", Blocks.IRON_BLOCK).noOcclusion().forceSolidOn()));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> INDUSTRIAL_GLASS_BLOCK = registerBlock("industrial_glass_block", new Block(blockProperties("industrial_glass_block", Blocks.GLASS).requiresCorrectToolForDrops().strength(7.0F, 8.0F).noOcclusion()));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @UseGeoBlockItem(scale = 0.5f)
    public static final RegistrySupplier<Block> TECH_DOOR = registerBlock("tech_door", new TechDoorBlock(blockProperties("tech_door", Blocks.IRON_DOOR).strength(8f).forceSolidOn()));
    @NoBlockItem
    @NoAutoDrop
    public static final RegistrySupplier<Block> TECH_DOOR_HINGE = registerBlock("tech_door_hinge", new TechDoorBlockHinge(blockProperties("tech_door_hinge", Blocks.IRON_DOOR).strength(8f).forceSolidOn()));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @UseGeoBlockItem(scale = 0.37f)
    public static final RegistrySupplier<Block> HANGAR_DOOR = registerBlock("hangar_door", new HangarDoorBlock(blockProperties("hangar_door", Blocks.IRON_DOOR).strength(8f).forceSolidOn()));
    @NoBlockItem
    @NoAutoDrop
    public static final RegistrySupplier<Block> HANGAR_DOOR_HELPER = registerBlock("hangar_door_helper", new HangarDoorHelperBlock(blockProperties("hangar_door_helper", Blocks.IRON_DOOR).strength(8f).forceSolidOn()));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> STEEL_BLOCK = registerBlock("steel_block", new Block(blockProperties("steel_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> ENERGITE_BLOCK = registerBlock("energite_block", new Block(blockProperties("energite_block", Blocks.IRON_BLOCK).lightLevel(state -> 6)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> NICKEL_BLOCK = registerBlock("nickel_block", new Block(blockProperties("nickel_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> BIOSTEEL_BLOCK = registerBlock("biosteel_block", new Block(blockProperties("biosteel_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> PLATINUM_BLOCK = registerBlock("platinum_block", new Block(blockProperties("platinum_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> ADAMANT_BLOCK = registerBlock("adamant_block", new Block(blockProperties("adamant_block", Blocks.DIAMOND_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> ELECTRUM_BLOCK = registerBlock("electrum_block", new Block(blockProperties("electrum_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> DURATIUM_BLOCK = registerBlock("duratium_block", new Block(blockProperties("duratium_block", Blocks.NETHERITE_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @Compostable(1.0f)
    public static final RegistrySupplier<Block> BIOMASS_BLOCK = registerBlock("biomass_block", new Block(blockProperties("biomass_block", Blocks.IRON_BLOCK).sound(SoundType.MOSS)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> PLASTIC_BLOCK = registerBlock("plastic_block", new Block(blockProperties("plastic_block", Blocks.IRON_BLOCK).sound(SoundType.SHROOMLIGHT)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> FLUXITE_BLOCK = registerBlock("fluxite_block", new Block(blockProperties("fluxite_block", Blocks.IRON_BLOCK).sound(SoundType.AMETHYST)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> SILICON_BLOCK = registerBlock("silicon_block", new SlimeBlock(blockProperties("silicon_block", Blocks.SLIME_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RAW_NICKEL_BLOCK = registerBlock("raw_nickel_block", new Block(blockProperties("raw_nickel_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RAW_PLATINUM_BLOCK = registerBlock("raw_platinum_block", new Block(blockProperties("raw_platinum_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> RAW_URANIUM_BLOCK = registerBlock("raw_uranium_block", new Block(blockProperties("raw_uranium_block", Blocks.IRON_BLOCK)));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final RegistrySupplier<Block> URANIUM_DUST_BLOCK = registerBlock("uranium_dust_block", new Block(blockProperties("uranium_dust_block", Blocks.IRON_BLOCK).lightLevel(state -> 2)));
    //endregion
    
    private static void postProcessField(Block value, Identifier identifier, Field field, RegistrySupplier<? extends Block> supplier) {
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
    
    private static Rarity getItemRarity(Field field) {
        return field.isAnnotationPresent(ItemRarity.class) ? field.getAnnotation(ItemRarity.class).value().rarity : null;
    }

    private static BlockItem createBlockItem(Block block, Rarity rarity, String identifier) {
        var properties = blockItemProperties(identifier);
        if (rarity != null) {
            properties = properties.rarity(rarity);
        }

        return new BlockItem(block, properties);
    }
    
    private static BlockItem getGeoBlockItem(Block block, String identifier, float scale, Rarity rarity) {
        var properties = blockItemProperties(identifier);
        if (rarity != null) {
            properties = properties.rarity(rarity);
        }
        return new OritechGeoItem(block, properties, scale, identifier);
    }

    private static Item.Properties blockItemProperties(String path) {
        return ItemContent.ITEMS.blockItemProperties(path);
    }

    private static Block blockValue(RegistrySupplier<? extends Block> supplier) {
        return value(supplier);
    }

    private static BlockBehaviour.Properties blockProperties(String path, BlockBehaviour source) {
        return BLOCKS.properties(path, () -> BlockBehaviour.Properties.ofFullCopy(source));
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

