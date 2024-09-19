package rearth.oritech.block.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.block.entity.machines.interaction.DeepDrillEntity;
import rearth.oritech.util.MultiblockMachineController;

import java.util.Objects;

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
    public BlockRenderType getRenderType(BlockState state) {
        return state.get(USED) ? BlockRenderType.INVISIBLE : BlockRenderType.MODEL;
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        
        if (!world.isClient() && state.get(USED)) {
            var controllerEntity = getControllerEntity(world, pos);
            if (controllerEntity == null) return state;
            
            if (controllerEntity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onCoreBroken(pos);
            }
        }
        
        return super.onBreak(world, pos, state, player);
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
            if (controllerEntity instanceof DeepDrillEntity deepDrill && !deepDrill.init()) {
                player.sendMessage(Text.translatable("message.oritech.deep_drill.ore_placement"));
                return ActionResult.SUCCESS;
            } else {
                return controllerBlock.getBlock().onUse(controllerBlock, world, controllerPos, player, hit);
            }
        }
        
        return ActionResult.SUCCESS;
        
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCoreEntity(pos, state);
    }
}
