package rearth.oritech.block.entity.pipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.AbstractPipeBlock;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.entity.interaction.PipeBoosterBlockEntity;

import java.util.*;

import static rearth.oritech.block.blocks.pipes.GenericPipeBlock.NO_CONNECTION;


public abstract class GenericPipeInterfaceEntity extends BlockEntity implements BlockEntityTicker<GenericPipeInterfaceEntity> {

    public static final int MAX_SEARCH_COUNT = 2048;

    public BlockPos connectedBooster = BlockPos.ZERO;

    private PipeBoosterBlockEntity cachedBooster;

    public GenericPipeInterfaceEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isBoostAvailable() {
        var booster = tryGetCachedBooster();
        return booster != null && booster.canUseBoost();
    }

    public void onBoostUsed() {
        var booster = tryGetCachedBooster();
        if (booster != null) booster.useBoost();
    }

    @Nullable
    private PipeBoosterBlockEntity tryGetCachedBooster() {

        // booster was removed
        if (cachedBooster != null && cachedBooster.isRemoved()) {
            cachedBooster = null;
            connectedBooster = BlockPos.ZERO;
            return null;
        }

        if (connectedBooster == BlockPos.ZERO) {  // no booster set
            if (cachedBooster != null) cachedBooster = null;
            return null;
        } else if (cachedBooster == null) { // booster freshly set
            var candidate = level.getBlockEntity(connectedBooster);
            if (candidate instanceof PipeBoosterBlockEntity booster) {
                cachedBooster = booster;
                return cachedBooster;
            } else {
                connectedBooster = BlockPos.ZERO;
                return null;
            }
        } else {    // no change
            return cachedBooster;
        }
    }

    public static void addNode(Level level, BlockPos pos, boolean isInterface, BlockState newState, PipeNetworkData data) {
        Oritech.LOGGER.debug("registering/updating node: " + pos);

        var connectedMachineDirections = EnumSet.noneOf(Direction.class);
        var block = (AbstractPipeBlock) newState.getBlock();
        for (var neighbor : Direction.values()) {
            var neighborPos = pos.relative(neighbor);
            var neighborMap = new HashSet<>(data.machinePipeNeighbors.getOrDefault(neighborPos, Set.of()));
            if (block.hasMachineInDirection(neighbor, level, pos, block.apiValidationFunction())) {
                if (block.isConnectingInDirection(newState, neighbor, pos, level, false))
                    connectedMachineDirections.add(neighbor);

                neighborMap.add(neighbor.getOpposite());
            } else {
                neighborMap.remove(neighbor.getOpposite());
            }

            if (!neighborMap.isEmpty()) data.machinePipeNeighbors.put(neighborPos, neighborMap);
            else data.machinePipeNeighbors.remove(neighborPos);
        }

        if (isInterface) {
            data.machineInterfaceDirections.put(pos, connectedMachineDirections);
        } else {
            data.machineInterfaceDirections.remove(pos);
        }

        updateFromNode(level, pos, data);
    }

    public static void removeNode(Level level, BlockPos pos, boolean wasInterface, BlockState oldState, PipeNetworkData data) {
        Oritech.LOGGER.debug("removing node: " + pos + " | " + wasInterface);

        var oldNetwork = data.pipeNetworkLinks.getOrDefault(pos, -1);

        data.pipeNetworkLinks.remove(pos);
        if (wasInterface) data.machineInterfaceDirections.remove(pos);

        removeStaleMachinePipeNeighbors(pos, data);

        data.pipeNetworks.remove(oldNetwork);
        data.pipeNetworkInterfaces.remove(oldNetwork);

        // re-calculate old network, is either shorter or split into multiple ones (starting from ones this block was connected to)
        if (oldNetwork != -1) {
            var block = oldState.getBlock();
            for (var direction : Direction.values()) {
                if (block instanceof GenericPipeBlock pipeBlock && oldState.getValue(pipeBlock.directionToProperty(direction)) == NO_CONNECTION) {
                    continue;
                }

                updateFromNode(level, pos.relative(direction), data);
            }
        }

        data.setDirty();
    }

