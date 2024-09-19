package rearth.oritech.block.base.block;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.entity.FrameInteractionBlockEntity;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.util.MachineAddonController;

import java.util.List;
import java.util.Objects;

import static rearth.oritech.util.TooltipHelper.addMachineTooltip;

public abstract class FrameInteractionBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    
    public static final BooleanProperty HAS_FRAME = BooleanProperty.of("has_frame");
    
    public FrameInteractionBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH).with(HAS_FRAME, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING, HAS_FRAME);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return null;
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof FrameInteractionBlockEntity machineEntity)) {
                return ActionResult.SUCCESS;
            }
            
            var frameValid = machineEntity.tryFindFrame();
            world.setBlockState(pos, state.with(HAS_FRAME, frameValid));
            
            if (frameValid) {
                if (entity instanceof MachineAddonController addonController)
                    addonController.initAddons();
                
                var handler = (ExtendedScreenHandlerFactory) world.getBlockEntity(pos);
                player.openHandledScreen(handler);
            } else {
                player.sendMessage(Text.translatable("message.oritech.machine_frame.missing_frame"));
            }
            
        }
        return ActionResult.SUCCESS;
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        
        if (!world.isClient() && state.get(HAS_FRAME)) {
            
            var ownEntity = (FrameInteractionBlockEntity) world.getBlockEntity(pos);
            ownEntity.cleanup();
            
            if (ownEntity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }
            
            if (ownEntity instanceof ItemEnergyFrameInteractionBlockEntity itemContainer) {
                var stacks = itemContainer.inventory.heldStacks;
                for (var stack : stacks) {
                    if (!stack.isEmpty()) {
                        var itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                        world.spawnEntity(itemEntity);
                    }
                }
            }
        }
        
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        addMachineTooltip(tooltip, this, this);
    }
}
