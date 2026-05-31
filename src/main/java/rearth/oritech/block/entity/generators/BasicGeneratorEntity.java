package rearth.oritech.block.entity.generators;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BasicGeneratorEntity extends UpgradableGeneratorBlockEntity {

    public BasicGeneratorEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BASIC_GENERATOR_ENTITY.get(), pos, state, OritechConfig.generators.basicGeneratorData.energyPerTick.get());
    }

    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.BIO_GENERATOR.get();
    }

    @Override
    protected float getAnimationSpeed() {
        return 1 * getSpeedMultiplier();
    }

    @Override
    protected boolean consumeInput(Transaction transaction) {
        var firstItem = this.getInputView().getFirst();
        if (firstItem.isEmpty() || firstItem.getItem() instanceof BucketItem) return false;

        var data = firstItem.typeHolder().getData(NeoForgeDataMaps.FURNACE_FUELS);
        if (data == null || data.burnTime() <= 0) return false;

        var fuelTime = data.burnTime();

        var removed = inventory.getInputContainer().extract(0, ItemResource.of(firstItem), 1, transaction);
        if (removed != 1) return false;

        progress.set(fuelTime);
        currentMaxBurnTime = fuelTime;

        return true;
    }

    @Override
    protected Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level level) {

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
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 1, 1, 0);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 80, 21));
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.BASIC_GENERATOR_SCREEN.get();
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
        return OritechConfig.generators.basicGeneratorData.maxEnergyExtraction.get();
    }

    @Override
    public long getDefaultCapacity() {
        return OritechConfig.generators.basicGeneratorData.energyCapacity.get();
    }

    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
}
