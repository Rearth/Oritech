package rearth.oritech.spaceage.block.trophy;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import rearth.oritech.spaceage.OritechSpaceAge;

import java.util.function.Consumer;

public final class SpaceTrophyItem extends BlockItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SpaceTrophyItem(Block block, Properties properties) {

        super(block, properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {

        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<SpaceTrophyItem> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {

                if (renderer == null) renderer = new GeoItemRenderer<>(new Model());
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {

        return cache;
    }

    private static final class Model extends GeoModel<SpaceTrophyItem> {

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return OritechSpaceAge.id("space_trophy");
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return OritechSpaceAge.id("textures/models/space_trophy.png");
        }

        @Override
        public Identifier getAnimationResource(SpaceTrophyItem item) {
            return OritechSpaceAge.id("space_trophy");
        }
    }
}
