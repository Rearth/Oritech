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
import rearth.oritech.block.entity.machines.addons.AddonBlockEntity;
import rearth.oritech.block.entity.machines.addons.EnergyAcceptorAddonBlockEntity;
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
    
    protected final AddonSettings addonSettings;

    // Bounding shapes for each type of addon, with rotations for all of their facing/face combinations
    // This is intended to work for "needsSupport" addon blocks, which have the FACING and FACE state properties
    // If any block does not have a boundingShape set, this will default to a full cube
    public static VoxelShape[][] MACHINE_ACCEPTOR_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_CAPACITOR_ADDON_SHAPE;
    public static VoxelShape[][] CROP_FILTER_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_EFFICIENCY_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_FLUID_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_INVENTORY_PROXY_ADDON_SHAPE;
    public static VoxelShape[][] QUARRY_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_HUNTER_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_REDSTONE_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_SPEED_ADDON_SHAPE;
    public static VoxelShape[][] STEAM_BOILER_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_YIELD_ADDON_SHAPE;
    
    // because this parameter is needed in appendProperties, but we can't initialize or pass it to that
    private static boolean constructorAssignmentSupportWorkaround = false;
    
    private static Settings doConstructorWorkaround(Settings settings, boolean needsSupport) {
        constructorAssignmentSupportWorkaround = needsSupport;
        return settings;
    }
    
    public MachineAddonBlock(Settings settings, AddonSettings addonSettings) {
        super(doConstructorWorkaround(settings, addonSettings.needsSupport()));
        
        this.addonSettings = addonSettings;
        
        if (addonSettings.needsSupport()) {
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
        if (addonSettings.needsSupport)
            return super.getPlacementState(ctx);
        return getDefaultState();
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (addonSettings.needsSupport)
            return super.canPlaceAt(state, world, pos);
        return true;
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        
        if (addonSettings.needsSupport) {
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
        if (!USE_ACCURATE_OUTLINES || !addonSettings.needsSupport() || addonSettings.boundingShape() == null)
            return super.getOutlineShape(state, world, pos, context);

        return addonSettings.boundingShape()[state.get(FACING).ordinal()][state.get(FACE).ordinal()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        try {
            return getBlockEntityType().getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(pos, state);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException |
                 IllegalAccessException e) {
            Oritech.LOGGER.error("Unable to create blockEntity for " + getBlockEntityType().getSimpleName() + " at " + this);
            return new AddonBlockEntity(pos, state);
        }
    }
    
    @NotNull
    public Class<? extends BlockEntity> getBlockEntityType() {
        return addonSettings.acceptEnergy ? EnergyAcceptorAddonBlockEntity.class : AddonBlockEntity.class;
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

    public AddonSettings getAddonSettings() {
        return addonSettings;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            
            if (addonSettings.speedMultiplier() != 1) {
                var displayedNumber = (int) ((1 - addonSettings.speedMultiplier()) * 100);
                tooltip.add(Text.translatable("tooltip.oritech.addon_speed_desc").formatted(Formatting.DARK_GRAY)
                              .append(TooltipHelper.getFormattedValueChangeTooltip(displayedNumber)));
            }
            
            if (addonSettings.efficiencyMultiplier() != 1) {
                var displayedNumber = (int) ((1 - addonSettings.efficiencyMultiplier()) * 100);
                tooltip.add(Text.translatable("tooltip.oritech.addon_efficiency_desc").formatted(Formatting.DARK_GRAY)
                              .append(TooltipHelper.getFormattedValueChangeTooltip(displayedNumber)));
            }

            if (addonSettings.addedCapacity() != 0) {
                tooltip.add(
                  Text.translatable("tooltip.oritech.addon_capacity_desc").formatted(Formatting.DARK_GRAY)
                    .append(TooltipHelper.getFormattedEnergyChangeTooltip(addonSettings.addedCapacity(), " RF")));
            }

            if (addonSettings.addedInsert() != 0) {
                tooltip.add(Text.translatable("tooltip.oritech.addon_transfer_desc").formatted(Formatting.DARK_GRAY)
                              .append(TooltipHelper.getFormattedEnergyChangeTooltip(addonSettings.addedInsert(), " RF/t")));
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
            if (blockType == BlockContent.MACHINE_HUNTER_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_hunter_desc").formatted(Formatting.DARK_GRAY));
            if (blockType == BlockContent.MACHINE_REDSTONE_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_redstone_desc").formatted(Formatting.DARK_GRAY));
            
            if (addonSettings.extender()) {
                tooltip.add(Text.translatable("tooltip.oritech.addon_extender_desc").formatted(Formatting.DARK_GRAY));
            }
            
        } else {
            tooltip.add(Text.translatable("tooltip.oritech.item_extra_info").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        }
        
    }

    static {
        MACHINE_ACCEPTOR_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        MACHINE_CAPACITOR_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        CROP_FILTER_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        MACHINE_EFFICIENCY_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        MACHINE_FLUID_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        MACHINE_INVENTORY_PROXY_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        QUARRY_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        MACHINE_HUNTER_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        MACHINE_REDSTONE_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        MACHINE_SPEED_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        STEAM_BOILER_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        MACHINE_YIELD_ADDON_SHAPE = new VoxelShape[Direction.values().length][BlockFace.values().length];
        for (var facing : Direction.values()) {
            if (!facing.getAxis().isHorizontal()) continue;
            for (var face : BlockFace.values()) {
                MACHINE_ACCEPTOR_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.25, 0.625, 0.25, 0.75, 0.75, 0.75), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.25, 0.125, 0.875, 0.375, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.375, 0.125, 0.875, 0.5, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.5, 0.125, 0.875, 0.625, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.75, 0, 1, 0.875, 1), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0, 0, 1, 0.125, 1), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.875, 0.125, 0.875, 1, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.1875, 0.625, 0.1875, 0.8125, 0.75, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.1875, 0.125, 0.1875, 0.8125, 0.25, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.0625, 0.0625, 0.6875, 0.8125, 0.1875), facing, face));
                MACHINE_CAPACITOR_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.1875, 0.1875, 0.3125, 0.25, 0.375, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.25, 0.125, 0.25, 0.75, 0.4375, 0.75), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.75, 0.125, 0.1875, 0.8125, 0.5, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5625, 0.4375, 0.25, 0.625, 0.5, 0.75), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.6875, 0.4375, 0.25, 0.75, 0.5, 0.75), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.4375, 0.25, 0.375, 0.5, 0.75), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.4375, 0.25, 0.5, 0.5, 0.75), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5625, 0.125, 0.1875, 0.625, 0.5, 0.25), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.6875, 0.125, 0.1875, 0.75, 0.5, 0.25), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.125, 0.1875, 0.375, 0.5, 0.25), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.125, 0.1875, 0.5, 0.5, 0.25), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.25, 0.125, 0.75, 0.75, 0.5, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.8125, 0.25, 0.5625, 0.875, 0.4375, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.8125, 0.25, 0.3125, 0.875, 0.4375, 0.4375), facing, face));
                CROP_FILTER_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.125, 0.1875, 0.875, 0.25, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.25, 0.1875, 0.875, 0.5625, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5625, 0.3125, 0.1875, 0.75, 0.4375, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5625, 0.125, 0.8125, 0.75, 0.4375, 0.9375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.375, 0.25, 0.875), facing, face));
                MACHINE_EFFICIENCY_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.25, 0.125, 0.1875, 0.75, 0.4375, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.75, 0.125, 0.125, 0.875, 0.5, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.25, 0.5, 0.875), facing, face));
                MACHINE_FLUID_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0, 0.125, 0.875, 0.125, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0.3125, 0.1875, 0.375, 0.625, 0.5625), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.625, 0.3125, 0.1875, 1, 0.625, 0.5625), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.5, 0.6875, 0.5625, 1, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.375, 0.6875, 0.875, 0.5, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.75, 0.375, 0.5625, 0.875, 0.5, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.46875, 0.125, 0.71875, 0.59375, 0.375, 0.78125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.734375, 0.125, 0.625, 0.859375, 0.375, 0.8125), facing, face), // angled post
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.25, 0.3125, 0.3125, 0.5), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0.375, 0.25, 0.625, 0.5625, 0.5), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.6875, 0.125, 0.25, 0.875, 0.3125, 0.5), facing, face));
                MACHINE_INVENTORY_PROXY_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.875, 0.875, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.875, 0.375, 0.375, 1, 0.625, 0.625), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.375, 0.375, 0.125, 0.625, 0.625), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0.375, 0, 0.625, 0.625, 0.125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0.375, 0.875, 0.625, 0.625, 1), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0.8125, 0.375, 0.625, 1, 0.625), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.00125, 0.3125, 1, 0.93875, 0.375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.00125, 0.625, 1, 0.93875, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.000625, 0, 0.375, 0.938125, 1), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.625, 0.000625, 0, 0.6875, 0.938125, 1), facing, face));
                QUARRY_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face), // base
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.375, 0.25, 0.875), facing, face), // status bar
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.625, 0.125, 0.3125, 0.6875, 0.1875, 0.8125), facing, face), // pickaxe handle
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.125, 0.25, 0.875, 0.1875, 0.4375), facing, face)); // pickaxe head
                MACHINE_HUNTER_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
		            Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.375, 0.25, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5, 0.1875, 0.4375, 0.75, 0.25, 0.5625), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.125, 0.375, 0.8125, 0.1875, 0.625), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5625, 0.1875, 0.375, 0.6875, 0.25, 0.4375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5, 0.125, 0.3125, 0.75, 0.1875, 0.375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5, 0.125, 0.625, 0.75, 0.1875, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5625, 0.1875, 0.5625, 0.6875, 0.25, 0.625), facing, face));
                MACHINE_REDSTONE_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.1875, 0.125, 0, 0.4375, 0.25, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.1875, 0, 0.0015625, 0.6875, 0.1875, 0.0640625), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.625, 0.125, 0.1875, 0.75, 0.25, 0.3125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.625, 0.125, 0.5, 0.75, 0.25, 0.625), facing, face));
                MACHINE_SPEED_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.125, 0.6875, 0.8125, 0.25, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.1875, 0.125, 0.1875, 0.3125, 0.25, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.3125, 0.1875, 0.25, 0.4375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.5625, 0.1875, 0.25, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5625, 0.125, 0.8125, 0.6875, 0.25, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.125, 0.8125, 0.4375, 0.25, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.125, 0.25, 0.75, 0.1875, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.1875, 0.375, 0.625, 0.625, 0.5625), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0.1875, 0.4375, 0.6875, 0.5625, 0.5), facing, face));
                STEAM_BOILER_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0, 0.125, 0.875, 0.125, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.3125, 0.25, 0.25, 0.4375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.75, 0.125, 0.3125, 0.875, 0.25, 0.4375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.125, 0.5625, 0.5625, 0.25, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0000625, 0.25, 0.3125, 1.000125, 0.625, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.1875, 0.375, 1, 0.6875, 0.625), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.000125, 0.3125, 0.25, 1.00025, 0.5625, 0.75), facing, face));
                MACHINE_YIELD_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face), // base
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.25, 0.375, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.75, 0.125, 0.125, 0.875, 0.375, 0.875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.25, 0.125, 0.125, 0.75, 0.375, 0.25), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.125, 0.3125, 0.6875, 0.25, 0.8125), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0.125, 0.5625, 0.3125, 0.4375, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.6875, 0.125, 0.5625, 0.9375, 0.4375, 0.6875), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0.125, 0.3125, 0.3125, 0.4375, 0.4375), facing, face),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.6875, 0.125, 0.3125, 0.9375, 0.4375, 0.4375), facing, face));
            }
        }
    }
    
    // AddonSettings is an immutable configuration record for a machine addon, and should be constructed in BlockContent
    public record AddonSettings(boolean extender, float speedMultiplier, float efficiencyMultiplier, long addedCapacity, long addedInsert, boolean acceptEnergy, boolean needsSupport, VoxelShape[][] boundingShape) {
        public static AddonSettings getDefaultSettings() {
            return new AddonSettings(false, 1.0f, 1.0f, 0, 0, false, true, null);
        }

        // extender and needsSupport aren't strictly exclusive, but are unlikely to be used together
        public AddonSettings withExtender(boolean newExtender) {
            return new AddonSettings(newExtender, speedMultiplier, efficiencyMultiplier, addedCapacity, addedInsert, acceptEnergy, needsSupport, boundingShape);
        }

        public AddonSettings withSpeedMultiplier(float newMultiplier) {
            return new AddonSettings(extender, newMultiplier, efficiencyMultiplier, addedCapacity, addedInsert, acceptEnergy, needsSupport, boundingShape);
        }

        public AddonSettings withEfficiencyMultiplier(float newMultiplier) {
            return new AddonSettings(extender, speedMultiplier, newMultiplier, addedCapacity, addedInsert, acceptEnergy, needsSupport, boundingShape);
        }

        public AddonSettings withAddedCapacity(long newCapacity) {
            return new AddonSettings(extender, speedMultiplier, efficiencyMultiplier, newCapacity, addedInsert, acceptEnergy, needsSupport, boundingShape);
        }

        public AddonSettings withAddedInsert(long newInsert) {
            return new AddonSettings(extender, speedMultiplier, efficiencyMultiplier, addedCapacity, newInsert, acceptEnergy, needsSupport, boundingShape);
        }
        
        public AddonSettings withAcceptEnergy(boolean newAccept) {
            return new AddonSettings(extender, speedMultiplier, efficiencyMultiplier, addedCapacity, addedInsert, newAccept, needsSupport, boundingShape);
        }

        public AddonSettings withNeedsSupport(boolean newSupport) {
            return new AddonSettings(extender, speedMultiplier, efficiencyMultiplier, addedCapacity, addedInsert, acceptEnergy, newSupport, boundingShape);
        }

        // boundingShape should only be set if needsSupport is also set
        public AddonSettings withBoundingShape(VoxelShape[][] newShape) {
            return new AddonSettings(extender, speedMultiplier, efficiencyMultiplier, addedCapacity, addedInsert, acceptEnergy, needsSupport, newShape);
        }
    }
}
