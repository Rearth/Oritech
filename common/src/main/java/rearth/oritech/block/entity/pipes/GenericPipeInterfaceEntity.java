package rearth.oritech.block.entity.pipes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.AbstractPipeBlock;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.entity.interaction.PipeBoosterBlockEntity;

import java.util.*;
import java.util.stream.Collectors;

import static rearth.oritech.block.blocks.pipes.GenericPipeBlock.NO_CONNECTION;

public abstract class GenericPipeInterfaceEntity extends BlockEntity implements BlockEntityTicker<GenericPipeInterfaceEntity> {
    
    public static final int MAX_SEARCH_COUNT = 2048;
    
    public BlockPos connectedBooster = BlockPos.ORIGIN;

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
			connectedBooster = BlockPos.ORIGIN;
			return null;
		}

		if (connectedBooster == BlockPos.ORIGIN) {  // no booster set
			if (cachedBooster != null) cachedBooster = null;
			return null;
		} else if (cachedBooster == null) { // booster freshly set
			var candidate = Objects.requireNonNull(world).getBlockEntity(connectedBooster);
			if (candidate instanceof PipeBoosterBlockEntity booster) {
				cachedBooster = booster;
				return cachedBooster;
			} else {
				connectedBooster = BlockPos.ORIGIN;
				return null;
			}
		} else {    // no change
			return cachedBooster;
		}
	}

    public static void addNode(World world, BlockPos pos, boolean isInterface, BlockState newState, PipeNetworkData data) {
        Oritech.LOGGER.debug("registering/updating node: " + pos);

        data.pipes.add(pos);
        var connectedMachines = new HashSet<BlockPos>(6);
        var block = (AbstractPipeBlock) newState.getBlock();
        for (var neighbor : Direction.values()) {
            var neighborPos = pos.offset(neighbor);
            var neighborMap = data.machinePipeNeighbors.getOrDefault(neighborPos, new HashSet<>());
            if (block.hasMachineInDirection(neighbor, world, pos, block.apiValidationFunction())) {
                if (block.isConnectingInDirection(newState, neighbor, pos, world, false))
                    connectedMachines.add(pos.offset(neighbor));

                neighborMap.add(neighbor.getOpposite());
            } else {
                neighborMap.remove(neighbor.getOpposite());
            }

            if (!neighborMap.isEmpty()) data.machinePipeNeighbors.put(neighborPos, neighborMap);
            else data.machinePipeNeighbors.remove(neighborPos);
        }

        if (isInterface) {
            data.machineInterfaces.put(pos, connectedMachines);
        } else {
            data.machineInterfaces.remove(pos);
        }

        updateFromNode(world, pos, data);
    }

    public static void removeNode(World world, BlockPos pos, boolean wasInterface, BlockState oldState, PipeNetworkData data) {
        Oritech.LOGGER.debug("removing node: " + pos + " | " + wasInterface);

        var oldNetwork = data.pipeNetworkLinks.getOrDefault(pos, -1);

        data.pipes.remove(pos);
        if (wasInterface) data.machineInterfaces.remove(pos);

        removeStaleMachinePipeNeighbors(pos, data);

        data.pipeNetworks.remove(oldNetwork);
        data.pipeNetworkInterfaces.remove(oldNetwork);
        data.pipeNetworkLinks.remove(pos);

        // re-calculate old network, is either shorter or split into multiple ones (starting from ones this block was connected to)
        if (oldNetwork != -1) {
            var block = oldState.getBlock();
            for (var direction : Direction.values()) {
                if (block instanceof GenericPipeBlock pipeBlock && oldState.get(pipeBlock.directionToProperty(direction)) == NO_CONNECTION) {
                    continue;
                }

                updateFromNode(world, pos.offset(direction), data);
            }
        }

        data.markDirty();
    }

    private static void updateFromNode(World world, BlockPos pos, PipeNetworkData data) {

        var searchInstance = new FloodFillSearch(pos, data.pipes, world);
        var foundNetwork = new HashSet<>(searchInstance.complete());
        var foundMachines = findConnectedMachines(foundNetwork, data);

        Oritech.LOGGER.debug("Nodes:    " + foundNetwork.size() + " | " + foundNetwork);
        Oritech.LOGGER.debug("Machines: " + foundMachines.size() + " | " + foundMachines.stream().map(elem -> elem.getLeft() + ":" + elem.getRight()).toList());

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

        data.markDirty();
    }

    private static Set<Pair<BlockPos, Direction>> findConnectedMachines(Set<BlockPos> network, PipeNetworkData data) {

        var res = new HashSet<Pair<BlockPos, Direction>>();

        for (var node : network) {
            if (data.machineInterfaces.containsKey(node)) {
                for (var machinePos : data.machineInterfaces.get(node)) {
                    var offset = machinePos.subtract(node);
                    var direction = Direction.fromVector(offset.getX(), offset.getY(), offset.getZ()).getOpposite();
                    res.add(new Pair<>(machinePos, direction));
                }
            }
        }

        return res;
    }

    public static Set<Pair<BlockPos, Direction>> findNetworkTargets(BlockPos from, PipeNetworkData data) {
        var connectedNetwork = data.pipeNetworkLinks.getOrDefault(from, -1);
        if (connectedNetwork == -1) return new HashSet<>();

        return data.pipeNetworkInterfaces.get(connectedNetwork);
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
            var machine = pos.offset(neighbor);
            var machineNeighbors = data.machinePipeNeighbors.get(machine);
            if (machineNeighbors == null) continue;

            machineNeighbors.remove(Direction.getFacing(Vec3d.of(pos.subtract(machine))));
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
        final HashSet<BlockPos> pipes;
        final World world;

        public FloodFillSearch(BlockPos startPosition, HashSet<BlockPos> pipes, World world) {
            this.pipes = pipes;
            this.world = world;
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
            return pipes.contains(target);
        }

        private void addNeighborsToQueue(BlockPos self) {
            var targetState = world.getBlockState(self);

            if (!(targetState.getBlock() instanceof AbstractPipeBlock targetBlock)) return;
            for (var direction : Direction.values()) {
                var neighbor = self.offset(direction);
                if (checkedPositions.contains(neighbor)) continue;
                if (!isValidTarget(neighbor)) {
                    checkedPositions.add(neighbor);
                    continue;
                }

                // check if the target can connect to the neighbor
                if (!targetBlock.isConnectingInDirection(targetState, direction, self, world, false)) continue;

                nextTargets.add(neighbor);
            }
        }
    }

    public static final class PipeNetworkData extends PersistentState {
        public final HashMap<BlockPos, Integer> pipeNetworkLinks = new HashMap<>(); // which blockpos belongs to which network (ID)
        public final HashSet<BlockPos> pipes = new HashSet<>();
        public final HashMap<BlockPos, Set<BlockPos>> machineInterfaces = new HashMap<>(); // list of machines per interface/connection block
        public final HashMap<Integer, Set<BlockPos>> pipeNetworks = new HashMap<>();   // networks are never updated, and instead always replaced by new ones with different ids
        public final HashMap<Integer, Set<Pair<BlockPos, Direction>>> pipeNetworkInterfaces = new HashMap<>(); // list of machines that are connected to the network

        public final HashMap<BlockPos, Set<Direction>> machinePipeNeighbors = new HashMap<>(); // List of neighboring pipes per machine, and the direction they are in. Missing direction means no connection

        @Override
        public int hashCode() {
            int result = pipeNetworkLinks.hashCode();
            result = 31 * result + pipes.hashCode();
            result = 31 * result + machineInterfaces.hashCode();
            result = 31 * result + pipeNetworks.hashCode();
            result = 31 * result + pipeNetworkInterfaces.hashCode();
            return result;
        }

        public static Type<PipeNetworkData> TYPE = new Type<>(PipeNetworkData::new, PipeNetworkData::fromNbt, null);

        public static PipeNetworkData fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {

            var result = new PipeNetworkData();

            if (nbt.contains("pipeNetworkLinks", NbtElement.LIST_TYPE)) {
                var pipeNetworkLinksList = nbt.getList("pipeNetworkLinks", NbtElement.COMPOUND_TYPE);
                for (var element : pipeNetworkLinksList) {
                    var entry = (NbtCompound) element;
                    var pos = BlockPos.fromLong(entry.getLong("pos"));
                    var id = entry.getInt("id");
                    result.pipeNetworkLinks.put(pos, id);
                }
            }

            // Deserialize pipes
            if (nbt.contains("pipes", NbtElement.LIST_TYPE)) {
                var pipesList = nbt.getList("pipes", NbtElement.LONG_TYPE);
                pipesList.stream().map(element -> BlockPos.fromLong(((NbtLong) element).longValue())).forEach(result.pipes::add);
            }

            // Deserialize machineInterfaces
            if (nbt.contains("machineInterfaces", NbtElement.COMPOUND_TYPE)) {
                var machineInterfacesNbt = nbt.getCompound("machineInterfaces");
                for (var key : machineInterfacesNbt.getKeys()) {
                    var interfacePos = BlockPos.fromLong(Long.parseLong(key));
                    var machinesArray = machineInterfacesNbt.getLongArray(key);
                    var machines = Arrays.stream(machinesArray)
                                     .mapToObj(BlockPos::fromLong)
                                     .collect(Collectors.toSet());
                    result.machineInterfaces.put(interfacePos, machines);
                }
            }

            // Deserialize pipeNetworks
            if (nbt.contains("pipeNetworks", NbtElement.COMPOUND_TYPE)) {
                var pipeNetworksNbt = nbt.getCompound("pipeNetworks");
                for (var key : pipeNetworksNbt.getKeys()) {
                    var id = Integer.parseInt(key);
                    var networkArray = pipeNetworksNbt.getLongArray(key);
                    var network = Arrays.stream(networkArray)
                                    .mapToObj(BlockPos::fromLong)
                                    .collect(Collectors.toSet());
                    result.pipeNetworks.put(id, network);
                }
            }

            // Deserialize pipeNetworkInterfaces
            if (nbt.contains("pipeNetworkInterfaces", NbtElement.COMPOUND_TYPE)) {
                var pipeNetworkInterfacesNbt = nbt.getCompound("pipeNetworkInterfaces");
                for (var key : pipeNetworkInterfacesNbt.getKeys()) {
                    var id = Integer.parseInt(key);
                    var interfacesList = pipeNetworkInterfacesNbt.getList(key, NbtElement.COMPOUND_TYPE);
                    var interfaces = new HashSet<Pair<BlockPos, Direction>>();
                    for (var interfaceElement : interfacesList) {
                        var pairNbt = (NbtCompound) interfaceElement;
                        var pos = BlockPos.fromLong(pairNbt.getLong("pos"));
                        var direction = Direction.byName(pairNbt.getString("direction"));
                        interfaces.add(new Pair<>(pos, direction));
                    }
                    result.pipeNetworkInterfaces.put(id, interfaces);
                }
            }

            // Deserialize machinePipeNeighbors
            if (nbt.contains("machinePipeNeighbors", NbtElement.COMPOUND_TYPE)) {
                var connectionPipeNeighborsNbt = nbt.getCompound("machinePipeNeighbors");
                for (var key : connectionPipeNeighborsNbt.getKeys()) {
                    var pos = BlockPos.fromLong(Long.parseLong(key));
                    var neighborsList = connectionPipeNeighborsNbt.getList(key, NbtElement.STRING_TYPE);
                    var neighbors = new HashSet<Direction>();
                    for (var neighborElement : neighborsList) {
                        var direction = Direction.byName(neighborElement.asString());
                        neighbors.add(direction);
                    }
                    result.machinePipeNeighbors.put(pos, neighbors);
                }
            }

            result.markDirty();

            return result;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {

            // Serialize pipeNetworkLinks
            var pipeNetworkLinksList = new NbtList();
            pipeNetworkLinks.forEach((pos, id) -> {
                var entry = new NbtCompound();
                entry.putLong("pos", pos.asLong());
                entry.putInt("id", id);
                pipeNetworkLinksList.add(entry);
            });
            nbt.put("pipeNetworkLinks", pipeNetworkLinksList);

            // Serialize pipes
            var pipesList = new NbtList();
            pipes.forEach(pos -> pipesList.add(NbtLong.of(pos.asLong())));
            nbt.put("pipes", pipesList);

            // Serialize machineInterfaces
            var machineInterfacesNbt = new NbtCompound();
            machineInterfaces.forEach((interfacePos, machines) -> {
                machineInterfacesNbt.putLongArray(Long.toString(interfacePos.asLong()), machines.stream().map(BlockPos::asLong).collect(Collectors.toList()));
            });
            nbt.put("machineInterfaces", machineInterfacesNbt);

            // Serialize pipeNetworks
            var pipeNetworksNbt = new NbtCompound();
            pipeNetworks.forEach((id, network) -> {
                pipeNetworksNbt.putLongArray(id.toString(), network.stream().map(BlockPos::asLong).collect(Collectors.toList()));
            });
            nbt.put("pipeNetworks", pipeNetworksNbt);

            // Serialize pipeNetworkInterfaces
            var pipeNetworkInterfacesNbt = new NbtCompound();
            pipeNetworkInterfaces.forEach((id, interfaces) -> {
                var interfacesList = new NbtList();
                interfaces.forEach(pair -> {
                    var pairNbt = new NbtCompound();
                    pairNbt.putLong("pos", pair.getLeft().asLong());
                    pairNbt.putString("direction", pair.getRight().getName());
                    interfacesList.add(pairNbt);
                });
                pipeNetworkInterfacesNbt.put(id.toString(), interfacesList);
            });
            nbt.put("pipeNetworkInterfaces", pipeNetworkInterfacesNbt);

            // Serialize machinePipeNeighbors
            var connectionPipeNeighborsNbt = new NbtCompound();
            machinePipeNeighbors.forEach((pos, neighbors) -> {
                var neighborsList = new NbtList();
                neighbors.forEach(direction -> {
                    var nbtElement = NbtString.of(direction.getName());
                    neighborsList.add(nbtElement);
                });
                connectionPipeNeighborsNbt.put(Long.toString(pos.asLong()), neighborsList);
            });
            nbt.put("machinePipeNeighbors", connectionPipeNeighborsNbt);

            return nbt;
        }
    }

}
