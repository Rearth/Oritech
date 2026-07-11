package rearth.oritech.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.client.cablesurfer.ZiplineRenderState;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends HumanoidRenderState> {

    @Shadow
    public ModelPart rightArm;
    @Shadow
    public ModelPart leftArm;
    @Shadow
    public ModelPart rightLeg;
    @Shadow
    public ModelPart leftLeg;
    @Shadow
    public ModelPart head;
    @Shadow
    public ModelPart body;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    public void oritech$ziplineAnim(T state, CallbackInfo ci) {

        if (state instanceof ZiplineRenderState ziplineState && ziplineState.oritech$isZiplining()) {

            float rightArmY = this.rightArm.y;
            float leftArmY = this.leftArm.y;

            // Raise arms overhead
            // -3.14 (PI) is straight up. We do -2.9 to angle slightly forward
            float armX = -2.9F;
            this.rightArm.xRot = armX;
            this.leftArm.xRot = armX;

            // Bring arms inward to touch hands
            // Right arm rotates CCW (-), Left arm CW (+)
            this.rightArm.zRot = 0.1F;
            this.leftArm.zRot = 0.05F;

            // Rotate arms inward on Y axis to align the Wrench handle horizontally
            this.rightArm.yRot = 1.8F;
            this.leftArm.yRot = 0.3F;

            // move slightly up because the default arms are too short
            this.rightArm.y = rightArmY - 3.0F;
            this.leftArm.y = leftArmY - 3.0F;

            this.rightArm.yScale = 1.15F;
            this.leftArm.yScale = 1.15F;


            // Apply a slight swing based on momentum/time
            // We use ageInTicks to create a gentle oscillation
            float legSwing = Mth.cos(state.ageInTicks * 0.2F) * 0.25F;

            // Bend knees slightly back (positive X)
            float kneeBend = 0.4F;

            this.rightLeg.xRot = kneeBend + legSwing;
            this.leftLeg.xRot = kneeBend - legSwing;

            this.rightLeg.yRot = 0.0F;
            this.leftLeg.yRot = 0.0F;
            this.rightLeg.zRot = 0.0F;
            this.leftLeg.zRot = 0.0F;

            // Tilt body slightly forward to look dynamic
            this.body.xRot = 0.1F;
        }
    }
}
