package rearth.oritech.block.entity.generators;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.base.entity.FluidMultiblockGeneratorBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LavaGeneratorEntity extends FluidMultiblockGeneratorBlockEntity {
    public LavaGeneratorEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.LAVA_GENERATOR_ENTITY.get(), pos, state, OritechConfig.generators.lavaGeneratorData.energyPerTick.get());
    }

    @Override
    protected Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level level) {

        var res = new HashSet<Tuple<BlockPos, Direction>>();
        res.add(new Tuple<>(pos.above(2), Direction.DOWN));
        res.add(new Tuple<>(pos.below(), Direction.DOWN));
        res.add(new Tuple<>(pos.east(), Direction.WEST));
        res.add(new Tuple<>(pos.east().above(), Direction.WEST));
        res.add(new Tuple<>(pos.south(), Direction.NORTH));
        res.add(new Tuple<>(pos.south().above(), Direction.NORTH));
        res.add(new Tuple<>(pos.west(), Direction.EAST));
        res.add(new Tuple<>(pos.west().above(), Direction.EAST));
        res.add(new Tuple<>(pos.north(), Direction.SOUTH));
        res.add(new Tuple<>(pos.north().above(), Direction.SOUTH));

        return res;

    }

    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.LAVA_GENERATOR.get();
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.LAVA_GENERATOR_SCREEN.get();
    }

    @Override
    public long getDefaultExtractionRate() {
        return OritechConfig.generators.lavaGeneratorData.maxEnergyExtraction.get();
    }

    @Override
    public long getDefaultCapacity() {
        return OritechConfig.generators.lavaGeneratorData.energyCapacity.get();
    }

    @Override
    public List<Vec3i> getAddonSlots() {

        return List.of(
                new Vec3i(1, 0, 0),
                new Vec3i(1, 1, 0)
        );
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
                new Vec3i(0, 1, 0)
        );
    }
}
