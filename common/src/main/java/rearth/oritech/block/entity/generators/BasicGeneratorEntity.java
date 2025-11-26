package rearth.oritech.block.entity.generators;

import dev.architectury.registry.fuel.FuelRegistry;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
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
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BasicGeneratorEntity extends UpgradableGeneratorBlockEntity {
    
    public BasicGeneratorEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BASIC_GENERATOR_ENTITY, pos, state, Oritech.CONFIG.generators.basicGeneratorData.energyPerTick());
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.BIO_GENERATOR;
    }
    
    @Override
    protected float getAnimationSpeed() {
        return 1 * getSpeedMultiplier();
    }
    
    @Override
    protected void tryConsumeInput() {
        var firstItem = this.getInputView().get(0);
        if (firstItem.isEmpty() || firstItem.getItem() instanceof BucketItem) return;
        
        var fuelTime = FuelRegistry.get(firstItem);
        if (fuelTime > 0) {
            if (firstItem.getItem() instanceof BucketItem) {
                this.getInputView().set(0, Items.BUCKET.getDefaultInstance());
            } else {
                firstItem.shrink(1);
            }
            progress = fuelTime;
            setCurrentMaxBurnTime(fuelTime);
            setChanged();
        }
    }
    
    @Override
    protected Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level world) {
        
        var res = new HashSet<Tuple<BlockPos, Direction>>();
        res.add(new Tuple<>(pos.above(), Direction.DOWN));
        res.add(new Tuple<>(pos.below(), Direction.DOWN));
        res.add(new Tuple<>(pos.east(), Direction.WEST));
        res.add(new Tuple<>(pos.south(), Direction.NORTH));
        res.add(new Tuple<>(pos.west(), Direction.EAST));
        res.add(new Tuple<>(pos.north(), Direction.SOUTH));
        
        return res;
        
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
        return ModScreens.BASIC_GENERATOR_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 1;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
        );
    }
    
    @Override
    public long getDefaultExtractionRate() {
        return Oritech.CONFIG.generators.basicGeneratorData.maxEnergyExtraction();
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.generators.basicGeneratorData.energyCapacity();
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
}
