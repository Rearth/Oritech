package rearth.oritech.client.renderers.blocks;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import rearth.oritech.block.entity.generators.BigSolarPanelEntity;
import rearth.oritech.client.renderers.models.SolarPanelModel;
import rearth.oritech.client.renderers.models.EndericLaserModel;
import rearth.oritech.util.Geometry;
import org.jspecify.annotations.Nullable;

public class SolarPanelRenderer<T extends BigSolarPanelEntity & GeoAnimatable, R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<T, R> {

    public static final DataTicket<Float> TARGET_ANGLE_TICKET = DataTicket.create("solar_target_angle", Float.class);

    public SolarPanelRenderer(BlockEntityRendererProvider.Context context, String modelPath) {
        super(context, new SolarPanelModel<>(modelPath));
    }

    @Override
    public void addRenderData(T solarEntity, @Nullable Void relatedObject, R renderState, float partialTick) {
        super.addRenderData(solarEntity, relatedObject, renderState, partialTick);

        var timeOfDay = solarEntity.getAdjustedTimeOfDay();
        var targetAngle = 0f;
        if (timeOfDay <= 13000) {
            var directionPercent = (timeOfDay - 6000) / 6000f;
            var maxAngle = 45;
            targetAngle = directionPercent * maxAngle * Geometry.DEG_TO_RAD;
        }
        renderState.addGeckolibData(TARGET_ANGLE_TICKET, targetAngle);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots boneSnapshots) {
        super.adjustModelBonesForRender(renderPassInfo, boneSnapshots);

        // retrieve current renderState target targetAngle
        var targetAngle = renderPassInfo.getOrDefaultGeckolibData(TARGET_ANGLE_TICKET, 0f);

        boneSnapshots.ifPresent("pivotZ", snapshot -> {
            var lastAngle = snapshot.getRotZ();
            var angle = EndericLaserModel.lerp(lastAngle, targetAngle, 0.06f);
            snapshot.setRotZ(angle);
        });
    }
}
