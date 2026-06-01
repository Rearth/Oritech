package rearth.oritech.block.entity.interaction;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.OritechScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.*;

import java.util.*;

public class TreefellerBlockEntity extends NetworkedBlockEntity implements
        BlockEntityTicker<NetworkedBlockEntity>, GeoBlockEntity, EnergyProvider, ColorableMachine, ItemProvider, MenuProvider, ScreenProvider {

    private static final int LOG_COST = 100;
    private static final int LEAF_COST = 10;

    private final Deque<BlockPos> pendingBlocks = new ArrayDeque<>();
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    private long lastWorkedAt = 0;

    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(50000, 4000, 0, 0, this::setChanged, false);

    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(6, this::setChanged) {

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    };

    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorVariant currentColor = getDefaultColor();

    public TreefellerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.TREEFELLER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (energyStorage.energy >= LOG_COST) {
            if (pendingBlocks.isEmpty() && serverLevel.getGameTime() % 20 == 0) {
                findTarget();
            }

            for (int i = 0; i < 6 && !pendingBlocks.isEmpty(); i++) {
                var candidate = pendingBlocks.peekLast();
                var candidateState = serverLevel.getBlockState(candidate);
                var isLog = candidateState.is(TagContent.CUTTER_LOGS_MINEABLE);

                var energyCost = isLog ? LOG_COST : LEAF_COST;
                if (energyCost > energyStorage.energy) break;

                var actionResult = breakTreeBlock(candidateState, candidate);
                if (actionResult == InteractionResult.FAIL) break;
                pendingBlocks.pollLast();
                if (actionResult == InteractionResult.PASS) continue;
                lastWorkedAt = serverLevel.getGameTime();

                energyStorage.energy -= energyCost;
                setChanged();

                if (isLog) break; // only harvest 1 log, but multiple leaves (up to 6)
            }
        }

        if (serverLevel.getGameTime() % 10 == 0) {
            var idleTicks = serverLevel.getGameTime() - lastWorkedAt;
            var isWorking = idleTicks < 20;
            var animName = isWorking ? "work" : "idle";
            playWorkAnimation(animName);
        }
    }

    private InteractionResult breakTreeBlock(BlockState candidateState, BlockPos candidate) {
        if (!candidateState.is(TagContent.CUTTER_LOGS_MINEABLE) && !candidateState.is(TagContent.CUTTER_LEAVES_MINEABLE))
            return InteractionResult.PASS;

        var dropped = Block.getDrops(candidateState, (ServerLevel) level, candidate, null);
        if (dropped.stream().anyMatch((itemStack) -> !(itemStack.isEmpty() || canInsert(itemStack))))
            return InteractionResult.FAIL;

        level.addDestroyBlockEffect(candidate, candidateState);
        if (level.getGameTime() % 2 == 0)
            level.playSound(null, candidate, candidateState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.5f, 1f);
        level.setBlockAndUpdate(candidate, Blocks.AIR.defaultBlockState());

        dropped.forEach(stack -> {
            try (var transaction = Transaction.openRoot()) {
                var inserted = inventory.insert(ItemResource.of(stack), stack.getCount(), transaction);
                if (inserted == stack.getCount()) {
                    transaction.commit();
                }
            }
        });
        return InteractionResult.SUCCESS;
    }

    private boolean canInsert(ItemStack stack) {
        return inventory.getStacks().stream().anyMatch((itemStack) ->
                itemStack.isEmpty() || (ItemStack.isSameItemSameComponents(itemStack, stack) && itemStack.getCount() + stack.getCount() <= itemStack.getMaxStackSize())
        );
    }

    public void findTarget() {

        var state = getBlockState();
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        var offset = Geometry.rotatePosition(new Vec3i(1, 0, 0), facing);
        var frontBlock = worldPosition.offset(offset);

        var res = getTreeBlocks(frontBlock, level);
        pendingBlocks.addAll(res);

    }

    public static Deque<BlockPos> getTreeBlocks(BlockPos startPos, Level level) {

        var startState = level.getBlockState(startPos);
        if (!startState.is(TagContent.CUTTER_LOGS_MINEABLE)) return new ArrayDeque<>();

        var checkedPositions = new HashSet<BlockPos>();
        var foundPositions = new ArrayDeque<BlockPos>();
        var foundLogs = new HashSet<BlockPos>();
        var pendingPositions = new ArrayDeque<BlockPos>();

        checkedPositions.add(startPos);
        foundPositions.add(startPos);
        pendingPositions.addAll(getNeighbors(startPos));
        foundLogs.add(startPos);

        while (!pendingPositions.isEmpty() && checkedPositions.size() < 8000) {
            // do logs first, if none available then leaves
            var candidate = pendingPositions.pollFirst();
            if (candidate.getY() < startPos.getY()) continue;

            if (checkedPositions.contains(candidate)) continue;

            var candidateState = level.getBlockState(candidate);
            checkedPositions.add(candidate);

            var isLog = candidateState.is(TagContent.CUTTER_LOGS_MINEABLE);
            var isValidLeaf = candidateState.is(TagContent.CUTTER_LEAVES_MINEABLE) && !candidateState.getOptionalValue(BlockStateProperties.PERSISTENT).orElse(false);

            if (!isLog && !isValidLeaf) continue;

            var isValid = false;
            if (isLog) {
                isValid = isInLogRange(candidate, foundLogs, 3);
            } else {
                // Give a default of 4 for "leaf" blocks without a DISTANCE_1_7 property (like shroomlights or mushrooms)
                var range = candidateState.getOptionalValue(BlockStateProperties.DISTANCE).orElse(4);
                isValid = isInLogRange(candidate, foundLogs, range + 2);
            }

            if (!isValid) continue;

            if (isLog) {
                foundLogs.add(candidate);
            }

            foundPositions.add(candidate);
            pendingPositions.addAll(getNeighbors(candidate));

        }

        // when no leaves are found, return nothing to prevent accidentally destroying buildings
        if (foundLogs.size() == foundPositions.size()) return new ArrayDeque<>();

        return foundPositions;
    }

    private static boolean isInLogRange(BlockPos pos, Set<BlockPos> logs, int maxDist) {
        return logs.stream().anyMatch(elem -> elem.distManhattan(pos) <= maxDist);
    }

    private static List<BlockPos> getNeighbors(BlockPos input) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (BlockPos pos : BlockPos.withinManhattan(input, 1, 1, 1)) {
            // Without toImmutable, all of the elements in the collected list end up being the same BlockPos
            neighbors.add(pos.immutable());
        }
        return neighbors;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
        output.putLong("energy_stored", energyStorage.energy);
        serializeColor(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
        energyStorage.energy = input.getLongOr("energy_stored", 0);
        deserializeColor(input);
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("machine", 5, state -> PlayState.CONTINUE)
                .triggerableAnim("work", MachineBlockEntity.WORKING)
                .triggerableAnim("idle", MachineBlockEntity.IDLE)
                .setSoundKeyframeHandler(new MachineSoundHandler<>()));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
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
    public List<GuiSlot> getGuiSlots() {
        var list = new ArrayList<GuiSlot>();
        for (int i = 0; i < inventory.size(); i++) {
            list.add(new GuiSlot(i, 40 + i * 19, 25, true));
        }
        return list;
    }

    @Override
    public float getDisplayedEnergyUsage() {
        return LOG_COST;
    }

    @Override
    public float getProgress() {
        return 0;
    }

    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }

    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }

    @Override
    public boolean showProgress() {
        return false;
    }

    @Override
    public SimpleInventoryStorage getDisplayedInventory() {
        return inventory;
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.TREEFELLER_SCREEN.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new OritechScreenHandler(syncId, playerInventory, this);
    }

    public void playWorkAnimation(String animName) {
        triggerAnim("machine", animName);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        this.sendUpdate(SyncType.GUI_OPEN);
    }
}
