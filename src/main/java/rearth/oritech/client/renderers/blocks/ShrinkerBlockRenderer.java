package rearth.oritech.client.renderers.blocks;

import com.geckolib.cache.object.GeoBone;
import com.geckolib.cache.object.GeoQuad;
import com.geckolib.cache.object.GeoVertex;
import com.geckolib.cache.texture.AnimatableTexture;
import com.geckolib.renderer.specialty.DynamicGeoBlockRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;
import rearth.oritech.client.renderers.models.MachineModel;

public class ShrinkerBlockRenderer extends DynamicGeoBlockRenderer<ShrinkerBlockEntity> {

    public ShrinkerBlockRenderer(String modelPath) {
        super(new MachineModel<>(modelPath));
    }

    @Override
    protected @Nullable Identifier getTextureOverrideForBone(GeoBone bone, ShrinkerBlockEntity animatable, float partialTick) {

        if (bone.getName().startsWith("moving")) {
            return Oritech.id("textures/block/fluid/fluid_strange_mixture.png");
        }

        return null;
    }

    @Override
    public void updateAnimatedTextureFrame(ShrinkerBlockEntity animatable) {
        super.updateAnimatedTextureFrame(animatable);
        AnimatableTexture.setAndUpdate(Oritech.id("textures/block/fluid/fluid_strange_mixture.png"));
    }

    // this is basically the same as above, but with different UV coordinates, and also fullbright
    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer,
                                     int packedLight, int packedOverlay, int colour) {
        if (this.textureOverride == null) {
            super.createVerticesOfQuad(quad, poseState, normal, buffer, packedLight, packedOverlay,
                    colour);

            return;
        }

        IntIntPair boneTextureSize = computeTextureSize(this.textureOverride);
        IntIntPair blockTextureSize = computeTextureSize(getTextureLocation(this.animatable));

        if (boneTextureSize == null || blockTextureSize == null) {
            super.createVerticesOfQuad(quad, poseState, normal, buffer, packedLight, packedOverlay,
                    colour);

            return;
        }

        for (GeoVertex vertex : quad.vertices()) {
            Vector4f vector4f = poseState.transform(new Vector4f(vertex.position().x(), vertex.position().y(), vertex.position().z(), 1.0f));
            float texU = vertex.texU();
            float texV = vertex.texV();

            buffer.addVertex(vector4f.x(), vector4f.y(), vector4f.z(), colour, texU, texV,
                    packedOverlay, LightTexture.FULL_BRIGHT, normal.x(), normal.y(), normal.z());
        }
    }
}
