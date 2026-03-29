package rearth.oritech.block.base.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.block.FrameInteractionBlock;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.Geometry;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;


public abstract class FrameInteractionBlockEntity extends NetworkedBlockEntity {
    
    private static final int MAX_SEARCH_LENGTH = OritechConfig.processingMachines.machineFrameMaxLength.get();
    private static final HashMap<Vec3i, HashMap<Vec3i, Vec3i>> occupiedAreas = new HashMap<>();
    
    @SyncField({SyncType.INITIAL, SyncType.SPARSE_TICK})
    private BlockPos areaMin;       // both min and max are inclusive
    @SyncField({SyncType.INITIAL, SyncType.SPARSE_TICK})
    private BlockPos areaMax;
    @SyncField({SyncType.INITIAL, SyncType.SPARSE_TICK})
    public boolean disabledViaRedstone;
    
    @SyncField
    private BlockPos currentTarget;
    @SyncField
    private BlockPos lastTarget;
    @SyncField
    private boolean moving;
    @SyncField
    private float currentProgress;
    
    private Vec3i currentDirection = new Vec3i(1, 0, 0);    // not synced
    public long lastWorkedAt;   // not synced
    
    // for smooth client rendering only
    public Vec3 lastRenderedPosition = new Vec3(0, 0, 0);
    
    public FrameInteractionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public boolean tryFindFrame() {
        
        Oritech.LOGGER.debug("searching machine frame");
        
        var facing = getFacing();
        var backRelative = new Vec3i(getFrameOffset(), 0, 0);
        // Frame machines always anchor to the frame block directly behind the controller.
        var searchStart = (BlockPos) Geometry.offsetToWorldPosition(facing, backRelative, worldPosition);

        var bounds = findFrameBounds(searchStart);
        if (bounds == null) {
            return false;
        }

        // offset values by 1 to define the working area instead of bounds
        var startX = bounds.minX + 1;
        var startZ = bounds.minZ + 1;
        areaMin = new BlockPos(startX, getBlockPos().getY(), startZ);

        var endX = bounds.maxX - 1;
        var endZ = bounds.maxZ - 1;
        areaMax = new BlockPos(endX, getBlockPos().getY(), endZ);
        
        if (currentTarget == null || !isInBounds(currentTarget)) {
            currentTarget = areaMin;
            lastTarget = areaMin;
        }
        this.setChanged();
        sendUpdate(SyncType.INITIAL);
        
        return true;
    }
    
    protected Direction getFacing() {
        return Objects.requireNonNull(level).getBlockState(getBlockPos()).getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    private FrameBounds findFrameBounds(BlockPos searchStart) {
        if (!testForFrame(searchStart)) {
            highlightBlock(searchStart);
            return null;
        }

        assert level != null;

        var frameBlocks = new HashSet<BlockPos>();
        var openSet = new ArrayDeque<BlockPos>();
        frameBlocks.add(searchStart);
        openSet.add(searchStart);

        var minX = searchStart.getX();
        var maxX = searchStart.getX();
        var minZ = searchStart.getZ();
        var maxZ = searchStart.getZ();
        var maxFrameBlocks = MAX_SEARCH_LENGTH * 4;

        // Collect the connected frame loop first so validation works no matter which edge or corner we started from.
        while (!openSet.isEmpty()) {
            var currentPos = openSet.removeFirst();

            minX = Math.min(minX, currentPos.getX());
            maxX = Math.max(maxX, currentPos.getX());
            minZ = Math.min(minZ, currentPos.getZ());
            maxZ = Math.max(maxZ, currentPos.getZ());

            for (var direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                var nextPos = currentPos.relative(direction);
                if (nextPos.getY() != searchStart.getY() || frameBlocks.contains(nextPos) || !testForFrame(nextPos)) {
                    continue;
                }

                frameBlocks.add(nextPos);
                openSet.add(nextPos);
                if (frameBlocks.size() > maxFrameBlocks) {
                    highlightBlock(nextPos);
                    return null;
                }
            }
        }

        var width = maxX - minX + 1;
        var depth = maxZ - minZ + 1;
        if (width < 3 || depth < 3 || width > MAX_SEARCH_LENGTH || depth > MAX_SEARCH_LENGTH) {
            highlightBlock(searchStart);
            return null;
        }

        // A valid frame is a solid rectangle border with a completely empty interior.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var checkPos = new BlockPos(x, searchStart.getY(), z);
                var isBoundary = x == minX || x == maxX || z == minZ || z == maxZ;
                var isFrame = testForFrame(checkPos);

                if (isBoundary && !isFrame) {
                    highlightBlock(checkPos);
                    return null;
                }

                if (!isBoundary && !level.getBlockState(checkPos).isAir()) {
                    highlightBlock(checkPos);
                    return null;
                }
            }
        }

