package rearth.oritech.block.base;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public abstract class MultiblockMachine extends MachineBlock {
    
    public static final BooleanProperty ASSEMBLED = BooleanProperty.of("machine_assembled");
    
    public MultiblockMachine(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(ASSEMBLED, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ASSEMBLED);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        if (!world.isClient) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof MultiblockMachineEntity machineEntity)) {
                return ActionResult.FAIL;
            }
            
            var isAssembled = machineEntity.initMultiblock(state);
            
            if (!isAssembled) {
                player.sendMessage(Text.literal("Machine is not assembled"));
                return ActionResult.FAIL;
            }
            
        }
        
        return super.onUse(state, world, pos, player, hand, hit);
    }
}