    private static void updateFromNode(Level level, BlockPos pos, PipeNetworkData data) {

        var searchInstance = new FloodFillSearch(pos, data.pipeNetworkLinks.keySet(), level);
        var foundNetwork = new HashSet<>(searchInstance.complete());
        if (foundNetwork.isEmpty()) return;

        var foundMachines = findConnectedMachines(foundNetwork, data);

        Oritech.LOGGER.debug("Nodes:    " + foundNetwork.size() + " | " + foundNetwork);
        Oritech.LOGGER.debug("Machines: " + foundMachines.size() + " | " + foundMachines.stream().map(elem -> elem.machinePos() + ":" + elem.insertedFrom()).toList());

        var netID = foundNetwork.hashCode();
        data.pipeNetworks.put(netID, foundNetwork);
        data.pipeNetworkInterfaces.put(netID, foundMachines);

        // these networks will be replaced, since these nodes now belong to the new network
        var networksToRemove = new HashSet<Integer>();

        for (var node : foundNetwork) {
            networksToRemove.add(data.pipeNetworkLinks.getOrDefault(node, -1));
            data.pipeNetworkLinks.put(node, netID);
        }

        networksToRemove.stream().filter(i -> i != -1 && i != netID).forEach(i -> {
            data.pipeNetworks.remove(i);
            data.pipeNetworkInterfaces.remove(i);
        });

        data.setDirty();
    }

    private static Set<PipeNetworkTarget> findConnectedMachines(Set<BlockPos> network, PipeNetworkData data) {

        var res = new HashSet<PipeNetworkTarget>();

        for (var node : network) {
            for (var machineDirection : data.getMachineDirections(node)) {
                var machinePos = node.relative(machineDirection);
                res.add(new PipeNetworkTarget(machinePos, machineDirection.getOpposite()));
            }
        }

        return res;
    }

    public static Set<PipeNetworkTarget> findNetworkTargets(BlockPos from, PipeNetworkData data) {
        return data.getNetworkTargets(from);
    }

    /**
     * Describes a machine that can be reached by a pipe network.
     *
     * @param machinePos    the position of the connected machine block
     * @param insertedFrom  the side the network inserts into, from the machine's perspective
     */
    public record PipeNetworkTarget(BlockPos machinePos, Direction insertedFrom) {

        /**
         * Gets the pipe interface position that is connected to this machine.
         *
         * @return the neighboring pipe position adjacent to the machine on {@code insertedFrom}
         */
        public BlockPos getPipePos() {
            return machinePos.relative(insertedFrom);
        }

        /**
         * Gets the facing of the pipe interface towards the machine.
         *
         * @return the pipe-side direction that points back at the connected machine
         */
        public Direction getPipeFacing() {
            return insertedFrom.getOpposite();
        }
    }

    /**
     * Removes any stale machine -> neighboring pipes mappings
     * Used when a pipe node is destroyed
     *
     * @param pos  position of the destroyed node
     * @param data network data
     */
    public static void removeStaleMachinePipeNeighbors(BlockPos pos, PipeNetworkData data) {
        for (var neighbor : Direction.values()) {
            var machine = pos.relative(neighbor);
            var machineNeighbors = data.machinePipeNeighbors.get(machine);
            if (machineNeighbors == null) continue;

            var offset = pos.subtract(machine);
            machineNeighbors.remove(Direction.getApproximateNearest(offset.getX(), offset.getY(), offset.getZ()));
            if (machineNeighbors.isEmpty())
                data.machinePipeNeighbors.remove(machine);
            else
                data.machinePipeNeighbors.put(machine, machineNeighbors);
        }
    }

    private static class FloodFillSearch {

        final HashSet<BlockPos> checkedPositions = new HashSet<>();
        final HashSet<BlockPos> nextTargets = new HashSet<>();
        final Deque<BlockPos> foundTargets = new ArrayDeque<>();
        final BlockPos startPosition;
        final Set<BlockPos> pipes;
        final Level level;

        public FloodFillSearch(BlockPos startPosition, Set<BlockPos> pipes, Level level) {
            this.startPosition = startPosition;
            this.pipes = pipes;
            this.level = level;
            nextTargets.add(startPosition);
        }

        public Deque<BlockPos> complete() {
            var active = true;
            while (active) {
                active = !nextGeneration();
            }

            return foundTargets;
        }

