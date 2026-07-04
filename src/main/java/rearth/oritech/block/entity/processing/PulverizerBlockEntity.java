package rearth.oritech.block.entity.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.block.base.entity.UpgradableMachineBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.ArrayList;
import java.util.List;

public class PulverizerBlockEntity extends UpgradableMachineBlockEntity {

    public PulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PULVERIZER_ENTITY.get(), pos, state, OritechConfig.processingMachines.pulverizerData.energyPerTick.get());
    }

    @Override
    protected boolean createCraftingOutputs(Transaction transaction) {
        PulverizerBlockEntity.CombineSmallDusts(transaction, level, inventory.getOutputContainer());
        return super.createCraftingOutputs(transaction);
    }

    public static void CombineSmallDusts(Transaction transaction, Level level, ResourceHandler<ItemResource> outputInventory) {

        try (var inner = Transaction.open(transaction)) {

            // try taking 9 items from slot 1
            var dustResource = outputInventory.getResource(1);
            if (dustResource.isEmpty()) return;
            var removed = outputInventory.extract(1, dustResource, 9, inner);
            if (removed != 9) return;

            // see if they can be combined
            var recipeInputStacks = new ArrayList<ItemStack>(9);
            for (int i = 0; i < 9; i++) {
                recipeInputStacks.add(i, dustResource.toStack(1));
            }
            var craftingInv = CraftingInput.of(3, 3, recipeInputStacks);
            var recipeCandidate = ((ServerLevel) level).recipeAccess().getRecipeFor(RecipeType.CRAFTING, craftingInv, level);
            if (recipeCandidate.isEmpty()) return;

            var itemResult = recipeCandidate.get().value().assemble(craftingInv);

            // try inserting result into slot 0, commit if successful
            var inserted = outputInventory.insert(0, ItemResource.of(itemResult), itemResult.count(), inner);

            if (inserted == 1) inner.commit();

        }
    }

    @Override
    public long getDefaultCapacity() {
        return OritechConfig.processingMachines.pulverizerData.energyCapacity.get();
    }

    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.processingMachines.pulverizerData.maxEnergyInsertion.get();
    }

    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.PULVERIZER.get();
    }

    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 1, 1, 2);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 56, 38),
                new GuiSlot(1, 117, 38, true),
                new GuiSlot(2, 135, 38, true));
    }

    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.PULVERIZER_SCREEN.get();
    }

    @Override
    public int getInventorySize() {
        return 3;
    }

    @Override
    protected void onProgressed() {
        super.onProgressed();

        if (level.getRandom().nextFloat() > 0.7 && !inventory.getItem(0).isEmpty()) {
            var effect = new ItemParticleOption(ParticleTypes.ITEM, inventory.getItem(0).getItem());
            var spawnAt = worldPosition.getCenter().add(0, 0.3, 0);
            var offsetX = (level.getRandom().nextFloat() - 0.5) * 0.1;
            var offsetY = (level.getRandom().nextFloat()) * 0.1;
            var offsetZ = (level.getRandom().nextFloat() - 0.5) * 0.1;
            ((ServerLevel) level).sendParticles(effect, spawnAt.x(), spawnAt.y(), spawnAt.z(), 3, offsetX, offsetY, offsetZ, 0.08);
        }
    }

    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
                new Vec3i(1, 0, 0)
        );
    }

    @Override
    public float getCoreQuality() {
        return 2;
    }
}
