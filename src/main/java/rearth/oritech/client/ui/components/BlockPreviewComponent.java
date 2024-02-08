package rearth.oritech.client.ui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class BlockPreviewComponent extends BaseComponent {
    
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final BlockState state;
    private final @Nullable BlockEntity entity;
    private final Vec3i offset;
    private final float mouseRotationSpeed;
    
    private float mouseRotation;
    
    public BlockPreviewComponent(BlockState state, @Nullable BlockEntity entity, Vec3i offset) {
        this(state, entity, offset, 0);
    }
    
    public BlockPreviewComponent(BlockState state, @Nullable BlockEntity entity, Vec3i offset, float mouseRotationSpeed) {
        this.state = state;
        this.entity = entity;
        this.offset = offset;
        this.mouseRotationSpeed = mouseRotationSpeed;
    }
    
    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        context.getMatrices().push();
        
        context.getMatrices().translate(x + this.width / 2f, y + this.height / 2f, 100);
        context.getMatrices().scale(40 * this.width / 64f, -40 * this.height / 64f, 40);
        
        context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(30));
        context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 + 180 + mouseRotation));
        
        mouseRotation += mouseRotationSpeed;
        
        context.getMatrices().translate(-.5 + offset.getX(), -.5 + offset.getY(), -.5 + offset.getZ());
        
        RenderSystem.runAsFancy(() -> {
            final var vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();
            if (this.state.getRenderType() != BlockRenderType.ENTITYBLOCK_ANIMATED) {
                this.client.getBlockRenderManager().renderBlockAsEntity(
                  this.state, context.getMatrices(), vertexConsumers,
                  LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV
                );
            }
            
            if (this.entity != null) {
                var entityRenderer = this.client.getBlockEntityRenderDispatcher().get(this.entity);
                if (entityRenderer != null) {
                    entityRenderer.render(entity, partialTicks, context.getMatrices(), vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
                }
            }
            
            RenderSystem.setShaderLights(new Vector3f(-1.5f, -.5f, 0), new Vector3f(0, -1, 0));
            vertexConsumers.draw();
            DiffuseLighting.enableGuiDepthLighting();
        });
        
        context.getMatrices().pop();
    }
}
