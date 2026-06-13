package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import rearth.oritech.client.renderers.blocks.SmallTankRenderer;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.ColorHelper;

public class SmallTankItemRenderer {

    private final Identifier tankVisualModelId;

    public SmallTankItemRenderer(Identifier tankVisualModelId) {
        this.tankVisualModelId = tankVisualModelId;
    }

    public void submit(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, SubmitNodeCollector collector, int light, int overlay) {

        matrices.pushPose();
        matrices.translate(0, 0.25, 0);
        matrices.scale(0.84f, 0.84f, 0.84f);

        // render the base tank model, resolved from its standalone item model id
        matrices.pushPose();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(0.9f, 0.9f, 0.9f);

        var mc = Minecraft.getInstance();
        var itemModel = mc.getModelManager().getItemModel(tankVisualModelId);
        var modelState = new ItemStackRenderState();
        itemModel.update(modelState, stack, mc.getItemModelResolver(), ItemDisplayContext.NONE, mc.level, null, 0);
        modelState.submit(matrices, collector, light, overlay, 0);

        matrices.popPose();

        // render the contained fluid on top
        var content = stack.getOrDefault(ComponentContent.STORED_FLUID.get(), SimpleFluidContent.EMPTY);
        if (!content.isEmpty()) {
            var fluidStack = content.copy();
            var fill = fluidStack.getAmount() / (float) (OritechConfig.portableTankCapacityBuckets.get() * FluidType.BUCKET_VOLUME);
            var sprite = RenderHelpers.getFluidSprite(fluidStack.getFluid());
            var spriteColor = ColorHelper.makeOpaque(ColorHelper.getFluidTint(fluidStack));

            SmallTankRenderer.submitTankFluid(collector, matrices, sprite, spriteColor, fill, light, overlay);
        }

        matrices.popPose();
    }
}
