package rearth.oritech.block.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.logistics.PipeCore.DirectionState;
import rearth.oritech.block.logistics.PipeCore.PartPosition;
import rearth.oritech.block.logistics.PipeCore.PipeKind;
import rearth.oritech.init.BlockContent;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Pipe topology stays in the dimension's saved data when chunks unload.
@EventBusSubscriber(modid = Oritech.MOD_ID)
public final class PipeNetworks extends SavedData {

    private static final int SAVE_VERSION = 1;
    private static final PipeKind[] KINDS = PipeKind.values();
    private static final PartPosition[] POSITIONS = PartPosition.values();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Codec<PipeNetworks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(SAVE_VERSION, SAVE_VERSION).fieldOf("version").forGetter(networks -> SAVE_VERSION),
        StoredPart.CODEC.listOf().fieldOf("parts").forGetter(PipeNetworks::storedParts)
    ).apply(instance, (version, parts) -> new PipeNetworks(parts)));
    private static final SavedDataType<PipeNetworks> TYPE = new SavedDataType<>(Oritech.id("ribbon_pipe_networks"), PipeNetworks::new, CODEC);

    // Remember every pipe part, even an isolated one, and where each arm points.
    // Two parts belong to the same network when their arms point back at each other.
    private final Map<Node, Connections> parts = new HashMap<>();
    // Group pipe locations by chunk so loading a chunk doesn't require searching the whole dimension.
    private final Map<ChunkPos, Set<BlockPos>> positionsByChunk = new HashMap<>();
    // Collect changed block positions so nearby pipes can be checked together at the end of the tick.
    private final Set<BlockPos> pendingRefresh = new HashSet<>();
    // Remember newly loaded chunks until their block entities are ready to compare with the saved pipes.
    private final Set<ChunkPos> pendingChunks = new HashSet<>();

    private PipeNetworks() {}

    private PipeNetworks(List<StoredPart> savedParts) {
        for (var part : savedParts) {
            parts.put(part.node(), Connections.of(part.node(), part.directions()));
            indexPosition(part.node().pos());
        }
    }

    public static PipeNetworks get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    static @Nullable PipeNetworks getExisting(ServerLevel level) {
        return level.getDataStorage().get(TYPE);
    }

    public record Node(BlockPos pos, PipeKind kind, PartPosition position) {
        private static final Codec<Node> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Node::pos),
            PipeKind.CODEC.fieldOf("kind").forGetter(Node::kind),
            PartPosition.CODEC.fieldOf("slot").forGetter(Node::position)
        ).apply(instance, Node::new));

        public Node {
            pos = pos.immutable();
        }
    }

    private record StoredPart(Node node, Map<Direction, DirectionState> directions) {
        private static final Codec<StoredPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Node.CODEC.fieldOf("part").forGetter(StoredPart::node),
            Codec.unboundedMap(Direction.CODEC, DirectionState.CODEC).fieldOf("directions").forGetter(StoredPart::directions)
        ).apply(instance, StoredPart::new));
    }

    private record Connections(Map<Direction, DirectionState> directions, Set<Node> targets) {
        private static final Connections EMPTY = new Connections(Map.of(), Set.of());

        private static Connections of(Node node, Map<Direction, DirectionState> directions) {
            if (directions.isEmpty()) return EMPTY;
            var targets = new HashSet<Node>();
            directions.forEach((direction, state) -> {
                var target = connectionTarget(node, direction, state);
                if (target != null) targets.add(target);
            });
            return new Connections(Map.copyOf(directions), Set.copyOf(targets));
        }
    }

    private List<StoredPart> storedParts() {
        return parts.entrySet().stream().map(entry -> new StoredPart(entry.getKey(), entry.getValue().directions())).toList();
    }

    public Set<Node> getConnections(Node node) {
        var connections = new HashSet<Node>();
        for (var target : parts.getOrDefault(node, Connections.EMPTY).targets()) {
            if (hasReturnConnection(node, target)) connections.add(target);
        }
        return Collections.unmodifiableSet(connections);
    }

    // Walk the current component on demand, including its saved parts in unloaded chunks.
    public Set<Node> getNetwork(Node start) {
        if (!parts.containsKey(start)) return Set.of();

        var visited = new HashSet<Node>();
        var pending = new ArrayDeque<Node>();
        visited.add(start);
        pending.add(start);

        while (!pending.isEmpty()) {
            var node = pending.removeFirst();
            for (var target : parts.get(node).targets()) {
                if (hasReturnConnection(node, target) && visited.add(target)) pending.addLast(target);
            }
        }
        return Collections.unmodifiableSet(visited);
    }

    private boolean hasReturnConnection(Node source, Node target) {
        return parts.getOrDefault(target, Connections.EMPTY).targets().contains(source);
    }

    DirectionState getDirection(Node node, Direction direction) {
        return parts.getOrDefault(node, Connections.EMPTY).directions().getOrDefault(direction, DirectionState.NONE);
    }

    Set<Node> getLoadedNodes(ServerLevel level) {
        var nodes = new HashSet<Node>();
        positionsByChunk.forEach((chunk, positions) -> {
            if (level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) == null) return;
            for (var pos : positions) {
                for (var kind : KINDS) {
                    for (var slot : POSITIONS) {
                        var node = new Node(pos, kind, slot);
                        if (parts.containsKey(node)) nodes.add(node);
                    }
                }
            }
        });
        return nodes;
    }

    void updatePipe(ServerLevel level, PipeCoreBlockEntity pipe, boolean partsChanged) {
        var pos = pipe.getBlockPos();
        if (indexParts(pos, pipe)) queueRefresh(pos);
        resolvePipe(level, pos);
        pipe.applyConnections(this, partsChanged);
    }

    private boolean indexParts(BlockPos pos, @Nullable PipeCoreBlockEntity pipe) {
        var changed = false;
        var hasParts = false;
        for (var kind : KINDS) {
            for (var slot : POSITIONS) {
                var node = new Node(pos, kind, slot);
                if (pipe != null && pipe.hasPart(kind, slot)) {
                    hasParts = true;
                    changed |= parts.putIfAbsent(node, Connections.EMPTY) == null;
                } else {
                    changed |= parts.remove(node) != null;
                }
            }
        }

        if (changed) {
            if (hasParts) {
                indexPosition(pos);
            } else {
                var chunk = ChunkPos.containing(pos);
                var positions = positionsByChunk.get(chunk);
                if (positions != null && positions.remove(pos) && positions.isEmpty()) positionsByChunk.remove(chunk);
            }
            setDirty();
        }
        return changed;
    }

    private void indexPosition(BlockPos pos) {
        positionsByChunk.computeIfAbsent(ChunkPos.containing(pos), chunk -> new HashSet<>()).add(pos);
    }

    void removePipe(BlockPos pos) {
        if (indexParts(pos, null)) queueRefresh(pos);
    }

    private boolean hasIndexedPipe(BlockPos pos) {
        return positionsByChunk.getOrDefault(ChunkPos.containing(pos), Set.of()).contains(pos);
    }

    private void resolvePipe(ServerLevel level, BlockPos pos) {
        if (!hasIndexedPipe(pos)) return;

        for (var kind : KINDS) {
            for (var slot : POSITIONS) {
                var node = new Node(pos, kind, slot);
                var previous = parts.get(node);
                if (previous == null) continue;

                var directions = new EnumMap<Direction, DirectionState>(Direction.class);
                for (var direction : DIRECTIONS) {
                    var state = resolveConnection(level, node, direction, previous.directions().get(direction));
                    if (state != DirectionState.NONE) directions.put(direction, state);
                }
                if (!previous.directions().equals(directions)) {
                    parts.put(node, Connections.of(node, directions));
                    setDirty();
                }
            }
        }
    }

    // connection rules (part -> part) based on each part's local position
    // all routes can only connect parts of the same kind
    // inside the same blockspace:
    //   routes can go across inside corners to connect two perpendicular surfaces (e.g. from down to east)
    //   routes cannot connect opposite surfaces (e.g. from the floor to the ceiling)
    //   routes can connect from a surface to the center
    // to neighboring blocks across back edges for surface parts:
    //   routes can wrap around outside corners if both parts use the same support block
    //   routes can only wrap around a corner if the block along the straight path has no full-block collision shape
    // to neighboring blocks for surface parts:
    //   straight routes can only connect if the neighboring block has a part in the same part position (e.g. if they share a surface)
    //   straight routes take priority over outside wraps
    // for center parts:
    //   routes can connect to neighboring center parts or any surface part within the same blockspace
    private DirectionState resolveConnection(ServerLevel level, Node node, Direction direction, @Nullable DirectionState previous) {
        var surface = PartPosition.ToDirection(node.position());
        if (direction == surface) return DirectionState.NONE;

        if (surface != null && direction == surface.getOpposite()) {
            return hasPartAt(level, node.pos(), node.kind(), PartPosition.CENTER) ? DirectionState.CONNECTED_INSIDE : DirectionState.NONE;
        }

        if (hasPartAt(level, node.pos(), node.kind(), PartPosition.FromDirection(direction))) {
            return surface == null ? DirectionState.CONNECTED_INSIDE : DirectionState.CONNECTED_CORNER;
        }

        var neighbor = node.pos().relative(direction);
        if (hasPartAt(level, neighbor, node.kind(), node.position())) return DirectionState.CONNECTED;

        if (surface == null || !hasPartAt(level, neighbor.relative(surface), node.kind(), PartPosition.FromDirection(direction.getOpposite()))) {
            return DirectionState.NONE;
        }

        // A wrap can cross a third chunk. Keep its last verified choice until the obstruction can be checked.
        var chunk = PipeCoreBlock.getLoadedChunk(level, neighbor);
        if (chunk == null) return previous == DirectionState.CONNECTED_EDGE ? previous : DirectionState.NONE;

        return chunk.getBlockState(neighbor).isCollisionShapeFullBlock(level, neighbor) ? DirectionState.NONE : DirectionState.CONNECTED_EDGE;
    }

    private boolean hasPartAt(ServerLevel level, BlockPos pos, PipeKind kind, PartPosition slot) {
        var chunk = PipeCoreBlock.getLoadedChunk(level, pos);
        if (chunk == null) return parts.containsKey(new Node(pos, kind, slot));
        return chunk.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe && pipe.hasPart(kind, slot);
    }

    static @Nullable Node connectionTarget(Node node, Direction direction, DirectionState state) {
        var pos = node.pos();
        var kind = node.kind();
        var position = node.position();
        return switch (state) {
            case CONNECTED -> new Node(pos.relative(direction), kind, position);
            case CONNECTED_INSIDE -> new Node(pos, kind,
                position == PartPosition.CENTER ? PartPosition.FromDirection(direction) : PartPosition.CENTER);
            case CONNECTED_CORNER -> new Node(pos, kind, PartPosition.FromDirection(direction));
            case CONNECTED_EDGE -> {
                var surface = PartPosition.ToDirection(position);
                yield surface == null ? null : new Node(pos.relative(direction).relative(surface),
                    kind, PartPosition.FromDirection(direction.getOpposite()));
            }
            case NONE -> null;
        };
    }

    void queueRefresh(BlockPos pos) {
        pendingRefresh.add(pos.immutable());
    }

    void refreshNearby(ServerLevel level, BlockPos pos) {
        PipeCoreBlock.forEachNearbyPipePosition(pos, target -> refreshAt(level, target));
        pendingRefresh.remove(pos);
    }

    private void refreshAt(ServerLevel level, BlockPos pos) {
        var chunk = PipeCoreBlock.getLoadedChunk(level, pos);
        if (chunk == null) {
            resolvePipe(level, pos);
        } else if (chunk.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe) {
            updatePipe(level, pipe, false);
        } else if (hasIndexedPipe(pos)) {
            removePipe(pos);
        }
    }

    // A neighbor notification can reach the graph even when both saved wrap endpoints are offline.
    @SubscribeEvent
    public static void onNeighborNotify(NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var networks = getExisting(level);
        if (networks == null || networks.positionsByChunk.isEmpty()) return;

        var pos = event.getPos();
        if (networks.hasIndexedPipe(pos)) {
            networks.queueRefresh(pos);
            return;
        }
        for (var direction : DIRECTIONS) {
            if (networks.hasIndexedPipe(pos.relative(direction))) {
                networks.queueRefresh(pos);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        var networks = getExisting(level);
        if (networks == null) {
            var chunk = event.getChunk();
            if (chunk.getBlockEntitiesPos().stream().noneMatch(pos -> chunk.getBlockState(pos).is(BlockContent.PIPE_CORE.get()))) return;
            networks = get(level);
        }
        // The event fires before the chunk is fully available. Reconcile at the end of the level tick.
        networks.pendingChunks.add(event.getChunk().getPos());
    }

    void onPipeLoad(PipeCoreBlockEntity pipe) {
        // A pipe can also be installed into a chunk that is already loaded.
        if (!pendingChunks.contains(ChunkPos.containing(pipe.getBlockPos()))) reconcilePipe(pipe);
    }

    public static void tick(ServerLevel level) {
        var networks = getExisting(level);
        if (networks == null || (networks.pendingChunks.isEmpty() && networks.pendingRefresh.isEmpty())) return;

        var chunks = Set.copyOf(networks.pendingChunks);
        networks.pendingChunks.clear();
        for (var pos : chunks) {
            var chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
            if (chunk != null) networks.reconcileChunk(chunk);
        }

        var nearby = new HashSet<BlockPos>();
        for (var pos : chunks) {
            networks.collectChunkConnections(pos, nearby);
        }
        for (var pos : networks.pendingRefresh) {
            PipeCoreBlock.forEachNearbyPipePosition(pos, nearby::add);
        }
        networks.pendingRefresh.clear();

        for (var pos : nearby) networks.refreshAt(level, pos);
    }

    private void reconcileChunk(LevelChunk chunk) {
        // Check both sets to discover old pipes and drop stale records after a rollback or an external edit.
        var positions = new HashSet<>(positionsByChunk.getOrDefault(chunk.getPos(), Set.of()));
        positions.addAll(chunk.getBlockEntitiesPos());
        for (var pos : positions) {
            if (chunk.getBlockState(pos).is(BlockContent.PIPE_CORE.get()) && chunk.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe) {
                reconcilePipe(pipe);
            } else if (hasIndexedPipe(pos)) {
                removePipe(pos);
            }
        }
    }

    private void reconcilePipe(PipeCoreBlockEntity pipe) {
        var pos = pipe.getBlockPos();
        if (indexParts(pos, pipe)) queueRefresh(pos);
        else pipe.applyConnections(this, false);
    }

    private void collectChunkConnections(ChunkPos chunk, Set<BlockPos> affected) {
        // Recheck both sides of the border, even when the arriving chunk contains only the obstruction for a wrap.
        // Unchanged interior parts only need a new check if a block can obstruct an outside wrap.
        ChunkPos.rangeClosed(chunk, 1).forEach(neighbor -> {
            for (var pos : positionsByChunk.getOrDefault(neighbor, Set.of())) {
                int x = pos.getX();
                int z = pos.getZ();
                if (x < chunk.getMinBlockX() - 1 || x > chunk.getMaxBlockX() + 1
                    || z < chunk.getMinBlockZ() - 1 || z > chunk.getMaxBlockZ() + 1) continue;

                if (x <= chunk.getMinBlockX() || x >= chunk.getMaxBlockX() || z <= chunk.getMinBlockZ() || z >= chunk.getMaxBlockZ()
                    || hasOutsideWrapCandidate(pos)) affected.add(pos);
            }
        });
    }

    private boolean hasOutsideWrapCandidate(BlockPos pos) {
        for (var kind : KINDS) {
            for (var slot : POSITIONS) {
                var surface = PartPosition.ToDirection(slot);
                if (surface == null || !parts.containsKey(new Node(pos, kind, slot))) continue;
                for (var direction : DIRECTIONS) {
                    if (direction.getAxis() == surface.getAxis()) continue;
                    var target = pos.relative(direction).relative(surface);
                    var targetSlot = PartPosition.FromDirection(direction.getOpposite());
                    if (parts.containsKey(new Node(target, kind, targetSlot))) return true;
                }
            }
        }
        return false;
    }
}
