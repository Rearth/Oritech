package rearth.oritech.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
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
    public void postRender(MatrixStack matrices, PortableLaserItem animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(matrices, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        
        var client = MinecraftClient.getInstance();
        var player = client.player;
        var heldStack = client.player.getMainHandStack();
        var world = client.world;
        
        if (isReRender || !this.renderPerspective.isFirstPerson()) return;
        
        var singleShotAge = world.getTime() - PortableLaserItem.lastSingleShot;
        
        if (!OritechClient.laserActive && singleShotAge > 10) return;
        
        if (!heldStack.getItem().equals(ToolsContent.PORTABLE_LASER)) return;
        if (animatable.getStoredEnergy(heldStack) < Oritech.CONFIG.portableLaserConfig.energyPerTick()) return;
        
        // at this point we know a laser is held and fired
        
        var startPos = player.getEyePos();
        var lookVec = player.getRotationVec(0F);
        var endPos = startPos.add(lookVec.multiply(128));
        
        var hit = PortableLaserItem.getPlayerTargetRay(player);
        if (hit != null && hit.getType().equals(HitResult.Type.MISS))
            endPos = hit.getPos();
        
        var dist = (float) endPos.distanceTo(startPos);
        
        matrices.push();
        
        var lineConsumer = bufferSource.getBuffer(LaserArmRenderer.CUSTOM_LINES);
        RenderSystem.lineWidth((float) (3 + Math.sin((world.getTime() + partialTick) * 1.1f) * 1));
        
        var startOffset = new Vector3f(0, 0.05f, 0);
        var endOffset = new Vector3f(0, 0, -dist);
        var cross = endPos.subtract(startPos).normalize().crossProduct(new Vec3d(0, 1, 0));
        
        
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), startOffset.x, startOffset.y, startOffset.z)
          .color(138, 242, 223, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal(0, 1, 0);
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), endOffset.x, endOffset.y, endOffset.z)
          .color(19, 91, 80, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal(1, 0, 0);
        
        // render a second one at right angle to first one
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), startOffset.x, startOffset.y, startOffset.z)
          .color(138, 242, 223, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal((float) cross.x, (float) cross.y, (float) cross.z);
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), endOffset.x, endOffset.y, endOffset.z)
          .color(19, 91, 80, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal((float) cross.x, (float) cross.y, (float) cross.z);
        
        matrices.pop();
        
    }
}
