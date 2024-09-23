package rearth.oritech.block.base.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.block.FrameInteractionBlock;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.Geometry;

import java.util.HashMap;
import java.util.Objects;

import static rearth.oritech.util.Geometry.*;

public abstract class FrameInteractionBlockEntity extends BlockEntity implements BlockEntityTicker<FrameInteractionBlockEntity> {
    
    private static final int MAX_SEARCH_LENGTH = Oritech.CONFIG.processingMachines.machineFrameMaxLength();
    private static final HashMap<Vec3i, HashMap<Vec3i, Vec3i>> occupiedAreas = new HashMap<>();
    private BlockPos areaMin;       // both min and max are inclusive
    private BlockPos areaMax;
    private BlockPos currentTarget; // rendering is based just on this (and move time)
    private BlockPos lastTarget;
    private int currentProgress;    // not synced
    private boolean moving;    // not synced
    private Vec3i currentDirection = new Vec3i(1, 0, 0);    // not synced
    public long lastWorkedAt;   // not synced
    
    // client only
    private long moveStartedAt;
    // for smooth client rendering only
    public Vec3d lastRenderedPosition = new Vec3d(0, 0, 0);
    
    public FrameInteractionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public boolean tryFindFrame() {
        
        Oritech.LOGGER.debug("searching machine frame");
        
        // select block on back (or based on offset of machine)
        // from there on move right, till no more frame blocks are found
        // then move back, searching again till end
        // then move left, searching again till end
        // then move forward, searching again till end
        // then move right again, searching till start position
        
        var facing = getFacing();
        var backRelative = new Vec3i(getFrameOffset(), 0, 0);
        var searchStart = (BlockPos) Geometry.offsetToWorldPosition(facing, backRelative, pos);
        
        var endRightFront = searchFrameLine(searchStart, getRight(facing));
        if (endRightFront.equals(BlockPos.ORIGIN)) {
            highlightBlock(searchStart);
            return false;
        }
        
        var endRightBack = searchFrameLine(endRightFront, getBackward(facing));
        if (endRightBack.equals(endRightFront)) {
            highlightBlock(endRightFront.add(getRight(facing)));
            highlightBlock(endRightFront.add(getBackward(facing)));
            return false;
        }
        
        var endLeftBack = searchFrameLine(endRightBack, getLeft(facing));
        if (endLeftBack.equals(endRightBack)) {
            highlightBlock(endRightBack.add(getBackward(facing)));
            highlightBlock(endRightBack.add(getLeft(facing)));
            return false;
        }
        
        var endLeftFront = searchFrameLine(endLeftBack, getForward(facing));
        if (endLeftFront.equals(endLeftBack)) {
            highlightBlock(endLeftBack.add(getLeft(facing)));
            highlightBlock(endLeftBack.add(getForward(facing)));
            return false;
        }
        
        var endMiddleFront = searchFrameLineEnd(endLeftFront, getRight(facing), searchStart);
        if (endMiddleFront.equals(endLeftFront)) {
            highlightBlock(endMiddleFront.add(getForward(facing)));
            highlightBlock(endMiddleFront.add(getRight(facing)));
            return false;
        }
        if (!endMiddleFront.equals(searchStart)) {
            highlightBlock(endMiddleFront.add(getRight(facing)));
            return false;
        }
        
        var innerValid = checkInnerEmpty(endLeftBack, endRightFront);
        if (!innerValid) return false;
        
        // offset values by 1 to define the working area instead of bounds
        var startX = Math.min(endLeftFront.getX(), endRightBack.getX()) + 1;
        var startZ = Math.min(endLeftFront.getZ(), endRightBack.getZ()) + 1;
        areaMin = new BlockPos(startX, getPos().getY(), startZ);
        
        var endX = Math.max(endLeftFront.getX(), endRightBack.getX()) - 1;
        var endZ = Math.max(endLeftFront.getZ(), endRightBack.getZ()) - 1;
        areaMax = new BlockPos(endX, getPos().getY(), endZ);
        
        if (currentTarget == null || !isInBounds(currentTarget)) {
            currentTarget = areaMin;
            lastTarget = areaMin;
        }
        
        updateNetwork();
        this.markDirty();
        
        return true;
    }
    
    protected Direction getFacing() {
        return Objects.requireNonNull(world).getBlockState(getPos()).get(Properties.HORIZONTAL_FACING);
    }
    
    private boolean checkInnerEmpty(BlockPos leftBack, BlockPos rightFront) {
        assert world != null;
        
        var lengthX = Math.abs(leftBack.getX() - rightFront.getX());
        var lengthZ = Math.abs(leftBack.getZ() - rightFront.getZ());
        
        var dirX = leftBack.getX() - rightFront.getX() > 0 ? -1 : 1;
        var dirZ = leftBack.getZ() - rightFront.getZ() > 0 ? -1 : 1;
        
        var valid = true;
        
        for (int x = 1; x < lengthX; x++) {
            for (int z = 1; z < lengthZ; z++) {
                var offset = new BlockPos(dirX * x, 0, dirZ * z);
                var checkPos = leftBack.add(offset);
                var foundBlock = world.getBlockState(checkPos).getBlock();
                if (!foundBlock.equals(Blocks.AIR)) {
                    highlightBlock(checkPos);
                    valid = false;
                }
            }
        }
        
        
        return valid;
    }
    
    private BlockPos searchFrameLine(BlockPos searchStart, Vec3i direction) {
        
        var lastPosition = BlockPos.ORIGIN;        // yes this will break if the frame starts at 0/0/0, however I'm willing to accept this
        
        for (int i = 0; i < MAX_SEARCH_LENGTH; i++) {
            var checkPos = searchStart.add(direction.multiply(i));
            if (testForFrame(checkPos)) {
                lastPosition = checkPos;
            } else {
                break;
            }
        }
        
        return lastPosition;
    }
    
