package rearth.oritech.block.custom;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;

public class MachineCoreBlock extends Block {
    
    public static final BooleanProperty USED = BooleanProperty.of("core_used");
    public static final IntProperty CONTROLLER_X = IntProperty.of("linked_x", 0, 7);
    public static final IntProperty CONTROLLER_Y = IntProperty.of("linked_y", 0, 7);
    public static final IntProperty CONTROLLER_Z = IntProperty.of("linked_z", 0, 7);
    
    public MachineCoreBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState()
                               .with(USED, false)
                               .with(CONTROLLER_X, 1)
                               .with(CONTROLLER_Y, 1)
                               .with(CONTROLLER_Z, 1)
        );
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(USED, CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return state.get(USED) ? BlockRenderType.INVISIBLE : BlockRenderType.MODEL;
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {

        if (!world.isClient() && state.get(USED)) {
            var controllerEntity = getControllerEntity(world, pos, state);
            
            if (controllerEntity instanceof MultiblockMachineEntity machineEntity) {
                machineEntity.onCoreBroken(pos, state);
            }
        }

        return super.onBreak(world, pos, state, player);
    }
    
    @Nullable
    private static BlockEntity getControllerEntity(World world, BlockPos pos, BlockState state) {
        var offset = new Vec3i(state.get(CONTROLLER_X) - 4, state.get(CONTROLLER_Y) - 4, state.get(CONTROLLER_Z) - 4);
        
        var controllerPos = pos.add(offset);
        return world.getBlockEntity(controllerPos);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        if (!state.get(USED)) return ActionResult.PASS;
        
        if (!world.isClient) {
            var controllerEntity = getControllerEntity(world, pos, state);
            
            if (controllerEntity instanceof MultiblockMachineEntity machineEntity) {
                var handler = (ExtendedScreenHandlerFactory) machineEntity;
                player.openHandledScreen(handler);
            }
        }
        
        return ActionResult.SUCCESS;
        
    }
}
