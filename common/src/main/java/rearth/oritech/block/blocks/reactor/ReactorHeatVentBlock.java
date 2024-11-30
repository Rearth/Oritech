package rearth.oritech.block.blocks.reactor;

public class ReactorHeatVentBlock extends BaseReactorBlock {
    public ReactorHeatVentBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public boolean validForWalls() {
        return true;
    }
}
