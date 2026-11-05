package rearth.oritech.spaceage.client.trophy;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.trophy.SpaceTrophyBlockEntity;
import rearth.oritech.spaceage.block.trophy.TrophyInscription;

import java.util.Locale;

public final class SpaceTrophyRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<SpaceTrophyBlockEntity, R> {

    private static final DataTicket<TrophyInscription> INSCRIPTION = DataTicket.create("space_trophy_inscription", TrophyInscription.class);

    public SpaceTrophyRenderer(BlockEntityRendererProvider.Context context) {

        super(context, new Model());
    }

    @Override
    public void addRenderData(SpaceTrophyBlockEntity trophy, Void related, R state, float partialTick) {

        if (trophy.data() != null) state.addGeckolibData(INSCRIPTION, trophy.data());
    }

    @Override
    public AABB getRenderBoundingBox(SpaceTrophyBlockEntity trophy) {

        return new AABB(trophy.getBlockPos()).inflate(1.5).expandTowards(0, 2, 0);
    }

    @Override
    public void postRenderPass(RenderPassInfo<R> pass, SubmitNodeCollector collector) {

        var inscription = pass.getGeckolibData(INSCRIPTION);
        if (inscription == null) return;
        var font = Minecraft.getInstance().font;
        var lines = new Component[]{Component.literal(inscription.player()),
                Component.translatable("trophy.oritech_space_age.orbit." + inscription.orbit().name().toLowerCase(Locale.ROOT))};
        var stack = pass.poseStack();
        stack.pushPose();
        stack.translate(.5, 0, .5);
        tryRotateByBlockstate(pass, stack);
        // The plaque is the north face of the pedestal: x=-8..8, y=4..12, z=-8.
        stack.translate(0, .5, -.501);
        stack.mulPose(Axis.YP.rotationDegrees(180));
        var width = Math.max(font.width(lines[0]), font.width(lines[1]));
        var scale = Math.min(.009f, .9f / Math.max(1, width));
        stack.scale(scale, -scale, scale);
        for (int row = 0; row < lines.length; row++)
            collector.submitText(stack, -font.width(lines[row]) / 2f, row * 12 - 10, lines[row].getVisualOrderText(),
                    false, Font.DisplayMode.NORMAL, pass.renderState().lightCoords, 0xFFFFFFFF, 0, 0);
        stack.popPose();
    }

    private static final class Model extends GeoModel<SpaceTrophyBlockEntity> {

        @Override
        public Identifier getModelResource(GeoRenderState state) {

            return OritechSpaceAge.id("space_trophy");
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {

            return OritechSpaceAge.id("textures/models/space_trophy.png");
        }

        @Override
        public Identifier getAnimationResource(SpaceTrophyBlockEntity trophy) {

            return OritechSpaceAge.id("space_trophy");
        }
    }
}
