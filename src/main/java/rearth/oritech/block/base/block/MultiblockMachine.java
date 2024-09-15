package rearth.oritech.block.base.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.MultiblockMachineController;

public abstract class MultiblockMachine extends UpgradableMachineBlock {
    
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof MultiblockMachineController machineEntity)) {
                return ActionResult.SUCCESS;
            }
            
            var wasAssembled = state.get(ASSEMBLED);
            
            if (!wasAssembled) {
                var corePlaced = machineEntity.tryPlaceNextCore(player);
                if (corePlaced) return ActionResult.SUCCESS;
            }
            
            var isAssembled = machineEntity.initMultiblock(state);
            
            // first time created
            if (isAssembled && !wasAssembled) {
                NetworkContent.MACHINE_CHANNEL.serverHandle(entity).send(new NetworkContent.MachineSetupEventPacket(pos));
                if (entity instanceof MachineAddonController controllerEntity)
                    controllerEntity.initAddons();
                return ActionResult.SUCCESS;
            }
            
            if (!isAssembled) {
                player.sendMessage(Text.translatable("message.oritech.machine.missing_core"));
                return ActionResult.SUCCESS;
            }
            
        }
        
        return super.onUse(state, world, pos, player, hit);
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        
        if (!world.isClient() && state.get(ASSEMBLED)) {
            
            var entity = world.getBlockEntity(pos);
            if (entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }
        }
        
        return super.onBreak(world, pos, state, player);
    }
}
