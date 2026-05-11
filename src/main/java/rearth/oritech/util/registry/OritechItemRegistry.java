package rearth.oritech.util.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class OritechItemRegistry extends OritechDeferredRegistry<Item> {

    public OritechItemRegistry() {
        super(Registries.ITEM);
    }

    public <T extends Item> RegistrySupplier<T> registerItem(String path, Function<Item.Properties, T> factory) {
        return registerItem(path, factory, UnaryOperator.identity());
    }

    public <T extends Item> RegistrySupplier<T> registerItem(
      String path,
      Function<Item.Properties, T> factory,
      UnaryOperator<Item.Properties> properties
    ) {
        return registerItem(path, factory, () -> properties.apply(new Item.Properties()));
    }

    public <T extends Item> RegistrySupplier<T> registerItem(
      String path,
      Function<Item.Properties, T> factory,
      Supplier<Item.Properties> properties
    ) {
        return register(path, () -> factory.apply(prepareProperties(path, properties)));
    }

    public RegistrySupplier<Item> registerSimpleItem(String path) {
        return registerSimpleItem(path, UnaryOperator.identity());
    }

    public RegistrySupplier<Item> registerSimpleItem(String path, UnaryOperator<Item.Properties> properties) {
        return registerItem(path, Item::new, properties);
    }

    public RegistrySupplier<Item> registerSimpleItem(String path, Supplier<Item.Properties> properties) {
        return registerItem(path, Item::new, properties);
    }

    public Item.Properties properties(String path) {
        return properties(path, UnaryOperator.identity());
    }

    public Item.Properties properties(String path, UnaryOperator<Item.Properties> properties) {
        return prepareProperties(path, () -> properties.apply(new Item.Properties()));
    }

    public Item.Properties properties(String path, Supplier<Item.Properties> properties) {
        return prepareProperties(path, properties);
    }

    public Item.Properties blockItemProperties(String path) {
        return blockItemProperties(path, UnaryOperator.identity());
    }

    public Item.Properties blockItemProperties(String path, UnaryOperator<Item.Properties> properties) {
        return prepareBlockItemProperties(path, () -> properties.apply(new Item.Properties()));
    }

    public Item.Properties blockItemProperties(String path, Supplier<Item.Properties> properties) {
        return prepareBlockItemProperties(path, properties);
    }

    public RegistrySupplier<BlockItem> registerSimpleBlockItem(RegistrySupplier<? extends Block> block) {
        return registerSimpleBlockItem(block.getId().getPath(), block, UnaryOperator.identity());
    }

    public RegistrySupplier<BlockItem> registerSimpleBlockItem(RegistrySupplier<? extends Block> block, UnaryOperator<Item.Properties> properties) {
        return registerSimpleBlockItem(block.getId().getPath(), block, properties);
    }

    public RegistrySupplier<BlockItem> registerSimpleBlockItem(String path, Supplier<? extends Block> block) {
        return registerSimpleBlockItem(path, block, UnaryOperator.identity());
    }

    public RegistrySupplier<BlockItem> registerSimpleBlockItem(
      String path,
      Supplier<? extends Block> block,
      UnaryOperator<Item.Properties> properties
    ) {
        return registerBlockItem(path, block, BlockItem::new, properties);
    }

    public <T extends BlockItem> RegistrySupplier<T> registerBlockItem(
      String path,
      Supplier<? extends Block> block,
      BiFunction<Block, Item.Properties, T> factory
    ) {
        return registerBlockItem(path, block, factory, UnaryOperator.identity());
    }

    public <T extends BlockItem> RegistrySupplier<T> registerBlockItem(
      String path,
      Supplier<? extends Block> block,
      BiFunction<Block, Item.Properties, T> factory,
      UnaryOperator<Item.Properties> properties
    ) {
        return registerBlockItem(path, block, factory, () -> properties.apply(new Item.Properties()));
    }

    public <T extends BlockItem> RegistrySupplier<T> registerBlockItem(
      String path,
      Supplier<? extends Block> block,
      BiFunction<Block, Item.Properties, T> factory,
      Supplier<Item.Properties> properties
    ) {
        return register(path, () -> factory.apply(block.get(), prepareBlockItemProperties(path, properties)));
    }

    private Item.Properties prepareProperties(String path, Supplier<Item.Properties> properties) {
        return properties.get().setId(key(path));
    }

    private Item.Properties prepareBlockItemProperties(String path, Supplier<Item.Properties> properties) {
        return prepareProperties(path, properties).useBlockDescriptionPrefix();
    }
}