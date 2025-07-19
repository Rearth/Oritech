package rearth.oritech.block.blocks.pipes.energy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.TriFunction;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import java.util.HashMap;
import java.util.List;

public class SuperConductorBlock extends GenericPipeBlock {
    
    public static HashMap<Identifier, GenericPipeInterfaceEntity.PipeNetworkData> SUPERCONDUCTOR_DATA = new HashMap<>();
    
    public SuperConductorBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public TriFunction<World, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((world, pos, direction) -> EnergyApi.BLOCK.find(world, pos, direction) != null);
    }
    
    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.SUPERCONDUCTOR_CONNECTION.getDefaultState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.SUPERCONDUCTOR.getDefaultState();
    }
    
    @Override
    public String getPipeTypeName() {
        return "superconductor";
    }
    
    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof SuperConductorBlock || block instanceof SuperConductorConnectionBlock || block instanceof SuperConductorDuctBlock;
    }
    
    @Override
    protected VoxelShape[] createShapes() {
        return EXTRA_THICK_SHAPES;
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world) {
        return SUPERCONDUCTOR_DATA.computeIfAbsent(world.getRegistryKey().getValue(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }
    
    @Override
    public boolean isCompatibleTarget(Block block) {
        return !block.equals(BlockContent.ENERGY_PIPE_CONNECTION);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        var text = Text.translatable("tooltip.oritech.energy_max_transfer").formatted(Formatting.GRAY)
            .append(Text.translatable("tooltip.oritech.energy_transfer_rate", Oritech.CONFIG.superConductorTransferRate()).formatted(Formatting.GOLD));
        tooltip.add(text);
        tooltip.add(Text.translatable("tooltip.oritech.superconductor").formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, options);
    }

	public static class FramedSuperConductorBlock extends SuperConductorBlock {

		public FramedSuperConductorBlock(Settings settings) {
			super(settings);
		}

		@Override
		public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
			return VoxelShapes.fullCube();
		}

		@Override
		public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
			return state.getOutlineShape(world, pos);
		}

		@Override
		public BlockState getNormalBlock() {
			return BlockContent.FRAMED_SUPERCONDUCTOR.getDefaultState();
		}

		@Override
		public BlockState getConnectionBlock() {
			return BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION.getDefaultState();
		}
	}
}
