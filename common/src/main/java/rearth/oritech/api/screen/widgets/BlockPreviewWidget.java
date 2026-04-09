package rearth.oritech.api.screen.widgets;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders multiple blocks in a 3D isometric preview with shared auto-rotation.
 * Each block is placed at a Vec3i offset from the center.
 */
public class BlockPreviewWidget extends UIComponent {
    private static final float X_ROTATION = 30f;
    private static final float X_ROTATION_COS = (float) Math.cos(Math.toRadians(X_ROTATION));
    private static final float X_ROTATION_SIN = (float) Math.sin(Math.toRadians(X_ROTATION));
    private static final float SCALE_MARGIN = 0.98f;
    
    public record BlockEntry(BlockState state, @Nullable BlockEntity entity, Vec3i offset) {}
    
    private final List<BlockEntry> blocks = new ArrayList<>();
    private float rotation;
    private float rotationSpeed = 0.2f;
    private float maxHorizontalRadius;
    private float maxVerticalRadius;
    private float centerX;
    private float centerY;
    private float centerZ;
    private boolean scaleDirty = true;
    
    public BlockPreviewWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }
    
    public BlockPreviewWidget withRotationSpeed(float speed) {
        this.rotationSpeed = speed;
        return this;
    }
    
    public void addBlock(BlockState state, @Nullable BlockEntity entity, Vec3i offset) {
        blocks.add(new BlockEntry(state, entity, offset));
        scaleDirty = true;
    }
    
    public void calculateSize() {
        if (blocks.isEmpty()) {
            maxHorizontalRadius = 0f;
            maxVerticalRadius = 0f;
            centerX = 0f;
            centerY = 0f;
            centerZ = 0f;
            scaleDirty = false;
            return;
        }

        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (var entry : blocks) {
            for (var offset : getPreviewPositions(entry)) {
                minX = Math.min(minX, offset.getX() - 0.5f);
                maxX = Math.max(maxX, offset.getX() + 0.5f);
                minY = Math.min(minY, offset.getY() - 0.5f);
                maxY = Math.max(maxY, offset.getY() + 0.5f);
                minZ = Math.min(minZ, offset.getZ() - 0.5f);
                maxZ = Math.max(maxZ, offset.getZ() + 0.5f);
            }
        }
        

        centerX = (minX + maxX) * 0.5f;
        centerY = (minY + maxY) * 0.5f;
        centerZ = (minZ + maxZ) * 0.5f;

        float horizontalRadius = 0f;
        float verticalRadius = 0f;

        for (var entry : blocks) {
            for (var offset : getPreviewPositions(entry)) {
                float blockMinX = offset.getX() - 0.5f;
                float blockMaxX = offset.getX() + 0.5f;
                float blockMinY = offset.getY() - 0.5f;
                float blockMaxY = offset.getY() + 0.5f;
                float blockMinZ = offset.getZ() - 0.5f;
                float blockMaxZ = offset.getZ() + 0.5f;

                float[] xValues = {blockMinX, blockMaxX};
                float[] yValues = {blockMinY, blockMaxY};
                float[] zValues = {blockMinZ, blockMaxZ};

                // Fit against the full Y rotation by checking the furthest block corners from the origin.
                for (float x : xValues) {
                    for (float y : yValues) {
                        for (float z : zValues) {
                            float centeredX = x - centerX;
                            float centeredY = y - centerY;
                            float centeredZ = z - centerZ;
                            float horizontalDistance = (float) Math.hypot(centeredX, centeredZ);
                            horizontalRadius = Math.max(horizontalRadius, horizontalDistance);
                            verticalRadius = Math.max(verticalRadius,
                                Math.abs(centeredY) * X_ROTATION_COS + horizontalDistance * X_ROTATION_SIN);
                        }
                    }
                }
            }
        }

        maxHorizontalRadius = horizontalRadius;
        maxVerticalRadius = verticalRadius;
        scaleDirty = false;
    }
    
    public void clearBlocks() {
        blocks.clear();
        scaleDirty = true;
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (blocks.isEmpty()) return;
        
        var client = Minecraft.getInstance();
        int cx = contentX();
        int cy = contentY();
        float cw = contentWidth();
        float ch = contentHeight();
        float scale = getScale(cw, ch);
        if (scale <= 0f) return;
        
        for (var entry : blocks) {
            graphics.pose().pushPose();
            
            graphics.pose().translate(cx + cw / 2f, cy + ch / 2f, 400);
            graphics.pose().scale(scale, -scale, scale);
            graphics.pose().mulPose(Axis.XP.rotationDegrees(X_ROTATION));
            graphics.pose().mulPose(Axis.YP.rotationDegrees(225 + rotation));
            
            graphics.pose().translate(
                -0.5f + entry.offset.getX() - centerX,
                -0.5f + entry.offset.getY() - centerY,
                -0.5f + entry.offset.getZ() - centerZ
            );
            
            RenderSystem.runAsFancy(() -> {
                var bufferSource = client.renderBuffers().bufferSource();
                
                if (entry.state.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
                    client.getBlockRenderer().renderSingleBlock(
                        entry.state, graphics.pose(), bufferSource,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
                    );
                }
                
                if (entry.entity != null) {
                    var entityRenderer = client.getBlockEntityRenderDispatcher().getRenderer(entry.entity);
                    if (entityRenderer != null) {
                        entityRenderer.render(entry.entity, delta, graphics.pose(), bufferSource,
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    }
                }
                
                RenderSystem.setShaderLights(new Vector3f(-1.5f, -0.5f, 0), new Vector3f(0, -1, 0));
                bufferSource.endBatch();
                Lighting.setupFor3DItems();
            });
            
            graphics.pose().popPose();
        }
        
        rotation += rotationSpeed;
    }

    private float getScale(float availableWidth, float availableHeight) {
        if (scaleDirty) {
            calculateSize();
        }

        if (maxHorizontalRadius <= 0f || maxVerticalRadius <= 0f) {
            return 0f;
        }

        float widthScale = availableWidth * 0.5f / maxHorizontalRadius;
        float heightScale = availableHeight * 0.5f / maxVerticalRadius;
        return Math.min(widthScale, heightScale) * SCALE_MARGIN;
    }

    private List<Vec3i> getPreviewPositions(BlockEntry entry) {
        var positions = new ArrayList<Vec3i>();
        positions.add(entry.offset());

        if (entry.entity() instanceof MultiblockMachineController multiblock) {
            var facing = multiblock.getFacingForMultiblock();
            for (var relativeOffset : multiblock.getCorePositions()) {
                positions.add(Geometry.rotatePosition(relativeOffset, facing).offset(entry.offset()));
            }
        }

        return positions;
    }
}
