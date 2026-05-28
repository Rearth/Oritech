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
import rearth.oritech.util.Geometry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FuelGeneratorEntity extends FluidMultiblockGeneratorBlockEntity {
    public FuelGeneratorEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FUEL_GENERATOR_ENTITY.get(), pos, state, OritechConfig.generators.fuelGeneratorData.energyPerTick.get());
    }
    
    @Override
    protected Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level level) {
        
        // because facing blocks make rotations and relative offsets a nightmare...
        var posA = new Vec3i(1, 0, -1);
        var corePosA = new Vec3i(0, 0, -1);
        var posB = new Vec3i(1, 0, 1);
        var facing = getFacingForAddon();
        var worldPosA = (BlockPos) Geometry.offsetToWorldPosition(facing, posA, pos);
        var coreWorldPosA = (BlockPos) Geometry.offsetToWorldPosition(facing, corePosA, pos);
        var worldPosB = (BlockPos) Geometry.offsetToWorldPosition(facing, posB, pos);
        var offset = worldPosA.subtract(coreWorldPosA);
        
        var direction = Direction.getApproximateNearest(offset.getX(), offset.getY(), offset.getZ());
        
        var res = new HashSet<Tuple<BlockPos, Direction>>();
        
        res.add(new Tuple<>(worldPosA, direction));
        res.add(new Tuple<>(worldPosB, direction));
        
        return res;
        
    }
    
    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.FUEL_GENERATOR.get();
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.FUEL_GENERATOR_SCREEN.get();
    }
    
    @Override
    public long getDefaultExtractionRate() {
        return OritechConfig.generators.fuelGeneratorData.maxEnergyExtraction.get();
    }
    
    @Override
    public long getDefaultCapacity() {
        return OritechConfig.generators.fuelGeneratorData.energyCapacity.get();
    }
    
    @Override
    protected float getAnimationSpeed() {
        return super.getAnimationSpeed() / 3;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        
        return List.of(
          new Vec3i(1, 0, 0),
          new Vec3i(-3, 0, 0)
        );
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 0, 1),
          new Vec3i(0, 0, -1),
          new Vec3i(-1, 0, 1),
          new Vec3i(-1, 0, 0),
          new Vec3i(-1, 0, -1),
          new Vec3i(-2, 0, 1),
          new Vec3i(-2, 0, 0),
          new Vec3i(-2, 0, -1),
          new Vec3i(0, 1, 1),
          new Vec3i(0, 1, 0),
          new Vec3i(0, 1, -1),
          new Vec3i(-1, 1, 1),
          new Vec3i(-1, 1, 0),
          new Vec3i(-1, 1, -1),
          new Vec3i(-2, 1, 1),
          new Vec3i(-2, 1, 0),
          new Vec3i(-2, 1, -1)
        );
    }
}
