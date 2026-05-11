package rearth.oritech.block.fluid;

import dev.architectury.core.block.ArchitecturyLiquidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SheolFireFluidBlock extends ArchitecturyLiquidBlock {
    
    public SheolFireFluidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }
    
    @Override
    protected void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        super.entityInside(state, level, pos, entity);
        
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.hurt(level.damageSources().lava(), 8);
            livingEntity.setRemainingFireTicks(8 * 20);
        }
        
    }
    
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        var blockpos = pos.above();
        if (level.getBlockState(blockpos).isAir() && !level.getBlockState(blockpos).isSolidRender(level, blockpos)) {
            if (random.nextInt(40) == 0) {
                double d0 = (double)pos.getX() + random.nextDouble();
                double d1 = (double)pos.getY() + 1.0;
                double d2 = (double)pos.getZ() + random.nextDouble();
                level.addParticle(ParticleTypes.LAVA, d0, d1, d2, 0.0, 0.0, 0.0);
                level.playLocalSound(
                  d0, d1, d2, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false
                );
            }
            
            if (random.nextInt(100) == 0) {
                level.playLocalSound(
                  pos.getX(),
                  pos.getY(),
                  pos.getZ(),
                  SoundEvents.LAVA_AMBIENT,
                  SoundSource.BLOCKS,
                  0.2F + random.nextFloat() * 0.2F,
                  0.9F + random.nextFloat() * 0.15F,
                  false
                );
            }
        }
    }
}
