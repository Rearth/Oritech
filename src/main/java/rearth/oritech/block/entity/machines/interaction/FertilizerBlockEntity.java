package rearth.oritech.block.entity.machines.interaction;

import net.minecraft.block.*;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.List;
import java.util.Objects;

public class FertilizerBlockEntity extends ItemEnergyFrameInteractionBlockEntity {
    public FertilizerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FERTILIZER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected boolean hasWorkAvailable(BlockPos toolPosition) {
        
        var targetPosition = toolPosition.down();
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);
        
        // skip not grown crops
        return targetState.getBlock() instanceof Fertilizable fertilizable && fertilizable.isFertilizable(world, targetPosition, targetState);
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
        
        var fertilizerStrength = 1;
        
        var targetPosition = processed.down();
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);
        
        if (!hasWorkAvailable(processed)) return;
        
        if (targetState.getBlock() instanceof CropBlock cropBlock) {
            var newAge = cropBlock.getAge(targetState) + fertilizerStrength;
            newAge = Math.min(newAge, cropBlock.getMaxAge());
            world.setBlockState(targetPosition, cropBlock.withAge(newAge), Block.NOTIFY_LISTENERS);
        } else {
            var fertilizable = (Fertilizable) targetState.getBlock();
            fertilizable.grow((ServerWorld) world, world.random, targetPosition, targetState);
        }
        
        ParticleContent.FERTILIZER_EFFECT.spawn(world, Vec3d.of(targetPosition), fertilizerStrength * 3 + 2);
        world.playSound(null, targetPosition, SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.BLOCKS, 1f, 1f);
    }
    
    @Override
    public BlockState getMachineHead() {
        return BlockContent.BLOCK_FERTILIZER_HEAD.getDefaultState();
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, -1,0)
        );
    }
    
    @Override
    protected void doProgress(boolean moving) {
        super.doProgress(moving);
        if (!moving && hasWorkAvailable(getCurrentTarget()))
            ParticleContent.WATERING_EFFECT.spawn(world, Vec3d.of(getCurrentTarget().down()), 2);
    }
    
    @Override
    public int getMoveTime() {
        return 10;
    }
    
    @Override
    public int getWorkTime() {
        return 20;
    }
    
    @Override
    public int getMoveEnergyUsage() {
        return 5;
    }
    
    @Override
    public int getOperationEnergyUsage() {
        return 50;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.DESTROYER_SCREEN;
    }
}
