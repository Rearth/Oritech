package rearth.oritech.client.renderers.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

public class RenderHelpers {

    public static final int FULL_BRIGHT = 15728880;


    public static void ExtractStateModels(List<BlockStateModelPart> list, BlockStateModel modelSet, Level level, BlockPos blockEntity, BlockState innerState) {
        list.clear();
        if (level instanceof BlockAndTintGetter blockAndTintGetter) {
            modelSet.collectParts(blockAndTintGetter, blockEntity, innerState, level.getRandom(), list);
        } else {
            modelSet.collectParts(level.getRandom(), list);
        }
    }

    /**
     * Resolves the still texture sprite of the given fluid using the data-driven fluid models introduced in
     * NeoForge 26.1. Client-side only. This replaces the removed Architectury {@code FluidStackHooks#getStillTexture}.
     */
    public static TextureAtlasSprite getFluidSprite(Fluid fluid) {
        return Minecraft.getInstance().getModelManager()
                .getFluidStateModelSet()
                .get(fluid.defaultFluidState())
                .stillMaterial()
                .sprite();
    }
}
