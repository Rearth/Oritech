package rearth.oritech.block.blocks.pipes.energy;

import org.apache.commons.lang3.function.TriFunction;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import java.util.HashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EnergyPipeBlock extends GenericPipeBlock {
    
    public static HashMap<ResourceLocation, GenericPipeInterfaceEntity.PipeNetworkData> ENERGY_PIPE_DATA = new HashMap<>();
    
    public EnergyPipeBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((world, pos, direction) -> EnergyApi.BLOCK.find(world, pos, direction) != null);
    }
    
    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.ENERGY_PIPE_CONNECTION.defaultBlockState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.ENERGY_PIPE.defaultBlockState();
    }

    @Override
    protected VoxelShape[] createShapes() {
        return THIN_SHAPES;
    }
    
    @Override
    public String getPipeTypeName() {
        return "energy";
    }
    
    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof EnergyPipeBlock || block instanceof EnergyPipeConnectionBlock || block instanceof EnergyPipeDuctBlock;
    }
    
    @Override
    public boolean isCompatibleTarget(Block block) {
        return !block.equals(BlockContent.SUPERCONDUCTOR_CONNECTION);
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level world) {
        return ENERGY_PIPE_DATA.computeIfAbsent(world.dimension().location(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        var text = Component.translatable("tooltip.oritech.energy_max_transfer").withStyle(ChatFormatting.GRAY)
            .append(Component.translatable("tooltip.oritech.energy_transfer_rate", Oritech.CONFIG.energyPipeTransferRate()).withStyle(ChatFormatting.GOLD));
        tooltip.add(text);
        super.appendHoverText(stack, context, tooltip, options);
    }

	public static class FramedEnergyPipeBlock extends EnergyPipeBlock {

		public FramedEnergyPipeBlock(Properties settings) {
			super(settings);
		}

		@Override
		public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
			return Shapes.block();
		}

		@Override
		public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
			return state.getShape(world, pos);
		}

		@Override
		public BlockState getNormalBlock() {
			return BlockContent.FRAMED_ENERGY_PIPE.defaultBlockState();
		}

		@Override
		public BlockState getConnectionBlock() {
			return BlockContent.FRAMED_ENERGY_PIPE_CONNECTION.defaultBlockState();
		}
	}
}
