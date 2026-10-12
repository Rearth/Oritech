package rearth.oritech.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.models.OritechBlockGeoModel;
import rearth.oritech.util.ColorableMachine;

import java.util.function.Consumer;

public class OritechGeoItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final float scale;
    private final String name;
    private final ColorableMachine.ColorVariant defaultColor;

    public OritechGeoItem(Block block, Properties settings, float scale, String name, ColorableMachine.ColorVariant defaultColor) {
        super(block, settings);
        this.scale = scale;
        this.name = name;
        this.defaultColor = defaultColor;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        if (getBlock() instanceof TooltipProvider tooltipProvider) {
            tooltipProvider.addToTooltip(context, builder, tooltipFlag, itemStack);
        }
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {

        consumer.accept(new GeoRenderProvider() {
            GeoItemRenderer<OritechGeoItem> renderer = null;

            @Override
            public @Nullable GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new GeoItemRenderer<>(new DefaultColoredBlockItemGeoModel(Oritech.id("models/" + name), defaultColor));

                this.renderer.withScale(scale);

                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private static class DefaultColoredBlockItemGeoModel extends OritechBlockGeoModel<OritechGeoItem> {
        private final ColorableMachine.ColorVariant defaultColor;

        private DefaultColoredBlockItemGeoModel(Identifier assetSubpath, ColorableMachine.ColorVariant defaultColor) {
            super(assetSubpath);
            this.defaultColor = defaultColor;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState renderState) {
            return ColorableMachine.getTextureForColor(super.getTextureResource(renderState), defaultColor);
        }
    }
}
