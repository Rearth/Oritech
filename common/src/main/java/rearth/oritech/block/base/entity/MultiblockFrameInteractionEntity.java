package rearth.oritech.block.base.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;

public abstract class MultiblockFrameInteractionEntity extends ItemEnergyFrameInteractionBlockEntity implements MultiblockMachineController {
    
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    private float coreQuality = 1f;
    
    public MultiblockFrameInteractionEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
    
    @Override
    public void triggerSetupAnimation() {
    
    }
}
