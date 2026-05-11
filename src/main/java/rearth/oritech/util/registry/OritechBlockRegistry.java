package rearth.oritech.util.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class OritechBlockRegistry extends OritechDeferredRegistry<Block> {

    public OritechBlockRegistry() {
        super(Registries.BLOCK);
    }

    public <T extends Block> RegistrySupplier<T> registerBlock(String path, Function<BlockBehaviour.Properties, T> factory) {
        return registerBlock(path, factory, UnaryOperator.identity());
    }

    public <T extends Block> RegistrySupplier<T> registerBlock(
      String path,
      Function<BlockBehaviour.Properties, T> factory,
      UnaryOperator<BlockBehaviour.Properties> properties
    ) {
        return registerBlock(path, factory, () -> properties.apply(BlockBehaviour.Properties.of()));
    }

    public <T extends Block> RegistrySupplier<T> registerBlock(
      String path,
      Function<BlockBehaviour.Properties, T> factory,
      Supplier<BlockBehaviour.Properties> properties
    ) {
        return register(path, () -> factory.apply(prepareProperties(path, properties)));
    }

    public RegistrySupplier<Block> registerSimpleBlock(String path) {
        return registerSimpleBlock(path, UnaryOperator.identity());
    }

    public RegistrySupplier<Block> registerSimpleBlock(String path, UnaryOperator<BlockBehaviour.Properties> properties) {
        return registerBlock(path, Block::new, properties);
    }

    public RegistrySupplier<Block> registerSimpleBlock(String path, Supplier<BlockBehaviour.Properties> properties) {
        return registerBlock(path, Block::new, properties);
    }

    public BlockBehaviour.Properties properties(String path) {
        return properties(path, UnaryOperator.identity());
    }

    public BlockBehaviour.Properties properties(String path, UnaryOperator<BlockBehaviour.Properties> properties) {
        return prepareProperties(path, () -> properties.apply(BlockBehaviour.Properties.of()));
    }

    public BlockBehaviour.Properties properties(String path, Supplier<BlockBehaviour.Properties> properties) {
        return prepareProperties(path, properties);
    }

    public BlockBehaviour.Properties properties(String path, BlockBehaviour.Properties properties) {
        return properties.setId(key(path));
    }

    private BlockBehaviour.Properties prepareProperties(String path, Supplier<BlockBehaviour.Properties> properties) {
        return properties.get().setId(key(path));
    }
}