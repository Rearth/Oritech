package rearth.oritech.block.blocks.addons;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.addons.AddonBlockEntity;
import rearth.oritech.block.entity.addons.EnergyAcceptorAddonBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.TooltipHelper;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

public class MachineAddonBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    
    public static final Boolean USE_ACCURATE_OUTLINES = Oritech.CONFIG.tightMachineAddonHitboxes();
    
    public static final BooleanProperty ADDON_USED = BooleanProperty.create("addon_used");
    
    protected final AddonSettings addonSettings;
    
    // Bounding shapes for each type of addon, with rotations for all of their facing/face combinations
    // This is intended to work for "needsSupport" addon blocks, which have the FACING and FACE state properties
    // If any block does not have a boundingShape set, this will default to a full cube
    public static VoxelShape[][] MACHINE_ACCEPTOR_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_CAPACITOR_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_PROCESSING_ADDON_SHAPE;
    public static VoxelShape[][] CROP_FILTER_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_EFFICIENCY_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_FLUID_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_INVENTORY_PROXY_ADDON_SHAPE;
    public static VoxelShape[][] QUARRY_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_HUNTER_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_REDSTONE_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_SPEED_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_ULTIMATE_ADDON_SHAPE;
    public static VoxelShape[][] STEAM_BOILER_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_YIELD_ADDON_SHAPE;
    public static VoxelShape[][] MACHINE_SILK_TOUCH_ADDON_SHAPE;
    
    // because this parameter is needed in appendProperties, but we can't initialize or pass it to that
    private static boolean constructorAssignmentSupportWorkaround = false;
    
    private static Properties doConstructorWorkaround(Properties settings, boolean needsSupport) {
        constructorAssignmentSupportWorkaround = needsSupport;
        return settings;
    }
    
    public MachineAddonBlock(Properties settings, AddonSettings addonSettings) {
        super(doConstructorWorkaround(settings, addonSettings.needsSupport()));
        
        this.addonSettings = addonSettings;
        
        if (addonSettings.needsSupport()) {
            this.registerDefaultState(defaultBlockState()
                                        .setValue(ADDON_USED, false)
                                        .setValue(FACING, Direction.NORTH)
                                        .setValue(FACE, AttachFace.FLOOR)
            );
        } else {
            this.registerDefaultState(defaultBlockState().setValue(ADDON_USED, false));
        }
    }
    
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        
        // search for addon extender or machine at neighbor
        // if addon extender, check if its connected to a machine, if so then init it
        // if machine then init it
        
        if (world.isClientSide) return;
        
        for (var direction : Direction.values()) {
            var checkPos = pos.offset(direction.getNormal());
            var checkEntity = world.getBlockEntity(checkPos);
            if (checkEntity instanceof MachineAddonController machineEntity) {
                AddonBlockEntity.pendingInits.add(machineEntity);
                break;
            } else if (checkEntity instanceof ItemEnergyFrameInteractionBlockEntity machineEntity) {
                AddonBlockEntity.pendingInits.add(machineEntity);
                break;
            } else if (checkEntity instanceof MachineCoreEntity machineEntity) {
                if (machineEntity.isEnabled() && machineEntity.getCachedController() instanceof MachineAddonController addonController) {
                    AddonBlockEntity.pendingInits.add(addonController);
                }
                break;
            } else if (checkEntity instanceof AddonBlockEntity addonEntity) {
                var addonState = addonEntity.getBlockState();
                var addonConnected = addonState.getValue(ADDON_USED);
                if (!addonConnected) continue;
                var controllerPos = addonEntity.getControllerPos();
                if (world.getBlockEntity(controllerPos) instanceof MachineAddonController controllerEntity) {
                    AddonBlockEntity.pendingInits.add(controllerEntity);
                    break;
                }
            }
        }
        
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ADDON_USED);
        if (constructorAssignmentSupportWorkaround) {
            builder.add(FACING);
            builder.add(FACE);
        }
    }
    
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        if (!state.hasProperty(FACING)) return state;
        return super.rotate(state, rotation);
    }
    
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (!state.hasProperty(FACING)) return state;
        return super.mirror(state, mirror);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (addonSettings.needsSupport)
            return super.getStateForPlacement(ctx);
        return defaultBlockState();
    }
    
    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        if (addonSettings.needsSupport)
            return super.canSurvive(state, world, pos);
        return true;
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        
        if (addonSettings.needsSupport) {
            return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
        } else {
            return state;
        }
    }
    
    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return null;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (!USE_ACCURATE_OUTLINES || !addonSettings.needsSupport() || addonSettings.boundingShape() == null)
            return super.getShape(state, world, pos, context);
        
        return addonSettings.boundingShape()[state.getValue(FACING).ordinal()][state.getValue(FACE).ordinal()];
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
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
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        
        if (!world.isClientSide() && state.getValue(ADDON_USED)) {
            
            var ownEntity = (AddonBlockEntity) world.getBlockEntity(pos);
            
            var controllerEntity = world.getBlockEntity(Objects.requireNonNull(ownEntity).getControllerPos());
            
            if (controllerEntity instanceof MachineAddonController machineEntity) {
                machineEntity.initAddons(pos);
            }
        }
        
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    public AddonSettings getAddonSettings() {
        return addonSettings;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            
            if (addonSettings.speedMultiplier() != 1) {
                var displayedNumber = Math.round((1 - addonSettings.speedMultiplier()) * 100);
                tooltip.add(Component.translatable("tooltip.oritech.addon_speed_desc").withStyle(ChatFormatting.DARK_GRAY)
                              .append(TooltipHelper.getFormattedValueChangeTooltip(displayedNumber)));
            }
            
            if (addonSettings.efficiencyMultiplier() != 1) {
                var displayedNumber = Math.round((1 - addonSettings.efficiencyMultiplier()) * 100);
                tooltip.add(Component.translatable("tooltip.oritech.addon_efficiency_desc").withStyle(ChatFormatting.DARK_GRAY)
                              .append(TooltipHelper.getFormattedValueChangeTooltip(displayedNumber)));
            }
            
            if (addonSettings.addedCapacity() != 0) {
                tooltip.add(
                  Component.translatable("tooltip.oritech.addon_capacity_desc").withStyle(ChatFormatting.DARK_GRAY)
                    .append(TooltipHelper.getFormattedEnergyChangeTooltip(addonSettings.addedCapacity(), " RF")));
            }
            
            if (addonSettings.addedInsert() != 0) {
                tooltip.add(Component.translatable("tooltip.oritech.addon_transfer_desc").withStyle(ChatFormatting.DARK_GRAY)
                              .append(TooltipHelper.getFormattedEnergyChangeTooltip(addonSettings.addedInsert(), " RF/t")));
            }
            
            var item = (BlockItem) stack.getItem();
            var blockType = item.getBlock();
            
            if (blockType == BlockContent.MACHINE_YIELD_ADDON)
                tooltip.add(Component.translatable("tooltip.oritech.addon_yield_desc").withStyle(ChatFormatting.GRAY));
            if (blockType == BlockContent.MACHINE_FLUID_ADDON)
                tooltip.add(Component.translatable("tooltip.oritech.addon_fluid_desc").withStyle(ChatFormatting.GRAY));
            if (blockType == BlockContent.MACHINE_ACCEPTOR_ADDON)
                tooltip.add(Component.translatable("tooltip.oritech.addon_acceptor_desc").withStyle(ChatFormatting.GRAY));
            if (blockType == BlockContent.STEAM_BOILER_ADDON)
                tooltip.add(Component.translatable("tooltip.oritech.addon_boiler_desc").withStyle(ChatFormatting.GRAY));
            if (blockType == BlockContent.CROP_FILTER_ADDON)
                tooltip.add(Component.translatable("tooltip.oritech.addon_crop_desc").withStyle(ChatFormatting.GRAY));
            if (blockType == BlockContent.MACHINE_INVENTORY_PROXY_ADDON)
                tooltip.add(Component.translatable("tooltip.oritech.addon_proxy_desc").withStyle(ChatFormatting.GRAY));
            if (blockType == BlockContent.QUARRY_ADDON)
                tooltip.add(Component.translatable("tooltip.oritech.addon_quarry_desc").withStyle(ChatFormatting.GRAY));
            if (blockType == BlockContent.MACHINE_HUNTER_ADDON)
                tooltip.add(Component.translatable("tooltip.oritech.addon_hunter_desc").withStyle(ChatFormatting.GRAY));
            if (blockType == BlockContent.MACHINE_REDSTONE_ADDON)
                tooltip.add(Component.translatable("tooltip.oritech.addon_redstone_desc").withStyle(ChatFormatting.GRAY));
            if (blockType == BlockContent.MACHINE_PROCESSING_ADDON)
                tooltip.add(Component.translatable("tooltip.oritech.processing_addon_desc").withStyle(ChatFormatting.GRAY));
            if (blockType == BlockContent.MACHINE_SILK_TOUCH_ADDON)
                tooltip.add(Component.translatable("tooltip.oritech.addon_silk_touch_desc").withStyle(ChatFormatting.GRAY));
            
            if (addonSettings.extender()) {
                tooltip.add(Component.translatable("tooltip.oritech.addon_extender_desc").withStyle(ChatFormatting.GRAY));
            }
            
        } else {
            tooltip.add(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
        
    }
    
    static {
        MACHINE_ACCEPTOR_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        MACHINE_CAPACITOR_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        MACHINE_PROCESSING_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        MACHINE_ULTIMATE_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        CROP_FILTER_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        MACHINE_EFFICIENCY_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        MACHINE_FLUID_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        MACHINE_INVENTORY_PROXY_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        QUARRY_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        MACHINE_HUNTER_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        MACHINE_REDSTONE_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        MACHINE_SPEED_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        STEAM_BOILER_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        MACHINE_YIELD_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        MACHINE_SILK_TOUCH_ADDON_SHAPE = new VoxelShape[Direction.values().length][AttachFace.values().length];
        for (var facing : Direction.values()) {
            if (!facing.getAxis().isHorizontal()) continue;
            for (var face : AttachFace.values()) {
                MACHINE_ACCEPTOR_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.25, 0.625, 0.25, 0.75, 0.75, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.25, 0.125, 0.875, 0.375, 0.875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.375, 0.125, 0.875, 0.5, 0.875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.5, 0.125, 0.875, 0.625, 0.875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0, 0.75, 0, 1, 0.875, 1), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0, 0, 0, 1, 0.125, 1), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.875, 0.125, 0.875, 1, 0.875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.1875, 0.625, 0.1875, 0.8125, 0.75, 0.8125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.1875, 0.125, 0.1875, 0.8125, 0.25, 0.8125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.3125, 0.0625, 0.0625, 0.6875, 0.8125, 0.1875), facing, face));
                MACHINE_CAPACITOR_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.1875, 0.1875, 0.3125, 0.25, 0.375, 0.6875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.25, 0.125, 0.25, 0.75, 0.4375, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.75, 0.125, 0.1875, 0.8125, 0.5, 0.8125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5625, 0.4375, 0.25, 0.625, 0.5, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.6875, 0.4375, 0.25, 0.75, 0.5, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.3125, 0.4375, 0.25, 0.375, 0.5, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.4375, 0.25, 0.5, 0.5, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5625, 0.125, 0.1875, 0.625, 0.5, 0.25), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.6875, 0.125, 0.1875, 0.75, 0.5, 0.25), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.3125, 0.125, 0.1875, 0.375, 0.5, 0.25), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.125, 0.1875, 0.5, 0.5, 0.25), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.25, 0.125, 0.75, 0.75, 0.5, 0.8125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.8125, 0.25, 0.5625, 0.875, 0.4375, 0.6875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.8125, 0.25, 0.3125, 0.875, 0.4375, 0.4375), facing, face));
                CROP_FILTER_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.125, 0.1875, 0.875, 0.25, 0.6875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.25, 0.1875, 0.875, 0.5625, 0.6875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5625, 0.3125, 0.1875, 0.75, 0.4375, 0.8125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5625, 0.125, 0.8125, 0.75, 0.4375, 0.9375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.125, 0.125, 0.375, 0.25, 0.875), facing, face));
                MACHINE_EFFICIENCY_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.25, 0, 0.25, 0.75, 0.125, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.25, 0.125, 0.1875, 0.75, 0.4375, 0.8125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.75, 0.125, 0.125, 0.875, 0.5, 0.875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.125, 0.125, 0.25, 0.5, 0.875), facing, face));
                MACHINE_PROCESSING_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.25, 0.25, 0.875, 0.75, 0.75), facing, face));
                MACHINE_ULTIMATE_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.1875, 0.125, 0.1875, 0.875, 1, 0.75), facing, face));
                MACHINE_FLUID_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.3125, 0.000625, 0.25, 0.6875, 0.125625, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.25, 0.00125, 0.3125, 0.75, 0.12625, 0.6875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0.3125, 0.1875, 0.375, 0.625, 0.5625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5625, 0.3125, 0.1875, 0.9375, 0.625, 0.5625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.3125, 0.5, 0.625, 0.4375, 1, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.3125, 0.375, 0.625, 0.75, 0.5, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.625, 0.375, 0.5, 0.75, 0.5, 0.625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.34375, 0.125, 0.65625, 0.46875, 0.375, 0.71875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.671875, 0.125, 0.625, 0.734375, 0.375, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.1875, 0.125, 0.25, 0.375, 0.3125, 0.5), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.375, 0.375, 0.25, 0.625, 0.5625, 0.5), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5625, 0.125, 0.25, 0.75, 0.3125, 0.5), facing, face));
                MACHINE_INVENTORY_PROXY_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.125, 0.125, 0.875, 0.875, 0.875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.875, 0.375, 0.375, 1, 0.625, 0.625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0, 0.375, 0.375, 0.125, 0.625, 0.625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.375, 0.375, 0, 0.625, 0.625, 0.125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.375, 0.375, 0.875, 0.625, 0.625, 1), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.375, 0.8125, 0.375, 0.625, 1, 0.625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0, 0.00125, 0.3125, 1, 0.93875, 0.375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0, 0.00125, 0.625, 1, 0.93875, 0.6875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.3125, 0.000625, 0, 0.375, 0.938125, 1), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.625, 0.000625, 0, 0.6875, 0.938125, 1), facing, face));
                QUARRY_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face), // base
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.125, 0.125, 0.375, 0.25, 0.875), facing, face), // status bar
                  Geometry.rotateVoxelShape(Shapes.box(0.625, 0.125, 0.3125, 0.6875, 0.1875, 0.8125), facing, face), // pickaxe handle
                  Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.125, 0.25, 0.875, 0.1875, 0.4375), facing, face)); // pickaxe head
                MACHINE_HUNTER_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.125, 0.125, 0.375, 0.25, 0.875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5, 0.1875, 0.4375, 0.75, 0.25, 0.5625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.125, 0.375, 0.8125, 0.1875, 0.625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5625, 0.1875, 0.375, 0.6875, 0.25, 0.4375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5, 0.125, 0.3125, 0.75, 0.1875, 0.375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5, 0.125, 0.625, 0.75, 0.1875, 0.6875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5625, 0.1875, 0.5625, 0.6875, 0.25, 0.625), facing, face));
                MACHINE_REDSTONE_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.1875, 0.125, 0, 0.4375, 0.25, 0.8125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.1875, 0, 0.0015625, 0.6875, 0.1875, 0.0640625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.625, 0.125, 0.1875, 0.75, 0.25, 0.3125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.625, 0.125, 0.5, 0.75, 0.25, 0.625), facing, face));
                MACHINE_SPEED_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.25, 0, 0.25, 0.75, 0.125, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.375, 0.125, 0.625, 0.875, 0.25, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.25, 0.125, 0.125, 0.375, 0.25, 0.75), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.1875, 0.125, 0.25, 0.25, 0.25, 0.375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.1875, 0.125, 0.5, 0.25, 0.25, 0.625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.625, 0.125, 0.75, 0.75, 0.25, 0.8125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.375, 0.125, 0.75, 0.5, 0.25, 0.8125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.375, 0.125, 0.1875, 0.8125, 0.1875, 0.625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.5, 0.1875, 0.3125, 0.6875, 0.625, 0.5), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.1875, 0.375, 0.75, 0.5625, 0.4375), facing, face));
                STEAM_BOILER_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0, 0.125, 0.875, 0.125, 0.875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.125, 0.3125, 0.25, 0.25, 0.4375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.75, 0.125, 0.3125, 0.875, 0.25, 0.4375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.125, 0.5625, 0.5625, 0.25, 0.6875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.0000625, 0.25, 0.3125, 1.000125, 0.625, 0.6875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0, 0.1875, 0.375, 1, 0.6875, 0.625), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.000125, 0.3125, 0.25, 1.00025, 0.5625, 0.75), facing, face));
                MACHINE_YIELD_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face), // base
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.125, 0.125, 0.25, 0.375, 0.875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.75, 0.125, 0.125, 0.875, 0.375, 0.875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.25, 0.125, 0.125, 0.75, 0.375, 0.25), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.3125, 0.125, 0.3125, 0.6875, 0.25, 0.8125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0.125, 0.5625, 0.3125, 0.4375, 0.6875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.6875, 0.125, 0.5625, 0.9375, 0.4375, 0.6875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0.125, 0.3125, 0.3125, 0.4375, 0.4375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.6875, 0.125, 0.3125, 0.9375, 0.4375, 0.4375), facing, face));
                MACHINE_SILK_TOUCH_ADDON_SHAPE[facing.ordinal()][face.ordinal()] = Shapes.or(
                  Geometry.rotateVoxelShape(Shapes.box(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.125, 0.125, 0.125, 0.375, 0.25, 0.875), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.625, 0.1875, 0.3125, 0.6875, 0.25, 0.8125), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.1875, 0.25, 0.875, 0.25, 0.4375), facing, face),
                  Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.125, 0.1875, 0.875, 0.1875, 0.875), facing, face)
                );
            }
        }
    }
    
    // AddonSettings is an immutable configuration record for a machine addon, and should be constructed in BlockContent
    public record AddonSettings(boolean extender, float speedMultiplier, float efficiencyMultiplier, long addedCapacity,
                                long addedInsert, boolean acceptEnergy, boolean needsSupport, int chamberCount,
                                VoxelShape[][] boundingShape) {
        public static AddonSettings getDefaultSettings() {
            return new AddonSettings(false, 1.0f, 1.0f, 0, 0, false, true, 0, null);
        }
        
        // extender and needsSupport aren't strictly exclusive, but are unlikely to be used together
        public AddonSettings withExtender(boolean newExtender) {
            return new AddonSettings(newExtender, speedMultiplier, efficiencyMultiplier, addedCapacity, addedInsert, acceptEnergy, needsSupport, chamberCount, boundingShape);
        }
        
        public AddonSettings withSpeedMultiplier(float newMultiplier) {
            return new AddonSettings(extender, newMultiplier, efficiencyMultiplier, addedCapacity, addedInsert, acceptEnergy, needsSupport, chamberCount, boundingShape);
        }
        
        public AddonSettings withEfficiencyMultiplier(float newMultiplier) {
            return new AddonSettings(extender, speedMultiplier, newMultiplier, addedCapacity, addedInsert, acceptEnergy, needsSupport, chamberCount, boundingShape);
        }
        
        public AddonSettings withAddedCapacity(long newCapacity) {
            return new AddonSettings(extender, speedMultiplier, efficiencyMultiplier, newCapacity, addedInsert, acceptEnergy, needsSupport, chamberCount, boundingShape);
        }
        
        public AddonSettings withAddedInsert(long newInsert) {
            return new AddonSettings(extender, speedMultiplier, efficiencyMultiplier, addedCapacity, newInsert, acceptEnergy, needsSupport, chamberCount, boundingShape);
        }
        
        public AddonSettings withAcceptEnergy(boolean newAccept) {
            return new AddonSettings(extender, speedMultiplier, efficiencyMultiplier, addedCapacity, addedInsert, newAccept, needsSupport, chamberCount, boundingShape);
        }
        
        public AddonSettings withNeedsSupport(boolean newSupport) {
            return new AddonSettings(extender, speedMultiplier, efficiencyMultiplier, addedCapacity, addedInsert, acceptEnergy, newSupport, chamberCount, boundingShape);
        }
        
        public AddonSettings withChambers(int chambers) {
            return new AddonSettings(extender, speedMultiplier, efficiencyMultiplier, addedCapacity, addedInsert, acceptEnergy, needsSupport, chambers, boundingShape);
        }
        
        // boundingShape should only be set if needsSupport is also set
        public AddonSettings withBoundingShape(VoxelShape[][] newShape) {
            return new AddonSettings(extender, speedMultiplier, efficiencyMultiplier, addedCapacity, addedInsert, acceptEnergy, needsSupport, chamberCount, newShape);
        }
    }
}
