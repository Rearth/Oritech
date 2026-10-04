package rearth.oritech.block.fluid;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

// lava like with more resistance
public class SheolFireFluidType extends FluidType {

    public SheolFireFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public boolean move(LivingEntity entity, Vec3 movementVector, double gravity) {
        // Adapted from LivingEntity.travelInLava/jumpOutOfFluid, with stronger horizontal drag.
        var oldY = entity.getY();
        entity.moveRelative(0.012F, movementVector);
        entity.move(MoverType.SELF, entity.getDeltaMovement());

        var movement = entity.getDeltaMovement().multiply(0.35, 0.65, 0.35).add(0, -gravity / 5, 0);
        entity.setDeltaMovement(movement);
        if (entity.horizontalCollision && entity.isFree(movement.x, movement.y + 0.6F - entity.getY() + oldY, movement.z)) {
            entity.setDeltaMovement(movement.x, 0.3, movement.z);
        }
        return true;
    }
}
