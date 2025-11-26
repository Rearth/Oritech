package rearth.oritech.block.entity.generators;

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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BioGeneratorEntity extends MultiblockGeneratorBlockEntity {
    public BioGeneratorEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BIO_GENERATOR_ENTITY, pos, state, Oritech.CONFIG.generators.bioGeneratorData.energyPerTick());
    }
    
    @Override
    protected Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level world) {
        
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
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.BIO_GENERATOR;
    }
    
    @Override
    public InventorySlotAssignment getSlotAssignments() {
        return new InventorySlotAssignment(0, 1, 1, 0);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 80, 21));
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
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
