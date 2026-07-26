package rearth.oritech.client.renderers.blocks;

import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.CustomBoneTextureGeoLayer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;
import rearth.oritech.client.renderers.models.MachineModel;
import rearth.oritech.client.renderers.util.RenderHelpers;

public class ShrinkerBlockRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<ShrinkerBlockEntity, R> {

    private static final Identifier STRANGE_MIXTURE_TEXTURE = Oritech.id("textures/block/fluid/fluid_strange_mixture.png");

    public ShrinkerBlockRenderer(BlockEntityRendererProvider.Context context, String modelPath) {
        super(context, new MachineModel<>(modelPath));
        withRenderLayer(new FullbrightFluidBoneLayer<>(this, "moving"));
        withRenderLayer(new FullbrightFluidBoneLayer<>(this, "moving2"));
    }

    @Override
    public AABB getRenderBoundingBox(ShrinkerBlockEntity blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), 4, 4, 4);
    }

    @Override
    public void addRenderData(ShrinkerBlockEntity animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);
        MachineRenderer.addColorRenderData(animatable, renderState);
    }

    private static class FullbrightFluidBoneLayer<R extends GeoRenderState> extends CustomBoneTextureGeoLayer<ShrinkerBlockEntity, Void, R> {

        public FullbrightFluidBoneLayer(GeoRenderer<ShrinkerBlockEntity, Void, R> renderer, String boneName) {
            super(renderer, boneName, STRANGE_MIXTURE_TEXTURE);
        }

        @Override
        protected void renderQuad(GeoQuad quad, Matrix4f pose, Vector3f normal, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int renderColor, float widthRatio, float heightRatio) {
            for (GeoVertex vertex : quad.vertices()) {
                Vector4f vector4f = pose.transform(new Vector4f(vertex.posX(), vertex.posY(), vertex.posZ(), 1));

                vertexConsumer.addVertex(vector4f.x(), vector4f.y(), vector4f.z(), renderColor, vertex.texU(), vertex.texV(),
                        packedOverlay, RenderHelpers.FULL_BRIGHT, normal.x(), normal.y(), normal.z());
            }
        }
    }
}
