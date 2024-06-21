package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.BlockRegistryContainer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.block.blocks.decorative.*;
import rearth.oritech.block.blocks.machines.addons.EnergyAddonBlock;
import rearth.oritech.block.blocks.machines.addons.InventoryProxyAddonBlock;
import rearth.oritech.block.blocks.machines.addons.MachineAddonBlock;
import rearth.oritech.block.blocks.machines.generators.*;
import rearth.oritech.block.blocks.machines.interaction.*;
import rearth.oritech.block.blocks.machines.processing.*;
import rearth.oritech.block.blocks.machines.storage.LargeStorageBlock;
import rearth.oritech.block.blocks.machines.storage.SmallFluidTank;
import rearth.oritech.block.blocks.machines.storage.SmallStorageBlock;
import rearth.oritech.block.blocks.pipes.*;
import rearth.oritech.init.datagen.BlockLootGenerator;
import rearth.oritech.item.other.SmallFluidTankBlockItem;
import rearth.oritech.util.item.OritechGeoItem;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

public class BlockContent implements BlockRegistryContainer {
    
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block BANANA_BLOCK = new Block(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK));
    
    public static final Block MACHINE_FRAME_BLOCK = new MachineFrameBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS));
    
    public static final Block FLUID_PIPE = new FluidPipeBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block ENERGY_PIPE = new EnergyPipeBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block ITEM_PIPE = new ItemPipeBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    public static final Block ITEM_FILTER_BLOCK = new ItemFilterBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    
    @NoBlockItem
    public static final Block FLUID_PIPE_CONNECTION = new FluidPipeConnectionBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block ENERGY_PIPE_CONNECTION = new EnergyPipeConnectionBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    @NoBlockItem
    public static final Block ITEM_PIPE_CONNECTION = new ItemPipeConnectionBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS).strength(1.0f, 2.0f));
    
    @NoBlockItem
    public static final Block FRAME_GANTRY_ARM = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block BLOCK_DESTROYER_HEAD = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block BLOCK_PLACER_HEAD = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block BLOCK_FERTILIZER_HEAD = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block PUMP_TRUNK_BLOCK = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block QUARRY_BEAM_INNER = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque().luminance(5));
    @NoBlockItem
    public static final Block QUARRY_BEAM_RING = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque().luminance(5));
    @NoBlockItem
    public static final Block QUARRY_BEAM_TARGET = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque());
    
    @NoBlockItem
    public static final Block ADDON_INDICATOR_BLOCK = new Block(FabricBlockSettings.copyOf(Blocks.GLASS));
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block PULVERIZER_BLOCK = new PulverizerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block FRAGMENT_FORGE_BLOCK = new FragmentForge(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block ASSEMBLER_BLOCK = new AssemblerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block FOUNDRY_BLOCK = new FoundryBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block CENTRIFUGE_BLOCK = new CentrifugeBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.3f)
    public static final Block ATOMIC_FORGE_BLOCK = new AtomicForgeBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block BIO_GENERATOR_BLOCK = new BioGeneratorBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block LAVA_GENERATOR_BLOCK = new LavaGeneratorBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.3f)
    public static final Block FUEL_GENERATOR_BLOCK = new FuelGeneratorBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block BASIC_GENERATOR_BLOCK = new BasicGeneratorBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block BIG_SOLAR_PANEL_BLOCK = new BigSolarPanelBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), Oritech.CONFIG.generators.solarGeneratorData.energyPerTick());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block POWERED_FURNACE_BLOCK = new PoweredFurnaceBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.5f)
    public static final Block LASER_ARM_BLOCK = new LaserArmBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.25f)
    public static final Block DEEP_DRILL_BLOCK = new DeepDrillBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.3f)
    public static final Block DRONE_PORT_BLOCK = new DronePortBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    
    @NoAutoDrop
    public static final Block SMALL_STORAGE_BLOCK = new SmallStorageBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block LARGE_STORAGE_BLOCK = new LargeStorageBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @NoAutoDrop
    public static final Block SMALL_TANK_BLOCK = new SmallFluidTank(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    
    public static final Block PLACER_BLOCK = new PlacerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block DESTROYER_BLOCK = new DestroyerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block FERTILIZER_BLOCK = new FertilizerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block TREEFELLER_BLOCK = new TreefellerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block PUMP_BLOCK = new PumpBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    
    public static final Block MACHINE_CORE_1 = new MachineCoreBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), 1);
    public static final Block MACHINE_CORE_2 = new MachineCoreBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), 2);
    public static final Block MACHINE_CORE_3 = new MachineCoreBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), 3);
    public static final Block MACHINE_CORE_4 = new MachineCoreBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), 4);
    public static final Block MACHINE_CORE_5 = new MachineCoreBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), 5);
    public static final Block MACHINE_CORE_6 = new MachineCoreBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), 6);
    public static final Block MACHINE_CORE_7 = new MachineCoreBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), 7);
    
    public static final Block MACHINE_SPEED_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 0.9f, 1.05f, true);
    public static final Block MACHINE_FLUID_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1f, 1f, true);
    public static final Block MACHINE_YIELD_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1f, 1f, true);
    public static final Block CROP_FILTER_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1f, 1f, true);
    public static final Block QUARRY_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1f, 1f, true);
    public static final Block MACHINE_EFFICIENCY_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 0.9f, true);
    public static final Block MACHINE_CAPACITOR_ADDON = new EnergyAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 1f, 2_000_000, 1000, false, true);
    public static final Block MACHINE_ACCEPTOR_ADDON = new EnergyAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 1f, 500_000, 2000, true, true);
    public static final Block MACHINE_INVENTORY_PROXY_ADDON = new InventoryProxyAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 1f);
    public static final Block MACHINE_EXTENDER = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), true, 1, 1, false);
    public static final Block CAPACITOR_ADDON_EXTENDER = new EnergyAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), true, 1, 1, 2_500_000, 500, false, false);
    
    @NoBlockItem
    public static final Block OIL_FLUID_BLOCK = new FluidBlock((FlowableFluid) FluidContent.FLOWING_OIL, FabricBlockSettings.copyOf(Blocks.WATER));
    @NoBlockItem
    public static final Block FUEL_FLUID_BLOCK = new FluidBlock((FlowableFluid) FluidContent.FLOWING_FUEL, FabricBlockSettings.copyOf(Blocks.WATER));
    
    //region metals
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block NICKEL_ORE = new Block(FabricBlockSettings.copyOf(Blocks.IRON_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block DEEPSLATE_NICKEL_ORE = new Block(FabricBlockSettings.copyOf(Blocks.DEEPSLATE_IRON_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block ENDSTONE_PLATINUM_ORE = new Block(FabricBlockSettings.copyOf(Blocks.DIAMOND_ORE));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block DEEPSLATE_PLATINUM_ORE = new Block(FabricBlockSettings.copyOf(Blocks.DEEPSLATE_DIAMOND_ORE));
    //endregion
    
    //region resource nodes
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_REDSTONE = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_LAPIS = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_IRON = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_COAL = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_GOLD = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_EMERALD = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_DIAMOND = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_COPPER = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_NICKEL = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK));
    @NoAutoDrop
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block RESOURCE_NODE_PLATINUM = new Block(FabricBlockSettings.copyOf(Blocks.BEDROCK));
    
    // region decorative
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block CEILING_LIGHT = new WallMountedLight(FabricBlockSettings.copyOf(Blocks.GLOWSTONE).nonOpaque(), 6);
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block CEILING_LIGHT_HANGING = new WallMountedLight(FabricBlockSettings.copyOf(Blocks.GLOWSTONE).nonOpaque(), 12);
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block TECH_BUTTON = new TechRedstoneButton(BlockSetType.IRON, 50, FabricBlockSettings.copyOf(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block TECH_LEVER = new TechLever(FabricBlockSettings.copyOf(Blocks.STONE_BUTTON));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block MACHINE_PLATING_BLOCK = new Block(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK));
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    public static final Block INDUSTRIAL_GLASS_BLOCK = new Block(FabricBlockSettings.copyOf(Blocks.GLASS).requiresTool().strength(7.0F, 8.0F).nonOpaque());
    @ItemContent.ItemGroupTarget(ItemContent.Groups.decorative)
    @UseGeoBlockItem(scale = 0.5f)
    public static final Block TECH_DOOR = new TechDoorBlock(FabricBlockSettings.copyOf(Blocks.IRON_DOOR).strength(8f));
    @NoBlockItem
    public static final Block TECH_DOOR_HINGE = new TechDoorBlockHinge(FabricBlockSettings.copyOf(Blocks.IRON_DOOR).strength(8f));
    //endregion
    
    @Override
    public void postProcessField(String namespace, Block value, String identifier, Field field) {
        
        if (field.isAnnotationPresent(NoBlockItem.class)) return;
        
        if (field.isAnnotationPresent(UseGeoBlockItem.class)) {
            Registry.register(Registries.ITEM, Identifier.of(namespace, identifier), getGeoBlockItem(value, identifier, field.getAnnotation(UseGeoBlockItem.class).scale()));
        } else if (value.equals(BlockContent.SMALL_TANK_BLOCK)) {
            Registry.register(Registries.ITEM, Identifier.of(namespace, identifier), new SmallFluidTankBlockItem(value, new Item.Settings()));
        } else {
            Registry.register(Registries.ITEM, Identifier.of(namespace, identifier), createBlockItem(value, identifier));
        }
        
        var targetGroup = ItemContent.Groups.machines;
        if (field.isAnnotationPresent(ItemContent.ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemContent.ItemGroupTarget.class).value();
        }
        
        if (!field.isAnnotationPresent(NoAutoDrop.class)) {
            BlockLootGenerator.autoRegisteredDrops.add(value);
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
    
}
