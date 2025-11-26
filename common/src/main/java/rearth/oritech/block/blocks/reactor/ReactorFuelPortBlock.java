package rearth.oritech.block.blocks.reactor;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.reactor.ReactorFuelPortEntity;

public class ReactorFuelPortBlock extends BaseReactorBlock implements EntityBlock {
    public ReactorFuelPortBlock(Properties settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorFuelPortEntity(pos, state);
    }
    
    @Override
    public boolean validForWalls() {
        return true;
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof ReactorFuelPortEntity) {
            var handler = (ExtendedMenuProvider) world.getBlockEntity(pos);
                MenuRegistry.openExtendedMenu((ServerPlayer) player, handler);
        }
        
        return InteractionResult.SUCCESS;
    }
}
