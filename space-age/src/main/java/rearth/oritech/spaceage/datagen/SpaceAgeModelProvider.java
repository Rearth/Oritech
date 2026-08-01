package rearth.oritech.spaceage.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.TexturedModel;
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
        blockModels.createTrivialCube(SpaceAgeBlocks.ROCKET_ENGINE_TIER_1.get());
        blockModels.createTrivialCube(SpaceAgeBlocks.ROCKET_ENGINE_TIER_2.get());
        blockModels.createTrivialCube(SpaceAgeBlocks.ROCKET_ENGINE_TIER_3.get());
    }
}
