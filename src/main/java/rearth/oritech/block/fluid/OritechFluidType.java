package rearth.oritech.block.fluid;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import rearth.oritech.mixin.LivingEntityInvoker;

/** Water movement with a per-fluid swimming speed multiplier. */
public class OritechFluidType extends FluidType {

    private final double movementScale;

    public OritechFluidType(Properties properties, double movementScale) {
        super(properties.isWaterLike(true));
        this.movementScale = movementScale;
    }

    @Override
    public boolean move(LivingEntity entity, Vec3 movementVector, double gravity) {
        // Reuse LivingEntity.travelInWater for drag, swimming attributes, climbing and exiting water.
        ((LivingEntityInvoker) entity).oritech$travelInWater(movementVector.scale(movementScale), gravity, entity.getDeltaMovement().y <= 0, entity.getY());
        return true;
    }
}
