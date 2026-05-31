package rearth.oritech.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;

import java.lang.reflect.Modifier;
import java.util.function.Supplier;

@SuppressWarnings("NullableProblems")
public class ItemGroups {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Oritech.MOD_ID);

    public static final Supplier<CreativeModeTab> MACHINE_GROUP = TABS.register("machine_group", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemgroup.oritech.machines"))
                    .icon(() -> new ItemStack(BlockContent.FERTILIZER_BLOCK.value()))
                    .displayItems((params, output) -> {
                        ItemGroups.AddItemsToGroup(output, ItemContent.Groups.MACHINES);
                    })
                    .build());


    public static final Supplier<CreativeModeTab> COMPONENT_GROUP = TABS.register("machine_group", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemgroup.oritech.components"))
                    .icon(() -> new ItemStack(ItemContent.SUPER_AI_CHIP.value()))
                    .displayItems((params, output) -> {
                        ItemGroups.AddItemsToGroup(output, ItemContent.Groups.COMPONENTS);
                    })
                    .build());

    public static final Supplier<CreativeModeTab> EQUIPMENT_GROUP = TABS.register("machine_group", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemgroup.oritech.equipment"))
                    .icon(() -> new ItemStack(ToolsContent.CHAINSAW.value()))
                    .displayItems((params, output) -> {
                        ItemGroups.AddItemsToGroup(output, ItemContent.Groups.EQUIPMENT);
                    })
                    .build());

    public static final Supplier<CreativeModeTab> DECORATIVE_GROUP = TABS.register("machine_group", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemgroup.oritech.decorative"))
                    .icon(() -> new ItemStack(BlockContent.RESOURCE_NODE_PLATINUM.value()))
                    .displayItems((params, output) -> {
                        ItemGroups.AddItemsToGroup(output, ItemContent.Groups.DECORATIVE);
                    })
                    .build());

    @SuppressWarnings("unchecked")
    private static void AddItemsToGroup(CreativeModeTab.Output output, ItemContent.Groups targetGroup) {

        // load from item fields based on annotation
        for (var field : ItemContent.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!Modifier.isPublic(field.getModifiers())) continue;
            if (!DeferredItem.class.isAssignableFrom(field.getType())) continue;

            try {
                field.setAccessible(true);
                var value = (DeferredItem<Item>) field.get(null);
                var identifier = field.getName().toLowerCase(java.util.Locale.ROOT);

                var fieldGroup = ItemContent.Groups.COMPONENTS;

                if (field.isAnnotationPresent(ItemContent.ItemGroupTarget.class)) {
                    fieldGroup = field.getAnnotation(ItemContent.ItemGroupTarget.class).value();
                }

                if (fieldGroup.equals(targetGroup)) {
                    output.accept(value.value());
                }

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + field.getName(), e);
            }
        }

        // load from blockitems
        for (var blockData : BlockContent.BLOCK_GROUPS) {
            if (targetGroup.equals(blockData.getB()))
                output.accept(blockData.getA());
        }

        // add fluid bucket items
        for (var bucket : FluidContent.BUCKET_ITEMS.getEntries()) {
            if (targetGroup.equals(ItemContent.Groups.COMPONENTS))
                output.accept(bucket.value());
        }

        // add tools
        for (var tool : ToolsContent.EQUIPMENT.getEntries()) {
            if (targetGroup.equals(ItemContent.Groups.EQUIPMENT))
                output.accept(tool.value());
        }
    }
}
