package rearth.oritech.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.item.tools.PortableLaserItem;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

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
        
        var lineConsumer = bufferSource.getBuffer(LaserArmRenderer.CUSTOM_LINES);
        RenderSystem.lineWidth((float) (3 + Math.sin((world.getGameTime() + partialTick) * 1.1f) * 1));
        
        var startOffset = new Vector3f(0, 0.05f, 0);
        var endOffset = new Vector3f(0, 0, -dist);
        var cross = endPos.subtract(startPos).normalize().cross(new Vec3(0, 1, 0));
        
        
        lineConsumer.addVertex(matrices.last().pose(), startOffset.x, startOffset.y, startOffset.z)
          .setColor(138, 242, 223, 255)
          .setLight(packedLight)
          .setOverlay(packedOverlay)
          .setNormal(0, 1, 0);
        lineConsumer.addVertex(matrices.last().pose(), endOffset.x, endOffset.y, endOffset.z)
          .setColor(19, 91, 80, 255)
          .setLight(packedLight)
          .setOverlay(packedOverlay)
          .setNormal(1, 0, 0);
        
        // render a second one at right angle to first one
        lineConsumer.addVertex(matrices.last().pose(), startOffset.x, startOffset.y, startOffset.z)
          .setColor(138, 242, 223, 255)
          .setLight(packedLight)
          .setOverlay(packedOverlay)
          .setNormal((float) cross.x, (float) cross.y, (float) cross.z);
        lineConsumer.addVertex(matrices.last().pose(), endOffset.x, endOffset.y, endOffset.z)
          .setColor(19, 91, 80, 255)
          .setLight(packedLight)
          .setOverlay(packedOverlay)
          .setNormal((float) cross.x, (float) cross.y, (float) cross.z);
        
        matrices.popPose();
        
    }
}
