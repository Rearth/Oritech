package rearth.oritech.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets custom fluids reuse vanilla swimming physics. */
@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {

    @Invoker("travelInWater")
    void oritech$travelInWater(Vec3 input, double gravity, boolean falling, double oldY);
}
