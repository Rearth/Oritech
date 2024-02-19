package rearth.oritech.block.custom.machines.addons;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.addons.AddonBlockEntity;
import rearth.oritech.block.base.entity.UpgradableMachineBlockEntity;
import rearth.oritech.block.entity.machines.PulverizerBlockEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class MachineAddonBlock extends WallMountedBlock implements BlockEntityProvider {
    
    public static final BooleanProperty ADDON_USED = BooleanProperty.of("addon_used");
    
    private final boolean extender;
    private final float speedMultiplier;
    private final float efficiencyMultiplier;
    
    
    public MachineAddonBlock(Settings settings, boolean extender, float speedMultiplier, float efficiencyMultiplier) {
        super(settings);
        this.setDefaultState(getDefaultState()
                               .with(ADDON_USED, false)
                               .with(FACING, Direction.NORTH)
                               .with(FACE, BlockFace.WALL)
        );
        
        this.extender = extender;
        this.speedMultiplier = speedMultiplier;
        this.efficiencyMultiplier = efficiencyMultiplier;
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ADDON_USED);
        builder.add(FACING);
        builder.add(FACE);
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
            return new PulverizerBlockEntity(pos, state);
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
            
            if (controllerEntity instanceof UpgradableMachineBlockEntity machineEntity) {
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
