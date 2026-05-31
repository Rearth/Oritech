package rearth.oritech.block.entity.augmenter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;
import java.util.List;

public class AugmentResearchStationBlockEntity extends BlockEntity implements MultiblockMachineController {

    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;

    public AugmentResearchStationBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.AUGMENTER_RESEARCH_STATION_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        serializeMultiblock(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        deserializeMultiblock(input);
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
    public StacksResourceHandler<ItemStack, ItemResource> getInventoryForMultiblock() {
        return null;
    }

    @Override
    public DynamicEnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return null;
    }

    @Override
    public void triggerSetupAnimation() {

    }
}
