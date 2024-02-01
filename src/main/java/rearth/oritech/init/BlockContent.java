package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.BlockRegistryContainer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import rearth.oritech.block.custom.*;

import java.lang.reflect.Field;

public class BlockContent implements BlockRegistryContainer {

    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Block BANANA_BLOCK = new Block(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK));
    public static final Block PULVERIZER_BLOCK = new PulverizerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block GRINDER_BLOCK = new GrinderBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block ASSEMBLER_BLOCK = new AssemblerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block MACHINE_CORE = new MachineCoreBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());
    public static final Block MACHINE_SPEED_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 0.9f, 1.05f);
    public static final Block MACHINE_EFFICIENCY_ADDON = new MachineAddonBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque(), false, 1, 0.9f);
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

}
