package rearth.oritech.block.fluid;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;

public abstract class FuelFluid extends MineralFluid {
    
    @Override
    public Fluid getStill() {
        return FluidContent.STILL_FUEL;
    }
    
    @Override
    public Fluid getFlowing() {
        return FluidContent.FLOWING_FUEL;
    }
    
    @Override
    public Item getBucketItem() {
        return ItemContent.FUEL_BUCKET;
    }
    
    @Override
    protected BlockState toBlockState(FluidState state) {
        return BlockContent.FUEL_FLUID_BLOCK.getDefaultState().with(Properties.LEVEL_15, getBlockStateLevel(state));
    }
    
    public static class Flowing extends FuelFluid {
        
        @Override
        protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }
        
        @Override
        public boolean isStill(FluidState state) {
            return false;
        }
        
        @Override
        public int getLevel(FluidState state) {
            return state.get(LEVEL);
        }
    }
    
    public static class Still extends FuelFluid {
        
        @Override
        public boolean isStill(FluidState state) {
            return true;
        }
        
        @Override
        public int getLevel(FluidState state) {
            return 8;
        }
    }
}
