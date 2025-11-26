package rearth.oritech.client.ui.components;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class ReactorBlockRenderComponent extends BaseComponent {
    
    private final Minecraft client = Minecraft.getInstance();
    
    public BlockState state;
    private final @Nullable BlockEntity entity;
    public float zIndex;
    public BlockPos pos;
    
    public ReactorBlockRenderComponent(@Nullable BlockState state, @Nullable BlockEntity entity, float zIndex, BlockPos pos) {
        this.state = state;
        this.entity = entity;
        this.zIndex = zIndex;
        this.pos = pos;
    }
    
    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        
        var usedState = this.state == null ? client.level.getBlockState(pos) : state;
        
        context.pose().pushPose();
        
        context.pose().translate(x + this.width / 2f, y + this.height / 2f, zIndex * 25 + 1000);
        context.pose().scale(40 * this.width / 64f, -40 * this.height / 64f, 40);
        
        context.pose().mulPose(Axis.XP.rotationDegrees(30));
        context.pose().mulPose(Axis.YP.rotationDegrees(45 + 180));
        
        context.pose().translate(-.5, -.5, -.5);
        
        RenderSystem.runAsFancy(() -> {
            final var vertexConsumers = client.renderBuffers().bufferSource();
            if (usedState.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
                this.client.getBlockRenderer().renderSingleBlock(
                  usedState, context.pose(), vertexConsumers,
                  LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
                );
            }
            
            if (this.entity != null) {
                var renderer = this.client.getBlockEntityRenderDispatcher().getRenderer(this.entity);
                if (renderer != null) {
                    renderer.render(entity, partialTicks, context.pose(), vertexConsumers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                }
            }
            
            RenderSystem.setShaderLights(new Vector3f(-1.5f, -.5f, 0), new Vector3f(0, -1, 0));
            vertexConsumers.endBatch();
            Lighting.setupFor3DItems();
        });
        
        context.pose().popPose();
    }
}
