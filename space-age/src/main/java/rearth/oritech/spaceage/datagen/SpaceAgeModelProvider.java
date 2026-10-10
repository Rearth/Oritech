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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import rearth.oritech.spaceage.OritechSpaceAge;
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
        blockModels.registerSimpleItemModel(block, model);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {

        blockModels.createHorizontallyRotatedBlock(SpaceAgeBlocks.ROCKET_ASSEMBLER.get(), TexturedModel.CUBE);
        blockModels.createTrivialCube(SpaceAgeBlocks.ROCKET_PAD.get());
        blockModels.createTrivialCube(SpaceAgeBlocks.ROCKET_COUPLING.get());
        createCustomModelBlock(SpaceAgeBlocks.NAVIGATION_COMPUTER.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.PARACHUTE.get(), blockModels);
        createPairBlock(SpaceAgeBlocks.VACUUM_CRAFTER.get(), blockModels);
        createPairBlock(SpaceAgeBlocks.CARGO.get(), blockModels);
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
