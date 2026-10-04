package rearth.oritech.spaceage.datagen;

import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.data.PackOutput;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

public class SpaceAgeModelProvider extends ModelProvider {

    public SpaceAgeModelProvider(PackOutput output) {
        super(output, OritechSpaceAge.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createHorizontallyRotatedBlock(SpaceAgeBlocks.ROCKET_ASSEMBLER.get(), TexturedModel.CUBE);
        blockModels.createTrivialCube(SpaceAgeBlocks.ROCKET_PAD.get());
        blockModels.createTrivialCube(SpaceAgeBlocks.ROCKET_COUPLING.get());
        createCustomModelBlock(SpaceAgeBlocks.MISSION_CONTROL.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.SPACE_SCANNER.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.ANTENNA.get(), blockModels);
        itemModels.itemModelOutput.accept(rearth.oritech.spaceage.init.SpaceAgeItems.MISSION_CARD.get(),
                net.minecraft.client.data.models.model.ItemModelUtils.plainModel(OritechSpaceAge.id("item/mission_card")));
        createCustomModelBlock(SpaceAgeBlocks.ASTEROID_ANCHOR.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get(), blockModels);
        createCustomModelBlock(SpaceAgeBlocks.ION_BOOSTER_ROCKET.get(), blockModels);
    }

    private static void createCustomModelBlock(net.minecraft.world.level.block.Block block, BlockModelGenerators blockModels) {
        var model = ModelLocationUtils.getModelLocation(block);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.variant(new Variant(model))));
        blockModels.registerSimpleItemModel(block, model);
    }
}
