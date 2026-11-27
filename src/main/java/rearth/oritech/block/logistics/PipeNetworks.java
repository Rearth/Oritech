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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.logistics.PipeCore.DirectionState;
import rearth.oritech.block.logistics.PipeCore.PartPosition;
import rearth.oritech.block.logistics.PipeCore.PipeBlockData;
import rearth.oritech.block.logistics.PipeCore.PipeKind;
import rearth.oritech.block.logistics.PipeCore.PipePart;
import rearth.oritech.init.BlockContent;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
    // NeoForge holds weak references to these listeners. Share one across all pipes probing a block.
    private final Map<BlockPos, ICapabilityInvalidationListener> machineListeners = new HashMap<>();

    private PipeNetworks() {}

    private PipeNetworks(List<StoredPart> savedParts) {
        for (var part : savedParts) {
            parts.put(part.node(), Connections.of(part.node(), part.directions(), part.closedPorts()));
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

    public record Port(Node node, Direction direction) {}

    private record StoredPart(Node node, Map<Direction, DirectionState> directions, Map<Direction, DirectionState> closedPorts) {
        private static final Codec<StoredPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Node.CODEC.fieldOf("part").forGetter(StoredPart::node),
            Codec.unboundedMap(Direction.CODEC, DirectionState.CODEC).fieldOf("directions").forGetter(StoredPart::directions),
            PipePart.CLOSED_PORTS_CODEC.optionalFieldOf("closed_ports", Map.of()).forGetter(StoredPart::closedPorts),
            Codec.INT.optionalFieldOf("disabled_directions", 0).forGetter(part -> 0)
        ).apply(instance, (node, directions, closedPorts, legacyMask) -> new StoredPart(node, directions,
            closedPorts.isEmpty() ? PipePart.readLegacyClosedPorts(legacyMask) : closedPorts)));
    }

    private record Connections(Map<Direction, DirectionState> directions, Set<Node> targets, Map<Direction, DirectionState> closedPorts) {
        private static final Connections EMPTY = new Connections(Map.of(), Set.of(), Map.of());

        private static Connections of(Node node, Map<Direction, DirectionState> directions, Map<Direction, DirectionState> closedPorts) {
            if (directions.isEmpty()) return EMPTY.withClosedPorts(closedPorts);
            var targets = new HashSet<Node>();
            directions.forEach((direction, state) -> {
                var target = connectionTarget(node, direction, state);
                if (target != null) targets.add(target);
            });
            return new Connections(Map.copyOf(directions), Set.copyOf(targets), Map.copyOf(closedPorts));
        }

        private Connections withClosedPorts(Map<Direction, DirectionState> ports) {
            return closedPorts.equals(ports) ? this : new Connections(directions, targets, Map.copyOf(ports));
        }
    }

    private List<StoredPart> storedParts() {
        return parts.entrySet().stream().map(entry -> new StoredPart(entry.getKey(), entry.getValue().directions(), entry.getValue().closedPorts())).toList();
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

    Map<Direction, DirectionState> getClosedPorts(Node node) {
        return parts.getOrDefault(node, Connections.EMPTY).closedPorts();
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
                var part = pipe == null ? null : pipe.getPart(kind, slot);
                if (part != null) {
                    hasParts = true;
                    // Once indexed, the dimension owns the port settings so an unloaded endpoint can be edited.
                    changed |= parts.putIfAbsent(node, Connections.EMPTY.withClosedPorts(part.closedPorts)) == null;
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
                for (var direction : DIRECTIONS) {
                    var target = pos.relative(direction);
                    if (!hasAdjacentPipe(target)) machineListeners.remove(target);
                }
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
                    var port = new Port(node, direction);
                    var state = autoDirection(level, port, TopologyChange.NONE);
                    mirrorClosedPort(level, port, state);
                    if (state != DirectionState.NONE && isOpen(port, state, TopologyChange.NONE)) directions.put(direction, state);
                }
                if (!previous.directions().equals(directions)) {
                    parts.put(node, Connections.of(node, directions, getClosedPorts(node)));
                    setDirty();
                }
            }
        }
    }

    private static @Nullable Port linkedPort(Port source, DirectionState state) {
        var target = connectionTarget(source.node(), source.direction(), state);
        if (target == null) return null;

        // At corners, the far arm points along the source ribbon's normal instead of back along the same axis.
        var targetDirection = switch (state) {
            case CONNECTED_CORNER -> PartPosition.ToDirection(source.node().position());
            case CONNECTED_EDGE -> PartPosition.ToDirection(source.node().position()).getOpposite();
            default -> source.direction().getOpposite();
        };
        return targetDirection == null ? null : new Port(target, targetDirection);
    }

    private @Nullable DirectionState closedState(Port port) {
        return getClosedPorts(port.node()).get(port.direction());
    }

    private DirectionState autoDirection(ServerLevel level, Port port, TopologyChange change) {
        var node = port.node();
        var direction = port.direction();
        var previous = getDirection(node, direction);
        var closed = change.closedState(port, closedState(port));
        if (previous == DirectionState.NONE && closed != null) previous = closed;
        return resolveConnection(level, node, direction, previous, change);
    }

    private boolean isOpen(Port port, DirectionState state, TopologyChange change) {
        if (change.closedState(port, closedState(port)) != null) return false;
        var target = linkedPort(port, state);
        return target == null || change.closedState(target, closedState(target)) == null;
    }

    private void mirrorClosedPort(ServerLevel level, Port source, DirectionState state) {
        var closed = closedState(source);
        if (closed == null || state == DirectionState.NONE) return;
        // Older saves record only a closed direction. The first valid route supplies the matching endpoint.
        if (closed == DirectionState.NONE) setClosedPort(source, state);
        else if (closed != state) return;

        var target = linkedPort(source, state);
        if (target != null && parts.containsKey(target.node()) && closedState(target) == null
            && source.equals(linkedPort(target, autoDirection(level, target, TopologyChange.NONE)))) {
            setClosedPort(target, state);
        }
    }

    private void setClosedPort(Port port, @Nullable DirectionState state) {
        var previous = parts.get(port.node());
        if (previous == null || previous.closedPorts().get(port.direction()) == state) return;

        var ports = new EnumMap<Direction, DirectionState>(Direction.class);
        ports.putAll(previous.closedPorts());
        if (state == null) ports.remove(port.direction());
        else ports.put(port.direction(), state);
        parts.put(port.node(), previous.withClosedPorts(ports));
        queueRefresh(port.node().pos());
        setDirty();
    }

    private record PortChange(Port source, @Nullable Port target, DirectionState route, boolean reopen) {}

    private @Nullable PortChange getPortChange(Port source) {
        var closed = closedState(source);
        var state = closed != null ? closed : getDirection(source.node(), source.direction());
        if (state == DirectionState.NONE && closed == null) return null;

        var target = linkedPort(source, state);
        if (target != null) {
            var returnState = closed != null ? closedState(target) : getDirection(target.node(), target.direction());
            if (returnState == null || !source.equals(linkedPort(target, returnState))) target = null;
        }
        return new PortChange(source, target, state, closed != null);
    }

    boolean toggleConnection(ServerLevel level, Port source) {
        var change = getPortChange(source);
        if (change == null) return false;

        var state = change.reopen() ? null : change.route();
        setClosedPort(source, state);
        if (change.target() != null) setClosedPort(change.target(), state);
        refreshNearby(level, source.node().pos());
        return true;
    }

    private record TopologyChange(@Nullable Node added, @Nullable PortChange ports) {
        private static final TopologyChange NONE = new TopologyChange(null, null);

        private @Nullable DirectionState closedState(Port port, @Nullable DirectionState previous) {
            if (ports != null && (port.equals(ports.source()) || port.equals(ports.target()))) {
                return ports.reopen() ? null : ports.route();
            }
            return previous;
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

    // connection rules to machines (part -> machine):
    // a central part can connect to machines in all 6 directions, if there's no a part of that side
    // a surface part can connect to machines under it (with a special indicator on the model indicating that "down" direction
    // a surface part cannot connect directly to machines above it
    // a surface part can connect to machines next to it (e.g. in the neighboring blockspace) if there's no other pipe part on that side
    private DirectionState resolveConnection(ServerLevel level, Node node, Direction direction, DirectionState previous, TopologyChange change) {
        var surface = PartPosition.ToDirection(node.position());
        var neighbor = node.pos().relative(direction);
        if (direction == surface) return resolveMachine(level, node.kind(), neighbor, direction, previous, change);

        if (surface != null && direction == surface.getOpposite()) {
            return hasPartAt(level, node.pos(), node.kind(), PartPosition.CENTER, change) ? DirectionState.CONNECTED_INSIDE : DirectionState.NONE;
        }

        if (hasPartAt(level, node.pos(), node.kind(), PartPosition.FromDirection(direction), change)) {
            return surface == null ? DirectionState.CONNECTED_INSIDE : DirectionState.CONNECTED_CORNER;
        }

        if (hasPartAt(level, neighbor, node.kind(), node.position(), change)) return DirectionState.CONNECTED;

        if (surface != null && hasPartAt(level, neighbor.relative(surface), node.kind(), PartPosition.FromDirection(direction.getOpposite()), change)) {
            // A wrap can cross a third chunk. Keep its last verified choice until the obstruction can be checked.
            var chunk = PipeCoreBlock.getLoadedChunk(level, neighbor);
            if (chunk == null && previous == DirectionState.CONNECTED_EDGE) return previous;
            if (chunk != null && (change.added() != null && change.added().pos().equals(neighbor)
                || !chunk.getBlockState(neighbor).isCollisionShapeFullBlock(level, neighbor))) {
                return DirectionState.CONNECTED_EDGE;
            }
        }

        return resolveMachine(level, node.kind(), neighbor, direction, previous, change);
    }

    private DirectionState resolveMachine(ServerLevel level, PipeKind kind, BlockPos target, Direction direction, DirectionState previous, TopologyChange change) {
        if (level.isOutsideBuildHeight(target)) return DirectionState.NONE;
        if (change.added() != null && change.added().pos().equals(target)) return DirectionState.NONE;
        var chunk = PipeCoreBlock.getLoadedChunk(level, target);
        // Keep the last known port for rendering across chunk borders. Check it again when the target loads.
        if (chunk == null) return previous == DirectionState.CONNECTED_MACHINE ? previous : DirectionState.NONE;

        var state = chunk.getBlockState(target);
        if (state.is(BlockContent.PIPE_CORE.get())) return DirectionState.NONE;

        // Also watch empty faces so a machine can expose a new port without replacing its block.
        if (change == TopologyChange.NONE) machineListeners.computeIfAbsent(target, pos -> {
            var listener = new ICapabilityInvalidationListener() {
                @Override
                public boolean onInvalidate() {
                    if (machineListeners.get(pos) != this) return false;
                    queueRefresh(pos);
                    return true;
                }
            };
            level.registerCapabilityListener(pos, listener);
            return listener;
        });

        var face = direction.getOpposite();
        var capability = switch (kind) {
            case ITEM -> Capabilities.Item.BLOCK;
            case FLUID -> Capabilities.Fluid.BLOCK;
            case ENERGY -> Capabilities.Energy.BLOCK;
        };
        return level.getCapability(capability, target, state, null, face) != null
            ? DirectionState.CONNECTED_MACHINE : DirectionState.NONE;
    }

    private boolean hasPartAt(ServerLevel level, BlockPos pos, PipeKind kind, PartPosition slot, TopologyChange change) {
        var added = change.added();
        if (added != null && added.kind() == kind && added.position() == slot && added.pos().equals(pos)) return true;
        var chunk = PipeCoreBlock.getLoadedChunk(level, pos);
        if (chunk == null) return parts.containsKey(new Node(pos, kind, slot));
        return chunk.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe && pipe.hasPart(kind, slot);
    }

    static VoxelShape previewPlacement(ServerLevel level, Node added) {
        var networks = getExisting(level);
        if (networks == null) networks = new PipeNetworks();
        var geometry = networks.previewGeometry(level, added, new TopologyChange(added, null), true);
        return Shapes.or(geometry, PipeCoreShape.partShape(added.kind(), added.position()));
    }

    @Nullable WrenchPreview previewWrench(ServerLevel level, Port port) {
        var change = getPortChange(port);
        if (change == null) return null;

        var node = port.node();
        var shape = previewGeometry(level, node, new TopologyChange(null, change), change.reopen());
        if (shape.isEmpty()) shape = PipeCoreShape.portMarker(node.kind(), node.position(), port.direction());
        return new WrenchPreview(change.reopen(), shape);
    }

    record WrenchPreview(boolean reopen, VoxelShape shape) {}

    private VoxelShape previewGeometry(ServerLevel level, Node origin, TopologyChange change, boolean adding) {
        var positions = new ArrayList<BlockPos>();
        PipeCoreBlock.forEachNearbyPipePosition(origin.pos(), positions::add);
        var before = Shapes.empty();
        var after = Shapes.empty();
        for (var pos : positions) {
            var pipe = PipeCoreBlock.getLoadedPipe(level, pos);
            if (pipe == null && !(change.added() != null && change.added().pos().equals(pos))) continue;

            // Leave nearby block entities untouched. Resolve the hypothetical ports into a temporary layout.
            var preview = new PipeBlockData();
            var changed = false;
            for (var kind : KINDS) {
                for (var slot : POSITIONS) {
                    var existing = pipe == null ? null : pipe.getPart(kind, slot);
                    var node = new Node(pos, kind, slot);
                    if (existing == null && !node.equals(change.added())) continue;
                    var part = existing;
                    if (kind == origin.kind()) {
                        part = new PipePart();
                        if (existing == null) changed = true;
                        else part.extractionPorts.addAll(existing.extractionPorts);
                        for (var direction : DIRECTIONS) {
                            var port = new Port(node, direction);
                            var state = autoDirection(level, port, change);
                            if (state != DirectionState.NONE && !isOpen(port, state, change)) state = DirectionState.NONE;
                            part.set(direction, state);
                            if (existing != null && existing.get(direction) != state) changed = true;
                        }
                    }
                    preview.getByKind(kind).put(slot, part);
                }
            }

            var offset = pos.subtract(origin.pos());
            var oldShape = pipe == null ? Shapes.empty() : pipe.modelShape(origin.kind());
            var newShape = changed ? PipeCoreShape.modelShape(preview, PipeCoreShape.build(preview), origin.kind()) : oldShape;
            before = Shapes.or(before, oldShape.move(offset));
            after = Shapes.or(after, newShape.move(offset));
        }
        return adding ? Shapes.join(after, before, BooleanOp.ONLY_FIRST) : Shapes.join(before, after, BooleanOp.ONLY_FIRST);
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
            case NONE, CONNECTED_MACHINE -> null;
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
        if (networks.hasIndexedPipe(pos) || networks.hasAdjacentPipe(pos)) {
            networks.queueRefresh(pos);
        }
    }

    private boolean hasAdjacentPipe(BlockPos pos) {
        for (var direction : DIRECTIONS) {
            if (hasIndexedPipe(pos.relative(direction))) return true;
        }
        return false;
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
        // Check interior machine ports and both sides of the border when their blocks become available.
        // The arriving chunk can also contain only the obstruction for a wrap.
        ChunkPos.rangeClosed(chunk, 1).forEach(neighbor -> {
            for (var pos : positionsByChunk.getOrDefault(neighbor, Set.of())) {
                int x = pos.getX();
                int z = pos.getZ();
                if (x < chunk.getMinBlockX() - 1 || x > chunk.getMaxBlockX() + 1
                    || z < chunk.getMinBlockZ() - 1 || z > chunk.getMaxBlockZ() + 1) continue;

                affected.add(pos);
            }
        });
    }

}
