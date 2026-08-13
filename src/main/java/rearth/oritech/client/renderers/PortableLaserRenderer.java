package rearth.oritech.client.renderers;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import org.jspecify.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.client.renderers.blocks.EndericLaserRenderer;
import rearth.oritech.client.renderers.models.PortableLaserModel;
import rearth.oritech.client.renderers.util.BeamRenderer;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.item.tools.PortableLaserItem;

import static rearth.oritech.client.renderers.blocks.EndericLaserRenderer.BEAM_TEXTURE;

public class PortableLaserRenderer extends GeoItemRenderer<PortableLaserItem> {

    // captured during extraction, consumed during submission
    public static final DataTicket<LaserBeamData> BEAM_DATA = DataTicket.create("portable_laser_beam_data", LaserBeamData.class);

    public record LaserBeamData(Vec3 startOffset, Vec3 deltaVec, float thickness) {}

    public PortableLaserRenderer(String modelName) {
        super(new PortableLaserModel(Oritech.id("models/" + modelName)));
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    // extract beam parameters while tool is fired in first person
    @Override
    public void addRenderData(PortableLaserItem animatable, @Nullable RenderData relatedObject, GeoRenderState renderState, float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);

        if (relatedObject == null) return;

        var client = Minecraft.getInstance();
        var player = relatedObject.itemOwner() == null ? null : relatedObject.itemOwner().asLivingEntity();
        var level = client.level;
        // Firing input is client-local. Without this ownership check, firing in third person
        // can put the local player's beam on every rendered portable laser in view.
        if (!(player instanceof Player aimingPlayer) || player != client.player || level == null) return;

        var heldStack = relatedObject.itemStack();

        var singleShotAge = level.getGameTime() - PortableLaserItem.lastSingleShot;
        if (!OritechClient.laserActive && singleShotAge > 10) return;

        if (!heldStack.is(ToolsContent.PORTABLE_LASER)) return;
        if (animatable.getStoredEnergy(heldStack, ItemAccess.forStack(heldStack)) < OritechStartupConfig.portableLaserConfig.energyPerTick.get())
            return;

        // target calculations
        var startPos = aimingPlayer.getEyePosition();
        var lookVec = aimingPlayer.getViewVector(partialTick);
        var endPos = startPos.add(lookVec.scale(128));

        var hit = PortableLaserItem.getPlayerTargetRay(aimingPlayer);
        if (hit != null && hit.getType().equals(HitResult.Type.MISS))
            endPos = hit.getLocation();

        var dist = (float) endPos.distanceTo(startPos);

        var localStart = new Vec3(0.5, 0.60, 0);
        var deltaVec = new Vec3(0, 0, -dist);
        float baseThickness = (float) (0.03f + Math.sin((level.getGameTime() + partialTick) * 0.6f) * 0.01f);

        renderState.addGeckolibData(BEAM_DATA, new LaserBeamData(localStart, deltaVec, baseThickness));
    }

    // submit straight beam geometry to renderer
    @Override
    public void postRenderPass(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
        super.postRenderPass(renderPassInfo, renderTasks);

        var data = renderPassInfo.getGeckolibData(BEAM_DATA);
        if (data == null) return;

        renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), RenderTypes.eyes(BEAM_TEXTURE), (pose, consumer) -> {
            // core
            BeamRenderer.renderStraightBeam(
                    pose, consumer, data.startOffset(), data.deltaVec(),
                    data.thickness() * 0.3f,
                    RenderHelpers.FULL_BRIGHT,
                    EndericLaserRenderer.CORE_COLOR_START,
                    EndericLaserRenderer.CORE_COLOR_END
            );

            // outer glow
            BeamRenderer.renderStraightBeam(
                    pose, consumer, data.startOffset(), data.deltaVec(),
                    data.thickness(),
                    RenderHelpers.FULL_BRIGHT,
                    EndericLaserRenderer.GLOW_COLOR_START,
                    EndericLaserRenderer.GLOW_COLOR_END
            );
        });
    }
}