    private BlockPos searchFrameLineEnd(BlockPos searchStart, Vec3i direction, BlockPos searchEnd) {
        
        var lastPosition = BlockPos.ORIGIN;        // yes this will break if the frame starts at 0/0/0, however I'm willing to accept this
        
        for (int i = 0; i < MAX_SEARCH_LENGTH; i++) {
            var checkPos = searchStart.add(direction.multiply(i));
            if (testForFrame(checkPos)) {
                
                if (checkPos.equals(searchEnd)) {
                    Oritech.LOGGER.debug("found start, machine is valid");
                    return checkPos;
                }
                
                lastPosition = checkPos;
            } else {
                break;
            }
        }
        
        return lastPosition;
    }
    
    @SuppressWarnings("DataFlowIssue")
    private boolean testForFrame(BlockPos pos) {
        var found = world.getBlockState(pos).getBlock();
        return found.equals(BlockContent.MACHINE_FRAME_BLOCK);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, FrameInteractionBlockEntity blockEntity) {
        if (world.isClient || !isActive(state) || !state.get(FrameInteractionBlock.HAS_FRAME) || getAreaMin() == null) return;
        
        if (!canProgress()) return;
        
        // yes this is inaccurate, but when the machine is this fast the move duration can just be skipped
        var skipMoveTime = getMoveTime() <= 1;
        if (skipMoveTime && moving && hasWorkAvailable(currentTarget))
            moving = false;
        
        if (!moving && currentProgress >= getWorkTime() && moveBlock()) {
            // if another machine occupies this position in the frame, we wait for it to move (with a timeout to avoid fully blocking everything)
            currentProgress = 0;
            finishBlockWork(lastTarget);
            updateToolPosInFrame();
            moving = true;
            updateNetwork();
            this.markDirty();
        } else if (moving && currentProgress >= getMoveTime()) {
            moving = false;
            currentProgress = 0;
            
            // basically skip work if not op is available
            if (!hasWorkAvailable(currentTarget)) currentProgress = getWorkTime();
        }
        
        doProgress(moving);
        currentProgress++;
        lastWorkedAt = world.getTime();
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
        frameEntries.put(pos, currentTarget);
    }
    
    public void cleanup() {
        var frameEntries = occupiedAreas.get(areaMin);
        if (frameEntries != null)
            frameEntries.remove(pos);
    }
    
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected abstract boolean hasWorkAvailable(BlockPos toolPosition);
    
    protected abstract void doProgress(boolean moving);
    
    protected abstract boolean canProgress();
    
    public abstract void finishBlockWork(BlockPos processed);
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (getCachedState().get(FrameInteractionBlock.HAS_FRAME) && areaMin != null) {
            nbt.putLong("areaMin", areaMin.asLong());
            nbt.putLong("areaMax", areaMax.asLong());
            nbt.putLong("currentTarget", currentTarget.asLong());
            nbt.putLong("currentDirection", new BlockPos(currentDirection).asLong());
            nbt.putInt("progress", currentProgress);
            nbt.putBoolean("moving", moving);
        }
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (getCachedState().get(FrameInteractionBlock.HAS_FRAME)) {
            areaMin = BlockPos.fromLong(nbt.getLong("areaMin"));
            areaMax = BlockPos.fromLong(nbt.getLong("areaMax"));
            currentTarget = BlockPos.fromLong(nbt.getLong("currentTarget"));
            currentDirection = BlockPos.fromLong(nbt.getLong("currentDirection"));
            lastTarget = currentTarget;
            currentProgress = nbt.getInt("progress");
            moving = nbt.getBoolean("moving");
        }
    }
    
    private boolean moveBlock() {
        
        var nextPos = currentTarget.add(currentDirection);
        var nextDir = currentDirection;
        if (!isInBounds(nextPos)) {
            nextPos = currentTarget.add(0, 0, 1);
            nextDir = currentDirection.multiply(-1);
            if (!isInBounds(nextPos)) {
                var data = resetWorkPosition();
                nextPos = data.getLeft();
                nextDir = data.getRight();
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
    private Pair<BlockPos, BlockPos> resetWorkPosition() {
        return new Pair<>(areaMin, new BlockPos(1, 0, 0));
    }
    
    private boolean isInBounds(BlockPos pos) {
        return pos.getX() >= areaMin.getX() && pos.getX() <= areaMax.getX()
                 && pos.getZ() >= areaMin.getZ() && pos.getZ() <= areaMax.getZ();
    }
    
    private void highlightBlock(BlockPos block) {
        ParticleContent.HIGHLIGHT_BLOCK.spawn(world, Vec3d.of(block), null);
    }
    
    public void updateNetwork() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.MachineFrameMovementPacket(pos, currentTarget, lastTarget, areaMin, areaMax));
    }
    
    @Override
    public void markDirty() {
        if (this.world != null)
            world.markDirty(pos);
    }
    
    public abstract BlockState getMachineHead();
    
    public int getFrameOffset() {
        return 1;
    }
    
    public float getSpeedMultiplier() { return 1f; }
    
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
        return currentProgress;
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
    
    public abstract int getMoveTime();
    
    public abstract int getWorkTime();
    
    public ItemStack getToolheadAdditionalRender() {
        return null;
    }
    
    public long getMoveStartedAt() {
        return moveStartedAt;
    }
    
    public void setMoveStartedAt(long moveStartedAt) {
        this.moveStartedAt = moveStartedAt;
    }
}
