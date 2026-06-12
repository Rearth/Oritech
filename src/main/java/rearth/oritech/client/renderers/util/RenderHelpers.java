package rearth.oritech.client.renderers.util;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class RenderHelpers {

    public static final int FULL_BRIGHT = 15728880;


    public static void ExtractStateModels(List<BlockStateModelPart> list, BlockStateModel modelSet, ClientLevel level, BlockPos blockEntity, BlockState innerState) {
        list.clear();
        modelSet.collectParts(level, blockEntity, innerState, level.getRandom(), list);
    }
}
