package rearth.oritech.block.blocks.processing;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.MachineBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.interaction.DeepDrillEntity;
import rearth.oritech.util.MultiblockMachineController;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class MachineCoreBlock extends Block implements BlockEntityProvider {
    
    public static final BooleanProperty USED = BooleanProperty.of("core_used");
    
    private final float coreQuality;
    
    public MachineCoreBlock(Settings settings, float coreQuality) {
        super(settings);
        this.setDefaultState(getDefaultState().with(USED, false));
        this.coreQuality = coreQuality;
    }
    
    public float getCoreQuality() {
        return coreQuality;
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(USED);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.translatable("tooltip.oritech.machine_core_block").formatted(Formatting.ITALIC, Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, options);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return state.get(USED) ? BlockRenderType.INVISIBLE : BlockRenderType.MODEL;
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        onBlockRemoved(state, world, pos);
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    protected void onExploded(BlockState state, World world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {
        onBlockRemoved(state, world, pos);
        super.onExploded(state, world, pos, explosion, stackMerger);
    }
    
    private static void onBlockRemoved(BlockState state, World world, BlockPos pos) {
        if (!world.isClient() && state.get(USED)) {
            var controllerEntity = getControllerEntity(world, pos);
            if (controllerEntity == null) return;
            
            if (controllerEntity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onCoreBroken(pos);
            }
        }
    }
    
    @NotNull
    public static BlockPos getControllerPos(World world, BlockPos pos) {
        var coreEntity = (MachineCoreEntity) world.getBlockEntity(pos);
        return Objects.requireNonNull(coreEntity).getControllerPos();
    }
    
    @Nullable
    public static BlockEntity getControllerEntity(World world, BlockPos pos) {
        return world.getBlockEntity(getControllerPos(world, pos));
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!state.get(USED)) return ActionResult.PASS;
        
        if (!world.isClient) {
            var controllerPos = getControllerPos(world, pos);
            var controllerBlock = world.getBlockState(controllerPos);
            var controllerEntity = world.getBlockEntity(controllerPos);
            if (controllerEntity instanceof DeepDrillEntity deepDrill && !deepDrill.init(true)) {
                player.sendMessage(Text.translatable("message.oritech.deep_drill.ore_placement"));
                return ActionResult.SUCCESS;
            } else {
                return controllerBlock.getBlock().onUse(controllerBlock, world, controllerPos, player, hit);
            }
        }
        
        return ActionResult.SUCCESS;
        
    }
    
    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        if (!state.get(USED)) super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        
        if (!world.isClient) {
            var controllerPos = getControllerPos(world, pos);
            var controllerBlock = world.getBlockState(controllerPos);
            if (controllerBlock.getBlock() instanceof MachineBlock machineBlock) {
                return machineBlock.onUseWithItem(stack, state, world, pos, player, hand, hit);
            } else if (controllerBlock.getBlock() instanceof RefineryModuleBlock machineBlock) {
                return machineBlock.onUseWithItem(stack, state, world, pos, player, hand, hit);
            }
        }
        
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCoreEntity(pos, state);
    }
}
