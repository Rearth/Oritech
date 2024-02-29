package rearth.oritech.block.blocks.machines;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.FrameInteractionBlock;
import rearth.oritech.block.entity.machines.DestroyerBlockEntity;
import rearth.oritech.block.entity.machines.PlacerBlockEntity;
import rearth.oritech.util.InventoryInputMode;

import java.util.List;

public class DestroyerBlock extends FrameInteractionBlock {
    public DestroyerBlock(Settings settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DestroyerBlockEntity(pos, state);
    }
}
