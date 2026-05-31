package rearth.oritech.block.entity.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.api.transfer.fluid.SimpleFluidStorage;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeInput;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;

import java.util.List;

public class CoolerBlockEntity extends MultiblockMachineEntity implements FluidProvider {

    private boolean inColdArea;
    private boolean initialized = false;

    @SyncField(SyncType.GUI_TICK)
    public final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(4 * FluidType.BUCKET_VOLUME, this::setChanged);

    public CoolerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.COOLER_ENTITY.get(), pos, state, OritechConfig.processingMachines.coolerData.energyPerTick.get());
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        super.serverTick(level, pos, state, blockEntity);

        if (!initialized) {
            initialized = true;
            var biome = level.getBiome(pos);
            inColdArea = biome.is(TagContent.CONVENTIONAL_COLD);
        }

    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fluidStorage.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluidStorage.deserialize(input);
    }

    @Override
    protected void onProgressed() {
        super.onProgressed();

        var progress = getProgress();
        if (progress < 0.35 || progress > 0.65) return;

        if (level == null || level.getRandom().nextFloat() > 0.4f) return;
        // emit particles
        var emitPosition = Vec3.atCenterOf(worldPosition);

        if (level instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.SNOWFLAKE, emitPosition.x, emitPosition.y, emitPosition.z, 2, 1.2, 1.2, 1.2, 0);

    }

    @Override
    protected OritechRecipeInput getRecipeInput() {
        return new OritechRecipeInput(List.of(), fluidStorage.getContent());
    }

    @Override
    protected boolean finishCrafting(Transaction transaction) {
        var craftCount = 1 + getBaseAddonData().extraChambers();
        var crafted = false;

        for (int i = 0; i < craftCount; i++) {
            try (var nested = Transaction.open(transaction)) {
                if (!processCraftInstance(currentRecipe, nested)) {
                    return crafted;
                }

                nested.commit();
                crafted = true;
            }
        }

        return crafted;
    }

    // returns true if crafting has been successful
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean processCraftInstance(OritechRecipe activeRecipe, Transaction transaction) {
        var results = getCraftingResults(activeRecipe);
        if (results.isEmpty()) return false;
        var result = results.getFirst();

        var inserted = inventory.getOutputContainer().insert(ItemResource.of(result), result.count(), transaction);
        if (inserted != result.count()) return false;

        var input = activeRecipe.fluidInput();
        if (input.amount() <= 0) return true;

        var fluidResource = fluidStorage.getResource(0);
        if (fluidResource.isEmpty()) return false;

        var extracted = fluidStorage.extract(0, fluidResource, input.amount(), transaction);
        return extracted == input.amount();
    }

    @Override
    public float getSpeedMultiplier() {
        var biomeBonus = inColdArea ? 0.5f : 1f;
        return super.getSpeedMultiplier() * biomeBonus;
    }

    @Override
    public float getEfficiencyMultiplier() {
        var biomeBonus = inColdArea ? 0.5f : 1f;
        return super.getEfficiencyMultiplier() * biomeBonus;
    }

    @Override
    public long getDefaultCapacity() {
        return OritechConfig.processingMachines.coolerData.energyCapacity.get();
    }

    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.processingMachines.coolerData.maxEnergyInsertion.get();
    }

    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.COOLER.get();
    }

    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 0, 0, 1);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 117, 36, true));
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.COOLER_SCREEN.get();
    }

    @Override
    public int getInventorySize() {
        return 1;
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
                new Vec3i(0, 0, -1)
        );
    }

    @Override
    public List<Vec3i> getAddonSlots() {

        return List.of(
                new Vec3i(0, 0, -2)
        );
    }

    @Override
    public net.neoforged.neoforge.transfer.ResourceHandler<FluidResource> getFluidLookup(@Nullable Direction direction) {
        return fluidStorage;
    }

}
