package rearth.oritech.spaceage.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.basic.AsteroidBlock;
import rearth.oritech.spaceage.block.basic.AsteroidBlock.Ore;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeItems;

public class SpaceAgeModelProvider extends ModelProvider {

    public SpaceAgeModelProvider(PackOutput output) {

        super(output, OritechSpaceAge.MOD_ID);
    }

    private static void createPairBlock(Block block, BlockModelGenerators blockModels) {

        var model = ModelLocationUtils.getModelLocation(block);
        var variants = PropertyDispatch.initial(BlockStateProperties.FACING);
        variants.select(Direction.NORTH, BlockModelGenerators.variant(new Variant(model)));
        variants.select(Direction.SOUTH, BlockModelGenerators.variant(new Variant(model)).with(BlockModelGenerators.Y_ROT_180));
        variants.select(Direction.WEST, BlockModelGenerators.variant(new Variant(model)).with(BlockModelGenerators.Y_ROT_270));
        variants.select(Direction.EAST, BlockModelGenerators.variant(new Variant(model)).with(BlockModelGenerators.Y_ROT_90));
        variants.select(Direction.UP, BlockModelGenerators.variant(new Variant(model)).with(BlockModelGenerators.X_ROT_270));
        variants.select(Direction.DOWN, BlockModelGenerators.variant(new Variant(model)).with(BlockModelGenerators.X_ROT_90));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(variants));
        blockModels.registerSimpleItemModel(block, model);
    }

    private static void createCustomModelBlock(Block block, BlockModelGenerators blockModels) {

        var model = ModelLocationUtils.getModelLocation(block);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.variant(new Variant(model))));
        if (block.asItem() != Items.AIR)
            blockModels.registerSimpleItemModel(block, model);
    }

    private static void createHorizontalModelBlock(Block block, BlockModelGenerators blockModels) {

        var model = ModelLocationUtils.getModelLocation(block);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.variant(new Variant(model)))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        blockModels.registerSimpleItemModel(block, model);
    }

    private static void createAsteroidBlock(Block block, BlockModelGenerators blockModels) {

        var model = ModelLocationUtils.getModelLocation(block);
        var variants = PropertyDispatch.initial(AsteroidBlock.ORE);
        for (var ore : Ore.values()) {
            variants.select(ore, BlockModelGenerators.variant(new Variant(model.withSuffix("_" + ore.getSerializedName()))));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(variants));
        blockModels.registerSimpleItemModel(block, model.withSuffix("_iron"));
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {

        createHorizontalModelBlock(SpaceAgeBlocks.ROBOT_ARM.get(), blockModels);
        itemModels.itemModelOutput.accept(SpaceAgeItems.ACTION_CARD.get(), ItemModelUtils.plainModel(OritechSpaceAge.id("item/action_card")));
        createCustomModelBlock(SpaceAgeBlocks.EARTH.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.SUN.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.MARS.get(), blockModels);
        createHorizontalModelBlock(SpaceAgeBlocks.MINI_EARTH.get(), blockModels);
        createHorizontalModelBlock(SpaceAgeBlocks.MINI_MARS.get(), blockModels);
        createHorizontalModelBlock(SpaceAgeBlocks.MINI_SUN.get(), blockModels);
        createHorizontalModelBlock(SpaceAgeBlocks.GALLERY_LIGHT.get(), blockModels);
        createAsteroidBlock(SpaceAgeBlocks.ASTEROID_1.get(), blockModels);
        createAsteroidBlock(SpaceAgeBlocks.ASTEROID_2.get(), blockModels);
        createAsteroidBlock(SpaceAgeBlocks.ASTEROID_3.get(), blockModels);
        blockModels.createHorizontallyRotatedBlock(SpaceAgeBlocks.ROCKET_ASSEMBLER.get(), TexturedModel.CUBE);
        blockModels.createTrivialCube(SpaceAgeBlocks.ROCKET_PAD.get());
        blockModels.createTrivialCube(SpaceAgeBlocks.ROCKET_COUPLING.get());
        createCustomModelBlock(SpaceAgeBlocks.NAVIGATION_COMPUTER.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.PARACHUTE.get(), blockModels);
        createPairBlock(SpaceAgeBlocks.VACUUM_CRAFTER.get(), blockModels);
        createPairBlock(SpaceAgeBlocks.CARGO.get(), blockModels);
        createPairBlock(SpaceAgeBlocks.BEAM_APERTURE.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.CRYSTAL_GLASS.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.MISSION_CONTROL.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.SPACE_SCANNER.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.ANTENNA.get(), blockModels);
        itemModels.itemModelOutput.accept(SpaceAgeItems.MISSION_CARD.get(),
                ItemModelUtils.plainModel(OritechSpaceAge.id("item/mission_card")));
        createCustomModelBlock(SpaceAgeBlocks.ASTEROID_ANCHOR.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.ION_BOOSTER_ROCKET.get(), blockModels);
    }
}
