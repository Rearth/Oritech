package rearth.oritech.spaceage.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.survey.EarthScan;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {

    @Unique
    private static final RenderType oritechSpaceAge$SURVEY_TABLET =
            RenderTypes.text(OritechSpaceAge.id("textures/map/earth_survey_tablet.png"));

    @ModifyVariable(method = "renderMap", at = @At("STORE"), ordinal = 0)
    private RenderType oritechSpaceAge$useSurveyTablet(RenderType background, PoseStack pose, SubmitNodeCollector collector,
                                                      int light, ItemStack stack) {

        // Change the frame for survey maps while vanilla draws their terrain, markers, and player position.
        return EarthScan.MAP_MODEL.equals(stack.get(DataComponents.ITEM_MODEL)) ? oritechSpaceAge$SURVEY_TABLET : background;
    }
}
