package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.client.renderers.util.BeamRenderer;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.item.tools.PortableLaserItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import static rearth.oritech.client.renderers.LaserArmRenderer.BEAM_TEXTURE;

public class PortableLaserRenderer extends GeoItemRenderer<PortableLaserItem> {
    
    public PortableLaserRenderer(String modelName) {
        super(new PortableLaserModel(Oritech.id("models/" + modelName)));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
    
    @Override
    public void postRender(PoseStack matrices, PortableLaserItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(matrices, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        
        
        var client = Minecraft.getInstance();
        var player = client.player;
        
        if (player == null) return;
        
        var heldStack = client.player.getMainHandItem();
        var world = client.level;
        
        if (isReRender || !this.renderPerspective.firstPerson()) return;
        
        var singleShotAge = world.getGameTime() - PortableLaserItem.lastSingleShot;
        
        if (!OritechClient.laserActive && singleShotAge > 10) return;
        
        if (!heldStack.getItem().equals(ToolsContent.PORTABLE_LASER)) return;
        if (animatable.getStoredEnergy(heldStack) < Oritech.CONFIG.portableLaserConfig.energyPerTick()) return;
        
        // at this point we know a laser is held and fired
        
        var startPos = player.getEyePosition();
        var lookVec = player.getViewVector(0F);
        var endPos = startPos.add(lookVec.scale(128));
        
        var hit = PortableLaserItem.getPlayerTargetRay(player);
        if (hit != null && hit.getType().equals(HitResult.Type.MISS))
            endPos = hit.getLocation();
        
        var dist = (float) endPos.distanceTo(startPos);
        
        matrices.pushPose();
        
        var localStart = new Vec3(0, 0.05, 0);
        
        var deltaVec = new Vec3(0, 0, -dist);
        
        var beamConsumer = bufferSource.getBuffer(RenderType.eyes(BEAM_TEXTURE));
        
        float baseThickness = (float) (0.03f + Math.sin((world.getGameTime() + partialTick) * 1.1f) * 0.01f);
        
        BeamRenderer.renderStraightBeam(
          matrices, beamConsumer, localStart, deltaVec,
          baseThickness * 0.3f,
          LightTexture.FULL_BRIGHT,
          LaserArmRenderer.CORE_COLOR_START,
          LaserArmRenderer.CORE_COLOR_END
        );
        
        BeamRenderer.renderStraightBeam(
          matrices, beamConsumer, localStart, deltaVec,
          baseThickness,
          LightTexture.FULL_BRIGHT,
          LaserArmRenderer.GLOW_COLOR_START,
          LaserArmRenderer.GLOW_COLOR_END
        );
        
        matrices.popPose();
        
    }
}
