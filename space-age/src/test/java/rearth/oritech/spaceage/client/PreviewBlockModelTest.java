package rearth.oritech.spaceage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import rearth.oritech.client.ui.render.PreviewBlockModel;
import rearth.oritech.client.ui.render.PreviewBlockRenderTypes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PreviewBlockModelTest {

    @Test
    void ctmPassBypassesBothWorldAndDirectionalLightingButKeepsTransparency() {

        for (var type : List.of(PreviewBlockRenderTypes.CUTOUT, PreviewBlockRenderTypes.TRANSLUCENT)) {
            var flags = type.pipeline().getShaderDefines().flags();
            assertTrue(flags.contains("NO_CARDINAL_LIGHTING"));
            assertTrue(flags.contains("EMISSIVE"));
            assertFalse(flags.contains("PER_FACE_LIGHTING"));
            assertEquals("0.1", type.pipeline().getShaderDefines().values().get("ALPHA_CUTOUT"));
        }
        assertFalse(PreviewBlockRenderTypes.CUTOUT.hasBlending());
        assertTrue(PreviewBlockRenderTypes.TRANSLUCENT.hasBlending());
    }

    @Test
    void ctmPartsUseTheUnlitPassAndSurviveModelReuse() {

        var model = new PreviewBlockModel();
        var part = new BlockStateModelPart() {
            @Override
            public List<BakedQuad> getQuads(Direction face) {

                return List.of();
            }

            @Override
            public boolean useAmbientOcclusion() {

                return false;
            }

            @Override
            public Material.Baked particleMaterial() {

                return null;
            }

            @Override
            public int materialFlags() {

                return 0;
            }
        };
        for (var translucent : List.of(false, true)) {
            model.clear();
            model.setupUnlitModel(translucent).add(part);
            var collector = new SubmitNodeStorage();
            model.submit(new PoseStack(), collector, 15728880, 0, 0);
            var submitted = collector.order(0).getBlockModelSubmits().getFirst();
            assertSame(translucent ? PreviewBlockRenderTypes.TRANSLUCENT : PreviewBlockRenderTypes.CUTOUT, submitted.renderType());
            assertEquals(List.of(part), submitted.modelParts());
            model.clear();
            assertEquals(List.of(part), submitted.modelParts());
        }
    }
}
