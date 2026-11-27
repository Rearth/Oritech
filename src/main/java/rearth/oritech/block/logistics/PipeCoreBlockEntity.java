package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.logistics.PipeCore.DirectionState;
import rearth.oritech.block.logistics.PipeCore.PartPosition;
import rearth.oritech.block.logistics.PipeCore.PipeBlockData;
import rearth.oritech.block.logistics.PipeCore.PipeKind;
import rearth.oritech.block.logistics.PipeCore.PipePart;
import rearth.oritech.block.logistics.PipeNetworks.Node;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class PipeCoreBlockEntity extends BlockEntity {

    public static final ModelProperty<Map<PipeKind, List<AABB>>> MODEL_LAYOUT = new ModelProperty<>();

    // position = down means we're talking about the part at the bottom of the current block
    private final PipeBlockData parts = new PipeBlockData();

    private PipeCoreShape.BlockShapes pipeShapes = PipeCoreShape.EMPTY;

    public PipeCoreBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntitiesContent.PIPE_CORE.get(), worldPosition, blockState);
    }

    boolean hasMultipleParts() {
        var count = 0;
        for (var kind : PipeKind.values()) {
            count += parts.getByKind(kind).size();
        }
        return count > 1;
    }

    @Nullable PipeCoreShape.PartHit findHitPart(Vec3 start, Vec3 end) {
        return pipeShapes.findHitPart(worldPosition, start, end);
    }

    boolean hasPart(PipeKind kind, PartPosition position) {
        return getPart(kind, position) != null;
    }

    @Nullable PipePart getPart(PipeKind kind, PartPosition position) {
        return parts.getByKind(kind).get(position);
    }

    boolean isRibbonTop(PipeKind kind, Direction surface, Vec3 localHit) {
        return pipeShapes.isRibbonTop(kind, surface, localHit);
    }

    void forEachPart(PipeKind kind, BiConsumer<PartPosition, PipePart> action) {
        parts.getByKind(kind).forEach(action);
    }

    List<Vec3> connectionPath(PipeKind kind, PartPosition position, Direction direction, DirectionState state) {
        return PipeCoreShape.connectionPath(parts, kind, position, direction, state);
    }

    List<Vec3> debugConnectionPath(PipeKind kind, PartPosition position, Direction direction, DirectionState state) {
        var path = connectionPath(kind, position, direction, state);
        if (state != DirectionState.CONNECTED_EDGE) return path;

        // Extend the outside corner until it meets the other ribbon's center line.
        var axis = direction.getAxis();
        var targetCenter = PipeCoreShape.partCenter(kind, direction.getOpposite());
        var end = path.getLast().with(axis, targetCenter.get(axis) + direction.getAxisDirection().getStep());
        var extended = new ArrayList<>(path);
        extended.set(extended.size() - 1, end);
        return extended;
    }

    boolean addPart(PipeKind kind, PartPosition position) {
        if (parts.getByKind(kind).putIfAbsent(position, new PipePart()) != null) {
            return false;
        }

        updateConnections(true);
        return true;
    }

    void removePart(PipeKind kind, PartPosition position) {
        if (parts.getByKind(kind).remove(position) != null) {
            updateConnections(true);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            PipeNetworks.get(serverLevel).onPipeLoad(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!(level instanceof ServerLevel serverLevel)) return;

        // the chunk installs the replacement state before removing the old block entity
        // during a chunk unload the block state itself is still the pipe core
        var chunk = PipeCoreBlock.getLoadedChunk(serverLevel, worldPosition);
        if (chunk == null || chunk.getBlockState(worldPosition).is(getBlockState().getBlock())) return;

        PipeNetworks.get(serverLevel).removePipe(worldPosition);
    }

    public VoxelShape getPipeShape() {
        return pipeShapes.combined();
    }

    public boolean hasPipeParts() {
        return !pipeShapes.outlines().isEmpty();
    }

    public @Nullable HitOutline findHitOutline(Vec3 start, Vec3 end) {
        var part = findHitPart(start, end);
        return part == null ? null : new HitOutline(pipeShapes.outlines().get(part.kind()), part.shape());
    }

    public record HitOutline(VoxelShape kindShape, VoxelShape partShape) {}

    // mainly for shape caching
    private void onPartsChanged() {
        pipeShapes = PipeCoreShape.build(parts);
        setChanged();

        // the client needs the same parts and connections for block targeting
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var storedParts = output.child("parts");

        for (var kind : PipeKind.values()) {
            var kindParts = parts.getByKind(kind);
            if (kindParts.isEmpty()) continue;

            var storedKind = storedParts.child(kind.name());
            for (var entry : kindParts.entrySet()) {
                var storedPart = storedKind.child(entry.getKey().name());
                for (var direction : Direction.values()) {
                    var connection = entry.getValue().get(direction);
                    if (connection != PipeCore.DirectionState.NONE) {
                        storedPart.store(direction.getName(), PipeCore.DirectionState.CODEC, connection);
                    }
                }
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        var storedParts = input.childOrEmpty("parts");

        for (var kind : PipeKind.values()) {
            var kindParts = parts.getByKind(kind);
            kindParts.clear();

            // older saves stored positions directly under parts. Those parts become item pipes.
            var storedKind = kind == PipeKind.ITEM ? storedParts.child(kind.name()).orElse(storedParts) : storedParts.childOrEmpty(kind.name());
            for (var position : PartPosition.values()) {
                storedKind.child(position.name()).ifPresent(storedPart -> {
                    var part = new PipePart();
                    for (var direction : Direction.values()) {
                        part.set(direction, storedPart.read(direction.getName(), PipeCore.DirectionState.CODEC).orElse(PipeCore.DirectionState.NONE));
                    }
                    kindParts.put(position, part);
                });
            }
        }

        pipeShapes = PipeCoreShape.build(parts);

        // A part update can arrive without changing the block state. Refresh after reading its data.
        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        // Commands can replace the parts of a core that is already loaded.
        if (level instanceof ServerLevel serverLevel) {
            PipeNetworks.get(serverLevel).queueRefresh(worldPosition);
        }
    }

    @Override
    public ModelData getModelData() {
        if (!hasPipeParts()) return ModelData.EMPTY;

        // Chunk builders use a snapshot instead of reading the live block entity.
        return ModelData.of(MODEL_LAYOUT, PipeCoreShape.buildModelLayout(parts, pipeShapes));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void updateConnections() {
        updateConnections(false);
    }

    private void updateConnections(boolean partsChanged) {
        if (level instanceof ServerLevel serverLevel) {
            PipeNetworks.get(serverLevel).updatePipe(serverLevel, this, partsChanged);
        } else if (partsChanged) {
            onPartsChanged();
        }
    }

    // Keep the live geometry in sync with the saved directions, including arms into unloaded chunks.
    void applyConnections(PipeNetworks networks, boolean changed) {
        for (var kind : PipeKind.values()) {
            for (var partEntry : parts.getByKind(kind).entrySet()) {
                var node = new Node(worldPosition, kind, partEntry.getKey());
                var part = partEntry.getValue();
                for (var direction : Direction.values()) {
                    var state = networks.getDirection(node, direction);
                    if (part.get(direction) != state) {
                        part.set(direction, state);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            onPartsChanged();
        }
    }

}
