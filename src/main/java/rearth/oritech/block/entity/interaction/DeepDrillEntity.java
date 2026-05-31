package rearth.oritech.block.entity.interaction;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.recipes.OritechRecipeInput;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.*;

import java.util.ArrayList;
import java.util.List;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.block.base.entity.MachineBlockEntity.*;


public class DeepDrillEntity extends NetworkedBlockEntity implements EnergyProvider, GeoBlockEntity, ItemProvider, MultiblockMachineController, ColorableMachine {

    // work data
    private boolean initialized;
    public final List<Block> targetedOre = new ArrayList<>();
    public ProgressStorage progress = new ProgressStorage();
    @SyncField
    private long lastWorkTime;

    // config

    // storage
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(OritechConfig.deepDrillConfig.energyCapacity.get(), getMaxRfInput(), 0, 0, this::setChanged, false);

    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(1, this::setChanged) {
        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    };

    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;

    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<DeepDrillEntity> animationController = getAnimationController();

    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorVariant currentColor = getDefaultColor();

    public DeepDrillEntity(BlockPos pos, BlockState state) {
        this(BlockEntitiesContent.DEEP_DRILL_ENTITY.get(), pos, state);
    }

    // this second option is here to allow addons to create custom deep drill entities with special logic
    public DeepDrillEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean init(boolean manual) {

        initialized = true;
        targetedOre.clear();
        loadOreBlocks(manual);

        return !targetedOre.isEmpty();
    }


    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {

        if (isActive(state) && !initialized && (serverLevel.getGameTime() + pos.asLong()) % 60 == 0) {
            init(false);
        }

        if (!initialized || targetedOre.isEmpty()) return;

        if (inventory.getStacks().getFirst().getCount() >= inventory.getStacks().getFirst().getMaxStackSize())
            return;    // inv full

        var energyPerStep = getRfPerStep();
        if (energyStorage.energy < energyPerStep) return;

        try (var transaction = Transaction.openRoot()) {

            var usedEnergy = 0L;
            while (energyStorage.energy >= energyPerStep) {
                progress.increment(transaction);
                usedEnergy += energyStorage.internalExtract(energyPerStep, transaction);
            }

            while (progress.get() >= OritechConfig.deepDrillConfig.stepsPerOre.get()) {
                craftResult(transaction, serverLevel);
            }

            if (usedEnergy > 0) {
                transaction.commit();
                lastWorkTime = serverLevel.getGameTime();

                var particlePos = getCenter(0);
                serverLevel.sendParticles(ParticleTypes.LAVA, particlePos.getX() + 0.5, particlePos.getY() + 0.5, particlePos.getZ() + 0.5, 1, 0.6, 0.6, 0.6, 0);

            }
        }

    }

    private BlockPos getCenter(int y) {
        var state = getBlockState();
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return worldPosition.offset(Geometry.rotatePosition(new Vec3i(1, y, 0), facing));
    }

    public void loadOreBlocks(boolean manual) {
        var center = getCenter(-1);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                // Only target the top-most uncovered resource node
                for (int y = 0; y >= -2; y--) {
                    var target = center.offset(x, y, z);
                    var targetState = level.getBlockState(target);
                    if (targetState.is(TagContent.RESOURCE_NODES)) {
                        if (manual) ParticleContent.DebugBlock(level, Vec3.atLowerCornerOf(target));
                        targetedOre.add(targetState.getBlock());
                        break;
                    } else if (!targetState.isAir()) break;
                }
            }
        }
    }

    private void craftResult(Transaction transaction, ServerLevel serverLevel) {
        var usedOre = targetedOre.get(level.getRandom().nextInt(0, targetedOre.size()));
        var nodeOreBlockItem = usedOre.asItem();
        var sampleInv = new OritechRecipeInput(List.of(new ItemStack(nodeOreBlockItem, 1)), FluidStack.EMPTY);

        var recipeCandidate = serverLevel.recipeAccess().getRecipeFor(RecipeContent.DEEP_DRILL.get(), sampleInv, level);
        if (recipeCandidate.isEmpty())
            return;

        var output = recipeCandidate.get().value().itemResults().getFirst().create();
        var inserted = inventory.insert(ItemResource.of(output), output.getCount(), transaction);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        inventory.serialize(output);
        progress.serialize(output);

        serializeMultiblock(output);
        serializeColor(output);

        output.putLong("energy_stored", energyStorage.energy);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        inventory.deserialize(input);
        progress.deserialize(input);

        deserializeMultiblock(input);
        deserializeColor(input);

        energyStorage.energy = input.getLongOr("energy_stored", 0);
    }

    @Override
    public EnergyHandler getEnergyLookup(Direction direction) {
        return energyStorage;
    }

    @Override
    public ResourceHandler<ItemResource> getItemLookup(Direction direction) {
        return inventory;
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
                new Vec3i(-2, 1, -1),
                new Vec3i(0, 2, 1),
                new Vec3i(0, 2, 0),
                new Vec3i(0, 2, -1),
                new Vec3i(-1, 2, 1),
                new Vec3i(-1, 2, 0),
                new Vec3i(-1, 2, -1),
                new Vec3i(-2, 2, 1),
                new Vec3i(-2, 2, 0),
                new Vec3i(-2, 2, -1)
        );
    }

    @Override
    public Direction getFacingForMultiblock() {
        var state = getBlockState();
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public BlockPos getPosForMultiblock() {
        return worldPosition;
    }

    @Override
    public Level getWorldForMultiblock() {
        return level;
    }

    @Override
    public ArrayList<BlockPos> getConnectedCores() {
        return coreBlocksConnected;
    }

    @Override
    public void setCoreQuality(float quality) {
        this.coreQuality = quality;
    }

    @Override
    public float getCoreQuality() {
        return coreQuality;
    }

    @Override
    public StacksResourceHandler<ItemStack, ItemResource> getInventoryForMultiblock() {
        return inventory;
    }

    @Override
    public DynamicEnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return null;
    }

    @Override
    public void triggerSetupAnimation() {
        triggerAnim("base_controller", "setup");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(animationController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    public int getMaxRfInput() {
        return 0;
    }

    public int getRfPerStep() {
        return OritechConfig.deepDrillConfig.energyPerStep.get();
    }

    @Override
    public ColorVariant getCurrentColor() {
        return currentColor;
    }

    @Override
    public void assignColor(ColorVariant color) {
        this.currentColor = color;

        if (this.level != null && !this.level.isClientSide()) {
            this.setChanged(false);
            this.sendUpdate(SyncType.SPARSE_TICK);
        }
    }

    private AnimationController<DeepDrillEntity> getAnimationController() {
        AnimationController<DeepDrillEntity> controller = new AnimationController<>("machine", state -> {

            if (state.isCurrentAnimation(SETUP)) {
                if (state.controller().hasAnimationFinished()) {
                    state.setAndContinue(IDLE);
                } else {
                    return state.setAndContinue(SETUP);
                }
            }

            if (isActive(getBlockState())) {

                var idleTime = level.getGameTime() - lastWorkTime;

                if (idleTime < 60) {
                    return state.setAndContinue(WORKING);
                } else {
                    return state.setAndContinue(IDLE);
                }
            } else {
                return state.setAndContinue(PACKAGED);
            }
        });
        controller.setSoundKeyframeHandler(new MachineSoundHandler<>());
        controller.triggerableAnim("setup", SETUP);
        return controller;
    }

    private boolean isActive(BlockState state) {
        return state.getValue(ASSEMBLED);
    }
}
