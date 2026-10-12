package rearth.oritech.client.renderers.blocks;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.Oritech;

/**
 * A GeckoLib block renderer that uses the model's visible bounds for frustum culling.
 * GeckoLib loads these values from the geo model but does not apply them to block
 * entity renderers itself.
 */
public class ModelBoundedGeoBlockRenderer<T extends BlockEntity & GeoAnimatable, R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<T, R> {

    private final Identifier modelResource;

    protected ModelBoundedGeoBlockRenderer(BlockEntityRendererProvider.Context context, GeoModel<T> model, String modelPath) {
        super(context, model);
        this.modelResource = Oritech.id(modelPath).withPrefix("block/");
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        var properties = model.getBakedModel(modelResource).properties();
        var width = properties.visibleBoundsWidth();
        var height = properties.visibleBoundsHeight();

        if (width == null || height == null)
            return new AABB(blockEntity.getBlockPos());

        var offset = properties.visibleBoundsOffset();
        if (offset == null)
            offset = Vec3.ZERO;

        // Models may be placed on walls or ceilings. Use the largest axis extent so
        // the bounds remain valid after any of the renderer's 90-degree rotations.
        var radius = Math.max(
                Math.max(Math.abs(offset.x) + width / 2, Math.abs(offset.z) + width / 2),
                Math.abs(offset.y - 0.5) + height / 2);

        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), radius * 2, radius * 2, radius * 2);
    }
}
