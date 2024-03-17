package rearth.oritech.block.entity.machines.interaction;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.fluid.Fluids;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.FluidProvider;

import java.util.List;
import java.util.Objects;

public class FertilizerBlockEntity extends ItemEnergyFrameInteractionBlockEntity implements FluidProvider {
    
    public static final long FLUID_USAGE = 1 * FluidConstants.BUCKET;   // per block, tick usage is this divided by work time
    
    // TODO create gui for water provider
    private final SingleVariantStorage<FluidVariant> fluidStorage = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }
        
        @Override
        protected long getCapacity(FluidVariant variant) {
            return (4 * FluidConstants.BUCKET);
        }
        
        @Override
        public boolean supportsExtraction() {
            return false;
        }
        
        @Override
        protected boolean canInsert(FluidVariant variant) {
            return variant.getFluid().matchesType(Fluids.WATER);
        }
        
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            FertilizerBlockEntity.this.markDirty();
        }
    };
    
    public FertilizerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FERTILIZER_BLOCK_ENTITY, pos, state);
    }
    
    private long getWaterUsagePerTick() {
        return FLUID_USAGE / getWorkTime();
    }
    
    private boolean hasEnoughWater() {
        return fluidStorage.amount >= getWaterUsagePerTick();
    }
    
    @Override
    protected boolean canProgress() {
        return hasEnoughWater() && super.canProgress();
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
        if (!moving && hasWorkAvailable(getCurrentTarget())) {
            fluidStorage.amount -= getWaterUsagePerTick();
            ParticleContent.WATERING_EFFECT.spawn(world, Vec3d.of(getCurrentTarget().down()), 2);
        }
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
    
    @Override
    public Storage<FluidVariant> getFluidStorage(Direction direction) {
        return fluidStorage;
    }
}
