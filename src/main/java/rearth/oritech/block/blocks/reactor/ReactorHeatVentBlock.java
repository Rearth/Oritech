package rearth.oritech.block.blocks.reactor;

public class ReactorHeatVentBlock extends BaseReactorBlock {
    public ReactorHeatVentBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public boolean validForWalls() {
        return true;
    }
}
