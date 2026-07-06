package rearth.oritech.block.entity.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.block.entity.addons.CombiAddonEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.Geometry;

import java.util.ArrayList;
import java.util.List;

public class FragmentForgeBlockEntity extends MultiblockMachineEntity {

    @SyncField(SyncType.GUI_OPEN)
    private boolean hasByproductAddon;

    public FragmentForgeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FRAGMENT_FORGE_ENTITY.get(), pos, state, OritechConfig.processingMachines.fragmentForgeData.energyPerTick.get());
    }

    @Override
    public long getDefaultCapacity() {
        return OritechConfig.processingMachines.fragmentForgeData.energyCapacity.get();
    }

    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.processingMachines.fragmentForgeData.maxEnergyInsertion.get();
    }

    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.GRINDER.get();
    }

    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_YIELD_ADDON.get()) || addonBlock.addonEntity() instanceof CombiAddonEntity combi && combi.getYieldCount() > 0) {
            hasByproductAddon = true;
        }
    }

    @Override
    protected void onProgressed() {
        super.onProgressed();

        if (level.getRandom().nextFloat() > 0.8) return;
        // emit particles
        var facing = getFacing();
        var offsetLocal = Geometry.rotatePosition(new Vec3(0.4, 0.6, 0.5), facing);
        var emitPosition = Vec3.atCenterOf(worldPosition).add(offsetLocal);

        if (level instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.DUST_PLUME, emitPosition.x, emitPosition.y, emitPosition.z, 1, 0.8, 0.8, 0.8, 0);
    }

    @Override
    public void resetAddons() {
        super.resetAddons();
        hasByproductAddon = false;
    }

    @Override
    public void gatherAddonStats(List<AddonBlock> addons) {
        hasByproductAddon = false;
        super.gatherAddonStats(addons);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("byproductAddon", hasByproductAddon);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hasByproductAddon = input.getBooleanOr("byproductAddon", false);
    }

    @Override
    protected boolean createCraftingOutputs(Transaction transaction) {
        PulverizerBlockEntity.CombineSmallDusts(transaction, level, inventory.getOutputContainer());
        return super.createCraftingOutputs(transaction);
    }

    @Override
    public List<ItemStackTemplate> getCraftingResults(OritechRecipe activeRecipe) {
        if (hasByproductAddon) {
            var result = new ArrayList<ItemStackTemplate>(activeRecipe.itemResults().size());
            var source = activeRecipe.itemResults();
            for (int i = 0; i < source.size(); i++) {
                var item = source.get(i);
                if (i == 0) {
                    result.add(item);
                } else {
                    var newCount = item.count() * 2;
                    var newItem = new ItemStackTemplate(item.item(), newCount);
                    result.add(newItem);
                }
            }
            return result;
        } else {
            return super.getCraftingResults(activeRecipe);
        }
    }

    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 1, 1, 3);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 56, 38),
                new GuiSlot(1, 117, 20, true),
                new GuiSlot(2, 117, 38, true),
                new GuiSlot(3, 117, 56, true));
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.GRINDER_SCREEN.get();
    }

    @Override
    public int getInventorySize() {
        return 4;
    }

    @Override
    public List<Tuple<Component, Component>> getExtraExtensionLabels() {
        if (!hasByproductAddon) return super.getExtraExtensionLabels();
        return List.of(new Tuple<>(Component.literal("\uD83C\uDF40: Enabled"), Component.translatable("tooltip.oritech.machine.byproduct_bonus.tooltip")));
    }

    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }

    // x = back
    // y = up
    // z = left

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
                new Vec3i(0, 1, 0),    // middle
                new Vec3i(0, 0, 1),    // left
                new Vec3i(0, 1, 1),
                new Vec3i(1, 0, 1),    // back left
                new Vec3i(1, 1, 1),
                new Vec3i(1, 0, 0),    // back middle
                new Vec3i(1, 1, 0)
        );
    }

    // x = back, // z = left
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
                new Vec3i(0, 0, 2),
                new Vec3i(1, 0, 2),
                new Vec3i(2, 0, 0),
                new Vec3i(1, 0, -1),
                new Vec3i(-1, 0, 1)
        );
    }
}
