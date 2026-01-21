package rearth.oritech.client.ui.components;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class BlockPreviewComponent extends BaseComponent {
    
    private final Minecraft client = Minecraft.getInstance();
    private final BlockState state;
    private final @Nullable BlockEntity entity;
    private final Vec3i offset;
    private final float mouseRotationSpeed;
    
    private float mouseRotation;
    
    public BlockPreviewComponent(BlockState state, @Nullable BlockEntity entity, Vec3i offset, float mouseRotationSpeed) {
        this.state = state;
        this.entity = entity;
        this.offset = offset;
        this.mouseRotationSpeed = mouseRotationSpeed;
    }
    
    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        context.pose().pushPose();
        
        context.pose().translate(x + this.width / 2f, y + this.height / 2f, 400);
        context.pose().scale(40 * this.width / 64f, -40 * this.height / 64f, 40);
        
        context.pose().mulPose(Axis.XP.rotationDegrees(30));
        context.pose().mulPose(Axis.YP.rotationDegrees(45 + 180 + mouseRotation));
        
        mouseRotation += mouseRotationSpeed;
        
        context.pose().translate(-.5 + offset.getX(), -.5 + offset.getY(), -.5 + offset.getZ());
        RenderSystem.runAsFancy(() -> {
            final var vertexConsumers = client.renderBuffers().bufferSource();
            if (this.state.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
                this.client.getBlockRenderer().renderSingleBlock(
                  this.state, context.pose(), vertexConsumers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
                );
            }
            
            if (this.entity != null) {
                var entityRenderer = this.client.getBlockEntityRenderDispatcher().getRenderer(this.entity);
                if (entityRenderer != null) {
                    entityRenderer.render(entity, partialTicks, context.pose(), vertexConsumers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                }
            }
            
            RenderSystem.setShaderLights(new Vector3f(-1.5f, -.5f, 0), new Vector3f(0, -1, 0));
            vertexConsumers.endBatch();
            Lighting.setupFor3DItems();
        });
        
        context.pose().popPose();
    }
}
