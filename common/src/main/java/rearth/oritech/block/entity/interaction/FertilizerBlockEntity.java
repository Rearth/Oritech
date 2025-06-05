package rearth.oritech.block.entity.interaction;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.block.*;
import net.minecraft.fluid.Fluid;
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
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.network.NetworkContent;

import java.util.List;
import java.util.Objects;

public class FertilizerBlockEntity extends ItemEnergyFrameInteractionBlockEntity implements FluidApi.BlockProvider {
    
    public static final long FLUID_USAGE = (long) (Oritech.CONFIG.fertilizerConfig.liquidPerBlockUsage() * FluidStackHooks.bucketAmount());   // per block, tick usage is this divided by work time
    
    private final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(4 * FluidStackHooks.bucketAmount(), this::markDirty) {
        
        @Override
        public long insert(FluidStack toInsert, boolean simulate) {
            var fluid = toInsert.getFluid();
            if (fluid.equals(FluidContent.STILL_MINERAL_SLURRY.get()) || fluid.equals(Fluids.WATER))
                return super.insert(toInsert, simulate);
            
            return 0;
        }
    };
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        fluidStorage.writeNbt(nbt, "");
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
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
        var fertilizerInInventory = !inventoryStack.isEmpty() && inventoryStack.isIn(TagContent.CONVENTIONAL_FERTILIZER);
        var mineralSlurried = fluidStorage.getFluid().equals(FluidContent.STILL_MINERAL_SLURRY.get());
        var fertilizerStrength = fertilizerInInventory ? 2 : 1;
        fertilizerStrength *= mineralSlurried ? 2 : 1;
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
        
        if (fertilized) {
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
            fluidStorage.setAmount(fluidStorage.getAmount() - getWaterUsagePerTick());
            ParticleContent.WATERING_EFFECT.spawn(world, Vec3d.of(getCurrentTarget().down()), 2);
        }
    }
    
    @Override
    public void sendMovementNetworkPacket(BlockPos from) {
        super.sendMovementNetworkPacket(from);
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.SingleVariantFluidSyncPacketAPI(pos, Registries.FLUID.getId(fluidStorage.getFluid()).toString(), fluidStorage.getAmount()));
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
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.DESTROYER_SCREEN;
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(Direction direction) {
        return fluidStorage;
    }
}
