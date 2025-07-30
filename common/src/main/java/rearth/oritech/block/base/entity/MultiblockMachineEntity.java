package rearth.oritech.block.base.entity;

import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class MultiblockMachineEntity extends UpgradableMachineBlockEntity implements MultiblockMachineController {
    
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    @SyncField(SyncType.GUI_OPEN)
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
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        addMultiblockToNbt(nbt);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
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
        return worldPosition;
    }
    
    @Override
    public Level getWorldForMultiblock() {
        return level;
    }
    
    @Override
    public boolean isActive(BlockState state) {
        return state.getValue(MultiblockMachine.ASSEMBLED);
    }
    
    // this seems to work as expected for some reason?
    public static Vec3i worldToRelativePos(Vec3i ownWorldPos, Vec3i worldPos, Direction ownFacing) {
        return worldPos.subtract(ownWorldPos);
    }
    
    @Override
    public void triggerSetupAnimation() {
        triggerAnim("base_controller", "setup");
    }
}
