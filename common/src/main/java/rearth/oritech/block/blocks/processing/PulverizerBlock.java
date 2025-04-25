package rearth.oritech.block.blocks.processing;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.UpgradableMachineBlock;
import rearth.oritech.block.entity.processing.PulverizerBlockEntity;
import rearth.oritech.init.BlockEntitiesContent;

public class PulverizerBlock extends UpgradableMachineBlock implements BlockEntityProvider {
    
    public PulverizerBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return PulverizerBlockEntity.class;
    }
    
    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            var targetPos = pos.toCenterPos().add(0, 0.5f, 0);
            var entityDist = entity.squaredDistanceTo(targetPos);
            if (entityDist > 0.7) return;
            var isWorking = world.getBlockEntity(pos, BlockEntitiesContent.PULVERIZER_ENTITY).get().progress > 0;
            if (isWorking)
                livingEntity.damage(new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getEntry(DamageTypes.CRAMMING).get()), 1f);
        }
        super.onSteppedOn(world, pos, state, entity);
    }
}
