package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.client.renderers.blocks.SmallTankRenderer;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.ColorHelper;

import java.util.function.Consumer;

/**
 * Renders the portable tank item: the base tank model plus the contained fluid drawn on top.
 * <p>
 * Wired into the NeoForge 26.1 item-rendering pipeline as a {@link SpecialModelRenderer}. The fluid content is pulled
 * out of the stack during extraction ({@link #extractArgument}) and the geometry is emitted into the submit pipeline
 * during ({@link #submit}). Register the {@link Unbaked#MAP_CODEC} through {@code RegisterSpecialModelRendererEvent}
 * (see {@link OritechClient}).
 * <p>
 * <b>Required item model json</b> (e.g. {@code assets/oritech/items/small_tank_block.json}): the item must use a
 * vanilla {@code minecraft:special} item model that points at this special renderer. The {@code base} field is a
 * plain block model that is only used to supply the GUI/hand transforms and lighting (it is <i>not</i> drawn as
 * geometry - this renderer draws everything). The nested {@code model} block selects this renderer by its registered
 * id ({@link #ID}, {@code "oritech:small_tank"}) and carries the {@code "model"} field consumed by
 * {@link Unbaked#MAP_CODEC}, which is the standalone item-model definition this renderer resolves and draws as the
 * tank body:
 * <pre>{@code
 * {
 *   "model": {
 *     "type": "minecraft:special",
 *     "base": "oritech:block/small_tank_block",
 *     "model": {
 *       "type": "oritech:small_tank",
 *       "model": "oritech:small_tank_block_body"
 *     }
 *   }
 * }
 * }</pre>
 * The {@code creative_tank} item model is identical apart from the {@code base}/{@code model} ids pointing at the
 * creative tank's standalone model.
 */
public class SmallTankItemRenderer implements SpecialModelRenderer<SmallTankItemRenderer.TankContents> {

    public static final Identifier ID = Oritech.id("small_tank");

    private final Identifier tankVisualModelId;

    public SmallTankItemRenderer(Identifier tankVisualModelId) {
        this.tankVisualModelId = tankVisualModelId;
    }

    // captured render state for the tank model and fluid overlay
    public record TankContents(ItemStackRenderState baseModelState, @Nullable TextureAtlasSprite fluidSprite, int fluidColor, float fill) {}

    @Override
    public @Nullable TankContents extractArgument(ItemStack stack) {
        var mc = Minecraft.getInstance();

        // resolve base model
        var baseState = new ItemStackRenderState();
        mc.getModelManager().getItemModel(tankVisualModelId).update(baseState, stack, mc.getItemModelResolver(), ItemDisplayContext.NONE, mc.level, null, 0);

        var content = stack.getOrDefault(ComponentContent.STORED_FLUID.get(), SimpleFluidContent.EMPTY);
        if (content.isEmpty())
            return new TankContents(baseState, null, 0, 0);

        var fluidStack = content.copy();
        var fill = fluidStack.getAmount() / (float) (OritechConfig.portableTankCapacityBuckets.get() * FluidType.BUCKET_VOLUME);
        var sprite = RenderHelpers.getFluidSprite(fluidStack.getFluid());
        var spriteColor = ColorHelper.makeOpaque(ColorHelper.getFluidTint(fluidStack));

        return new TankContents(baseState, sprite, spriteColor, fill);
    }

    @Override
    public void submit(@Nullable TankContents argument, PoseStack matrices, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        if (argument == null) return;

        matrices.pushPose();

        // raw model
        matrices.pushPose();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(0.9f, 0.9f, 0.9f);
        argument.baseModelState().submit(matrices, collector, light, overlay, outlineColor);
        matrices.popPose();

        // fluid content overlay
        if (argument.fluidSprite() != null) {
            SmallTankRenderer.submitTankFluid(collector, matrices, argument.fluidSprite(), argument.fluidColor(), argument.fill(), light, overlay);
        }

        matrices.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        // bounds for frustum culling
        output.accept(new Vector3f(0, 0, 0));
        output.accept(new Vector3f(1, 1.25f, 1));
    }

    // unbaked model holder registered to RegisterSpecialModelRendererEvent
    public record Unbaked(Identifier model) implements SpecialModelRenderer.Unbaked<TankContents> {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model)
                ).apply(instance, Unbaked::new));

        @Override
        public SpecialModelRenderer<TankContents> bake(SpecialModelRenderer.BakingContext context) {
            return new SmallTankItemRenderer(this.model);
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<TankContents>> type() {
            return MAP_CODEC;
        }
    }
}
