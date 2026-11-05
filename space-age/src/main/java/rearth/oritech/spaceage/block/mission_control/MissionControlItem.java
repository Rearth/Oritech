package rearth.oritech.spaceage.block.mission_control;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.core.Direction;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import rearth.oritech.spaceage.OritechSpaceAge;

import java.util.function.Consumer;

public final class MissionControlItem extends BlockItem implements GeoItem {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MissionControlItem(Block block, Properties properties) {

        super(block, properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {

        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<MissionControlItem> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {

                if (renderer == null) renderer = new GeoItemRenderer<>(new Model()) {
                    @Override
                    public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> pass, BoneSnapshots bones) {

                        for (var direction : Direction.Plane.HORIZONTAL)
                            bones.ifPresent("antenna_connection_" + direction.getSerializedName(), bone -> bone.skipRender(true));
                    }
                };
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        controllers.add(new AnimationController<>("idle", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {

        return cache;
    }

    private static final class Model extends GeoModel<MissionControlItem> {

        @Override
        public Identifier getModelResource(GeoRenderState state) {

            return OritechSpaceAge.id("mission_control");
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {

            return OritechSpaceAge.id("textures/models/mission_control.png");
        }

        @Override
        public Identifier getAnimationResource(MissionControlItem item) {

            return OritechSpaceAge.id("mission_control");
        }
    }
}
