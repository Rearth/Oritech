package rearth.oritech.block.entity.machines.interaction;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.*;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.FluidProvider;

import java.util.List;
import java.util.Objects;

public class FertilizerBlockEntity extends ItemEnergyFrameInteractionBlockEntity implements FluidProvider {
    
    public static final long FLUID_USAGE = (long) (Oritech.CONFIG.fertilizerConfig.liquidPerBlockUsage() * FluidConstants.BUCKET);   // per block, tick usage is this divided by work time
    
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
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        SingleVariantStorage.writeNbt(fluidStorage, FluidVariant.CODEC, nbt, registryLookup);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        SingleVariantStorage.readNbt(fluidStorage, FluidVariant.CODEC, FluidVariant::blank, nbt, registryLookup);
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
        if (canFertilizeFarmland(toolPosition)) return true;
        return targetState.getBlock() instanceof Fertilizable fertilizable && fertilizable.isFertilizable(world, targetPosition, targetState);
    }
    
    private boolean canFertilizeFarmland(BlockPos toolPosition) {
        var targetPosition = toolPosition.down(2);
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);
        
        if (targetState.getBlock() instanceof FarmlandBlock) {
            var moistureStatus = targetState.get(Properties.MOISTURE);
            return moistureStatus != 7;
        }
        
        return false;
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
        
        var inventoryStack = inventory.getStack(0);
        var fertilizerInInventory = !inventoryStack.isEmpty() && inventoryStack.isIn(ConventionalItemTags.FERTILIZERS);
        var fertilizerStrength = fertilizerInInventory ? 2 : 1;
        var fertilized = false;
        
        var targetPosition = processed.down();
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);
        
        if (!hasWorkAvailable(processed)) return;

        if (targetState.getBlock() instanceof CropBlock cropBlock) {
            var newAge = cropBlock.getAge(targetState) + fertilizerStrength;
            newAge = Math.min(newAge, cropBlock.getMaxAge());
            world.setBlockState(targetPosition, cropBlock.withAge(newAge), Block.NOTIFY_LISTENERS);
            fertilized = true;
        } else if (targetState.getBlock() instanceof Fertilizable fertilizable) {
            fertilizable.grow((ServerWorld) world, world.random, targetPosition, targetState);
            if (fertilizerInInventory) {
                fertilizable.grow((ServerWorld) world, world.random, targetPosition, targetState);
                fertilized = true;
            }
        }

        var farmlandPosition = processed.down(2);
        var farmlandState = world.getBlockState(farmlandPosition);

        if (farmlandState.getBlock() instanceof FarmlandBlock && farmlandState.get(Properties.MOISTURE) != 7) {
            world.setBlockState(farmlandPosition, farmlandState.with(Properties.MOISTURE, 7));
        }
        
        if (fertilized == true) {
            if (fertilizerInInventory) {
                inventoryStack.decrement(1);
                inventory.setStack(0, inventoryStack);
            }
            super.finishBlockWork(processed);
            ParticleContent.FERTILIZER_EFFECT.spawn(world, Vec3d.of(targetPosition), fertilizerStrength * 3 + 2);
            world.playSound(null, targetPosition, SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.BLOCKS, 1f, 1f);
        }
    }
    
    @Override
    public BlockState getMachineHead() {
        return BlockContent.BLOCK_FERTILIZER_HEAD.getDefaultState();
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
            fluidStorage.amount -= getWaterUsagePerTick();
            ParticleContent.WATERING_EFFECT.spawn(world, Vec3d.of(getCurrentTarget().down()), 2);
        }
    }
    
    @Override
    public void updateNetwork() {
        super.updateNetwork();
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.SingleVariantFluidSyncPacket(pos, Registries.FLUID.getId(fluidStorage.variant.getFluid()).toString(), fluidStorage.amount));
    }
    
    @Override
    public @Nullable SingleVariantStorage<FluidVariant> getForDirectFluidAccess() {
        return fluidStorage;
    }
    
    @Override
    public int getMoveTime() {
        return Oritech.CONFIG.fertilizerConfig.moveDuration();
    }
    
    @Override
    public int getWorkTime() {
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
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.DESTROYER_SCREEN;
    }
    
    @Override
    public Storage<FluidVariant> getFluidStorage(Direction direction) {
        return fluidStorage;
    }
}
