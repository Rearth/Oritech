package rearth.oritech.block.entity.augmenter;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.InventoryProvider;
import rearth.oritech.util.MultiblockMachineController;
import rearth.oritech.util.energy.EnergyApi;

import java.util.ArrayList;
import java.util.List;

public class AugmenterResearchStationBlockEntity extends BlockEntity implements MultiblockMachineController {
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;
    
    public AugmenterResearchStationBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.AUGMENTER_RESEARCH_STATION_ENTITY, pos, state);
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
        var state = getCachedState();
        return state.get(Properties.FACING).getOpposite();
    }
    
    @Override
    public BlockPos getMachinePos() {
        return pos;
    }
    
    @Override
    public World getMachineWorld() {
        return world;
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
    public InventoryProvider getInventoryForLink() {
        return null;
    }
    
    @Override
    public EnergyApi.EnergyContainer getEnergyStorageForLink() {
        return null;
    }
    
    @Override
    public void playSetupAnimation() {
    }
}
