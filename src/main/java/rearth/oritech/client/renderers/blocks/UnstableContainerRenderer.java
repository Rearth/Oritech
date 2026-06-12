package rearth.oritech.client.renderers.blocks;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.block.entity.storage.UnstableContainerBlockEntity;
import rearth.oritech.client.renderers.util.RenderHelpers;

public class UnstableContainerRenderer<R extends BlockEntityRenderState & GeoRenderState> extends MachineRenderer<UnstableContainerBlockEntity, R> {

    private final ItemModelResolver itemModelResolver;

    public static final DataTicket<ContainerBlockData> CONTAINMENT_DATA = DataTicket.create("contained_item", ContainerBlockData.class);

    public UnstableContainerRenderer(BlockEntityRendererProvider.Context context, String modelPath) {
        super(context, modelPath);
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public void addRenderData(UnstableContainerBlockEntity animatable, @org.jspecify.annotations.Nullable Void relatedObject, R renderState, float partialTick) {

        if (animatable.capturedBlock == null) return;

        var time = animatable.getLevel().getGameTime() + partialTick;
        var rotationY = (time * 10) % 360;

        var itemState = new ItemStackRenderState();
        itemModelResolver.updateForTopItem(itemState, new ItemStack(animatable.capturedBlock.getBlock().asItem()), ItemDisplayContext.FIXED, animatable.getLevel(), null, 0);

        renderState.getOrDefaultGeckolibData(CONTAINMENT_DATA, new ContainerBlockData(itemState, rotationY));
    }

    @Override
    public void postRenderPass(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {

        var data = renderPassInfo.getGeckolibData(CONTAINMENT_DATA);
        if (data == null) return;

        var poseStack = renderPassInfo.poseStack();
        var rotationY = data.rotation();
        var itemModel = data.item();

        poseStack.pushPose();
        poseStack.scale(0.6f, 0.6f, 0.6f);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        poseStack.translate(-0.5, 0, -0.5);

        itemModel.submit(poseStack, renderTasks, RenderHelpers.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();

    }

    public record ContainerBlockData(ItemStackRenderState item, float rotation) {}
}
