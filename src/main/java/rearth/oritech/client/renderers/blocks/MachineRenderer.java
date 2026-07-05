package rearth.oritech.client.renderers.blocks;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import rearth.oritech.client.renderers.models.MachineModel;
import rearth.oritech.util.ColorableMachine;

public class MachineRenderer<T extends BlockEntity & GeoAnimatable, R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<T, R> {

    public static final DataTicket<ColorableMachine.ColorVariant> TEXTURE_OVERRIDE_TICKET = DataTicket.create("machine_color", ColorableMachine.ColorVariant.class);

    public MachineRenderer(BlockEntityRendererProvider.Context context, String modelPath) {
        this(context, modelPath, false);
    }
    public MachineRenderer(BlockEntityRendererProvider.Context context, String modelPath, boolean glowing) {
        super(context, new MachineModel<>(modelPath));

        if (glowing) {
            withRenderLayer(new AutoGlowingGeoLayer<>(this) {
                @Override
                protected boolean shouldRespectWorldLighting(R renderState) {
                    return true;
                }
            });
        }
    }

    // add machine color to state (can be empty, meaning default orange)
    @Override
    public void addRenderData(T animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        addColorRenderData(animatable, renderState);
    }

    public static void addColorRenderData(Object animatable, GeoRenderState renderState) {
        if (animatable instanceof ColorableMachine colorableMachine && colorableMachine.supportRecoloring()) {
            var color = colorableMachine.getCurrentColor();

            if (color.equals(ColorableMachine.ColorVariant.ORANGE)) return;
            renderState.addGeckolibData(TEXTURE_OVERRIDE_TICKET, color);
        }
    }

    @Override
    protected void tryRotateByBlockstate(RenderPassInfo<R> renderPassInfo, PoseStack poseStack) {

        var facing = renderPassInfo.getOrDefaultGeckolibData(DIRECTION_FACING, Direction.NORTH);

        if (facing.equals(Direction.UP)) {
            poseStack.translate(0, 0.5, -0.5);
        } else if (facing.equals(Direction.DOWN)) {
            poseStack.translate(0, 0.5, 0.5);
        }

        super.tryRotateByBlockstate(renderPassInfo, poseStack);
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntity blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), 4, 4, 4);
    }
}


