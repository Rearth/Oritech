package rearth.oritech.block.entity.augmenter;

import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class AugmentResearchStationBlockEntity extends BlockEntity implements MultiblockMachineController {
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;
    
    public AugmentResearchStationBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.AUGMENTER_RESEARCH_STATION_ENTITY, pos, state);
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
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 0, 1),
          new Vec3i(0, 1, 1),
          new Vec3i(0, 1, 0)
        );
    }
    
    @Override
    public Direction getFacingForMultiblock() {
        var state = getBlockState();
        return state.getValue(BlockStateProperties.FACING).getOpposite();
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
    public ArrayList<BlockPos> getConnectedCores() {
        return coreBlocksConnected;
    }
    
    @Override
    public void setCoreQuality(float quality) {
        this.coreQuality = quality;
    }
    
    @Override
    public float getCoreQuality() {
        return coreQuality;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryForMultiblock() {
        return null;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return null;
    }
    
    @Override
    public void triggerSetupAnimation() {
    
    }
}