        // returns true when done
        @SuppressWarnings("unchecked")
        public boolean nextGeneration() {

            var currentGeneration = (HashSet<BlockPos>) nextTargets.clone();

            for (var target : currentGeneration) {
                if (isValidTarget(target)) {
                    foundTargets.addLast(target);
                    addNeighborsToQueue(target);
                }

                checkedPositions.add(target);
                nextTargets.remove(target);
            }

            if (cutoffSearch()) nextTargets.clear();

            return nextTargets.isEmpty();
        }

        private boolean cutoffSearch() {
            return foundTargets.size() >= MAX_SEARCH_COUNT;
        }

        private boolean isValidTarget(BlockPos target) {
            return pipes.contains(target) || target.equals(startPosition) && level.getBlockState(target).getBlock() instanceof AbstractPipeBlock;
        }

        private void addNeighborsToQueue(BlockPos self) {
            var targetState = level.getBlockState(self);

            if (!(targetState.getBlock() instanceof AbstractPipeBlock targetBlock)) return;
            for (var direction : Direction.values()) {
                var neighbor = self.relative(direction);
                if (checkedPositions.contains(neighbor)) continue;
                if (!isValidTarget(neighbor)) {
                    checkedPositions.add(neighbor);
                    continue;
                }

                // check if the target can connect to the neighbor
                if (!targetBlock.isConnectingInDirection(targetState, direction, self, level, false)) continue;

                nextTargets.add(neighbor);
            }
        }
    }

    public static final class PipeNetworkData extends SavedData {
        public final HashMap<BlockPos, Integer> pipeNetworkLinks = new HashMap<>(); // which blockpos belongs to which network (ID)
        public final HashMap<BlockPos, Set<Direction>> machineInterfaceDirections = new HashMap<>(); // directions of connected machines per interface/connection block
        public final HashMap<Integer, Set<BlockPos>> pipeNetworks = new HashMap<>();   // networks are never updated, and instead always replaced by new ones with different ids
        public final HashMap<Integer, Set<PipeNetworkTarget>> pipeNetworkInterfaces = new HashMap<>(); // list of machines and insertion faces connected to the network
        public final HashMap<BlockPos, Set<Direction>> machinePipeNeighbors = new HashMap<>(); // List of neighboring pipes per machine, and the direction they are in. Missing direction means no connection

        @Override
        public int hashCode() {
            int result = pipeNetworkLinks.hashCode();
            result = 31 * result + machineInterfaceDirections.hashCode();
            result = 31 * result + pipeNetworks.hashCode();
            result = 31 * result + pipeNetworkInterfaces.hashCode();
            result = 31 * result + machinePipeNeighbors.hashCode();
            return result;
        }

        // unboundedMap requires keys that can be represented as strings in object/compound formats,
        // so BlockPos keys are stored as their packed long string and network ids as decimal strings.
        private static final Codec<BlockPos> BLOCK_POS_KEY_CODEC = Codec.STRING.comapFlatMap(value -> {
            try {
                return DataResult.success(BlockPos.of(Long.parseLong(value)));
            } catch (NumberFormatException e) {
                return DataResult.error(() -> "Invalid packed BlockPos key: " + value);
            }
        }, pos -> Long.toString(pos.asLong()));

        private static final Codec<Integer> INT_KEY_CODEC = Codec.STRING.comapFlatMap(value -> {
            try {
                return DataResult.success(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                return DataResult.error(() -> "Invalid integer key: " + value);
            }
        }, Object::toString);

        private static final Codec<Set<BlockPos>> POS_SET_CODEC = BlockPos.CODEC.listOf().xmap(HashSet::new, List::copyOf);
        private static final Codec<Set<Direction>> DIRECTION_SET_CODEC = Direction.CODEC.listOf().xmap(HashSet::new, List::copyOf);

        private static final Codec<PipeNetworkTarget> PIPE_NETWORK_TARGET_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(PipeNetworkTarget::machinePos),
                Direction.CODEC.fieldOf("direction").forGetter(PipeNetworkTarget::insertedFrom)
        ).apply(instance, PipeNetworkTarget::new));

        private static final Codec<Set<PipeNetworkTarget>> INTERFACE_TARGET_SET_CODEC =
                PIPE_NETWORK_TARGET_CODEC.listOf().xmap(HashSet::new, List::copyOf);

        private static final Codec<HashMap<BlockPos, Integer>> PIPE_NETWORK_LINKS_CODEC =
                Codec.unboundedMap(BLOCK_POS_KEY_CODEC, Codec.INT).xmap(HashMap::new, map -> map);

