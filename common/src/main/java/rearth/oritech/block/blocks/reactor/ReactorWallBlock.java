package rearth.oritech.block.blocks.reactor;

public class ReactorWallBlock extends BaseReactorBlock {
    public ReactorWallBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public boolean validForWalls() {
        return true;
    }
}
