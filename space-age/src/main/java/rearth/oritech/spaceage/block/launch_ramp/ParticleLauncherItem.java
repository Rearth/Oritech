package rearth.oritech.spaceage.block.launch_ramp;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.spaceage.OritechSpaceAge;

import java.util.function.Consumer;

public final class ParticleLauncherItem extends BlockItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ParticleLauncherItem(Block block, Properties properties) {

        super(block, properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {

        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<ParticleLauncherItem> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {

                if (renderer == null) renderer = new GeoItemRenderer<>(new Model());
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        controllers.add(new AnimationController<>("machine", 0, state -> state.setAndContinue(MachineBlockEntity.PACKAGED)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {

        return cache;
    }

    private static final class Model extends GeoModel<ParticleLauncherItem> {

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return OritechSpaceAge.id("particle_launcher");
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return OritechSpaceAge.id("textures/models/particle_launcher.png");
        }

        @Override
        public Identifier getAnimationResource(ParticleLauncherItem item) {
            return OritechSpaceAge.id("particle_launcher");
        }
    }
}
