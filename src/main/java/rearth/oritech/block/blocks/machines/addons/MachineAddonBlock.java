package rearth.oritech.block.blocks.machines.addons;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.block.entity.machines.addons.AddonBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.TooltipHelper;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

public class MachineAddonBlock extends WallMountedBlock implements BlockEntityProvider {
    
    public static final Boolean USE_ACCURATE_OUTLINES = Oritech.CONFIG.tightMachineAddonHitboxes();

    public static final BooleanProperty ADDON_USED = BooleanProperty.of("addon_used");
    
    private final boolean extender;
    private final float speedMultiplier;
    private final float efficiencyMultiplier;
    private final boolean needsSupport;
    
    // because this parameter is needed in appendProperties, but we can't initialize or pass it to that
    private static boolean constructorAssignmentSupportWorkaround = false;
    
    private static Settings doConstructorWorkaround(Settings settings, boolean needsSupport) {
        constructorAssignmentSupportWorkaround = needsSupport;
        return settings;
    }
    
    public MachineAddonBlock(Settings settings, boolean extender, float speedMultiplier, float efficiencyMultiplier, boolean needsSupport) {
        super(doConstructorWorkaround(settings, needsSupport));
        
        this.extender = extender;
        this.speedMultiplier = speedMultiplier;
        this.efficiencyMultiplier = efficiencyMultiplier;
        this.needsSupport = needsSupport;
        
        if (needsSupport) {
            this.setDefaultState(getDefaultState()
                                   .with(ADDON_USED, false)
                                   .with(FACING, Direction.NORTH)
                                   .with(FACE, BlockFace.FLOOR)
            );
        } else {
            this.setDefaultState(getDefaultState().with(ADDON_USED, false));
        }
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ADDON_USED);
        if (constructorAssignmentSupportWorkaround) {
            builder.add(FACING);
            builder.add(FACE);
        }
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if (needsSupport)
            return super.getPlacementState(ctx);
        return getDefaultState();
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (needsSupport)
            return super.canPlaceAt(state, world, pos);
        return true;
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

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (!USE_ACCURATE_OUTLINES)
            return super.getOutlineShape(state, world, pos, context);
        
        var block = state.getBlock();
        if (block == BlockContent.QUARRY_ADDON) {
            return VoxelShapes.union(
                // base
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), state.get(FACING), state.get(FACE)),
                // status bar
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.375, 0.25, 0.875), state.get(FACING), state.get(FACE)),
                // pickaxe handle
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.625, 0.125, 0.3125, 0.6875, 0.1875, 0.8125), state.get(FACING), state.get(FACE)),
                // pickaxe head
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.125, 0.25, 0.875, 0.1875, 0.4375), state.get(FACING), state.get(FACE))
            );
        }
        else if (block == BlockContent.MACHINE_EFFICIENCY_ADDON) {
            return VoxelShapes.union(
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.25, 0.125, 0.1875, 0.75, 0.4375, 0.8125), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.75, 0.125, 0.125, 0.875, 0.5, 0.875), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.25, 0.5, 0.875), state.get(FACING), state.get(FACE))
            );
        }
        else if (block == BlockContent.MACHINE_SPEED_ADDON) {
            return VoxelShapes.union(
                // base
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.6875, 0.125, 0.3125, 0.8125, 0.25, 0.8125), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.1875, 0.125, 0.1875, 0.8125, 0.25, 0.3125), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.125, 0.125, 0.4375, 0.25, 0.1875), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5625, 0.125, 0.125, 0.6875, 0.25, 0.1875), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.8125, 0.125, 0.5625, 0.875, 0.25, 0.6875), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.8125, 0.125, 0.3125, 0.875, 0.25, 0.4375), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.25, 0.125, 0.3125, 0.6875, 0.1875, 0.75), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0.1875, 0.4375, 0.5625, 0.625, 0.625), state.get(FACING), state.get(FACE)),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.1875, 0.5, 0.625, 0.5625, 0.5625), state.get(FACING), state.get(FACE))
            );
        }
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }
    
    public boolean isExtender() {
        return extender;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        try {
            return getBlockEntityType().getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(pos, state);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException |
                 IllegalAccessException e) {
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
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            
            if (speedMultiplier != 1) {
                var displayedNumber = (int) ((1 - speedMultiplier) * 100);
                tooltip.add(Text.translatable("tooltip.oritech.addon_speed_desc").formatted(Formatting.DARK_GRAY)
                              .append(TooltipHelper.getFormattedValueChangeTooltip(displayedNumber)));
            }
            
            if (efficiencyMultiplier != 1) {
                var displayedNumber = (int) ((1 - efficiencyMultiplier) * 100);
                tooltip.add(Text.translatable("tooltip.oritech.addon_efficiency_desc").formatted(Formatting.DARK_GRAY)
                              .append(TooltipHelper.getFormattedValueChangeTooltip(displayedNumber)));
            }
            
            var item = (BlockItem) stack.getItem();
            var blockType = item.getBlock();
            
            if (blockType == BlockContent.MACHINE_YIELD_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_yield_desc").formatted(Formatting.DARK_GRAY));
            if (blockType == BlockContent.MACHINE_FLUID_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_fluid_desc").formatted(Formatting.DARK_GRAY));
            if (blockType == BlockContent.MACHINE_ACCEPTOR_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_acceptor_desc").formatted(Formatting.DARK_GRAY));
            if (blockType == BlockContent.STEAM_BOILER_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_boiler_desc").formatted(Formatting.DARK_GRAY));
            if (blockType == BlockContent.CROP_FILTER_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_crop_desc").formatted(Formatting.DARK_GRAY));
            if (blockType == BlockContent.MACHINE_INVENTORY_PROXY_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_proxy_desc").formatted(Formatting.DARK_GRAY));
            if (blockType == BlockContent.QUARRY_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_quarry_desc").formatted(Formatting.DARK_GRAY));
            if (blockType == BlockContent.MACHINE_REDSTONE_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_redstone_desc").formatted(Formatting.DARK_GRAY));
            
            if (extender) {
                tooltip.add(Text.translatable("tooltip.oritech.addon_extender_desc").formatted(Formatting.DARK_GRAY));
            }
            
        } else {
            tooltip.add(Text.translatable("tooltip.oritech.item_extra_info").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        }
        
    }
}
