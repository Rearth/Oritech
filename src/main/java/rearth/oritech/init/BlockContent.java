package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.BlockRegistryContainer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.block.blocks.machines.addons.EnergyAddonBlock;
import rearth.oritech.block.blocks.machines.addons.InventoryProxyAddonBlock;
import rearth.oritech.block.blocks.machines.addons.MachineAddonBlock;
import rearth.oritech.block.blocks.machines.generators.BasicGeneratorBlock;
import rearth.oritech.block.blocks.machines.generators.BigSolarPanelBlock;
import rearth.oritech.block.blocks.machines.generators.TestGeneratorBlock;
import rearth.oritech.block.blocks.machines.interaction.*;
import rearth.oritech.block.blocks.machines.processing.*;
import rearth.oritech.block.blocks.pipes.*;
import rearth.oritech.util.item.OritechGeoItem;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

public class BlockContent implements BlockRegistryContainer {
    
    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Block BANANA_BLOCK = new Block(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK));
    
    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Block MACHINE_FRAME_BLOCK = new MachineFrameBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS));
    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Block FLUID_PIPE = new FluidPipeBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS));
    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Block ENERGY_PIPE = new EnergyPipeBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS));
    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Block ITEM_PIPE = new ItemPipeBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS));
    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Block ITEM_FILTER_BLOCK = new ItemFilterBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS));
    @NoBlockItem
    public static final Block FLUID_PIPE_CONNECTION = new FluidPipeConnectionBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS));
    @NoBlockItem
    public static final Block ENERGY_PIPE_CONNECTION = new EnergyPipeConnectionBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS));
    @NoBlockItem
    public static final Block ITEM_PIPE_CONNECTION = new ItemPipeConnectionBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS));
    
    @NoBlockItem
    public static final Block FRAME_GANTRY_ARM = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block BLOCK_DESTROYER_HEAD = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block BLOCK_PLACER_HEAD = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque());
    @NoBlockItem
    public static final Block BLOCK_FERTILIZER_HEAD = new Block(FabricBlockSettings.copyOf(Blocks.CHAIN).nonOpaque());
    
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
    public static final Block TEST_GENERATOR_BLOCK = new TestGeneratorBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block BASIC_GENERATOR_BLOCK = new BasicGeneratorBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block BIG_SOLAR_PANEL_BLOCK = new BigSolarPanelBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), 50);
    @UseGeoBlockItem(scale = 0.7f)
    public static final Block POWERED_FURNACE_BLOCK = new PoweredFurnaceBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @UseGeoBlockItem(scale = 0.5f)
    public static final Block LASER_ARM_BLOCK = new LaserArmBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    
    public static final Block PLACER_BLOCK = new PlacerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block DESTROYER_BLOCK = new DestroyerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block FERTILIZER_BLOCK = new FertilizerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block PUMP_BLOCK = new PumpBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    
    public static final Block MACHINE_CORE_BASIC = new MachineCoreBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), 1);
    public static final Block MACHINE_CORE_GOOD = new MachineCoreBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), 7);
    public static final Block MACHINE_SPEED_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 0.9f, 1.05f, true);
    public static final Block MACHINE_FLUID_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1f, 1f, true);
    public static final Block MACHINE_YIELD_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1f, 1f, true);
    public static final Block CROP_FILTER_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1f, 1f, true);
    public static final Block MACHINE_EFFICIENCY_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 0.9f, true);
    public static final Block MACHINE_CAPACITOR_ADDON = new EnergyAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 1f, 20000, 500, false);
    public static final Block MACHINE_ACCEPTOR_ADDON = new EnergyAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 1f, 5000, 50, true);
    public static final Block MACHINE_INVENTORY_PROXY_ADDON = new InventoryProxyAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 1f);
    public static final Block MACHINE_EXTENDER = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), true, 1, 1, false);
    
    @Override
    public void postProcessField(String namespace, Block value, String identifier, Field field) {
        
        Oritech.LOGGER.info("Registering Block: " + value + " with id " + identifier);
        
        if (field.isAnnotationPresent(NoBlockItem.class)) return;
        
        if (field.isAnnotationPresent(UseGeoBlockItem.class)) {
            Registry.register(Registries.ITEM, new Identifier(namespace, identifier), getGeoBlockItem(value, identifier, field.getAnnotation(UseGeoBlockItem.class).scale()));
        } else {
            Registry.register(Registries.ITEM, new Identifier(namespace, identifier), createBlockItem(value, identifier));
        }
        
        var targetGroup = ItemGroups.GROUPS.first;
        if (field.isAnnotationPresent(ItemGroups.ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemGroups.ItemGroupTarget.class).value();
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
    
}
