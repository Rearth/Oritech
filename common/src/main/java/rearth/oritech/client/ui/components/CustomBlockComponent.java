package rearth.oritech.client.ui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CustomBlockComponent extends BaseComponent {
    
    private final Minecraft client = Minecraft.getInstance();
    
    private final BlockState state;
    private final @Nullable BlockEntity entity;
    
    public CustomBlockComponent(BlockState state, @Nullable BlockEntity entity) {
        this.state = state;
        this.entity = entity;
    }
    
    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        context.pose().pushPose();
        
        context.pose().translate(x + this.width / 2f, y + this.height / 2f - this.height * 0.15f, 100);
        context.pose().scale(40 * this.width / 64f, -40 * this.height / 64f, 40);
        context.pose().scale(1.3f, 1.3f, 1.3f);
        
        context.pose().mulPose(Axis.XP.rotationDegrees(-30));
        context.pose().mulPose(Axis.YP.rotationDegrees(180));
        context.pose().mulPose(Axis.ZP.rotationDegrees(45));
        
        context.pose().translate(-.5, -.5, -.5);
        
        // renders unlit
        RenderSystem.runAsFancy(() -> {
            final var vertexConsumers = client.renderBuffers().bufferSource();
            if (this.state.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
                this.client.getBlockRenderer().renderSingleBlock(
                  this.state, context.pose(), vertexConsumers,
                  LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
                );
            }
            
            // RenderSystem.setShaderLights(new Vector3f(-1.5f, -.5f, 0), new Vector3f(0, -1, 0));
            vertexConsumers.endBatch();
            // Lighting.setupFor3DItems();
        });
        
        context.pose().popPose();
    }
}