        private static final Codec<HashMap<BlockPos, Set<Direction>>> MACHINE_INTERFACE_DIRECTIONS_CODEC =
                Codec.unboundedMap(BLOCK_POS_KEY_CODEC, DIRECTION_SET_CODEC).xmap(HashMap::new, map -> map);

        private static final Codec<HashMap<Integer, Set<BlockPos>>> PIPE_NETWORKS_CODEC =
                Codec.unboundedMap(INT_KEY_CODEC, POS_SET_CODEC).xmap(HashMap::new, map -> map);

        private static final Codec<HashMap<Integer, Set<PipeNetworkTarget>>> PIPE_NETWORK_INTERFACES_CODEC =
                Codec.unboundedMap(INT_KEY_CODEC, INTERFACE_TARGET_SET_CODEC).xmap(HashMap::new, map -> map);

        private static final Codec<HashMap<BlockPos, Set<Direction>>> MACHINE_PIPE_NEIGHBORS_CODEC =
                Codec.unboundedMap(BLOCK_POS_KEY_CODEC, DIRECTION_SET_CODEC).xmap(HashMap::new, map -> map);

        public static final Codec<PipeNetworkData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PIPE_NETWORK_LINKS_CODEC.optionalFieldOf("pipeNetworkLinks", new HashMap<>()).forGetter(data -> data.pipeNetworkLinks),
                MACHINE_INTERFACE_DIRECTIONS_CODEC.optionalFieldOf("machineInterfaceDirections", new HashMap<>()).forGetter(data -> data.machineInterfaceDirections),
                PIPE_NETWORKS_CODEC.optionalFieldOf("pipeNetworks", new HashMap<>()).forGetter(data -> data.pipeNetworks),
                PIPE_NETWORK_INTERFACES_CODEC.optionalFieldOf("pipeNetworkInterfaces", new HashMap<>()).forGetter(data -> data.pipeNetworkInterfaces),
                MACHINE_PIPE_NEIGHBORS_CODEC.optionalFieldOf("machinePipeNeighbors", new HashMap<>()).forGetter(data -> data.machinePipeNeighbors)
        ).apply(instance, PipeNetworkData::fromCodec));

        private static SavedDataType<PipeNetworkData> createType(String path) {
            return new SavedDataType<>(
                    Oritech.id(path),
                    PipeNetworkData::new,
                    CODEC,
                    null);
        }

        public static final SavedDataType<PipeNetworkData> ENERGY_TYPE = createType("energy_pipe_data");
        public static final SavedDataType<PipeNetworkData> FLUID_TYPE = createType("fluid_pipe_data");
        public static final SavedDataType<PipeNetworkData> ITEM_TYPE = createType("item_pipe_data");
        public static final SavedDataType<PipeNetworkData> SUPERCONDUCTOR_TYPE = createType("superconductor_pipe_data");

        public Set<Direction> getMachineDirections(BlockPos interfacePos) {
            return machineInterfaceDirections.getOrDefault(interfacePos, Set.of());
        }

        public Set<BlockPos> getNetworkNodes(BlockPos pipePos) {
            return pipeNetworks.getOrDefault(pipeNetworkLinks.getOrDefault(pipePos, -1), Set.of());
        }

        public Set<PipeNetworkTarget> getNetworkTargets(BlockPos pipePos) {
            return pipeNetworkInterfaces.getOrDefault(pipeNetworkLinks.getOrDefault(pipePos, -1), Set.of());
        }

        private static PipeNetworkData fromCodec(HashMap<BlockPos, Integer> links,
                                                 HashMap<BlockPos, Set<Direction>> machineInterfaceDirections,
                                                 HashMap<Integer, Set<BlockPos>> networks,
                                                 HashMap<Integer, Set<PipeNetworkTarget>> networkInterfaces,
                                                 HashMap<BlockPos, Set<Direction>> machineNeighbors) {
            var data = new PipeNetworkData();
            data.pipeNetworkLinks.putAll(links);
            data.machineInterfaceDirections.putAll(machineInterfaceDirections);
            data.pipeNetworks.putAll(networks);
            data.pipeNetworkInterfaces.putAll(networkInterfaces);
            data.machinePipeNeighbors.putAll(machineNeighbors);

            return data;
        }
    }
}
