package rearth.oritech.block.entity.machines.generators;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.List;
import java.util.Map;

public class BasicGeneratorEntity extends UpgradableGeneratorBlockEntity {
    
    public static final Map<Item, Integer> FUEL_MAP = AbstractFurnaceBlockEntity.createFuelTimeMap();
    
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
        
        var fuelTime = FUEL_MAP.getOrDefault(firstItem.getItem(), 0);
        if (fuelTime > 0) {
            if (firstItem.getItem() instanceof BucketItem) {
                this.getInputView().set(0, ItemVariant.of(Items.BUCKET, firstItem.getComponentChanges()).toStack());
            } else {
                firstItem.decrement(1);
            }
            progress = fuelTime;
            setCurrentMaxBurnTime(fuelTime);
            markNetDirty();
            markDirty();
        }
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
