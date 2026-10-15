package rearth.oritech.client.ui.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import rearth.oritech.Oritech;

/** Unlit CTM previews retain depth testing, cutout edges and translucent glass. */
public final class PreviewBlockRenderTypes {

    private static final RenderPipeline CUTOUT_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
            .withLocation(Oritech.id("pipeline/preview_cutout"))
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("ALPHA_CUTOUT", 0.1f)
            .build();
    private static final RenderPipeline TRANSLUCENT_PIPELINE = CUTOUT_PIPELINE.toBuilder()
            .withLocation(Oritech.id("pipeline/preview_translucent"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();

    public static final RenderType CUTOUT = RenderType.create("oritech_preview_cutout",
            RenderSetup.builder(CUTOUT_PIPELINE)
                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                    .createRenderSetup());
    public static final RenderType TRANSLUCENT = RenderType.create("oritech_preview_translucent",
            RenderSetup.builder(TRANSLUCENT_PIPELINE)
                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                    .sortOnUpload()
                    .createRenderSetup());

    private PreviewBlockRenderTypes() {}

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {

        event.registerPipeline(CUTOUT_PIPELINE);
        event.registerPipeline(TRANSLUCENT_PIPELINE);
    }
}
