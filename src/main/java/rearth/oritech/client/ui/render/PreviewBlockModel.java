package rearth.oritech.client.ui.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;

import java.util.List;

/** Keep CTM texture colors independent of world lighting and generated face normals in GUI previews. */
public final class PreviewBlockModel extends BlockModelRenderState {

    private List<BlockStateModelPart> unlitParts;
    private RenderType unlitType;

    public List<BlockStateModelPart> setupUnlitModel(boolean translucent) {

        unlitType = translucent ? PreviewBlockRenderTypes.TRANSLUCENT : PreviewBlockRenderTypes.CUTOUT;
        unlitParts = setupModel(new Matrix4f(), translucent);
        return unlitParts;
    }

    @Override
    public void clear() {

        super.clear();
        unlitParts = null;
        unlitType = null;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outline) {

        if (unlitParts == null) {
            super.submit(poseStack, collector, light, overlay, outline);
        } else if (!unlitParts.isEmpty()) {
            collector.submitBlockModel(poseStack, unlitType, List.copyOf(unlitParts), tintLayers().toIntArray(), light, overlay, outline);
        }
    }
}
