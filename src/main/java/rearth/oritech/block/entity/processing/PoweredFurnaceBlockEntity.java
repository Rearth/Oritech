package rearth.oritech.block.entity.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeInput;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.List;
import java.util.Optional;

public class PoweredFurnaceBlockEntity extends MultiblockMachineEntity {

    private final float FURNACE_SPEED_MULTIPLIER = OritechConfig.processingMachines.furnaceData.speedMultiplier.get().floatValue();

    public PoweredFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.POWERED_FURNACE_ENTITY.get(), pos, state, OritechConfig.processingMachines.furnaceData.energyPerTick.get());
    }

    @Override
    public long getDefaultCapacity() {
        return OritechConfig.processingMachines.furnaceData.energyCapacity.get();
    }

    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.processingMachines.furnaceData.maxEnergyInsertion.get();
    }

    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.FURNACE_ADAPTER.get();
    }   // not used in this special case

    @Override
    protected float calculateEnergyUsage() {
        return energyPerTick * getEfficiencyMultiplier() * (1 / getSpeedMultiplier()) / 2;
    }

    // uses a furnace recipe type adapter to
    @Override
    protected OritechRecipe loadRecipeFromInput(ServerLevel serverLevel, OritechRecipeInput recipeInput, RecipeType<OritechRecipe> type) {
        if (recipeInput.isEmpty()) return OritechRecipe.EMPTY;

        // existing recipe matches (if non-empty)
        if (!currentRecipe.isEmpty() && currentRecipe.matches(recipeInput, level)) return currentRecipe;

        var input = new SingleRecipeInput(inventory.getItem(0));

        var recipeCandidate = serverLevel.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
        if (recipeCandidate.isEmpty()) return OritechRecipe.EMPTY;

        var furnaceRecipe = recipeCandidate.get().value();

        return new OritechRecipe(List.of(furnaceRecipe.input()), List.of(furnaceRecipe.result), Optional.empty(), List.of(), furnaceRecipe.cookingTime(), RecipeContent.FURNACE_ADAPTER.get());
    }

    @Override
    protected void onProgressed() {
        super.onProgressed();

        if (level.getGameTime() % 18 == 0)
            updateFurnaceState(getBlockState());
    }

    private void updateFurnaceState(BlockState state) {
        var wasLit = state.getValue(BlockStateProperties.LIT);
        var isLit = isActivelyWorking();

        if (wasLit != isLit) {
            level.setBlockAndUpdate(worldPosition, state.setValue(BlockStateProperties.LIT, isLit));
        }

    }

    @Override
    public float getSpeedMultiplier() {
        return super.getSpeedMultiplier() * FURNACE_SPEED_MULTIPLIER;
    }

    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 1, 1, 1);
    }

    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 56, 38),
                new GuiSlot(1, 117, 38, true));
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.POWERED_FURNACE_SCREEN.get();
    }

    @Override
    public int getInventorySize() {
        return 2;
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
                new Vec3i(0, 1, 0)
        );
    }

    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
                new Vec3i(0, -1, 0)
        );
    }
}
