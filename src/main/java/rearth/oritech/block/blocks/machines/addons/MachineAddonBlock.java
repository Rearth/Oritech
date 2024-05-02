package rearth.oritech.block.blocks.machines.addons;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.block.entity.machines.addons.AddonBlockEntity;
import rearth.oritech.util.MachineAddonController;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class MachineAddonBlock extends WallMountedBlock implements BlockEntityProvider {
    
    public static final BooleanProperty ADDON_USED = BooleanProperty.of("addon_used");
    
    private final boolean extender;
    private final float speedMultiplier;
    private final float efficiencyMultiplier;
    private final boolean needsSupport;
    
    
    public MachineAddonBlock(Settings settings, boolean extender, float speedMultiplier, float efficiencyMultiplier, boolean needsSupport) {
        super(settings);
        this.setDefaultState(getDefaultState()
                               .with(ADDON_USED, false)
                               .with(FACING, Direction.UP)
                               .with(FACE, BlockFace.WALL)
        );
        
        this.extender = extender;
        this.speedMultiplier = speedMultiplier;
        this.efficiencyMultiplier = efficiencyMultiplier;
        this.needsSupport = needsSupport;
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ADDON_USED);
        builder.add(FACING);
        builder.add(FACE);
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        
        if (needsSupport) {
            return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        } else {
            return state;
        }
    }
    
    @Override
    protected MapCodec<? extends WallMountedBlock> getCodec() {
        return null;
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    public boolean isExtender() {
        return extender;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        try {
            return getBlockEntityType().getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(pos, state);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            Oritech.LOGGER.error("Unable to create blockEntity for " + getBlockEntityType().getSimpleName() + " at " + this);
            return new MachineCoreEntity(pos, state);
        }
    }
    
    @NotNull
    public Class<? extends BlockEntity> getBlockEntityType() {
        return AddonBlockEntity.class;
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        
        if (!world.isClient() && state.get(ADDON_USED)) {
            
            var ownEntity = (AddonBlockEntity) world.getBlockEntity(pos);
            
            var controllerEntity = world.getBlockEntity(Objects.requireNonNull(ownEntity).getControllerPos());
            
            if (controllerEntity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }
        }
        
        return super.onBreak(world, pos, state, player);
    }
    
    public float getSpeedMultiplier() {
        return speedMultiplier;
    }
    
    public float getEfficiencyMultiplier() {
        return efficiencyMultiplier;
    }
    
}
