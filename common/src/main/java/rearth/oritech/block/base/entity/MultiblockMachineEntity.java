package rearth.oritech.block.base.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.util.MultiblockMachineController;
import rearth.oritech.api.item.ItemApi;

import java.util.ArrayList;

public abstract class MultiblockMachineEntity extends UpgradableMachineBlockEntity implements MultiblockMachineController {
    
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    private float coreQuality = 1f;
    
    public MultiblockMachineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }
    
    @Override
    public ArrayList<BlockPos> getConnectedCores() {
        return coreBlocksConnected;
    }
    
    @Override
    public Direction getFacingForMultiblock() {
        return getFacing();
    }
    
    @Override
    public void setCoreQuality(float quality) {
        this.coreQuality = quality;
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        addMultiblockToNbt(nbt);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        loadMultiblockNbtData(nbt);
    }
    
    @Override
    public float getCoreQuality() {
        return this.coreQuality;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryForMultiblock() {
        return inventory;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public BlockPos getPosForMultiblock() {
        return pos;
    }
    
    @Override
    public World getWorldForMultiblock() {
        return world;
    }
    
    @Override
    public boolean isActive(BlockState state) {
        return state.get(MultiblockMachine.ASSEMBLED);
    }
    
    // this seems to work as expected for some reason?
    public static Vec3i worldToRelativePos(Vec3i ownWorldPos, Vec3i worldPos, Direction ownFacing) {
        return worldPos.subtract(ownWorldPos);
    }
}
