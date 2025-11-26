package rearth.oritech.block.blocks.reactor;

public class ReactorWallBlock extends BaseReactorBlock {
    public ReactorWallBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public boolean validForWalls() {
        return true;
    }
}
