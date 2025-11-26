package rearth.oritech.block.entity.interaction;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.TagContent;

import java.util.List;
import java.util.Objects;

public class FertilizerBlockEntity extends ItemEnergyFrameInteractionBlockEntity implements FluidApi.BlockProvider {
    
    public static final long FLUID_USAGE = (long) (Oritech.CONFIG.fertilizerConfig.liquidPerBlockUsage() * FluidStackHooks.bucketAmount());   // per block, tick usage is this divided by work time
    
    @SyncField(SyncType.GUI_TICK)
    private final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(4 * FluidStackHooks.bucketAmount(), this::setChanged) {
        
        @Override
        public long insert(FluidStack toInsert, boolean simulate) {
            var fluid = toInsert.getFluid();
            if (fluid.equals(FluidContent.STILL_MINERAL_SLURRY.get()) || fluid.equals(Fluids.WATER))
                return super.insert(toInsert, simulate);
            
            return 0;
        }
    };
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        fluidStorage.writeNbt(nbt, "");
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        fluidStorage.readNbt(nbt, "");
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 56, 38));
    }
    
    public FertilizerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FERTILIZER_BLOCK_ENTITY, pos, state);
    }
    
    private long getWaterUsagePerTick() {
        return (long) (FLUID_USAGE / getWorkTime());
    }
    
    private boolean hasEnoughWater() {
        return fluidStorage.getAmount() >= getWaterUsagePerTick();
    }
    
    @Override
    protected boolean canProgress() {
        return hasEnoughWater() && super.canProgress();
    }
    
    @Override
    protected boolean hasWorkAvailable(BlockPos toolPosition) {
        
        var targetPosition = toolPosition.below();
        var targetState = Objects.requireNonNull(level).getBlockState(targetPosition);
        
        // skip not grown crops
        if (canFertilizeFarmland(toolPosition)) return true;
        return targetState.getBlock() instanceof BonemealableBlock fertilizable && fertilizable.isValidBonemealTarget(level, targetPosition, targetState);
    }
    
    private boolean canFertilizeFarmland(BlockPos toolPosition) {
        var targetPosition = toolPosition.below(2);
        var targetState = Objects.requireNonNull(level).getBlockState(targetPosition);
        
        if (targetState.getBlock() instanceof FarmBlock) {
            var moistureStatus = targetState.getValue(BlockStateProperties.MOISTURE);
            return moistureStatus != 7;
        }
        
        return false;
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
        
        var inventoryStack = inventory.getItem(0);
        var fertilizerInInventory = !inventoryStack.isEmpty() && inventoryStack.is(TagContent.CONVENTIONAL_FERTILIZER);
        var mineralSlurried = fluidStorage.getFluid().equals(FluidContent.STILL_MINERAL_SLURRY.get());
        var fertilizerStrength = fertilizerInInventory ? 2 : 1;
        fertilizerStrength *= mineralSlurried ? 2 : 1;
        var fertilized = false;
        
        var targetPosition = processed.below();
        var targetState = Objects.requireNonNull(level).getBlockState(targetPosition);
        
        if (!hasWorkAvailable(processed)) return;

        if (targetState.getBlock() instanceof CropBlock cropBlock) {
            var newAge = cropBlock.getAge(targetState) + fertilizerStrength;
            newAge = Math.min(newAge, cropBlock.getMaxAge());
            level.setBlock(targetPosition, cropBlock.getStateForAge(newAge), Block.UPDATE_CLIENTS);
            fertilized = true;
        } else if (targetState.getBlock() instanceof BonemealableBlock fertilizable) {
            fertilizable.performBonemeal((ServerLevel) level, level.random, targetPosition, targetState);
            if (fertilizerInInventory) {
                fertilizable.performBonemeal((ServerLevel) level, level.random, targetPosition, targetState);
                fertilized = true;
            }
        }

        var farmlandPosition = processed.below(2);
        var farmlandState = level.getBlockState(farmlandPosition);

        if (farmlandState.getBlock() instanceof FarmBlock && farmlandState.getValue(BlockStateProperties.MOISTURE) != 7) {
            level.setBlockAndUpdate(farmlandPosition, farmlandState.setValue(BlockStateProperties.MOISTURE, 7));
        }
        
        if (fertilized) {
            if (fertilizerInInventory) {
                inventoryStack.shrink(1);
                inventory.setItem(0, inventoryStack);
            }
            super.finishBlockWork(processed);
            ParticleContent.FERTILIZER_EFFECT.spawn(level, Vec3.atLowerCornerOf(targetPosition), fertilizerStrength * 3 + 2);
            level.playSound(null, targetPosition, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1f, 1f);
        }
    }
    
    @Override
    public BlockState getMachineHead() {
        return BlockContent.BLOCK_FERTILIZER_HEAD.defaultBlockState();
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, -1, 0)
        );
    }
    
    @Override
    protected void doProgress(boolean moving) {
        super.doProgress(moving);
        if (!moving && hasWorkAvailable(getCurrentTarget())) {
            fluidStorage.setAmount(fluidStorage.getAmount() - getWaterUsagePerTick());
            ParticleContent.WATERING_EFFECT.spawn(level, Vec3.atLowerCornerOf(getCurrentTarget().below()), 2);
        }
    }
    
    @Override
    public float getMoveTime() {
        return Oritech.CONFIG.fertilizerConfig.moveDuration();
    }
    
    @Override
    public float getWorkTime() {
        return Oritech.CONFIG.fertilizerConfig.workDuration();
    }
    
    @Override
    public int getMoveEnergyUsage() {
        return Oritech.CONFIG.fertilizerConfig.moveEnergyUsage();
    }
    
    @Override
    public int getOperationEnergyUsage() {
        return Oritech.CONFIG.fertilizerConfig.workEnergyUsage();
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.DESTROYER_SCREEN;
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(Direction direction) {
        return fluidStorage;
    }
}
