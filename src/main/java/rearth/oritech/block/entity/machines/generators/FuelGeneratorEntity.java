package rearth.oritech.block.entity.machines.generators;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.block.base.entity.FluidMultiblockGeneratorBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventorySlotAssignment;
import team.reborn.energy.api.EnergyStorage;

import java.util.List;

public class FuelGeneratorEntity extends FluidMultiblockGeneratorBlockEntity {
    public FuelGeneratorEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FUEL_GENERATOR_ENTITY, pos, state, 64);
    }
    
    protected Multimap<Direction, BlockApiCache<EnergyStorage, Direction>> getNeighborCaches(BlockPos pos, World world) {
        
        var res = ArrayListMultimap.<Direction, BlockApiCache<EnergyStorage, Direction>>create();
        
        // because facing blocks make rotations and relative offsets a nightmare...
        var posA = new Vec3i(1, 0, -1);
        var corePosA = new Vec3i(0, 0, -1);
        var posB = new Vec3i(1, 0, 1);
        var facing = getFacingForAddon();
        var worldPosA = (BlockPos) Geometry.offsetToWorldPosition(facing, posA, pos);
        var coreWorldPosA = (BlockPos) Geometry.offsetToWorldPosition(facing, corePosA, pos);
        var worldPosB = (BlockPos) Geometry.offsetToWorldPosition(facing, posB, pos);
        var offset = worldPosA.subtract(coreWorldPosA);
        
        var direction = Direction.fromVector(offset.getX(), offset.getY(), offset.getZ());
        
        var cacheA = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, worldPosA);
        res.put(direction, cacheA);
        var cacheB = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, worldPosB);
        res.put(direction, cacheB);
        
        return res;
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.FUEL_GENERATOR;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 0, 0, 0);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of();
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.FUEL_GENERATOR_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 0;
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
