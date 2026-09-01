package rearth.oritech.api.screen.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.client.ui.render.BlockPreviewRenderState;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Renders blocks in a centered, fitted orthographic preview.
 * <p>
 * A block entry's offset describes the center of that block. Use
 * {@link #getHoveredBlock()} after rendering, or {@link #findBlockAt(double, double)}
 * directly, to determine which entry is under the mouse.
 */
public class BlockPreviewWidget extends UIComponent {
    private static final float DEFAULT_X_ROTATION = 30f;
    private static final float DEFAULT_Y_ROTATION = 225f;
    private static final float SCALE_MARGIN = 0.98f;
    private static final float PICK_RAY_DISTANCE = 1_000_000f;
    private static final float DRAG_SENSITIVITY = 0.45f;
    private static final float MOMENTUM_SMOOTHING = 0.35f;
    private static final float MOMENTUM_DAMPING = 0.86f;
    private static final float MIN_PITCH = -85f;
    private static final float MAX_PITCH = 85f;

    public record BlockEntry(BlockState state, @Nullable BlockEntity entity, Vec3i offset) {
    }

    private final List<BlockEntry> blocks = new ArrayList<>();
    private float rotationX = DEFAULT_X_ROTATION;
    private float rotationY = DEFAULT_Y_ROTATION;
    private float rotation;
    private float rotationSpeed;
    private float yawVelocity;
    private float pitchVelocity;
    private float maxHorizontalRadius;
    private float maxVerticalRadius;
    private float centerX;
    private float centerY;
    private float centerZ;
    private boolean scaleDirty = true;
    private float lastScale;
    private float lastRenderedRotationX = DEFAULT_X_ROTATION;
    private float lastRenderedRotation = DEFAULT_Y_ROTATION;
    private boolean dragRotationEnabled;
    private boolean draggingRotation;
    private @Nullable BlockEntry hoveredBlock;

    public BlockPreviewWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Sets automatic Y-axis rotation in degrees per tick. Zero disables it.
     */
    public BlockPreviewWidget withRotationSpeed(float speed) {
        this.rotationSpeed = speed;
        return this;
    }

    public BlockPreviewWidget withRotation(float xRotation, float yRotation) {
        this.rotationX = Mth.clamp(xRotation, MIN_PITCH, MAX_PITCH);
        this.rotationY = yRotation;
        this.lastRenderedRotationX = this.rotationX;
        this.lastRenderedRotation = yRotation + rotation;
        scaleDirty = true;
        return this;
    }

    /**
     * Enables orbit-style mouse dragging with inertial rotation after release.
     */
    public BlockPreviewWidget withDragRotation() {
        this.dragRotationEnabled = true;
        this.scaleDirty = true;
        return this;
    }

    public void addBlock(BlockState state, @Nullable BlockEntity entity, Vec3i offset) {
        blocks.add(new BlockEntry(state, entity, offset));
        scaleDirty = true;
    }

    public void clearBlocks() {
        blocks.clear();
        hoveredBlock = null;
        scaleDirty = true;
    }

    public List<BlockEntry> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public @Nullable BlockEntry getHoveredBlock() {
        return hoveredBlock;
    }

    /**
     * Finds the front-most block whose unit cube contains the mouse ray.
     */
    public @Nullable BlockEntry findBlockAt(double mouseX, double mouseY) {
        if (blocks.isEmpty() || !isMouseOver(mouseX, mouseY)) {
            return null;
        }

        float scale = getScale(contentWidth(), contentHeight());
        if (scale <= 0f) {
            return null;
        }

        float screenX = ((float) mouseX - (contentX() + contentWidth() * 0.5f)) / scale;
        float screenY = -((float) mouseY - (contentY() + contentHeight() * 0.5f)) / scale;

        var inverseRotation = createRotationMatrix(lastRenderedRotationX, lastRenderedRotation).invert();
        var rayOrigin = inverseRotation.transform(new Vector3f(screenX, screenY, PICK_RAY_DISTANCE));
        var rayDirection = inverseRotation.transform(new Vector3f(0f, 0f, -1f));

        BlockEntry closest = null;
        float closestDistance = Float.POSITIVE_INFINITY;
        for (var entry : blocks) {
            float distance = intersectUnitCube(rayOrigin, rayDirection, entry.offset());
            if (distance >= 0f && distance < closestDistance) {
                closestDistance = distance;
                closest = entry;
            }
        }
        return closest;
    }

    @Override
    public void tick() {
        rotation = wrapDegrees(rotation + rotationSpeed);

        if (dragRotationEnabled && !draggingRotation) {
            rotation = wrapDegrees(rotation + yawVelocity);
            rotationX = Mth.clamp(rotationX + pitchVelocity, MIN_PITCH, MAX_PITCH);

            if (rotationX == MIN_PITCH || rotationX == MAX_PITCH) {
                pitchVelocity = 0f;
            }
            yawVelocity = damp(yawVelocity);
            pitchVelocity = damp(pitchVelocity);
        }
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (!dragRotationEnabled || button != 0 || !isMouseOver(mouseX, mouseY)) return false;

        draggingRotation = true;
        yawVelocity = 0f;
        pitchVelocity = 0f;
        return true;
    }

    @Override
    public boolean handleDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (!dragRotationEnabled || !draggingRotation || button != 0) return false;

        var yawDelta = (float) deltaX * DRAG_SENSITIVITY;
        var pitchDelta = (float) deltaY * DRAG_SENSITIVITY;
        rotation = wrapDegrees(rotation + yawDelta);
        rotationX = Mth.clamp(rotationX + pitchDelta, MIN_PITCH, MAX_PITCH);

        yawVelocity = Mth.lerp(MOMENTUM_SMOOTHING, yawVelocity, yawDelta);
        pitchVelocity = Mth.lerp(MOMENTUM_SMOOTHING, pitchVelocity, pitchDelta);
        return true;
    }

    @Override
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        if (button != 0 || !draggingRotation) return false;

        draggingRotation = false;
        return true;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (blocks.isEmpty()) {
            hoveredBlock = null;
            return;
        }

        int cx = contentX();
        int cy = contentY();
        int width = contentWidth();
        int height = contentHeight();
        lastScale = getScale(width, height);
        if (lastScale <= 0f) {
            hoveredBlock = null;
            return;
        }

        var momentumDelta = dragRotationEnabled && !draggingRotation ? delta : 0f;
        lastRenderedRotationX = Mth.clamp(rotationX + pitchVelocity * momentumDelta, MIN_PITCH, MAX_PITCH);
        lastRenderedRotation = rotationY + rotation + rotationSpeed * delta + yawVelocity * momentumDelta;
        hoveredBlock = findBlockAt(mouseX, mouseY);

        var entries = new ArrayList<BlockPreviewRenderState.Entry>(blocks.size());
        for (var entry : blocks) {
            entries.add(new BlockPreviewRenderState.Entry(
                    entry.state(), entry.entity(), entry.offset(), 1f, getOverlayCoords(entry)));
        }
        appendRenderEntries(entries);

        graphics.submitPictureInPictureRenderState(new BlockPreviewRenderState(
                List.copyOf(entries),
                lastRenderedRotationX,
                lastRenderedRotation,
                centerX, centerY, centerZ,
                delta,
                cx, cy, cx + width, cy + height,
                lastScale,
                graphics.pose(),
                graphics.peekScissorStack()
        ));
    }

    /**
     * Allows specialized previews to add transient overlays without affecting
     * fitting or mouse picking.
     */
    protected void appendRenderEntries(List<BlockPreviewRenderState.Entry> entries) {
    }

    /**
     * Allows specialized previews to brighten or tint existing blocks without adding replacement models.
     */
    protected int getOverlayCoords(BlockEntry entry) {
        return OverlayTexture.NO_OVERLAY;
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

    private void calculateSize() {
        if (blocks.isEmpty()) {
            maxHorizontalRadius = 0f;
            maxVerticalRadius = 0f;
            centerX = centerY = centerZ = 0f;
            scaleDirty = false;
            return;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (var entry : blocks) {
            for (var position : getPreviewPositions(entry)) {
                minX = Math.min(minX, position.getX() - 0.5f);
                minY = Math.min(minY, position.getY() - 0.5f);
                minZ = Math.min(minZ, position.getZ() - 0.5f);
                maxX = Math.max(maxX, position.getX() + 0.5f);
                maxY = Math.max(maxY, position.getY() + 0.5f);
                maxZ = Math.max(maxZ, position.getZ() + 0.5f);
            }
        }

        centerX = (minX + maxX) * 0.5f;
        centerY = (minY + maxY) * 0.5f;
        centerZ = (minZ + maxZ) * 0.5f;

        float xSin = Math.abs((float) Math.sin(Math.toRadians(rotationX)));
        float xCos = Math.abs((float) Math.cos(Math.toRadians(rotationX)));
        maxHorizontalRadius = 0f;
        maxVerticalRadius = 0f;

        for (var entry : blocks) {
            for (var position : getPreviewPositions(entry)) {
                float horizontalDistance = (float) Math.hypot(
                        Math.abs(position.getX() - centerX) + 0.5f,
                        Math.abs(position.getZ() - centerZ) + 0.5f
                );
                float verticalDistance = Math.abs(position.getY() - centerY) + 0.5f;
                maxHorizontalRadius = Math.max(maxHorizontalRadius, horizontalDistance);
                var projectedVerticalRadius = dragRotationEnabled
                        ? (float) Math.hypot(verticalDistance, horizontalDistance)
                        : verticalDistance * xCos + horizontalDistance * xSin;
                maxVerticalRadius = Math.max(maxVerticalRadius, projectedVerticalRadius);
            }
        }
        scaleDirty = false;
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

    private Matrix3f createRotationMatrix(float xRotation, float yRotation) {
        return new Matrix3f()
                .rotateX((float) Math.toRadians(xRotation))
                .rotateY((float) Math.toRadians(yRotation));
    }

    private static float damp(float velocity) {
        var damped = velocity * MOMENTUM_DAMPING;
        return Math.abs(damped) < 0.001f ? 0f : damped;
    }

    private float intersectUnitCube(Vector3f origin, Vector3f direction, Vec3i offset) {
        float minX = offset.getX() - centerX - 0.5f;
        float minY = offset.getY() - centerY - 0.5f;
        float minZ = offset.getZ() - centerZ - 0.5f;
        float maxX = minX + 1f;
        float maxY = minY + 1f;
        float maxZ = minZ + 1f;

        float near = 0f;
        float far = Float.POSITIVE_INFINITY;
        float[] origins = {origin.x, origin.y, origin.z};
        float[] directions = {direction.x, direction.y, direction.z};
        float[] minimums = {minX, minY, minZ};
        float[] maximums = {maxX, maxY, maxZ};

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(directions[axis]) < 1.0e-6f) {
                if (origins[axis] < minimums[axis] || origins[axis] > maximums[axis]) {
                    return -1f;
                }
                continue;
            }

            float first = (minimums[axis] - origins[axis]) / directions[axis];
            float second = (maximums[axis] - origins[axis]) / directions[axis];
            near = Math.max(near, Math.min(first, second));
            far = Math.min(far, Math.max(first, second));
            if (far < near) {
                return -1f;
            }
        }
        return near;
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360f;
        return wrapped < 0f ? wrapped + 360f : wrapped;
    }
}
