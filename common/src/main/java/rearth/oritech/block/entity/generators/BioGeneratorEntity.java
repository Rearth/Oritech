package rearth.oritech.block.entity.generators;

import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockGeneratorBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BioGeneratorEntity extends MultiblockGeneratorBlockEntity {
    public BioGeneratorEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BIO_GENERATOR_ENTITY, pos, state, Oritech.CONFIG.generators.bioGeneratorData.energyPerTick());
    }
    
    @Override
    protected Set<Pair<BlockPos, Direction>> getOutputTargets(BlockPos pos, World world) {
        
        var res = new HashSet<Pair<BlockPos, Direction>>();
        res.add(new Pair<>(pos.up(2), Direction.DOWN));
        res.add(new Pair<>(pos.down(), Direction.DOWN));
        res.add(new Pair<>(pos.east(), Direction.WEST));
        res.add(new Pair<>(pos.east().up(), Direction.WEST));
        res.add(new Pair<>(pos.south(), Direction.NORTH));
        res.add(new Pair<>(pos.south().up(), Direction.NORTH));
        res.add(new Pair<>(pos.west(), Direction.EAST));
        res.add(new Pair<>(pos.west().up(), Direction.EAST));
        res.add(new Pair<>(pos.north(), Direction.SOUTH));
        res.add(new Pair<>(pos.north().up(), Direction.SOUTH));
        
        return res;
        
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.BIO_GENERATOR;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 1, 1, 0);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 80, 21));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.BIO_GENERATOR_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 2;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        
        return List.of(
          new Vec3i(1, 0, 0),
          new Vec3i(1, 1, 0)
        );
    }
    
    @Override
    public long getDefaultExtractionRate() {
        return Oritech.CONFIG.generators.bioGeneratorData.maxEnergyExtraction();
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.generators.bioGeneratorData.energyCapacity();
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1, 0)
        );
    }
}
