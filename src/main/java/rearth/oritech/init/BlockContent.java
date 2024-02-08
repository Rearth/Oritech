package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.BlockRegistryContainer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import rearth.oritech.block.custom.*;
import rearth.oritech.block.custom.machines.AssemblerBlock;
import rearth.oritech.block.custom.machines.GrinderBlock;
import rearth.oritech.block.custom.machines.PulverizerBlock;
import rearth.oritech.block.custom.machines.addons.CapacitorAddonBlock;
import rearth.oritech.block.custom.machines.addons.InventoryProxyAddonBlock;
import rearth.oritech.block.custom.machines.addons.MachineAddonBlock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

public class BlockContent implements BlockRegistryContainer {

    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Block BANANA_BLOCK = new Block(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK));
    
    @RegisterREIWorkstation("pulverizer")
    public static final Block PULVERIZER_BLOCK = new PulverizerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @RegisterREIWorkstation("grinder")
    public static final Block GRINDER_BLOCK = new GrinderBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    @RegisterREIWorkstation("assembler")
    public static final Block ASSEMBLER_BLOCK = new AssemblerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    
    public static final Block MACHINE_CORE = new MachineCoreBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block MACHINE_SPEED_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 0.9f, 1.05f);
    public static final Block MACHINE_EFFICIENCY_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 0.9f);
    public static final Block MACHINE_CAPACITOR_ADDON = new CapacitorAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 1f, 5000, 100);
    public static final Block MACHINE_INVENTORY_PROXY_ADDON = new InventoryProxyAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 1f);
    public static final Block MACHINE_EXTENDER = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), true, 1, 1);

    @Override
    public void postProcessField(String namespace, Block value, String identifier, Field field) {
        BlockRegistryContainer.super.postProcessField(namespace, value, identifier, field);

        var targetGroup = ItemGroups.GROUPS.first;
        if (field.isAnnotationPresent(ItemGroups.ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemGroups.ItemGroupTarget.class).value();
        }

        ItemGroups.add(targetGroup, value);
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface RegisterREIWorkstation {
        String value(); // the name of the recipe type identifier, skipping the "oritech_". We can't directly reference the field because annotations don't allow that
    }

}
