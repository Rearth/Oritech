package rearth.oritech.init;

import dev.architectury.registry.registries.RegistrySupplier;
import io.wispforest.owo.registration.reflect.BlockRegistryContainer.NoBlockItem;
import net.minecraft.block.*;
import net.minecraft.block.dispenser.BlockPlacementDispenserBehavior;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.block.blocks.arcane.*;
import rearth.oritech.block.blocks.decorative.*;
import rearth.oritech.block.blocks.machines.accelerator.*;
import rearth.oritech.block.blocks.machines.addons.InventoryProxyAddonBlock;
import rearth.oritech.block.blocks.machines.addons.MachineAddonBlock;
import rearth.oritech.block.blocks.machines.addons.MachineAddonBlock.AddonSettings;
import rearth.oritech.block.blocks.machines.addons.RedstoneAddonBlock;
import rearth.oritech.block.blocks.machines.addons.SteamBoilerAddonBlock;
import rearth.oritech.block.blocks.machines.generators.*;
import rearth.oritech.block.blocks.machines.interaction.*;
import rearth.oritech.block.blocks.machines.processing.*;
import rearth.oritech.block.blocks.machines.storage.*;
import rearth.oritech.block.blocks.pipes.*;
import rearth.oritech.item.other.SmallFluidTankBlockItem;
import rearth.oritech.util.ArchitecturyBlockRegistryContainer;
import rearth.oritech.util.item.OritechGeoItem;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class BlockContent implements ArchitecturyBlockRegistryContainer {
    
    public static Set<Block> autoRegisteredDrops = new HashSet<>();
    
    public static final Block SPAWNER_CAGE_BLOCK = new SpawnerCageBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    
    public static final Block MACHINE_FRAME_BLOCK = new MachineFrameBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS));
    
    public static final Block FLUID_PIPE = new FluidPipeBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block ENERGY_PIPE = new EnergyPipeBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block SUPERCONDUCTOR = new SuperConductorBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block ITEM_PIPE = new ItemPipeBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block ITEM_FILTER_BLOCK = new ItemFilterBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    
    @NoBlockItem
    public static final Block FLUID_PIPE_CONNECTION = new FluidPipeConnectionBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block ENERGY_PIPE_CONNECTION = new EnergyPipeConnectionBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block SUPERCONDUCTOR_CONNECTION = new SuperConductorConnectionBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block ITEM_PIPE_CONNECTION = new ItemPipeConnectionBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    
    @NoBlockItem
    public static final Block FRAME_GANTRY_ARM = new Block(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block BLOCK_DESTROYER_HEAD = new Block(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block BLOCK_PLACER_HEAD = new Block(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block BLOCK_FERTILIZER_HEAD = new Block(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block PUMP_TRUNK_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block QUARRY_BEAM_INNER = new Block(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque().luminance(item -> 5));
    @NoBlockItem
    public static final Block QUARRY_BEAM_RING = new Block(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque().luminance(item -> 5));
    @NoBlockItem
    public static final Block QUARRY_BEAM_TARGET = new Block(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block BLACK_HOLE_INNER = new Block(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque().luminance(item -> 5));
    @NoBlockItem
    public static final Block BLACK_HOLE_MIDDLE = new Block(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque().luminance(item -> 5));
    @NoBlockItem
    public static final Block BLACK_HOLE_OUTER = new Block(AbstractBlock.Settings.copy(Blocks.CHAIN).nonOpaque().luminance(item -> 5));
    
    @NoBlockItem
    public static final Block ADDON_INDICATOR_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.GLASS));
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block PULVERIZER_BLOCK = new PulverizerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block FRAGMENT_FORGE_BLOCK = new FragmentForge(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block ASSEMBLER_BLOCK = new AssemblerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block FOUNDRY_BLOCK = new FoundryBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block CENTRIFUGE_BLOCK = new CentrifugeBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.3f)
    public static final Block ATOMIC_FORGE_BLOCK = new AtomicForgeBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block BIO_GENERATOR_BLOCK = new BioGeneratorBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block LAVA_GENERATOR_BLOCK = new LavaGeneratorBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.3f)
    public static final Block FUEL_GENERATOR_BLOCK = new FuelGeneratorBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block BASIC_GENERATOR_BLOCK = new BasicGeneratorBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block STEAM_ENGINE_BLOCK = new SteamEngineBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block BIG_SOLAR_PANEL_BLOCK = new BigSolarPanelBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), Oritech.CONFIG.generators.solarGeneratorData.energyPerTick());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block POWERED_FURNACE_BLOCK = new PoweredFurnaceBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.5f)
    public static final Block LASER_ARM_BLOCK = new LaserArmBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.25f)
    public static final Block DEEP_DRILL_BLOCK = new DeepDrillBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.3f)
    public static final Block DRONE_PORT_BLOCK = new DronePortBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    
    @NoAutoDrop
    @DispenserPlace
    public static final Block SMALL_STORAGE_BLOCK = new SmallStorageBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque().pistonBehavior(PistonBehavior.DESTROY));
    public static final Block LARGE_STORAGE_BLOCK = new LargeStorageBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @DispenserPlace
    public static final Block CREATIVE_STORAGE_BLOCK = new CreativeStorageBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque().pistonBehavior(PistonBehavior.BLOCK).hardness(-1.0F));
    
    @NoAutoDrop
    @DispenserPlace
    public static final Block SMALL_TANK_BLOCK = new SmallFluidTank(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque().pistonBehavior(PistonBehavior.DESTROY).luminance(Blocks.createLightLevelFromLitBlockState(15)));

    @NoAutoDrop
    @DispenserPlace
    public static final Block CREATIVE_TANK_BLOCK = new CreativeFluidTank(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque().pistonBehavior(PistonBehavior.BLOCK).luminance(Blocks.createLightLevelFromLitBlockState(15)).hardness(-1.0F));
    
    public static final Block PLACER_BLOCK = new PlacerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block DESTROYER_BLOCK = new DestroyerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block FERTILIZER_BLOCK = new FertilizerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block TREEFELLER_BLOCK = new TreefellerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block ENCHANTMENT_CATALYST_BLOCK = new EnchantmentCatalystBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block ENCHANTER_BLOCK = new EnchanterBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block SPAWNER_CONTROLLER_BLOCK = new SpawnerControllerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @NoAutoDrop
    public static final Block WITHER_CROP_BLOCK = new WitheredCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT));
    
    public static final Block ACCELERATOR_RING = new AcceleratorRingBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block ACCELERATOR_MOTOR = new AcceleratorMotorBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque().luminance(item -> 5));
    public static final Block ACCELERATOR_CONTROLLER = new AcceleratorControllerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block ACCELERATOR_SENSOR = new AcceleratorSensorBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block BLACK_HOLE_BLOCK = new BlackHoleBlock(AbstractBlock.Settings.copy(Blocks.END_PORTAL).luminance(item -> 12).nonOpaque());
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block PUMP_BLOCK = new PumpBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block CHARGER_BLOCK = new ChargerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    
    public static final Block MACHINE_CORE_1 = new MachineCoreBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), 1);
    public static final Block MACHINE_CORE_2 = new MachineCoreBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), 2);
    public static final Block MACHINE_CORE_3 = new MachineCoreBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), 3);
    public static final Block MACHINE_CORE_4 = new MachineCoreBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), 4);
    public static final Block MACHINE_CORE_5 = new MachineCoreBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), 5);
    public static final Block MACHINE_CORE_6 = new MachineCoreBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), 6);
    public static final Block MACHINE_CORE_7 = new MachineCoreBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), 7);
    
    public static final Block MACHINE_SPEED_ADDON = new MachineAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withSpeedMultiplier(0.9f).withEfficiencyMultiplier(1.05f).withBoundingShape(MachineAddonBlock.MACHINE_SPEED_ADDON_SHAPE));
    public static final Block MACHINE_FLUID_ADDON = new MachineAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_FLUID_ADDON_SHAPE));
    public static final Block MACHINE_YIELD_ADDON = new MachineAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_YIELD_ADDON_SHAPE));
    public static final Block CROP_FILTER_ADDON = new MachineAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.CROP_FILTER_ADDON_SHAPE));
    public static final Block QUARRY_ADDON = new MachineAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.QUARRY_ADDON_SHAPE));
    public static final Block MACHINE_HUNTER_ADDON = new MachineAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_HUNTER_ADDON_SHAPE));
    public static final Block MACHINE_EFFICIENCY_ADDON = new MachineAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withEfficiencyMultiplier(0.9f).withBoundingShape(MachineAddonBlock.MACHINE_EFFICIENCY_ADDON_SHAPE));
    public static final Block MACHINE_CAPACITOR_ADDON = new MachineAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withAddedCapacity(2_000_000).withAddedInsert(1_000).withBoundingShape(MachineAddonBlock.MACHINE_CAPACITOR_ADDON_SHAPE));
    public static final Block MACHINE_ACCEPTOR_ADDON = new MachineAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withAddedCapacity(500_000).withAddedInsert(2000).withAcceptEnergy(true).withBoundingShape(MachineAddonBlock.MACHINE_ACCEPTOR_ADDON_SHAPE));
    public static final Block MACHINE_INVENTORY_PROXY_ADDON = new InventoryProxyAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_INVENTORY_PROXY_ADDON_SHAPE));
    public static final Block MACHINE_EXTENDER = new MachineAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false));
    public static final Block CAPACITOR_ADDON_EXTENDER = new MachineAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withExtender(true).withNeedsSupport(false).withAddedCapacity(2_500_000).withAddedInsert(500));
    public static final Block STEAM_BOILER_ADDON = new SteamBoilerAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.STEAM_BOILER_ADDON_SHAPE));
    public static final Block MACHINE_REDSTONE_ADDON = new RedstoneAddonBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque(), AddonSettings.getDefaultSettings().withBoundingShape(MachineAddonBlock.MACHINE_REDSTONE_ADDON_SHAPE));
    
    //region metals
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_ORE = new Block(AbstractBlock.Settings.copy(Blocks.IRON_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block DEEPSLATE_NICKEL_ORE = new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE_IRON_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block ENDSTONE_PLATINUM_ORE = new Block(AbstractBlock.Settings.copy(Blocks.DIAMOND_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block DEEPSLATE_PLATINUM_ORE = new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE_DIAMOND_ORE));
    //endregion
    
    //region resource nodes
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_REDSTONE = new Block(AbstractBlock.Settings.copy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_LAPIS = new Block(AbstractBlock.Settings.copy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_IRON = new Block(AbstractBlock.Settings.copy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_COAL = new Block(AbstractBlock.Settings.copy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_GOLD = new Block(AbstractBlock.Settings.copy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_EMERALD = new Block(AbstractBlock.Settings.copy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_DIAMOND = new Block(AbstractBlock.Settings.copy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_COPPER = new Block(AbstractBlock.Settings.copy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_NICKEL = new Block(AbstractBlock.Settings.copy(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_PLATINUM = new Block(AbstractBlock.Settings.copy(Blocks.BEDROCK));
    
    // region decorative
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block CEILING_LIGHT = new WallMountedLight(AbstractBlock.Settings.copy(Blocks.GLOWSTONE).nonOpaque(), 6);
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block CEILING_LIGHT_HANGING = new WallMountedLight(AbstractBlock.Settings.copy(Blocks.GLOWSTONE).nonOpaque(), 12);
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block TECH_BUTTON = new TechRedstoneButton(BlockSetType.IRON, 80, AbstractBlock.Settings.copy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block TECH_LEVER = new TechLever(AbstractBlock.Settings.copy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block MACHINE_PLATING_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block MACHINE_PLATING_SLAB = new SlabBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block MACHINE_PLATING_STAIRS = new StairsBlock(MACHINE_PLATING_BLOCK.getDefaultState(), AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block MACHINE_PLATING_PRESSURE_PLATE = new PressurePlateBlock(BlockSetType.IRON, AbstractBlock.Settings.copy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block IRON_PLATING_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block IRON_PLATING_SLAB = new SlabBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block IRON_PLATING_STAIRS = new StairsBlock(IRON_PLATING_BLOCK.getDefaultState(), AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block IRON_PLATING_PRESSURE_PLATE = new PressurePlateBlock(BlockSetType.IRON, AbstractBlock.Settings.copy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_PLATING_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_PLATING_SLAB = new SlabBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_PLATING_STAIRS = new StairsBlock(NICKEL_PLATING_BLOCK.getDefaultState(), AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_PLATING_PRESSURE_PLATE = new PressurePlateBlock(BlockSetType.IRON, AbstractBlock.Settings.copy(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block METAL_BEAM_BLOCK = new MetalBeamBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block INDUSTRIAL_GLASS_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.GLASS).requiresTool().strength(7.0F, 8.0F).nonOpaque());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @UseGeoBlockItem(scale = 0.5f)
    public static final Block TECH_DOOR = new TechDoorBlock(AbstractBlock.Settings.copy(Blocks.IRON_DOOR).strength(8f));
    @NoBlockItem
    public static final Block TECH_DOOR_HINGE = new TechDoorBlockHinge(AbstractBlock.Settings.copy(Blocks.IRON_DOOR).strength(8f));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block STEEL_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block ENERGITE_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block BIOSTEEL_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block PLATINUM_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block ADAMANT_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.DIAMOND_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block ELECTRUM_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block DURATIUM_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.NETHERITE_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block BIOMASS_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block PLASTIC_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).sounds(BlockSoundGroup.SHROOMLIGHT));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block FLUXITE_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block SILICON_BLOCK = new SlimeBlock(AbstractBlock.Settings.copy(Blocks.SLIME_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RAW_NICKEL_BLOCK = new SlimeBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RAW_PLATINUM_BLOCK = new SlimeBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    //endregion
    
    @Override
    public void postProcessField(String namespace, Block value, String identifier, Field field, RegistrySupplier<Block> supplier) {
        
        if (field.isAnnotationPresent(NoBlockItem.class)) return;
        
        if (field.isAnnotationPresent(UseGeoBlockItem.class)) {
            Registry.register(Registries.ITEM, Identifier.of(namespace, identifier), getGeoBlockItem(value, identifier, field.getAnnotation(UseGeoBlockItem.class).scale()));
        } else if (value.equals(BlockContent.SMALL_TANK_BLOCK) || value.equals(BlockContent.CREATIVE_TANK_BLOCK)) {
            Registry.register(Registries.ITEM, Identifier.of(namespace, identifier), new SmallFluidTankBlockItem(value, new Item.Settings()));
        } else {
            Registry.register(Registries.ITEM, Identifier.of(namespace, identifier), createBlockItem(value, identifier));
        }
        
        var targetGroup = ItemContent.Groups.machines;
        if (field.isAnnotationPresent(ItemContent.ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemContent.ItemGroupTarget.class).value();
        }
        
        if (!field.isAnnotationPresent(NoAutoDrop.class)) {
            autoRegisteredDrops.add(value);
        }

        if (field.isAnnotationPresent(DispenserPlace.class)) {
            DispenserBlock.registerBehavior(value, new BlockPlacementDispenserBehavior());
        }
        
        ItemGroups.add(targetGroup, value);
    }
    
    private BlockItem getGeoBlockItem(Block block, String identifier, float scale) {
        return new OritechGeoItem(block, new Item.Settings(), scale, identifier);
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
