package rearth.oritech.block.fluid;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

/** Steam carries entities and items upward. */
public class SteamFluidType extends FluidType {

    public SteamFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public boolean move(LivingEntity entity, Vec3 movementVector, double gravity) {
        entity.moveRelative(0.018F, movementVector);
        entity.move(MoverType.SELF, entity.getDeltaMovement());

        var movement = entity.getDeltaMovement().multiply(0.91, 0.96, 0.91);
        entity.setDeltaMovement(movement.x, Math.min(movement.y + 0.035, 0.12), movement.z);
        return true;
    }

    @Override
    public void setItemMovement(ItemEntity entity) {
        var movement = entity.getDeltaMovement();
        entity.setDeltaMovement(movement.x * 0.95, Math.min(movement.y + 0.04, 0.12), movement.z * 0.95);
    }
}
