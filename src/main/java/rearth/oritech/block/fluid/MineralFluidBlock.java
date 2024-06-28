package rearth.oritech.block.fluid;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MineralFluidBlock extends FluidBlock {
    public MineralFluidBlock(FlowableFluid fluid, Settings settings) {
        super(fluid, settings);
    }
    
    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        super.onEntityCollision(state, world, pos, entity);
        
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 1, false, false, false));
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 10, 1, false, false, false));
        }
        
    }
}
