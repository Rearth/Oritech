package rearth.oritech.block.blocks.machines.addons;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.block.entity.machines.addons.AddonBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.TooltipHelper;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
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
                               .with(FACING, Direction.NORTH)
                               .with(FACE, BlockFace.FLOOR)
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
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        super.appendTooltip(stack, world, tooltip, options);
        
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
            if (blockType == BlockContent.CROP_FILTER_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_crop_desc").formatted(Formatting.DARK_GRAY));
            if (blockType == BlockContent.MACHINE_INVENTORY_PROXY_ADDON)
                tooltip.add(Text.translatable("tooltip.oritech.addon_proxy_desc").formatted(Formatting.DARK_GRAY));
        } else {
            tooltip.add(Text.translatable("tooltip.oritech.item_extra_info").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        }
        
    }
}
