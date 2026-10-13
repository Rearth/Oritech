package rearth.oritech.spaceage.block.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AsteroidBlock extends Block {

    public static final EnumProperty<Ore> ORE = EnumProperty.create("ore", Ore.class);

    private final VoxelShape shape;

    public AsteroidBlock(Properties properties, int variant) {

        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ORE, Ore.IRON));
        shape = switch (variant) {
            case 1 -> Shapes.or(Block.box(1, 0, 2, 13, 12, 14), Block.box(7, 6, 5, 16, 15, 16));
            case 2 -> Shapes.or(Block.box(0, 0, 3, 11, 11, 14), Block.box(7, 3, 1, 16, 12, 10));
            case 3 -> Shapes.or(Block.box(1, 0, 1, 15, 8, 15), Block.box(4, 5, 4, 14, 14, 14));
            default -> throw new IllegalArgumentException("Unknown asteroid variant: " + variant);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(ORE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide())
            level.setBlockAndUpdate(pos, state.cycle(ORE));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return shape;
    }

    public enum Ore implements StringRepresentable {
        IRON("iron"), COPPER("copper"), GOLD("gold"), NICKEL("nickel"), PLATINUM("platinum"),
        URANIUM("uranium"), REDSTONE("redstone"), LAPIS("lapis"), DIAMOND("diamond"), EMERALD("emerald");

        private final String name;

        Ore(String name) {

            this.name = name;
        }

        @Override
        public String getSerializedName() {

            return name;
        }
    }
}
