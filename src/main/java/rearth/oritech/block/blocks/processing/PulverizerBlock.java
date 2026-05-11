package rearth.oritech.block.blocks.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.UpgradableMachineBlock;
import rearth.oritech.block.entity.processing.PulverizerBlockEntity;
import rearth.oritech.init.BlockEntitiesContent;

public class PulverizerBlock extends UpgradableMachineBlock implements EntityBlock {
    
    public PulverizerBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return PulverizerBlockEntity.class;
    }
    
    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        
        if (!world.isClientSide && entity instanceof LivingEntity livingEntity) {
            var targetPos = pos.getCenter().add(0, 0.5f, 0);
            var entityDist = entity.distanceToSqr(targetPos);
            if (entityDist > 0.7) return;
            var isWorking = world.getBlockEntity(pos, BlockEntitiesContent.PULVERIZER_ENTITY).get().progress > 0;
            if (isWorking)
                livingEntity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(DamageTypes.CRAMMING).get()), 1f);
        }
        super.stepOn(world, pos, state, entity);
    }
}