        Oritech.LOGGER.debug("found valid machine frame from {} to {}", new BlockPos(minX, searchStart.getY(), minZ), new BlockPos(maxX, searchStart.getY(), maxZ));
        return new FrameBounds(minX, maxX, minZ, maxZ);
    }
    
    @SuppressWarnings("DataFlowIssue")
    private boolean testForFrame(BlockPos pos) {
        var found = level.getBlockState(pos).getBlock();
        return found.equals(BlockContent.MACHINE_FRAME_BLOCK);
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (!isActive(state) || !state.getValue(FrameInteractionBlock.HAS_FRAME) || getAreaMin() == null)
            return;
        
        if (!canProgress()) return;
        
        while (currentProgress > 0.01) {
            if (!moving && currentProgress >= getWorkTime()) {
                setChanged();
                if (startBlockMove()) { // only complete work if we can move to the next position
                    currentProgress -= getWorkTime();
                    finishBlockWork(lastTarget);
                    updateToolPosInFrame();
                    moving = true;
                } else {
                    break;  // next pos is blocked. Keep current progress, but dont perform any more actions.
                }
            } else if (moving && currentProgress >= getMoveTime()) {
                setChanged();
                if (hasWorkAvailable(currentTarget)) {
                    moving = false;
                    currentProgress -= getMoveTime();
                } else if (startBlockMove()) {
                    updateToolPosInFrame();
                    currentProgress -= getMoveTime();
                } else {
                    break;
                }
                
            } else {
                break;
            }
        }
        
        doProgress(moving);
        currentProgress++;
        lastWorkedAt = world.getGameTime();
    }
    
    @Override
    public int getTickUpdateInterval() {
        return 1;
    }
    
    private boolean isBlockAvailable(BlockPos target) {
        if (!occupiedAreas.containsKey(areaMin)) {
            occupiedAreas.put(areaMin, new HashMap<>(1));
            return true;
        }
        
        var frameEntries = occupiedAreas.get(areaMin);
        return !frameEntries.containsValue(target);
    }
    
    private void updateToolPosInFrame() {
        var frameEntries = occupiedAreas.get(areaMin);
        frameEntries.put(worldPosition, currentTarget);
    }
    
    public void cleanup() {
        var frameEntries = occupiedAreas.get(areaMin);
        if (frameEntries != null)
            frameEntries.remove(worldPosition);
    }
    
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected abstract boolean hasWorkAvailable(BlockPos toolPosition);
    
    protected abstract void doProgress(boolean moving);
    
    protected abstract boolean canProgress();
    
    public abstract void finishBlockWork(BlockPos processed);
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        if (getBlockState().getValue(FrameInteractionBlock.HAS_FRAME) && areaMin != null) {
            
            nbt.putLong("areaMin", areaMin.asLong());
            nbt.putLong("areaMax", areaMax.asLong());
            
            if (currentTarget != null)
                nbt.putLong("currentTarget", currentTarget.asLong());
            
            if (currentDirection != null)
                nbt.putLong("currentDirection", new BlockPos(currentDirection).asLong());
            
            nbt.putInt("progress", (int) currentProgress);
            nbt.putBoolean("moving", moving);
        }
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        if (getBlockState().getValue(FrameInteractionBlock.HAS_FRAME)) {
            areaMin = BlockPos.of(nbt.getLong("areaMin"));
            areaMax = BlockPos.of(nbt.getLong("areaMax"));
            currentTarget = BlockPos.of(nbt.getLong("currentTarget"));
            currentDirection = BlockPos.of(nbt.getLong("currentDirection"));
            lastTarget = currentTarget;
            currentProgress = nbt.getInt("progress");
            moving = nbt.getBoolean("moving");
        }
    }
    
    private boolean startBlockMove() {
        
        var nextPos = currentTarget.offset(currentDirection);
        var nextDir = currentDirection;
        if (!isInBounds(nextPos)) {
            nextPos = currentTarget.offset(0, 0, 1);
            nextDir = currentDirection.multiply(-1);
            if (!isInBounds(nextPos)) {
                var data = resetWorkPosition();
                nextPos = data.getA();
                nextDir = data.getB();
            }
        }
        
        // tries to not put 2 tool heads in the same spot, but also allow overtaking if previous machine is too slow
        if (!isBlockAvailable(nextPos) && currentProgress <= getWorkTime() * getSpeedMultiplier() * 2 + 4) return false;
        
        lastTarget = currentTarget;
        currentTarget = nextPos;
        currentDirection = nextDir;
        
        return true;
    }
    
    // return start position + direction
    private Tuple<BlockPos, BlockPos> resetWorkPosition() {
        return new Tuple<>(areaMin, new BlockPos(1, 0, 0));
    }
    
    private boolean isInBounds(BlockPos pos) {
        return pos.getX() >= areaMin.getX() && pos.getX() <= areaMax.getX()
                 && pos.getZ() >= areaMin.getZ() && pos.getZ() <= areaMax.getZ();
    }
    
    private void highlightBlock(BlockPos block) {
        ParticleContent.HighlightBlock(level, Vec3.atLowerCornerOf(block));
    }
    
    public abstract BlockState getMachineHead();
    
    public int getFrameOffset() {
        return 1;
    }
    
    public float getSpeedMultiplier() {
        return 1f;
    }
    
    public BlockPos getAreaMin() {
        return areaMin;
    }
    
    public void setAreaMin(BlockPos areaMin) {
        this.areaMin = areaMin;
    }
    
    public BlockPos getAreaMax() {
        return areaMax;
    }
    
    public void setAreaMax(BlockPos areaMax) {
        this.areaMax = areaMax;
    }
    
    public BlockPos getCurrentTarget() {
        return currentTarget;
    }
    
    public void setCurrentTarget(BlockPos currentTarget) {
        this.currentTarget = currentTarget;
    }
    
    public BlockPos getLastTarget() {
        return lastTarget;
    }
    
    public void setLastTarget(BlockPos lastTarget) {
        this.lastTarget = lastTarget;
    }
    
    public int getCurrentProgress() {
        return (int) currentProgress;
    }
    
    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }
    
    public boolean isActive(BlockState state) {
        return true;
    }
    
    public boolean isMoving() {
        return moving;
    }
    
    public void setMoving(boolean moving) {
        this.moving = moving;
    }
    
    public Vec3i getCurrentDirection() {
        return currentDirection;
    }
    
    public void setCurrentDirection(Vec3i currentDirection) {
        this.currentDirection = currentDirection;
    }
    
    public abstract float getMoveTime();
    
    public abstract float getWorkTime();
    
    public ItemStack getToolheadAdditionalRender() {
        return null;
    }
    
    @Override
    public void sendUpdate(SyncType type) {
        if (currentTarget == null || lastTarget == null) return;
        super.sendUpdate(type);
    }

    private record FrameBounds(int minX, int maxX, int minZ, int maxZ) {
    }
}
